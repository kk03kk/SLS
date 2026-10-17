"""Qualify the existing finite Act1–2 corpus on the current simulator source.

No game launch, training, selection rebinding, or historical evidence rewrite.
"""

from __future__ import annotations

import argparse
import json
import xml.etree.ElementTree as ET
from pathlib import Path

from sls.audit.act12_qualification import DISCOVERY, PROFILE, assess
from sls.audit.validation_scope import file_identity, natural_witnesses
from sls.rl.training_contract import (
    ROOT,
    local_source_digest,
    native_artifact,
    native_source_digest,
    sha256_file,
)

GATE_SOURCES = ("src/sls/audit/act12_qualification.py", "src/sls/audit/validation_scope.py",
                "src/sls/audit/act2_differential.py", "src/sls/audit/corpse_cleanup.py",
                "src/sls/audit/semantic_actions.py", "tools/qualify_normalstart_act12.py", "tools/qualify_act12_foundation.py",
                "tools/check_act2_production_coverage.py", "tools/verify_act12_foundation.py",
                "tools/replay_act2_encounter_batch.py", "tools/replay_act2_system_batch.py",
                "tools/replay_act2_production_trajectory.py", "tools/replay_act2_clock_conditioned_trajectory.py")
GATE_SOURCES += ('src/sls/audit/public_projection_migration.py','src/sls/backends/original/adapter.py')
GATE_SOURCES += ('tools/revalidate_act12_simulator.py',)


def gate_digest():
    return local_source_digest(GATE_SOURCES)


def recovered_capture(path):
    """Verify historical backups; never compare/restore today's active game files."""
    from tools.verify_oracle import read_verified_launch
    launch_path = path.with_suffix(".launch.json")
    launch = read_verified_launch(launch_path)
    journal_path = Path(launch["recovery_journal"])
    journal = json.loads(journal_path.read_text(encoding="utf-8"))
    if (journal.get("status") != "RECOVERED" or journal.get("recovery_failures")
            or launch.get("recovery_status") != "RECOVERED" or launch.get("execution_error")
            or launch.get("completion", {}).get("exit_code") != 0):
        raise ValueError(f"original recovery/launch incomplete: {path}")
    for entry in journal["entries"]:
        if entry["existed"] and sha256_file(Path(entry["backup"])) != entry["sha256"]:
            raise ValueError("historical recovery backup identity mismatch")
    return {"capture": file_identity(path), "launch": file_identity(launch_path),
            "journal": file_identity(journal_path), "status": "RECOVERED",
            "backup_bytes_verified": True, "oracle_sha256": launch["oracle_sha256"]}


