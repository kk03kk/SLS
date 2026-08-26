"""Generate the committed portable Act 1 parity readiness attestation."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
import tomllib

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "tools"))

from sls.rl.training_contract import git_state
from sls.validation.readiness_lock import (
    ENGINEERING_LOCK, ENGINEERING_READY, READINESS_LEVELS, build_readiness_lock,
)
from replay_truth import replay


def _verify_segment(bundle: Path, start: int, end: int) -> None:
    matched, difference = replay(bundle, from_step=start, to_step=end)
    if not matched:
        raise ValueError(
            f"current offline replay failed for {bundle.name}[{start}:{end}]: {difference}"
        )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT / "validation-results" / "truth")
    parser.add_argument("--config", type=Path, default=ROOT / "configs" / "validation" / "act1_training.toml")
    parser.add_argument("--output", type=Path, default=ENGINEERING_LOCK)
    parser.add_argument("--allow-dirty", action="store_true", help="development/test only")
    parser.add_argument("--level", choices=sorted(READINESS_LEVELS), default=ENGINEERING_READY)
    parser.add_argument("--expansion-report", type=Path)
    args = parser.parse_args()
    if git_state()["dirty"] and not args.allow_dirty:
        raise SystemExit("readiness lock generation requires a clean Git worktree")
    with args.config.open("rb") as stream:
        requirements = tomllib.load(stream)["requirements"]
    expansion = None
    if args.expansion_report is not None:
        expansion = json.loads(args.expansion_report.read_text(encoding="utf-8"))
    lock = build_readiness_lock(
        args.root, requirements, replay_validator=_verify_segment,
        level=args.level, expansion_report=expansion,
    )
    text = json.dumps(lock, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(text, encoding="utf-8")
    temporary.replace(args.output)
    print(json.dumps({"output": str(args.output), "lock_sha256": lock["lock_sha256"]}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
