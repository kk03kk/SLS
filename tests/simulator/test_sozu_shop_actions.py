import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A20_ACT2


def test_sozu_shop_has_no_effectful_potion_purchase():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-sozu-shop-131100068.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.load_checkpoint(fixture['before'])
    assert [a.to_dict() for a in decision.actions if a.kind is ActionKind.BUY_POTION] == []