def qualify(root: Path, foundation: Path):
    from sls.audit.act2_differential import replay_controlled_run
    from sls.diagnostics.canary import read_trajectory
    from tools.check_act2_production_coverage import satisfies, witnessed_roles

    inputs = {}
    def read(path):
        inputs[str(path.resolve())] = sha256_file(path)
        return json.loads(path.read_text(encoding="utf-8"))
    source = native_source_digest()
    before = gate_digest()
    replay = root / "replay"
    summary = read(replay / "summary.json")
    if (summary["native_source_sha256"] != source or summary.get('comparator_source_sha256')!=before
            or any(row["exit_code"] for row in summary["reports"])):
        raise ValueError("missing or stale current-source replay")
    for report in summary["reports"]:
        entry = report["evidence"]
        if sha256_file(Path(entry["path"])) != entry["sha256"]:
            raise ValueError("registered raw replay changed")
    build = read(Path(summary["oracle_build"]["path"]))
    recovered = []
    for name in ("controlled-stock", "systems-0", "systems-1", "systems-2", "systems-policy-131100063",
                 "gremlin-leader-stock-retry"):
        receipt = recovered_capture(foundation / (name + ".json"))
        if receipt["oracle_sha256"] != build["output_sha256"]:
            raise ValueError("stock evidence has a different Oracle identity")
        recovered.append(receipt)
    selection = read(foundation / "selection-final.json")
    roles = {row["seed"]: row["role"] for row in selection["runs"]}
    witnessed = []
    for item in summary["natural_receipts"]:
        _, receipt = natural_witnesses(Path(item["batch"]["path"]), build["dependencies"]["game"])
        recovered.append({**receipt, "status": "RECOVERED", "backup_bytes_verified": True})
        for trajectory in receipt["trajectories"]:
            metadata, rows = read_trajectory(Path(trajectory["path"]))
            if metadata["policy"]["model_sha256"] != selection["model_sha256"]:
                raise ValueError("stock canary policy differs from fixed selection")
            evidence = witnessed_roles(rows)
            if not satisfies(roles[metadata["seed"]], evidence):
                raise ValueError("stock canary role not witnessed")
            witnessed.append({"seed": metadata["seed"], "role": roles[metadata["seed"]],
                               "trajectory_sha256": trajectory["sha256"], "role_witnessed": True})
    gremlin_capture = read(foundation / "gremlin-leader-stock-retry.json")
    if not gremlin_capture.get("execution_complete") or gremlin_capture.get("execution_error"):
        raise ValueError("supplementary capture incomplete")
    gremlin = {"native_source_sha256": source,
               "stock_capture_sha256": sha256_file(foundation / "gremlin-leader-stock-retry.json"),
               "runs": [replay_controlled_run(row, production_projection=True) for row in gremlin_capture["runs"]]}
    gremlin_path = root / "gremlin-current.json"
    if gremlin_path.exists():
        if read(gremlin_path) != gremlin:
            raise ValueError("supplementary replay changed")
    else:
        gremlin_path.write_text(json.dumps(gremlin, indent=2) + "\n", encoding="utf-8")
    inputs[str(gremlin_path.resolve())] = sha256_file(gremlin_path)
    boundary = read(root / "boundaries.json")
    seed_allocation = read(root / "development-seeds.json")
    if (seed_allocation["profile"] != PROFILE or not seed_allocation["seed_range"][0] <= min(boundary["seeds"])
            or not max(boundary["seeds"]) < seed_allocation["seed_range"][1]):
        raise ValueError("boundary seeds were not registered for this profile")
    natural = [read(replay / f"natural-{seed}-raw.json") for seed in sorted(roles)]
    conditioned = [read(path) for path in sorted(replay.glob("natural-*-conditional.json"))]
    facts = assess(controlled=read(replay / "controlled.json"),
        systems=[read(replay / f"systems-{i}-raw.json") for i in range(3)],
        conditional_systems=[read(replay / f"systems-{i}-conditional.json") for i in range(3)],
        replacement=read(replay / "replacement-131100063-raw.json"),
        conditional_replacement=read(replay / "replacement-131100063-conditional.json"),
        natural=natural, conditional_natural=conditioned, gremlin=gremlin, boundaries=boundary,
        source=source, model_sha256=selection["model_sha256"])
    impact_xml = root / "impact-tests.xml"
    inputs[str(impact_xml.resolve())] = sha256_file(impact_xml)
    cases = list(ET.parse(impact_xml).getroot().iter("testcase"))
    if (not cases or any(case.find(tag) is not None for case in cases for tag in ("failure", "error", "skipped"))):
        raise ValueError("rule-impact regression missing, failed or skipped")
    expected = {"test_chest_key_interactions", "test_dynamic_permanent_deck", "test_revival_boundaries",
                "test_event_constructor_timing", "test_battle_master_deck_restore", "test_large_combat_piles",
                "test_exploder_fatal_stock", "test_suicide_order", "test_flight_headbutt_order",
                "test_nested_callback_restore", "test_sparse_monster_extent_restore",
                "test_dormant_selector_restore", "test_selection_quantity",
                "test_public_projection_migration"}
    seen = {case.attrib.get("classname", "").split(".")[-1] for case in cases}
    if not expected <= seen:
        raise ValueError("required changed-rule mechanism regression not executed")
    if gate_digest() != before or native_source_digest() != source:
        raise ValueError("source changed during rule qualification")
    return {"schema": "sls-normalstart-act12-qualification-v1", "qualification": "PASS_CONTRACT_BOUNDED_ACT12",
            "profile": PROFILE, "ascension": 20, "start_distribution": "NATURAL_RUN_START",
            "native_source_sha256": source, "native_artifact": native_artifact(), "gate_source_sha256": before,
            "scope": "EXISTING_72_ACT12_MECHANISM_FLOW_OBLIGATIONS_AND_8_STOCK_CANARIES",
            "scope_is_exhaustive": False, "full_803_scope_qualification": False,
            "discovery_environment_contract": DISCOVERY, "facts": facts,
            "boundary_summary": {key: boundary[key] for key in ("decisions", "continuous_restores", "screen_counts", "restore_screen_counts")},
            "canary_roles": witnessed, "impact_regression_cases": len(cases),
            "recoveries": recovered, "input_sha256": inputs, "blockers": [],
            "limits": ["Finite supported Act1–2 contract; not full-content, Heart or exhaustive RNG proof",
                       "Raw unconditional differences retained; independent clock conditions remain conditional",
                       "No PPO, GPU memory, training-efficacy or server qualification inferred"]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--foundation", type=Path, default=ROOT / "local/reports/act12-foundation")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("immutable rule qualification already exists")
    result = qualify(args.root, args.foundation)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"qualification": result["qualification"], "facts": result["facts"]}))


if __name__ == "__main__":
    main()
