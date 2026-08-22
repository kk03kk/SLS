"""Immutable Original truth bundles and deterministic integrity metadata."""

from __future__ import annotations

from dataclasses import asdict, dataclass
import base64
from datetime import datetime, timezone
import gzip
import hashlib
import json
import os
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


TRUTH_BUNDLE_SCHEMA = "sls-original-truth-bundle-v2"
LEGACY_TRUTH_BUNDLE_SCHEMA = "sls-original-truth-bundle-v1"
BOUNDARY_SCHEMA = "sls-original-truth-boundary-v2"
LEGACY_BOUNDARY_SCHEMA = "sls-original-truth-boundary-v1"
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
        value, ensure_ascii=True, sort_keys=True, separators=(",", ":"),
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


def native_build_metadata(repository: Path) -> dict[str, Any]:
    """Describe the native producer without treating every rebuild as an ABI break."""

    candidates = sorted(
        (repository / ".build" / "native" / sys.implementation.cache_tag).glob("_lightspeed*.pyd")
    )
    if not candidates:
        return {"abi": sys.implementation.cache_tag, "checkpoint_schema": CHECKPOINT_SCHEMA}
    artifact = candidates[0]
    return {
        "path": str(artifact), "sha256": file_hash(artifact),
        "abi": sys.implementation.cache_tag, "checkpoint_schema": CHECKPOINT_SCHEMA,
    }


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


def append_jsonl(path: Path, value: Mapping[str, Any], *, durable: bool = True) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("ab") as stream:
        stream.write(canonical_json_bytes(value))
        stream.flush()
        if durable:
            os.fsync(stream.fileno())


def sync_file(path: Path) -> None:
    if not path.is_file():
        return
    with path.open("ab") as stream:
        stream.flush()
        os.fsync(stream.fileno())


def read_jsonl(path: Path, *, tolerate_truncated_tail: bool = False) -> list[dict[str, Any]]:
    values: list[dict[str, Any]] = []
    with path.open("rt", encoding="utf-8") as stream:
        for number, line in enumerate(stream, 1):
            if not line.strip():
                continue
            try:
                value = json.loads(line)
            except json.JSONDecodeError:
                if tolerate_truncated_tail and not stream.read(1):
                    break
                raise ValueError(f"invalid JSONL record at {path}:{number}")
            if not isinstance(value, dict):
                raise ValueError(f"JSONL record is not an object at {path}:{number}")
            values.append(value)
    return values


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


