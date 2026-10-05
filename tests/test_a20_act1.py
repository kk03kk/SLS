"""Ascension branches checked against stock classes, plus natural oracle fixtures."""

from dataclasses import replace

import pytest
import torch

from sls.backends.simulator import SimulatorBackend, native
from sls.content.scope import ironclad_scope, ironclad_scope_contract
from sls.contracts import ActionKind, ScreenType
from sls.curriculum import (
    CURRICULUM_PROFILES_BY_ID,
    IRONCLAD_A0_ACT1,
    IRONCLAD_A20_ACT1,
)
from sls.model import ENCODING_SCHEMA, PolicyBatch
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, load_checkpoint, save_checkpoint
from sls.rl.checkpoint import (
    CheckpointContractMismatch,
    policy_from_training_checkpoint,
)
from tests.simulator.test_fullrun_structure import _structural_action


@pytest.mark.parametrize("asc,hp,max_hp,bane,slots", [
    (0, 80, 80, 0, 3), (5, 80, 80, 0, 3), (6, 72, 80, 0, 3),
    (10, 72, 80, 1, 3), (11, 72, 80, 1, 2), (14, 68, 75, 1, 2), (20, 68, 75, 1, 2),
])
def test_stock_start_thresholds_and_v5_observation(asc, hp, max_hp, bane, slots):
    backend = SimulatorBackend(replace(IRONCLAD_A20_ACT1, ascension=asc))
    decision = backend.reset(0)
    observation = decision.observation
    assert (observation.player.current_hp, observation.player.max_hp) == (hp, max_hp)
    assert sum(c.card_id == "ASCENDERS_BANE" for c in observation.deck) == bane
    assert backend.raw_state["player_state"]["potion_capacity"] == slots
    assert observation.run.ascension == asc
    PolicyBatch.from_decisions((decision,))
    assert ENCODING_SCHEMA == "sls-policy-input-v5"
    restored = SimulatorBackend(backend.profile)
    assert restored.load_checkpoint(backend.checkpoint()) == decision


@pytest.mark.parametrize("seed", [0, 3, 4])
def test_a20_three_act1_bosses_stop_before_reward_decisions(seed):
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    decision = backend.reset(seed)
    backend._native._set_skip_battles_for_testing(True)
    for _ in range(180):
        transition = backend.step(_structural_action(decision))
        decision = transition.decision
        if decision.terminal:
            break
    assert decision.terminal and transition.info["success"]
    assert transition.info["reason"] == "ACT_1_CLEARED"
    assert decision.observation.run.act == 1 and not decision.actions


def _shop(ascension, seed, relics=()):
    backend = SimulatorBackend(replace(IRONCLAD_A20_ACT1, ascension=ascension))
    decision = backend.reset(seed)
    if relics:
        state = backend.checkpoint()
        ids = {r["enum_id"]: i for i, r in enumerate(native.relic_metadata_probe())}
        state["player_state"]["relics"].extend(
            {"id": ids[name], "data": 0} for name in relics
        )
        decision = backend.load_checkpoint(state)
    backend._native._set_skip_battles_for_testing(True)
    for _ in range(160):
        if decision.observation.screen is ScreenType.SHOP:
            return decision.observation, backend.raw_state["rng"]
        if decision.terminal:
            return None
        decision = backend.step(_structural_action(decision)).decision
    return None


def test_a16_stock_shop_markup_preserves_offers_rng_and_purge_price():
    pairs = []
    for seed in range(20):
        before = _shop(15, seed)
        if before:
            after = _shop(16, seed)
            assert after is not None
            pairs.append((before, after))
        if len(pairs) == 3:
            break
    assert len(pairs) == 3
    for (before, rng), (after, after_rng) in pairs:
        assert dict(before.choice_options[0].properties)["price"] == 75
        assert dict(after.choice_options[0].properties)["price"] == 75
        assert rng == after_rng
        assert len(before.shop_items) == len(after.shop_items)
        for a, b in zip(before.shop_items, after.shop_items):
            assert a.content_id == b.content_id
            # Stock ShopScreen.init: applyDiscount(1.1f, false).
            assert b.price == int(a.price * 1.1 + 0.5)


