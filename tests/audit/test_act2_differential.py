import copy
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import (
    comparison_projection,
    direct_projection,
    replay_controlled_run,
)
from sls.audit.card_parity import structured_differences
from sls.backends.simulator import native


def payload():
    battle = native.LightspeedBattle()
    battle.reset(131100000, "SNAKE_PLANT", 20)
    return battle.snapshot()


@pytest.mark.parametrize("mutation", ["damage", "mask", "duration", "rng", "cost"])
def test_differencer_detects_material_mutations(mutation):
    before = payload()
    changed = copy.deepcopy(before)
    if mutation == "damage":
        changed["game_state"]["combat_state"]["player"]["current_hp"] -= 1
    elif mutation == "mask":
        changed["_legal_actions"].pop()
    elif mutation == "duration":
        changed["game_state"]["combat_state"]["monsters"][0]["powers"][0]["amount"] += 1
    elif mutation == "rng":
        changed["_rng"]["ai"]["counter"] += 1
    else:
        changed["game_state"]["combat_state"]["hand"][0]["cost"] += 1
    assert structured_differences(comparison_projection(before, stock=False),
                                  comparison_projection(changed, stock=False))


def test_rejects_wrong_ascension_and_unstable_boundary():
    before = payload()
    before["game_state"]["ascension_level"] = 0
    with pytest.raises(ValueError, match="A20"):
        comparison_projection(before, stock=False)
    before["game_state"]["ascension_level"] = 20
    before["game_state"]["input_state"] = "INTERNAL"
    with pytest.raises(ValueError, match="stable"):
        comparison_projection(before, stock=False)


def test_loss_boundary_keeps_direct_state_and_rejects_live_actions():
    battle = native.LightspeedBattle()
    battle.reset(131200120, 'THE_HEART', 20)
    battle.set_player_health(1, 80)
    battle.set_card_piles(['Anger'], ['Anger'] * 30, [], [])
    battle.step('play', card_index=1, target_index=0)
    raw = battle.snapshot()
    assert raw['game_state']['outcome'] == 'PLAYER_LOSS'
    result = comparison_projection(raw, stock=False)
    assert result['adapted']['terminal']
    assert result['direct']['player']['current_hp'] == 0
    assert result['actual_actions'] == []
    changed = copy.deepcopy(raw)
    changed['_legal_actions'] = [{'kind': 'end_turn'}]
    with pytest.raises(ValueError, match='no legal actions'):
        comparison_projection(changed, stock=False)
    changed = copy.deepcopy(raw)
    changed['game_state']['combat_state']['player']['current_hp'] = 1
    changed['game_state']['input_state'] = 'INTERNAL'
    with pytest.raises(ValueError, match='stable'):
        comparison_projection(changed, stock=False)


@pytest.mark.parametrize('field', ['cost', 'base_cost', 'retain', 'upgrades', 'pile_order',
                                  'potion_target', 'potion_capacity', 'occupied_slot', 'relic_counter'])
def test_independent_v2_projection_detects_changes_even_if_adapter_payload_is_unchanged(field):
    original = payload()
    direct = copy.deepcopy(original['game_state']['combat_state'])
    direct.update(schema='sls-stock-direct-v2', energy=direct['player']['energy'],
                  relics=copy.deepcopy(original['game_state']['relics']),
                  potions=copy.deepcopy(original['game_state']['potions']))
    original['_stock_direct'] = direct
    if field == 'occupied_slot':
        direct['potions'][0].update(id='Fire Potion', slot=0, requires_target=True)
    changed = copy.deepcopy(original)
    raw = changed['_stock_direct']
    if field == 'pile_order':
        other = next(i for i, card in enumerate(raw['hand']) if card['id'] != raw['hand'][0]['id'])
        raw['hand'][0], raw['hand'][other] = raw['hand'][other], raw['hand'][0]
    elif field == 'potion_target':
        raw['potions'][0]['requires_target'] = not raw['potions'][0]['requires_target']
    elif field == 'potion_capacity':
        raw['potions'].pop()
    elif field == 'occupied_slot':
        raw['potions'][0]['slot'] = 1
    elif field == 'relic_counter':
        raw['relics'][0]['counter'] = 2
    elif field == 'retain':
        raw['hand'][0][field] = not raw['hand'][0][field]
    else:
        raw['hand'][0][field] += 1
    assert original['game_state'] == changed['game_state']
    assert structured_differences(direct_projection(original, stock=True),
                                  direct_projection(changed, stock=True))


def test_rejects_missing_initial_state_action_and_stale_native(monkeypatch):
    with pytest.raises(ValueError, match="initial state"):
        replay_controlled_run({})
    with pytest.raises(ValueError, match="missing action"):
        replay_controlled_run({"before": {"_rng": payload()["_rng"]},
                               "boundaries": [], "actions": [{"kind": "end_turn"}]})
    monkeypatch.setattr(native, "NATIVE_SOURCE_SHA256", "stale")
    with pytest.raises(ValueError, match="stale native"):
        replay_controlled_run({})


