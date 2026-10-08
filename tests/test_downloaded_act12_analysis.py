"""Small evidence validation checks; never import torch or start workers."""
import json
import subprocess
import sys

import pytest

from tools.analyze_downloaded_act12_study import (
    extension_decision,
    paired_reach,
    verify_bundle,
)
from tools.analyze_reward_screen import digest


def test_analysis_import_does_not_load_torch():
    subprocess.run([sys.executable, '-c',
                    "import sys; import tools.analyze_downloaded_act12_study; "
                    "assert 'torch' not in sys.modules"], check=True)


def test_bundle_rejects_tampered_evidence(tmp_path):
    evidence = tmp_path / 'raw.json'
    evidence.write_text('{}')
    (tmp_path / 'training-bundle.json').write_text(json.dumps({'files': {'raw.json': digest(evidence)}}))
    assert verify_bundle(tmp_path)['raw.json'] == digest(evidence)
    evidence.write_text('{"successes": 99}')
    with pytest.raises(ValueError, match='hash mismatch'):
        verify_bundle(tmp_path)


def test_bundle_rejects_escaping_path(tmp_path):
    (tmp_path / 'training-bundle.json').write_text(json.dumps({'files': {'../outside': 'invalid'}}))
    with pytest.raises(ValueError, match='escapes'):
        verify_bundle(tmp_path)


def comparisons(net=30, p=.01):
    return {key: {'net': net, 'paired_seeds': 2048, 'exact_mcnemar_p': p}
            for key in ('lambda1_vs_control', 'lambda1_vs_parent', 'control_vs_parent_secondary')}


def arms(healthy=True):
    return {key: {'strict_registered_health_gate_passed': healthy,
                 'late_success_windows': [{'successes': 1}, {'successes': 1}]}
            for key in ('control', 'experimental')}


def test_no_extension_for_small_unconfirmed_gain():
    result = extension_decision(comparisons(net=3, p=.55), arms())
    assert not result['automatic_20m_extension']
    assert result['eligible_parent_arm'] is None


def test_failed_historical_health_gate_is_not_silently_relaxed():
    assert not extension_decision(comparisons(), arms(False))['automatic_20m_extension']


def test_primary_and_explicit_secondary_selection():
    assert extension_decision(comparisons(), arms())['eligible_parent_arm'] == 'experimental'
    c = comparisons()
    c['lambda1_vs_control']['net'] = -1
    assert extension_decision(c, arms())['eligible_parent_arm'] == 'control'


def test_late_success_is_required_for_each_candidate():
    a = arms()
    for value in a.values():
        value['late_success_windows'][1]['successes'] = 0
    assert not extension_decision(comparisons(), a)['automatic_20m_extension']


def test_reach_pairs_use_all_openings():
    def result(reaches):
        return {'seed_results': [{'seed': i, 'bosses': {'1': 'B', **({'2': 'C'} if r else {})}}
                                 for i, r in enumerate(reaches)]}
    r = paired_reach(result([True, True, False]), result([True, False, True]))
    assert (r['both_reached'], r['lost_reach'], r['gained_reach']) == (1, 1, 1)
    assert r['exact_mcnemar_p'] == 1
    with pytest.raises(ValueError, match='seed mismatch'):
        paired_reach(result([True]), result([True, False]))
