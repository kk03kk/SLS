from __future__ import annotations

import json

from sls.backends.original import OriginalBackend, OriginalSession
from sls.backends.original.adapter import adapt_original
from sls.contracts import ActionKind, ScreenType
from sls.contracts.continuation import continuation_original
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


def test_power_entities_have_backend_independent_semantic_order() -> None:
    from sls.backends.original.adapter import _powers

    powers = _powers([
        {"id": "Vulnerable", "amount": 2},
        {"id": "Split", "amount": -1},
    ], "MONSTER:0:POWER")
    assert [(item.content_id, dict(item.properties)["amount"]) for item in powers] == [
        ("SPLIT", 1), ("VULNERABLE", 2),
    ]
    reversed_powers = _powers([
        {"id": "Split", "amount": -1},
        {"id": "Vulnerable", "amount": 2},
    ], "MONSTER:0:POWER")
    assert reversed_powers == powers


def test_boss_chest_is_folded_into_the_boss_relic_decision() -> None:
    chest = game_payload([])
    chest["available_commands"] = ["choose", "proceed"]
    chest["game_state"].update({
        "screen_type": "CHEST",
        "room_class": "com.megacrit.cardcrawl.rooms.TreasureRoomBoss",
        "screen_state": {"chest_open": False, "chest_type": "BossChest"},
    })
    reward = game_payload(["Relic A", "Relic B", "Relic C"])
    reward["game_state"]["screen_type"] = "BOSS_REWARD"
    transport = ScriptedTransport([reward])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    backend.session.payload = chest
    folded = backend._fold_protocol_only_boundaries(chest, [])
    assert transport.sent == ["choose 0"]
    assert folded["game_state"]["screen_type"] == "BOSS_REWARD"


def test_open_boss_chest_is_folded_into_the_next_act() -> None:
    chest = game_payload([])
    chest["available_commands"] = ["proceed"]
    chest["game_state"].update({
        "screen_type": "CHEST",
        "room_class": "com.megacrit.cardcrawl.rooms.TreasureRoomBoss",
        "screen_state": {"chest_open": True, "chest_type": "BossChest"},
    })
    act_two = game_payload([])
    act_two["game_state"].update({"screen_type": "MAP", "act": 2, "floor": 17})
    transport = ScriptedTransport([act_two])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    backend.session.payload = chest
    folded = backend._fold_protocol_only_boundaries(chest, [])
    assert transport.sent == ["proceed"]
    assert folded["game_state"]["act"] == 2


def test_terminal_neow_leave_is_folded_into_map() -> None:
    leave = game_payload(["Leave"])
    leave["available_commands"] = ["choose"]
    leave["game_state"].update({
        "screen_type": "EVENT", "room_type": "NeowRoom", "floor": 0,
    })
    mapped = game_payload([])
    mapped["game_state"].update({"screen_type": "MAP", "floor": 0})
    transport = ScriptedTransport([mapped])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    backend.session.payload = leave
    folded = backend._fold_protocol_only_boundaries(leave, [], fold_single_event=False)
    assert transport.sent == ["choose 0"]
    assert folded["game_state"]["screen_type"] == "MAP"


def test_terminal_event_after_card_selection_is_folded_into_map() -> None:
    leave = game_payload(["离开"])
    leave["available_commands"] = ["choose"]
    leave["game_state"].update({
        "screen_type": "EVENT",
        "room_type": "EventRoom",
        "floor": 3,
        # The first ready payload after GridSelectScreen closes can expose the
        # localized choice before CommunicationMod fills screen_state.options.
        "screen_state": {"event_id": "Upgrade Shrine"},
    })
    mapped = game_payload([])
    mapped["game_state"].update({"screen_type": "MAP", "floor": 3})
    transport = ScriptedTransport([mapped])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    backend.session.payload = leave
    folded = backend._fold_terminal_selection_event(leave, [])
    assert transport.sent == ["choose 0"]
    assert folded["game_state"]["screen_type"] == "MAP"


