from __future__ import annotations

from sls.backends.original import OriginalBackend, OriginalSession
from sls.backends.original.adapter import adapt_original
from sls.contracts import ActionKind, ScreenType
from sls.curriculum import IRONCLAD_A0_ACT1


class ScriptedTransport:
    def __init__(self, payloads: list[dict]) -> None:
        self.payloads = iter(payloads)
        self.sent: list[str] = []

    def send(self, command: str) -> None:
        self.sent.append(command)

    def receive(self) -> dict:
        return next(self.payloads)


def game_payload(choices: list[str]) -> dict:
    return {
        "in_game": True,
        "ready_for_command": True,
        "available_commands": ["choose"],
        "game_state": {
            "class": "IRONCLAD",
            "ascension_level": 0,
            "act": 1,
            "floor": 0,
            "gold": 99,
            "current_hp": 80,
            "max_hp": 80,
            "deck": [],
            "relics": [],
            "potions": [],
            "map": [],
            "screen_type": "EVENT",
            "screen_state": {},
            "choice_list": choices,
            "_parity_run": {},
        },
    }


def test_reset_folds_the_original_only_neow_dialog() -> None:
    menu = {
        "in_game": False,
        "ready_for_command": True,
        "available_commands": ["start"],
    }
    transport = ScriptedTransport([menu, game_payload(["Continue"]), game_payload(["A", "B", "C", "D"])])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    decision = backend.reset(0)
    assert transport.sent[0] == "ready"
    assert transport.sent[1].startswith("start IRONCLAD 0 ")
    assert transport.sent[2] == "choose 0"
    assert decision.observation.screen is ScreenType.NEOW
    assert len(decision.actions) == 4
    assert all(action.kind is ActionKind.CHOOSE_NEOW_OPTION for action in decision.actions)


def test_step_folds_grid_confirmation_and_single_neow_leave_boundary() -> None:
    menu = {"in_game": False, "ready_for_command": True, "available_commands": ["start"]}
    grid = game_payload([])
    grid["available_commands"] = ["confirm"]
    grid["game_state"]["screen_type"] = "GRID"
    leave = game_payload(["Leave"])
    map_payload = game_payload([])
    map_payload["game_state"]["screen_type"] = "MAP"
    map_payload["game_state"]["screen_state"] = {
        "next_nodes": [{"x": 1, "y": 0}],
    }
    map_payload["available_commands"] = ["choose", "wait", "reset_run"]
    stable_map_payload = {**map_payload, "game_state": {**map_payload["game_state"]}}
    restored_menu = {"in_game": False, "ready_for_command": True, "available_commands": ["start"]}
    transport = ScriptedTransport([
        menu, game_payload(["Continue"]), game_payload(["A", "B", "C", "D"]),
        grid, leave, map_payload, stable_map_payload, restored_menu,
    ])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    decision = backend.reset(0)
    transition = backend.step(decision.actions[0])
    assert transition.decision.observation.screen is ScreenType.MAP
    assert backend.last_executed_commands == ("choose 0", "confirm", "choose 0", "wait 30")
    backend.return_to_menu()
    assert transport.sent[-1] == "reset_run"


def test_combat_boundary_waits_until_stock_intent_is_materialized() -> None:
    combat = game_payload([])
    combat["available_commands"] = ["wait"]
    combat["game_state"]["combat_state"] = {"monsters": [{"id": "Cultist"}]}
    combat["_monster_intents"] = [{"intent": "DEBUG"}]
    settled = {**combat, "_monster_intents": [{"intent": "BUFF"}]}
    transport = ScriptedTransport([settled])
    session = OriginalSession(transport)
    session.payload = combat
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []
    result = backend._settle_debug_intents(combat, executed)
    assert result["_monster_intents"] == [{"intent": "BUFF"}]
    assert executed == ["wait 1"]
    assert transport.sent == ["wait 1"]


