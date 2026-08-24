"""Validation-only continuation evidence extraction."""

from __future__ import annotations

from typing import Any, Mapping


def continuation_original(payload: Mapping[str, Any]) -> dict[str, Any]:
    injected = payload.get("_continuation") or (payload.get("game_state") or {}).get("_continuation")
    if isinstance(injected, Mapping):
        result = dict(injected)
        if isinstance(result.get("bottled_cards"), list):
            from sls.content.normalize import normalize_content_id
            result["bottled_cards"] = sorted([
                {
                    "type": str(item.get("type") or "").upper(),
                    "deck_index": int(item.get("deck_index", -1)),
                    "id": normalize_content_id(item.get("id")),
                    "upgrades": int(item.get("upgrades", 0) or 0),
                    "misc": int(item.get("misc", 0) or 0),
                }
                for item in result["bottled_cards"] if isinstance(item, Mapping)
            ], key=lambda item: {"ATTACK": 0, "SKILL": 1, "POWER": 2}.get(item["type"], 3))
        game = payload.get("game_state") or {}
        screen_state = game.get("screen_state") or {}
        result["screen"] = result.get("screen") or game.get("screen_type")
        result["event_phase"] = result.get("event_phase") or screen_state.get("phase")
        result["action_phase"] = result.get("action_phase") or game.get("action_phase")
        raw_screen = str(game.get("screen_type") or "").upper()
        if raw_screen == "EVENT" and screen_state.get("event_id"):
            # CommunicationMod's public event ID is the same save/game ID the
            # native run exposes.  Prefer it over the Oracle Java class name,
            # whose spelling is often unrelated (GoopPuddle/World of Goop).
            result["event_id"] = screen_state["event_id"]
        if raw_screen == "GRID":
            combat = game.get("combat_state") or {}
            card_in_play = combat.get("card_in_play") or {}
            from sls.content.normalize import normalize_card_id
            headbutt_selection = normalize_card_id(card_in_play.get("id")) == "HEADBUTT"
            liquid_memories_selection = str(
                game.get("current_action") or ""
            ).endswith("BetterDiscardPileToHandAction")
            task = result.get("card_selection_task")
            if not task:
                task = (
                    "HEADBUTT" if headbutt_selection else
                    "LIQUID_MEMORIES_POTION" if liquid_memories_selection else
                    "TRANSFORM" if screen_state.get("for_transform") else
                    "UPGRADE" if screen_state.get("for_upgrade") else
                    "PURGE" if screen_state.get("for_purge") else "SELECT"
                )
            result["card_selection_source"] = result.get("card_selection_source") or (
                "DISCARD" if headbutt_selection or liquid_memories_selection else "MASTER_DECK"
            )
            result["card_selection_task"] = task
            result["card_selection_count"] = int(
                result.get("card_selection_count") or screen_state.get("num_cards") or 0
            )
        elif raw_screen == "HAND_SELECT":
            combat = game.get("combat_state") or {}
            card_in_play = combat.get("card_in_play") or {}
            from sls.content.normalize import normalize_card_id
            result["card_selection_source"] = "HAND"
            task = normalize_card_id(card_in_play.get("id"))
            if bool(screen_state.get("can_pick_zero")) and int(
                screen_state.get("max_cards", 0) or 0
            ) > 1:
                task = "EXHAUST_MANY"
            result["card_selection_task"] = result.get("card_selection_task") or task
            result["card_selection_count"] = int(
                result.get("card_selection_count")
                or screen_state.get("max_cards") or 0
            )
        elif game.get("combat_state") and raw_screen == "CARD_REWARD":
            result["card_selection_source"] = result.get("card_selection_source") or "GENERATED"
            result["card_selection_task"] = result.get("card_selection_task") or "DISCOVERY"
            result["card_selection_count"] = int(result.get("card_selection_count") or 1)
        terminal_screen = str(result.get("screen") or "").upper() in {
            "DEATH", "VICTORY", "GAME_OVER", "COMPLETE",
        }
        if game.get("combat_state") and not terminal_screen:
            result["continuation_kind"] = "COMBAT"
        else:
            result["combat_turn"] = None
            if str(result.get("continuation_kind") or "").upper() in {"", "NONE"}:
                result["continuation_kind"] = str(game.get("screen_type") or "NONE").upper()
        return result
    game = payload.get("game_state") or {}
    combat = game.get("combat_state") or {}
    screen_state = game.get("screen_state") or {}
    selection = combat.get("card_select") or {}
    queue = combat.get("action_queue") or game.get("action_queue") or []
    card_queue = combat.get("card_queue") or game.get("card_queue") or []
    return {
        "room_class": game.get("room_class") or game.get("room_type"),
        "screen": game.get("screen_type"),
        "event_id": game.get("event_id") or game.get("current_event_id"),
        "event_phase": game.get("event_phase") or screen_state.get("phase"),
        "action_phase": combat.get("action_phase") or game.get("action_phase"),
        "combat_turn": int(combat.get("turn", 0) or 0) if combat else None,
        "card_selection_source": selection.get("source") or screen_state.get("source"),
        "card_selection_task": selection.get("type") or screen_state.get("type") or screen_state.get("select_type"),
        "card_selection_count": int(selection.get("num_cards", screen_state.get("num_cards", 0)) or 0),
        "post_combat": bool(game.get("post_combat", False)),
        "loading_post_combat": bool(game.get("loading_post_combat", False)),
        "ui_boundary_folded": False,
        "continuation_kind": "COMBAT" if combat else str(game.get("screen_type") or "NONE").upper(),
        "action_queue_types": [str(item.get("type") if isinstance(item, Mapping) else item) for item in queue],
        "card_queue_types": [str(item.get("type") if isinstance(item, Mapping) else item) for item in card_queue],
    }


