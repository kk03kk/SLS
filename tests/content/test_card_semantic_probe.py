from __future__ import annotations

import importlib.util
from pathlib import Path

import pytest

from sls.backends.original.adapter import adapt_original
from sls.content.scope import load_ironclad_a0_scope


ROOT = Path(__file__).resolve().parents[2]
native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


def _load_build_oracle():
    spec = importlib.util.spec_from_file_location("build_oracle", ROOT / "tools" / "build_oracle.py")
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _load_card_audit():
    spec = importlib.util.spec_from_file_location(
        "audit_card_semantics", ROOT / "tools" / "audit_card_semantics.py"
    )
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_oracle_card_allowlist_is_exactly_the_scoped_card_closure() -> None:
    module = _load_build_oracle()
    rows = module.scenario_card_allowlist().decode("utf-8").splitlines()
    assert [row.split("\t", 1)[0] for row in rows] == sorted(
        load_ironclad_a0_scope()["cards"]["ids"]
    )


@pytest.mark.parametrize("card_id,expected_relic", [
    ("ANGER", None),
    ("BURN", "MEDICAL_KIT"),
    ("REGRET", "BLUE_CANDLE"),
])
def test_native_card_probe_has_the_stable_audit_baseline(
    card_id: str, expected_relic: str | None,
) -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, card_id, False)
    payload = battle.snapshot()
    decision = adapt_original(payload).decision
    observation = decision.observation
    assert observation.player.current_hp == observation.player.max_hp == 80
    assert observation.player.energy == 4
    assert observation.player.block == 0
    assert [(enemy.monster_id, enemy.current_hp, enemy.intent) for enemy in observation.enemies] == [
        ("CULTIST", 999, "ATTACK")
    ]
    assert observation.hand[0].card_id == card_id
    relic_ids = {relic.content_id for relic in observation.relics}
    assert "BURNING_BLOOD" in relic_ids
    if expected_relic is not None:
        assert expected_relic in relic_ids
    assert any(
        action.kind.value == "PLAY_CARD" and action.subject_id == "HAND:0"
        for action in decision.actions
    )


def test_card_audit_reads_the_snapshot_after_a_native_step() -> None:
    module = _load_card_audit()
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "ANGER", False)
    adapted = adapt_original(battle.snapshot())
    action = next(
        item for item in adapted.decision.actions
        if item.kind.value == "PLAY_CARD" and item.subject_id == "HAND:0"
    )
    payload = module._execute_native(battle, adapted, action)
    assert isinstance(payload, dict)
    assert adapt_original(payload).decision.observation.hand[0].card_id == "STRIKE_RED"


def test_berserk_remains_a_visible_power_and_does_not_change_base_energy() -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "BERSERK", False)
    before = adapt_original(battle.snapshot()).decision
    action = next(
        item for item in before.actions
        if item.kind.value == "PLAY_CARD" and item.subject_id == "HAND:0"
    )
    battle.step("play", card_index=1, target_index=0)
    after = adapt_original(battle.snapshot()).decision.observation
    assert after.player.max_energy == 3
    assert [(power.content_id, dict(power.properties)["amount"]) for power in after.powers] == [
        ("BERSERK", 1), ("VULNERABLE", 2)
    ]


def test_card_probe_does_not_force_an_upgrade_onto_stock_unupgradable_cards() -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "CLUMSY", True)
    card = adapt_original(battle.snapshot()).decision.observation.hand[0]
    assert card.card_id == "CLUMSY"
    assert card.upgrades == 0


def test_burn_is_the_stock_upgradeable_status_exception() -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "BURN", True)
    card = adapt_original(battle.snapshot()).decision.observation.hand[0]
    assert card.card_id == "BURN"
    assert card.upgrades == 1


def test_card_audit_adapts_native_generated_card_choices() -> None:
    module = _load_card_audit()
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "DISCOVERY", False)
    battle.step("play", card_index=1, target_index=0)
    decision = module._adapt_probe_payload(battle.snapshot()).decision
    assert len(decision.actions) == 3
    assert len(decision.observation.choice_options) == 3
    assert {action.subject_id for action in decision.actions} == {
        "CHOICE:0", "CHOICE:1", "CHOICE:2"
    }


def test_card_probe_accepts_original_discovery_retrieval_timing() -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "DISCOVERY", True)
    battle.step("play", card_index=1, target_index=0)
    battle.set_discovery_retrieval_updates(14)
    battle.step("choose", choice_index=0)
    assert adapt_original(battle.snapshot()).decision.observation.screen.value == "COMBAT"


def test_fiend_fire_randomly_exhausts_even_the_last_hand_card() -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "FIEND_FIRE", False)
    before = battle.snapshot()["_rng"]["card_random"]["counter"]
    battle.step("play", card_index=1, target_index=0)
    after = battle.snapshot()["_rng"]["card_random"]["counter"]
    assert after - before == 1


def test_card_audit_preserves_hand_selection_source_and_confirm() -> None:
    module = _load_card_audit()
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "FORETHOUGHT", True)
    battle.step("play", card_index=1, target_index=0)
    decision = module._adapt_probe_payload(battle.snapshot()).decision
    assert dict(decision.observation.choice_options[0].properties)["source"] == "HAND"
    assert {action.kind.value for action in decision.actions} == {"SELECT_CARD", "CONFIRM"}
