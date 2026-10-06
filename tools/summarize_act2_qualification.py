"""Fail-closed coverage rollup; never treat a partial stock run as a pass."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.rl.training_contract import native_source_digest


def exclude_superseded_system_rows(reports: list[tuple[Path, dict]], exclusions: list[list[str]]) -> None:
    """An exclusion must name its old report and have a later replacement."""
    for report_path, seed_text in exclusions:
        path, seed = Path(report_path).resolve(), int(seed_text)
        matches = [i for i, (p, _) in enumerate(reports) if p.resolve() == path]
        if len(matches) != 1:
            raise ValueError("superseded system report must appear exactly once in inputs")
        index = matches[0]
        report = reports[index][1]
        if not any(r['seed'] == seed for r in report['runs']):
            raise ValueError("superseded system seed is absent from old report")
        if not any(r['seed'] == seed for _, newer in reports[index + 1:] for r in newer['runs']):
            raise ValueError("cannot discard a system obligation without a later replacement")
        report['runs'] = [r for r in report['runs'] if r['seed'] != seed]


def rollup(controlled: list[dict], systems: list[dict], production: list[dict],
           selection: dict, source: str) -> dict:
    blockers = []
    rows = {}
    for report in controlled + systems:
        if report.get("native_source_sha256") != source:
            raise ValueError("stale differential source identity")
        for row in report["runs"]:
            seed = row["seed"]
            if seed in rows:
                raise ValueError("ambiguous duplicate controlled seed; explicitly exclude superseded harness rows")
            rows[seed] = row
    expected = set(range(131100000, 131100072))
    if set(rows) - expected:
        raise ValueError("controlled seeds outside the frozen mechanism space")
    passed = {seed for seed, row in rows.items()
              if row["status"] in {"HARNESS_MATCH", "SYSTEM_BRANCH_MATCH"}
              and row.get("first_divergence") is None}
    missing = sorted(expected - passed)
    if missing:
        blockers.append("controlled obligations missing, incomplete or divergent")
    if not any(r.get("act2_boundaries", 0) for s, r in rows.items() if 63 <= s - 131100000 <= 65):
        blockers.append("Act1 to Act2 continuation not witnessed")
    if not any(r.get("reward_boundaries", 0) for s, r in rows.items() if 66 <= s - 131100000 <= 68):
        blockers.append("reward collection not witnessed")
    if not all(rows.get(s, {}).get("checkpoint_replays", 0) > 0 for s in range(131100069, 131100072)):
        blockers.append("checkpoint continuation evidence missing")
    if selection.get("native_source_sha256") != source:
        raise ValueError("stale diagnostic selection")
    selected = {r["seed"] for r in selection["runs"]}
    if len(selected) != 8:
        raise ValueError("requires the frozen eight-canary selection")
    natural = {}
    for row in production:
        if row.get("native_source_sha256") != source or row["seed"] in natural:
            raise ValueError("stale source or duplicate production trajectory")
        if row["evaluation_environment"].get("frozen_model_sha256") != selection["model_sha256"]:
            raise ValueError("production policy differs from frozen selection")
        natural[row["seed"]] = row
    if set(natural) - selected:
        raise ValueError("unselected production trajectory")
    natural_missing = sorted(selected - {s for s, r in natural.items()
        if r["status"] == "TRAJECTORY_MATCH" and r["first_divergence"] is None})
    if natural_missing:
        blockers.append("production canaries missing or divergent")
    return {"schema": "sls-act2-qualification-rollup-v1", "native_source_sha256": source,
            "controlled_matched": len(passed), "controlled_expected": 72,
            "controlled_missing_or_failed": missing, "production_expected": 8,
            "production_missing_or_failed": natural_missing, "blockers": blockers,
            "training_gate": "NOT_QUALIFIED" if blockers else "EVIDENCE_COMPLETE_PENDING_RECOVERY_AND_LOCAL_CHECKS",
            "scope": "24 bounded obligations only; no complete Act2 certification or win-rate estimate"}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--controlled", type=Path, nargs="*", default=[])
    parser.add_argument("--systems", type=Path, nargs="*", default=[])
    parser.add_argument("--production", type=Path, nargs="*", default=[])
    parser.add_argument("--selection", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--exclude-superseded", type=int, nargs="*", default=[],
                        help="exclude only the explicitly documented older controlled harness rows")
    parser.add_argument("--exclude-system-row", nargs=2, action="append", default=[],
                        metavar=("OLD_REPORT", "SEED"), help="exclude one old system row only with a later replacement")
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite qualification evidence")
    def read(path):
        return json.loads(path.read_text(encoding="utf-8"))
    controlled = [read(p) for p in args.controlled]
    for report in controlled[:-1]:
        report["runs"] = [r for r in report["runs"] if r["seed"] not in args.exclude_superseded]
    systems = [(p, read(p)) for p in args.systems]
    exclude_superseded_system_rows(systems, args.exclude_system_row)
    result = rollup(controlled, [report for _, report in systems],
                    [read(p) for p in args.production], read(args.selection), native_source_digest())
    result["input_sha256"] = {p.as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
        for p in (*args.controlled, *args.systems, *args.production, args.selection)}
    result["superseded_harness_seeds"] = args.exclude_superseded
    result["superseded_system_rows"] = args.exclude_system_row
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
