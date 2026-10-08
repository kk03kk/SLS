"""Submit one new recipe in a three-allocation afterok chain; no torch."""
from __future__ import annotations

import argparse
import json
import shlex
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import sha256_file, source_sha256
from tools.act12_critic20m_contract import PLAN, validate
from tools.prepare_act12_long_run import reject_used_seeds
from tools.submit_slurm import _parser, build_sbatch_command


def commands(path, *, python=sys.executable):
    commands = []
    for index in range(3):
        command = build_sbatch_command(_parser().parse_args([
            "train", "--config", str(path), "--python", python, "--constraint", "xgpg",
            "--cpus", "16", "--memory", "64G", "--time", "2-00:00:00"]))
        command[command.index("--job-name=sls-train")] = "--job-name=sls-act12-critic20m-r1"
        command[command.index("--wrap")+1] = "exec " + shlex.join([
            python, str(ROOT / "tools/run_act12_critic20m.py"), "--plan", str(path),
            "--allocation", str(index)])
        commands.append(command)
    return commands


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, default=ROOT / PLAN)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    plan, _, config = validate(args.plan)
    chain = commands(args.plan.resolve())
    if args.dry_run:
        for command in chain:
            print(shlex.join(command))
        return
    if subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip():
        raise ValueError("submit only clean committed sources")
    if (ROOT / config["run"]["output"]).exists():
        raise FileExistsError("output exists; inspect instead of resubmitting")
    for name, expected in plan["parent"]["evidence"].items():
        if sha256_file(ROOT / plan["parent"]["run"] / name) != expected:
            raise ValueError("imported parent evidence changed")
    reject_used_seeds([ROOT / "local/runs", ROOT / "docs/results"])
    receipt = ROOT / "local/operator/act12-critic20m-r1-submission.json"
    receipt.parent.mkdir(parents=True, exist_ok=True)
    with receipt.open("x", encoding="utf-8") as stream:
        json.dump({"status": "SUBMITTING", "plan_sha256": source_sha256(args.plan)}, stream)
    (ROOT / "local/runs/slurm-logs").mkdir(parents=True, exist_ok=True)
    jobs = []
    raw_response = None
    try:
        for command in chain:
            if jobs:
                command.insert(1, "--dependency=afterok:" + jobs[-1])
            raw_response = subprocess.check_output(command, cwd=ROOT, text=True).strip()
            job = raw_response.split(";")[0]
            if not job.isdecimal():
                raise ValueError("unrecognized sbatch response; inspect before retry")
            jobs.append(job)
            receipt.write_text(json.dumps({"status": "SUBMITTING", "jobs": jobs,
                                          "plan_sha256": source_sha256(args.plan)})+"\n")
    except BaseException:
        receipt.write_text(json.dumps({"status": "PARTIAL_SUBMISSION_INSPECT", "jobs": jobs,
                                       "last_response": raw_response})+"\n")
        raise
    receipt.write_text(json.dumps({"status": "SUBMITTED", "jobs": jobs,
                                  "plan_sha256": source_sha256(args.plan)})+"\n")
    print(json.dumps({"jobs": jobs, "logical_training_budget": 20000000,
                      "compute_gate": "NOT_YET_RUN", "training_results": "NOT_YET_RUN"}))


if __name__ == "__main__":
    main()
