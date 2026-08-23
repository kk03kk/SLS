"""Select coverage-novel Act 1 seeds without launching Original."""

from __future__ import annotations

import argparse
from concurrent.futures import ProcessPoolExecutor
import json
import os
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend
from sls.contracts.continuation import continuation_simulator
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.validation.novelty import coverage_fingerprints, greedy_select
from sls.validation.policies import deterministic_action
from sls.validation.readiness import load_records
from sls.validation.truth import value_hash


def _baseline(root: Path) -> tuple[set[int], set[str]]:
    records, invalid = load_records(root)
    if invalid:
        raise ValueError(f"truth corpus contains invalid bundles: {invalid}")
    seeds: set[int] = set()
    fingerprints: set[str] = set()
    for record in records.values():
        seeds.add(int(record.manifest["seed"]))
        for boundary in record.boundaries:
            if (boundary.get("comparison") or {}).get("status") != "MATCH":
                continue
            decision = boundary.get("canonical_original_decision") or {}
            continuation = (boundary.get("continuation") or {}).get("original") or {}
            fingerprints.update(coverage_fingerprints(
                decision.get("observation") or {}, cursor=boundary.get("cursor") or {},
                continuation=continuation,
                selected_action=boundary.get("selected_action"),
            ))
    return seeds, fingerprints


def _candidate(seed: int, variant: int, max_steps: int) -> dict[str, object]:
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    decision = backend.reset(seed)
    fingerprints: set[str] = set()
    boundaries = 0
    max_floor = 0
    while boundaries < max_steps:
        action = None if decision.terminal else deterministic_action(decision, decision, variant=variant)
        state = backend.raw_state
        public = state.get("public_run") or {}
        cursor = {
            "screen": decision.observation.screen.value,
            "room": public.get("room_type"),
        }
        fingerprints.update(coverage_fingerprints(
            decision.observation.to_dict(), cursor=cursor,
            continuation=continuation_simulator(state),
            selected_action=None if action is None else action.to_dict(),
        ))
        boundaries += 1
        max_floor = max(max_floor, decision.observation.run.floor)
        if decision.terminal:
            break
        decision = backend.step(action).decision
    return {
        "seed": seed, "variant": variant, "boundary_count": boundaries,
        "max_floor": max_floor, "terminal": decision.terminal,
        "fingerprints": sorted(fingerprints),
    }


def select(
    root: Path, *, seed_start: int, seed_end: int, variants: tuple[int, ...],
    max_steps: int, count: int, workers: int = 1, progress: bool = False,
) -> dict[str, object]:
    excluded, baseline = _baseline(root)
    tasks = [
        (seed, variant, max_steps)
        for seed in range(seed_start, seed_end + 1) if seed not in excluded
        for variant in variants
    ]
    if workers <= 0:
        raise ValueError("workers must be positive")
    if workers == 1:
        iterator = (_candidate(*task) for task in tasks)
        executor = None
    else:
        executor = ProcessPoolExecutor(max_workers=workers)
        iterator = executor.map(_candidate_task, tasks, chunksize=max(1, len(variants)))
    candidates = []
    try:
        for index, candidate in enumerate(iterator, 1):
            candidates.append(candidate)
            if progress and (index % max(1, 100 * len(variants)) == 0 or index == len(tasks)):
                print(f"scanned {index}/{len(tasks)} seed-variants", file=sys.stderr, flush=True)
    finally:
        if executor is not None:
            executor.shutdown()
    selections = greedy_select(candidates, baseline, count=count)
    payload: dict[str, object] = {
        "schema": "sls-act1-validation-selection-v1",
        "candidate_seed_range": [seed_start, seed_end],
        "variants": list(variants), "max_steps": max_steps,
        "excluded_seeds": sorted(excluded),
        "baseline_fingerprint_sha256": value_hash(sorted(baseline)),
        "baseline_fingerprint_count": len(baseline),
        "selections": selections,
    }
    payload["selection_sha256"] = value_hash(payload)
    return payload


def _candidate_task(task: tuple[int, int, int]) -> dict[str, object]:
    return _candidate(*task)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--truth-root", type=Path, default=ROOT / "validation-results" / "truth")
    parser.add_argument("--seed-start", type=int, default=0)
    parser.add_argument("--seed-end", type=int, default=999)
    parser.add_argument("--variants", default="0,1,2,3")
    parser.add_argument("--max-steps", type=int, default=128)
    parser.add_argument("--count", type=int, default=4)
    parser.add_argument("--workers", type=int, default=max(1, min(8, os.cpu_count() or 1)))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    variants = tuple(int(value) for value in args.variants.split(",") if value.strip())
    if args.seed_start < 0 or args.seed_end < args.seed_start or not variants:
        parser.error("invalid seed range or variants")
    result = select(
        args.truth_root, seed_start=args.seed_start, seed_end=args.seed_end,
        variants=variants, max_steps=args.max_steps, count=args.count,
        workers=args.workers, progress=True,
    )
    text = json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