def test_selection_completion_advances_transient_none_with_wait() -> None:
    transient = game_payload([])
    transient["available_commands"] = ["wait"]
    transient["game_state"].update({"screen_type": "NONE", "floor": 3})
    event = game_payload(["离开"])
    event["game_state"].update({"screen_type": "EVENT", "floor": 3})
    transport = ScriptedTransport([event])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    backend.session.payload = transient
    result = backend._wait_for_selection_completion(transient, [])
    assert transport.sent == ["wait 1"]
    assert result["game_state"]["screen_type"] == "EVENT"


def test_grid_cards_use_master_deck_uuid_indices_and_oracle_bottle_task() -> None:
    payload = game_payload([])
    payload["game_state"].update({
        "screen_type": "GRID",
        "deck": [
            {"id": "Strike_R", "uuid": "first", "upgrades": 0},
            {"id": "Bash", "uuid": "second", "upgrades": 0},
        ],
        "screen_state": {
            "num_cards": 1,
            "cards": [
                {"id": "Bash", "uuid": "second", "upgrades": 0},
                {"id": "Strike_R", "uuid": "first", "upgrades": 0},
            ],
        },
    })
    payload["_continuation"] = {
        "card_selection_source": "MASTER_DECK",
        "card_selection_task": "BOTTLE",
        "card_selection_count": 0,
    }
    decision = adapt_original(payload).decision
    assert [
        dict(entity.properties)["deck_index"]
        for entity in decision.observation.reward_options
    ] == [1, 0]
    continuation = continuation_original(payload)
    assert continuation["card_selection_task"] == "BOTTLE"
    assert continuation["card_selection_count"] == 1


def test_completed_rest_screen_is_folded_to_map() -> None:
    rest = game_payload([])
    rest["game_state"].update({
        "screen_type": "REST",
        "screen_state": {"has_rested": True, "rest_options": []},
    })
    rest["available_commands"] = ["proceed", "wait"]
    mapped = game_payload([])
    mapped["game_state"].update({
        "screen_type": "MAP",
        "screen_state": {"next_nodes": [{"x": 0, "y": 7}]},
    })
    mapped["available_commands"] = ["choose", "wait"]
    mapped["_continuation"] = {"ui_boundary_folded": False}
    transport = ScriptedTransport([mapped])
    session = OriginalSession(transport)
    session.payload = rest
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []
    result = backend._fold_protocol_only_boundaries(rest, executed)
    assert result["game_state"]["screen_type"] == "MAP"
    assert result["_continuation"]["ui_boundary_folded"] is True
    assert executed == ["proceed"]


def test_headbutt_grid_is_a_discard_continuation() -> None:
    payload = game_payload([])
    payload["game_state"].update({
        "screen_type": "GRID",
        "choice_list": ["Strike"],
        "combat_state": {"card_in_play": {"id": "Headbutt"}},
        "screen_state": {
            "cards": [{"id": "Strike_R", "upgrades": 0}],
            "num_cards": 1,
        },
    })
    payload["_continuation"] = {
        "action_queue_types": [
            "com.megacrit.cardcrawl.actions.utility.UseCardAction",
        ],
    }
    decision = adapt_original(payload).decision
    assert dict(decision.observation.choice_options[0].properties)["source"] == "DISCARD"
    continuation = continuation_original(payload)
    assert continuation["card_selection_source"] == "DISCARD"
    assert continuation["card_selection_task"] == "HEADBUTT"


def test_liquid_memories_grid_is_a_discard_continuation() -> None:
    payload = game_payload([])
    payload["game_state"].update({
        "screen_type": "GRID",
        "current_action": "BetterDiscardPileToHandAction",
        "choice_list": ["Strike"],
        "combat_state": {"card_in_play": None},
        "screen_state": {
            "cards": [{"id": "Strike_R", "upgrades": 0}],
            "num_cards": 1,
        },
    })
    payload["_continuation"] = {"action_queue_types": []}

    decision = adapt_original(payload).decision
    assert dict(decision.observation.choice_options[0].properties)["source"] == "DISCARD"
    continuation = continuation_original(payload)
    assert continuation["card_selection_source"] == "DISCARD"
    assert continuation["card_selection_task"] == "LIQUID_MEMORIES_POTION"