def autosave_identity(path: Path) -> dict[str, Any]:
    """Decode the stock Base64/XOR save envelope and return anchor identity."""

    encoded = path.read_bytes()
    decoded = base64.b64decode(encoded, validate=True)
    key = b"key"
    payload = bytes(value ^ key[index % len(key)] for index, value in enumerate(decoded))
    save = json.loads(payload.decode("utf-8"))
    if not isinstance(save, dict):
        raise ValueError("autosave payload is not an object")
    return {
        "seed": int(save["seed"]), "floor": int(save["floor_num"]),
        "character": "IRONCLAD", "room_class": str(save.get("current_room") or ""),
        "map_x": int(save.get("room_x", -1)), "map_y": int(save.get("room_y", -1)),
    }


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
    terminal = str(original.get("screen") or original.get("continuation_kind") or "").upper() in {
        "DEATH", "VICTORY", "GAME_OVER", "COMPLETE",
    }
    for key in (
        "event_phase", "action_phase", "combat_turn",
        "card_selection_source", "card_selection_task", "card_selection_count",
        "post_combat", "loading_post_combat",
        "action_queue_types", "card_queue_types", "bottled_cards",
    ):
        if terminal and key in {
            "action_phase", "combat_turn", "action_queue_types", "card_queue_types",
        }:
            continue
        left, right = original.get(key), simulator.get(key)
        if key == "event_phase":
            # Oracle reflection emits numeric Java fields as strings so enum
            # and integer phases share one stable wire representation.
            try:
                left = int(left) if left is not None else None
            except (TypeError, ValueError):
                pass
            try:
                right = int(right) if right is not None else None
            except (TypeError, ValueError):
                pass
        if key == "card_selection_task":
            left = {"PURGE": "REMOVE"}.get(str(left).upper(), left)
            right = {"PURGE": "REMOVE"}.get(str(right).upper(), right)
        if key == "post_combat" and str(original.get("continuation_kind")).upper() == "MAP":
            # Stock retains the battle-over room object while its map is open;
            # native has already discarded that inert UI owner.
            continue
        if left is not None and right is not None and left != right:
            result[f"$.{key}"] = (left, right)
    left_event, right_event = original.get("event_id"), simulator.get("event_id")
    if left_event is not None and right_event is not None:
        from sls.content.normalize import normalize_event_id
        left_token = str(left_event).rsplit(".", 1)[-1]
        if left_token == "NeowEvent":
            left_token = "NEOW"
        if normalize_event_id(left_token) != normalize_event_id(right_event):
            result["$.event_id"] = (left_event, right_event)
    left_kind, right_kind = original.get("continuation_kind"), simulator.get("continuation_kind")
    left_kind = {
        "GRID": "CARD_REWARD", "CHEST": "TREASURE",
    }.get(str(left_kind).upper(), left_kind)
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
    continuation = continuation_original(payload)
    bottled = continuation.get("bottled_cards")
    if isinstance(bottled, list):
        from sls.content.normalize import normalize_content_id
        deck = game.get("deck") or ()
        normalized_bottles = []
        for item in bottled:
            value = dict(item)
            for index, card in enumerate(deck):
                if (
                    normalize_content_id(card.get("id")) == value.get("id")
                    and int(card.get("upgrades", 0) or 0) == int(value.get("upgrades", 0))
                    and int(card.get("misc", card.get("special_data", 0)) or 0)
                    == int(value.get("misc", 0))
                ):
                    value["deck_index"] = index
                    break
            normalized_bottles.append(value)
        continuation["bottled_cards"] = normalized_bottles
        normalizations.append("normalize_continuation.bottled_identity_for_stock_autosave")
    if continuation.get("post_combat"):
        rng = state.get("rng") or {}
        for stream in ("monster_hp", "ai", "shuffle", "card_random", "misc"):
            rng.pop(stream, None)
        normalizations.append("drop_rng.floor_local_after_combat")
    return {
        "state": state,
        "continuation": continuation,
        "normalizations": normalizations,
    }


def resume_verification_boundary(
    payload: Mapping[str, Any], *, ignored_evidence_codes: Iterable[str] = (),
) -> dict[str, Any]:
    """Build an anchor identity while omitting only proven legacy evidence gaps."""

    value = resumable_original_boundary(payload)
    ignored = set(ignored_evidence_codes)
    if "MISSING_EVENT_PHASE" in ignored:
        value.get("continuation", {}).pop("event_phase", None)
    if {"MISSING_MONSTER_INTENTS", "INCOMPLETE_MONSTER_INTENTS"} & ignored:
        combat = value["state"].get("combat")
        if combat:
            combat["monsters"] = [
                tuple(item[:4]) + tuple(item[5:]) for item in combat.get("monsters", ())
            ]
    if {
        "MISSING_ADJUSTED_MONSTER_INTENT_DAMAGE",
        "UNSETTLED_ADJUSTED_MONSTER_INTENT_DAMAGE",
    } & ignored:
        combat = value["state"].get("combat")
        if combat:
            combat["monsters"] = [
                tuple(item[:5]) + tuple(item[7:]) for item in combat.get("monsters", ())
            ]
    return value


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
        status = subprocess.run(
            ["git", "status", "--porcelain=v1", "-z"], cwd=root, check=True,
            capture_output=True,
        ).stdout
        untracked = subprocess.run(
            ["git", "ls-files", "--others", "--exclude-standard", "-z"],
            cwd=root, check=True, capture_output=True,
        ).stdout.split(b"\0")
        digest = hashlib.sha256(diff)
        for encoded in sorted(item for item in untracked if item):
            digest.update(b"\0UNTRACKED\0" + encoded + b"\0")
            candidate = root / os.fsdecode(encoded)
            if candidate.is_file():
                digest.update(candidate.read_bytes())
        return commit, digest.hexdigest(), bool(status)
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
    capability: str = "NATIVE_ONLY"


