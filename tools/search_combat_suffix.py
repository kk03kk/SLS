"""Search a winning semantic action suffix from an exact native checkpoint."""

from __future__ import annotations

import argparse
import gzip
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_HEART
from sls.contracts.continuation import continuation_simulator
from sls.validation.compare import canonical_simulator
from sls.validation.truth import value_hash


def _load(path: Path) -> dict:
    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8") as stream:
        value = json.load(stream)
    if not isinstance(value, dict):
        raise ValueError("native checkpoint must be a JSON object")
    return value


def search(checkpoint: Path, simulations: int) -> dict:
    backend = SimulatorBackend(IRONCLAD_A0_HEART)
    backend.load_checkpoint(_load(checkpoint))
    native_result = dict(backend._native.search_battle_suffix(int(simulations)))
    steps = []
    if native_result["found"]:
        for sequence, bits in enumerate(native_result["action_bits"]):
            by_bits = {value: key for key, value in backend._candidate_bits.items()}
            if int(bits) not in by_bits:
                raise RuntimeError(f"search returned non-legal action bits at step {sequence}: {bits}")
            candidate_id = by_bits[int(bits)]
            action = next(item for item in backend._adapt(backend.raw_state).actions if item.candidate_id == candidate_id)
            transition = backend.step(action)
            canonical = canonical_simulator(backend.raw_state)
            boundary_hash = value_hash({
                "state": canonical,
                "continuation": continuation_simulator(backend.raw_state),
            })
            steps.append({
                "sequence": sequence,
                "native_bits": int(bits),
                "semantic_action": action.to_dict(),
                "boundary_hash": boundary_hash,
                "terminated": transition.terminated,
                "outcome": backend.raw_state["public_run"]["outcome"],
            })
    return {
        "schema": "sls-semantic-action-plan-v1",
        "actions": [step["semantic_action"] for step in steps],
        "expected_boundaries": steps,
        "source": {
            "kind": "NATIVE_COMBAT_CHECKPOINT",
            "checkpoint": str(checkpoint.resolve()),
            "checkpoint_sha256": value_hash(_load(checkpoint)),
        },
        "search_evidence": {
            "schema": "sls-combat-suffix-search-v1",
            **native_result,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--simulations", type=int, choices=(150000, 450000), default=150000)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = search(args.checkpoint, args.simulations)
    text = json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0 if result["search_evidence"]["found"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