def test_lethal_combat_animation_is_settled_to_terminal_screen() -> None:
    dying = game_payload([])
    dying["game_state"].update({
        "screen_type": "NONE", "current_hp": 0,
        "combat_state": {
            "turn": 2,
            "monsters": [{"id": "Sentry", "current_hp": 38, "is_gone": False}],
        },
    })
    dying["available_commands"] = ["wait", "state"]
    dead = {**dying, "game_state": {**dying["game_state"]}}
    dead["_continuation"] = {"screen": "DEATH"}
    dead["available_commands"] = ["wait", "state", "reset_run"]
    transport = ScriptedTransport([dead])
    session = OriginalSession(transport)
    session.payload = dying
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []
    result = backend._settle_combat_terminal(dying, executed)
    terminal = adapt_original(result).decision
    assert terminal.terminal
    assert terminal.observation.enemies == ()
    assert executed == ["wait 30"]


def test_lethal_combat_uses_combat_player_hp_over_stale_run_hp() -> None:
    dying = game_payload([])
    dying["game_state"].update({
        "screen_type": "NONE", "current_hp": 7,
        "combat_state": {
            "player": {"current_hp": 0}, "turn": 5,
            "monsters": [{"id": "Sentry", "current_hp": 15, "is_gone": False}],
        },
    })
    dying["available_commands"] = ["wait", "state"]
    dead = {**dying, "game_state": {**dying["game_state"]}}
    dead["_continuation"] = {"screen": "DEATH"}
    transport = ScriptedTransport([dead])
    session = OriginalSession(transport)
    session.payload = dying
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []
    assert backend._settle_combat_terminal(dying, executed) is dead
    assert executed == ["wait 30"]


def test_post_card_boundary_waits_for_delayed_selection_ui() -> None:
    transient = game_payload([])
    transient["game_state"].update({
        "screen_type": "NONE",
        "combat_state": {"player": {"current_hp": 31}, "monsters": [{"current_hp": 20}]},
    })
    transient["available_commands"] = ["wait", "state"]
    selection = {**transient, "available_commands": ["choose", "wait", "state"]}
    selection["game_state"] = {
        **transient["game_state"], "screen_type": "GRID",
        "screen_state": {"cards": [{"id": "Strike_R"}], "num_cards": 1},
    }
    transport = ScriptedTransport([selection])
    session = OriginalSession(transport)
    session.payload = transient
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []
    assert backend._wait_for_actionable_combat_boundary(transient, executed) is selection
    assert executed == ["wait 1"]


def test_hand_select_uses_stable_hand_choice_indices() -> None:
    payload = game_payload([])
    payload["available_commands"] = ["choose", "wait", "state"]
    payload["game_state"].update({
        "screen_type": "HAND_SELECT",
        "combat_state": {
            "turn": 5,
            "player": {"current_hp": 31, "max_hp": 80, "energy": 2},
            "monsters": [{"id": "Sentry", "current_hp": 20}],
            "card_in_play": {"id": "Armaments"},
            "hand": [
                {"id": "Anger", "uuid": "anger", "upgrades": 0, "cost": 0},
                {"id": "Defend_R", "uuid": "defend", "upgrades": 0, "cost": 1},
            ],
        },
        "screen_state": {
            "max_cards": 1,
            "hand": [
                {"id": "Anger", "uuid": "anger", "upgrades": 0},
                {"id": "Defend_R", "uuid": "defend", "upgrades": 0},
            ],
        },
    })
    payload["_continuation"] = {
        "screen": "HAND_SELECT", "action_queue_types": [
            "com.megacrit.cardcrawl.actions.utility.UseCardAction",
        ],
    }
    decision = adapt_original(payload).decision
    assert decision.observation.screen is ScreenType.COMBAT
    assert [item.instance_id for item in decision.observation.choice_options] == [
        "CHOICE:0", "CHOICE:1",
    ]
    assert [item.subject_id for item in decision.actions] == ["CHOICE:0", "CHOICE:1"]
    adapted = adapt_original(payload)
    assert adapted.commands[decision.actions[0].candidate_id] == ("choose 0",)
    assert adapted.commands[decision.actions[1].candidate_id] == ("choose 1",)
    continuation = continuation_original(payload)
    assert continuation["card_selection_source"] == "HAND"
    assert continuation["card_selection_task"] == "ARMAMENTS"
    assert continuation["card_selection_count"] == 1


