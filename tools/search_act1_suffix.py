"""Find the shortest non-combat suffix leading to a searched Act 1 Boss win."""

from __future__ import annotations

import argparse
from collections import deque
import gzip
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A0_HEART
from sls.contracts.continuation import continuation_simulator
from sls.validation.compare import canonical_simulator
from sls.validation.truth import value_hash


def _checkpoint(path: Path) -> dict:
    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8") as stream:
        value = json.load(stream)
    if not isinstance(value, dict):
        raise ValueError("checkpoint must be a JSON object")
    return value


def _combat_actions(backend: SimulatorBackend, bits_values: list[int]) -> list[Action]:
    result = []
    for sequence, bits in enumerate(bits_values):
        by_bits = {value: key for key, value in backend._candidate_bits.items()}
        if bits not in by_bits:
            raise RuntimeError(f"search returned non-legal bits at combat step {sequence}: {bits}")
        candidate = by_bits[bits]
        decision = backend._adapt(backend.raw_state)
        action = next(item for item in decision.actions if item.candidate_id == candidate)
        result.append(action)
        backend.step(action)
    return result


def _plan_evidence(checkpoint: dict, actions: list[Action]) -> list[dict]:
    backend = SimulatorBackend(IRONCLAD_A0_HEART)
    backend.load_checkpoint(checkpoint)
    result = []
    for sequence, action in enumerate(actions):
        native_bits = backend._candidate_bits[action.candidate_id]
        transition = backend.step(action)
        result.append({
            "sequence": sequence,
            "native_bits": int(native_bits),
            "semantic_action": action.to_dict(),
            "boundary_hash": value_hash({
                "state": canonical_simulator(backend.raw_state),
                "continuation": continuation_simulator(backend.raw_state),
            }),
            "terminated": transition.terminated,
        })
    return result


def search(
    checkpoint: dict, *, simulations: int, max_prefix_steps: int,
    target: str = "boss",
) -> dict:
    queue = deque([(checkpoint, [])])
    seen: set[str] = set()
    boss_states = 0
    restore_failures = 0
    initial = SimulatorBackend(IRONCLAD_A0_HEART)
    initial_decision = initial.load_checkpoint(checkpoint)
    start_floor = initial_decision.observation.run.floor
    starts_in_combat = initial_decision.observation.screen.value == "COMBAT"
    while queue:
        state, prefix = queue.popleft()
        digest = value_hash(state)
        if digest in seen:
            continue
        seen.add(digest)
        backend = SimulatorBackend(IRONCLAD_A0_HEART)
        try:
            decision = backend.load_checkpoint(state)
        except (ValueError, RuntimeError):
            restore_failures += 1
            continue
        observation = decision.observation
        target_combat = bool(
            observation.screen.value == "COMBAT"
            and (
                (target == "boss" and observation.run.floor == 16)
                or (
                    target == "next-combat"
                    and observation.run.floor >= start_floor + (0 if starts_in_combat else 1)
                )
            )
        )
        if target_combat:
            boss_states += 1
            native = dict(backend._native.search_battle_suffix(simulations))
            if native["found"]:
                combat = _combat_actions(backend, [int(value) for value in native["action_bits"]])
                actions = prefix + combat
                return {
                    "schema": "sls-semantic-action-plan-v1",
                    "actions": [action.to_dict() for action in actions],
                    "expected_boundaries": _plan_evidence(checkpoint, actions),
                    "search_evidence": {
                        "schema": "sls-act1-suffix-search-v1",
                        "checkpoint_hash": value_hash(checkpoint),
                        "states": len(seen),
                        "restore_failures": restore_failures,
                        "boss_states": boss_states,
                        "prefix_steps": len(prefix),
                        "combat_steps": len(combat),
                        "requested_simulations": simulations,
                        "completed_simulations": int(native["completed_simulations"]),
                        "outcome_player_hp": int(native["outcome_player_hp"]),
                        "final_state_hash": value_hash(backend.checkpoint()),
                        "deterministic_config": {
                            "algorithm": "BattleScumSearcher2",
                            "max_prefix_steps": max_prefix_steps,
                            "boss_simulations": simulations,
                            "target": target,
                        },
                    },
                }
            continue
        if observation.screen.value == "COMBAT" or len(prefix) >= max_prefix_steps:
            continue
        for action in decision.actions:
            candidate = SimulatorBackend(IRONCLAD_A0_HEART)
            try:
                candidate.load_checkpoint(state)
                transition = candidate.step(action)
            except (ValueError, RuntimeError):
                continue
            if not transition.terminated:
                queue.append((candidate.checkpoint(), prefix + [action]))
    return {
        "schema": "sls-semantic-action-plan-v1",
        "actions": [],
        "search_evidence": {
            "schema": "sls-act1-suffix-search-v1", "checkpoint_hash": value_hash(checkpoint),
            "states": len(seen), "restore_failures": restore_failures,
            "boss_states": boss_states, "requested_simulations": simulations,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--simulations", type=int, choices=(150000, 450000), default=150000)
    parser.add_argument("--max-prefix-steps", type=int, default=14)
    parser.add_argument("--target", choices=("boss", "next-combat"), default="boss")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = search(
        _checkpoint(args.checkpoint), simulations=args.simulations,
        max_prefix_steps=args.max_prefix_steps, target=args.target,
    )
    text = json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0 if result["actions"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
