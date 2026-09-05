"""Exercise FullRun policy choices, not just the standalone battle wrapper."""

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.contracts import ActionKind
from sls.model import encode_decision

native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


def prepared_combat(hand, discard=(), relics=(), potions=()):
    backend = SimulatorBackend()
    decision = backend.reset(0)
    for _ in range(30):
        if decision.observation.screen.value == "COMBAT":
            break
        decision = backend.step(decision.actions[0]).decision
    assert decision.observation.screen.value == "COMBAT"
    checkpoint = backend.checkpoint()
    battle = native.LightspeedBattle()
    battle.reset(0, "CULTIST", relics=list(relics), replace_relics=True)
    battle.set_card_piles(list(hand), [], list(discard), [])
    battle.set_potions(list(potions))
    combat = battle.snapshot()
    combat["rng"] = combat.pop("_rng")
    # Inject only a settled combat. Pending lambdas are not reconstructed by
    # direct state restore; normal production choice checkpoints use replay.
    checkpoint["combat_checkpoint"] = combat
    checkpoint["replay_required"] = False
    checkpoint["replay_actions"] = []
    return backend, backend.load_checkpoint(checkpoint)


@pytest.mark.parametrize("selected", [(), (0,), (0, 1), (1, 0)])
def test_fullrun_forethought_plus_can_select_any_subset(selected):
    backend, decision = prepared_combat(["Forethought+", "Bash", "Strike_R"])
    decision = backend.step(next(
        action for action in decision.actions if action.subject_id == "HAND:0"
    )).decision
    assert [action.kind for action in decision.actions] == [
        ActionKind.SELECT_CARD, ActionKind.SELECT_CARD, ActionKind.CONFIRM,
    ]
    for index in selected:
        decision = backend.step(next(
            action for action in decision.actions if action.subject_id == f"CHOICE:{index}"
        )).decision
        encode_decision(decision)
    selected_cards = decision.observation.selected_cards
    assert [c.content_id for c in selected_cards] == [["BASH", "STRIKE_RED"][i] for i in selected]
    assert [dict(c.properties)["selected_order"] for c in selected_cards] == list(range(len(selected)))
    assert all(dict(c.properties)["selected"] and dict(c.properties)["source"] == "HAND"
               for c in selected_cards)
    decision = backend.step(next(
        action for action in decision.actions if action.kind is ActionKind.CONFIRM
    )).decision
    assert len(decision.observation.draw_pile) == len(selected)
    assert not decision.observation.selected_cards
    assert len(decision.observation.hand) == 2 - len(selected)
    assert all(dict(card.properties)["free_to_play_once"] for card in decision.observation.draw_pile)
    assert [card["content_id"] for card in reversed(
        backend.raw_state["public_combat"]["draw_pile"]
    )] == [["BASH", "STRIKE_RED"][i] for i in selected]


@pytest.mark.parametrize("indices", [(0, 2), (10, 11)])
def test_fullrun_sacred_bark_liquid_memories_retrieves_two_chosen_cards(indices):
    backend, decision = prepared_combat(
        ["Defend_R"], ["Bash"] * 12, ["Sacred Bark"], ["Liquid Memories"],
    )
    decision = backend.step(next(
        action for action in decision.actions if action.kind is ActionKind.USE_POTION
    )).decision
    assert len([a for a in decision.actions if a.kind is ActionKind.SELECT_CARD]) == 12
    assert not any(a.kind is ActionKind.CONFIRM for a in decision.actions)
    for index in indices:
        decision = backend.step(next(
            action for action in decision.actions if action.subject_id == f"CHOICE:{index}"
        )).decision
        encode_decision(decision)
    assert [a.kind for a in decision.actions] == [ActionKind.CONFIRM]
    decision = backend.step(decision.actions[0]).decision
    assert len(decision.observation.discard_pile) == 10
    retrieved = [card for card in decision.observation.hand if card.card_id == "BASH"]
    assert len(retrieved) == 2
    assert all(card.current_cost == 0 for card in retrieved)


