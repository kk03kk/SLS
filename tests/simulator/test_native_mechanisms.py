from __future__ import annotations

import pytest


native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


def test_original_compatible_rng_is_seeded_and_advances_exactly() -> None:
    first = native.rng_probe(0)
    second = native.rng_probe(0)

    assert first == second
    assert first["initial"]["counter"] == 0
    assert first["final"]["counter"] == 9
    assert native.shuffle_probe(0) == [4, 8, 9, 6, 3, 5, 2, 1, 7, 0]


def test_core_combat_rule_probes() -> None:
    fairy = native.run_fairy_potion_probe()
    assert fairy == {"normal": 30, "sacred_bark": 60}

    stance = native.stance_mechanics_probe()
    assert stance["calm_exit"] == {"energy": 2, "stance": "WRATH"}
    assert stance["divinity_entry"]["stance"] == "DIVINITY"
    assert stance["divinity_entry"]["energy"] == 5

    orb = native.orb_mechanics_probe()
    assert orb["slot_cap"] == 10
    assert orb["plasma"]["energy_gained"] == 2
    assert orb["frost_evoke"]["block"] == 11

    damage = native.damage_pipeline_probe()
    assert damage["intangible_damage"] == 1
    assert damage["torii_tungsten_five"] == 0
    assert damage["buffer_multi_hit"] == {"buffer": 0, "damage": 7}


@pytest.mark.parametrize("power", ["DOUBLE_TAP", "DUPLICATION", "ECHO_FORM"])
@pytest.mark.parametrize("upgraded,expected_damage", [(False, 21), (True, 24)])
def test_duplicated_rampage_uses_the_mutated_damage(
    power: str, upgraded: bool, expected_damage: int,
) -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "RAMPAGE", upgraded)
    battle._set_duplication_power_for_testing(power)
    before = battle.snapshot()["game_state"]["combat_state"]["monsters"][0]["current_hp"]
    battle.step("play", card_index=1, target_index=0)
    after = battle.snapshot()["game_state"]["combat_state"]["monsters"][0]["current_hp"]
    assert before - after == expected_damage


@pytest.mark.parametrize("field", ["potion_ids", "potion_capacity", "orb_slots", "monsters"])
def test_combat_checkpoint_rejects_oversized_fixed_arrays(field: str) -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, "STRIKE_RED", False)
    checkpoint = battle.snapshot()
    combat = checkpoint["game_state"]["combat_state"]
    if field == "potion_ids":
        combat["_internal"][field].append(0)
    elif field == "potion_capacity":
        combat["_internal"][field] = 6
    elif field == "orb_slots":
        combat["player"]["_internal"][field] = 99
    else:
        combat[field].extend([combat[field][0]] * 7)

    restored = native.LightspeedBattle()
    with pytest.raises(ValueError, match="potion array|potion counts|orb slot|too many monsters"):
        restored.load_checkpoint(checkpoint)


def test_turn_lifecycle_and_stable_power_order() -> None:
    lifecycle = native.card_turn_lifecycle_probe()
    assert lifecycle["pride"]["copy_has_new_identity"] is True
    assert lifecycle["pride"]["rng_calls"] == 0
    assert lifecycle["no_trigger_cards"]["trigger_on_use"] is False

    powers = native.stable_power_order_probe()
    assert powers["initial"] == [
        "DEMON_FORM",
        "BRUTALITY",
        "DRAW_CARD_NEXT_TURN",
    ]
    assert powers["after_reapply"] == [
        "BRUTALITY",
        "DEMON_FORM",
        "DRAW_CARD_NEXT_TURN",
    ]
    assert powers["first_callback_is_demon_form"] is True