@pytest.mark.parametrize("relics,price", [
    (("SMILING_MASK",), 50), (("THE_COURIER",), 60),
    (("MEMBERSHIP_CARD",), 38), (("THE_COURIER", "MEMBERSHIP_CARD"), 38),
    (("SMILING_MASK", "THE_COURIER", "MEMBERSHIP_CARD"), 50),
])
def test_stock_removal_relic_prices_are_visible(relics, price):
    observation, _ = _shop(20, 0, relics)
    assert observation.choice_options[0].instance_id == "shop-remove"
    assert dict(observation.choice_options[0].properties)["price"] == price


def test_original_shop_removal_visibility():
    from sls.backends.original.adapter import _screen_entities

    state = {"purge_available": True, "purge_cost": 125}
    choices = _screen_entities({}, {}, {}, state, ScreenType.SHOP)["choice"]
    assert choices[0].instance_id == "shop-remove"
    assert dict(choices[0].properties) == {"price": 125}
    state["purge_available"] = False
    assert not _screen_entities({}, {}, {}, state, ScreenType.SHOP)["choice"]


def test_a20_transfer_preserves_weights_only_and_can_resume(tmp_path):
    from sls.model import ModelConfig, Policy
    from sls.rl.act1_transfer import initialize_a20_weights
    from sls.rl.training_contract import sha256_file

    model_config = ModelConfig(embedding_dim=32, transformer_layers=1,
                               attention_heads=4, feedforward_dim=64)
    ppo = PPOConfig(rollout_steps=2, recurrent_sequence_length=1, epochs=1)
    source = tmp_path / "parent" / "best.pt"
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        parent = PPOTrainer(Policy(model_config), workers, ppo, seed=8)
        parent.train_update()
        parent.environment_steps = 17006592
        save_checkpoint(source, parent)
    digest = sha256_file(source)
    config = {
        "run": {"output": "child"},
        "stages": {"train": {"target_environment_steps": 30000000}},
        "warm_start": {"checkpoint": "parent/best.pt", "checkpoint_sha256": digest,
                       "parent_environment_steps": 17006592},
    }
    with WorkerPool(IRONCLAD_A20_ACT1, 1) as workers:
        trainer = PPOTrainer(Policy(model_config), workers, ppo, seed=40000000)
        rng = torch.get_rng_state().clone()
        record = initialize_a20_weights(trainer, config, root=tmp_path)
        assert torch.equal(rng, torch.get_rng_state())
        assert not record["exact_resume_of_parent_experiment"]
        assert trainer.environment_steps == 17006592 and trainer.update == 0
        assert not trainer.optimizer.state
        for name, value in trainer.model.state_dict().items():
            assert torch.equal(value, parent.model.state_dict()[name])
        trainer.train_update()
        assert trainer.environment_steps == 17006594 and trainer.optimizer.state
        latest = save_checkpoint(tmp_path / "child" / "latest.pt", trainer)
        with pytest.raises(ValueError, match="fresh trainer"):
            initialize_a20_weights(trainer, config, root=tmp_path)
    with WorkerPool(IRONCLAD_A20_ACT1, 1) as workers:
        resumed = PPOTrainer(Policy(model_config), workers, ppo, seed=40000000)
        load_checkpoint(latest, resumed)
        resumed.train_update()
        assert resumed.environment_steps == 17006596
    assert sha256_file(source) == digest


