"""Observational hooks: no random draws, loss terms or memory interventions."""
from __future__ import annotations

import time
from collections import Counter

import torch

from sls.diagnostics.cpu import decision_record
from sls.research.bank import rebuild, score
from sls.rl.ppo import decision_domains, normalize_advantages_by_domain


class ResearchDiagnostics:
    def __init__(self, workers, *, prefix_workers=4):
        self.histories = [[] for _ in range(workers)]
        self.sources = [{"kind": "normal"} for _ in range(workers)]
        self.seen = [set() for _ in range(workers)]
        self.episode_domains = [Counter() for _ in range(workers)]
        self.rollout_acts = []
        self.exposure = Counter()
        self.records = []
        self.prefix_workers = prefix_workers
        self.seconds = 0.

    def transition(self, index, current, action, transition, reward, reason):
        act = current.observation.run.act
        domain = current.observation.screen.value
        self.rollout_acts.append(act)
        # Bounded to one maximum configured production rollout; warmup is also observed.
        if len(self.rollout_acts) > len(self.histories) * 256:
            del self.rollout_acts[:len(self.histories)]
        self.episode_domains[index][f"act{act}:{domain}"] += 1
        self.exposure[f"decisions:act{act}:{domain}"] += 1
        encounter = "+".join(sorted(e.monster_id for e in current.observation.enemies))
        key = (act, current.observation.run.floor, encounter)
        if domain == "COMBAT" and encounter and key not in self.seen[index]:
            self.seen[index].add(key)
            self.exposure[f"combat_entries:act{act}:{encounter}"] += 1
        if reason:
            self.exposure[f"termination:act{act}:{reason}"] += 1
            if reason == "success":
                for key, count in self.episode_domains[index].items():
                    self.exposure[f"successful_episode_decisions:{key}"] += count
        row = {**decision_record(current), "chosen_action": action.to_dict(),
               "raw_reward": float(transition.reward),
               "shaped_reward": float(torch.tensor(reward, dtype=torch.float32)),
               "terminal": reason is not None}
        self.histories[index].append(row)

    def reset(self, index, initial):
        self.histories[index] = list(initial.prefix) if initial is not None else []
        self.sources[index] = dict(initial.source) if initial is not None else {"kind": "normal"}
        self.seen[index] = set()
        self.episode_domains[index] = Counter()

    def state_dict(self):
        return {"histories": self.histories, "sources": self.sources,
                "exposure": dict(self.exposure), "records": self.records,
                "seen": self.seen, "episode_domains": self.episode_domains, "rollout_acts": self.rollout_acts,
                "prefix_workers": self.prefix_workers, "seconds": self.seconds}

    def load_state_dict(self, state):
        if state["prefix_workers"] != self.prefix_workers or len(state["histories"]) != len(self.histories):
            raise ValueError("research diagnostics layout mismatch")
        self.histories, self.sources = state["histories"], state["sources"]
        self.exposure, self.records = Counter(state["exposure"]), state["records"]
        self.seen, self.episode_domains, self.rollout_acts = state["seen"], state["episode_domains"], state["rollout_acts"]
        self.seconds = state["seconds"]

    @torch.no_grad()
    def before_update(self, trainer, rollout):
        begin = time.perf_counter()
        self.probes = []
        # Full prefix on fixed worker IDs, not truncated training sequence chunks.
        for index in range(min(self.prefix_workers, trainer.workers.size)):
            memory, action, reward = rebuild(trainer.model, self.histories[index])
            boundary = trainer.decisions[index]
            fresh = score(trainer.model, boundary, memory, action, reward, not self.histories[index])
            saved = score(trainer.model, boundary, trainer.memory[index:index + 1],
                          int(trainer.previous_action_types[index]), float(trainer.previous_rewards[index]),
                          bool(trainer.episode_starts[index]))
            self.probes.append((index, fresh.logits.detach(), memory.detach(),
                                saved.logits.detach(), trainer.memory[index:index + 1].detach().clone()))
        domains = decision_domains(rollout.encoded_decisions)
        normalized = normalize_advantages_by_domain(rollout.advantages, rollout.encoded_decisions)
        acts = torch.tensor(self.rollout_acts[-rollout.action_indices.numel():]).reshape(rollout.shape)
        groups = {}
        for domain in domains.unique().tolist():
            raw = rollout.advantages[domains == domain]
            groups[str(domain)] = {"n": raw.numel(), "raw_mean": float(raw.mean()),
                                  "raw_std": float(raw.std(unbiased=False)),
                                  "scaled_std": float(normalized[domains == domain].std(unbiased=False))}
        self.current = {"update": trainer.update + 1, "advantages_by_domain": groups,
                        "exposure_cumulative": dict(self.exposure), "gradient_sampling": "first_minibatch"}
        self.current["advantages_by_act_domain"] = {}
        for act in acts.unique().tolist():
            for domain in domains.unique().tolist():
                mask = (acts == act) & (domains == domain)
                if not mask.any():
                    continue
                raw, scaled = rollout.advantages[mask], normalized[mask]
                std = rollout.advantages[domains == domain].std(unbiased=False)
                self.current["advantages_by_act_domain"][f"act{act}:domain{domain}"] = {
                    "n": raw.numel(), "raw_mean": float(raw.mean()), "raw_std": float(raw.std(unbiased=False)),
                    "scaled_mean": float(scaled.mean()), "scaled_std": float(scaled.std(unbiased=False)),
                    "actual_domain_scale": float(1 / (std + 1e-8))}
        self.seconds += time.perf_counter() - begin

    def gradients(self, trainer, policy_loss, value_loss, total_loss):
        begin = time.perf_counter()
        parameters = [p for p in trainer.model.parameters() if p.requires_grad]
        def flatten(loss):
            gradients = torch.autograd.grad(loss, parameters, retain_graph=True, allow_unused=True)
            return torch.cat([torch.zeros_like(p).flatten() if g is None else g.detach().flatten()
                              for p, g in zip(parameters, gradients)])
        actor, value, combined = map(flatten, (policy_loss, value_loss, total_loss))
        a, v, c = (float(x.norm()) for x in (actor, value, combined))
        cosine = float(torch.dot(actor, value)) / (a * v) if a * v else None
        offset, shared_actor, shared_value = 0, [], []
        for name, parameter in trainer.model.named_parameters():
            if not parameter.requires_grad:
                continue
            size = parameter.numel()
            if not name.startswith("value_head."):
                shared_actor.append(actor[offset:offset + size])
                shared_value.append(value[offset:offset + size])
            offset += size
        sa, sv = torch.cat(shared_actor), torch.cat(shared_value)
        san, svn = float(sa.norm()), float(sv.norm())
        self.current["gradients"] = {"actor_norm": a, "weighted_value_norm": v,
            "cosine": cosine, "combined_preclip_norm": c,
            "combined_clip_factor": min(1., trainer.config.max_gradient_norm / (c + 1e-6)),
            "includes_entropy_in_combined": True}
        self.current["gradients"].update({"shared_actor_norm": san, "shared_weighted_value_norm": svn,
            "shared_cosine": float(torch.dot(sa, sv)) / (san * svn) if san * svn else None})
        self.seconds += time.perf_counter() - begin

    def warmup_gradient(self, trainer, loss):
        begin = time.perf_counter()
        parameters = list(trainer.model.value_head.parameters())
        gradients = torch.autograd.grad(loss, parameters, retain_graph=True)
        norm = float(torch.cat([g.detach().flatten() for g in gradients]).norm())
        self.records.append({"update": trainer.update + 1, "phase": "critic_warmup",
            "gradient_sampling": "first_minibatch", "actor_norm": 0., "shared_value_norm": 0.,
            "weighted_value_head_norm": norm, "clip_factor": min(1., trainer.config.max_gradient_norm / (norm + 1e-6)),
            "exposure_cumulative": dict(self.exposure)})
        self.seconds += time.perf_counter() - begin

    @torch.no_grad()
    def after_update(self, trainer):
        begin = time.perf_counter()
        probes = []
        for index, old_logits, old_memory, old_saved_logits, saved_memory in self.probes:
            memory, action, reward = rebuild(trainer.model, self.histories[index])
            decision = trainer.decisions[index]
            current = score(trainer.model, decision, memory, action, reward, not self.histories[index])
            saved = score(trainer.model, decision, saved_memory, action, reward, not self.histories[index])
            def kl(a, b):
                p, q = a.log_softmax(-1), b.log_softmax(-1)
                return float((p.exp() * (p - q)).sum())
            probes.append({"worker": index, "prefix_decisions": len(self.histories[index]),
                "source": self.sources[index], "memory_rms_before": float((old_memory - saved_memory).square().mean().sqrt()),
                "memory_rms_after": float((memory - saved_memory).square().mean().sqrt()),
                "saved_vs_rebuilt_kl_before": kl(old_logits, old_saved_logits),
                "saved_vs_rebuilt_kl_after": kl(current.logits, saved.logits),
                "full_prefix_policy_kl": kl(old_logits, current.logits)})
        self.current["prefix_probes"] = probes
        self.records.append(self.current)
        self.seconds += time.perf_counter() - begin