def test_status_and_curse_playability_rules() -> None:
    pride = native.LightspeedBattle()
    pride.reset(0, "CULTIST")
    pride.set_card_piles(["Pride"], [], [], [])
    assert any(
        action["kind"] == "play" for action in pride.snapshot()["_legal_actions"]
    )
    pride.step("play", card_index=1)
    assert pride.snapshot()["game_state"]["combat_state"]["exhaust_pile"][0][
        "id"
    ] == "PRIDE"

    injury = native.LightspeedBattle()
    injury.reset(0, "CULTIST")
    injury.set_card_piles(["Injury"], [], [], [])
    assert not any(
        action["kind"] == "play" for action in injury.snapshot()["_legal_actions"]
    )

    candle = native.LightspeedBattle()
    candle.reset(0, "CULTIST", relics=["Blue Candle"], replace_relics=True)
    candle.set_card_piles(["Injury"], [], [], [])
    assert any(
        action["kind"] == "play" for action in candle.snapshot()["_legal_actions"]
    )

    wound = native.LightspeedBattle()
    wound.reset(0, "CULTIST")
    wound.set_card_piles(["Wound"], [], [], [])
    assert not any(
        action["kind"] == "play" for action in wound.snapshot()["_legal_actions"]
    )

    medkit = native.LightspeedBattle()
    medkit.reset(0, "CULTIST", relics=["Medical Kit"], replace_relics=True)
    medkit.set_card_piles(["Wound"], [], [], [])
    assert any(
        action["kind"] == "play" for action in medkit.snapshot()["_legal_actions"]
    )


def test_map_graph_invariants_and_full_run_structure_without_combat() -> None:
    for seed in range(20):
        run = native.LightspeedRunState()
        run.reset(seed)
        initial = run.snapshot()
        nodes = initial["public_map"]
        node_ids = {node["node_id"] for node in nodes}
        assert nodes
        assert all(0 <= node["x"] < 7 and 0 <= node["y"] < 15 for node in nodes)
        assert all(
            target in node_ids or target.endswith(":15")
            for node in nodes
            for target in node["outgoing_node_ids"]
        )
        assert any(node["reachable"] for node in nodes)

        # This is a state-machine probe, not a combat or parity claim.  It lets
        # maps, rooms, events, rewards, shops, bosses and Act transitions run
        # without random combat deaths hiding later floors.
        run._set_skip_battles_for_testing(True)
        seen_acts = {initial["public_run"]["act"]}
        for decision_index in range(600):
            state = run.snapshot()
            seen_acts.add(state["public_run"]["act"])
            actions = state["legal_actions"]
            if not actions:
                break
            candidates = [
                action for action in actions
                # Skipping only the nested CardRewardScreen intentionally
                # returns to the unchanged parent RewardItem in stock. Avoid
                # repeatedly selecting that semantic no-op in this structural
                # traversal probe.
                if not (action.get("reward_type") == 0 and action.get("idx2") == 6)
            ] or actions
            action = candidates[(seed + decision_index * 7) % len(candidates)]
            run.step(action["bits"])
        else:
            pytest.fail(f"seed {seed} did not terminate structurally")
        assert max(seen_acts) >= 3
        final = run.snapshot()
        assert final["public_run"]["outcome"] != 0
        assert "combat_state" not in final


def test_checkpoint_restores_boss_reward_continuations() -> None:
    run = native.LightspeedRunState()
    run.reset(0)
    run._set_skip_battles_for_testing(True)
    reward_checkpoint = None
    for _ in range(400):
        state = run.snapshot()
        public = state["public_run"]
        if public["act"] == 1 and public["floor"] == 16 and public["screen_state"] == 2:
            reward_checkpoint = state
            break
        actions = state["legal_actions"]
        assert actions
        skip = next((item for item in actions if item.get("reward_type") == 6), None)
        run.step((skip or actions[0])["bits"])
    assert reward_checkpoint is not None

    restored = native.LightspeedRunState()
    restored.load_state(reward_checkpoint)
    reward_state = restored.snapshot()
    skip = next(item for item in reward_state["legal_actions"] if item.get("reward_type") == 6)
    boss_relic_state = restored.step(skip["bits"])
    assert boss_relic_state["public_run"]["screen_state"] == 3

    second = native.LightspeedRunState()
    second.load_state(boss_relic_state)
    choose = second.snapshot()["legal_actions"][0]
    act_two = second.step(choose["bits"])
    assert act_two["public_run"]["act"] == 2