def test_composite_card_reward_waits_for_each_ui_transition() -> None:
    parent = game_payload([])
    parent["game_state"].update({
        "floor": 1, "screen_type": "COMBAT_REWARD", "deck": [],
        "screen_state": {"rewards": [
            {"reward_type": "GOLD", "gold": 11}, {"reward_type": "CARD"},
        ]},
    })
    parent["available_commands"] = ["choose", "proceed", "wait"]
    parent["_combat_reward_cards"] = [[{"id": "Anger", "upgrades": 0}]]
    opening = {**parent, "game_state": {**parent["game_state"]}}
    card_screen = game_payload(["anger"])
    card_screen["game_state"].update({
        "floor": 1, "screen_type": "CARD_REWARD",
        "screen_state": {"cards": [{"id": "Anger", "upgrades": 0}]},
    })
    card_screen["available_commands"] = ["choose", "skip", "wait"]
    choosing = {**card_screen, "game_state": {**card_screen["game_state"]}}
    completed = {**parent, "game_state": {**parent["game_state"]}}
    completed["game_state"]["deck"] = [{"id": "Anger", "upgrades": 0}]
    completed["game_state"]["screen_state"] = {
        "rewards": [{"reward_type": "GOLD", "gold": 11}],
    }
    completed["_combat_reward_cards"] = []
    transport = ScriptedTransport([opening, card_screen, choosing, completed])
    session = OriginalSession(transport)
    session.payload = parent
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    backend._adapted = adapt_original(parent)
    action = next(
        item for item in backend._adapted.decision.actions
        if item.kind is ActionKind.CHOOSE_CARD_REWARD
    )
    transition = backend.step(action)
    assert [card.card_id for card in transition.decision.observation.deck] == ["ANGER"]
    assert backend.last_executed_commands == ("choose 1", "wait 1", "choose 0", "wait 1")


def test_event_action_folds_single_forced_leave_outside_neow() -> None:
    # Regression: 20260820T170023.957843Z-seed-0 step 1 (Big Fish).
    event = game_payload(["Banana", "Donut", "Box"])
    event["game_state"].update({"floor": 2, "screen_state": {"event_id": "Big Fish"}})
    leave = game_payload(["Leave"])
    leave["game_state"].update({"floor": 2, "screen_state": {"event_id": "Big Fish"}})
    map_payload = game_payload([])
    map_payload["game_state"].update({
        "floor": 2, "screen_type": "MAP",
        "screen_state": {"next_nodes": [{"x": 1, "y": 2}]},
    })
    transport = ScriptedTransport([leave, map_payload])
    session = OriginalSession(transport)
    session.payload = event
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    backend._adapted = adapt_original(event)
    action = next(
        item for item in backend._adapted.decision.actions
        if item.kind is ActionKind.CHOOSE_EVENT_OPTION
    )
    transition = backend.step(action)
    assert transition.decision.observation.screen is ScreenType.MAP
    assert backend.last_executed_commands == ("choose 0", "choose 0")


def test_map_action_folds_shop_room_entry_wrapper() -> None:
    # Regression: 20260820T170551.039303Z-seed-0.partial after boundary 1.
    map_payload = game_payload(["x=1"])
    map_payload["game_state"].update({
        "floor": 2, "screen_type": "MAP",
        "screen_state": {"next_nodes": [{"x": 1, "y": 2}]},
    })
    shop_room = game_payload(["shop"])
    shop_room["game_state"].update({"floor": 3, "screen_type": "SHOP_ROOM"})
    shop = game_payload([])
    shop["game_state"].update({
        "floor": 3, "screen_type": "SHOP_SCREEN",
        "screen_state": {"cards": [], "relics": [], "potions": []},
    })
    shop["available_commands"] = ["leave", "wait"]
    transport = ScriptedTransport([shop_room, shop])
    session = OriginalSession(transport)
    session.payload = map_payload
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    backend._adapted = adapt_original(map_payload)
    transition = backend.step(backend._adapted.decision.actions[0])
    assert transition.decision.observation.screen is ScreenType.SHOP
    assert backend.last_executed_commands == ("choose 0", "choose 0")
    assert transition.decision.actions[0].kind is ActionKind.LEAVE_SHOP
