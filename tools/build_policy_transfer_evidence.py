"""Build a signed, current-code policy-transfer evidence index.

The index is intentionally derived from immutable Original truth payloads and
fresh simulator replay.  It is not a declaration file: any public observation,
candidate action, or terminal mismatch aborts generation.
"""

from __future__ import annotations

import argparse
from dataclasses import asdict
import json
from pathlib import Path
import sys
import tomllib
import subprocess

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from replay_truth import replay  # noqa: E402
from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash  # noqa: E402
from sls.content.semantic_audit import (  # noqa: E402
    CARD_SEMANTIC_AUDIT_PATH, ENCOUNTER_SEMANTIC_AUDIT_PATH,
    EVENT_SEMANTIC_AUDIT_PATH, MECHANISM_SEMANTIC_AUDIT_PATH,
    POTION_SEMANTIC_AUDIT_PATH, RELIC_SEMANTIC_AUDIT_PATH,
    load_card_semantic_audit, load_encounter_semantic_audit,
    load_event_semantic_audit, load_mechanism_semantic_audit,
    load_potion_semantic_audit, load_relic_semantic_audit,
)  # noqa: E402
from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash  # noqa: E402
from sls.rl.training_contract import (  # noqa: E402
    canonical_digest, native_source_digest, source_sha256,
)
from sls.validation.readiness import readiness_report  # noqa: E402
from sls.validation.transfer import compare_distributions  # noqa: E402
from sls.validation.transfer_gate import STOCHASTIC_CATEGORIES  # noqa: E402


EVIDENCE_SCHEMA = "sls-policy-transfer-evidence-v1"


def _relative(path: Path) -> str:
    return path.resolve().relative_to(ROOT.resolve()).as_posix()


def _replay_segment(bundle: Path, start: int, end: int) -> None:
    matched, detail = replay(
        bundle, from_step=start, to_step=end, public_only=True,
    )
    if not matched:
        raise ValueError(f"public contract mismatch in {bundle.name}: {detail}")


def _git_commit() -> str:
    status = subprocess.run(
        ["git", "status", "--porcelain"], cwd=ROOT, check=True,
        capture_output=True, text=True,
    ).stdout
    if status.strip():
        raise ValueError("policy-transfer evidence can only be built from a clean repository")
    return subprocess.run(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, check=True,
        capture_output=True, text=True,
    ).stdout.strip()


def _stochastic_evidence(path: Path) -> dict[str, object]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("schema") != "sls-stochastic-samples-v1":
        raise ValueError("unsupported stochastic sample artifact")
    supplied = payload.get("samples_sha256")
    unsigned = dict(payload)
    unsigned.pop("samples_sha256", None)
    if supplied != canonical_digest(unsigned):
        raise ValueError("stochastic sample artifact digest mismatch")
    categories = dict(payload.get("categories") or {})
    if set(categories) != STOCHASTIC_CATEGORIES:
        raise ValueError("stochastic sample artifact lacks required categories")
    result = {}
    for name, raw in categories.items():
        original, simulator = dict(raw["original"]), dict(raw["simulator"])
        comparison = compare_distributions(
            original.get("samples") or (), simulator.get("samples") or (),
            maximum_total_variation=0.05, confidence=0.95,
            bootstrap_resamples=10_000, bootstrap_seed=17,
        )
        result[name] = {
            **asdict(comparison),
            "original_seeds": len(set(map(int, original.get("seeds") or ()))),
            "simulator_seeds": len(set(map(int, simulator.get("seeds") or ()))),
            "sample_artifact": _relative(path),
            "sample_artifact_sha256": source_sha256(path),
        }
    return result