def test_liquid_memories_respects_click_order_and_does_not_move_overflow_cards():
    backend, decision = prepared_combat(
        ["Defend_R"] * 9, ["Bash", "Strike_R", "Anger"],
        ["Sacred Bark"], ["Liquid Memories"],
    )
    decision = backend.step(next(
        a for a in decision.actions if a.kind is ActionKind.USE_POTION
    )).decision
    for index in (1, 0):
        decision = backend.step(next(
            a for a in decision.actions if a.subject_id == f"CHOICE:{index}"
        )).decision
    decision = backend.step(decision.actions[0]).decision
    assert decision.observation.hand[-1].card_id == "STRIKE_RED"
    assert decision.observation.hand[-1].current_cost == 0
    assert [c["content_id"] for c in backend.raw_state["public_combat"]["discard_pile"]] == [
        "BASH", "ANGER",
    ]


@pytest.mark.parametrize("potion_action", [ActionKind.USE_POTION, ActionKind.DISCARD_POTION])
def test_potion_during_fullrun_multi_selection_preserves_click_order(potion_action):
    backend, decision = prepared_combat(
        ["Forethought+", "Bash", "Strike_R"], potions=["Block Potion"],
    )
    decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.PLAY_CARD
                                 and a.subject_id == "HAND:0")).decision
    decision = backend.step(next(a for a in decision.actions if a.subject_id == "CHOICE:1")).decision
    assert backend.checkpoint()["_policy_multi_selection"] == [1]
    decision = backend.step(next(a for a in decision.actions if a.kind is potion_action)).decision
    assert backend.checkpoint()["_policy_multi_selection"] == [1]
    assert not decision.observation.potions
    assert decision.observation.player.block == 0  # Effect waits for the open selector.
    assert not any(a.subject_id == "CHOICE:1" for a in decision.actions)
    decision = backend.step(next(a for a in decision.actions if a.subject_id == "CHOICE:0")).decision
    decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.CONFIRM)).decision
    assert decision.observation.player.block == (12 if potion_action is ActionKind.USE_POTION else 0)
    assert [c["content_id"] for c in reversed(backend.raw_state["public_combat"]["draw_pile"])] == [
        "STRIKE_RED", "BASH",
    ]
    assert "_policy_multi_selection" not in backend.checkpoint()


def test_queued_elixir_opens_a_fresh_selection_after_forethought():
    backend, decision = prepared_combat(
        ["Forethought+", "Bash", "Strike_R", "Defend_R"], potions=["ELIXIR_POTION"],
    )
    decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.PLAY_CARD
                                 and a.subject_id == "HAND:0")).decision
    decision = backend.step(next(a for a in decision.actions if a.subject_id == "CHOICE:0")).decision
    decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.USE_POTION)).decision
    assert backend.raw_state["public_combat"]["choice"]["task"] == "FORETHOUGHT"
    assert backend.checkpoint()["_policy_multi_selection"] == [0]
    decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.CONFIRM)).decision
    assert backend.raw_state["public_combat"]["choice"]["task"] == "EXHAUST_MANY"
    assert "_policy_multi_selection" not in backend.checkpoint()
    assert [c.card_id for c in decision.observation.draw_pile] == ["BASH"]
    assert {a.subject_id for a in decision.actions if a.kind is ActionKind.SELECT_CARD} == {
        "CHOICE:0", "CHOICE:1",
    }
    # The old Forethought selection must not leak into the new Elixir choice.
    decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.CONFIRM)).decision
    assert not decision.observation.exhaust_pile
    assert {c.card_id for c in decision.observation.hand} == {"STRIKE_RED", "DEFEND_RED"}


@pytest.mark.parametrize("indices", [list(reversed(range(10))), [3, 0, 9, 2, 8, 1, 7, 4, 6, 5]])
def test_fullrun_elixir_exhausts_ten_cards_in_click_order(indices):
    hand = ["Strike_R", "Defend_R", "Bash", "Anger", "Cleave", "Shrug It Off",
            "Iron Wave", "Pommel Strike", "Twin Strike", "Flex"]
    backend, decision = prepared_combat(hand, potions=["ELIXIR_POTION"])
    expected = [decision.observation.hand[index].card_id for index in indices]
    decision = backend.step(next(
        a for a in decision.actions if a.kind is ActionKind.USE_POTION
    )).decision
    for index in indices:
        decision = backend.step(next(
            a for a in decision.actions if a.subject_id == f"CHOICE:{index}"
        )).decision
    decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.CONFIRM)).decision
    assert not decision.observation.hand
    assert [c["content_id"] for c in backend.raw_state["public_combat"]["exhaust_pile"]] == expected