def test_completed_hand_selection_folds_protocol_only_confirm() -> None:
    selected = game_payload([])
    selected["available_commands"] = ["confirm", "wait", "state"]
    selected["game_state"].update({
        "screen_type": "HAND_SELECT", "room_phase": "COMBAT",
        "combat_state": {"player": {"current_hp": 20}, "monsters": [{"current_hp": 10}]},
        "screen_state": {
            "max_cards": 1, "can_pick_zero": False,
            "selected": [{"id": "Anger"}], "hand": [{"id": "Defend_R"}],
        },
    })
    done = {**selected, "available_commands": ["play", "end", "wait", "state"]}
    done["game_state"] = {
        **selected["game_state"], "screen_type": "NONE",
        "screen_state": {},
    }
    done["_continuation"] = {"action_phase": "WAITING_ON_USER"}
    transport = ScriptedTransport([done])
    session = OriginalSession(transport)
    session.payload = selected
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []
    assert backend._wait_for_selection_completion(selected, executed) is done
    assert executed == ["confirm"]


def test_continuation_prefers_public_event_id_over_java_class_name() -> None:
    payload = game_payload([])
    payload["game_state"].update({
        "screen_type": "EVENT",
        "screen_state": {"event_id": "World of Goop", "options": []},
    })
    payload["_continuation"] = {
        "event_id": "com.megacrit.cardcrawl.events.exordium.GoopPuddle",
        "screen": "EVENT",
    }
    assert continuation_original(payload)["event_id"] == "World of Goop"


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


def test_combat_boundary_waits_for_adjusted_attack_damage() -> None:
    combat = game_payload([])
    combat["available_commands"] = ["wait"]
    combat["game_state"]["combat_state"] = {"monsters": [{"id": "JawWorm"}]}
    combat["_monster_intents"] = [{"intent": "ATTACK", "damage": -1, "hits": 1}]
    settled = {**combat, "_monster_intents": [{"intent": "ATTACK", "damage": 11, "hits": 1}]}
    transport = ScriptedTransport([settled])
    session = OriginalSession(transport)
    session.payload = combat
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []
    result = backend._settle_debug_intents(combat, executed)
    assert result["_monster_intents"][0]["damage"] == 11
    assert executed == ["wait 1"]


def test_discovery_selection_allows_stock_retrieval_animation_past_30_frames() -> None:
    transient = game_payload(["Panache", "Mayhem", "Thinking Ahead"])
    transient["available_commands"] = ["wait"]
    transient["game_state"]["screen_type"] = "CARD_REWARD"
    transient["_continuation"] = {"card_selection_task": "DISCOVERY"}
    settled = game_payload([])
    settled["game_state"].update({"screen_type": "NONE", "room_phase": "COMBAT"})
    settled["_continuation"] = {"action_phase": "WAITING_ON_USER"}
    transport = ScriptedTransport([transient] * 30 + [settled])
    session = OriginalSession(transport)
    session.payload = transient
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []

    result = backend._wait_for_selection_completion(transient, executed)

    assert result["_continuation"]["action_phase"] == "WAITING_ON_USER"
    assert executed == ["wait 1"] * 31


def test_combat_card_choice_reads_screen_state_cards() -> None:
    payload = game_payload(["Panache", "Mayhem", "Thinking Ahead"])
    payload["game_state"].update({
        "floor": 6, "screen_type": "CARD_REWARD",
        "combat_state": {"hand": [], "monsters": []},
        "screen_state": {"cards": [
            {"id": "Panache"}, {"id": "Mayhem"}, {"id": "Thinking Ahead"},
        ]},
    })
    decision = adapt_original(payload).decision
    assert [action.kind for action in decision.actions] == [
        ActionKind.SELECT_CARD, ActionKind.SELECT_CARD, ActionKind.SELECT_CARD,
    ]
    assert [item.content_id for item in decision.observation.choice_options] == [
        "PANACHE", "MAYHEM", "THINKING_AHEAD",
    ]


