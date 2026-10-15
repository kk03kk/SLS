"""Production and probes share stock event constructor HP-stream timing."""

import gzip
import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend, native
from sls.contracts import Action, ActionKind
from sls.curriculum import ironclad_fullrun_profile


def _stock_event():
    fixture = json.loads(gzip.decompress((Path(__file__).parents[1] /
        "fixtures/regressions/masked-bandits-constructor-131213000.json.gz").read_bytes()))
    backend = SimulatorBackend(ironclad_fullrun_profile(20, require_heart=True))
    backend.load_checkpoint(fixture["before"])
    decision = backend.step(Action.from_dict(fixture["entry_action"])).decision
    assert decision.observation.to_dict() == fixture["event_observation"]
    assert backend.validation_snapshot().rng_streams == fixture["event_rng"]
    return backend, decision, fixture


def test_actual_masked_bandits_entry_and_fight_match_stock_and_restore():
    backend, _, fixture = _stock_event()
    restored = SimulatorBackend(backend.profile)
    restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint())))
    action = Action.from_dict(fixture["fight_action"])
    decision = backend.step(action).decision
    restored.step(action)
    assert restored.checkpoint() == backend.checkpoint()
    assert decision.observation.to_dict() == fixture["fight_observation"]
    assert backend.validation_snapshot().rng_streams == fixture["fight_rng"]


def test_masked_bandits_pay_does_not_undo_constructor_rng_and_restores():
    backend, decision, _ = _stock_event()
    before = backend.validation_snapshot().rng_streams
    action = next(a for a in decision.actions if a.option_id == "event-option:0")
    restored = SimulatorBackend(backend.profile)
    restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint())))
    decision = backend.step(action).decision
    restored.step(action)
    assert restored.checkpoint() == backend.checkpoint()
    assert decision.observation.run.gold == 0
    assert backend.validation_snapshot().rng_streams == before
    assert any(a.kind is ActionKind.CHOOSE_MAP_NODE for a in decision.actions)


@pytest.mark.parametrize("event,count,leave", [("MASKED_BANDITS", 3, 0), ("MYSTERIOUS_SPHERE", 4, 1)])
def test_probe_does_not_double_apply_shared_constructor_consumption(event, count, leave):
    run = native.LightspeedRunState()
    run.reset(0, 20)
    rng = run.snapshot()["rng"]
    rng["monster_hp"] = native.rng_probe(1)["initial"]  # probe's seed + floor
    run.reset_event_probe(0, event, rng, ascension=20)
    state = run.snapshot()
    assert state["rng"]["monster_hp"]["counter"] == count
    before = state["rng"]["monster_hp"]
    run.step(leave)
    assert run.snapshot()["rng"]["monster_hp"] == before


@pytest.mark.parametrize("event,count,fight", [("MASKED_BANDITS", 3, 1), ("MYSTERIOUS_SPHERE", 4, 0)])
def test_probe_fight_reconstructs_same_hp_stream_without_consuming_it_twice(event, count, fight):
    run = native.LightspeedRunState()
    run.reset(0, 20)
    rng = run.snapshot()["rng"]
    rng["monster_hp"] = native.rng_probe(1)["initial"]
    run.reset_event_probe(0, event, rng, ascension=20)
    before = run.snapshot()["rng"]["monster_hp"]
    assert before["counter"] == count
    run.step(fight)
    assert run.snapshot()["rng"]["monster_hp"] == before
    assert "combat_checkpoint" in run.snapshot()
