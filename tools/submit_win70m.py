"""Submit one preregistered Win continuation job; no duplicate submissions."""

from __future__ import annotations

import argparse
import hashlib
import json
import shlex
import subprocess
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT))

from sls.rl.training_contract import (
    native_source_digest,
    training_implementation_digest,
)
from tools.submit_slurm import _parser, build_sbatch_command

PLAN = ROOT / "configs/experiments/win-70m-20260930.json"


def validate_plan(plan: dict, *, root: Path = ROOT) -> Path:
    path = root / plan["config"]
    actual = hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
    if actual != plan["config_sha256"]:
        raise ValueError("preregistered configuration changed")
    config = tomllib.loads(path.read_text(encoding="utf-8"))
    run = config["run"]
    if training_implementation_digest(root=root) != run["continuation_to_training_implementation_sha256"]:
        raise ValueError("implementation differs from reviewed continuation target")
    source = root / run["continuation_from"]
    manifest = json.loads((source / "run-manifest.json").read_text(encoding="utf-8"))
    if manifest["native_source_sha256"] != native_source_digest():
        raise ValueError("environment semantics changed")
    if manifest["status"] != "COMPLETE":
        raise ValueError("parent Win run is not complete")
    if manifest["training_implementation_sha256"] != run["continuation_from_training_implementation_sha256"]:
        raise ValueError("parent implementation differs from the reviewed source")
    bundle = json.loads((source / "training-bundle.json").read_text(encoding="utf-8"))
    if bundle["files"].get("latest.pt") != run["continuation_checkpoint_sha256"]:
        raise ValueError("source bundle does not register the pinned endpoint")
    for filename in ("run-manifest.json", "training-config.toml"):
        with (source / filename).open("rb") as stream:
            if hashlib.file_digest(stream, "sha256").hexdigest() != bundle["files"][filename]:
                raise ValueError(f"source bundle mismatch: {filename}")
    for path_to_check, expected in (
        (source / "latest.pt", run["continuation_checkpoint_sha256"]),
        (root / run["development_reference_checkpoint"], run["development_reference_sha256"]),
    ):
        with path_to_check.open("rb") as stream:
            if hashlib.file_digest(stream, "sha256").hexdigest() != expected:
                raise ValueError(f"pinned checkpoint hash mismatch: {path_to_check}")
    return path


def main(*, plan_path: Path = PLAN, receipt_name: str = "win-70m-20260930-submission.json") -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    plan = json.loads(plan_path.read_text(encoding="utf-8"))
    path = validate_plan(plan)
    hours = int(plan["wall_limit_hours"])
    if not 1 <= hours <= 72:
        raise ValueError("wall limit must fit the 72-hour training partition")
    command = build_sbatch_command(_parser().parse_args([
        "train", "--config", str(path), "--prepare", "--python", sys.executable,
        "--constraint", "xgpg", "--cpus", "16", "--memory", "64G",
        "--time", f"{hours // 24}-{hours % 24:02d}:00:00",
    ]))
    print(shlex.join(command), flush=True)
    if args.dry_run:
        return 0
    dirty = subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True)
    if dirty.strip():
        raise ValueError("pull committed sources and resolve working changes before submission")
    config = tomllib.loads(path.read_text(encoding="utf-8"))
    if (ROOT / config["run"]["output"]).exists():
        raise FileExistsError("target already exists; inspect/resume explicitly rather than duplicate submission")
    if Path(receipt_name).name != receipt_name:
        raise ValueError("receipt must be a plain filename")
    receipt = ROOT / "local/operator" / receipt_name
    receipt.parent.mkdir(parents=True, exist_ok=True)
    plan_hash = hashlib.sha256(plan_path.read_bytes()).hexdigest()
    # Retain a partial receipt if sbatch fails or returns ambiguous output.
    with receipt.open("x", encoding="utf-8") as stream:
        json.dump({"status": "SUBMITTING", "plan_sha256": plan_hash}, stream)
    (ROOT / "local/runs/slurm-logs").mkdir(parents=True, exist_ok=True)
    job = subprocess.check_output(command, cwd=ROOT, text=True).strip()
    receipt.write_text(json.dumps({"status": "SUBMITTED", "job": job,
                                  "plan_sha256": plan_hash}, indent=2) + "\n", encoding="utf-8")
    print(job, flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