def test_same_profile_transfer_is_an_explicit_optimization_experiment(tmp_path):
    from sls.model import ModelConfig, Policy
    from sls.rl.act1_transfer import initialize_act1_weights
    from sls.rl.training_contract import sha256_file

    model_config = ModelConfig(embedding_dim=32, transformer_layers=1,
                               attention_heads=4, feedforward_dim=64)
    ppo = PPOConfig(rollout_steps=1, recurrent_sequence_length=1, epochs=1)
    source = tmp_path / "parent" / "best.pt"
    with WorkerPool(IRONCLAD_A20_ACT1, 1) as workers:
        parent = PPOTrainer(Policy(model_config), workers, ppo, seed=8)
        parent.environment_steps = 46_006_272
        save_checkpoint(source, parent)
    config = {
        "run": {"output": "child"},
        "stages": {"train": {"target_environment_steps": 60_000_000}},
        "warm_start": {
            "checkpoint": "parent/best.pt",
            "checkpoint_sha256": sha256_file(source),
            "parent_environment_steps": 46_006_272,
        },
    }
    with WorkerPool(IRONCLAD_A20_ACT1, 1) as workers:
        trainer = PPOTrainer(Policy(model_config), workers, ppo, seed=50_000_000)
        with pytest.raises(ValueError, match="transfer_kind"):
            initialize_act1_weights(trainer, config, root=tmp_path)
        config["warm_start"]["transfer_kind"] = "optimization-experiment"
        record = initialize_act1_weights(trainer, config, root=tmp_path)
        assert record["source_profile"] == "IRONCLAD_A20_ACT1"
        assert record["schema"] == "sls-act1-weight-transfer-v2"
        assert trainer.environment_steps == 46_006_272
        assert trainer.update == 0 and not trainer.optimizer.state


def _event(event, ascension):
    backend = SimulatorBackend(replace(IRONCLAD_A20_ACT1, ascension=ascension))
    backend.reset(8)
    backend._native.reset_event_probe(8, event, backend.raw_state["rng"], ascension=ascension)
    return backend, backend._adapt(backend._native.snapshot())


@pytest.mark.parametrize("event,option,low,high,field", [
    ("GOLDEN_SHRINE", 0, 100, 50, "gold"),
    ("THE_SSSSSERPENT", 0, 175, 150, "gold"),
    ("THE_WOMAN_IN_BLUE", 3, 0, -4, "hp"),
])
def test_a15_event_effects_match_stock(event, option, low, high, field):
    for ascension, expected in [(14, low), (20, high)]:
        backend, decision = _event(event, ascension)
        action = next(a for a in decision.actions if a.option_id == f"event-option:{option}")
        after = backend.step(action).decision.observation
        delta = (after.run.gold - decision.observation.run.gold if field == "gold"
                 else after.player.current_hp - decision.observation.player.current_hp)
        assert delta == expected


def test_a15_random_event_public_costs_and_bane_not_removable():
    backend, decision = _event("SCRAP_OOZE", 20)
    assert dict(decision.observation.event_options[0].properties)["hp_loss"] == 5
    backend, decision = _event("THE_CLERIC", 20)
    action = next(a for a in decision.actions if a.option_id == "event-option:1")
    after = backend.step(action).decision
    assert all(c.content_id != "ASCENDERS_BANE" for c in after.observation.choice_options)
    assert not any(a.kind is ActionKind.PLAY_CARD for a in after.actions)


@pytest.mark.parametrize("encounter,hp", [("SLIME_BOSS", 150), ("HEXAGHOST", 264), ("THE_GUARDIAN", 250)])
def test_a9_boss_health_and_a19_powers(encounter, hp):
    battle = native.LightspeedBattle()
    battle.reset(0, encounter, 20)
    monster = battle.snapshot()["game_state"]["combat_state"]["monsters"][0]
    assert monster["max_hp"] == hp
    if encounter == "THE_GUARDIAN":
        assert any(p["amount"] == 40 for p in monster["powers"] if p["id"] == "Mode Shift")
    if encounter == "SLIME_BOSS":
        battle.step("end_turn")
        cards = battle.snapshot()["game_state"]["combat_state"]
        assert sum(c["id"] == "SLIMED" for zone in ("hand", "draw_pile", "discard_pile") for c in cards[zone]) == 5


