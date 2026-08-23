"""Variable-candidate PPO for canonical FullRun decisions."""

from __future__ import annotations

from dataclasses import asdict, dataclass
import random
from typing import Sequence

import torch
from torch.distributions import Categorical

from sls.contracts import Decision
from sls.model import Policy, PolicyBatch
from sls.rl.episode_limit import (
    EPISODE_LIMIT_SCHEMA, EpisodeLimitState, TERMINATION_REASONS,
)
from sls.rl.rollout import RolloutBatch, generalized_advantage_estimate
from sls.rl.reward import REWARD_SCHEMA, shape_act_one_reward
from sls.rl.training_contract import native_source_digest
from sls.rl.workers import WorkerPool


@dataclass(frozen=True, slots=True)
class PPOConfig:
    rollout_steps: int = 128
    learning_rate: float = 3e-4
    gamma: float = 0.99
    gae_lambda: float = 0.95
    clip_ratio: float = 0.2
    value_coefficient: float = 0.5
    entropy_coefficient: float = 0.01
    max_gradient_norm: float = 0.5
    epochs: int = 4
    minibatch_size: int = 256
    potential_shaping: bool = False
    potential_scale: float = 0.2
    reward_schema: str = REWARD_SCHEMA
    episode_limit_schema: str = EPISODE_LIMIT_SCHEMA
    max_episode_steps: int = 512
    max_boundary_visits: int = 4
    limit_failure_reward: float = -1.0

    def __post_init__(self) -> None:
        if self.rollout_steps <= 0 or self.epochs <= 0 or self.minibatch_size <= 0:
            raise ValueError("PPO sizes must be positive")
        if not 0.0 < self.gamma <= 1.0 or not 0.0 <= self.gae_lambda <= 1.0:
            raise ValueError("invalid discount configuration")
        if self.potential_scale < 0.0:
            raise ValueError("potential_scale cannot be negative")
        if self.reward_schema != REWARD_SCHEMA:
            raise ValueError(f"unsupported reward schema: {self.reward_schema}")
        if self.episode_limit_schema != EPISODE_LIMIT_SCHEMA:
            raise ValueError(f"unsupported episode limit schema: {self.episode_limit_schema}")
        if self.max_episode_steps <= 0 or self.max_boundary_visits <= 0:
            raise ValueError("episode limits must be positive")
        if self.limit_failure_reward >= 0.0:
            raise ValueError("limit_failure_reward must be negative")

    def to_dict(self) -> dict[str, int | float | bool | str]:
        return asdict(self)


