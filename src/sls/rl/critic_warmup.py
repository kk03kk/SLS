"""Pure-Python phase and complete-return bookkeeping; no model runtime."""
from __future__ import annotations

import math
from dataclasses import asdict, dataclass

WARMUP_CHECKPOINT_SCHEMA = "sls-full-run-ppo-v6"


@dataclass(frozen=True)
class CriticWarmupConfig:
    rollout_updates: int = 0
    epochs: int = 2
    batch_size: int = 1024

    def __post_init__(self):
        for name in ("rollout_updates", "epochs", "batch_size"):
            value = getattr(self, name)
            if type(value) is not int or value < (0 if name == "rollout_updates" else 1):
                raise ValueError(f"invalid critic warmup {name}")

    def to_dict(self):
        return asdict(self)


class CriticWarmupState:
    def __init__(self, config: CriticWarmupConfig, workers: int, max_steps: int):
        self.config = config
        self.workers = workers
        self.max_steps = max_steps
        self.completed_updates = 0
        self.pending = [[] for _ in range(workers)]
        self.discarded_states = 0
        self.completed_episodes = 0

    @property
    def active(self):
        return self.completed_updates < self.config.rollout_updates

    def observe(self, worker, feature, reward, terminal, *, backend_fault=False):
        if not self.active or not 0 <= worker < self.workers or not math.isfinite(reward):
            raise ValueError("invalid warmup observation")
        if backend_fault:
            raise RuntimeError("backend fault is not a critic training target")
        pending = self.pending[worker]
        pending.append((feature, float(reward)))
        if len(pending) > self.max_steps:
            raise ValueError("warmup episode exceeds limit")
        if not terminal:
            return []
        total = 0.0
        samples = []
        for hidden, r in reversed(pending):
            total += r
            samples.append((hidden, total))
        self.pending[worker] = []
        self.completed_episodes += 1
        return list(reversed(samples))

    def finish_update(self):
        if not self.active:
            raise ValueError("warmup already completed")
        self.completed_updates += 1
        if not self.active:
            self.discarded_states += sum(map(len, self.pending))
            self.pending = [[] for _ in range(self.workers)]

    def state_dict(self):
        return {"schema": "sls-critic-warmup-state-v1",
                "phase": "CRITIC_WARMUP" if self.active else "PPO",
                "config": self.config.to_dict(), "workers": self.workers,
                "max_steps": self.max_steps, "completed_updates": self.completed_updates,
                "pending": self.pending, "discarded_states": self.discarded_states,
                "completed_episodes": self.completed_episodes}

    def load_state_dict(self, state):
        if (state.get("schema") != "sls-critic-warmup-state-v1"
                or state.get("config") != self.config.to_dict()
                or state.get("workers") != self.workers or state.get("max_steps") != self.max_steps):
            raise ValueError("warmup state contract mismatch")
        count = state["completed_updates"]
        if type(count) is not int or not 0 <= count <= self.config.rollout_updates:
            raise ValueError("invalid warmup update cursor")
        pending = state["pending"]
        if not isinstance(pending, list) or len(pending) != self.workers:
            raise ValueError("invalid warmup worker buffers")
        for buffer in pending:
            if not isinstance(buffer, list) or len(buffer) > self.max_steps:
                raise ValueError("invalid warmup episode buffer")
            for item in buffer:
                if len(item) != 2 or not math.isfinite(item[1]):
                    raise ValueError("invalid warmup reward")
        if count == self.config.rollout_updates and any(pending):
            raise ValueError("PPO phase contains unfinished warmup targets")
        expected = "CRITIC_WARMUP" if count < self.config.rollout_updates else "PPO"
        if state.get("phase") != expected:
            raise ValueError("warmup phase/cursor mismatch")
        for key in ("discarded_states", "completed_episodes"):
            if type(state[key]) is not int or state[key] < 0:
                raise ValueError("invalid warmup counters")
        self.completed_updates = count
        self.pending = [list(buffer) for buffer in pending]
        self.discarded_states = state["discarded_states"]
        self.completed_episodes = state["completed_episodes"]
