"""Submit a separate read-only GPU evaluation, preserving the cancelled run."""
from __future__ import annotations

import argparse
import json
import os
import shlex
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]

from sls.diagnostics.critic_archive import require, sha
from sls.rl.training_contract import git_state, native_source_digest
from tools.qualify_stopped_critic import validate_plan
from tools.submit_slurm import _parser, build_sbatch_command


def command(args, plan_path, run, parent, output):
    parsed = _parser().parse_args(["evaluate", "--python", args.python, "--constraint", "xgpg",
        "--partition", "gpu-long", "--cpus", "16", "--memory", "64G", "--time", "12:00:00"])
    cmd = build_sbatch_command(parsed)
    cmd[cmd.index("--job-name=sls-evaluate")] = "--job-name=sls-critic-stopped-eval"
    cmd[cmd.index("--wrap") + 1] = "exec " + shlex.join([args.python,
        str(ROOT / "tools/submit_stopped_critic_qualification.py"), "--source-root", str(args.source_root.resolve()),
        "--python", args.python, "--execute-in-allocation"])
    return cmd


def collect_missing_evidence(source, output):
    records = {}
    preparation = source / "local/runs/preparation/ironclad-a20-act12-critic20m-r1"
    for name in ("compute-gate.json", "benchmark.json", "preflight-00.json", "initial.pt"):
        p = preparation / name
        record = {"exists": p.exists()}
        if p.exists():
            record["sha256"] = sha(p)
            if p.suffix == ".json":
                record["payload"] = json.loads(p.read_text(encoding="utf-8"))
        records[str(p.relative_to(source))] = record
    for p in sorted((source / "local/operator").glob("*critic20m*.json")):
        records[str(p.relative_to(source))] = {"sha256": sha(p), "payload": json.loads(p.read_text(encoding="utf-8"))}
    result = {"source": str(source), "read_only": True, "files": records}
    try:
        result["sacct"] = subprocess.check_output(["sacct", "-j", "924694", "--parsable2",
            "--format=JobID,State,ExitCode,Elapsed,AllocCPUS,ReqMem,MaxRSS,TotalCPU,AllocTRES"], text=True)
    except (OSError, subprocess.CalledProcessError) as error:
        result["sacct_unavailable"] = str(error)
    with output.open("x", encoding="utf-8") as stream:
        json.dump(result, stream, indent=2)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--python", default="/home/h/hengzhi/venvs/sls/bin/python")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--execute-in-allocation", action="store_true")
    args = parser.parse_args()
    plan_path = ROOT / "docs/results/critic20m-review-20261010/qualification-plan.json"
    plan = json.loads(plan_path.read_text(encoding="utf-8"))
    run = args.source_root / "local/runs/ironclad-a20-act12-critic20m-r1"
    parent = args.source_root / "local/runs/ironclad-a20-act1-win-90m-continuation/final.pt"
    validate_plan(plan, run, parent)
    require(native_source_digest() == plan["native_source_sha256"], "wrong source environment")
    output = ROOT / "local/reports/critic20m-nus-confirmation-20261010"
    require(not output.exists(), "new qualification output already exists")
    if args.execute_in_allocation:
        require("SLURM_JOB_ID" in os.environ, "evaluation must execute in a GPU allocation")
        output.parent.mkdir(parents=True, exist_ok=True)
        collect_missing_evidence(args.source_root, output.parent / "critic20m-server-evidence-20261010.json")
        subprocess.run([args.python, str(ROOT / "tools/build_native.py"), "--jobs", "16"], cwd=ROOT, check=True)
        subprocess.run([args.python, str(ROOT / "tools/qualify_stopped_critic.py"),
            "--plan", str(plan_path), "--run", str(run), "--parent", str(parent),
            "--output", str(output), "--device", "cuda"], cwd=ROOT, check=True)
        return
    cmd = command(args, plan_path, run, parent, output)
    if args.dry_run:
        print(shlex.join(cmd))
        return
    require(not git_state()["dirty"], "submit only clean committed research sources")
    receipt = ROOT / "local/operator/stopped-critic-qualification-submission.json"
    require(not receipt.exists(), "submission receipt exists; inspect job instead of resubmitting")
    (ROOT / "local/runs/slurm-logs").mkdir(parents=True, exist_ok=True)
    receipt.parent.mkdir(parents=True, exist_ok=True)
    with receipt.open("x", encoding="utf-8") as stream:
        json.dump({"status": "SUBMITTING", "plan_sha256": sha(plan_path), "command": cmd}, stream, indent=2)
    response = subprocess.check_output(cmd, cwd=ROOT, text=True).strip()
    job = response.split(";")[0]
    require(job.isdecimal(), "ambiguous scheduler receipt; inspect before retry")
    receipt.write_text(json.dumps({"status": "SUBMITTED", "job": job, "plan_sha256": sha(plan_path),
        "git": git_state(), "training": False}, indent=2), encoding="utf-8")
    print(json.dumps({"job": job, "training": False, "output": str(output)}))


if __name__ == "__main__":
    main()
