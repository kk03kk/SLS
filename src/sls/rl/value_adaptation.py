"""Isolated, resumable Monte Carlo value-head calibration before PPO.

The collector owns its simulator and random generator. No formal PPO worker,
optimizer or random stream is borrowed. Episode files become immutable once
registered in the atomic journal; an unregistered file is an interrupted write.
"""

from __future__ import annotations

import copy
import hashlib
import os
from dataclasses import asdict, dataclass
from pathlib import Path

import torch

from sls.model import PolicyBatch
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.reward import curriculum_terminal_reward, shape_curriculum_reward
from sls.rl.training_contract import native_source_digest, sha256_file


@dataclass(frozen=True)
class ValueAdaptationConfig:
    episodes: int = 512
    fit_episodes: int = 384
    seed_start: int = 190_000_000
    random_seed: int = 190_000_000
    max_decisions: int = 524_288
    learning_rate: float = 1e-3
    batch_size: int = 512
    epochs: int = 8

    def __post_init__(self):
        import math

        for key in ("episodes", "fit_episodes", "seed_start", "random_seed", "max_decisions", "batch_size", "epochs"):
            if type(getattr(self, key)) is not int or getattr(self, key) < 0:
                raise ValueError(f"invalid value adaptation {key}")
        if not 0 < self.fit_episodes < self.episodes:
            raise ValueError("value adaptation requires disjoint fit and validation episodes")
        if not self.max_decisions or not self.batch_size or not 0 < self.epochs <= 8:
            raise ValueError("invalid value adaptation budget")
        if not math.isfinite(self.learning_rate) or self.learning_rate <= 0:
            raise ValueError("invalid value adaptation learning rate")
        if self.seed_start + self.episodes > 2_000_000_000_000:
            raise ValueError("adaptation seeds must stay in the training namespace")


def config_from_payload(payload):
    spec = payload.get("value_adaptation")
    if spec is None:
        return None
    if "start_curriculum" in payload:
        raise ValueError("value adaptation requires 100% normal starts")
    result = ValueAdaptationConfig(**spec)
    run = payload.get("run", {})
    if "seed" in run:
        # A conservative reservation for subsequent PPO resets, including all
        # planned decisions. Never overlap the calibration episodes.
        first = int(run["seed"])
        target = int(payload["stages"]["train"]["target_environment_steps"])
        parent = int(payload.get("warm_start", {}).get("parent_environment_steps", 0))
        if result.seed_start < first + target - parent + 128 and first < result.seed_start + result.episodes:
            raise ValueError("adaptation and PPO training seed reservations overlap")
        if result.seed_start + result.episodes > int(run.get("training_seed_limit", 2_000_000_000_000)):
            raise ValueError("adaptation seed reservation reaches held-out namespace")
    return result


def decision_accounting(trainer, parent_steps):
    adaptation = getattr(trainer, "value_adaptation_state", None)
    count = int(adaptation["decisions"]) if adaptation is not None else 0
    new = trainer.environment_steps - parent_steps
    return {"parent_decisions": parent_steps, "new_decisions": new,
            "value_adaptation_decisions": count, "ppo_decisions": new - count}


def state_digest(state):
    digest = hashlib.sha256()
    for name, value in sorted(state.items()):
        value = value.detach().cpu().contiguous()
        digest.update(name.encode())
        digest.update(str((str(value.dtype), tuple(value.shape))).encode())
        digest.update(value.numpy().tobytes())
    return digest.hexdigest()


def complete_returns(rewards):
    """Undiscounted complete-episode returns, with no value bootstrap."""
    rewards = torch.as_tensor(rewards, dtype=torch.float32)
    if rewards.ndim != 1 or not rewards.numel() or not torch.isfinite(rewards).all():
        raise ValueError("invalid complete episode rewards")
    return rewards.flip(0).cumsum(0).flip(0)


