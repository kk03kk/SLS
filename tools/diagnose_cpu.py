"""Capture natural histories, compare matched states, or analyze complete returns."""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

# Set before any model import; inherited by every diagnostic subprocess.
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]

from sls.diagnostics.cpu import (
    SEED_COUNT,
    SEED_START,
    analyze_returns,
    capture,
    check_seed_collisions,
    compare,
    cpu_runtime,
    load_models,
    verify_greedy,
)


def checkpoint_args(values, artifact_root):
    if values is None:
        return {label: artifact_root / "local/runs" / run / "final.pt" for label, run in (
            ("parent90", "ironclad-a20-act1-win-90m-continuation"),
            ("lambda098", "ironclad-a20-act12-lambda098-r1"),
            ("lambda100", "ironclad-a20-act12-lambda100-r1"))}
    result = {}
    for value in values:
        label, path = value.split("=", 1)
        if not re.fullmatch(r"[a-zA-Z0-9_-]+", label) or label in result:
            raise ValueError("model labels must be distinct filename-safe identifiers")
        result[label] = Path(path)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("capture", "compare", "returns", "verify"))
    parser.add_argument("--device", choices=("cpu",), default="cpu")
    parser.add_argument("--artifact-root", type=Path, default=ROOT)
    parser.add_argument("--checkpoint", action="append", metavar="LABEL=PATH")
    parser.add_argument("--corpus", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seed-start", type=int, default=SEED_START)
    parser.add_argument("--seed-count", type=int, default=SEED_COUNT)
    parser.add_argument("--max-steps", type=int)
    parser.add_argument("--max-states", type=int, default=64)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("diagnostic output already exists")
    runtime = cpu_runtime(args.device)
    if args.command == "returns":
        if args.corpus is None:
            parser.error("returns requires --corpus")
        analyze_returns(args.corpus, args.output)
        return
    paths = checkpoint_args(args.checkpoint, args.artifact_root)
    if args.command == "verify":
        if args.corpus is None:
            parser.error("verify requires --corpus")
        verify_greedy(args.corpus, args.output, load_models(paths), runtime)
        return
    if args.command == "capture":
        if args.seed_count <= 0 or not 0 < args.max_states <= 64:
            parser.error("seed count must be positive and states must be in 1..64")
        roots = [ROOT / "configs", ROOT / "local/runs", args.artifact_root / "configs",
                 args.artifact_root / "local/runs", ROOT / "local/reports",
                 args.artifact_root / "local/reports"]
        scanned = check_seed_collisions(roots, args.seed_start, args.seed_count)
        maximum = 4096 if args.max_steps is None else args.max_steps
        if not 0 < maximum <= 4096:
            parser.error("capture --max-steps must be in 1..4096")
        capture(args.output, load_models(paths), runtime, seed_start=args.seed_start,
                seed_count=args.seed_count, max_steps=maximum, max_states=args.max_states,
                scanned=scanned)
    else:
        if args.corpus is None:
            parser.error("compare requires --corpus")
        maximum = 256 if args.max_steps is None else args.max_steps
        if not 0 < maximum <= 256:
            parser.error("compare --max-steps must be in 1..256")
        compare(args.corpus, args.output, load_models(paths), runtime, max_steps=maximum)


if __name__ == "__main__":
    main()
