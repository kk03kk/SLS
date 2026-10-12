"""Locate the frozen parent and submit the fully prepared v6 20M run."""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import sha256_file

CONFIG = ROOT / "configs/train/ironclad_a20_act12_real_entry_20m_v6_r1.toml"


def locate_parent(config: dict, *, root: Path = ROOT) -> Path:
    spec = config["warm_start"]
    target = root / spec["checkpoint"]
    if target.exists():
        if sha256_file(target) != spec["checkpoint_sha256"]:
            raise ValueError(f"Existing parent has the wrong SHA256; preserved: {target}")
        return target
    # Search only known parent lineage directories; never load unverified .pt files.
    for path in sorted((root / "local/runs").glob("*90m*/*.pt")):
        if sha256_file(path) == spec["checkpoint_sha256"]:
            target.parent.mkdir(parents=True, exist_ok=True)
            with path.open("rb") as source, target.open("xb") as output:
                shutil.copyfileobj(source, output)
            if sha256_file(target) != spec["checkpoint_sha256"]:
                raise ValueError("Copied parent SHA256 mismatch")
            return target
    raise FileNotFoundError(
        f"The frozen 90M parent was not found. Upload it to {target}; "
        f"required SHA256={spec['checkpoint_sha256']}. No job has been submitted."
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    config = tomllib.loads(CONFIG.read_text())
    parent = locate_parent(config)
    output = ROOT / config["run"]["output"]
    manifest = output / "run-manifest.json"
    if manifest.exists():
        status = json.loads(manifest.read_text()).get("status")
        if status in {"COMPLETE", "FAILED"}:
            raise ValueError(f"Run status={status}; inspect before any new submission")
    print(json.dumps({"parent": str(parent), "parent_sha256": sha256_file(parent),
                      "architecture": config["model"]["architecture"],
                      "entry_bank": "exact prefix reconstruction on GPU node if missing"}), flush=True)
    os.environ.setdefault("PYTHONPATH", os.pathsep.join((str(ROOT), str(ROOT / "src"))))
    os.environ.setdefault("PYTHONPYCACHEPREFIX", str(ROOT / "local/build/python-cache"))
    command = [sys.executable, str(ROOT / "tools/submit_slurm.py"), "train", "--prepare",
               "--config", str(CONFIG), "--python", sys.executable,
               "--account", "allusers", "--qos", "normal", "--partition", "gpu-long",
               "--gpu", "a100-40", "--cpus", "16", "--memory", "64G", "--time", "3-00:00:00",
               "--stop-signal-seconds", "900"]
    if args.dry_run:
        command.append("--dry-run")
    subprocess.run(command, cwd=ROOT, check=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
