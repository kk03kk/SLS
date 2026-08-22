"""Original-game <-> simulator FullRun differential helpers.

CommunicationMod exposes several UI-only confirmation boundaries which the
native simulator deliberately folds into one semantic action.  This module
keeps those protocol details out of parity assertions and provides the command
translation used by the live oracle entry point.
"""

from __future__ import annotations

from collections.abc import Mapping, Sequence
from typing import Any

from sls.content.normalize import normalize_card_id, normalize_content_id, normalize_potion_id


ROOM_SYMBOLS = {
    "M": "MONSTER", "?": "EVENT", "$": "SHOP", "E": "ELITE",
    "T": "TREASURE", "R": "REST",
}


def _cards(cards: Sequence[Mapping[str, Any]]) -> list[tuple[str, int]]:
    return [
        (normalize_card_id(card.get("content_id", card.get("id"))),
         int(card.get("upgrades", 0)))
        for card in cards
    ]


def _rng(payload: Mapping[str, Any]) -> dict[str, dict[str, int]]:
    return {
        str(name): {
            "counter": int(state["counter"]),
            "seed0": int(state["seed0"]),
            "seed1": int(state["seed1"]),
        }
        for name, state in payload.items()
    }


def _original_map(game: Mapping[str, Any]) -> list[tuple[Any, ...]]:
    nodes = []
    for node in game.get("map") or []:
        # CommunicationMod numbers the boss row as y=16 while Lightspeed's
        # public map uses its internal terminal coordinate y=15.
        edges = sorted(
            (int(edge["x"]), 15 if int(edge["y"]) == 16 else int(edge["y"]))
            for edge in node.get("children") or []
        )
        nodes.append((
            int(node["x"]), int(node["y"]),
            ROOM_SYMBOLS.get(str(node.get("symbol")), str(node.get("symbol"))),
            tuple(edges),
        ))
    return sorted(nodes)


def _simulator_map(state: Mapping[str, Any]) -> list[tuple[Any, ...]]:
    nodes = []
    for node in state.get("public_map") or []:
        # CommunicationMod omits The Ending's terminal Heart node from its map
        # payload. It becomes a boss room through the fixed Act 4 progression,
        # so the extra simulator node is not a gameplay-semantic difference.
        if int((state.get("public_run") or {}).get("act", 0)) == 4 and str(node["room_type"]) == "BOSS":
            continue
        edges = []
        for target in node.get("outgoing_node_ids") or []:
            _, x, y = str(target).split(":")
            edges.append((int(x), int(y)))
        nodes.append((
            int(node["x"]), int(node["y"]), str(node["room_type"]), tuple(sorted(edges)),
        ))
    return sorted(nodes)


def _original_combat(
    game: Mapping[str, Any], parity_intents: Sequence[Mapping[str, Any]] = (),
) -> dict[str, Any] | None:
    combat = game.get("combat_state")
    if not combat:
        return None
    player = combat.get("player") or {}
    return {
        "turn": int(combat.get("turn", 0)),
        "player": (
            int(player.get("current_hp", game.get("current_hp", 0))),
            int(player.get("max_hp", game.get("max_hp", 0))),
            int(player.get("block", 0)), int(player.get("energy", 0)),
        ),
        "hand": _cards(combat.get("hand") or []),
        "draw": _cards(combat.get("draw_pile") or []),
        "discard": _cards(combat.get("discard_pile") or []),
        "exhaust": _cards(combat.get("exhaust_pile") or []),
        "monsters": [
            _original_monster(
                monster, parity_intents[index] if index < len(parity_intents) else {},
            )
            for index, monster in enumerate(combat.get("monsters") or [])
        ],
    }


def _original_monster(
    monster: Mapping[str, Any], parity_intent: Mapping[str, Any],
) -> tuple[Any, ...]:
    gone = bool(monster.get("is_gone", False)) or int(monster.get("current_hp", 0)) <= 0
    intent = (
        "UNKNOWN" if gone
        else str(parity_intent.get("intent") or monster.get("intent", "UNKNOWN"))
    )
    is_attack = intent.upper() in {"ATTACK", "ATTACK_BUFF", "ATTACK_DEBUFF", "ATTACK_DEFEND"}
    damage = parity_intent.get("damage")
    if damage is None:
        damage = monster.get("move_adjusted_damage", 0)
    return (
        normalize_content_id(monster.get("id")),
        int(monster.get("current_hp", 0)), int(monster.get("max_hp", 0)),
        int(monster.get("block", 0)), intent,
        int(damage) if is_attack and not gone else 0,
        int(monster.get("move_hits", monster.get("intent_hits", 0))) if is_attack and not gone else 0,
        gone,
    )


