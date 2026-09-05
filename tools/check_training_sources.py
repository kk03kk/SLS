"""Read-only source-gate diagnosis; safe on login nodes, without Torch/native imports."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import (
    local_source_digest,
    source_sha256,
    training_validation_digest,
    validate_training_sources,
)


def diagnose(manifest_path: Path, *, root: Path = ROOT) -> dict[str, object]:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    initialization = manifest.get("initialization", manifest)
    transition_path = root / "configs/compatibility/training-validation-transitions.json"
    transitions = json.loads(transition_path.read_text(encoding="utf-8")) if transition_path.is_file() else []
    report = {
        "manifest": str(manifest_path),
        "old_source_tree_sha256": initialization.get("source_tree_sha256"),
        "old_training_validation_sha256": initialization.get("training_validation_sha256"),
        "old_git": initialization.get("git"),
        "current_training_validation_sha256": training_validation_digest(root=root),
        "current_source_tree_sha256": local_source_digest(("src", "tools", "configs"), root=root),
        "reviewed_transitions": transitions,
    }
    try:
        tracked = set(subprocess.check_output(
            ["git", "ls-files", "-z", "--", "src", "tools", "configs"], cwd=root,
        ).decode("utf-8").split("\0"))
        report["untracked_files_in_legacy_digest"] = {
            path.relative_to(root).as_posix(): source_sha256(path)
            for parent in ("src", "tools", "configs")
            for path in (root / parent).rglob("*")
            if path.is_file() and "__pycache__" not in path.parts and path.suffix not in {".pyc", ".pyo"}
            and path.relative_to(root).as_posix() not in tracked
        }
    except (OSError, subprocess.CalledProcessError) as error:
        report["git_inventory_error"] = str(error)
    try:
        report["validation_reuse"] = validate_training_sources(initialization, root=root)
        report["ok"] = True
    except ValueError as error:
        report["ok"] = False
        report["error"] = str(error)
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path,
                        default=ROOT / "local/runs/ironclad-a0-fullrun-v4-15m/run-manifest.json")
    args = parser.parse_args()
    report = diagnose(args.manifest)
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    return 0 if report["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
