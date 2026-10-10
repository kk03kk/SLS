"""Natural, training-only anchors and current-policy public-prefix reconstruction."""
from __future__ import annotations

import gzip
import json
import random
import time
from dataclasses import dataclass
from pathlib import Path

import torch

from sls.contracts import Action
from sls.diagnostics.cpu import decision_from_record, decision_record
from sls.model import PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.research.protocol import NATIVE_SHA256, PARENT_SHA256, RANGES, STRATA, digest
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.training_contract import native_source_digest, sha256_file

BANK_SCHEMA = "sls-act2-training-bank-v1"


@torch.no_grad()
def score(model, decision, memory, previous_action=0, previous_reward=0., start=False):
    device = next(model.parameters()).device
    batch = PolicyBatch.from_decisions([decision], model.config).to(device)
    was_training = model.training
    model.eval()
    try:
        return model(*batch.model_inputs(), memory=memory,
                     episode_start_mask=torch.tensor([start], device=device),
                     previous_action_types=torch.tensor([previous_action], device=device),
                     previous_rewards=torch.tensor([previous_reward], dtype=torch.float32, device=device))
    finally:
        model.train(was_training)


def rebuild(model, rows):
    """Teacher actions are inputs only, never PPO samples; private state is absent."""
    memory = model.initial_memory(1, next(model.parameters()).device)
    previous_action, previous_reward = 0, 0.
    for i, row in enumerate(rows):
        if row.get("terminal"):
            raise ValueError("prefix crosses a termination")
        decision = decision_from_record(row)
        action = Action.from_dict(row["chosen_action"])
        if action not in decision.actions:
            raise ValueError("illegal prefix action")
        output = score(model, decision, memory, previous_action, previous_reward, i == 0)
        memory = output.next_memory.detach()
        previous_action = ACTION_TYPE_IDS[action.kind.value] + 1
        previous_reward = float(torch.tensor(row["raw_reward"], dtype=torch.float32))
    return memory, previous_action, previous_reward