def _simulator_combat(state: Mapping[str, Any]) -> dict[str, Any] | None:
    combat = state.get("public_combat")
    if not combat:
        return None
    player = combat.get("player") or {}
    return {
        "turn": int(combat.get("turn", 0)),
        "player": (
            int(player.get("current_hp", 0)), int(player.get("max_hp", 0)),
            int(player.get("block", 0)), int(player.get("energy", 0)),
        ),
        "hand": _cards(combat.get("hand") or []),
        "draw": _cards(combat.get("draw_pile") or []),
        "discard": _cards(combat.get("discard_pile") or []),
        "exhaust": _cards(combat.get("exhaust_pile") or []),
        "monsters": [
            (
                normalize_content_id(monster.get("content_id")),
                int(monster.get("current_hp", 0)), int(monster.get("max_hp", 0)),
                int(monster.get("block", 0)),
                "UNKNOWN" if bool(monster.get("is_gone", False)) else str(monster.get("intent", "UNKNOWN")),
                0 if bool(monster.get("is_gone", False)) else int(monster.get("intent_damage", 0)),
                0 if bool(monster.get("is_gone", False)) else int(monster.get("intent_hits", 0)),
                bool(monster.get("is_gone", False)),
            )
            for monster in combat.get("monsters") or []
        ],
    }


def canonical_original(payload: Mapping[str, Any]) -> dict[str, Any]:
    """Extract only cross-backend, gameplay-semantic state."""

    game = payload.get("game_state") or {}
    parity_run = game.get("_parity_run") or payload.get("_parity_run") or {}
    relics = [
        (normalize_content_id(item.get("id")), int(item.get("counter", -1)))
        for item in game.get("relics") or []
    ]
    # Lightspeed stores the ordinary no-counter value as 0 in its public
    # inventory; CommunicationMod uses -1.  It is not a gameplay divergence.
    relics = [(content_id, max(0, counter)) for content_id, counter in relics]
    continuation = payload.get("_continuation") or game.get("_continuation") or {}
    terminal = str(
        continuation.get("screen") or game.get("screen_type") or ""
    ).upper() in {"DEATH", "VICTORY", "GAME_OVER", "COMPLETE"}
    return {
        "run": {
            "act": int(game.get("act", 0)), "floor": int(game.get("floor", 0)),
            "hp": int(game.get("current_hp", 0)), "max_hp": int(game.get("max_hp", 0)),
            "gold": int(game.get("gold", 0)),
            "boss": normalize_content_id(game.get("act_boss")),
            "keys": (
                bool(parity_run.get("ruby_key", False)),
                bool(parity_run.get("emerald_key", False)),
                bool(parity_run.get("sapphire_key", False)),
            ),
        },
        "deck": _cards(game.get("deck") or []),
        "relics": relics,
        "potions": [normalize_potion_id(item.get("id")) for item in game.get("potions") or []],
        "map": _original_map(game),
        "combat": None if terminal else _original_combat(
            game, payload.get("_monster_intents") or (),
        ),
        "rng": _rng(payload.get("_rng") or game.get("_rng") or {}),
    }


def canonical_simulator(state: Mapping[str, Any]) -> dict[str, Any]:
    run = state["public_run"]
    player = state["player_state"]
    inventory = state["public_inventory"]
    visible_player = (state.get("public_combat") or {}).get("player") or player
    rng = (state.get("combat_checkpoint") or {}).get("rng") if state.get("public_combat") else state["rng"]
    return {
        "run": {
            "act": int(run["act"]), "floor": int(run["floor"]),
            "hp": int(visible_player["current_hp"]), "max_hp": int(visible_player["max_hp"]),
            "gold": int(run["gold"]), "boss": normalize_content_id(run["visible_boss_id"]),
            "keys": (bool(player["red_key"]), bool(player["green_key"]), bool(player["blue_key"])),
        },
        "deck": _cards(inventory.get("deck") or []),
        "relics": [
            (normalize_content_id(item.get("content_id")), max(0, int(item.get("counter", 0))))
            for item in inventory.get("relics") or []
        ],
        "potions": [normalize_potion_id(item.get("content_id")) for item in inventory.get("potions") or []],
        "map": _simulator_map(state),
        "combat": None if int(run.get("outcome", 1)) != 1 else _simulator_combat(state),
        "rng": _rng(rng or {}),
    }


