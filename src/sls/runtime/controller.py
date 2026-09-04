"""Fail-closed deterministic live policy controller."""

from __future__ import annotations

import hashlib
import json
import math
import os
import time
import uuid
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Callable

import torch

from sls.backends.original import LiveGameBackend
from sls.contracts import Decision
from sls.model import PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.runtime.artifact import LoadedPolicyArtifact


@dataclass(frozen=True, slots=True)
class ScoredAction:
    index: int
    candidate_id: str
    logit: float
    probability: float


@dataclass(frozen=True, slots=True)
class PolicyScore:
    actions: tuple[ScoredAction, ...]
    value: float
    next_memory: torch.Tensor

    @property
    def recommended(self) -> ScoredAction:
        return max(self.actions, key=lambda item: item.probability)


def boundary_id(decision: Decision) -> str:
    payload = {
        "observation": decision.observation.to_dict(),
        "actions": sorted(action.candidate_id for action in decision.actions),
        "terminal": decision.terminal,
    }
    encoded = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


class AgentRuntime:
    def __init__(
        self,
        backend: LiveGameBackend,
        artifact: LoadedPolicyArtifact,
        *,
        device: str | torch.device = "cpu",
        log_path: Path | None = None,
        low_confidence: float = 0.55,
    ) -> None:
        if not 0.0 <= low_confidence <= 1.0:
            raise ValueError("low_confidence must be between zero and one")
        self.backend = backend
        self.artifact = artifact
        self.device = torch.device(device)
        self.log_path = log_path
        self.low_confidence = low_confidence
        self.memory = artifact.model.initial_memory(1, self.device)
        self.previous_action_types = torch.zeros(1, dtype=torch.long, device=self.device)
        self.previous_rewards = torch.zeros(1, dtype=torch.float32, device=self.device)
        self._choice_memory = self.memory
        self._last_score: PolicyScore | None = None
        self.session_id = uuid.uuid4().hex

    @property
    def artifact_id(self) -> str:
        encoded = json.dumps(
            asdict(self.artifact.metadata), sort_keys=True, separators=(",", ":"),
        ).encode("utf-8")
        return hashlib.sha256(encoded).hexdigest()

    def _log(self, record: dict[str, object]) -> None:
        if self.log_path is None:
            return
        self.log_path.parent.mkdir(parents=True, exist_ok=True)
        with self.log_path.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(record, sort_keys=True) + "\n")
            stream.flush()
            os.fsync(stream.fileno())

    def _diagnostic_run_seed(self) -> int | None:
        """Read the wire seed for journaling without exposing it to the policy."""

        try:
            payload = self.backend.raw_payload
        except (AttributeError, RuntimeError):
            return None
        game = payload.get("game_state") or {}
        value = game.get("seed", payload.get("seed"))
        try:
            return int(value) if value is not None else None
        except (TypeError, ValueError):
            return None

    def _journal_state(
        self,
    ) -> tuple[dict[str, object] | None, dict[str, object] | None]:
        if self.log_path is None or not self.log_path.is_file():
            return None, None
        pending: dict[str, dict[str, object]] = {}
        latest: dict[str, object] | None = None
        for line in self.log_path.read_text(encoding="utf-8").splitlines():
            try:
                record = json.loads(line)
            except json.JSONDecodeError:
                continue
            identifier = str(record.get("boundary_id") or "")
            if not identifier:
                continue
            if record.get("phase") == "INTENT":
                pending[identifier] = record
            elif record.get("phase") == "ACK":
                intent = pending.pop(identifier, None)
                if intent is not None:
                    latest = {
                        **intent,
                        "observed_boundary_id": record.get("observed_boundary_id"),
                        "previous_action_type": record.get("previous_action_type"),
                        "previous_reward": record.get("previous_reward"),
                    }
            elif record.get("phase") == "RECOVERED":
                pending.pop(identifier, None)
                latest = record
        unresolved = next(reversed(pending.values()), None) if pending else None
        return unresolved, latest

    def _restore_memory(self, record: dict[str, object]) -> None:
        if record.get("artifact_id") != self.artifact_id:
            raise RuntimeError("live memory journal belongs to another policy artifact")
        raw = record.get("memory_after")
        if not isinstance(raw, list) or len(raw) != self.artifact.metadata.recurrent_memory_size:
            raise RuntimeError("live memory journal has an invalid recurrent state")
        values = [float(value) for value in raw]
        if not all(math.isfinite(value) for value in values):
            raise RuntimeError("live memory journal contains non-finite values")
        self.memory = torch.tensor(values, dtype=torch.float32, device=self.device).view(1, -1)
        action_type = int(record.get("previous_action_type") or 0)
        reward = float(record.get("previous_reward") or 0.0)
        if not 0 <= action_type <= len(ACTION_TYPE_IDS) or not math.isfinite(reward):
            raise RuntimeError("live memory journal has invalid previous experience")
        self.previous_action_types = torch.tensor(
            [action_type], dtype=torch.long, device=self.device,
        )
        self.previous_rewards = torch.tensor(
            [reward], dtype=torch.float32, device=self.device,
        )

    @torch.no_grad()
    def score(self, decision: Decision) -> PolicyScore:
        """Score a boundary without committing recurrent policy state."""

        batch = PolicyBatch.from_decisions((decision,), self.artifact.model.config).to(self.device)
        output = self.artifact.model(
            *batch.model_inputs(), memory=self.memory,
            previous_action_types=self.previous_action_types,
            previous_rewards=self.previous_rewards,
        )
        next_memory = output.next_memory.detach()
        probabilities = output.logits.softmax(dim=1)
        actions = tuple(
            ScoredAction(
                index=index,
                candidate_id=action.candidate_id,
                logit=float(output.logits[0, index].item()),
                probability=float(probabilities[0, index].item()),
            )
            for index, action in enumerate(decision.actions)
        )
        return PolicyScore(actions, float(output.value[0].item()), next_memory)

    @torch.no_grad()
    def choose(self, decision: Decision) -> tuple[int, float]:
        score = self.score(decision)
        self._last_score = score
        self._choice_memory = score.next_memory
        selected = score.recommended
        return selected.index, selected.probability

    def attach(self) -> Decision:
        """Attach and restore the recurrent state at a proven boundary."""

        metadata = self.artifact.metadata
        configure_goal = getattr(self.backend, "configure_goal", None)
        if configure_goal is not None:
            configure_goal(metadata.goal)
        decision = self.backend.attach()
        ascension = decision.observation.run.ascension
        if not metadata.ascension_min <= ascension <= metadata.ascension_max:
            raise ValueError(f"policy artifact does not support ascension {ascension}")
        unresolved, latest = self._journal_state()
        current = boundary_id(decision)
        if unresolved is not None:
            unresolved_boundary = str(unresolved["boundary_id"])
            if unresolved_boundary == current:
                raise RuntimeError(
                    "previous action delivery is uncertain and the boundary is unchanged; "
                    "refusing to resend"
                )
            raise RuntimeError(
                "previous action delivery is uncertain and the current boundary is different; "
                "the protocol cannot prove that this boundary is the intended successor"
            )
        if latest is not None and latest.get("observed_boundary_id") == current:
            self._restore_memory(latest)
        elif not (
            decision.observation.screen.value == "NEOW"
            and decision.observation.run.act == 1
            and decision.observation.run.floor == 0
        ):
            raise RuntimeError(
                "recurrent policy can only start at Neow or resume from its matching journal"
            )
        return decision

    def execute_scored_action(
        self,
        decision: Decision,
        score: PolicyScore,
        index: int,
        *,
        selection_source: str = "model",
        inspector_mode: str | None = None,
        delay_seconds: float | None = None,
        card_reward_preview_seconds: float | None = None,
    ):  # type: ignore[no-untyped-def]
        """Durably commit and execute one action from a scored boundary."""

        if selection_source not in {"model", "manual"}:
            raise ValueError("selection_source must be model or manual")
        if not 0 <= index < len(decision.actions):
            raise ValueError("selected action index is out of range")
        selected_score = score.actions[index]
        action = decision.actions[index]
        if selected_score.candidate_id != action.candidate_id:
            raise ValueError("score does not belong to this decision boundary")
        recommended = score.recommended
        current_boundary = boundary_id(decision)
        rankings = [
            {
                "rank": rank,
                "candidate_id": item.candidate_id,
                "logit": item.logit,
                "probability": item.probability,
            }
            for rank, item in enumerate(
                sorted(score.actions, key=lambda item: item.probability, reverse=True),
                start=1,
            )
        ]
        memory_after = score.next_memory[0].detach().cpu().tolist()
        record: dict[str, object] = {
            "schema": "sls-live-action-v4",
            "phase": "INTENT",
            "time_unix": time.time(),
            "session_id": self.session_id,
            "boundary_id": current_boundary,
            "screen": decision.observation.screen.value,
            "act": decision.observation.run.act,
            "floor": decision.observation.run.floor,
            "ascension": decision.observation.run.ascension,
            "candidate_id": action.candidate_id,
            "selected_action": action.to_dict(),
            "model_top_candidate_id": recommended.candidate_id,
            "model_top_action": decision.actions[recommended.index].to_dict(),
            "selection_source": selection_source,
            "logit": selected_score.logit,
            "confidence": selected_score.probability,
            "value": score.value,
            "action_rankings": rankings,
            "candidate_actions": [candidate.to_dict() for candidate in decision.actions],
            "observation": decision.observation.to_dict(),
            "low_confidence": selected_score.probability < self.low_confidence,
            "artifact_id": self.artifact_id,
            "memory_after": memory_after,
        }
        run_seed = self._diagnostic_run_seed()
        if run_seed is not None:
            record["run_seed"] = run_seed
        if inspector_mode is not None:
            record["inspector_mode"] = inspector_mode
        if delay_seconds is not None:
            record["delay_seconds"] = delay_seconds
        if card_reward_preview_seconds is not None:
            record["card_reward_preview_seconds"] = card_reward_preview_seconds
        self._log(record)
        self.memory = score.next_memory
        transition = self.backend.step(action)
        self.previous_action_types = torch.tensor(
            [ACTION_TYPE_IDS[action.kind.value] + 1],
            dtype=torch.long, device=self.device,
        )
        self.previous_rewards = torch.tensor(
            [float(transition.reward)], dtype=torch.float32, device=self.device,
        )
        self._log({
            "schema": "sls-live-action-v4", "phase": "ACK",
            "session_id": self.session_id,
            "time_unix": time.time(), "boundary_id": current_boundary,
            "observed_boundary_id": boundary_id(transition.decision),
            "previous_action_type": int(self.previous_action_types[0]),
            "previous_reward": float(self.previous_rewards[0]),
        })
        return transition

    def run(
        self,
        *,
        max_actions: int | None = None,
        stop_requested: Callable[[], bool] | None = None,
    ) -> Decision:
        if max_actions is not None and max_actions <= 0:
            raise ValueError("max_actions must be positive")
        decision = self.attach()
        previous_boundary: str | None = None
        actions_taken = 0
        while not decision.terminal:
            if stop_requested is not None and stop_requested():
                break
            current_boundary = boundary_id(decision)
            if current_boundary == previous_boundary:
                raise RuntimeError(
                    "live decision boundary did not advance; refusing to repeat an action"
                )
            index, confidence = self.choose(decision)
            score = self._last_score or PolicyScore(
                tuple(
                    ScoredAction(
                        action_index,
                        candidate.candidate_id,
                        0.0,
                        confidence if action_index == index else 0.0,
                    )
                    for action_index, candidate in enumerate(decision.actions)
                ),
                0.0,
                self._choice_memory,
            )
            self._last_score = None
            previous_boundary = current_boundary
            transition = self.execute_scored_action(decision, score, index)
            decision = transition.decision
            actions_taken += 1
            if max_actions is not None and actions_taken >= max_actions:
                break
        return decision