def write_gzip(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("xb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as stream:
        stream.write(json.dumps(value, sort_keys=True, allow_nan=False).encode())


def read_checked(root, name, sha):
    path = (root / name).resolve()
    if not path.is_relative_to(root.resolve()) or sha256_file(path) != sha:
        raise ValueError("bank path or artifact digest mismatch")
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        return json.load(stream)


def coverage(anchors):
    seeds = {s: sorted({a["seed"] for a in anchors if a["stratum"] == s}) for s in STRATA}
    bosses = sorted({a["boss"] for a in anchors if a["stratum"] == "boss"})
    return {"seeds_per_stratum": {s: len(v) for s, v in seeds.items()}, "bosses": bosses,
            "qualified": all(len(v) >= 32 for v in seeds.values()) and len(bosses) == 3}


class NaturalBank:
    def __init__(self, root, *, require_qualified=True):
        self.root = Path(root)
        self.manifest = json.loads((self.root / "manifest.json").read_text(encoding="utf-8"))
        m = self.manifest
        if (m.get("schema") != BANK_SCHEMA or m.get("role") != "training-only"
                or m.get("native_sha256") != native_source_digest()
                or m.get("native_sha256") != NATIVE_SHA256
                or m.get("teacher_sha256") != PARENT_SHA256
                or m.get("seed_range") != list(RANGES["bank"])):
            raise ValueError("wrong training bank identity or seed role")
        anchors = m["anchors"]
        if len({(a["seed"], a["stratum"]) for a in anchors}) != len(anchors):
            raise ValueError("duplicate seed-stratum anchor")
        for a in anchors:
            if a["stratum"] not in STRATA or not RANGES["bank"][0] <= a["seed"] < RANGES["bank"][1]:
                raise ValueError("invalid anchor seed or stratum")
            self.payload(a)
        if coverage(anchors) != m["coverage"] or require_qualified and not m["coverage"]["qualified"]:
            raise ValueError("natural-bank coverage gate failed")
        if require_qualified:
            trajectories = m.get("trajectories", [])
            if sorted(t["seed"] for t in trajectories) != list(range(*RANGES["bank"])):
                raise ValueError("training bank did not collect the exact 2048 registered seeds")
            total = 0
            for trajectory in trajectories:
                payload = read_checked(self.root, trajectory["path"], trajectory["sha256"])
                if len(payload["rows"]) != trajectory["steps"] or not payload["rows"][-1]["terminal"]:
                    raise ValueError("incomplete teacher trajectory")
                total += trajectory["steps"]
            if total != m["teacher_decisions"]:
                raise ValueError("teacher compute accounting mismatch")
        self.identity = sha256_file(self.root / "manifest.json")
        self.groups = {s: sorted([a for a in anchors if a["stratum"] == s], key=lambda a: a["seed"])
                       for s in STRATA}

    def payload(self, anchor):
        public = read_checked(self.root, anchor["public_path"], anchor["public_sha256"])
        private = read_checked(self.root, anchor["private_path"], anchor["private_sha256"])
        # Native data can only reach load_one; the model sees the public prefix.
        if set(public) != {"prefix", "boundary"} or set(private) != {"native", "limits"}:
            raise ValueError("bank public/private envelope mismatch")
        limits = EpisodeLimitState.from_dict(private["limits"])
        if limits.steps != len(public["prefix"]) or anchor["step"] != len(public["prefix"]) or not limits.visits:
            raise ValueError("bank limiter does not preserve complete prefix")
        if digest(public["boundary"]) != anchor["boundary_sha256"]:
            raise ValueError("bank public boundary mismatch")
        return public, private

    def qualify(self, *, stop_requested=lambda: False):
        """Independently replay every anchor's native/public/limiter contract."""
        from sls.backends.simulator import SimulatorBackend
        from sls.curriculum import IRONCLAD_A20_ACT2

        begin = time.perf_counter()
        replayed = 0
        for anchor in self.manifest["anchors"]:
            public, private = self.payload(anchor)
            backend = SimulatorBackend(IRONCLAD_A20_ACT2)
            decision = backend.reset(anchor["seed"])
            limits = EpisodeLimitState.initial(decision)
            for row in public["prefix"]:
                if stop_requested():
                    raise InterruptedError("bank qualification interrupted")
                if decision_record(decision) != {k: row[k] for k in ("observation", "actions")}:
                    raise ValueError("bank prefix differs from natural native replay")
                transition = backend.step(Action.from_dict(row["chosen_action"]))
                if transition.terminated or transition.truncated or float(transition.reward) != row["raw_reward"]:
                    raise ValueError("invalid bank prefix reward/termination")
                decision = transition.decision
                if limits.observe(decision, max_steps=4096, max_boundary_visits=4):
                    raise ValueError("bank prefix exhausted historical limiter")
                replayed += 1
            if (decision_record(decision) != public["boundary"] or limits.to_dict() != private["limits"]
                    or digest(backend.checkpoint()) != digest(private["native"])):
                raise ValueError("bank boundary/native/history limiter mismatch")
        return {"bank_sha256": self.identity, "anchors": len(self.manifest["anchors"]),
                "native_replay_decisions": replayed, "seconds": time.perf_counter() - begin,
                "passed": True}


@dataclass
class EpisodeInitialization:
    decision: object
    memory: torch.Tensor
    previous_action: int
    previous_reward: float
    limits: EpisodeLimitState
    source: dict
    prefix: list
    start: bool = False


class NaturalInitializer:
    """Uniform stratum, then uniform seed; episode-level independent coin flips."""
    def __init__(self, bank, *, probability=.25, seed=0):
        if not 0 <= probability <= 1:
            raise ValueError("invalid episode reset probability")
        self.bank, self.probability = bank, probability
        self.rng = random.Random(seed)
        self.cache_version = None
        self.cache = {}
        self.counts = {"normal": 0, "bank": 0}
        self.phase_counts = {"warmup_normal": 0, "ppo_normal": 0, "ppo_bank": 0}
        self.prefix_seconds = 0.
        self.prefix_decisions = 0

    def state_dict(self):
        return {"bank_sha256": self.bank.identity, "probability": self.probability,
                "rng": self.rng.getstate(), "counts": dict(self.counts),
                "phase_counts": dict(self.phase_counts),
                "prefix_seconds": self.prefix_seconds, "prefix_decisions": self.prefix_decisions}

    def load_state_dict(self, state):
        if state["bank_sha256"] != self.bank.identity or state["probability"] != self.probability:
            raise ValueError("research sampler identity mismatch")
        self.rng.setstate(state["rng"])
        self.counts = dict(state["counts"])
        self.phase_counts = dict(state["phase_counts"])
        self.prefix_seconds = state["prefix_seconds"]
        self.prefix_decisions = state["prefix_decisions"]
        self.cache.clear()
        self.cache_version = None

    def record_initial(self, count, *, warmup):
        self.counts["normal"] += count
        self.phase_counts["warmup_normal" if warmup else "ppo_normal"] += count

    def reset_many(self, trainer, indices):
        if self.cache_version != trainer.update:
            self.cache.clear()
            self.cache_version = trainer.update
        warm = trainer.critic_warmup.active
        choices = [None if warm or self.rng.random() >= self.probability else
                   self.rng.choice(self.bank.groups[self.rng.choice(STRATA)]) for _ in indices]
        normal = [i for i, a in zip(indices, choices) if a is None]
        decisions = dict(zip(normal, trainer.workers.reset_many(normal, trainer._take_seeds(len(normal)))))
        result = []
        for index, anchor in zip(indices, choices):
            if anchor is None:
                decision = decisions[index]
                result.append(EpisodeInitialization(decision, trainer.model.initial_memory(1, trainer.device),
                              0, 0., EpisodeLimitState.initial(decision), {"kind": "normal"}, [], True))
                self.counts["normal"] += 1
                self.phase_counts["warmup_normal" if warm else "ppo_normal"] += 1
                continue
            public, private = self.bank.payload(anchor)
            decision = trainer.workers.load_one(index, private["native"])
            if decision_record(decision) != public["boundary"]:
                raise ValueError("restored curriculum boundary does not match public history")
            begin = time.perf_counter()
            key = anchor["id"]
            if key not in self.cache:
                self.cache[key] = rebuild(trainer.model, public["prefix"])
                self.prefix_decisions += len(public["prefix"])
            memory, previous_action, previous_reward = self.cache[key]
            self.prefix_seconds += time.perf_counter() - begin
            limits = EpisodeLimitState.from_dict(private["limits"])
            if limits.steps >= trainer.config.max_episode_steps or max(limits.visits.values()) > trainer.config.max_boundary_visits:
                raise ValueError("curriculum reset resurrects an exhausted episode")
            result.append(EpisodeInitialization(decision, memory.clone(), previous_action, previous_reward,
                          limits, {"kind": "bank", "bank_sha256": self.bank.identity, "anchor": key},
                          public["prefix"], len(public["prefix"]) == 0))
            self.counts["bank"] += 1
            self.phase_counts["ppo_bank"] += 1
        return result
