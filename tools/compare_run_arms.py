"""Compare two training runs arm-to-arm on their recorded per-seed evaluations.

A reward or hyperparameter comparison has to be decided on the paired per-seed
outcomes the runs already record, not on two independent success rates. Comparing
proportions instead of pairs throws away most of the power: with 512 seeds a real
3-point improvement is only detectable about a third of the time, while the same
seeds give a much sharper answer when the runs are matched seed by seed.

Every periodic evaluation in `stages/train/metrics.jsonl` carries its own
`seed_results`, so the pairing is available without re-running anything.

Usage::

    python tools/compare_run_arms.py \\
        --arm "progress=local/runs/...-progress_2m_r1" \\
        --arm "win=local/runs/...-win_2m_r1" \\
        --output local/reports/reward-arm-comparison.json

Each --arm takes LABEL=RUN_DIRECTORY and reads stages/train/metrics.jsonl from it.
The last evaluation in each file is compared by default; --step-selector final
picks the final-evaluation.json instead when it exists.
"""

from __future__ import annotations

import argparse
import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

SEED_RESULT_FIELDS = ("seed", "success", "reason", "steps", "floor")


@dataclass(frozen=True, slots=True)
class ArmEvaluation:
    label: str
    source: str
    seeds: dict[int, bool]
    steps: dict[int, int]
    environment_steps: int | None


def _from_seed_results(
    label: str, source: str, seed_results: list[dict],
    environment_steps: int | None = None,
) -> ArmEvaluation:
    if not seed_results:
        raise ValueError(f"{label}: empty seed results")
    seeds = [int(row["seed"]) for row in seed_results]
    if len(seeds) != len(set(seeds)):
        raise ValueError(f"{label}: duplicate evaluation seeds")
    if any(type(row["success"]) is not bool for row in seed_results):
        raise ValueError(f"{label}: success must be a boolean")
    return ArmEvaluation(
        label=label,
        source=source,
        seeds={int(row["seed"]): bool(row["success"]) for row in seed_results},
        steps={int(row["seed"]): int(row["steps"]) for row in seed_results},
        environment_steps=environment_steps,
    )


