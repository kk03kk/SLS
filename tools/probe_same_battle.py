"""CPU-only matched combat diagnostics with full public history reconstruction."""
from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]

from sls.diagnostics.battle_probe import probe_battles
from sls.diagnostics.cpu import cpu_runtime, load_models
from tools.diagnose_cpu import checkpoint_args


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", action="append", required=True)
    parser.add_argument("--corpus", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--states", type=int, default=12)
    parser.add_argument("--max-steps", type=int, default=256)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("battle output already exists")
    if not 0 < args.states <= 64 or not 0 < args.max_steps <= 256:
        raise ValueError("invalid diagnostic bounds")
    runtime = cpu_runtime()
    probe_battles(args.corpus, args.output, load_models(checkpoint_args(args.checkpoint, ROOT)), runtime,
                  count=args.states, max_steps=args.max_steps)


if __name__ == "__main__":
    main()
