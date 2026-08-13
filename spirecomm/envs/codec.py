"""Canonical battle payload codec used by every STS environment backend."""

from __future__ import annotations

from dataclasses import asdict
from typing import Any, Iterable

import numpy as np

from spirecomm.envs.contracts import (
    CHOICE_SOURCE_TO_INDEX,
    CHOICE_TASK_TO_INDEX,
    INTENT_TO_INDEX,
    MAX_ENEMIES,
    MAX_HAND,
    MAX_CHOICES,
    MAX_LEGAL_ACTIONS,
    MAX_POTIONS,
    LegalAction,
)
from spirecomm.envs.vocab import (
    CARD_IDS,
    CARD_ID_TO_INDEX,
    ENEMY_POWER_IDS,
    PLAYER_POWER_IDS,
    POTION_IDS,
    POTION_ID_TO_INDEX,
    normalize_card_id,
    normalize_potion_id,
    normalize_power_id,
)


def is_combat_payload(payload: dict[str, Any]) -> bool:
    game_state = payload.get("game_state") or {}
    return (
        bool(payload.get("in_game"))
        and game_state.get("room_phase") == "COMBAT"
        and bool(game_state.get("combat_state"))
    )


def alive_enemy_indices(game_state: dict[str, Any]) -> list[int]:
    combat = game_state.get("combat_state") or {}
    return [
        index
        for index, enemy in enumerate(combat.get("monsters") or [])
        if enemy.get("current_hp", 0) > 0
        and not enemy.get("half_dead", False)
        and not enemy.get("is_gone", False)
    ]


def choice_count(game_state: dict[str, Any]) -> int:
    choices = game_state.get("choice_list")
    if isinstance(choices, list):
        return len(choices)

    screen = game_state.get("screen_state") or {}
    for key in ("options", "cards", "hand", "rewards", "relics", "next_nodes"):
        value = screen.get(key)
        if isinstance(value, list):
            return len(value)
    return 0


def generate_legal_actions(payload: dict[str, Any]) -> list[LegalAction]:
    """Generate semantic actions legal in the exact canonical payload."""

    if not payload.get("ready_for_command", False) or payload.get("error"):
        return []

    explicit = payload.get("_legal_actions")
    if explicit is not None:
        actions = [LegalAction(**item) for item in explicit]
        if len(actions) > MAX_LEGAL_ACTIONS:
            raise RuntimeError(
                f"Canonical action count {len(actions)} exceeds "
                f"MAX_LEGAL_ACTIONS={MAX_LEGAL_ACTIONS}"
            )
        return actions

    available = {str(command).lower() for command in payload.get("available_commands", [])}
    game_state = payload.get("game_state") or {}
    combat = game_state.get("combat_state") or {}
    actions: list[LegalAction] = []

    if "play" in available:
        targets = alive_enemy_indices(game_state)
        for zero_based_index, card in enumerate(combat.get("hand") or []):
            if not card.get("is_playable", False):
                continue
            card_index = zero_based_index + 1
            if card.get("has_target", False):
                card_targets = card.get("playable_targets")
                for target_index in targets if card_targets is None else card_targets:
                    actions.append(
                        LegalAction(
                            kind="play",
                            card_index=card_index,
                            target_index=target_index,
                            command=f"play {card_index} {target_index}",
                        )
                    )
            else:
                actions.append(
                    LegalAction(
                        kind="play",
                        card_index=card_index,
                        command=f"play {card_index}",
                    )
                )

    if "potion" in available:
        targets = alive_enemy_indices(game_state)
        for potion_index, potion in enumerate(game_state.get("potions") or []):
            if potion.get("can_use", False):
                if potion.get("requires_target", False):
                    for target_index in targets:
                        actions.append(
                            LegalAction(
                                kind="potion",
                                potion_index=potion_index,
                                target_index=target_index,
                                command=f"potion use {potion_index} {target_index}",
                            )
                        )
                else:
                    actions.append(
                        LegalAction(
                            kind="potion",
                            potion_index=potion_index,
                            command=f"potion use {potion_index}",
                        )
                    )
            if potion.get("can_discard", False):
                actions.append(
                    LegalAction(
                        kind="discard_potion",
                        potion_index=potion_index,
                        command=f"potion discard {potion_index}",
                    )
                )

    if "choose" in available:
        for index in range(choice_count(game_state)):
            actions.append(
                LegalAction(kind="choose", choice_index=index, command=f"choose {index}")
            )

    for command, kind in (
        ("proceed", "proceed"),
        ("confirm", "proceed"),
        ("cancel", "cancel"),
        ("return", "cancel"),
        ("leave", "cancel"),
        ("skip", "cancel"),
        ("end", "end_turn"),
    ):
        if command in available:
            actions.append(LegalAction(kind=kind, command=command))

    if len(actions) > MAX_LEGAL_ACTIONS:
        raise RuntimeError(
            f"Canonical action count {len(actions)} exceeds MAX_LEGAL_ACTIONS={MAX_LEGAL_ACTIONS}"
        )
    return actions


