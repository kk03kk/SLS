import copy
import json
from pathlib import Path
from types import SimpleNamespace

import pytest

from sls.contracts import ActionKind
from tools import capture_green_key_batch
from tools.audit_green_key_capture import next_buff
from tools.capture_green_key_batch import choose
from tools.run_green_key_batch import validate

ROOT = Path(__file__).resolve().parents[2]


def test_green_manifest_has_independent_namespace_and_no_injected_buff():
    resource = ROOT / 'native/oracle/resources/spirecomm/parity/fullrun-green-key-r1.json'
    manifest = json.loads(resource.read_text())
    validate(manifest)
    seeds = {seed for scene in manifest['scenes'] for seed in scene['seeds']}
    assert seeds == {131200360, 131200361}
    assert all('buff' not in scene and 'encounter' not in scene for scene in manifest['scenes'])
    for other in resource.parent.glob('*.json'):
        if other != resource:
            content = json.loads(other.read_text())
            assert not seeds.intersection(seed for scene in content.get('scenes', [])
                                          for seed in scene.get('seeds', []))
    for field, value in [('emerald_key', True), ('final_act_available', False)]:
        invalid = copy.deepcopy(manifest)
        invalid['scenes'][0]['initial'][field] = value
        with pytest.raises(ValueError):
            validate(invalid)


def test_controlled_strategy_uses_only_legal_public_actions():
    attack = SimpleNamespace(kind=ActionKind.PLAY_CARD)
    end = SimpleNamespace(kind=ActionKind.END_TURN)
    decision = SimpleNamespace(actions=[attack, end])
    assert choose(decision, 'PLAY_ATTACKS_THEN_END') is attack
    assert choose(decision, 'END_TURN_ONLY') is end
    decision.actions = [end]
    assert choose(decision, 'PLAY_ATTACKS_THEN_END') is end
    decision.actions = []
    with pytest.raises(ValueError):
        choose(decision, 'END_TURN_ONLY')


def test_stock_combat_entry_is_explicit_and_seeds_do_not_reuse_prototype():
    folder = ROOT / 'native/oracle/resources/spirecomm/parity'
    manifest = json.loads((folder / 'fullrun-green-key-r2.json').read_text())
    validate(manifest)
    assert {s['seeds'][0] for s in manifest['scenes']} == {131200362, 131200363}
    assert 'com.megacrit.cardcrawl.characters.AbstractPlayer' in manifest['source_evidence']
    manifest.pop('entry_policy')
    with pytest.raises(ValueError, match='actual stock combat entry'):
        validate(manifest)


def test_map_draw_matches_independently_captured_stock_transition():
    # Actual original-game seed131200362, entry before -> settled combat.
    before = dict(counter=95, seed0=5037639165087250210, seed1=14124583170291490205)
    frozen = dict(before)
    buff, after = next_buff(before)
    assert buff == 2
    assert after == dict(counter=96, seed0=14124583170291490205, seed1=10469521420122236744)
    assert before == frozen


def test_entry_wait_rejects_old_idle_manager_and_empty_hand(monkeypatch):
    decision = SimpleNamespace(observation=SimpleNamespace(screen='COMBAT'),
                               actions=[SimpleNamespace(kind=ActionKind.END_TURN)])
    monkeypatch.setattr(capture_green_key_batch, 'adapt_original', lambda _: SimpleNamespace(decision=decision))
    raw = {'_stock_direct': {'act': 2, 'turn': 1, 'current_node_has_emerald_key': True,
                            'monsters': [{'next_move': -1}]},
           '_parity_scenario': {'corpus': 'fullrun-green-key-r2'},
           'game_state': {'combat_state': {'hand': []}}}
    assert not capture_green_key_batch.initial_ready(raw)
    raw['_stock_direct']['monsters'][0]['next_move'] = 2
    assert not capture_green_key_batch.initial_ready(raw)
    raw['game_state']['combat_state']['hand'] = [{'id': 'Bludgeon'}]
    assert capture_green_key_batch.initial_ready(raw)


def test_expanded_manifest_namespace_is_disjoint_and_does_not_inject_buff():
    folder = ROOT / 'native/oracle/resources/spirecomm/parity'
    path = folder / 'fullrun-green-key-r3.json'
    manifest = json.loads(path.read_text())
    validate(manifest)
    seeds = {seed for s in manifest['scenes'] for seed in s['seeds']}
    assert seeds == {131200371,131200372,131200373}
    assert all('buff' not in s and 'encounter' not in s for s in manifest['scenes'])
    for other in folder.glob('*.json'):
        if other != path:
            content = json.loads(other.read_text())
            assert not seeds.intersection(seed for s in content.get('scenes', []) for seed in s.get('seeds', []))


def test_max_hp_target_rule_reads_public_enemies_and_selects_a_legal_action():
    small = SimpleNamespace(kind=ActionKind.PLAY_CARD, target_id='MONSTER:0')
    big = SimpleNamespace(kind=ActionKind.PLAY_CARD, target_id='MONSTER:1')
    end = SimpleNamespace(kind=ActionKind.END_TURN)
    observation = SimpleNamespace(enemies=[SimpleNamespace(instance_id='MONSTER:0',max_hp=15),
                                           SimpleNamespace(instance_id='MONSTER:1',max_hp=151)])
    decision = SimpleNamespace(actions=[small,big,end], observation=observation)
    assert choose(decision, 'PLAY_MAX_HP_TARGET_THEN_END') is big
    decision.actions = [small,end]
    assert choose(decision, 'PLAY_MAX_HP_TARGET_THEN_END') is small
