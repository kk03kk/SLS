"""Immutable Original truth bundles and deterministic integrity metadata."""

from __future__ import annotations

from dataclasses import asdict, dataclass
from datetime import datetime, timezone
import gzip
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time
from typing import Any, Iterable, Mapping

from sls.backends.original.adapter import adapt_original
from sls.contracts.continuation import continuation_original, continuation_simulator
from sls.validation.compare import canonical_original, canonical_simulator
from sls.validation.diff import differences


TRUTH_BUNDLE_SCHEMA = "sls-original-truth-bundle-v1"
BOUNDARY_SCHEMA = "sls-original-truth-boundary-v1"
CHECKPOINT_SCHEMA = "sls-native-checkpoint-v1"
EVIDENCE_CLASSES = (
    "SIMULATOR_REPLAY", "ORACLE_SCENARIO", "RESUMED_AUTOSAVE", "LIVE_FULLRUN",
)


def evidence_at_least(actual: str, required: str) -> bool:
    if actual not in EVIDENCE_CLASSES or required not in EVIDENCE_CLASSES:
        raise ValueError("unknown evidence class")
    return EVIDENCE_CLASSES.index(actual) >= EVIDENCE_CLASSES.index(required)


def canonical_json_bytes(value: Any) -> bytes:
    return (json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":"),
        allow_nan=False,
    ) + "\n").encode("utf-8")


def value_hash(value: Any) -> str:
    return hashlib.sha256(canonical_json_bytes(value)).hexdigest()


def file_hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(canonical_json_bytes(value))


