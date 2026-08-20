"""Validation-only continuation evidence extraction."""

from __future__ import annotations

from typing import Any, Mapping


def continuation_original(payload: Mapping[str, Any]) -> dict[str, Any]:
    injected = payload.get("_continuation") or (payload.get("game_state") or {}).get("_continuation")
    if isinstance(injected, Mapping):
        result = dict(injected)
        game = payload.get("game_state") or {}
        screen_state = game.get("screen_state") or {}
        result["screen"] = result.get("screen") or game.get("screen_type")
        result["event_phase"] = result.get("event_phase") or screen_state.get("phase")
        result["action_phase"] = result.get("action_phase") or game.get("action_phase")
        if str(game.get("screen_type") or "").upper() == "GRID":
            task = result.get("card_selection_task")
            if not task:
                task = (
                    "TRANSFORM" if screen_state.get("for_transform") else
                    "UPGRADE" if screen_state.get("for_upgrade") else
                    "PURGE" if screen_state.get("for_purge") else "SELECT"
                )
            result["card_selection_source"] = result.get("card_selection_source") or "MASTER_DECK"
            result["card_selection_task"] = task
            result["card_selection_count"] = int(
                result.get("card_selection_count") or screen_state.get("num_cards") or 0
            )
        if game.get("combat_state"):
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
    return {
        "room_class": public.get("room_type"), "screen": public.get("screen_state"),
        "event_id": public.get("current_event_id"), "event_phase": screen.get("phase"),
        "action_phase": checkpoint.get("input_state"),
        "combat_turn": int(combat.get("turn", 0) or 0) if combat else None,
        "card_selection_source": (
            (combat.get("choice") or {}).get("source")
            or ("MASTER_DECK" if info.get("select_type") is not None else None)
        ),
        "card_selection_task": info.get("type") or screen.get("select_type"),
        "card_selection_count": int(
            info.get("select_count", info.get("count", screen.get("select_count", 0))) or 0
        ),
        "post_combat": bool(state.get("post_combat", False) or public.get("screen_state") == 2),
        "loading_post_combat": bool(state.get("loading_post_combat", False)),
        "ui_boundary_folded": bool(state.get("ui_boundary_folded", False)),
        "continuation_kind": info.get("kind") or public.get("screen_state"),
        "action_queue_types": list(checkpoint.get("action_queue_types") or ()),
        "card_queue_types": list(checkpoint.get("card_queue_types") or ()),
    }
