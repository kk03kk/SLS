"""CommunicationMod entry point for a multi-seed canonical parity corpus."""

from __future__ import annotations

import argparse
from dataclasses import asdict
import json
import os
from pathlib import Path
import sys
import tomllib


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport
from sls.backends.simulator import (
    IRONCLAD_A0_ACT1,
    IRONCLAD_A0_ACT2,
    IRONCLAD_A0_ACT3,
    IRONCLAD_A0_HEART,
    SimulatorBackend,
)
from sls.validation import run_paired, summarize


PROFILES = {
    profile.profile_id: profile
    for profile in (
        IRONCLAD_A0_ACT1,
        IRONCLAD_A0_ACT2,
        IRONCLAD_A0_ACT3,
        IRONCLAD_A0_HEART,
    )
}


def _load_config(path: Path) -> dict[str, object]:
    with path.open("rb") as stream:
        return tomllib.load(stream)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--config",
        type=Path,
        default=ROOT / "configs" / "validation" / "full_run.toml",
    )
    parser.add_argument("--seeds")
    parser.add_argument("--profile", choices=sorted(PROFILES))
    parser.add_argument("--max-steps", type=int)
    parser.add_argument(
        "--output-dir",
        type=Path,
    )
    parser.add_argument("--include-rng", action=argparse.BooleanOptionalAction, default=None)
    parser.add_argument(
        "--stop-on-difference",
        action=argparse.BooleanOptionalAction,
        default=None,
    )
    args = parser.parse_args()
    config = _load_config(args.config.resolve())
    configured_seeds = config.get("seeds", ())
    seeds = (
        tuple(int(value.strip()) for value in args.seeds.split(",") if value.strip())
        if args.seeds is not None
        else tuple(int(value) for value in configured_seeds)
    )
    if not seeds:
        parser.error("at least one seed is required")
    profile_id = args.profile or str(config.get("profile", "IRONCLAD_A0_HEART"))
    if profile_id not in PROFILES:
        parser.error(f"unknown profile in config: {profile_id}")
    profile = PROFILES[profile_id]
    max_steps = args.max_steps if args.max_steps is not None else int(config.get("max_steps", 10_000))
    if max_steps <= 0:
        parser.error("max_steps must be positive")
    output_dir = args.output_dir or Path(
        os.environ.get(
            "SLS_PARITY_OUTPUT_DIR",
            str(config.get("output_dir", "validation-results/corpus")),
        )
    )
    include_rng = args.include_rng if args.include_rng is not None else bool(config.get("include_rng", True))
    stop_on_difference = (
        args.stop_on_difference
        if args.stop_on_difference is not None
        else bool(config.get("stop_on_difference", True))
    )
    acceptance = config.get("acceptance") or {}
    if not isinstance(acceptance, dict):
        parser.error("[acceptance] must be a TOML table")
    required_complete = int(acceptance.get("require_complete_runs", len(seeds)))
    allowed_differences = int(acceptance.get("allow_unexplained_differences", 0))
    if required_complete < 0 or allowed_differences < 0:
        parser.error("acceptance thresholds must be non-negative")
    transport = StdioTransport(
        log_path=Path(os.environ["SLS_PROTOCOL_LOG"]).resolve()
        if os.environ.get("SLS_PROTOCOL_LOG") else None,
    )
    original = OriginalBackend(OriginalSession(transport), profile)
    traces = []
    for seed in seeds:
        trace = run_paired(
            original,
            SimulatorBackend(profile),
            seed=seed,
            max_steps=max_steps,
            include_rng=include_rng,
            stop_on_difference=stop_on_difference,
        )
        trace.write(output_dir / f"seed-{seed}.json")
        traces.append(trace)
        if stop_on_difference and not trace.matches:
            break
    summary = summarize(traces)
    unexplained = summary.seeds - summary.matching_runs
    accepted = (
        summary.complete_runs >= required_complete
        and unexplained <= allowed_differences
        and summary.seeds == len(seeds)
    )
    output_dir.mkdir(parents=True, exist_ok=True)
    report = {
        "accepted": accepted,
        "acceptance": {
            "require_complete_runs": required_complete,
            "allow_unexplained_differences": allowed_differences,
        },
        "profile": profile.profile_id,
        "summary": asdict(summary),
    }
    (output_dir / "summary.json").write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(
        f"CORPUS seeds={summary.seeds} complete={summary.complete_runs} "
        f"matching={summary.matching_runs} steps={summary.semantic_steps} "
        f"accepted={accepted}",
        file=sys.stderr,
        flush=True,
    )
    return 0 if accepted else 1


if __name__ == "__main__":
    raise SystemExit(main())