def test_discovery_timing_is_validation_only_action_evidence() -> None:
    payload = game_payload(["Panache", "Mayhem", "Thinking Ahead"])
    payload["game_state"].update({
        "floor": 6, "screen_type": "CARD_REWARD",
        "combat_state": {"hand": [], "monsters": []},
        "screen_state": {"cards": [
            {"id": "Panache"}, {"id": "Mayhem"}, {"id": "Thinking Ahead"},
        ]},
    })
    payload["_timing_evidence"] = {
        "discovery_completion_serial": 0, "discovery_retrieval_updates": 0,
    }
    completed = game_payload([])
    completed["game_state"].update({
        "floor": 6, "screen_type": "COMBAT",
        "combat_state": {"hand": [{"id": "Panache"}], "monsters": []},
    })
    completed["available_commands"] = ["end"]
    completed["_timing_evidence"] = {
        "discovery_completion_serial": 1, "discovery_retrieval_updates": 15,
    }
    transport = ScriptedTransport([completed])
    session = OriginalSession(transport)
    session.payload = payload
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    backend._adapted = adapt_original(payload)
    action = backend._adapted.decision.actions[0]
    assert action.metadata == ()
    backend.step(action)
    assert backend.last_validation_evidence == {"discovery_retrieval_updates": 15}


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
    choosing = {**parent, "game_state": {**parent["game_state"]}}
    choosing["game_state"]["screen_state"] = {
        "rewards": [{"reward_type": "GOLD", "gold": 11}],
    }
    choosing["_combat_reward_cards"] = []
    completed = {**choosing, "game_state": {**choosing["game_state"]}}
    completed["game_state"]["deck"] = [{"id": "Anger", "upgrades": 0}]
    completed["game_state"]["screen_state"] = {
        "rewards": [{"reward_type": "GOLD", "gold": 11}],
    }
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


def test_emerald_key_reward_waits_for_stock_obtain_effect() -> None:
    reward = game_payload([])
    reward["game_state"].update({
        "floor": 6, "screen_type": "COMBAT_REWARD",
        "screen_state": {"rewards": [{"reward_type": "EMERALD_KEY"}]},
    })
    reward["available_commands"] = ["choose", "wait"]
    reward["_parity_run"] = {"emerald_key": False}
    pending = {**reward, "game_state": {**reward["game_state"]}}
    pending["game_state"]["screen_state"] = {"rewards": []}
    pending["available_commands"] = ["proceed", "wait"]
    settled = {**pending, "_parity_run": {"emerald_key": True}}
    transport = ScriptedTransport([pending, settled])
    session = OriginalSession(transport)
    session.payload = reward
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    backend._adapted = adapt_original(reward)
    action = next(
        item for item in backend._adapted.decision.actions
        if item.kind is ActionKind.TAKE_REWARD
    )
    transition = backend.step(action)
    assert transition.decision.observation.run.has_emerald_key
    assert backend.last_executed_commands == ("choose 0", "wait 1")


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


