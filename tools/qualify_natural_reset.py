"""CPU-only natural-reset readiness; never collect PPO samples or train."""
from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]

from sls.diagnostics.cpu import cpu_runtime, load_models
from sls.diagnostics.reset_readiness import readiness


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument("--corpus", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--states", type=int, default=4)
    parser.add_argument("--device", choices=("cpu",), default="cpu")
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("readiness report already exists")
    runtime = cpu_runtime(args.device)
    readiness(args.corpus, args.output, load_models({"student": args.checkpoint})["student"], runtime, args.states)
    print(args.output)


if __name__ == "__main__":
    main()
