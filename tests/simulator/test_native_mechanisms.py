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