class TruthBundleRecorder:
    """Collect one run and atomically finalize its immutable truth bundle."""

    def __init__(
        self, root: Path, *, seed: int, profile_id: str, policy_id: str,
        evidence_class: str = "LIVE_FULLRUN", repository_root: Path | None = None,
        jar_paths: Mapping[str, Path] | None = None, autosave: Path | None = None,
        capture_mode: str = "PAIRED", acceptance_eligible: bool | None = None,
        instrumentation_schema: str = "spirecomm-parity-v1",
        launch: Mapping[str, Any] | None = None,
        native_build: Mapping[str, Any] | None = None,
        policy_hash: str | None = None,
        provenance: Mapping[str, Any] | None = None,
    ) -> None:
        if evidence_class not in EVIDENCE_CLASSES:
            raise ValueError(f"unknown evidence class: {evidence_class}")
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
        if capture_mode not in {"PAIRED", "ORIGINAL_SURVEY"}:
            raise ValueError(f"unknown capture mode: {capture_mode}")
        self.final_path = root / f"{stamp}-seed-{seed}"
        self.path = Path(str(self.final_path) + ".partial")
        self.path.mkdir(parents=True, exist_ok=False)
        self.seed, self.profile_id, self.policy_id = seed, profile_id, policy_id
        self.evidence_class = evidence_class
        self.capture_mode = capture_mode
        self.acceptance_eligible = (
            evidence_class == "LIVE_FULLRUN" and capture_mode == "PAIRED"
            if acceptance_eligible is None else bool(acceptance_eligible)
        )
        self.instrumentation_schema = instrumentation_schema
        self.launch = dict(launch or {})
        self.native_build = dict(native_build or {})
        self.policy_hash = policy_hash
        self.provenance = dict(provenance or {})
        self.repository_root = repository_root or Path.cwd()
        self.jar_paths = dict(jar_paths or {})
        self.autosave = autosave
        self._autosave_last_hash = file_hash(autosave) if autosave and autosave.is_file() else None
        self._boundary_stage = self.path / "boundaries.jsonl.partial"
        self._protocol_stage = self.path / "protocol.jsonl.partial"
        self._update_stage = self.path / "boundary-updates.jsonl.partial"
        self._boundary_count = 0
        self._protocol_count = 0
        self._first_boundary: dict[str, Any] | None = None
        self._last_boundary: dict[str, Any] | None = None
        self.anchors: list[Anchor] = []
        self._last_anchor_cursor: tuple[Any, ...] | None = None
        write_json(self.path / "recording.json", {
            "schema": "sls-original-recording-v1", "status": "RECORDING",
            "seed": seed, "profile_id": profile_id, "capture_mode": capture_mode,
            "policy_id": policy_id, "evidence_class": evidence_class,
            "acceptance_eligible": self.acceptance_eligible,
            "instrumentation_schema": instrumentation_schema,
            "launch": self.launch, "native_build": self.native_build,
            "policy_hash": policy_hash, "provenance": self.provenance,
            "created_at": datetime.now(timezone.utc).isoformat(),
        })

    def record_protocol(self, direction: str, data: Any) -> None:
        append_jsonl(self._protocol_stage, {
            "sequence": self._protocol_count, "direction": direction, "data": data,
        }, durable=False)
        self._protocol_count += 1

    def record_boundary(
        self, *, sequence: int, original_payload: Mapping[str, Any], original_decision: Any,
        simulator_state: Mapping[str, Any], simulator_decision: Any,
        action: Any | None, commands: Iterable[str], observation_diff: Mapping[str, Any],
        action_diff: Mapping[str, Any], state_diff: Mapping[str, Any], checkpoint: Mapping[str, Any],
        terminal_kind: str | None = None,
    ) -> dict[str, Any]:
        previous_boundary = self._last_boundary
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
            "action_evidence": {},
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
                preceding_action=None if previous_boundary is None else (
                    (previous_boundary.get("selected_action") or {}).get("kind")
                ),
            ),
            "terminal_kind": terminal_kind,
        }
        from sls.validation.evidence import comparison_result, original_evidence_gaps
        preceding_action = None if previous_boundary is None else (
            (previous_boundary.get("selected_action") or {}).get("kind")
        )
        gaps = original_evidence_gaps(
            original_payload, canonical_screen=observation.screen.value,
        )
        record["comparison"] = comparison_result(
            evidence_class=self.evidence_class, profile=self.profile_id,
            screen=observation.screen.value, act=observation.run.act,
            floor=observation.run.floor, differences=all_differences,
            evidence_gaps=gaps, preceding_action=preceding_action,
            occurrence_signature=record["difference_signature"] if not gaps else None,
        )
        if gaps:
            record["difference_signature"] = record["comparison"]["occurrence_signature"]
        append_jsonl(self._boundary_stage, record)
        sync_file(self._protocol_stage)
        if self._first_boundary is None:
            self._first_boundary = record
        self._last_boundary = record
        self._boundary_count += 1
        cursor = (observation.run.act, observation.run.floor)
        anchor_kind = "RUN_START" if sequence == 0 else "ENTER_ROOM" if cursor != self._last_anchor_cursor else None
        if (
            observation.screen.value in {"COMBAT_REWARD", "BOSS_REWARD", "ACT_TRANSITION"}
            and (
                previous_boundary is None
                or previous_boundary["cursor"]["screen"] != observation.screen.value
            )
        ):
            anchor_kind = observation.screen.value
        if anchor_kind:
            self._create_anchor(
                sequence, anchor_kind, record["original_boundary_hash"],
                value_hash(resumable_original_boundary(original_payload)), checkpoint,
            )
            self._last_anchor_cursor = cursor
        return record

    def record_survey_boundary(
        self, *, sequence: int, original_payload: Mapping[str, Any],
        original_decision: Any, action: Any | None, commands: Iterable[str],
        terminal_kind: str | None = None,
    ) -> dict[str, Any]:
        if self.capture_mode != "ORIGINAL_SURVEY":
            raise RuntimeError("survey boundaries require ORIGINAL_SURVEY capture mode")
        payload = json.loads(json.dumps(original_payload, ensure_ascii=False))
        observation = original_decision.observation
        continuation = continuation_original(payload)
        canonical = canonical_original(payload)
        record = {
            "schema": BOUNDARY_SCHEMA, "sequence": sequence,
            "cursor": {
                "act": observation.run.act, "floor": observation.run.floor,
                "room": continuation.get("room_class"), "screen": observation.screen.value,
                "combat_turn": continuation.get("combat_turn"),
            },
            "raw_original_payload": payload,
            "canonical_original_decision": {
                "observation": observation.to_dict(),
                "actions": [candidate.to_dict() for candidate in original_decision.actions],
                "terminal": original_decision.terminal,
            },
            "canonical_simulator_decision": None, "canonical_public_state": canonical,
            "rng": payload.get("_rng") or {}, "math_seed": payload.get("math_seed"),
            "continuation": {"original": continuation, "simulator": None},
            "candidates": [candidate.to_dict() for candidate in original_decision.actions],
            "selected_action": None if action is None else action.to_dict(),
            "action_evidence": {}, "commands": list(commands), "action_executed": False,
            "original_boundary_hash": value_hash({"state": canonical, "continuation": continuation}),
            "simulator_boundary_hash": None, "differences": {}, "difference_signature": None,
            "comparison": {
                "status": "INCONCLUSIVE", "category": "EVIDENCE_GAP", "differences": {},
                "evidence_gaps": [{"code": "SURVEY_NOT_PAIRED", "path": "$"}],
                "occurrence_signature": None, "cluster_key": None,
            },
            "terminal_kind": terminal_kind,
        }
        append_jsonl(self._boundary_stage, record)
        sync_file(self._protocol_stage)
        if self._first_boundary is None:
            self._first_boundary = record
        self._last_boundary = record
        self._boundary_count += 1
        cursor = (observation.run.act, observation.run.floor)
        if sequence > 0 and cursor != self._last_anchor_cursor:
            self._create_survey_anchor(sequence, record)
        self._last_anchor_cursor = cursor
        return record

    def _create_survey_anchor(self, sequence: int, record: Mapping[str, Any]) -> None:
        anchor_id = f"a{len(self.anchors):04d}-s{sequence:06d}-enter_room"
        target = self.path / "anchors" / anchor_id
        target.mkdir(parents=True)
        copied: dict[str, str] = {}
        identity = None
        if self.autosave and stable_file(self.autosave):
            current_hash = file_hash(self.autosave)
            try:
                candidate = autosave_identity(self.autosave)
            except (OSError, ValueError, KeyError, json.JSONDecodeError):
                candidate = None
            cursor = record["cursor"]
            if (candidate and current_hash != self._autosave_last_hash
                    and candidate["seed"] == self.seed
                    and candidate["floor"] == int(cursor["floor"])):
                shutil.copy2(self.autosave, target / "original.autosave")
                copied["original.autosave"] = file_hash(target / "original.autosave")
                identity, self._autosave_last_hash = candidate, current_hash
        metadata = {
            "schema": "sls-original-anchor-v1", "anchor_id": anchor_id,
            "sequence": sequence, "kind": "ENTER_ROOM",
            "boundary_hash": record["original_boundary_hash"],
            "resume_boundary_hash": value_hash(resumable_original_boundary(record["raw_original_payload"])),
            "resume_normalizations": resumable_original_boundary(record["raw_original_payload"])["normalizations"],
            "files": copied, "save_identity": identity,
            "capability": "OFFICIAL_SAVE_ONLY" if copied else "NATIVE_ONLY",
        }
        write_json(target / "metadata.json", metadata)
        self.anchors.append(Anchor(
            anchor_id, sequence, "ENTER_ROOM", record["original_boundary_hash"],
            metadata["resume_boundary_hash"], str(target.relative_to(self.path)),
            metadata["capability"],
        ))

    def mark_last_action_executed(
        self, commands: Iterable[str], action_evidence: Mapping[str, Any] | None = None,
    ) -> None:
        if self._last_boundary is None:
            raise RuntimeError("no truth boundary is available")
        values = list(commands)
        self._last_boundary["commands"] = values
        self._last_boundary["action_executed"] = True
        evidence = dict(action_evidence or {})
        self._last_boundary["action_evidence"] = evidence
        append_jsonl(self._update_stage, {
            "sequence": self._last_boundary["sequence"], "commands": values,
            "action_executed": True, "action_evidence": evidence,
        })

    def mark_initial_resume_verified(
        self, autosave_source: Path, backup_source: Path | None = None,
        *, source_run_id: str | None = None, source_anchor_id: str | None = None,
    ) -> None:
        """Promote a successfully compared sequence-zero resume to a portable anchor."""

        if self._last_boundary is None or int(self._last_boundary["sequence"]) != 0:
            raise RuntimeError("initial resume anchor must be promoted at sequence zero")
        if len(self.anchors) != 1 or self.anchors[0].sequence != 0:
            raise RuntimeError("initial native anchor is unavailable")
        identity = autosave_identity(autosave_source)
        cursor = self._last_boundary["cursor"]
        if (
            identity["seed"] != self.seed
            or identity["floor"] != int(cursor["floor"])
            or identity["character"] != "IRONCLAD"
        ):
            raise ValueError("verified resume save identity does not match sequence zero")
        anchor = self.anchors[0]
        target = self.path / anchor.path
        save_target = target / "original.autosave"
        shutil.copy2(autosave_source, save_target)
        files = {"original.autosave": file_hash(save_target)}
        if backup_source is not None and backup_source.is_file():
            backup_target = target / "original.autosave.backUp"
            shutil.copy2(backup_source, backup_target)
            files["original.autosave.backUp"] = file_hash(backup_target)
        metadata_path = target / "metadata.json"
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        metadata.update({
            "capability": "RESUME_VERIFIED", "files": files,
            "save_identity": identity,
            "derived_from": {
                "source_run_id": source_run_id, "source_anchor_id": source_anchor_id,
            },
        })
        write_json(metadata_path, metadata)
        self.anchors[0] = Anchor(
            anchor.anchor_id, anchor.sequence, anchor.kind, anchor.boundary_hash,
            anchor.resume_boundary_hash, anchor.path, "RESUME_VERIFIED",
        )

    def select_last_action(self, action: Any, commands: Iterable[str]) -> None:
        if self._last_boundary is None:
            raise RuntimeError("no truth boundary is available")
        action_value = action.to_dict()
        values = list(commands)
        self._last_boundary["selected_action"] = action_value
        self._last_boundary["commands"] = values
        append_jsonl(self._update_stage, {
            "sequence": self._last_boundary["sequence"],
            "selected_action": action_value, "commands": values,
        })

    def _create_anchor(
        self, sequence: int, kind: str, boundary_hash: str,
        resume_boundary_hash: str, checkpoint: Mapping[str, Any],
    ) -> None:
        anchor_id = f"a{len(self.anchors):04d}-s{sequence:06d}-{kind.lower()}"
        target = self.path / "anchors" / anchor_id
        target.mkdir(parents=True)
        write_json_gz(target / "simulator-checkpoint.json.gz", checkpoint)
        copied: dict[str, str] = {}
        save_identity: dict[str, Any] | None = None
        if kind != "RUN_START" and self.autosave and stable_file(self.autosave):
            current_hash = file_hash(self.autosave)
            try:
                identity = autosave_identity(self.autosave)
            except (OSError, ValueError, KeyError, json.JSONDecodeError):
                identity = None
            cursor = self._last_boundary["cursor"]
            identity_matches = bool(
                identity
                and identity["seed"] == self.seed
                and identity["floor"] == int(cursor["floor"])
                and identity["character"] == "IRONCLAD"
                and (
                    not identity["room_class"]
                    or identity["room_class"] == cursor["room"]
                )
            )
            if current_hash != self._autosave_last_hash and identity_matches:
                shutil.copy2(self.autosave, target / "original.autosave")
                copied["original.autosave"] = file_hash(target / "original.autosave")
                save_identity = identity
                self._autosave_last_hash = current_hash
            backup = Path(str(self.autosave) + ".backUp")
            if copied and backup.is_file() and stable_file(backup):
                shutil.copy2(backup, target / "original.autosave.backUp")
                copied["original.autosave.backUp"] = file_hash(target / "original.autosave.backUp")
        metadata = {
            "schema": "sls-original-anchor-v1", "anchor_id": anchor_id,
            "sequence": sequence, "kind": kind, "boundary_hash": boundary_hash,
            "resume_boundary_hash": resume_boundary_hash,
            "resume_normalizations": resumable_original_boundary(
                self._last_boundary["raw_original_payload"]
            )["normalizations"],
            "files": copied, "save_identity": save_identity,
            "checkpoint_producer": self.native_build,
            "checkpoint_state_hash": value_hash(checkpoint),
        }
        write_json(target / "metadata.json", metadata)
        capability = "OFFICIAL_SAVE_AND_NATIVE" if copied.get("original.autosave") else "NATIVE_ONLY"
        metadata["capability"] = capability
        write_json(target / "metadata.json", metadata)
        self.anchors.append(Anchor(
            anchor_id, sequence, kind, boundary_hash, resume_boundary_hash,
            str(target.relative_to(self.path)), capability,
        ))

    def finalize(self, *, complete: bool, outcome: str | None, error: str | None) -> Path:
        boundaries_values = read_jsonl(self._boundary_stage)
        update_values = read_jsonl(self._update_stage) if self._update_stage.is_file() else []
        updates: dict[int, dict[str, Any]] = {}
        for value in update_values:
            updates.setdefault(int(value["sequence"]), {}).update(value)
        for index, boundary in enumerate(boundaries_values):
            if int(boundary.get("sequence", -1)) != index:
                raise ValueError(f"missing or unordered staged boundary at {index}")
            if index in updates:
                boundary.update({key: value for key, value in updates[index].items() if key != "sequence"})
        protocol_values = read_jsonl(self._protocol_stage) if self._protocol_stage.is_file() else []
        boundaries = self.path / "boundaries.jsonl.gz"
        protocol = self.path / "protocol.jsonl.gz"
        write_jsonl_gz(boundaries, boundaries_values)
        write_jsonl_gz(protocol, protocol_values)
        commit, dirty_hash, dirty = _git_metadata(self.repository_root)
        jar_hashes = {
            name: {"path": str(path), "sha256": file_hash(path)}
            for name, path in sorted(self.jar_paths.items()) if path.is_file()
        }
        code_files = {
            "adapter": self.repository_root / "src" / "sls" / "backends" / "original" / "adapter.py",
            "canonicalizer": self.repository_root / "src" / "sls" / "validation" / "compare.py",
        }
        code_hashes = {
            name: file_hash(path) for name, path in code_files.items() if path.is_file()
        }
        observed_instrumentation = sorted({
            str(boundary["raw_original_payload"].get("_parity_schema"))
            for boundary in boundaries_values if boundary["raw_original_payload"].get("_parity_schema")
        })
        artifacts: dict[str, str] = {
            "boundaries.jsonl.gz": file_hash(boundaries),
            "protocol.jsonl.gz": file_hash(protocol),
        }
        for metadata in sorted((self.path / "anchors").glob("*/metadata.json")) if (self.path / "anchors").exists() else ():
            artifacts[str(metadata.relative_to(self.path)).replace("\\", "/")] = file_hash(metadata)
            checkpoint = metadata.parent / "simulator-checkpoint.json.gz"
            if checkpoint.is_file():
                artifacts[str(checkpoint.relative_to(self.path)).replace("\\", "/")] = file_hash(checkpoint)
            for save in metadata.parent.glob("original.autosave*"):
                artifacts[str(save.relative_to(self.path)).replace("\\", "/")] = file_hash(save)
        manifest = {
            "schema": TRUTH_BUNDLE_SCHEMA, "seed": self.seed,
            "profile_id": self.profile_id, "policy_id": self.policy_id,
            "evidence_class": self.evidence_class,
            "capture_mode": self.capture_mode,
            "acceptance_eligible": self.acceptance_eligible and not dirty and complete,
            "instrumentation": {
                "schema": self.instrumentation_schema,
                "observed_schemas": observed_instrumentation,
                "sha256": (jar_hashes.get("Oracle") or {}).get("sha256"),
            },
            "code": code_hashes,
            "policy": {"id": self.policy_id, "sha256": self.policy_hash},
            "native_build": self.native_build,
            "launch": self.launch,
            "provenance": self.provenance,
            "started_at": self.path.name.split("-seed-")[0],
            "finished_at": datetime.now(timezone.utc).isoformat(),
            "git": {"commit": commit, "dirty": dirty, "dirty_diff_hash": dirty_hash},
            "jars": jar_hashes,
            "python": {"executable": sys.executable, "version": sys.version},
            "trace_schema": BOUNDARY_SCHEMA, "checkpoint_schema": CHECKPOINT_SCHEMA,
            "anchors": [asdict(anchor) for anchor in self.anchors],
            "restore_mode": "EXACT_CHECKPOINT",
            "segments": [{"start": 0, "end": max(0, len(boundaries_values) - 1)}] if boundaries_values else [],
            "start_state": None if not boundaries_values else {
                "cursor": boundaries_values[0]["cursor"],
                "boundary_hash": boundaries_values[0]["original_boundary_hash"],
            },
            "end_state": None if not boundaries_values else {
                "cursor": boundaries_values[-1]["cursor"],
                "boundary_hash": boundaries_values[-1]["original_boundary_hash"],
            },
            "complete": complete, "aborted": False, "outcome": outcome, "error": error,
            "termination_reason": outcome or error or ("COMPLETE" if complete else "STOPPED"),
            "artifacts": artifacts,
        }
        write_json(self.path / "manifest.json", manifest)
        for staged in (self._boundary_stage, self._protocol_stage, self._update_stage, self.path / "recording.json"):
            if staged.exists():
                staged.unlink()
        os.replace(self.path, self.final_path)
        self.path = self.final_path
        return self.final_path


