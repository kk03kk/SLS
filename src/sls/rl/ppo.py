"""Variable-candidate PPO for canonical FullRun decisions."""

from __future__ import annotations

import math
import random
import time
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


def normalize_advantages_by_domain(
    advantages: torch.Tensor,
    encoded_decisions: tuple[tuple[object, ...], ...],
) -> torch.Tensor:
    """Normalize combat, run and choice decisions independently."""

    result = torch.empty_like(advantages)
    domains = torch.tensor([
        [int(getattr(item, "screen_type")) for item in step]
        for step in encoded_decisions
    ])
    for domain in domains.unique():
        mask = domains == domain
        values = advantages[mask]
        result[mask] = (values - values.mean()) / (values.std(unbiased=False) + 1e-8)
    return result


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


def _coalesced_cpu(values: tuple[torch.Tensor, ...]) -> tuple[torch.Tensor, ...]:
    """Copy small CUDA tensors in one contiguous transfer per dtype."""

    if not values or values[0].device.type != "cuda":
        return tuple(value.cpu() for value in values)
    moved: list[torch.Tensor | None] = [None] * len(values)
    positions_by_dtype: dict[torch.dtype, list[int]] = {}
    for index, value in enumerate(values):
        positions_by_dtype.setdefault(value.dtype, []).append(index)
    for positions in positions_by_dtype.values():
        packed = torch.cat([values[index].reshape(-1) for index in positions]).cpu()
        offset = 0
        for index in positions:
            count = values[index].numel()
            moved[index] = packed[offset:offset + count].view(values[index].shape)
            offset += count
    return tuple(value for value in moved if value is not None)


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
        self.previous_action_types = torch.zeros(workers.size, dtype=torch.long, device=self.device)
        self.previous_rewards = torch.zeros(workers.size, dtype=torch.float32, device=self.device)
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
        previous_action_steps: list[torch.Tensor] = []
        previous_reward_steps: list[torch.Tensor] = []
        collect_terminations = {reason: 0 for reason in TERMINATION_REASONS}
        neow_count = 0
        neow_swap_probability = 0.0
        neow_entropy = 0.0
        neow_choices = [0, 0, 0, 0]
        self.model.eval()
        for _ in range(self.config.rollout_steps):
            encoded = tuple(encode_decision(value) for value in self.decisions)
            batch = PolicyBatch.from_encoded(encoded).to(self.device)
            output = self.model(
                *batch.model_inputs(),
                memory=self.memory,
                episode_start_mask=self.episode_starts,
                previous_action_types=self.previous_action_types,
                previous_rewards=self.previous_rewards,
            )
            distribution = Categorical(logits=output.logits)
            actions = distribution.sample()
            # Materialize rollout outputs before entering the serial native
            # simulator step.  Reusing actions_cpu also avoids transferring
            # the sampled actions twice on CUDA.
            log_probabilities = distribution.log_prob(actions)
            (
                actions_cpu,
                log_probabilities_cpu,
                values_cpu,
                episode_starts_cpu,
                memory_cpu,
                previous_actions_cpu,
                previous_rewards_cpu,
            ) = _coalesced_cpu((
                actions, log_probabilities, output.value, self.episode_starts,
                self.memory, self.previous_action_types, self.previous_rewards,
            ))
            episode_start_steps.append(episode_starts_cpu)
            memory_steps.append(memory_cpu)
            previous_action_steps.append(previous_actions_cpu)
            previous_reward_steps.append(previous_rewards_cpu)
            candidate_ids = [
                decision.actions[int(index)].candidate_id
                for decision, index in zip(self.decisions, actions_cpu)
            ]
            # Observe rare initial choices separately; never change sampling or loss.
            neow_indices = [i for i, d in enumerate(self.decisions)
                            if d.observation.screen.value == "NEOW"]
            if neow_indices:
                probabilities = distribution.probs[neow_indices].cpu().tolist()
                for i, row in zip(neow_indices, probabilities):
                    decision = self.decisions[i]
                    neow_count += 1
                    neow_entropy += -sum(p * math.log(p) for p in row if p > 0) / math.log(max(2, len(decision.actions)))
                    for j, action in enumerate(decision.actions):
                        if action.option_id == "event-option:3":
                            neow_swap_probability += row[j]
                        if action.candidate_id == candidate_ids[i] and action.option_id in {
                                f"event-option:{k}" for k in range(4)}:
                            neow_choices[int(action.option_id.rsplit(":", 1)[1])] += 1
            transitions = self.workers.step(candidate_ids)
            encoded_steps.append(encoded)
            action_steps.append(actions_cpu)
            log_probability_steps.append(log_probabilities_cpu)
            value_steps.append(values_cpu)
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
            next_previous_actions = batch.action_types[
                torch.arange(self.workers.size, device=self.device), actions
            ] + 1
            next_previous_rewards = torch.tensor(
                [float(item.reward) for item in transitions],
                dtype=torch.float32, device=self.device,
            )
            reset_decisions = self.workers.reset_many(
                reset_indices, self._take_seeds(len(reset_indices)),
            ) if reset_indices else []
            for index, decision in zip(reset_indices, reset_decisions):
                next_decisions[index] = decision
                self.episode_limits[index] = EpisodeLimitState.initial(decision)
                next_memory[index].zero_()
                next_episode_starts[index] = True
                next_previous_actions[index] = 0
                next_previous_rewards[index] = 0.0
                self.episodes += 1
            self.decisions = next_decisions
            self.memory = next_memory
            self.episode_starts = next_episode_starts
            self.previous_action_types = next_previous_actions
            self.previous_rewards = next_previous_rewards

        self.last_collect_terminations = collect_terminations
        self.environment_steps += self.config.rollout_steps * self.workers.size

        bootstrap_batch = PolicyBatch.from_decisions(self.decisions, self.model.config).to(self.device)
        bootstrap = self.model(
            *bootstrap_batch.model_inputs(),
            memory=self.memory,
            episode_start_mask=self.episode_starts,
            previous_action_types=self.previous_action_types,
            previous_rewards=self.previous_rewards,
        ).value.cpu()
        values = torch.stack(value_steps)
        self.last_collect_neow = {"neow_decisions": float(neow_count),
            **{f"neow_option_{i}_count": float(n) for i, n in enumerate(neow_choices)}}
        if neow_count:
            self.last_collect_neow.update({
                "neow_mean_swap_probability": neow_swap_probability / neow_count,
                "neow_mean_normalized_entropy": neow_entropy / neow_count,
            })
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
            torch.stack(previous_action_steps),
            torch.stack(previous_reward_steps),
        )

    def optimize(self, rollout: RolloutBatch) -> dict[str, float]:
        chunks = self._sequence_chunks(rollout)
        normalized_advantages = normalize_advantages_by_domain(
            rollout.advantages, rollout.encoded_decisions,
        )
        metric_names = (
            "policy", "value", "entropy", "loss", "approx_kl", "gradient_norm",
            "gradient_clip_fraction",
        )
        # Keep metric reduction on-device.  Calling float() for every metric in
        # every minibatch serialized CUDA six times per optimizer step.
        totals = {
            key: torch.zeros((), dtype=torch.float32, device=self.device)
            for key in metric_names
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
                    (
                        "gradient_clip_fraction",
                        (gradient_norm > self.config.max_gradient_norm).to(torch.float32),
                    ),
                ):
                    totals[key] = totals[key] + value.detach().to(torch.float32)
                updates += 1
            epoch_kl, epoch_clip_fraction = self._policy_diagnostics(rollout)
            epochs_completed = float(epoch + 1)
            epoch_diagnostics[f"approx_kl_epoch_{epoch + 1}"] = epoch_kl
            epoch_diagnostics["approx_kl_final"] = epoch_kl
            epoch_diagnostics["clip_fraction"] = epoch_clip_fraction
            if epoch_kl > self.config.target_kl:
                break
        self.update += 1
        reduced = torch.stack(tuple(totals[key] for key in metric_names)).div(updates)
        result = dict(zip(metric_names, reduced.detach().cpu().tolist()))
        result["epochs_completed"] = epochs_completed
        result["entropy_coefficient"] = entropy_coefficient
        result["learning_rate"] = float(self.optimizer.param_groups[0]["lr"])
        result["kl_early_stop"] = float(
            epoch_diagnostics.get("approx_kl_final", 0.0) > self.config.target_kl
        )
        returns_variance = rollout.returns.var(unbiased=False)
        result["value_explained_variance"] = float(
            1.0 - (rollout.returns - rollout.old_values).var(unbiased=False)
            / returns_variance.clamp_min(1e-8)
        )
        domains = torch.tensor([
            [int(item.screen_type) for item in step]
            for step in rollout.encoded_decisions
        ])
        domain_names = ("combat", "run", "choice")
        for domain, name in enumerate(domain_names):
            result[f"samples_{name}_fraction"] = float((domains == domain).float().mean())
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
        time_indices, environment_indices = self._sequence_indices(chunks)
        return tensor[time_indices, environment_indices].flatten()

    def _sequence_indices(
        self,
        chunks: list[tuple[int, int]],
    ) -> tuple[torch.Tensor, torch.Tensor]:
        starts = torch.tensor([start for start, _ in chunks], dtype=torch.long)
        environments = torch.tensor(
            [environment for _, environment in chunks], dtype=torch.long,
        )
        offsets = torch.arange(self.config.recurrent_sequence_length)
        return starts[:, None] + offsets[None, :], environments[:, None].expand(
            -1, self.config.recurrent_sequence_length,
        )

    def _evaluate_sequences(
        self,
        rollout: RolloutBatch,
        chunks: list[tuple[int, int]],
    ) -> tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
        starts = torch.tensor([start for start, _ in chunks], dtype=torch.long)
        environments = torch.tensor(
            [environment for _, environment in chunks], dtype=torch.long,
        )
        memory = rollout.input_memories[starts, environments].to(self.device)
        log_probabilities: list[torch.Tensor] = []
        values: list[torch.Tensor] = []
        entropies: list[torch.Tensor] = []
        for offset in range(self.config.recurrent_sequence_length):
            encoded = (
                rollout.encoded_decisions[start + offset][environment]
                for start, environment in chunks
            )
            batch = PolicyBatch.from_encoded(encoded).to(self.device)
            time_indices = starts + offset
            episode_starts = rollout.episode_starts[
                time_indices, environments
            ].to(self.device)
            output = self.model(
                *batch.model_inputs(), memory=memory, episode_start_mask=episode_starts,
                previous_action_types=rollout.previous_action_types[
                    time_indices, environments
                ].to(self.device),
                previous_rewards=rollout.previous_rewards[
                    time_indices, environments
                ].to(self.device),
            )
            memory = output.next_memory
            distribution = Categorical(logits=output.logits)
            actions = rollout.action_indices[
                time_indices, environments
            ].to(self.device)
            log_probabilities.append(distribution.log_prob(actions))
            values.append(output.value)
            legal = (~batch.action_padding).sum(dim=1).to(output.logits.dtype)
            scale = legal.log()
            normalized_entropy = torch.where(
                legal > 1,
                distribution.entropy() / scale.clamp_min(1e-8),
                torch.zeros_like(legal),
            )
            entropies.append(normalized_entropy)
        return tuple(
            torch.stack(items, dim=1).flatten()
            for items in (log_probabilities, values, entropies)
        )  # type: ignore[return-value]

    @torch.no_grad()
    def _policy_diagnostics(self, rollout: RolloutBatch) -> tuple[float, float]:
        """Measure the current policy on the complete fixed rollout."""

        chunks = self._sequence_chunks(rollout)
        count = rollout.action_indices.numel()
        kl_parts: list[torch.Tensor] = []
        clipped_parts: list[torch.Tensor] = []
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
            kl_parts.append(approximate_kl * weight)
            clipped_parts.append(clip_fraction * weight)
        self.model.train(was_training)
        diagnostics = torch.stack((
            torch.stack(kl_parts).sum(), torch.stack(clipped_parts).sum(),
        )).div(count).detach().cpu().tolist()
        return diagnostics[0], diagnostics[1]

    def train_update(self) -> dict[str, float]:
        started = time.perf_counter()
        rollout = self.collect()
        self.last_collect_seconds = time.perf_counter() - started
        optimize_started = time.perf_counter()
        metrics = self.optimize(rollout)
        self.last_optimize_seconds = time.perf_counter() - optimize_started
        metrics.update({f"terminations_{key}": float(value) for key, value in self.last_collect_terminations.items()})
        metrics.update(self.last_collect_neow)
        return metrics
