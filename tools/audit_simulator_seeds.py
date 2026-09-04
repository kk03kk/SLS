"""Long-run simulator invariant and checkpoint round-trip audit."""

from __future__ import annotations

import argparse
import json
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend  # noqa: E402
from sls.curriculum import IRONCLAD_A0_FULLRUN  # noqa: E402


def _decision_payload(decision: object) -> dict[str, object]:
    return {
        "observation": decision.observation.to_dict(),
        "actions": [action.to_dict() for action in decision.actions],
        "terminal": decision.terminal,
    }


def audit_seeds(start: int, count: int, *, max_actions: int) -> dict[str, object]:
    failures: list[dict[str, object]] = []
    completed = 0
    total_actions = 0
    for seed in range(start, start + count):
        chooser = random.Random(seed ^ 0x5A17C0DE)
        backend = SimulatorBackend(IRONCLAD_A0_FULLRUN)
        try:
            decision = backend.reset(seed)
            seen: dict[str, int] = {}
            for step in range(max_actions):
                candidate_ids = [action.candidate_id for action in decision.actions]
                if len(candidate_ids) != len(set(candidate_ids)):
                    raise AssertionError("duplicate candidate identity")
                if decision.terminal:
                    if decision.actions:
                        raise AssertionError("terminal decision exposes actions")
                    completed += 1
                    break
                if not decision.actions:
                    raise AssertionError("nonterminal decision has no legal action")
                signature = json.dumps(_decision_payload(decision), sort_keys=True)
                seen[signature] = seen.get(signature, 0) + 1
                if seen[signature] > 4:
                    raise AssertionError("decision boundary repeated more than four times")
                if step % 17 == 0:
                    checkpoint = backend.checkpoint()
                    restored = SimulatorBackend(IRONCLAD_A0_FULLRUN)
                    round_trip = restored.load_checkpoint(checkpoint)
                    if _decision_payload(round_trip) != _decision_payload(decision):
                        raise AssertionError("checkpoint decision round-trip mismatch")
                    if restored.checkpoint() != checkpoint:
                        raise AssertionError("checkpoint state round-trip mismatch")
                action = decision.actions[chooser.randrange(len(decision.actions))]
                transition = backend.step(action)
                if transition.truncated:
                    raise AssertionError("backend truncated an episode")
                decision = transition.decision
                total_actions += 1
            else:
                raise AssertionError("episode action limit reached")
        except Exception as error:
            failures.append({"seed": seed, "error": f"{type(error).__name__}: {error}"})
            if len(failures) >= 100:
                break
    return {
        "schema": "sls-simulator-seed-invariants-v1",
        "seed_range": [start, start + count],
        "requested_seeds": count,
        "completed_seeds": completed,
        "total_actions": total_actions,
        "failures": failures,
        "passed": completed == count and not failures,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-start", type=int, default=0)
    parser.add_argument("--seeds", type=int, default=10_000)
    parser.add_argument("--max-actions", type=int, default=4096)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = audit_seeds(args.seed_start, args.seeds, max_actions=args.max_actions)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(args.output)
    print(json.dumps({key: result[key] for key in ("requested_seeds", "completed_seeds", "passed")}))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