def parse_battle_observation(payload: dict[str, Any]) -> dict[str, np.ndarray | int]:
    game_state = payload.get("game_state") or {}
    combat = game_state.get("combat_state") or {}
    player = combat.get("player") or {}
    hand = combat.get("hand") or []
    enemies = combat.get("monsters") or []
    choice = combat.get("choice") or {}
    potions = game_state.get("potions") or []
    orbs = player.get("orbs") or []

    def power_amounts(powers: Iterable[dict[str, Any]] | None, vocab: tuple[str, ...]):
        amounts = np.zeros(len(vocab), dtype=np.int16)
        index_by_id = {power_id: index for index, power_id in enumerate(vocab)}
        for power in powers or []:
            power_id = normalize_power_id(power.get("id") or power.get("name"))
            amounts[index_by_id.get(power_id, 0)] += int(power.get("amount") or 0)
        return amounts

    hand_costs = np.full(MAX_HAND, -3, dtype=np.int16)
    hand_playable = np.zeros(MAX_HAND, dtype=np.int8)
    hand_card_ids = np.zeros(MAX_HAND, dtype=np.int16)
    hand_upgrades = np.zeros(MAX_HAND, dtype=np.int16)
    for index, card in enumerate(hand[:MAX_HAND]):
        hand_costs[index] = int(card.get("cost", -3))
        hand_playable[index] = int(bool(card.get("is_playable", False)))
        hand_card_ids[index] = CARD_ID_TO_INDEX[normalize_card_id(card.get("id"))]
        hand_upgrades[index] = int(card.get("upgrades", 0))

    def pile_counts(name: str) -> np.ndarray:
        counts = np.zeros(len(CARD_IDS), dtype=np.int16)
        for card in combat.get(name) or []:
            card_id = normalize_card_id(card.get("id"))
            counts[CARD_ID_TO_INDEX[card_id]] += 1
        return counts

    enemy_hp = np.zeros((MAX_ENEMIES, 2), dtype=np.int32)
    enemy_block = np.zeros(MAX_ENEMIES, dtype=np.int32)
    enemy_intents = np.zeros(MAX_ENEMIES, dtype=np.int16)
    enemy_powers = np.zeros((MAX_ENEMIES, len(ENEMY_POWER_IDS)), dtype=np.int16)
    for index, enemy in enumerate(enemies[:MAX_ENEMIES]):
        enemy_hp[index] = [int(enemy.get("current_hp", 0)), int(enemy.get("max_hp", 0))]
        enemy_block[index] = int(enemy.get("block", 0))
        enemy_intents[index] = INTENT_TO_INDEX.get(str(enemy.get("intent", "UNKNOWN")), 0)
        enemy_powers[index] = power_amounts(enemy.get("powers"), ENEMY_POWER_IDS)

    choice_card_ids = np.zeros(MAX_CHOICES, dtype=np.int16)
    choice_options = choice.get("options") or []
    for index, option in enumerate(choice_options[:MAX_CHOICES]):
        choice_card_ids[index] = CARD_ID_TO_INDEX[normalize_card_id(option.get("id"))]

    potion_ids = np.zeros(MAX_POTIONS, dtype=np.int16)
    potion_usable = np.zeros(MAX_POTIONS, dtype=np.int8)
    for index, potion in enumerate(potions[:MAX_POTIONS]):
        potion_ids[index] = POTION_ID_TO_INDEX[
            normalize_potion_id(potion.get("id") or potion.get("name"))
        ]
        potion_usable[index] = int(bool(potion.get("can_use", False)))

    orb_ids = np.zeros(10, dtype=np.int8)
    orb_passive = np.zeros(10, dtype=np.int16)
    orb_evoke = np.zeros(10, dtype=np.int16)
    orb_index = {"EMPTY": 0, "DARK": 1, "FROST": 2, "PLASMA": 3, "LIGHTNING": 4}
    for index, orb in enumerate(orbs[:10]):
        orb_id = "".join(
            char for char in str(orb.get("id") or orb.get("name") or "EMPTY").upper()
            if char.isalnum()
        )
        orb_ids[index] = orb_index.get(orb_id, 0)
        orb_passive[index] = int(orb.get("passive_amount") or 0)
        orb_evoke[index] = int(orb.get("evoke_amount") or 0)

    return {
        "player_hp": np.array(
            [
                int(player.get("current_hp", game_state.get("current_hp", 0))),
                int(player.get("max_hp", game_state.get("max_hp", 0))),
            ],
            dtype=np.int32,
        ),
        "energy": int(player.get("energy", 0)),
        "energy_per_turn": int(player.get("energy_per_turn", 3)),
        "card_draw_per_turn": int(player.get("card_draw_per_turn", 5)),
        "turn": int(combat.get("turn", 0)),
        "player_block": int(player.get("block", 0)),
        "player_powers": power_amounts(player.get("powers"), PLAYER_POWER_IDS),
        "max_orbs": min(int(player.get("max_orbs", len(orbs)) or 0), 10),
        "orb_ids": orb_ids,
        "orb_passive": orb_passive,
        "orb_evoke": orb_evoke,
        "hand_count": min(len(hand), MAX_HAND),
        "hand_costs": hand_costs,
        "hand_playable": hand_playable,
        "hand_card_ids": hand_card_ids,
        "hand_upgrades": hand_upgrades,
        "draw_pile_counts": pile_counts("draw_pile"),
        "discard_pile_counts": pile_counts("discard_pile"),
        "exhaust_pile_counts": pile_counts("exhaust_pile"),
        "enemy_count": min(len(enemies), MAX_ENEMIES),
        "enemy_hp": enemy_hp,
        "enemy_block": enemy_block,
        "enemy_intents": enemy_intents,
        "enemy_powers": enemy_powers,
        "choice_task": CHOICE_TASK_TO_INDEX.get(str(choice.get("task", "UNKNOWN")), 0),
        "choice_source": CHOICE_SOURCE_TO_INDEX.get(str(choice.get("source", "")), 0),
        "choice_count": min(len(choice_options), MAX_CHOICES),
        "choice_card_ids": choice_card_ids,
        "potion_count": min(
            sum(normalize_potion_id(p.get("id")) != "EMPTY_POTION_SLOT" for p in potions),
            MAX_POTIONS,
        ),
        "potion_ids": potion_ids,
        "potion_usable": potion_usable,
    }