def write_json_gz(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as stream:
            stream.write(canonical_json_bytes(value))


def write_jsonl_gz(path: Path, values: Iterable[Mapping[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as stream:
            for value in values:
                stream.write(canonical_json_bytes(value))


def read_jsonl_gz(path: Path) -> list[dict[str, Any]]:
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        return [json.loads(line) for line in stream if line.strip()]


def stable_file(path: Path, *, attempts: int = 20, interval: float = 0.1) -> bool:
    """Wait until size and digest are unchanged across two observations."""

    previous: tuple[int, str] | None = None
    for _ in range(attempts):
        if path.is_file():
            current = (path.stat().st_size, file_hash(path))
            if current == previous:
                return True
            previous = current
        time.sleep(interval)
    return False


def difference_signature(
    *, evidence_class: str, profile: str, screen: str, act: int, floor: int,
    category: str, values: Mapping[str, Any], preceding_action: str | None,
) -> str | None:
    if not values:
        return None
    path = sorted(values)[0]
    pair = values[path]
    left, right = pair if isinstance(pair, (list, tuple)) and len(pair) == 2 else (pair, None)
    identity = {
        "evidence_class": evidence_class, "profile": profile, "screen": screen,
        "act": act, "floor": floor, "category": category, "path": path,
        "preceding_action": preceding_action,
        "original_hash": value_hash(left), "simulator_hash": value_hash(right),
    }
    return value_hash(identity)


def continuation_differences(
    original: Mapping[str, Any], simulator: Mapping[str, Any],
) -> dict[str, tuple[Any, Any]]:
    """Compare only continuation fields with a stable cross-backend meaning."""

    aliases = {1: "EVENT", 2: "COMBAT_REWARD", 3: "BOSS_REWARD", 4: "CARD_REWARD",
               5: "MAP", 6: "TREASURE", 7: "REST", 8: "SHOP", 9: "COMBAT"}
    result: dict[str, tuple[Any, Any]] = {}
    for key in (
        "event_phase", "combat_turn",
        "card_selection_source", "card_selection_task", "card_selection_count",
        "post_combat", "loading_post_combat", "action_queue_types", "card_queue_types",
    ):
        left, right = original.get(key), simulator.get(key)
        if left is not None and right is not None and left != right:
            result[f"$.{key}"] = (left, right)
    left_event, right_event = original.get("event_id"), simulator.get("event_id")
    if left_event is not None and right_event is not None:
        left_token = str(left_event).rsplit(".", 1)[-1]
        if left_token == "NeowEvent":
            left_token = "NEOW"
        if left_token.upper() != str(right_event).upper():
            result["$.event_id"] = (left_event, right_event)
    left_kind, right_kind = original.get("continuation_kind"), simulator.get("continuation_kind")
    left_kind = {"GRID": "CARD_REWARD"}.get(str(left_kind).upper(), left_kind)
    right_kind = aliases.get(right_kind, right_kind)
    if left_kind is not None and right_kind is not None and str(left_kind).upper() != str(right_kind).upper():
        result["$.continuation_kind"] = (left_kind, right_kind)
    return result


def resumable_original_boundary(payload: Mapping[str, Any]) -> dict[str, Any]:
    """Canonical official-save boundary with explicit dead-stream normalization."""

    state = json.loads(json.dumps(canonical_original(payload)))
    game = payload.get("game_state") or {}
    normalizations: list[str] = []
    if int(game.get("floor", 0) or 0) > 0:
        rng = state.get("rng") or {}
        if "neow" in rng:
            rng.pop("neow")
        normalizations.append("drop_rng.neow_after_floor0")
    return {
        "state": state,
        "continuation": continuation_original(payload),
        "normalizations": normalizations,
    }


def _git_metadata(root: Path) -> tuple[str | None, str | None, bool]:
    try:
        commit = subprocess.run(
            ["git", "rev-parse", "HEAD"], cwd=root, check=True,
            capture_output=True, text=True,
        ).stdout.strip()
        diff = subprocess.run(
            ["git", "diff", "--binary", "HEAD"], cwd=root, check=True,
            capture_output=True,
        ).stdout
        return commit, hashlib.sha256(diff).hexdigest(), bool(diff)
    except (OSError, subprocess.CalledProcessError):
        return None, None, True


@dataclass(frozen=True, slots=True)
class Anchor:
    anchor_id: str
    sequence: int
    kind: str
    boundary_hash: str
    resume_boundary_hash: str
    path: str


class TruthBundleRecorder:
    """Collect one run and atomically finalize its immutable truth bundle."""

    def __init__(
        self, root: Path, *, seed: int, profile_id: str, policy_id: str,
        evidence_class: str = "LIVE_FULLRUN", repository_root: Path | None = None,
        jar_paths: Mapping[str, Path] | None = None, autosave: Path | None = None,
    ) -> None:
        if evidence_class not in EVIDENCE_CLASSES:
            raise ValueError(f"unknown evidence class: {evidence_class}")
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
        self.path = root / f"{stamp}-seed-{seed}"
        self.path.mkdir(parents=True, exist_ok=False)
        self.seed, self.profile_id, self.policy_id = seed, profile_id, policy_id
        self.evidence_class = evidence_class
        self.repository_root = repository_root or Path.cwd()
        self.jar_paths = dict(jar_paths or {})
        self.autosave = autosave
        self.boundaries: list[dict[str, Any]] = []
        self.protocol: list[dict[str, Any]] = []
        self.anchors: list[Anchor] = []
        self._last_anchor_cursor: tuple[Any, ...] | None = None

    def record_protocol(self, direction: str, data: Any) -> None:
        self.protocol.append({"sequence": len(self.protocol), "direction": direction, "data": data})

    def record_boundary(
        self, *, sequence: int, original_payload: Mapping[str, Any], original_decision: Any,
        simulator_state: Mapping[str, Any], simulator_decision: Any,
        action: Any | None, commands: Iterable[str], observation_diff: Mapping[str, Any],
        action_diff: Mapping[str, Any], state_diff: Mapping[str, Any], checkpoint: Mapping[str, Any],
        terminal_kind: str | None = None,
    ) -> dict[str, Any]:
        original_payload = json.loads(json.dumps(original_payload, ensure_ascii=False))
        simulator_state = json.loads(json.dumps(simulator_state, ensure_ascii=False))
        observation = original_decision.observation
        original_canonical = canonical_original(original_payload)
        simulator_canonical = canonical_simulator(simulator_state)
        continuation = {
            "original": continuation_original(original_payload),
            "simulator": continuation_simulator(simulator_state),
        }
        all_differences = {
            **{f"observation:{k}": v for k, v in observation_diff.items()},
            **{f"actions:{k}": v for k, v in action_diff.items()},
            **{f"state:{k}": v for k, v in state_diff.items()},
        }
        action_value = None if action is None else action.to_dict()
        continuation_diff = continuation_differences(continuation["original"], continuation["simulator"])
        all_differences.update({f"continuation:{k}": v for k, v in continuation_diff.items()})
        record = {
            "schema": BOUNDARY_SCHEMA,
            "sequence": sequence,
            "cursor": {
                "act": observation.run.act, "floor": observation.run.floor,
                "room": continuation["original"]["room_class"],
                "screen": observation.screen.value,
                "combat_turn": continuation["original"]["combat_turn"],
            },
            "raw_original_payload": original_payload,
            "canonical_original_decision": {
                "observation": original_decision.observation.to_dict(),
                "actions": [candidate.to_dict() for candidate in original_decision.actions],
                "terminal": original_decision.terminal,
            },
            "canonical_simulator_decision": {
                "observation": simulator_decision.observation.to_dict(),
                "actions": [candidate.to_dict() for candidate in simulator_decision.actions],
                "terminal": simulator_decision.terminal,
            },
            "canonical_public_state": original_canonical,
            "rng": original_payload.get("_rng") or (original_payload.get("game_state") or {}).get("_rng") or {},
            "math_seed": original_payload.get("math_seed"),
            "continuation": continuation,
            "candidates": [candidate.to_dict() for candidate in original_decision.actions],
            "selected_action": action_value,
            "commands": list(commands),
            "action_executed": False,
            "original_boundary_hash": value_hash({"state": original_canonical, "continuation": continuation["original"]}),
            "simulator_boundary_hash": value_hash({"state": simulator_canonical, "continuation": continuation["simulator"]}),
            "differences": all_differences,
            "difference_signature": difference_signature(
                evidence_class=self.evidence_class, profile=self.profile_id,
                screen=observation.screen.value, act=observation.run.act,
                floor=observation.run.floor, category="paired-boundary",
                values=all_differences,
                preceding_action=None if not self.boundaries else (
                    (self.boundaries[-1].get("selected_action") or {}).get("kind")
                ),
            ),
            "terminal_kind": terminal_kind,
        }
        self.boundaries.append(record)
        cursor = (observation.run.act, observation.run.floor)
        anchor_kind = "RUN_START" if sequence == 0 else "ENTER_ROOM" if cursor != self._last_anchor_cursor else None
        if observation.screen.value in {"COMBAT_REWARD", "BOSS_REWARD", "ACT_TRANSITION"}:
            anchor_kind = observation.screen.value
        if anchor_kind:
            self._create_anchor(
                sequence, anchor_kind, record["original_boundary_hash"],
                value_hash(resumable_original_boundary(original_payload)), checkpoint,
            )
            self._last_anchor_cursor = cursor
        return record

    def mark_last_action_executed(self, commands: Iterable[str]) -> None:
        if not self.boundaries:
            raise RuntimeError("no truth boundary is available")
        self.boundaries[-1]["commands"] = list(commands)
        self.boundaries[-1]["action_executed"] = True

    def _create_anchor(
        self, sequence: int, kind: str, boundary_hash: str,
        resume_boundary_hash: str, checkpoint: Mapping[str, Any],
    ) -> None:
        anchor_id = f"a{len(self.anchors):04d}-s{sequence:06d}-{kind.lower()}"
        target = self.path / "anchors" / anchor_id
        target.mkdir(parents=True)
        write_json_gz(target / "simulator-checkpoint.json.gz", checkpoint)
        copied: dict[str, str] = {}
        if kind != "RUN_START" and self.autosave and stable_file(self.autosave):
            shutil.copy2(self.autosave, target / "original.autosave")
            copied["original.autosave"] = file_hash(target / "original.autosave")
            backup = Path(str(self.autosave) + ".backUp")
            if backup.is_file() and stable_file(backup):
                shutil.copy2(backup, target / "original.autosave.backUp")
                copied["original.autosave.backUp"] = file_hash(target / "original.autosave.backUp")
        metadata = {
            "schema": "sls-original-anchor-v1", "anchor_id": anchor_id,
            "sequence": sequence, "kind": kind, "boundary_hash": boundary_hash,
            "resume_boundary_hash": resume_boundary_hash,
            "resume_normalizations": resumable_original_boundary(
                self.boundaries[sequence]["raw_original_payload"]
            )["normalizations"],
            "files": copied,
        }
        write_json(target / "metadata.json", metadata)
        self.anchors.append(Anchor(
            anchor_id, sequence, kind, boundary_hash, resume_boundary_hash,
            str(target.relative_to(self.path)),
        ))

    def finalize(self, *, complete: bool, outcome: str | None, error: str | None) -> Path:
        boundaries = self.path / "boundaries.jsonl.gz"
        protocol = self.path / "protocol.jsonl.gz"
        write_jsonl_gz(boundaries, self.boundaries)
        write_jsonl_gz(protocol, self.protocol)
        commit, dirty_hash, dirty = _git_metadata(self.repository_root)
        jar_hashes = {
            name: {"path": str(path), "sha256": file_hash(path)}
            for name, path in sorted(self.jar_paths.items()) if path.is_file()
        }
        artifacts: dict[str, str] = {
            "boundaries.jsonl.gz": file_hash(boundaries),
            "protocol.jsonl.gz": file_hash(protocol),
        }
        for metadata in sorted((self.path / "anchors").glob("*/metadata.json")) if (self.path / "anchors").exists() else ():
            artifacts[str(metadata.relative_to(self.path)).replace("\\", "/")] = file_hash(metadata)
            checkpoint = metadata.parent / "simulator-checkpoint.json.gz"
            artifacts[str(checkpoint.relative_to(self.path)).replace("\\", "/")] = file_hash(checkpoint)
            for save in metadata.parent.glob("original.autosave*"):
                artifacts[str(save.relative_to(self.path)).replace("\\", "/")] = file_hash(save)
        manifest = {
            "schema": TRUTH_BUNDLE_SCHEMA, "seed": self.seed,
            "profile_id": self.profile_id, "policy_id": self.policy_id,
            "evidence_class": self.evidence_class,
            "started_at": self.path.name.split("-seed-")[0],
            "finished_at": datetime.now(timezone.utc).isoformat(),
            "git": {"commit": commit, "dirty": dirty, "dirty_diff_hash": dirty_hash},
            "jars": jar_hashes,
            "python": {"executable": sys.executable, "version": sys.version},
            "trace_schema": BOUNDARY_SCHEMA, "checkpoint_schema": CHECKPOINT_SCHEMA,
            "anchors": [asdict(anchor) for anchor in self.anchors],
            "segments": [{"start": 0, "end": max(0, len(self.boundaries) - 1)}] if self.boundaries else [],
            "start_state": None if not self.boundaries else {
                "cursor": self.boundaries[0]["cursor"],
                "boundary_hash": self.boundaries[0]["original_boundary_hash"],
            },
            "end_state": None if not self.boundaries else {
                "cursor": self.boundaries[-1]["cursor"],
                "boundary_hash": self.boundaries[-1]["original_boundary_hash"],
            },
            "complete": complete, "outcome": outcome, "error": error,
            "artifacts": artifacts,
        }
        write_json(self.path / "manifest.json", manifest)
        return self.path


def load_bundle(path: Path, *, verify: bool = True) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    manifest_path = path / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("schema") != TRUTH_BUNDLE_SCHEMA:
        raise ValueError(f"unsupported truth bundle schema: {manifest.get('schema')}")
    if verify:
        for relative, expected in manifest.get("artifacts", {}).items():
            artifact = path / relative
            if not artifact.is_file() or file_hash(artifact) != expected:
                raise ValueError(f"truth artifact hash mismatch: {relative}")
    boundaries = read_jsonl_gz(path / "boundaries.jsonl.gz")
    for boundary in boundaries:
        if boundary.get("schema") != BOUNDARY_SCHEMA:
            raise ValueError(f"unsupported boundary schema: {boundary.get('schema')}")
        # Prove raw wire truth remains parseable. Its newly adapted result is
        # deliberately allowed to differ from the historical canonical cache.
        adapt_original(boundary["raw_original_payload"])
    return manifest, boundaries
