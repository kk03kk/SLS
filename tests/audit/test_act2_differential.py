import copy
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import comparison_projection, replay_controlled_run
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
