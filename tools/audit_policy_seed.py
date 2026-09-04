"""Audit one deterministic policy seed and a diagnostic defense counterfactual."""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend  # noqa: E402
from sls.contracts import ActionKind, Decision  # noqa: E402
from sls.curriculum import (  # noqa: E402
    IRONCLAD_A0_ACT1,
    IRONCLAD_A0_ACT2,
    IRONCLAD_A0_ACT3,
    IRONCLAD_A0_FULLRUN,
    IRONCLAD_A0_HEART,
)
from sls.runtime import AgentRuntime, load_policy_artifact  # noqa: E402

_PROFILES = {
    "ACT1": IRONCLAD_A0_ACT1,
    "ACT2": IRONCLAD_A0_ACT2,
    "ACT3": IRONCLAD_A0_ACT3,
    "FULLRUN": IRONCLAD_A0_FULLRUN,
    "HEART": IRONCLAD_A0_HEART,
}


def _selected_card_id(decision: Decision, action_index: int) -> str | None:
    action = decision.actions[action_index]
    return next(
        (
            card.card_id for card in decision.observation.hand
            if card.instance_id == action.subject_id
        ),
        None,
    )


def _defense_override(decision: Decision, score: Any) -> int | None:
    observation = decision.observation
    if observation.screen.value != "COMBAT" or not observation.enemies:
        return None
    boss = observation.run.visible_boss_id
    if not boss or not any(enemy.monster_id == boss for enemy in observation.enemies):
        return None
    incoming = sum(
        enemy.intent_damage * enemy.intent_hits
        for enemy in observation.enemies if enemy.current_hp > 0
    )
    if incoming <= observation.player.block:
        return None
    candidates = [
        item for item in score.actions
        if _selected_card_id(decision, item.index) == "DEFEND_RED"
    ]
    return max(candidates, key=lambda item: item.probability).index if candidates else None


def _run(artifact: Any, seed: int, *, block_counterfactual: bool) -> dict[str, Any]:
    profile = _PROFILES[artifact.metadata.goal]
    backend = SimulatorBackend(profile)
    decision = backend.reset(seed)
    runtime = AgentRuntime(backend, artifact)  # type: ignore[arg-type]
    action_kinds: Counter[str] = Counter()
    boss_cards: Counter[str] = Counter()
    boss_entries: list[dict[str, Any]] = []
    entered: set[tuple[int, str]] = set()
    overrides: list[dict[str, Any]] = []
    targeted_while_sharp_hide = 0
    potion_uses = 0
    potion_discards = 0
    steps = 0
    transition = None
    while not decision.terminal and steps < 4096:
        score = runtime.score(decision)
        selected_index = score.recommended.index
        if block_counterfactual:
            alternate = _defense_override(decision, score)
            if alternate is not None and alternate != selected_index:
                overrides.append({
                    "step": steps,
                    "act": decision.observation.run.act,
                    "floor": decision.observation.run.floor,
                    "model_action": decision.actions[selected_index].to_dict(),
                    "override_action": decision.actions[alternate].to_dict(),
                })
                selected_index = alternate
        action = decision.actions[selected_index]
        action_kinds[action.kind.value] += 1
        observation = decision.observation
        boss = observation.run.visible_boss_id
        fighting_boss = (
            observation.screen.value == "COMBAT"
            and boss is not None
            and any(enemy.monster_id == boss for enemy in observation.enemies)
        )
        if fighting_boss:
            encounter = (observation.run.act, boss)
            if encounter not in entered:
                entered.add(encounter)
                boss_entries.append({
                    "act": observation.run.act,
                    "floor": observation.run.floor,
                    "boss": boss,
                    "hp": observation.player.current_hp,
                    "potions": [item.content_id for item in observation.potions],
                })
            card_id = _selected_card_id(decision, selected_index)
            if card_id is not None:
                boss_cards[card_id] += 1
            if (
                action.kind is ActionKind.PLAY_CARD
                and action.target_id is not None
                and any(power.content_id == "SHARP_HIDE" for power in observation.powers)
            ):
                targeted_while_sharp_hide += 1
        potion_uses += int(action.kind is ActionKind.USE_POTION)
        potion_discards += int(action.kind is ActionKind.DISCARD_POTION)
        transition = runtime.execute_scored_action(
            decision,
            score,
            selected_index,
            selection_source=(
                "manual" if selected_index != score.recommended.index else "model"
            ),
        )
        decision = transition.decision
        steps += 1
    if transition is None:
        raise RuntimeError("policy audit terminated before its first action")
    return {
        "actions": steps,
        "terminal": decision.terminal,
        "success": bool(transition.info.get("success")),
        "reason": transition.info.get("reason"),
        "act": decision.observation.run.act,
        "floor": decision.observation.run.floor,
        "hp": decision.observation.player.current_hp,
        "action_kinds": dict(sorted(action_kinds.items())),
        "boss_entries": boss_entries,
        "boss_card_plays": dict(sorted(boss_cards.items())),
        "targeted_card_plays_while_sharp_hide": targeted_while_sharp_hide,
        "potion_uses": potion_uses,
        "potion_discards": potion_discards,
        "overrides": overrides,
    }


def audit_policy_seed(artifact_path: Path, seed: int) -> dict[str, Any]:
    artifact = load_policy_artifact(artifact_path, device="cpu")
    if artifact.metadata.goal not in _PROFILES:
        raise ValueError(f"unsupported policy goal: {artifact.metadata.goal}")
    return {
        "schema": "sls-policy-seed-audit-v1",
        "artifact": str(artifact_path.resolve()),
        "goal": artifact.metadata.goal,
        "input_seed": seed,
        "native_seed_bits": seed & ((1 << 64) - 1),
        "baseline": _run(artifact, seed, block_counterfactual=False),
        "block_deficit_counterfactual": _run(
            artifact, seed, block_counterfactual=True,
        ),
        "counterfactual_scope": (
            "Diagnostic only: at boss combat boundaries, choose the highest-scored "
            "DEFEND_RED while incoming damage exceeds current block."
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--seed", type=int, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = audit_policy_seed(args.artifact, args.seed)
    rendered = json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True)
    if args.output is not None:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        temporary = args.output.with_suffix(args.output.suffix + ".tmp")
        temporary.write_text(rendered + "\n", encoding="utf-8")
        temporary.replace(args.output)
    print(rendered)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