def _power_state(powers: Iterable[dict[str, Any]] | None) -> list[dict[str, Any]]:
    return [
        {
            "id": power.get("id"),
            "name": power.get("name"),
            "amount": power.get("amount"),
        }
        for power in powers or []
    ]


def _card_state(card: dict[str, Any], index: int) -> dict[str, Any]:
    return {
        "index": index,
        "id": card.get("id"),
        "name": card.get("name"),
        "uuid": card.get("uuid"),
        "cost": card.get("cost"),
        "base_cost": card.get("base_cost", card.get("cost")),
        "upgrades": card.get("upgrades", 0),
        "special_data": card.get("special_data", 0),
        "free_to_play_once": card.get("free_to_play_once", False),
        "retain": card.get("retain", False),
        "ethereal": card.get("ethereal", False),
        "is_playable": card.get("is_playable", False),
        "has_target": card.get("has_target", False),
        "selected": bool(card.get("selected", False)),
    }


def rich_battle_state(payload: dict[str, Any]) -> dict[str, Any]:
    """Stable, JSON-serializable battle state for agents and trace comparison."""

    game_state = payload.get("game_state") or {}
    combat = game_state.get("combat_state") or {}
    player = combat.get("player") or {}
    return {
        "floor": game_state.get("floor"),
        "turn": combat.get("turn"),
        "potions": [
            {
                "index": index,
                "id": potion.get("id"),
                "name": potion.get("name"),
                "can_use": bool(potion.get("can_use", False)),
                "can_discard": bool(potion.get("can_discard", False)),
                "requires_target": bool(potion.get("requires_target", False)),
            }
            for index, potion in enumerate(game_state.get("potions") or [])
        ],
        "relics": [
            {
                "id": relic.get("id"),
                "name": relic.get("name"),
                "counter": relic.get("counter", -1),
            }
            for relic in game_state.get("relics") or []
        ],
        "player": {
            "hp": player.get("current_hp", game_state.get("current_hp")),
            "max_hp": player.get("max_hp", game_state.get("max_hp")),
            "block": player.get("block", 0),
            "energy": player.get("energy", 0),
            "energy_per_turn": player.get("energy_per_turn", 3),
            "card_draw_per_turn": player.get("card_draw_per_turn", 5),
            "powers": _power_state(player.get("powers")),
            "max_orbs": player.get("max_orbs", len(player.get("orbs") or [])),
            "orbs": [
                {
                    "id": orb.get("id"),
                    "name": orb.get("name"),
                    "passive_amount": orb.get("passive_amount", 0),
                    "evoke_amount": orb.get("evoke_amount", 0),
                }
                for orb in player.get("orbs") or []
            ],
        },
        "hand": [
            _card_state(card, index + 1)
            for index, card in enumerate(combat.get("hand") or [])
        ],
        "draw_pile": [
            _card_state(card, index)
            for index, card in enumerate(combat.get("draw_pile") or [])
        ],
        "discard_pile": [
            _card_state(card, index)
            for index, card in enumerate(combat.get("discard_pile") or [])
        ],
        "exhaust_pile": [
            _card_state(card, index)
            for index, card in enumerate(combat.get("exhaust_pile") or [])
        ],
        "choice": {
            "task": (combat.get("choice") or {}).get("task"),
            "source": (combat.get("choice") or {}).get("source"),
            "options": [
                _card_state(card, int(card.get("choice_index", index)))
                for index, card in enumerate((combat.get("choice") or {}).get("options") or [])
            ],
        },
        "enemies": [
            {
                "index": index,
                "id": enemy.get("id"),
                "name": enemy.get("name"),
                "hp": enemy.get("current_hp"),
                "max_hp": enemy.get("max_hp"),
                "block": enemy.get("block", 0),
                "intent": enemy.get("intent", "UNKNOWN"),
                "move_id": enemy.get("move_id"),
                "move_base_damage": enemy.get("move_base_damage"),
                "move_damage": enemy.get("move_adjusted_damage"),
                "move_hits": enemy.get("move_hits"),
                "powers": _power_state(enemy.get("powers")),
                "half_dead": enemy.get("half_dead", False),
                "is_gone": enemy.get("is_gone", False),
            }
            for index, enemy in enumerate(combat.get("monsters") or [])
        ],
    }


def action_mask(actions: list[LegalAction]) -> np.ndarray:
    mask = np.zeros(MAX_LEGAL_ACTIONS, dtype=np.int8)
    mask[: len(actions)] = 1
    return mask


def actions_info(actions: list[LegalAction]) -> list[dict[str, Any]]:
    return [asdict(action) for action in actions]