def load_bundle(path: Path, *, verify: bool = True) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    manifest_path = path / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("schema") not in {TRUTH_BUNDLE_SCHEMA, LEGACY_TRUTH_BUNDLE_SCHEMA}:
        raise ValueError(f"unsupported truth bundle schema: {manifest.get('schema')}")
    if verify:
        for relative, expected in manifest.get("artifacts", {}).items():
            artifact = path / relative
            if not artifact.is_file() or file_hash(artifact) != expected:
                raise ValueError(f"truth artifact hash mismatch: {relative}")
    boundaries = read_jsonl_gz(path / "boundaries.jsonl.gz")
    for boundary in boundaries:
        if boundary.get("schema") not in {BOUNDARY_SCHEMA, LEGACY_BOUNDARY_SCHEMA}:
            raise ValueError(f"unsupported boundary schema: {boundary.get('schema')}")
        # Prove raw wire truth remains parseable. Its newly adapted result is
        # deliberately allowed to differ from the historical canonical cache.
        adapt_original(boundary["raw_original_payload"])
    return manifest, boundaries


def recover_partial_bundle(path: Path) -> Path:
    """Finalize a crashed staging directory as an explicitly aborted v2 bundle."""

    if not path.name.endswith(".partial") or not path.is_dir():
        raise ValueError("partial bundle path must end with .partial")
    recording = json.loads((path / "recording.json").read_text(encoding="utf-8"))
    boundaries = read_jsonl(path / "boundaries.jsonl.partial", tolerate_truncated_tail=True)
    if not boundaries:
        raise ValueError("partial bundle has no complete boundary")
    for index, boundary in enumerate(boundaries):
        if int(boundary.get("sequence", -1)) != index:
            raise ValueError(f"partial bundle has a sequence gap at {index}")
    updates_path = path / "boundary-updates.jsonl.partial"
    updates: dict[int, dict[str, Any]] = {}
    for value in (read_jsonl(updates_path, tolerate_truncated_tail=True) if updates_path.is_file() else []):
        updates.setdefault(int(value["sequence"]), {}).update(value)
    for index, update in updates.items():
        if index >= len(boundaries):
            raise ValueError(f"partial update references missing boundary {index}")
        boundaries[index].update({key: value for key, value in update.items() if key != "sequence"})
    protocol = read_jsonl(
        path / "protocol.jsonl.partial", tolerate_truncated_tail=True,
    ) if (path / "protocol.jsonl.partial").is_file() else []
    write_jsonl_gz(path / "boundaries.jsonl.gz", boundaries)
    write_jsonl_gz(path / "protocol.jsonl.gz", protocol)
    artifacts = {
        "boundaries.jsonl.gz": file_hash(path / "boundaries.jsonl.gz"),
        "protocol.jsonl.gz": file_hash(path / "protocol.jsonl.gz"),
    }
    anchors = []
    anchor_root = path / "anchors"
    for metadata_path in sorted(anchor_root.glob("*/metadata.json")) if anchor_root.exists() else ():
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        anchors.append({key: metadata.get(key) for key in (
            "anchor_id", "sequence", "kind", "boundary_hash", "resume_boundary_hash", "capability",
        )} | {"path": str(metadata_path.parent.relative_to(path))})
        for artifact in metadata_path.parent.iterdir():
            if artifact.is_file():
                artifacts[str(artifact.relative_to(path)).replace("\\", "/")] = file_hash(artifact)
    manifest = {
        "schema": TRUTH_BUNDLE_SCHEMA, "seed": recording["seed"],
        "profile_id": recording["profile_id"],
        "policy_id": recording.get("policy_id", "unknown-after-crash"),
        "evidence_class": recording.get("evidence_class", "LIVE_FULLRUN"),
        "capture_mode": recording["capture_mode"],
        "acceptance_eligible": False, "anchors": anchors,
        "instrumentation": {"schema": recording.get("instrumentation_schema")},
        "launch": recording.get("launch") or {},
        "native_build": recording.get("native_build") or {},
        "policy": {
            "id": recording.get("policy_id", "unknown-after-crash"),
            "sha256": recording.get("policy_hash"),
        },
        "provenance": recording.get("provenance") or {},
        "trace_schema": BOUNDARY_SCHEMA, "checkpoint_schema": CHECKPOINT_SCHEMA,
        "segments": [{"start": 0, "end": len(boundaries) - 1}],
        "start_state": {
            "cursor": boundaries[0]["cursor"],
            "boundary_hash": boundaries[0]["original_boundary_hash"],
        },
        "end_state": {
            "cursor": boundaries[-1]["cursor"],
            "boundary_hash": boundaries[-1]["original_boundary_hash"],
        },
        "complete": False, "aborted": True, "outcome": None,
        "error": "recovered from interrupted recording",
        "termination_reason": "PROCESS_INTERRUPTED", "artifacts": artifacts,
        "recovery": {"source": path.name, "recovered_at": datetime.now(timezone.utc).isoformat()},
    }
    write_json(path / "manifest.json", manifest)
    for staged in path.glob("*.partial"):
        staged.unlink()
    (path / "recording.json").unlink(missing_ok=True)
    target = Path(str(path)[:-len(".partial")] + "-aborted")
    os.replace(path, target)
    return target
