from pathlib import Path

import pytest

from tools.summarize_act2_qualification import exclude_superseded_system_rows, rollup


def selection():
    return {"native_source_sha256": "source", "model_sha256": "model",
            "runs": [{"seed": s} for s in range(8000011000000, 8000011000008)]}


def test_partial_or_divergent_evidence_never_qualifies():
    report = {"native_source_sha256": "source", "runs": [
        {"seed": 131100000, "status": "HARNESS_MATCH", "first_divergence": None},
        {"seed": 131100001, "status": "INCOMPLETE_PREFIX_MATCH", "first_divergence": None}]}
    result = rollup([report], [], [], selection(), "source")
    assert result["training_gate"] == "NOT_QUALIFIED"
    assert result["controlled_matched"] == 1
    assert 131100001 in result["controlled_missing_or_failed"]
    assert len(result["production_missing_or_failed"]) == 8


def test_stale_source_or_duplicate_seed_is_rejected():
    report = {"native_source_sha256": "stale", "runs": []}
    with pytest.raises(ValueError, match="stale"):
        rollup([report], [], [], selection(), "source")
    report = {"native_source_sha256": "source", "runs": [
        {"seed": 131100000, "status": "HARNESS_MATCH", "first_divergence": None}]}
    with pytest.raises(ValueError, match="duplicate"):
        rollup([report, report], [], [], selection(), "source")


def test_even_complete_counts_require_independent_recovery_and_local_checks():
    report = {"native_source_sha256": "source", "runs": [
        {"seed": s, "status": "SYSTEM_BRANCH_MATCH", "first_divergence": None,
         "act2_boundaries": 1, "reward_boundaries": 1, "checkpoint_replays": 1}
        for s in range(131100000, 131100072)]}
    production = [{"native_source_sha256": "source", "seed": s,
        "evaluation_environment": {"frozen_model_sha256": "model"},
        "status": "TRAJECTORY_MATCH", "first_divergence": None}
        for s in range(8000011000000, 8000011000008)]
    result = rollup([report], [], production, selection(), "source")
    assert result["training_gate"] == "EVIDENCE_COMPLETE_PENDING_RECOVERY_AND_LOCAL_CHECKS"


def test_system_exclusion_names_old_report_and_preserves_replacement():
    reports = [(Path('old.json'), {'runs': [{'seed': 66}, {'seed': 67}]}),
               (Path('new.json'), {'runs': [{'seed': 66}]})]
    exclude_superseded_system_rows(reports, [['old.json', '66']])
    assert reports[0][1]['runs'] == [{'seed': 67}]
    assert reports[1][1]['runs'] == [{'seed': 66}]


def test_system_exclusion_cannot_hide_a_missing_obligation():
    reports = [(Path('old.json'), {'runs': [{'seed': 66}]}),
               (Path('new.json'), {'runs': [{'seed': 67}]})]
    with pytest.raises(ValueError, match='later replacement'):
        exclude_superseded_system_rows(reports, [['old.json', '66']])
    with pytest.raises(ValueError, match='exactly once'):
        exclude_superseded_system_rows(reports, [['unknown.json', '66']])


def test_clock_conditioned_match_is_never_an_unconditional_production_pass():
    controlled = {'native_source_sha256': 'source', 'runs': [
        {'seed': s, 'status': 'SYSTEM_BRANCH_MATCH', 'first_divergence': None,
         'act2_boundaries': 1, 'reward_boundaries': 1, 'checkpoint_replays': 1}
        for s in range(131100000, 131100072)]}
    production = [{'native_source_sha256': 'source', 'seed': s,
        'evaluation_environment': {'frozen_model_sha256': 'model'},
        'status': 'TRAJECTORY_MATCH', 'first_divergence': None}
        for s in range(8000011000000, 8000011000008)]
    production[0]['status'] = 'CONDITIONAL_PUBLIC_TRAJECTORY_MATCH'
    result = rollup([controlled], [], production, selection(), 'source')
    assert result['training_gate'] == 'NOT_QUALIFIED'
    assert result['production_missing_or_failed'] == [8000011000000]
