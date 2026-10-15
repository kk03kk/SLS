"""Freeze current audit identities and map the complete conservative candidate ledger."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from sls.audit.validation_scope import (
    file_identity,
    map_scope,
    natural_witnesses,
    verified_stock_projections,
    verify_bytecode,
)
from sls.backends.simulator import native
from sls.rl.training_contract import native_artifact, native_source_digest
from tools.verify_oracle import inspect

ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ledger", type=Path, required=True)
    parser.add_argument("--bytecode", type=Path, required=True)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--oracle", type=Path, required=True)
    parser.add_argument("--natural-batch", type=Path, action="append", default=[])
    parser.add_argument("--stock-source-root", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite scope evidence")
    source = native_source_digest()
    if native.NATIVE_SOURCE_SHA256 != source:
        raise ValueError("stale native binary")
    ledger = json.loads(args.ledger.read_text(encoding="utf-8"))
    bytecode = json.loads(args.bytecode.read_text(encoding="utf-8"))
    verify_bytecode(bytecode, args.stock_jar)
    oracle = inspect(args.oracle)
    if oracle["dependencies"]["game"] != file_identity(args.stock_jar)["sha256"]:
        raise ValueError("Oracle stock game mismatch")
    result = map_scope(ledger, bytecode, ROOT)
    result["shared_and_flow_stock_sources"] = (
        verified_stock_projections(args.stock_source_root, args.stock_jar) if args.stock_source_root else [])
    result["natural_stock_batches"] = []
    index = {row["id"]: row for row in result["obligations"]}
    seen_trajectories = set()
    for path in args.natural_batch:
        witnesses, receipt = natural_witnesses(path, bytecode["stock_jar_sha256"])
        hashes = {row["sha256"] for row in receipt["trajectories"]}
        if seen_trajectories.intersection(hashes):
            raise ValueError("duplicate natural evidence across batches")
        seen_trajectories.update(hashes)
        result["natural_stock_batches"].append(receipt)
        for identifier, evidence in witnesses.items():
            if identifier not in index:
                raise ValueError(f"observed content missing from candidate denominator: {identifier}")
            row = index[identifier]
            row["reachability"] = "OBSERVED_OWNED_OR_ENCOUNTERED_IN_NORMAL_STOCK_TRAJECTORY"
            row.setdefault("reachability_evidence", []).extend(evidence)
    result["summary"]["stock_witnessed_candidates"] = sum(
        row["reachability"] != "UNCONFIRMED" for row in result["obligations"])
    comparison_paths = set((ROOT / "src/sls/audit").glob("*.py"))
    comparison_paths.update((ROOT / "src/sls/backends").rglob("*.py"))
    comparison_paths.update(ROOT / path for path in (
        "src/sls/curriculum.py",
        "tools/replay_act2_encounter_batch.py", "tools/replay_act2_system_batch.py",
        "tools/replay_act2_production_trajectory.py", "tools/replay_act2_clock_conditioned_trajectory.py",
        "tools/prepare_a20_validation_scope.py", "tools/revalidate_act12_simulator.py",
        "tools/audit_stock_bytecode.py"))
    result["source_freeze"] = {
        "native_source_sha256": source, "native_artifact": native_artifact(),
        "stock_jar": file_identity(args.stock_jar), "oracle": file_identity(args.oracle),
        "oracle_build": file_identity(args.oracle.with_suffix(".build.json")),
        "ledger": file_identity(args.ledger), "bytecode": file_identity(args.bytecode),
        "ledger_native_source_is_current": ledger.get("native_source_sha256") == source,
        "comparator_sources": [file_identity(path) for path in sorted(comparison_paths)],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(result, stream, indent=2)
        stream.write("\n")
    print(json.dumps(result["summary"]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
