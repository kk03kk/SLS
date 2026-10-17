import copy
import json
from pathlib import Path

import pytest

from sls.audit.public_projection_migration import stock_observation


def case():
    return json.loads(Path('tests/fixtures/regressions/public-selection-quantity-stock.json').read_text())['cases']['GRID']


def test_stock_raw_quantity_upgrades_only_the_declared_public_context():
    row=case()
    current,receipt=stock_observation(row['observation'],row['actions'],row['terminal'],row['stock_raw'])
    quantity=next(e for e in current['choice_options'] if e['instance_id']=='selection-requirement')
    assert quantity['properties']=={'amount':2}
    assert current['selected_cards']==row['observation']['selected_cards']
    assert receipt['legacy_all_other_fields_equal'] is True
    assert receipt['old_observation_preserved_in_raw_evidence'] is True


def test_projection_migration_cannot_hide_resource_or_card_differences():
    row=case()
    corrupted=copy.deepcopy(row['observation'])
    corrupted['player']['current_hp']+=1
    with pytest.raises(ValueError,match='unapproved historical'):
        stock_observation(corrupted,row['actions'],row['terminal'],row['stock_raw'])


def test_current_projection_is_idempotent():
    row=case()
    current,_=stock_observation(row['observation'],row['actions'],row['terminal'],row['stock_raw'])
    again,receipt=stock_observation(current,row['actions'],row['terminal'],row['stock_raw'])
    assert again==current and receipt is None