def parity_differences(
    original: Mapping[str, Any], simulator: Mapping[str, Any], *, include_rng: bool = True,
    drop_dead_neow: bool = False,
) -> dict[str, tuple[Any, Any]]:
    left, right = canonical_original(original), canonical_simulator(simulator)
    if drop_dead_neow and int(left["run"]["floor"]) > 0:
        left.get("rng", {}).pop("neow", None)
        right.get("rng", {}).pop("neow", None)
    continuation = original.get("_continuation") or (original.get("game_state") or {}).get("_continuation") or {}
    if drop_dead_neow and continuation.get("post_combat"):
        for stream in ("monster_hp", "ai", "shuffle", "card_random", "misc"):
            left.get("rng", {}).pop(stream, None)
            right.get("rng", {}).pop(stream, None)
    if not include_rng:
        left.pop("rng", None)
        right.pop("rng", None)
    return {key: (left.get(key), right.get(key)) for key in left.keys() | right.keys() if left.get(key) != right.get(key)}


def semantic_screen_original(payload: Mapping[str, Any]) -> str:
    game = payload.get("game_state") or {}
    screen = str(game.get("screen_type") or "NONE").upper()
    if game.get("combat_state"):
        return "COMBAT"
    return {
        "GRID": "CARD_SELECT", "CARD_REWARD": "CARD_REWARD",
        "COMBAT_REWARD": "REWARDS", "BOSS_REWARD": "BOSS_RELIC",
        "SHOP_SCREEN": "SHOP", "SHOP_ROOM": "SHOP_ENTRY",
        "REST": "REST", "REST_ROOM": "REST", "CHEST": "TREASURE",
    }.get(screen, screen)


def semantic_screen_simulator(state: Mapping[str, Any]) -> str:
    if state.get("public_combat"):
        return "COMBAT"
    code = int(state["public_run"]["screen_state"])
    return {
        1: "EVENT", 2: "REWARDS", 3: "BOSS_RELIC", 4: "CARD_SELECT",
        5: "MAP", 6: "TREASURE", 7: "REST", 8: "SHOP", 9: "COMBAT",
    }.get(code, "TERMINAL")


