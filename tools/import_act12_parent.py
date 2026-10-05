"""Copy only a bound pilot's parent evidence into a fresh clone; preserve old runs."""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import sha256_file
from tools.prepare_act12_pilot import repository_path


def import_parent(source: Path, plan: dict, *, root: Path = ROOT) -> Path:
    destination = repository_path(root, plan["parent"]["run"])
    if destination.exists():
        raise FileExistsError("parent destination already exists; inspect rather than overwrite")
    source = source.resolve(strict=True)
    evidence = plan["parent"]["evidence"]
    bundle = json.loads((source / "training-bundle.json").read_text(encoding="utf-8"))
    for name, digest in evidence.items():
        path = repository_path(source, name)
        if bundle["files"].get(name) != digest or sha256_file(path) != digest:
            raise ValueError(f"source parent evidence mismatch: {name}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    staging = destination.with_name(destination.name + ".importing")
    staging.mkdir()  # Exclusive: interrupted imports require explicit inspection.
    for name, digest in evidence.items():
        target = repository_path(staging, name)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source / name, target)
        if sha256_file(target) != digest:
            raise ValueError(f"copied parent evidence mismatch: {name}")
    shutil.copy2(source / "training-bundle.json", staging / "training-bundle.json")
    staging.rename(destination)
    return destination


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--plan", type=Path, required=True)
    args = parser.parse_args()
    plan = json.loads(repository_path(ROOT, args.plan).read_text(encoding="utf-8"))
    print(import_parent(args.source, plan))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
