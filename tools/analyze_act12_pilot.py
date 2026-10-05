"""Verify a completed Act1-2 pilot and recompute normal-start paired outcomes."""

from __future__ import annotations

import argparse
import contextlib
import io
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools.analyze_reward_screen import analyze_run, outcomes, read
from tools.compare_run_arms import _from_seed_results, report


def paired_act12(reference: dict, candidate: dict) -> dict:
    interval = tuple(reference["seeds"])
    if tuple(candidate["seeds"]) != interval:
        raise ValueError("Act1-2 paired seeds differ")
    for key in ("runtime", "simulator"):
        if not reference.get(key) or reference[key] != candidate.get(key):
            raise ValueError(f"Act1-2 evaluation {key} differs or is missing")
    for key in ("environment", "reward", "decoding"):
        if (key in reference or key in candidate) and reference.get(key) != candidate.get(key):
            raise ValueError(f"Act1-2 evaluation {key} differs")
    for evaluation in (reference, candidate):
        if evaluation.get("evaluation_role") != "development-confirmation":
            raise ValueError("pilot confirmation must remain development data")
        outcomes(evaluation["result"], interval, horizon=2)
        if any(row["success"] != (row["reason"] == "ACT_2_CLEARED")
               for row in evaluation["result"]["seed_results"]):
            raise ValueError("success does not mean complete Act1-2 clear")
    left, right = reference["result"]["seed_results"], candidate["result"]["seed_results"]
    indexed = {row["seed"]: row for row in left}
    if any(row["bosses"]["1"] != indexed[row["seed"]]["bosses"]["1"] for row in right):
        raise ValueError("initial seed-to-boss assignment changed")
    # Act2 boss information is absent when the policy never reaches Act2.
    # Unequal reach rates are legitimate; never pair only surviving subsets.
    with contextlib.redirect_stdout(io.StringIO()):
        return report(_from_seed_results("frozen-parent", "Act1-2", left),
                      _from_seed_results("candidate", "Act1-2", right), 256)


def analyze(run: Path) -> dict:
    summary, _, selected = analyze_run(run, horizon=2)
    config = summary["config"]
    if config["run"].get("profile") != "IRONCLAD_A20_ACT2":
        raise ValueError("not an A20 Act1-2 run")
    reference = read(run / "reference-evaluation.json")
    endpoint = read(run / "endpoint-evaluation.json")
    warm = config["warm_start"]
    if (warm["transfer_kind"] != "curriculum-stage"
            or reference.get("schema") != "sls-frozen-reference-evaluation-v2"
            or reference.get("source_profile") != "IRONCLAD_A20_ACT1"
            or reference.get("evaluation_profile") != "IRONCLAD_A20_ACT2"
            or reference.get("checkpoint_sha256") != warm["checkpoint_sha256"]
            or endpoint["checkpoint_sha256"] != summary["verified_bundle_files"]["final.pt"]):
        raise ValueError("cross-horizon control or endpoint identity mismatch")
    interval = tuple(reference["seeds"])
    return {"schema": "sls-act12-pilot-analysis-v2", "run": summary,
            "endpoint_vs_parent": paired_act12(reference, endpoint),
            "selected_vs_parent": paired_act12(reference, selected),
            "selected_vs_endpoint": paired_act12(endpoint, selected),
            "primary_metric": "normal-start probability of clearing both Act1 and Act2",
            "confirmation_diagnostics": {name: outcomes(record["result"], interval, horizon=2)
                                         for name, record in (("frozen_parent", reference),
                                                              ("endpoint", endpoint), ("selected", selected))},
            "boss_metric_note": "Per-act clears and full-horizon wins are distinct; Act2 boss groups are policy-dependent reached populations.",
            "inference_limit": "Pilot development evidence, one training seed, simulator only; no reserved final test."}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = analyze(args.run)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({"endpoint_vs_parent": result["endpoint_vs_parent"]["net"]}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
