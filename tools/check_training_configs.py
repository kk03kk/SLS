"""Validate every checked-in run configuration without touching the simulator.

Nothing else in the repository parses all of `configs/`, so a malformed PPO or
model section, an unknown profile, or overlapping evaluation seed ranges is
otherwise only discovered on the compute node after a job has been submitted.

Exit status is nonzero if any configuration fails. Historical configurations must
keep parsing, so this checks structure and contracts, not whether a configuration
is still a good experiment.

Usage:
    python tools/check_training_configs.py
"""

from __future__ import annotations

import argparse
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.curriculum import CURRICULUM_PROFILES_BY_ID  # noqa: E402
from sls.model import ModelConfig  # noqa: E402
from sls.rl.ppo import PPOConfig  # noqa: E402

SEED_KEYS = (
    "periodic_evaluation_seed_start",
    "final_evaluation_seed_start",
    "diagnostic_evaluation_seed_start",
)


def _seed_range(run: dict, *, key: str, count_key: str) -> tuple[int, int] | None:
    start = run.get(key)
    if start is None:
        return None
    count = int(run.get(count_key, 0))
    if count <= 0:
        raise ValueError(f"{key} is set but {count_key} is not positive")
    return int(start), int(start) + count


def validate_auxiliary(path: Path, payload: dict) -> list[str]:
    """Validate a non-training configuration that points at a training config.

    `configs/diagnostics/*.toml` uses a flat schema, so it cannot be validated as
    a run configuration, but its references must still resolve.
    """

    problems: list[str] = []
    profile = payload.get("profile")
    if profile is not None and profile not in CURRICULUM_PROFILES_BY_ID:
        problems.append(f"unknown curriculum profile: {profile!r}")
    referenced = payload.get("preflight_config")
    if referenced is not None:
        if not (ROOT / str(referenced)).is_file():
            problems.append(f"preflight_config does not exist: {referenced}")
        else:
            preflight = tomllib.loads((ROOT / str(referenced)).read_text(encoding="utf-8"))
            referenced_profile = (preflight.get("run") or {}).get("profile")
            if profile is not None and referenced_profile != profile:
                problems.append(
                    "profile does not match its preflight_config "
                    f"({profile!r} vs {referenced_profile!r})"
                )
    pinned = payload.get("native_source_sha256")
    if pinned is not None and len(str(pinned)) != 64:
        problems.append("native_source_sha256 is not a SHA256 digest")
    return problems


def validate(path: Path, payload: dict) -> list[str]:
    run = payload.get("run") or {}
    if not run and "ppo" not in payload:
        # Not a training run configuration.
        return validate_auxiliary(path, payload)
    problems: list[str] = []
    stages = payload.get("stages") or {}

    if "ppo" not in payload:
        problems.append("missing [ppo] section")
    try:
        PPOConfig(**payload["ppo"])
    except (KeyError, TypeError, ValueError) as error:
        problems.append(f"[ppo] is invalid: {error}")
    if "model" in payload:
        try:
            ModelConfig(**payload["model"])
        except (TypeError, ValueError) as error:
            problems.append(f"[model] is invalid: {error}")

    profiles = [run.get("profile")]
    profiles += [stage.get("profile") for stage in stages.values()]
    for profile in profiles:
        if profile is not None and profile not in CURRICULUM_PROFILES_BY_ID:
            problems.append(f"unknown curriculum profile: {profile!r}")

    training = int(run.get("seed", 0))
    training_limit = run.get("training_seed_limit")
    held_out: list[tuple[str, int, int]] = []
    for key, count_key in (
        ("periodic_evaluation_seed_start", "periodic_evaluation_seed_count"),
        ("final_evaluation_seed_start", "final_evaluation_seed_count"),
        ("diagnostic_evaluation_seed_start", "diagnostic_evaluation_seed_count"),
    ):
        try:
            found = _seed_range(run, key=key, count_key=count_key)
        except ValueError as error:
            problems.append(str(error))
            continue
        if found is not None:
            held_out.append((key, *found))

    for name, start, stop in held_out:
        if start < training:
            problems.append(f"{name} starts below the training seed base ({start} < {training})")
        if training_limit is not None and start < int(training_limit):
            problems.append(
                f"{name} starts inside the training seed namespace "
                f"({start} < training_seed_limit {training_limit})"
            )
    for index, (name_a, start_a, stop_a) in enumerate(held_out):
        for name_b, start_b, stop_b in held_out[index + 1:]:
            if start_a < stop_b and start_b < stop_a:
                problems.append(f"{name_a} and {name_b} overlap")

    if not run.get("output"):
        problems.append("[run] has no output directory")
    return problems


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root", type=Path, default=ROOT / "configs",
        help="directory tree to scan for .toml run configurations",
    )
    args = parser.parse_args(argv)

    paths = sorted(args.root.rglob("*.toml"))
    if not paths:
        raise SystemExit(f"no configurations found under {args.root}")

    failures = 0
    for path in paths:
        try:
            payload = tomllib.loads(path.read_text(encoding="utf-8"))
            problems = validate(path, payload)
        except Exception as error:  # noqa: BLE001 - report, never crash the scan
            problems = [f"unreadable: {type(error).__name__}: {error}"]
        relative = path.relative_to(ROOT).as_posix()
        if problems:
            failures += 1
            print(f"FAIL {relative}")
            for problem in problems:
                print(f"     {problem}")
        else:
            print(f"ok   {relative}")
    print(f"\n{len(paths) - failures}/{len(paths)} configurations valid")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