def test_a20_scope_and_weight_transfer_are_not_exact_resume(tmp_path):
    from sls.model import ModelConfig, Policy

    assert CURRICULUM_PROFILES_BY_ID["IRONCLAD_A20_ACT1"] is IRONCLAD_A20_ACT1
    assert "ASCENDERS_BANE" in ironclad_scope(20)["cards"]["ids"]
    assert ironclad_scope_contract(0) != ironclad_scope_contract(20)
    config = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4, feedforward_dim=64)
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(Policy(config), workers, PPOConfig(rollout_steps=1, recurrent_sequence_length=1), seed=3)
        checkpoint = save_checkpoint(tmp_path / "a0.pt", trainer)
    payload = torch.load(checkpoint, weights_only=False)
    policy = policy_from_training_checkpoint(payload)
    with WorkerPool(IRONCLAD_A20_ACT1, 1) as workers:
        fresh = PPOTrainer(policy, workers, PPOConfig(rollout_steps=1, recurrent_sequence_length=1), seed=4)
        with pytest.raises(CheckpointContractMismatch):
            load_checkpoint(checkpoint, fresh)
        assert fresh.environment_steps == 0 and not fresh.optimizer.state
        a20 = save_checkpoint(tmp_path / "a20.pt", fresh)
        assert policy_from_training_checkpoint(torch.load(a20, weights_only=False)).config == policy.config


@pytest.mark.parametrize("ascension,amount", [(17, 2), (18, 3), (20, 3)])
def test_stock_a18_nob_enrage_and_sentry_dazed_threshold(ascension, amount):
    # Stock GremlinNob.takeTurn / Sentry constructor: >=18, not >18.
    battle = native.LightspeedBattle()
    battle.reset(0, "GREMLIN_NOB", ascension)
    battle.step("end_turn")
    monster = battle.snapshot()["game_state"]["combat_state"]["monsters"][0]
    assert next(p["amount"] for p in monster["powers"] if p["id"] == "Enrage") == amount
    if ascension >= 18:
        assert monster["move_id"] == "GREMLIN_NOB_SKULL_BASH"
        battle.step("end_turn")
        assert battle.snapshot()["game_state"]["combat_state"]["monsters"][0]["move_id"] == "GREMLIN_NOB_RUSH"
    battle.reset(0, "THREE_SENTRIES", ascension)
    battle.step("end_turn")
    combat = battle.snapshot()["game_state"]["combat_state"]
    assert sum(c["id"] == "DAZED" for zone in ("hand", "draw_pile", "discard_pile")
               for c in combat[zone]) == 2 * amount


@pytest.mark.parametrize("ascension,burns", [(18, 1), (19, 2), (20, 2)])
def test_stock_a19_hexaghost_sear_threshold(ascension, burns):
    # Stock Hexaghost constructor and takeTurn: first Sear creates these Burns.
    battle = native.LightspeedBattle()
    battle.reset(0, "HEXAGHOST", ascension)
    for _ in range(3):
        battle.step("end_turn")
    combat = battle.snapshot()["game_state"]["combat_state"]
    assert sum(c["id"] == "BURN" for zone in ("hand", "draw_pile", "discard_pile")
               for c in combat[zone]) == burns


@pytest.mark.parametrize("ascension,damage", [(14, 16), (15, 24), (20, 24)])
def test_stock_a15_shining_light_cost_and_two_upgrades(ascension, damage):
    backend, decision = _event("SHINING_LIGHT", ascension)
    action = next(a for a in decision.actions if a.option_id == "event-option:0")
    after = backend.step(action).decision.observation
    assert decision.observation.player.current_hp - after.player.current_hp == damage
    assert sum(c.upgrades for c in after.deck) - sum(c.upgrades for c in decision.observation.deck) == 2