class PPOTrainer:
    def __init__(
        self,
        model: Policy,
        workers: WorkerPool,
        config: PPOConfig = PPOConfig(),
        *,
        device: str | torch.device = "cpu",
        seed: int = 0,
        readiness_lock_digest: str = "UNVERIFIED",
        native_contract_digest: str | None = None,
    ) -> None:
        self.model = model.to(device)
        self.workers = workers
        self.config = config
        self.device = torch.device(device)
        self.optimizer = torch.optim.Adam(self.model.parameters(), lr=config.learning_rate)
        self.random = random.Random(seed)
        self.next_seed = int(seed)
        self.update = 0
        self.episodes = 0
        self.readiness_lock_digest = str(readiness_lock_digest)
        self.native_contract_digest = native_contract_digest or native_source_digest()
        self.decisions = workers.reset(self._take_seeds(workers.size))
        self.episode_limits = [EpisodeLimitState.initial(item) for item in self.decisions]
        self.termination_counts = {reason: 0 for reason in TERMINATION_REASONS}
        self.last_collect_terminations = {reason: 0 for reason in TERMINATION_REASONS}

    def _take_seeds(self, count: int) -> list[int]:
        result = list(range(self.next_seed, self.next_seed + count))
        self.next_seed += count
        return result

    @torch.no_grad()
    def collect(self) -> RolloutBatch:
        decision_steps: list[list[Decision]] = []
        action_steps: list[torch.Tensor] = []
        log_probability_steps: list[torch.Tensor] = []
        value_steps: list[torch.Tensor] = []
        reward_steps: list[torch.Tensor] = []
        terminal_steps: list[torch.Tensor] = []
        collect_terminations = {reason: 0 for reason in TERMINATION_REASONS}
        self.model.eval()
        for _ in range(self.config.rollout_steps):
            batch = PolicyBatch.from_decisions(self.decisions, self.model.config).to(self.device)
            output = self.model(*batch.model_inputs())
            distribution = Categorical(logits=output.logits)
            actions = distribution.sample()
            candidate_ids = [
                decision.actions[int(index)].candidate_id
                for decision, index in zip(self.decisions, actions.cpu())
            ]
            transitions = self.workers.step(candidate_ids)
            decision_steps.append(list(self.decisions))
            action_steps.append(actions.cpu())
            log_probability_steps.append(distribution.log_prob(actions).cpu())
            value_steps.append(output.value.cpu())
            rewards = []
            terminals = []
            next_decisions = [item.decision for item in transitions]
            reset_indices: list[int] = []
            for index, (current, item) in enumerate(zip(self.decisions, transitions)):
                reason: str | None = None
                terminal = bool(item.terminated or item.truncated)
                reward = float(item.reward)
                if item.terminated:
                    reason = "success" if bool(item.info.get("success")) else "death"
                elif item.truncated:
                    reason = "backend_truncated"
                else:
                    reason = self.episode_limits[index].observe(
                        item.decision,
                        max_steps=self.config.max_episode_steps,
                        max_boundary_visits=self.config.max_boundary_visits,
                    )
                    if reason is not None:
                        terminal = True
                        reward = self.config.limit_failure_reward
                if self.config.potential_shaping:
                    reward = shape_act_one_reward(
                        reward, current.observation, item.decision.observation,
                        gamma=self.config.gamma, scale=self.config.potential_scale,
                        terminal=terminal,
                    )
                rewards.append(reward)
                terminals.append(terminal)
                if reason is not None:
                    collect_terminations[reason] += 1
                    self.termination_counts[reason] += 1
                    reset_indices.append(index)
            reward_steps.append(torch.tensor(rewards, dtype=torch.float32))
            terminal_steps.append(torch.tensor(terminals, dtype=torch.bool))
            for index in reset_indices:
                next_decisions[index] = self.workers.reset_one(index, self._take_seeds(1)[0])
                self.episode_limits[index] = EpisodeLimitState.initial(next_decisions[index])
                self.episodes += 1
            self.decisions = next_decisions

        self.last_collect_terminations = collect_terminations

        bootstrap_batch = PolicyBatch.from_decisions(self.decisions, self.model.config).to(self.device)
        bootstrap = self.model(*bootstrap_batch.model_inputs()).value.cpu()
        values = torch.stack(value_steps)
        advantages, returns = generalized_advantage_estimate(
            torch.stack(reward_steps),
            values,
            torch.stack(terminal_steps),
            bootstrap,
            self.config.gamma,
            self.config.gae_lambda,
        )
        flat_decisions = tuple(value for step in decision_steps for value in step)
        return RolloutBatch(
            flat_decisions,
            torch.stack(action_steps).flatten(),
            torch.stack(log_probability_steps).flatten(),
            values.flatten(),
            advantages.flatten(),
            returns.flatten(),
        )

    def optimize(self, rollout: RolloutBatch) -> dict[str, float]:
        count = len(rollout.decisions)
        normalized_advantages = (
            rollout.advantages - rollout.advantages.mean()
        ) / (rollout.advantages.std(unbiased=False) + 1e-8)
        totals = {
            "policy": 0.0, "value": 0.0, "entropy": 0.0, "loss": 0.0,
            "approx_kl": 0.0, "gradient_norm": 0.0,
        }
        updates = 0
        self.model.train()
        for _ in range(self.config.epochs):
            indices = list(range(count))
            self.random.shuffle(indices)
            for start in range(0, count, self.config.minibatch_size):
                selected = indices[start:start + self.config.minibatch_size]
                batch = PolicyBatch.from_decisions(
                    (rollout.decisions[index] for index in selected), self.model.config,
                ).to(self.device)
                output = self.model(*batch.model_inputs())
                action_indices = rollout.action_indices[selected].to(self.device)
                distribution = Categorical(logits=output.logits)
                log_probabilities = distribution.log_prob(action_indices)
                ratio = torch.exp(
                    log_probabilities - rollout.old_log_probabilities[selected].to(self.device)
                )
                advantage = normalized_advantages[selected].to(self.device)
                policy_loss = -torch.minimum(
                    ratio * advantage,
                    ratio.clamp(1.0 - self.config.clip_ratio, 1.0 + self.config.clip_ratio) * advantage,
                ).mean()
                value_loss = torch.nn.functional.mse_loss(
                    output.value, rollout.returns[selected].to(self.device),
                )
                entropy = distribution.entropy().mean()
                loss = (
                    policy_loss
                    + self.config.value_coefficient * value_loss
                    - self.config.entropy_coefficient * entropy
                )
                self.optimizer.zero_grad(set_to_none=True)
                loss.backward()
                gradient_norm = torch.nn.utils.clip_grad_norm_(
                    self.model.parameters(), self.config.max_gradient_norm,
                )
                self.optimizer.step()
                approximate_kl = (
                    rollout.old_log_probabilities[selected].to(self.device)
                    - log_probabilities
                ).mean()
                for key, value in (
                    ("policy", policy_loss), ("value", value_loss),
                    ("entropy", entropy), ("loss", loss),
                    ("approx_kl", approximate_kl),
                    ("gradient_norm", gradient_norm),
                ):
                    totals[key] += float(value.detach())
                updates += 1
        self.update += 1
        return {key: value / updates for key, value in totals.items()}

    def train_update(self) -> dict[str, float]:
        metrics = self.optimize(self.collect())
        metrics.update({f"terminations_{key}": float(value) for key, value in self.last_collect_terminations.items()})
        return metrics
