"""Inspect a stopped NUS archive in a new directory, using CPU only."""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]

from sls.diagnostics.cpu import cpu_runtime, write_json
from sls.diagnostics.critic_archive import audit_run, extract_archive, sha


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--extract", type=Path, required=True)
    parser.add_argument("--parent", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--reuse-extraction", action="store_true", help="Verify every existing extracted byte, without writing")
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("audit output already exists")
    runtime = cpu_runtime()
    original = sha(args.archive)
    inventory = extract_archive(args.archive, args.extract, verify_existing=args.reuse_extraction)
    runs = list(args.extract.glob("*/run-manifest.json"))
    if len(runs) != 1:
        raise ValueError("expected exactly one archived run")
    report = audit_run(runs[0].parent, args.parent)
    stdout = next(args.extract.glob("local/runs/slurm-logs/*.out"))
    logged = [json.loads(line) for line in stdout.read_text(encoding="utf-8").splitlines()
              if line.startswith('{"advantage_') or line.startswith('{"collect_encode_seconds"')]
    metrics = [json.loads(line) for line in (runs[0].parent / "stages/train/metrics.jsonl").read_text(encoding="utf-8").splitlines()
               if '"update_seconds"' in line]
    expected_stdout = []
    for row in metrics:
        slim = dict(row)
        if "evaluation" in slim:
            slim["evaluation"] = {k: v for k, v in slim["evaluation"].items()
                                  if k not in ("seed_results", "failure_traces")}
        expected_stdout.append(slim)
    if logged != expected_stdout:
        raise ValueError("stdout and metrics update evidence differs")
    stderr = next(args.extract.glob("local/runs/slurm-logs/*.err"))
    report.update(archive=inventory, runtime=runtime, stdout_updates_verified=len(logged),
                  cancellation_evidence=[s for s in stderr.read_text(encoding="utf-8").splitlines() if "CANCELLED" in s])
    from sls.rl.training_contract import git_state, source_sha256

    report["execution_git"] = git_state()
    report["diagnostic_source_sha256"] = {p: source_sha256(ROOT / p) for p in (
        "tools/audit_stopped_critic.py", "src/sls/diagnostics/critic_archive.py")}
    if sha(args.archive) != original:
        raise ValueError("archive changed during audit")
    write_json(args.output, report)
    print(json.dumps({k: report[k] for k in ("new_logged_decisions", "new_saved_decisions",
        "unsaved_logged_decisions", "stdout_updates_verified", "missing_completion_evidence")}))


if __name__ == "__main__":
    main()
