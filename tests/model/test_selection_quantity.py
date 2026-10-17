import copy
import json
from pathlib import Path

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model.batching import encode_decision
from sls.model.encoding import NUMERIC_FIELD_IDS


def test_required_count_is_public_distinguishable_without_tensor_layout_change():
    fixture=json.loads(Path('tests/fixtures/regressions/act2-empty-cage-grid-131100069.json').read_text())
    backend=SimulatorBackend(IRONCLAD_A20_ACT2)
    decision=backend.load_checkpoint(fixture['before'])
    entity=next(e for e in decision.observation.choice_options if e.instance_id=='selection-requirement')
    assert dict(entity.properties)=={'amount':2}
    before=encode_decision(decision)
    raw=copy.deepcopy(backend.raw_state)
    raw['public_screen']['select_count']=1
    after=encode_decision(backend._adapt(raw))
    assert before.entity_numeric.shape==after.entity_numeric.shape
    assert not torch.equal(before.entity_numeric[:,NUMERIC_FIELD_IDS['amount']],
                           after.entity_numeric[:,NUMERIC_FIELD_IDS['amount']])
    assert decision.actions==backend._adapt(raw).actions
