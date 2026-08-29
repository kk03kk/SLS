"""Variable-candidate PPO for canonical FullRun decisions."""

from __future__ import annotations

import random
from dataclasses import asdict, dataclass

import torch
from torch.distributions import Categorical

from sls.model import Policy, PolicyBatch, encode_decision
from sls.rl.episode_limit import (
    EPISODE_LIMIT_SCHEMA,
    TERMINATION_REASONS,
    EpisodeLimitState,
)
from sls.rl.reward import (
    DEFAULT_FAILURE_PROGRESS_SCALE,
    REWARD_SCHEMA,
    curriculum_terminal_reward,
    shape_curriculum_reward,
)
from sls.rl.rollout import RolloutBatch, generalized_advantage_estimate
from sls.rl.training_contract import native_source_digest
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
    recurrent_sequence_length: int = 32
    minibatch_sequences: int = 16
    learning_rate: float = 2.5e-4
    gamma: float = 1.0
    gae_lambda: float = 0.95
    clip_ratio: float = 0.2
    value_coefficient: float = 0.5
    entropy_coefficient: float = 0.02
    entropy_final: float = 0.002
    entropy_decay_steps: int = 40_000_000
    target_kl: float = 0.02
    value_clip_ratio: float = 0.2
    max_gradient_norm: float = 0.5
    epochs: int = 2
    potential_shaping: bool = True
    potential_scale: float = 0.2
    failure_progress_scale: float = DEFAULT_FAILURE_PROGRESS_SCALE
    reward_schema: str = REWARD_SCHEMA
    episode_limit_schema: str = EPISODE_LIMIT_SCHEMA
    max_episode_steps: int = 4_096
    max_boundary_visits: int = 4
    limit_failure_reward: float = -1.0

    def __post_init__(self) -> None:
        if (
            self.rollout_steps <= 0
            or self.epochs <= 0
            or self.recurrent_sequence_length <= 0
            or self.minibatch_sequences <= 0
        ):
            raise ValueError("PPO sizes must be positive")
        if self.rollout_steps % self.recurrent_sequence_length:
            raise ValueError("rollout_steps must be divisible by recurrent_sequence_length")
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
        if not 0.0 < self.failure_progress_scale < 1.0:
            raise ValueError("failure_progress_scale must be between zero and one")
        if self.reward_schema != REWARD_SCHEMA:
            raise ValueError(f"unsupported reward schema: {self.reward_schema}")
        if self.episode_limit_schema != EPISODE_LIMIT_SCHEMA:
            raise ValueError(f"unsupported episode limit schema: {self.episode_limit_schema}")
        if not 0.0 <= self.entropy_final <= self.entropy_coefficient:
            raise ValueError("entropy_final must be between zero and entropy_coefficient")
        if self.entropy_decay_steps <= 0 or self.target_kl <= 0.0:
            raise ValueError("entropy decay and target KL must be positive")
        if self.value_clip_ratio <= 0.0:
            raise ValueError("value_clip_ratio must be positive")
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
        native_contract_digest: str | None = None,
        git_commit: str = "TEST_OR_UNSPECIFIED",
        training_config_digest: str = "TEST_OR_UNSPECIFIED",
        training_seed_limit: int | None = None,
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
        self.environment_steps = 0
        self.native_contract_digest = native_contract_digest or native_source_digest()
        self.git_commit = str(git_commit)
        self.training_config_digest = str(training_config_digest)
        self.training_seed_limit = training_seed_limit
        self.decisions = workers.reset(self._take_seeds(workers.size))
        self.memory = self.model.initial_memory(workers.size, self.device)
        self.episode_starts = torch.ones(workers.size, dtype=torch.bool, device=self.device)
        self.episode_limits = [EpisodeLimitState.initial(item) for item in self.decisions]
        self.termination_counts = {reason: 0 for reason in TERMINATION_REASONS}
        self.last_collect_terminations = {reason: 0 for reason in TERMINATION_REASONS}

    def _take_seeds(self, count: int) -> list[int]:
        if (
            self.training_seed_limit is not None
            and self.next_seed + count > self.training_seed_limit
        ):
            raise RuntimeError("training seed namespace reached held-out evaluation seeds")
        result = list(range(self.next_seed, self.next_seed + count))
        self.next_seed += count
        return result

    @torch.no_grad()
    def collect(self) -> RolloutBatch:
        encoded_steps: list[tuple[object, ...]] = []
        action_steps: list[torch.Tensor] = []
        log_probability_steps: list[torch.Tensor] = []
        value_steps: list[torch.Tensor] = []
        reward_steps: list[torch.Tensor] = []
        terminal_steps: list[torch.Tensor] = []
        episode_start_steps: list[torch.Tensor] = []
        memory_steps: list[torch.Tensor] = []
        collect_terminations = {reason: 0 for reason in TERMINATION_REASONS}
        self.model.eval()
        for _ in range(self.config.rollout_steps):
            encoded = tuple(encode_decision(value) for value in self.decisions)
            batch = PolicyBatch.from_encoded(encoded).to(self.device)
            episode_start_steps.append(self.episode_starts.cpu())
            memory_steps.append(self.memory.cpu())
            output = self.model(
                *batch.model_inputs(),
                memory=self.memory,
                episode_start_mask=self.episode_starts,
            )
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
                    success = bool(item.info.get("success"))
                    reason = "success" if success else "death"
                    reward = curriculum_terminal_reward(
                        item.decision.observation,
                        self.workers.profile,
                        success=success,
                        failure_progress_scale=self.config.failure_progress_scale,
                    )
                elif item.truncated:
                    reason = "backend_truncated"
                    reward = self.config.limit_failure_reward
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
            next_memory = output.next_memory.detach()
            next_episode_starts = torch.zeros(
                self.workers.size, dtype=torch.bool, device=self.device,
            )
            for index in reset_indices:
                next_decisions[index] = self._reset_worker(index)
                self.episode_limits[index] = EpisodeLimitState.initial(next_decisions[index])
                next_memory[index].zero_()
                next_episode_starts[index] = True
                self.episodes += 1
            self.decisions = next_decisions
            self.memory = next_memory
            self.episode_starts = next_episode_starts

        self.last_collect_terminations = collect_terminations
        self.environment_steps += self.config.rollout_steps * self.workers.size

        bootstrap_batch = PolicyBatch.from_decisions(self.decisions, self.model.config).to(self.device)
        bootstrap = self.model(
            *bootstrap_batch.model_inputs(),
            memory=self.memory,
            episode_start_mask=self.episode_starts,
        ).value.cpu()
        values = torch.stack(value_steps)
        advantages, returns = generalized_advantage_estimate(
            torch.stack(reward_steps),
            values,
            torch.stack(terminal_steps),
            bootstrap,
            self.config.gamma,
            self.config.gae_lambda,
        )
        return RolloutBatch(
            tuple(encoded_steps),
            torch.stack(action_steps),
            torch.stack(log_probability_steps),
            values,
            advantages,
            returns,
            torch.stack(episode_start_steps),
            torch.stack(memory_steps),
        )

    def optimize(self, rollout: RolloutBatch) -> dict[str, float]:
        chunks = self._sequence_chunks(rollout)
        normalized_advantages = normalize_advantages(rollout.advantages)
        totals = {
            "policy": 0.0, "value": 0.0, "entropy": 0.0, "loss": 0.0,
            "approx_kl": 0.0, "gradient_norm": 0.0, "epochs_completed": 0.0,
        }
        updates = 0
        self.model.train()
        entropy_progress = min(
            1.0, self.environment_steps / self.config.entropy_decay_steps,
        )
        entropy_coefficient = (
            self.config.entropy_coefficient
            + entropy_progress * (self.config.entropy_final - self.config.entropy_coefficient)
        )
        epoch_diagnostics: dict[str, float] = {}
        for epoch in range(self.config.epochs):
            self.random.shuffle(chunks)
            for start in range(0, len(chunks), self.config.minibatch_sequences):
                selected = chunks[start:start + self.config.minibatch_sequences]
                log_probabilities, values, entropy_values = self._evaluate_sequences(
                    rollout, selected,
                )
                old_log_probabilities = self._select_sequences(
                    rollout.old_log_probabilities, selected,
                ).to(self.device)
                ratio = torch.exp(
                    log_probabilities - old_log_probabilities
                )
                advantage = self._select_sequences(
                    normalized_advantages, selected,
                ).to(self.device)
                policy_loss = clipped_policy_loss(
                    ratio, advantage, self.config.clip_ratio,
                )
                old_values = self._select_sequences(
                    rollout.old_values, selected,
                ).to(self.device)
                returns = self._select_sequences(rollout.returns, selected).to(self.device)
                clipped_values = old_values + (values - old_values).clamp(
                    -self.config.value_clip_ratio, self.config.value_clip_ratio,
                )
                value_loss = 0.5 * torch.maximum(
                    (values - returns).square(),
                    (clipped_values - returns).square(),
                ).mean()
                entropy = entropy_values.mean()
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
                    old_log_probabilities, log_probabilities, self.config.clip_ratio,
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

    def _sequence_chunks(self, rollout: RolloutBatch) -> list[tuple[int, int]]:
        time_steps, environments = rollout.shape
        length = self.config.recurrent_sequence_length
        if time_steps % length:
            raise ValueError("rollout time dimension is not sequence aligned")
        return [
            (time, environment)
            for environment in range(environments)
            for time in range(0, time_steps, length)
        ]

    def _select_sequences(
        self,
        tensor: torch.Tensor,
        chunks: list[tuple[int, int]],
    ) -> torch.Tensor:
        length = self.config.recurrent_sequence_length
        return torch.stack([
            tensor[start:start + length, environment]
            for start, environment in chunks
        ]).flatten()

    def _evaluate_sequences(
        self,
        rollout: RolloutBatch,
        chunks: list[tuple[int, int]],
    ) -> tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
        memory = torch.stack([
            rollout.input_memories[start, environment]
            for start, environment in chunks
        ]).to(self.device)
        log_probabilities: list[torch.Tensor] = []
        values: list[torch.Tensor] = []
        entropies: list[torch.Tensor] = []
        for offset in range(self.config.recurrent_sequence_length):
            encoded = (
                rollout.encoded_decisions[start + offset][environment]
                for start, environment in chunks
            )
            batch = PolicyBatch.from_encoded(encoded).to(self.device)
            starts = torch.tensor([
                bool(rollout.episode_starts[start + offset, environment])
                for start, environment in chunks
            ], dtype=torch.bool, device=self.device)
            output = self.model(
                *batch.model_inputs(), memory=memory, episode_start_mask=starts,
            )
            memory = output.next_memory
            distribution = Categorical(logits=output.logits)
            actions = torch.tensor([
                int(rollout.action_indices[start + offset, environment])
                for start, environment in chunks
            ], dtype=torch.long, device=self.device)
            log_probabilities.append(distribution.log_prob(actions))
            values.append(output.value)
            entropies.append(distribution.entropy())
        return tuple(
            torch.stack(items, dim=1).flatten()
            for items in (log_probabilities, values, entropies)
        )  # type: ignore[return-value]

    def _reset_worker(self, index: int):  # type: ignore[no-untyped-def]
        return self.workers.reset_one(index, self._take_seeds(1)[0])

    @torch.no_grad()
    def _policy_diagnostics(self, rollout: RolloutBatch) -> tuple[float, float]:
        """Measure the current policy on the complete fixed rollout."""

        chunks = self._sequence_chunks(rollout)
        count = rollout.action_indices.numel()
        kl_total = 0.0
        clipped_total = 0.0
        was_training = self.model.training
        self.model.eval()
        for start in range(0, len(chunks), self.config.minibatch_sequences):
            selected = chunks[start:start + self.config.minibatch_sequences]
            new_log_probabilities, _, _ = self._evaluate_sequences(rollout, selected)
            old_log_probabilities = self._select_sequences(
                rollout.old_log_probabilities, selected,
            ).to(self.device)
            approximate_kl, clip_fraction = policy_distance_metrics(
                old_log_probabilities, new_log_probabilities, self.config.clip_ratio,
            )
            weight = len(new_log_probabilities)
            kl_total += float(approximate_kl) * weight
            clipped_total += float(clip_fraction) * weight
        self.model.train(was_training)
        return kl_total / count, clipped_total / count

    def train_update(self) -> dict[str, float]:
        metrics = self.optimize(self.collect())
        metrics.update({f"terminations_{key}": float(value) for key, value in self.last_collect_terminations.items()})
        return metrics
