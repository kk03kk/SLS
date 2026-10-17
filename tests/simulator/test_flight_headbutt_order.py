"""Flight reduction waits behind Headbutt's discard selection in actual stock."""

import copy
import gzip
import json
from pathlib import Path

from sls.audit.decision_identity import mapped_action
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import ironclad_fullrun_profile


def test_flight_reduction_waits_for_headbutt_selection_and_restores_continuation():
    fixture = json.loads(gzip.decompress((Path(__file__).parents[1] /
        "fixtures/regressions/flight-headbutt-final-131213000.json.gz").read_bytes()))
    backend = SimulatorBackend(ironclad_fullrun_profile(20, require_heart=True))
    backend.load_checkpoint(fixture["before"])
    decision = backend.step(Action.from_dict(fixture["action"])).decision
    expected=copy.deepcopy(fixture['stock_selection'])
    # Stock Headbutt selects one discarded card; retain every old public field.
    expected['choice_options'].append({'instance_id':'selection-requirement',
        'content_id':'OPTION','owner_id':None,'properties':{'amount':1}})
    assert decision.observation.to_dict() == expected
    checkpoint = json.loads(json.dumps(backend.checkpoint()))
    restored = SimulatorBackend(ironclad_fullrun_profile(20, require_heart=True))
    restored_decision = restored.load_checkpoint(checkpoint)
    assert restored.checkpoint() == backend.checkpoint()
    action = mapped_action(fixture["selection_action"], fixture["stock_selection"],
                          decision.observation.to_dict())
    restored_action = next(a for a in restored_decision.actions if a.to_dict() == action)
    decision = backend.step(Action.from_dict(action)).decision
    restored.step(restored_action)
    assert restored.checkpoint() == backend.checkpoint()
    assert decision.observation.to_dict() == fixture["after_selection"]