def load_metrics_arm(run: Path, label: str) -> ArmEvaluation:
    path = run / "stages/train/metrics.jsonl"
    if not path.is_file():
        raise ValueError(f"{label}: no metrics at {path}")
    records = [
        json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    evaluated = [r for r in records if r.get("evaluation")]
    if not evaluated:
        raise ValueError(f"{label}: {path} has no evaluation records")
    last = evaluated[-1]
    return _from_seed_results(
        label, f"{path} (update {last.get('update')})",
        last["evaluation"]["seed_results"], last.get("environment_steps"),
    )


def load_final_arm(run: Path, label: str) -> ArmEvaluation:
    path = run / "final-evaluation.json"
    if not path.is_file():
        raise ValueError(f"{label}: no final evaluation at {path}")
    payload = json.loads(path.read_text(encoding="utf-8"))
    return _from_seed_results(
        label, str(path), payload["result"]["seed_results"],
        payload.get("checkpoint_environment_steps"),
    )


def exact_mcnemar(b: int, c: int) -> float:
    n = b + c
    if n == 0:
        return 1.0
    tail = sum(math.comb(n, i) for i in range(0, min(b, c) + 1)) / 2 ** n
    return min(1.0, 2 * tail)


def wilson(k: int, n: int) -> tuple[float, float]:
    if n <= 0:
        return (0.0, 1.0)
    z = 1.959963984540054
    p = k / n
    denominator = 1 + z * z / n
    centre = (p + z * z / (2 * n)) / denominator
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denominator
    return centre - half, centre + half


def report(left: ArmEvaluation, right: ArmEvaluation, blocks: int) -> dict:
    if blocks <= 0:
        raise ValueError("block size must be positive")
    if set(left.seeds) != set(right.seeds):
        raise ValueError("paired comparison requires identical complete seed sets")
    seeds = sorted(left.seeds)
    if not seeds:
        raise ValueError("the two arms share no evaluation seeds")
    print(f"paired seeds: {len(seeds)}")
    print(f"  {left.label:<24} {sum(left.seeds[s] for s in seeds):>5}/{len(seeds)} "
          f"= {sum(left.seeds[s] for s in seeds) / len(seeds) * 100:6.2f}%   [{left.source}]")
    print(f"  {right.label:<24} {sum(right.seeds[s] for s in seeds):>5}/{len(seeds)} "
          f"= {sum(right.seeds[s] for s in seeds) / len(seeds) * 100:6.2f}%   [{right.source}]")

    b = sum(left.seeds[s] and not right.seeds[s] for s in seeds)
    c = sum(not left.seeds[s] and right.seeds[s] for s in seeds)
    p = exact_mcnemar(b, c)
    print(f"\npaired: {left.label} win / {right.label} lose = {b}; "
          f"{left.label} lose / {right.label} win = {c}; net {c - b:+d}; exact p = {p:.4f}")

    print(f"\n=== {blocks}-seed block breakdown ===")
    print(f"{'block start':>16} {'n':>5} {left.label:>12} {right.label:>12} {'net':>5}")
    block_rows = []
    ordered = sorted(seeds)
    for start in range(0, len(ordered), blocks):
        chunk = ordered[start:start + blocks]
        lw = sum(left.seeds[s] for s in chunk)
        rw = sum(right.seeds[s] for s in chunk)
        print(f"{chunk[0]:>16} {len(chunk):>5} "
              f"{lw / len(chunk) * 100:>11.2f}% {rw / len(chunk) * 100:>11.2f}% "
              f"{rw - lw:>+5}")
        block_rows.append({
            "start": chunk[0], "n": len(chunk),
            "left_wins": lw, "right_wins": rw,
        })

    verdict = (
        f"{right.label} is better (p={p:.4f})" if p < 0.05 and c > b
        else f"{left.label} is better (p={p:.4f})" if p < 0.05 and b > c
        else f"no significant difference (p={p:.4f})"
    )
    print(f"\nverdict at alpha=0.05: {verdict}")
    return {
        "schema": "sls-arm-comparison-v1",
        "arms": [left.label, right.label],
        "sources": [left.source, right.source],
        "paired_seeds": len(seeds),
        "seed_range": [seeds[0], seeds[-1] + 1],
        "results": {
            left.label: {
                "wins": sum(left.seeds[s] for s in seeds),
                "successes": sum(left.seeds[s] for s in seeds),
                "success_rate": sum(left.seeds[s] for s in seeds) / len(seeds),
                "ci95": list(wilson(sum(left.seeds[s] for s in seeds), len(seeds))),
            },
            right.label: {
                "wins": sum(right.seeds[s] for s in seeds),
                "successes": sum(right.seeds[s] for s in seeds),
                "success_rate": sum(right.seeds[s] for s in seeds) / len(seeds),
                "ci95": list(wilson(sum(right.seeds[s] for s in seeds), len(seeds))),
            },
        },
        "discordant": {f"{left.label}_win": b, f"{right.label}_win": c},
        "net": c - b,
        "exact_mcnemar_p": p,
        "verdict": verdict,
        "blocks": block_rows,
    }


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--arm", action="append", required=True,
        help="LABEL=RUN_DIRECTORY; exactly two are compared",
    )
    parser.add_argument("--output", type=Path)
    parser.add_argument("--blocks", type=int, default=512)
    parser.add_argument(
        "--selector", choices=("metrics", "final"), default="final",
        help="'final' prefers final-evaluation.json and falls back to metrics.jsonl",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    if len(args.arm) != 2:
        raise ValueError("exactly two --arm arguments are required")
    arms: list[ArmEvaluation] = []
    for spec in args.arm:
        if "=" not in spec:
            raise ValueError(f"--arm must be LABEL=RUN_DIRECTORY, got {spec!r}")
        label, _, directory = spec.partition("=")
        run = Path(directory)
        loader: Callable[[Path, str], ArmEvaluation] = (
            load_final_arm if args.selector == "final" else load_metrics_arm
        )
        try:
            arms.append(loader(run, label))
        except ValueError:
            if args.selector == "metrics":
                raise
            arms.append(load_metrics_arm(run, label))

    record = report(arms[0], arms[1], args.blocks)
    if args.output is not None:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(
            json.dumps(record, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"\nwrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