def test_map_action_folds_match_and_keep_intro_and_rules() -> None:
    map_payload = game_payload(["x=1"])
    map_payload["game_state"].update({
        "floor": 7, "screen_type": "MAP",
        "screen_state": {"next_nodes": [{"x": 1, "y": 6}]},
    })
    intro = game_payload(["Continue"])
    intro["game_state"].update({
        "floor": 7, "screen_type": "EVENT",
        "screen_state": {"event_id": "Match and Keep!"},
    })
    intro["_continuation"] = {"event_id": "Match and Keep!"}
    intro["_match_slots"] = []
    rules = game_payload(["Start"])
    rules["game_state"].update({
        "floor": 7, "screen_type": "EVENT",
        "screen_state": {"event_id": "Match and Keep!"},
    })
    rules["_continuation"] = {"event_id": "Match and Keep!"}
    rules["_match_slots"] = []
    play = game_payload([])
    play["game_state"].update({
        "floor": 7, "screen_type": "EVENT",
        "screen_state": {"event_id": "Match and Keep!"},
    })
    play["_continuation"] = {"event_id": "Match and Keep!"}
    play["available_commands"] = ["click", "wait"]
    play["_match_slots"] = [
        {
            "slot": index, "content_id": None, "known": False,
            "removed": False, "click_x": 640, "click_y": 330,
        }
        for index in range(12)
    ]
    transport = ScriptedTransport([intro, rules, play, play])
    session = OriginalSession(transport)
    session.payload = map_payload
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    backend._adapted = adapt_original(map_payload)

    transition = backend.step(backend._adapted.decision.actions[0])

    assert len(transition.decision.actions) == 66
    assert backend.last_executed_commands == (
        "choose 0", "choose 0", "choose 0", "wait 30",
    )


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


def test_resume_folds_shop_room_without_selecting_single_event() -> None:
    menu = {"in_game": False, "ready_for_command": True, "available_commands": ["parity_continue"]}
    shop_room = game_payload(["shop"])
    shop_room["game_state"].update({"floor": 3, "screen_type": "SHOP_ROOM"})
    shop = game_payload([])
    shop["game_state"].update({
        "floor": 3, "screen_type": "SHOP_SCREEN",
        "screen_state": {"cards": [], "relics": [], "potions": []},
    })
    shop["available_commands"] = ["leave", "wait"]
    transport = ScriptedTransport([menu, shop_room, shop])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    decision = backend.resume()
    assert decision.observation.screen is ScreenType.SHOP
    assert transport.sent == ["ready", "parity_continue", "choose 0"]


def test_leave_shop_uses_room_proceed_instead_of_reentering_shop() -> None:
    shop = game_payload([])
    shop["game_state"].update({
        "floor": 3, "screen_type": "SHOP_SCREEN",
        "screen_state": {"cards": [], "relics": [], "potions": []},
    })
    shop["available_commands"] = ["leave"]
    shop_room = game_payload(["shop"])
    shop_room["game_state"].update({"floor": 3, "screen_type": "SHOP_ROOM"})
    shop_room["available_commands"] = ["choose", "proceed"]
    map_payload = game_payload(["x=0"])
    map_payload["game_state"].update({
        "floor": 3, "screen_type": "MAP",
        "screen_state": {"next_nodes": [{"x": 0, "y": 3}]},
    })
    transport = ScriptedTransport([shop_room, map_payload])
    session = OriginalSession(transport)
    session.payload = shop
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    backend._adapted = adapt_original(shop)
    transition = backend.step(backend._adapted.decision.actions[0])
    assert transition.decision.observation.screen is ScreenType.MAP
    assert backend.last_executed_commands == ("leave", "proceed")


def test_command_boundary_requires_two_equal_nonadvancing_snapshots() -> None:
    transient = game_payload([])
    transient["available_commands"] = ["state", "wait"]
    transient["game_state"]["combat_state"] = {
        "hand": [{"id": "Wild Strike", "cost_for_turn": 1}],
    }
    transient["game_state"]["screen_type"] = "NONE"
    transient["_continuation"] = {
        "action_phase": "WAITING_ON_USER",
        "action_queue_types": [],
        "card_queue_types": [],
        "active_card_souls": [{"card_uuid": "card-1"}],
    }
    stable = json.loads(json.dumps(transient))
    stable["game_state"]["combat_state"]["hand"][0]["cost_for_turn"] = 0
    stable["_continuation"]["active_card_souls"] = []
    transport = ScriptedTransport([stable, stable, json.loads(json.dumps(stable))])
    session = OriginalSession(transport)
    session.payload = transient
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    executed: list[str] = []

    result = backend._settle_command_boundary(transient, executed)

    assert result["game_state"]["combat_state"]["hand"][0]["cost_for_turn"] == 0
    assert transport.sent == ["wait 30", "state", "state"]
    assert executed == ["wait 30", "state", "state"]