def test_smoke_bomb_restrictions_and_curl_up_lethal_order() -> None:
    smoke = native.smoke_bomb_core_probe()
    assert smoke["normal_legal"] is True
    assert smoke["escaped"] is True
    assert smoke["back_attack_blocked"] is True
    assert all(smoke["bosses_blocked"].values())

    curl_up = native.curl_up_lethal_probe()
    assert curl_up["lethal"]["hp"] == 0
    assert curl_up["lethal"]["block"] == 0
    assert curl_up["nonlethal"] == {"block": 4, "curl_up": 0, "hp": 1}


def test_card_metadata_is_complete_enough_for_all_character_colors() -> None:
    metadata = native.card_metadata_probe()
    by_id = {card["enum_id"]: card for card in metadata}

    assert len(metadata) > 300
    assert len(by_id) == len(metadata)
    assert by_id["BASH"]["color"] == "RED"
    assert by_id["SURVIVOR"]["color"] == "GREEN"
    assert by_id["ZAP"]["color"] == "BLUE"
    assert by_id["VIGILANCE"]["color"] == "PURPLE"
    assert {
        card["enum_id"] for card in metadata if card["type"] == "STATUS"
    } == {"BURN", "DAZED", "SLIMED", "VOID", "WOUND"}
    assert {
        card["enum_id"] for card in metadata if card["type"] == "CURSE"
    } == {
        "ASCENDERS_BANE",
        "CLUMSY",
        "CURSE_OF_THE_BELL",
        "DECAY",
        "DOUBT",
        "INJURY",
        "NECRONOMICURSE",
        "NORMALITY",
        "PAIN",
        "PARASITE",
        "PRIDE",
        "REGRET",
        "SHAME",
        "WRITHE",
    }


def test_full_run_checkpoint_replays_the_same_transition() -> None:
    from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend

    first = SimulatorBackend(IRONCLAD_A0_ACT1)
    decision = first.reset(123456789)
    checkpoint = first.checkpoint()
    candidate_id = decision.actions[0].candidate_id
    expected = first.step(candidate_id)

    replay = SimulatorBackend(IRONCLAD_A0_ACT1)
    restored = replay.load_checkpoint(checkpoint)
    assert candidate_id in {action.candidate_id for action in restored.actions}
    actual = replay.step(candidate_id)

    assert actual == expected
    assert replay.raw_state == first.raw_state


def test_native_simple_agent_has_valid_boss_relic_fallback() -> None:
    from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend

    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    backend.reset(10041)
    result = backend._native.scripted_playout_act1()
    assert result["act_one_boss"] == "HEXAGHOST"
    assert result["scripted_action_count"] > 0


def test_full_run_checkpoint_is_exact_across_decision_boundaries() -> None:
    """Exercise run screens, combat choices, rewards, RNG and card identity."""

    from sls.backends.simulator import IRONCLAD_A0_HEART, SimulatorBackend
    from sls.validation.policies import PRIORITY

    for seed in (0, 1, 2, 9, 10, 11, 12):
        first = SimulatorBackend(IRONCLAD_A0_HEART)
        decision = first.reset(seed)
        for step_index in range(250):
            checkpoint = first.checkpoint()
            replay = SimulatorBackend(IRONCLAD_A0_HEART)
            restored = replay.load_checkpoint(checkpoint)
            assert restored == decision
            assert replay.raw_state == first.raw_state
            if decision.terminal:
                break

            ordered = sorted(
                decision.actions,
                key=lambda action: (
                    PRIORITY.get(action.kind, 999), action.candidate_id,
                ),
            )
            action = ordered[
                (seed + step_index // 17) % min(len(ordered), 3)
            ]
            expected = first.step(action)
            actual = replay.step(action)
            assert actual == expected
            assert replay.raw_state == first.raw_state
            decision = expected.decision
        else:
            pytest.fail(f"seed {seed} did not terminate within 250 decisions")
