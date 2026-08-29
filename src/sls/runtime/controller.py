"""Fail-closed deterministic live policy controller."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import time
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

    def _log(self, record: dict[str, object]) -> None:
        if self.log_path is None:
            return
        self.log_path.parent.mkdir(parents=True, exist_ok=True)
        with self.log_path.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(record, sort_keys=True) + "\n")

    def _unresolved_intent(self) -> dict[str, object] | None:
        if self.log_path is None or not self.log_path.is_file():
            return None
        pending: dict[str, dict[str, object]] = {}
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
            elif record.get("phase") in {"ACK", "RECOVERED"}:
                pending.pop(identifier, None)
        return next(reversed(pending.values()), None) if pending else None

    @torch.no_grad()
    def choose(self, decision: Decision) -> tuple[int, float]:
        batch = PolicyBatch.from_decisions((decision,), self.artifact.model.config).to(self.device)
        output = self.artifact.model(*batch.model_inputs())
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
        decision = self.backend.attach()
        ascension = decision.observation.run.ascension
        metadata = self.artifact.metadata
        if metadata.goal != "HEART":
            raise ValueError(
                f"live deployment requires a HEART artifact, got {metadata.goal}"
            )
        if not metadata.ascension_min <= ascension <= metadata.ascension_max:
            raise ValueError(f"policy artifact does not support ascension {ascension}")
        unresolved = self._unresolved_intent()
        if unresolved is not None:
            unresolved_boundary = str(unresolved["boundary_id"])
            if unresolved_boundary == boundary_id(decision):
                raise RuntimeError(
                    "previous action delivery is uncertain and the boundary is unchanged; "
                    "refusing to resend"
                )
            self._log({
                "schema": "sls-live-action-v1", "phase": "RECOVERED",
                "time_unix": time.time(), "boundary_id": unresolved_boundary,
                "observed_boundary_id": boundary_id(decision),
            })
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
            })
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
