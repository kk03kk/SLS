"""Run foundation release checks and seal their unchanged source identities."""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

from tools.qualify_act12_foundation import (
    CHECK_FILES,
    validation_manifest,
    validation_matches,
    validation_source_identity,
)

PROJECT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--config-check-only", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    if not root.is_relative_to(PROJECT / "local/reports"):
        raise ValueError("check evidence must stay under local/reports")
    root.mkdir(parents=True, exist_ok=True)
    source = validation_source_identity()
    commands = [
        ("pytest.txt", ["pytest", "-q", f"--junitxml={root / 'pytest.xml'}"]),
        ("ruff.txt", ["ruff", "check", "."]),
        ("content.txt", ["tools.generate_content_registry", "--check"]),
        ("vocabulary.txt", ["tools.generate_policy_vocabulary", "--check"]),
        ("configs.txt", ["tools.check_training_configs"]),
    ]
    if args.config_check_only:
        if not validation_matches(root, json.loads((root / "validation-final.json").read_text())):
            raise ValueError("cannot extend configuration checks without unchanged passing full-check evidence")
        commands = commands[-1:]
        names = ("configs.txt", "validation-final.json")
    else:
        names = (*CHECK_FILES, "pytest.txt", "validation-start.json", "validation-final.json")
    previous = root / ("checks-previous-" + datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ"))
    previous.mkdir()
    for name in names:
        path = root / name
        if path.exists():
            path.rename(previous / name)
    if not args.config_check_only:
        (root / "validation-start.json").write_text(json.dumps(source, indent=2) + "\n")
    for name, arguments in commands:
        print(json.dumps({"running": arguments[0], "source": source}), flush=True)
        with (root / name).open("w", encoding="utf-8") as output:
            subprocess.run([sys.executable, "-m", *arguments], cwd=PROJECT,
                           stdout=output, stderr=subprocess.STDOUT, check=True)
    if validation_source_identity() != source:
        raise ValueError("source changed during checks; outputs retained but not qualified")
    receipt = validation_manifest(root)
    (root / "validation-final.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"checks": "PASS", **source}), flush=True)


if __name__ == "__main__":
    main()