def build_evidence(
    truth_root: Path, stochastic_report: Path, canary_report: Path | None,
) -> dict[str, object]:
    with (ROOT / "configs/validation/act1_training.toml").open("rb") as stream:
        requirements = tomllib.load(stream)["requirements"]
    readiness = readiness_report(truth_root, requirements)
    if not readiness["ready"]:
        raise ValueError(f"Act 1 truth routes are incomplete: {readiness['failures']}")

    selected_routes = []
    for boss in map(str, requirements["bosses"]):
        candidates = [
            route for route in readiness["valid_routes"]
            if boss in route["coverage"]["bosses"]
        ]
        if not candidates:
            raise ValueError(f"no current public-contract route covers {boss}")
        selected_routes.append(max(candidates, key=lambda item: item["used_boundaries"]))

    route_rows = []
    route_boundaries = 0
    for route in selected_routes:
        segments = []
        for segment in route["segments"]:
            bundle = truth_root / str(segment["bundle"])
            start, end = int(segment["from_step"]), int(segment["to_step"])
            _replay_segment(bundle, start, end)
            manifest = bundle / "manifest.json"
            count = end - start + 1
            route_boundaries += count
            segments.append({
                "bundle": bundle.name, "from_step": start, "to_step": end,
                "boundaries": count, "manifest_sha256": source_sha256(manifest),
            })
        route_rows.append({
            "seed": route["seed"], "bosses": route["coverage"]["bosses"],
            "screens": route["coverage"]["screens"],
            "selected_actions": route["coverage"]["selected_actions"],
            "segments": segments,
        })

    stochastic = _stochastic_evidence(stochastic_report)
    if not all(bool(dict(record).get("accepted")) for record in stochastic.values()):
        raise ValueError("one or more stochastic distributions exceed the acceptance limit")
    canary = None
    if canary_report is not None:
        canary = json.loads(canary_report.read_text(encoding="utf-8"))
        supplied_canary_digest = canary.get("report_sha256")
        unsigned_canary = dict(canary)
        unsigned_canary.pop("report_sha256", None)
        if supplied_canary_digest != canonical_digest(unsigned_canary) or not canary.get("accepted"):
            raise ValueError("policy canary is unsigned or not accepted")

    audit_loaders = (
        ("cards", CARD_SEMANTIC_AUDIT_PATH, load_card_semantic_audit),
        ("potions", POTION_SEMANTIC_AUDIT_PATH, load_potion_semantic_audit),
        ("relics", RELIC_SEMANTIC_AUDIT_PATH, load_relic_semantic_audit),
        ("events", EVENT_SEMANTIC_AUDIT_PATH, load_event_semantic_audit),
        ("mechanisms", MECHANISM_SEMANTIC_AUDIT_PATH, load_mechanism_semantic_audit),
        ("encounters", ENCOUNTER_SEMANTIC_AUDIT_PATH, load_encounter_semantic_audit),
    )
    deterministic = {}
    for name, path, loader in audit_loaders:
        audit = loader(require_current=True)
        deterministic[name] = {
            "path": _relative(path), "audit_sha256": audit["audit_sha256"],
            "entries": len(audit["entries"]),
        }

    source_paths = (
        ROOT / "src/sls/backends/original/adapter.py",
        ROOT / "src/sls/backends/simulator/environment.py",
        ROOT / "src/sls/contracts/observation.py",
        ROOT / "tools/replay_truth.py",
    )
    payload: dict[str, object] = {
        "schema": EVIDENCE_SCHEMA,
        "profile": "IRONCLAD_A0_ACT1",
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "native_source_sha256": native_source_digest(),
        "git_commit": _git_commit(),
        "source_files": {_relative(path): source_sha256(path) for path in source_paths},
        "public_contract": {
            "accepted": True, "boundaries": route_boundaries,
            "bosses": sorted({boss for row in route_rows for boss in row["bosses"]}),
            "screens": sorted({screen for row in route_rows for screen in row["screens"]}),
            "selected_actions": sorted({
                action for row in route_rows for action in row["selected_actions"]
            }),
            "routes": route_rows,
        },
        "stochastic_distributions": stochastic,
        "deterministic_mechanisms": deterministic,
    }
    if canary is not None and canary_report is not None:
        payload["policy_canary"] = {
            "path": _relative(canary_report),
            "report_sha256": source_sha256(canary_report),
            "artifact_sha256": canary["artifact_sha256"],
        }
    payload["evidence_sha256"] = canonical_digest(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--truth-root", type=Path, default=ROOT / "validation-results/truth",
    )
    parser.add_argument(
        "--output", type=Path,
        default=ROOT / "runs/policy_transfer_evidence_v1.json",
    )
    parser.add_argument(
        "--gate-template", type=Path,
        default=ROOT / "configs/validation/policy_transfer_v1.json",
    )
    parser.add_argument(
        "--gate-output", type=Path,
        default=ROOT / "runs/policy_transfer_v1.json",
    )
    parser.add_argument("--stochastic-report", type=Path, required=True)
    parser.add_argument("--canary-report", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    payload = build_evidence(args.truth_root, args.stochastic_report, args.canary_report)
    text = json.dumps(payload, indent=2, sort_keys=True) + "\n"
    gate = json.loads(args.gate_template.read_text(encoding="utf-8"))
    gate.pop("gate_sha256", None)
    gate["evidence"] = _relative(args.output)
    gate["evidence_sha256"] = payload["evidence_sha256"]
    gate["gate_sha256"] = canonical_digest(gate)
    gate_text = json.dumps(gate, indent=2, sort_keys=True) + "\n"
    if args.check:
        if not args.output.is_file() or args.output.read_text(encoding="utf-8") != text:
            raise SystemExit(f"stale policy-transfer evidence: {args.output}")
        if not args.gate_output.is_file() or args.gate_output.read_text(encoding="utf-8") != gate_text:
            raise SystemExit(f"stale policy-transfer gate: {args.gate_output}")
        print(f"policy-transfer evidence: OK ({payload['evidence_sha256']})")
        return 0
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(text, encoding="utf-8")
    temporary.replace(args.output)
    args.gate_output.parent.mkdir(parents=True, exist_ok=True)
    gate_temporary = args.gate_output.with_suffix(args.gate_output.suffix + ".tmp")
    gate_temporary.write_text(gate_text, encoding="utf-8")
    gate_temporary.replace(args.gate_output)
    print(args.gate_output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