def command_for_simulator_action(
    simulator: Mapping[str, Any], action: Mapping[str, Any], original: Mapping[str, Any],
) -> list[str]:
    """Translate one native semantic action into advertised Original commands."""

    game = original.get("game_state") or {}
    screen = semantic_screen_simulator(simulator)
    idx1, idx2 = int(action.get("idx1", 0)), int(action.get("idx2", 0))
    if screen == "COMBAT":
        kind = int(action["action_type"])
        target = int(action.get("target_index", 0))
        if action.get("requires_target"):
            simulator_monsters = (simulator.get("public_combat") or {}).get("monsters") or []
            selected = next(
                (
                    monster for monster in simulator_monsters
                    if monster.get("instance_id") == f"monster:{target}"
                ),
                None,
            )
            if selected is not None:
                wanted = normalize_content_id(selected.get("content_id"))
                simulator_matches = [
                    monster for monster in simulator_monsters
                    if not monster.get("is_gone")
                    and normalize_content_id(monster.get("content_id")) == wanted
                ]
                occurrence = simulator_matches.index(selected)
                original_matches = [
                    index for index, monster in enumerate(
                        (game.get("combat_state") or {}).get("monsters") or []
                    )
                    if not monster.get("is_gone")
                    and normalize_content_id(monster.get("id")) == wanted
                ]
                if occurrence >= len(original_matches):
                    raise RuntimeError(
                        f"Original combat has no occurrence {occurrence} of {wanted}"
                    )
                target = original_matches[occurrence]
        if kind == 0:
            command = f"play {int(action['source_index']) + 1}"
            if action.get("requires_target"):
                command += f" {target}"
            return [command]
        if kind == 1:
            command = f"potion use {int(action['source_index'])}"
            if action.get("requires_target"):
                command += f" {target}"
            return [command]
        if kind == 4:
            return ["end"]
        return [f"choose {int(action['source_index'])}"]
    if screen == "MAP":
        original_map_screen = game.get("screen_state") or {}
        nodes = original_map_screen.get("next_nodes") or []
        if original_map_screen.get("boss_available") and not nodes:
            return ["choose 0"]
        for index, node in enumerate(nodes):
            if int(node.get("x", -99)) == idx1:
                return [f"choose {index}"]
        raise RuntimeError(f"Original map has no reachable x={idx1}: {nodes}")
    if screen == "REWARDS":
        reward_type = int(action.get("reward_type", 0))
        if reward_type == 6:
            return ["proceed"]
        names = {0: {"CARD"}, 1: {"GOLD"}, 2: {"KEY", "SAPPHIRE_KEY", "EMERALD_KEY"},
                 3: {"POTION"}, 4: {"RELIC"}}
        wanted = names[reward_type]
        rewards = (game.get("screen_state") or {}).get("rewards") or []
        matches = [
            i for i, reward in enumerate(rewards)
            if str(reward.get("reward_type")) in wanted
        ]
        if idx1 >= len(matches):
            raise RuntimeError(f"Original reward {sorted(wanted)}[{idx1}] is absent: {rewards}")
        commands = [f"choose {matches[idx1]}"]
        if reward_type == 0:
            commands.append("skip" if idx2 == 5 else f"choose {idx2}")
        return commands
    if screen == "SHOP":
        reward_type = int(action.get("reward_type", 0))
        if reward_type == 6:
            return ["leave"]
        shop = game.get("screen_state") or {}
        gold = int(game.get("gold", 0))
        candidates: list[tuple[int, int]] = []
        if shop.get("purge_available") and int(shop.get("purge_cost", 10**9)) <= gold:
            candidates.append((5, 0))
        for kind, key in ((0, "cards"), (4, "relics"), (3, "potions")):
            for index, item in enumerate(shop.get(key) or []):
                if int(item.get("price", 10**9)) <= gold:
                    candidates.append((kind, index))
        # Lightspeed leaves an unavailable item in its stable shop slot, while
        # CommunicationMod compacts the corresponding list after a purchase.
        # Card actions must consequently be translated by card identity rather
        # than by the old physical slot number.
        if reward_type == 0:
            simulator_cards = (simulator.get("public_screen") or {}).get("cards") or []
            if idx1 >= len(simulator_cards):
                raise RuntimeError(f"Simulator shop card {idx1} is absent: {simulator_cards}")
            selected = simulator_cards[idx1]
            wanted = (
                normalize_card_id(selected.get("content_id")),
                int(selected.get("upgrades", 0)),
            )
            simulator_matches = [
                index for index, card in enumerate(simulator_cards)
                if (
                    normalize_card_id(card.get("content_id")),
                    int(card.get("upgrades", 0)),
                ) == wanted
            ]
            occurrence = simulator_matches.index(idx1)
            original_matches = [
                index for index, card in enumerate(shop.get("cards") or [])
                if (
                    normalize_card_id(card.get("id")),
                    int(card.get("upgrades", 0)),
                ) == wanted
                and int(card.get("price", 10**9)) <= gold
            ]
            if occurrence >= len(original_matches):
                raise RuntimeError(
                    f"Original shop has no card occurrence {occurrence} of {wanted}: "
                    f"{shop.get('cards')}"
                )
            target = (reward_type, original_matches[occurrence])
        else:
            target = (reward_type, idx1)
        try:
            choice = candidates.index(target)
        except ValueError as error:
            raise RuntimeError(f"Original shop action {target} is absent: {candidates}") from error
        return [f"choose {choice}"]
    if screen == "CARD_SELECT":
        simulator_cards = (simulator.get("public_screen") or {}).get("card_options") or []
        original_cards = (game.get("screen_state") or {}).get("cards") or []
        if idx1 >= len(simulator_cards):
            raise RuntimeError(f"Simulator card selection index {idx1} is absent: {simulator_cards}")
        selected = simulator_cards[idx1]
        wanted = (
            normalize_card_id(selected.get("content_id")),
            int(selected.get("upgrades", 0)),
        )
        simulator_matches = [
            index for index, card in enumerate(simulator_cards)
            if (
                normalize_card_id(card.get("content_id")),
                int(card.get("upgrades", 0)),
            ) == wanted
        ]
        occurrence = simulator_matches.index(idx1)
        original_matches = [
            index for index, card in enumerate(original_cards)
            if (
                normalize_card_id(card.get("id")),
                int(card.get("upgrades", 0)),
            ) == wanted
        ]
        if occurrence >= len(original_matches):
            raise RuntimeError(
                f"Original card selection has no occurrence {occurrence} of {wanted}: {original_cards}"
            )
        return [f"choose {original_matches[occurrence]}"]
    return [f"choose {idx1}"]