def _atomic_save(value, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(target.suffix + ".tmp")
    torch.save(value, temporary)
    os.replace(temporary, target)


class ValueAdapter:
    def __init__(self, model, profile, ppo, config, directory, *, backend=None):
        from sls.backends.simulator.environment import SimulatorBackend

        if ppo.gamma != 1:
            raise ValueError("complete-return calibration requires gamma=1")
        self.config, self.ppo, self.profile = config, ppo, profile
        self.directory = Path(directory)
        self.journal = self.directory / "latest.pt"
        self.counter = self.directory / "interaction-counter.pt"
        self.model = copy.deepcopy(model).eval()
        for parameter in self.model.parameters():
            parameter.requires_grad_(False)
        self.head = copy.deepcopy(self.model.value_head)
        for parameter in self.head.parameters():
            parameter.requires_grad_(True)
        self.device = next(self.model.parameters()).device
        self.optimizer = torch.optim.Adam(self.head.parameters(), lr=config.learning_rate)
        self.generator = torch.Generator(device="cpu").manual_seed(config.random_seed)
        self.backend = backend if backend is not None else SimulatorBackend(profile)
        self.contract = {
            "schema": "sls-value-adaptation-v1", "config": asdict(config),
            "profile": profile, "ppo": ppo.to_dict(), "model": model.config.to_dict(),
            "initial_model_sha256": state_digest(model.state_dict()),
            "native_source_sha256": native_source_digest(),
            "torch": str(torch.__version__), "device": str(self.device),
        }
        self.data = None
        if self.journal.exists():
            self._load()
        else:
            self.state = {"phase": "COLLECT", "episodes": [], "decisions": 0, "interactions": 0,
                          "active": None, "epoch": 0, "cursor": 0,
                          "permutation": None, "baseline_mse": None, "best_mse": None,
                          "best_epoch": 0, "history": []}
            self.best_head = copy.deepcopy(self.head.state_dict())
            self._save()
            self._save_counter()

    def _save_counter(self, interactions=None):
        # Tiny write before each backend call: a process death may replay cached
        # steps, but must not silently give the collector free interactions.
        _atomic_save({"parent": self.contract["initial_model_sha256"],
                      "interactions": self.state["interactions"] if interactions is None else interactions}, self.counter)

    def _save(self):
        _atomic_save({"contract": self.contract, "state": self.state,
                      "head": self.head.state_dict(), "best_head": self.best_head,
                      "optimizer": self.optimizer.state_dict(),
                      "rng": self.generator.get_state()}, self.journal)

    def _load(self):
        payload = torch.load(self.journal, weights_only=False, map_location="cpu")
        if payload["contract"] != self.contract:
            raise ValueError("value adaptation resume contract mismatch")
        self.state = payload["state"]
        if not self.counter.exists():
            if self.state["decisions"]:
                raise ValueError("value adaptation interaction counter missing")
            self._save_counter()
        counter = torch.load(self.counter, weights_only=False, map_location="cpu")
        if (counter["parent"] != self.contract["initial_model_sha256"]
                or type(counter["interactions"]) is not int
                or counter["interactions"] < self.state["interactions"]
                or counter["interactions"] > self.config.max_decisions):
            raise ValueError("value adaptation interaction counter mismatch")
        self.state["interactions"] = counter["interactions"]
        self.head.load_state_dict(payload["head"])
        self.best_head = payload["best_head"]
        self.optimizer.load_state_dict(payload["optimizer"])
        self.generator.set_state(payload["rng"])
        for item in self.state["episodes"]:
            if sha256_file(self.directory / item["path"]) != item["sha256"]:
                raise ValueError("value adaptation episode identity mismatch")

    def _collect_one(self):
        active = self.state["active"]
        if active is None:
            decision = self.backend.reset(self.config.seed_start + len(self.state["episodes"]))
            if decision.observation.screen.value != "NEOW":
                raise ValueError("value adaptation must start at normal Neow")
            active = {"backend": self.backend.checkpoint(), "features": [], "rewards": [],
                      "memory": torch.zeros(1, self.model.config.recurrent_hidden_dim),
                      "action": 0, "reward": 0., "limit": EpisodeLimitState.initial(decision).to_dict()}
        else:
            decision = self.backend.load_checkpoint(active["backend"])
        batch = PolicyBatch.from_decisions([decision], self.model.config).to(self.device)
        rng = self.generator.get_state()
        try:
            with torch.no_grad():
                output = self.model(*batch.model_inputs(), memory=active["memory"].to(self.device),
                                    episode_start_mask=torch.tensor([not active["features"]], device=self.device),
                                    previous_action_types=torch.tensor([active["action"]], device=self.device),
                                    previous_rewards=torch.tensor([active["reward"]], device=self.device))
                index = int(torch.multinomial(output.logits.softmax(-1).cpu(), 1, generator=self.generator))
            interactions = self.state["interactions"] + 1
            self._save_counter(interactions)
            self.state["interactions"] = interactions
            item = self.backend.step(decision.actions[index].candidate_id)
            if item.truncated:
                raise RuntimeError("value adaptation backend truncated")
            limiter = EpisodeLimitState.from_dict(active["limit"])
            reason = ("success" if item.info.get("success") else "death") if item.terminated else limiter.observe(
                item.decision, max_steps=self.ppo.max_episode_steps, max_boundary_visits=self.ppo.max_boundary_visits)
            terminal = reason is not None
            reward = float(item.reward)
            if item.terminated:
                reward = curriculum_terminal_reward(item.decision.observation, self.profile,
                    success=bool(item.info.get("success")), failure_progress_scale=self.ppo.failure_progress_scale)
            elif terminal:
                reward = self.ppo.limit_failure_reward
            if self.ppo.potential_shaping:
                reward = shape_curriculum_reward(reward, decision.observation, item.decision.observation,
                    self.profile, gamma=self.ppo.gamma, scale=self.ppo.potential_scale, terminal=terminal)
            # Prepare everything that can fail before committing the step.
            following = {**active, "backend": self.backend.checkpoint(),
                "memory": output.next_memory.detach().cpu(), "action": int(batch.action_types[0, index]) + 1,
                "reward": float(item.reward), "limit": limiter.to_dict(),
                "features": [*active["features"], output.next_memory[0].detach().cpu()],
                "rewards": [*active["rewards"], reward]}
            entry = None
            if terminal:
                ordinal = len(self.state["episodes"])
                path = self.directory / "episodes" / f"{ordinal:06d}.pt"
                episode = {"features": torch.stack(following["features"]),
                           "returns": complete_returns(following["rewards"]),
                           "seed": self.config.seed_start + ordinal, "reason": reason}
                _atomic_save(episode, path)
                entry = {"path": path.relative_to(self.directory).as_posix(),
                         "sha256": sha256_file(path), "decisions": len(following["rewards"]),
                         "seed": episode["seed"], "reason": reason,
                         "split": "fit" if ordinal < self.config.fit_episodes else "validation"}
        except BaseException:
            self.generator.set_state(rng)
            self.backend.load_checkpoint(active["backend"])
            raise
        self.state["decisions"] += 1
        self.state["active"] = None if terminal else following
        if entry is not None:
            self.state["episodes"].append(entry)

    def _dataset(self):
        if self.data is None:
            splits = {}
            for name in ("fit", "validation"):
                episodes = [torch.load(self.directory / entry["path"], map_location="cpu", weights_only=False)
                            for entry in self.state["episodes"] if entry["split"] == name]
                splits[name] = (torch.cat([e["features"] for e in episodes]), torch.cat([e["returns"] for e in episodes]))
            self.data = splits
        return self.data

    @torch.no_grad()
    def _validation_mse(self):
        x, y = self._dataset()["validation"]
        total = 0.
        for start in range(0, len(y), self.config.batch_size):
            target = y[start:start + self.config.batch_size].to(self.device)
            prediction = self.head(x[start:start + self.config.batch_size].to(self.device)).squeeze(-1)
            total += float((prediction - target).square().sum())
        error = total / len(y)
        import math
        if not math.isfinite(error):
            raise RuntimeError("nonfinite value adaptation validation error")
        return error

    def _fit_one(self):
        x, y = self._dataset()["fit"]
        if self.state["baseline_mse"] is None:
            self.state["baseline_mse"] = self.state["best_mse"] = self._validation_mse()
        if self.state["permutation"] is None:
            self.state["permutation"] = torch.randperm(len(y), generator=self.generator)
        cursor = self.state["cursor"]
        indices = self.state["permutation"][cursor:cursor + self.config.batch_size]
        # A failed Adam call may partially modify state: restore this transaction
        # before saving diagnostics or resuming in the same process.
        head = copy.deepcopy(self.head.state_dict())
        optimizer = copy.deepcopy(self.optimizer.state_dict())
        try:
            self.optimizer.zero_grad(set_to_none=True)
            loss = (self.head(x[indices].to(self.device)).squeeze(-1) - y[indices].to(self.device)).square().mean()
            if not torch.isfinite(loss):
                raise RuntimeError("nonfinite value adaptation loss")
            loss.backward()
            if any(p.grad is not None and not torch.isfinite(p.grad).all() for p in self.head.parameters()):
                raise RuntimeError("nonfinite value adaptation gradient")
            self.optimizer.step()
            if any(not torch.isfinite(p).all() for p in self.head.parameters()):
                raise RuntimeError("nonfinite value adaptation weights")
        except BaseException:
            self.head.load_state_dict(head)
            self.optimizer.load_state_dict(optimizer)
            raise
        self.state["cursor"] += len(indices)
        if self.state["cursor"] == len(y):
            error = self._validation_mse()
            self.state["epoch"] += 1
            self.state["history"].append(error)
            if error < self.state["best_mse"]:
                self.state["best_mse"] = error
                self.state["best_epoch"] = self.state["epoch"]
                self.best_head = copy.deepcopy(self.head.state_dict())
            self.state["cursor"], self.state["permutation"] = 0, None
            if self.state["epoch"] == self.config.epochs:
                self.state["phase"] = "DONE"

    def advance(self, *, max_decisions=None, max_fit_batches=None, stop_requested=lambda: False):
        decisions = batches = 0
        try:
            while self.state["phase"] != "DONE" and not stop_requested():
                if self.state["phase"] == "FAILED":
                    raise RuntimeError(self.state["error"])
                if self.state["phase"] == "COLLECT":
                    if len(self.state["episodes"]) == self.config.episodes:
                        self.state["phase"] = "FIT"
                        self._save()
                        continue
                    if max_decisions is not None and decisions >= max_decisions:
                        break
                    if self.state["interactions"] >= self.config.max_decisions:
                        self.state.update(phase="FAILED", error="value adaptation decision cap exhausted")
                        raise RuntimeError(self.state["error"])
                    self._collect_one()
                    decisions += 1
                    if self.state["active"] is None or decisions % 64 == 0:
                        self._save()
                else:
                    if max_fit_batches is not None and batches >= max_fit_batches:
                        break
                    transaction = (copy.deepcopy(self.state), copy.deepcopy(self.head.state_dict()),
                                   copy.deepcopy(self.optimizer.state_dict()), copy.deepcopy(self.best_head),
                                   self.generator.get_state())
                    try:
                        self._fit_one()
                    except BaseException:
                        self.state, head, optimizer, self.best_head, rng = transaction
                        self.head.load_state_dict(head)
                        self.optimizer.load_state_dict(optimizer)
                        self.generator.set_state(rng)
                        raise
                    batches += 1
                    self._save()
        except BaseException as error:
            try:
                self._save()
            except Exception as save_error:
                error.add_note(f"Could not save value adaptation progress: {save_error}")
            raise
        self._save()
        return self.report()

    def report(self):
        from collections import Counter
        return {"schema": "sls-value-adaptation-result-v1",
                "status": ("ADAPTED" if self.state["best_epoch"] else "NO_ADAPTATION_GAIN")
                          if self.state["phase"] == "DONE" else self.state["phase"],
                **{key: self.state[key] for key in ("epoch", "best_epoch", "baseline_mse", "best_mse", "history")},
                "decisions": self.state["interactions"], "cached_decisions": self.state["decisions"],
                "complete_episodes": len(self.state["episodes"]),
                "episode_outcomes": dict(Counter(entry["reason"] for entry in self.state["episodes"])),
                "split_episodes": dict(Counter(entry["split"] for entry in self.state["episodes"])),
                "dataset_sha256": hashlib.sha256(str([(e["seed"], e["sha256"], e["split"])
                    for e in self.state["episodes"]]).encode()).hexdigest(),
                "contract": self.contract}

    def apply(self, model):
        if self.report()["status"] != "ADAPTED":
            raise ValueError("value adaptation has no validated gain")
        if state_digest(model.state_dict()) != self.contract["initial_model_sha256"]:
            raise ValueError("value adaptation parent model changed")
        model.value_head.load_state_dict(self.best_head)
