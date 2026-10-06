import pytest

from tools.capture_act2_production_batch import ProductionBackend


@pytest.mark.parametrize('field', ['_rng', '_stock_direct', '_parity_scenario',
                                  '_timing_evidence', '_continuation'])
def test_production_refuses_validation_diagnostics_even_if_empty(field):
    raw = {'_oracle_mode': 'production', 'available_commands': ['choose'], field: {}}
    with pytest.raises(ValueError, match='isolation'):
        ProductionBackend.require_isolation(raw)


def test_production_refuses_intervention_commands():
    with pytest.raises(ValueError, match='isolation'):
        ProductionBackend.require_isolation({'_oracle_mode': 'production',
                                             'available_commands': ['parity_act2']})
