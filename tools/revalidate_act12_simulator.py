"""Offline CPU replay of existing Act1–2 captures; independent of PPO readiness."""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from collections import Counter
from pathlib import Path

from sls.audit.validation_scope import file_identity, natural_witnesses
from sls.rl.training_contract import native_source_digest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--foundation", type=Path, required=True)
    parser.add_argument("--oracle-build", type=Path, required=True)
    parser.add_argument("--output-directory", type=Path, required=True)
    args = parser.parse_args()
    output = args.output_directory
    output.mkdir(parents=True, exist_ok=False)
    source = native_source_digest()
    build = json.loads(args.oracle_build.read_text(encoding="utf-8"))
    foundation = args.foundation
    env = {**os.environ, "CUDA_VISIBLE_DEVICES": "-1", "SLS_TEST_CUDA": "0"}
    reports = []

    def run(name: str, module: str, *arguments: str) -> dict:
        destination = output / (name + ".json")
        command = [sys.executable, "-m", "tools." + module, *map(str, arguments),
                   "--output", str(destination)]
        with (output / (name + ".log")).open("w", encoding="utf-8") as log:
            completed = subprocess.run(command, env=env, stdout=log, stderr=subprocess.STDOUT)
        report = {"name": name, "command": command, "exit_code": completed.returncode,
                  "log": file_identity(output / (name + ".log"))}
        data = {}
        if completed.returncode == 0:
            data = json.loads(destination.read_text(encoding="utf-8"))
            if data.get("native_source_sha256") != source:
                raise ValueError("replay did not use frozen native source")
            report.update(evidence=file_identity(destination), statuses=dict(Counter(
                row["status"] for row in data.get("runs", [data]))))
        reports.append(report)
        print(json.dumps({key: report[key] for key in ("name", "exit_code", "statuses") if key in report}), flush=True)
        return data

    run("controlled", "replay_act2_encounter_batch", foundation / "controlled-stock.json",
        "--oracle-build", args.oracle_build, "--native-view", "production-public")
    for index in range(3):
        capture = foundation / f"systems-{index}.json"
        for conditional in (False, True):
            run(f"systems-{index}-{'conditional' if conditional else 'raw'}",
                "replay_act2_system_batch", capture, *(["--measured-runtime"] if conditional else []))
    replacements = json.loads((foundation / "system-replacements.json").read_text(encoding="utf-8"))
    for row in replacements["rows"]:
        for conditional in (False, True):
            run(f"replacement-{row['seed']}-{'conditional' if conditional else 'raw'}",
                "replay_act2_system_batch", foundation / (row["capture_basename"] + ".json"),
                *(["--measured-runtime"] if conditional else []))
    receipts = []
    for name in ("natural-0", "natural-1-retry"):
        batch_path = foundation / (name + ".json")
        _, receipt = natural_witnesses(batch_path, build["dependencies"]["game"])
        if receipt["oracle_sha256"] != build["output_sha256"]:
            raise ValueError("natural capture Oracle identity mismatch")
        receipts.append(receipt)
        for trajectory in receipt["trajectories"]:
            data = run(f"natural-{trajectory['seed']}-raw", "replay_act2_production_trajectory",
                       trajectory["path"])
            if data.get("first_divergence"):
                run(f"natural-{trajectory['seed']}-conditional", "replay_act2_clock_conditioned_trajectory",
                    trajectory["path"], "--oracle-build", args.oracle_build)
    if native_source_digest() != source:
        raise ValueError("native source changed during replay")
    summary = {"schema": "sls-act12-simulator-revalidation-v1", "profile": "IRONCLAD_A20_ACT2",
               "native_source_sha256": source, "device": "cpu", "training_started": False,
               "qualification": "NOT_QUALIFIED", "scope": "EXISTING_CORPUS_REPLAY_ONLY",
               "discovery_contract": "FIXED_14_RAW_DIFFERENCES_RETAINED_CONDITIONAL_SEPARATE",
               "oracle_build": file_identity(args.oracle_build), "natural_receipts": receipts,
               "reports": reports,
               "blockers": ["STAGE1_REACHABILITY_AND_BRANCH_REVIEW_INCOMPLETE",
                            "SIMULATOR_QUALIFICATION_GATE_AND_REQUIRED_COVERAGE_NOT_YET_COMPLETE"]}
    with (output / "summary.json").open("x", encoding="utf-8") as stream:
        json.dump(summary, stream, indent=2)
        stream.write("\n")
    return int(any(row["exit_code"] != 0 for row in reports))


if __name__ == "__main__":
    raise SystemExit(main())
