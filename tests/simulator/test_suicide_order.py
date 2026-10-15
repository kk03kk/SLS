"""Stock SuicideAction sets HP to zero and dies; it is not a damage hit."""

from sls.backends.simulator import native


def _exploders(*, hp=300, intangible=False):
    battle = native.LightspeedBattle()
    battle.reset(131200051, "FOUR_SHAPES", 20)
    battle.set_player_health(hp, 300)
    payload = battle.snapshot()
    for monster in payload["game_state"]["combat_state"]["monsters"]:
        identifier = monster["monster_id"]
        monster["move_id"] = {
            "EXPLODER": "EXPLODER_EXPLODE",
            "REPULSOR": "REPULSOR_REPULSE",
            "SPIKER": "SPIKER_SPIKE",
        }[identifier]
        if identifier == "EXPLODER" and intangible:
            # Lossless checkpoint enum INTANGIBLE=22; no artificial rule API.
            monster["_internal"]["status_bits"] |= 1 << 22
            monster["_internal"]["unique_power0"] = 10
            monster["_internal"]["power_order"] = [22]
    battle.load_checkpoint({"game_state": payload["game_state"], "rng": payload["_rng"]})
    return battle


def test_exploder_suicide_bypasses_intangible():
    # ExplosivePower.duringTurn queues SuicideAction before THORNS DamageAction.
    battle = _exploders(intangible=True)
    battle.step("end_turn")
    monsters = battle.public_combat_probe_snapshot()["monsters"]
    assert all(m["current_hp"] == 0 for m in monsters if m["content_id"] == "EXPLODER")


def test_fatal_explosion_still_kills_its_owner_first():
    battle = _exploders(hp=1)
    battle.step("end_turn")
    assert battle.snapshot()["outcome"] == "PLAYER_LOSS"
    monsters = battle.public_combat_probe_snapshot()["monsters"]
    first = next(m for m in monsters if m["content_id"] == "EXPLODER")
    assert first["current_hp"] == 0


def test_reptomancer_death_kills_daggers_even_with_block():
    battle = native.LightspeedBattle()
    battle.reset(131200066, "REPTOMANCER", 20)
    payload = battle.snapshot()
    leader = None
    for monster in payload["game_state"]["combat_state"]["monsters"]:
        if monster["monster_id"] == "REPTOMANCER":
            monster["current_hp"] = 1
            leader = int(monster["instance_id"].split(":")[1])
        else:
            monster["block"] = 100
    battle.load_checkpoint({"game_state": payload["game_state"], "rng": payload["_rng"]})
    battle.set_card_piles(["ANGER"], [], [], [])
    battle.step("play", card_index=1, target_index=leader)
    assert battle.snapshot()["outcome"] == "PLAYER_VICTORY"
    assert all(m["current_hp"] == 0 for m in battle.public_combat_probe_snapshot()["monsters"])


def test_fatal_dagger_attack_stops_before_its_later_suicide():
    # Unlike ExplosivePower, stock SnakeDagger.takeTurn queues damage first.
    # Preserve this adjacent branch when fixing the shared SuicideAction.
    battle = native.LightspeedBattle()
    battle.reset(131200066, "REPTOMANCER", 20)
    battle.set_player_health(1, 80)
    payload = battle.snapshot()
    dagger = next(m for m in payload['game_state']['combat_state']['monsters']
                  if m['monster_id'] == 'DAGGER')
    dagger['move_id'] = 'DAGGER_EXPLODE'
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    battle.step('end_turn')
    assert battle.snapshot()['outcome'] == 'PLAYER_LOSS'
    first = next(m for m in battle.public_combat_probe_snapshot()['monsters']
                 if m['content_id'] == 'DAGGER')
    assert first['current_hp'] > 0
