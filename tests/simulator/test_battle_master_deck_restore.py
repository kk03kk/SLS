import pytest

from sls.backends.simulator import native


def test_battle_checkpoint_restores_gold_and_permanent_deck():
    battle = native.LightspeedBattle()
    battle.reset(131200129, "THE_HEART", 20)
    payload = battle.snapshot()
    game = payload["game_state"]
    game["gold"] = 321
    game["combat_state"]["player"]["_internal"]["gold"] = 321
    game["master_deck"] = [
        {"id": "SEARING_BLOW", "upgrades": 3, "misc": 3},
        {"id": "RITUAL_DAGGER", "upgrades": 1, "misc": 42},
        {"id": "STRIKE_RED", "upgrades": 1, "misc": 0},
    ]
    battle.load_checkpoint({"game_state": game, "rng": payload["_rng"]})
    current = battle.snapshot()
    assert current["game_state"]["gold"] == 321
    assert current["game_state"]["master_deck"] == game["master_deck"]
    restored = native.LightspeedBattle()
    restored.load_checkpoint({"game_state": current["game_state"], "rng": current["_rng"]})
    assert restored.snapshot() == current


def test_battle_checkpoint_retains_permanent_bottle_indices():
    battle = native.LightspeedBattle()
    battle.reset(7, "CULTIST", 20)
    payload = battle.snapshot()
    game = payload['game_state']
    game['master_deck'] = [
        {'id': name, 'upgrades': 0, 'misc': 0}
        for name in ['STRIKE_RED', 'DEFEND_RED', 'INFLAME']]
    game['master_deck_bottle_indices'] = [0, 1, 2]
    battle.load_checkpoint({'game_state': game, 'rng': payload['_rng']})
    assert battle.snapshot()['game_state']['master_deck_bottle_indices'] == [0, 1, 2]


@pytest.mark.parametrize('card', [
    {'id': 'STRIKE_RED', 'upgrades': -1, 'misc': 0},
    {'id': 'STRIKE_RED', 'upgrades': 2, 'misc': 0},
    {'id': 'SEARING_BLOW', 'upgrades': 3, 'misc': 4},
])
def test_master_deck_rejects_silently_changed_upgrade_counts(card):
    battle = native.LightspeedBattle()
    battle.reset(7, 'CULTIST', 20)
    payload = battle.snapshot()
    payload['game_state']['master_deck'] = [card]
    with pytest.raises(ValueError, match='upgrade'):
        battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})


@pytest.mark.parametrize('indices', [[0], [-2, -1, -1], [99, -1, -1]])
def test_master_deck_rejects_invalid_bottle_references(indices):
    battle = native.LightspeedBattle()
    battle.reset(7, 'CULTIST', 20)
    payload = battle.snapshot()
    payload['game_state']['master_deck_bottle_indices'] = indices
    with pytest.raises(ValueError, match='bottle'):
        battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