def continuation_simulator(state: Mapping[str, Any]) -> dict[str, Any]:
    public = state.get("public_run") or {}
    combat = state.get("public_combat") or {}
    screen = state.get("public_screen") or {}
    info = state.get("screen_info") or {}
    checkpoint = state.get("combat_checkpoint") or {}
    choice = combat.get("choice") or {}
    inventory = state.get("public_inventory") or {}
    deck = inventory.get("deck") or ()
    player = state.get("player_state") or {}
    progress = state.get("progress_state") or {}
    internal_deck = player.get("deck") or ()
    bottle_types = ("ATTACK", "SKILL", "POWER")
    bottled_cards = []
    for bottle_type, raw_index in zip(bottle_types, player.get("bottle_indices") or ()):
        index = int(raw_index)
        if index < 0 or index >= len(deck):
            continue
        card = deck[index]
        bottled_cards.append({
            "type": bottle_type,
            "deck_index": index,
            "id": str(card.get("content_id") or "INVALID"),
            "upgrades": int(card.get("upgrades", 0) or 0),
            "misc": int(internal_deck[index].get("misc", 0) or 0)
            if index < len(internal_deck) else 0,
        })
    terminal_kind = None
    if int(public.get("outcome", 1)) != 1:
        terminal_kind = "DEATH" if int(player.get("current_hp", 0)) <= 0 else "VICTORY"
    neow_card_reward = bool(
        int(public.get("screen_state", 0) or 0) == 2
        and str(public.get("current_event_id") or "").upper() == "NEOW"
        and len(screen.get("card_rewards") or ()) == 1
        and not any(screen.get(key) for key in ("gold", "relics", "potions"))
    )
    action_queue_types = list(checkpoint.get("action_queue_types") or ())
    if choice.get("task") in {"HEADBUTT", "ARMAMENTS", "EXHAUST_MANY"} and not action_queue_types:
        # The native input state is suspended inside the played Headbutt.  The
        # Original exposes that suspension as its pending UseCardAction.
        action_queue_types = ["com.megacrit.cardcrawl.actions.utility.UseCardAction"]
    return {
        "room_class": public.get("room_type"), "screen": public.get("screen_state"),
        "event_id": public.get("current_event_id"), "event_phase": screen.get("phase"),
        "action_phase": checkpoint.get("input_state"),
        "combat_turn": int(combat.get("turn", 0) or 0) if combat else None,
        "card_selection_source": (
            choice.get("source")
            or ("MASTER_DECK" if info.get("select_type") is not None else None)
        ),
        "card_selection_task": choice.get("task") or info.get("type") or screen.get("select_type"),
        "card_selection_count": int(
            (
                (((state.get("combat_checkpoint") or {}).get("game_state") or {})
                 .get("combat_state") or {}).get("_internal", {}).get("choice", {})
                .get("pick_count", 1 if choice else 0)
            )
            or info.get("select_count", info.get("count", screen.get("select_count", 0))) or 0
        ),
        "post_combat": bool(
            state.get("post_combat", False)
            or (
                public.get("screen_state") == 2
                and int(progress.get("current_room", -1)) in {3, 4, 6}
            )
            or info.get("from_rewards", False)
        ),
        "loading_post_combat": bool(state.get("loading_post_combat", False)),
        "ui_boundary_folded": bool(state.get("ui_boundary_folded", False)),
        "continuation_kind": (
            terminal_kind or ("CARD_REWARD" if neow_card_reward else None)
            or info.get("kind") or public.get("screen_state")
        ),
        "action_queue_types": action_queue_types,
        "card_queue_types": list(checkpoint.get("card_queue_types") or ()),
        "bottled_cards": bottled_cards,
    }
