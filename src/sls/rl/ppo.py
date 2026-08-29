"""Variable-candidate PPO for canonical FullRun decisions."""

from __future__ import annotations

from dataclasses import asdict, dataclass
import random
from typing import Mapping, Sequence

import torch
from torch.distributions import Categorical

from sls.contracts import Decision
from sls.model import Policy, PolicyBatch, encode_decision
from sls.rl.episode_limit import (
    EPISODE_LIMIT_SCHEMA, EpisodeLimitState, TERMINATION_REASONS,
)
from sls.rl.rollout import RolloutBatch, generalized_advantage_estimate
from sls.rl.reward import REWARD_SCHEMA, shape_curriculum_reward
from sls.rl.training_contract import native_source_digest
from sls.rl.training_mode import TrainingMode, parse_training_mode
from sls.rl.workers import WorkerPool


def normalize_advantages(advantages: torch.Tensor) -> torch.Tensor:
    """Normalize one fixed rollout without changing its sample ordering."""

    return (advantages - advantages.mean()) / (
        advantages.std(unbiased=False) + 1e-8
    )


def clipped_policy_loss(
    ratios: torch.Tensor, advantages: torch.Tensor, clip_ratio: float,
) -> torch.Tensor:
    """Return the canonical maximization surrogate as a minimization loss."""

    unclipped = ratios * advantages
    clipped = ratios.clamp(1.0 - clip_ratio, 1.0 + clip_ratio) * advantages
    return -torch.minimum(unclipped, clipped).mean()


def policy_distance_metrics(
    old_log_probabilities: torch.Tensor,
    new_log_probabilities: torch.Tensor,
    clip_ratio: float,
) -> tuple[torch.Tensor, torch.Tensor]:
    """Sampled forward-KL estimate and PPO ratio clip fraction."""

    if old_log_probabilities.shape != new_log_probabilities.shape:
        raise ValueError("old and new log probabilities must have the same shape")
    log_ratio = new_log_probabilities - old_log_probabilities
    ratios = torch.exp(log_ratio)
    approximate_kl = (-log_ratio).mean()
    clip_fraction = ((ratios - 1.0).abs() > clip_ratio).to(ratios.dtype).mean()
    return approximate_kl, clip_fraction


