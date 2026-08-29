"""Fail-closed deterministic live policy controller."""

from __future__ import annotations

import hashlib
import json
import math
import time
from dataclasses import asdict
from pathlib import Path
from typing import Callable

import torch

from sls.backends.original import LiveGameBackend
from sls.contracts import Decision
from sls.model import PolicyBatch
from sls.runtime.artifact import LoadedPolicyArtifact


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
        self._choice_memory = self.memory

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

    @torch.no_grad()
    def choose(self, decision: Decision) -> tuple[int, float]:
        batch = PolicyBatch.from_decisions((decision,), self.artifact.model.config).to(self.device)
        output = self.artifact.model(*batch.model_inputs(), memory=self.memory)
        self._choice_memory = output.next_memory.detach()
        probabilities = output.logits.softmax(dim=1)
        index = int(probabilities[0].argmax().item())
        return index, float(probabilities[0, index].item())

    def run(
        self,
        *,
        max_actions: int | None = None,
        stop_requested: Callable[[], bool] | None = None,
    ) -> Decision:
        if max_actions is not None and max_actions <= 0:
            raise ValueError("max_actions must be positive")
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
            self._restore_memory(unresolved)
            self._log({
                "schema": "sls-live-action-v1", "phase": "RECOVERED",
                "time_unix": time.time(), "boundary_id": unresolved_boundary,
                "observed_boundary_id": current,
                "artifact_id": self.artifact_id,
                "memory_after": self.memory[0].detach().cpu().tolist(),
            })
        elif latest is not None and latest.get("observed_boundary_id") == current:
            self._restore_memory(latest)
        elif not (
            decision.observation.screen.value == "NEOW"
            and decision.observation.run.act == 1
            and decision.observation.run.floor == 0
        ):
            raise RuntimeError(
                "recurrent policy can only start at Neow or resume from its matching journal"
            )
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
            action = decision.actions[index]
            memory_after = self._choice_memory[0].detach().cpu().tolist()
            self._log({
                "schema": "sls-live-action-v1",
                "phase": "INTENT",
                "time_unix": time.time(),
                "boundary_id": current_boundary,
                "screen": decision.observation.screen.value,
                "act": decision.observation.run.act,
                "floor": decision.observation.run.floor,
                "ascension": ascension,
                "candidate_id": action.candidate_id,
                "confidence": confidence,
                "low_confidence": confidence < self.low_confidence,
                "artifact_id": self.artifact_id,
                "memory_after": memory_after,
            })
            self.memory = self._choice_memory
            previous_boundary = current_boundary
            transition = self.backend.step(action)
            decision = transition.decision
            self._log({
                "schema": "sls-live-action-v1", "phase": "ACK",
                "time_unix": time.time(), "boundary_id": current_boundary,
                "observed_boundary_id": boundary_id(decision),
            })
            actions_taken += 1
            if max_actions is not None and actions_taken >= max_actions:
                break
        return decision
