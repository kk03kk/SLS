"""Deterministic FullRun policy evaluation."""

from __future__ import annotations

from dataclasses import dataclass

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import CurriculumProfile
from sls.model import Policy, PolicyBatch


@dataclass(frozen=True, slots=True)
class EvaluationResult:
    episodes: int
    successes: int
    mean_reward: float
    mean_steps: float


@torch.no_grad()
def evaluate(
    model: Policy,
    profile: CurriculumProfile,
    seeds: tuple[int, ...],
    *,
    device: str | torch.device = "cpu",
    max_steps: int = 100_000,
) -> EvaluationResult:
    model.eval().to(device)
    successes = 0
    rewards = 0.0
    steps = 0
    for seed in seeds:
        backend = SimulatorBackend(profile)
        decision = backend.reset(seed)
        episode_reward = 0.0
        for _ in range(max_steps):
            batch = PolicyBatch.from_decisions((decision,), model.config).to(device)
            output = model(*batch.model_inputs())
            action = decision.actions[int(output.logits[0].argmax())]
            transition = backend.step(action)
            episode_reward += transition.reward
            steps += 1
            decision = transition.decision
            if transition.terminated or transition.truncated:
                successes += int(bool(transition.info.get("success")))
                break
        else:
            raise RuntimeError(f"evaluation seed {seed} exceeded {max_steps} steps")
        rewards += episode_reward
    count = len(seeds)
    return EvaluationResult(count, successes, rewards / count, steps / count)