def test_manifest_has_fixed_disjoint_seeds_and_stock_sources():
    manifest = json.loads(Path("native/oracle/resources/spirecomm/parity/act2-scenes.json").read_text())
    assert len(manifest["scenes"]) == 24
    assert [seed for row in manifest["scenes"] for seed in row["seeds"]] == list(range(131100000, 131100072))
    assert len({row["id"] for row in manifest["scenes"]}) == 24
    assert all("com.megacrit.cardcrawl." + cls in manifest["stock_sources"]
               for row in manifest["scenes"] for cls in row["stock_classes"])


def test_explicit_probe_context_and_legacy_default():
    before = payload()
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(0, "SNAKE_PLANT", before["_rng"])
    assert battle.snapshot()["game_state"]["ascension_level"] == 0
    battle.reset_encounter_probe(0, "SNAKE_PLANT", before["_rng"], 20, 2, 20, "plant-single")
    game = battle.snapshot()["game_state"]
    assert (game["ascension_level"], game["act"], game["floor"]) == (20, 2, 20)
    with pytest.raises(ValueError, match="context"):
        battle.reset_encounter_probe(0, "SNAKE_PLANT", before["_rng"], 21)


@pytest.mark.parametrize('act,floor,room', [(3, 40, 'MONSTER'), (4, 54, 'ELITE'),
                                          (4, 55, 'BOSS')])
def test_late_context_refuses_an_act_number_without_the_actual_stock_dungeon(act, floor, room):
    raw = payload()
    raw['_oracle_mode'] = 'validation'
    raw['_stock_direct'] = {'ascension': 20, 'act': act, 'floor': floor,
                            'dungeon_id': 'Exordium',
                            'dungeon_class': 'com.megacrit.cardcrawl.dungeons.Exordium',
                            'room_class': 'com.megacrit.cardcrawl.rooms.MonsterRoom'}
    run = {'before': {'_rng': raw['_rng']}, 'boundaries': [raw], 'actions': [],
           'act': act, 'floor': floor,
           'scene': {'actual_dungeon_required': True, 'room': room}}
    with pytest.raises(ValueError, match='actual stock dungeon'):
        replay_controlled_run(run)


def test_act4_probe_only_accepts_canonical_encounters_and_sets_act_context():
    battle = native.LightspeedBattle()
    rng = payload()['_rng']
    with pytest.raises(ValueError, match='Act4'):
        battle.reset_encounter_probe(1, 'CULTIST', rng, 20, 4, 54, 'invalid-act4')
    for encounter, identifiers in (
        ('SHIELD_AND_SPEAR', ['SPIRE_SHIELD', 'SPIRE_SPEAR']),
        ('THE_HEART', ['CORRUPT_HEART']),
    ):
        battle.reset_encounter_probe(1, encounter, rng, 20, 4, 54, 'act4')
        game = battle.snapshot()['game_state']
        assert (game['act'], game['floor'], game['ascension_level']) == (4, 54, 20)
        assert [row['id'] for row in game['combat_state']['monsters']] == identifiers


@pytest.mark.parametrize('room', ['ELITE', 'BOSS'])
def test_act4_replay_rejects_the_ending_with_the_wrong_actual_room(room):
    raw = payload()
    raw['_oracle_mode'] = 'validation'
    raw['_stock_direct'] = {
        'ascension': 20, 'act': 4, 'floor': 54,
        'dungeon_id': 'TheEnding',
        'dungeon_class': 'com.megacrit.cardcrawl.dungeons.TheEnding',
        'room_class': 'com.megacrit.cardcrawl.rooms.MonsterRoom',
    }
    run = {'before': {'_rng': raw['_rng']}, 'boundaries': [raw], 'actions': [],
           'act': 4, 'floor': 54,
           'scene': {'actual_dungeon_required': True, 'room': room}}
    with pytest.raises(ValueError, match='actual stock dungeon or room'):
        replay_controlled_run(run)


@pytest.mark.parametrize("encounter,slot", [("COLLECTOR", 2), ("AUTOMATON", 1)])
def test_reserved_internal_target_slots_map_to_public_monster_index(encounter, slot):
    battle = native.LightspeedBattle()
    battle.reset(131100051, encounter, 20)
    raw = battle.snapshot()
    targeted = [a for a in raw["_legal_actions"] if a.get("target_index") is not None]
    assert {a["target_index"] for a in targeted} == {slot}
    projected = comparison_projection(raw, stock=False)
    assert all(json.loads(a).get("target_index", 0) == 0 for a in projected["actual_actions"])
    changed = copy.deepcopy(raw)
    changed["_legal_actions"].remove(targeted[0])
    assert structured_differences(projected, comparison_projection(changed, stock=False))
