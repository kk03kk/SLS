"""Submit one hash-bound Act1-2 pilot after local validation and GitHub push."""

from __future__ import annotations

import argparse
import json
import shlex
import subprocess
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import (
    sha256_file,
    source_sha256,
    training_implementation_digest,
)
from tools.prepare_act12_pilot import (
    build_configuration,
    inspect_parent,
    repository_path,
)
from tools.submit_slurm import _parser, build_sbatch_command


def validate_plan(plan: dict, *, root: Path = ROOT) -> Path:
    if plan.get("schema") != "sls-act12-bound-plan-v1" or plan.get("status") != "READY_FOR_LOCAL_VALIDATION":
        raise ValueError("only a hash-bound Act1-2 plan can be submitted")
    config_path = repository_path(root, plan["config"])
    if source_sha256(config_path) != plan["config_sha256"]:
        raise ValueError("bound pilot configuration changed")
    if training_implementation_digest(root=root) != plan["target_training_implementation_sha256"]:
        raise ValueError("pilot training implementation changed")
    original, parent, _ = inspect_parent(
        Path(plan["parent"]["run"]), plan["parent"]["role"], root=root,
        simulator_transition=plan["parent"].get("simulator_transition"),
    )
    if parent != plan["parent"]:
        raise ValueError("parent identity or evidence changed")
    config = tomllib.loads(config_path.read_text(encoding="utf-8"))
    expected = build_configuration(original, parent, plan["recipe"], run_name=Path(config["run"]["output"]).name)
    if config != expected:
        raise ValueError("configuration differs from the registered transfer recipe")
    return config_path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    plan_path = repository_path(ROOT, args.plan)
    plan = json.loads(plan_path.read_text(encoding="utf-8"))
    config_path = validate_plan(plan)
    hours = int(plan["wall_limit_hours"])
    if not 1 <= hours <= 72:
        raise ValueError("wall limit must be between 1 and 72 hours")
    command = build_sbatch_command(_parser().parse_args([
        "train", "--config", str(config_path), "--prepare", "--python", sys.executable,
        "--constraint", "xgpg", "--cpus", "16", "--memory", "64G",
        "--time", f"{hours // 24}-{hours % 24:02d}:00:00",
    ]))
    print(shlex.join(command), flush=True)
    if args.dry_run:
        return 0
    dirty = subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True)
    if dirty.strip():
        raise ValueError("pull committed sources and resolve changes before submission")
    config = tomllib.loads(config_path.read_text(encoding="utf-8"))
    if repository_path(ROOT, config["run"]["output"]).exists():
        raise FileExistsError("target run exists; inspect/resume explicitly")
    receipt = ROOT / "local/operator" / f"{Path(config['run']['output']).name}-submission.json"
    receipt.parent.mkdir(parents=True, exist_ok=True)
    with receipt.open("x", encoding="utf-8") as stream:
        json.dump({"status": "SUBMITTING", "plan_sha256": sha256_file(plan_path)}, stream)
    (ROOT / "local/runs/slurm-logs").mkdir(parents=True, exist_ok=True)
    job = subprocess.check_output(command, cwd=ROOT, text=True).strip()
    receipt.write_text(json.dumps({"status": "SUBMITTED", "job": job,
                                  "plan_sha256": sha256_file(plan_path)}, indent=2) + "\n", encoding="utf-8")
    print(job, flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