@dataclass(frozen=True, slots=True)
class PPOConfig:
    rollout_steps: int = 128
    learning_rate: float = 3e-4
    gamma: float = 0.999
    gae_lambda: float = 0.95
    clip_ratio: float = 0.2
    value_coefficient: float = 0.5
    entropy_coefficient: float = 0.01
    entropy_final: float = 0.001
    entropy_decay_updates: int = 1_000
    target_kl: float = 0.015
    value_clip_ratio: float = 0.2
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
    reservoir_reset_probability: float = 0.5

    def __post_init__(self) -> None:
        if self.rollout_steps <= 0 or self.epochs <= 0 or self.minibatch_size <= 0:
            raise ValueError("PPO sizes must be positive")
        if self.learning_rate <= 0.0:
            raise ValueError("learning_rate must be positive")
        if not 0.0 < self.gamma <= 1.0 or not 0.0 <= self.gae_lambda <= 1.0:
            raise ValueError("invalid discount configuration")
        if not 0.0 < self.clip_ratio < 1.0:
            raise ValueError("clip_ratio must be between zero and one")
        if self.value_coefficient < 0.0 or self.entropy_coefficient < 0.0:
            raise ValueError("PPO loss coefficients cannot be negative")
        if self.max_gradient_norm <= 0.0:
            raise ValueError("max_gradient_norm must be positive")
        if self.potential_scale < 0.0:
            raise ValueError("potential_scale cannot be negative")
        if self.reward_schema != REWARD_SCHEMA:
            raise ValueError(f"unsupported reward schema: {self.reward_schema}")
        if self.episode_limit_schema != EPISODE_LIMIT_SCHEMA:
            raise ValueError(f"unsupported episode limit schema: {self.episode_limit_schema}")
        if not 0.0 <= self.entropy_final <= self.entropy_coefficient:
            raise ValueError("entropy_final must be between zero and entropy_coefficient")
        if self.entropy_decay_updates <= 0 or self.target_kl <= 0.0:
            raise ValueError("entropy decay and target KL must be positive")
        if self.value_clip_ratio <= 0.0:
            raise ValueError("value_clip_ratio must be positive")
        if self.max_episode_steps <= 0 or self.max_boundary_visits <= 0:
            raise ValueError("episode limits must be positive")
        if self.limit_failure_reward >= 0.0:
            raise ValueError("limit_failure_reward must be negative")
        if not 0.0 <= self.reservoir_reset_probability <= 1.0:
            raise ValueError("reservoir_reset_probability must be between zero and one")

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
        checkpoint_reservoir: Sequence[Mapping[str, object]] = (),
        checkpoint_reservoir_digest: str = "NONE",
        training_mode: TrainingMode | str = TrainingMode.EXPERIMENTAL,
        policy_transfer_verified: bool = False,
        git_commit: str = "TEST_OR_UNSPECIFIED",
        training_config_digest: str = "TEST_OR_UNSPECIFIED",
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
        self.checkpoint_reservoir = tuple(checkpoint_reservoir)
        self.checkpoint_reservoir_digest = str(checkpoint_reservoir_digest)
        self.training_mode = parse_training_mode(training_mode)
        self.policy_transfer_verified = bool(policy_transfer_verified)
        if self.training_mode is TrainingMode.PRODUCTION and not self.policy_transfer_verified:
            raise ValueError("production trainer requires verified policy transfer")
        self.git_commit = str(git_commit)
        self.training_config_digest = str(training_config_digest)
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
        encoded_steps = []
        action_steps: list[torch.Tensor] = []
        log_probability_steps: list[torch.Tensor] = []
        value_steps: list[torch.Tensor] = []
        reward_steps: list[torch.Tensor] = []
        terminal_steps: list[torch.Tensor] = []
        collect_terminations = {reason: 0 for reason in TERMINATION_REASONS}
        self.model.eval()
        for _ in range(self.config.rollout_steps):
            encoded = [encode_decision(value) for value in self.decisions]
            batch = PolicyBatch.from_encoded(encoded).to(self.device)
            output = self.model(*batch.model_inputs())
            distribution = Categorical(logits=output.logits)
            actions = distribution.sample()
            candidate_ids = [
                decision.actions[int(index)].candidate_id
                for decision, index in zip(self.decisions, actions.cpu())
            ]
            transitions = self.workers.step(candidate_ids)
            encoded_steps.append(encoded)
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
                    reward = shape_curriculum_reward(
                        reward, current.observation, item.decision.observation,
                        self.workers.profile,
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
                next_decisions[index] = self._reset_worker(index)
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
        flat_encoded = tuple(value for step in encoded_steps for value in step)
        return RolloutBatch(
            flat_encoded,
            torch.stack(action_steps).flatten(),
            torch.stack(log_probability_steps).flatten(),
            values.flatten(),
            advantages.flatten(),
            returns.flatten(),
        )

    def optimize(self, rollout: RolloutBatch) -> dict[str, float]:
        count = len(rollout.encoded_decisions)
        normalized_advantages = normalize_advantages(rollout.advantages)
        totals = {
            "policy": 0.0, "value": 0.0, "entropy": 0.0, "loss": 0.0,
            "approx_kl": 0.0, "gradient_norm": 0.0, "epochs_completed": 0.0,
        }
        updates = 0
        self.model.train()
        entropy_progress = min(1.0, self.update / self.config.entropy_decay_updates)
        entropy_coefficient = (
            self.config.entropy_coefficient
            + entropy_progress * (self.config.entropy_final - self.config.entropy_coefficient)
        )
        epoch_diagnostics: dict[str, float] = {}
        for epoch in range(self.config.epochs):
            indices = list(range(count))
            self.random.shuffle(indices)
            for start in range(0, count, self.config.minibatch_size):
                selected = indices[start:start + self.config.minibatch_size]
                batch = PolicyBatch.from_encoded(
                    rollout.encoded_decisions[index] for index in selected
                ).to(self.device)
                output = self.model(*batch.model_inputs())
                action_indices = rollout.action_indices[selected].to(self.device)
                distribution = Categorical(logits=output.logits)
                log_probabilities = distribution.log_prob(action_indices)
                ratio = torch.exp(
                    log_probabilities - rollout.old_log_probabilities[selected].to(self.device)
                )
                advantage = normalized_advantages[selected].to(self.device)
                policy_loss = clipped_policy_loss(
                    ratio, advantage, self.config.clip_ratio,
                )
                old_values = rollout.old_values[selected].to(self.device)
                returns = rollout.returns[selected].to(self.device)
                clipped_values = old_values + (output.value - old_values).clamp(
                    -self.config.value_clip_ratio, self.config.value_clip_ratio,
                )
                value_loss = 0.5 * torch.maximum(
                    (output.value - returns).square(),
                    (clipped_values - returns).square(),
                ).mean()
                entropy = distribution.entropy().mean()
                loss = (
                    policy_loss
                    + self.config.value_coefficient * value_loss
                    - entropy_coefficient * entropy
                )
                self.optimizer.zero_grad(set_to_none=True)
                loss.backward()
                gradient_norm = torch.nn.utils.clip_grad_norm_(
                    self.model.parameters(), self.config.max_gradient_norm,
                )
                self.optimizer.step()
                approximate_kl, _ = policy_distance_metrics(
                    rollout.old_log_probabilities[selected].to(self.device),
                    log_probabilities,
                    self.config.clip_ratio,
                )
                for key, value in (
                    ("policy", policy_loss), ("value", value_loss),
                    ("entropy", entropy), ("loss", loss),
                    ("approx_kl", approximate_kl),
                    ("gradient_norm", gradient_norm),
                ):
                    totals[key] += float(value.detach())
                updates += 1
            epoch_kl, epoch_clip_fraction = self._policy_diagnostics(rollout)
            totals["epochs_completed"] = float(epoch + 1)
            epoch_diagnostics[f"approx_kl_epoch_{epoch + 1}"] = epoch_kl
            epoch_diagnostics["approx_kl_final"] = epoch_kl
            epoch_diagnostics["clip_fraction"] = epoch_clip_fraction
            if epoch_kl > self.config.target_kl:
                break
        self.update += 1
        result = {
            key: (value if key == "epochs_completed" else value / updates)
            for key, value in totals.items()
        }
        result["entropy_coefficient"] = entropy_coefficient
        result["kl_early_stop"] = float(
            epoch_diagnostics.get("approx_kl_final", 0.0) > self.config.target_kl
        )
        return {**result, **epoch_diagnostics}

    def _reset_worker(self, index: int):  # type: ignore[no-untyped-def]
        if self.checkpoint_reservoir and (
            self.random.random() < self.config.reservoir_reset_probability
        ):
            return self.workers.load_one(
                index, self.random.choice(self.checkpoint_reservoir),
            )
        return self.workers.reset_one(index, self._take_seeds(1)[0])

    @torch.no_grad()
    def _policy_diagnostics(self, rollout: RolloutBatch) -> tuple[float, float]:
        """Measure the current policy on the complete fixed rollout."""

        count = len(rollout.encoded_decisions)
        kl_total = 0.0
        clipped_total = 0.0
        was_training = self.model.training
        self.model.eval()
        for start in range(0, count, self.config.minibatch_size):
            selected = list(range(start, min(count, start + self.config.minibatch_size)))
            batch = PolicyBatch.from_encoded(
                rollout.encoded_decisions[index] for index in selected
            ).to(self.device)
            output = self.model(*batch.model_inputs())
            distribution = Categorical(logits=output.logits)
            actions = rollout.action_indices[selected].to(self.device)
            new_log_probabilities = distribution.log_prob(actions)
            approximate_kl, clip_fraction = policy_distance_metrics(
                rollout.old_log_probabilities[selected].to(self.device),
                new_log_probabilities,
                self.config.clip_ratio,
            )
            weight = len(selected)
            kl_total += float(approximate_kl) * weight
            clipped_total += float(clip_fraction) * weight
        self.model.train(was_training)
        return kl_total / count, clipped_total / count

    def train_update(self) -> dict[str, float]:
        metrics = self.optimize(self.collect())
        metrics.update({f"terminations_{key}": float(value) for key, value in self.last_collect_terminations.items()})
        return metrics
