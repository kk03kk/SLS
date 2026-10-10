"""Lite archives must retain every registered file except the policy export."""
import json

import pytest

from tools.analyze_act12_download import binary_pair, entry_summary
from tools.analyze_reward_screen import analyze_run, digest


def test_missing_export_switch_keeps_required_evidence(tmp_path):
    run = tmp_path / "pilot"
    run.mkdir()
    (run / "training-bundle.json").write_text(json.dumps({"files": {"pilot.pt": "a" * 64}}))
    with pytest.raises(FileNotFoundError, match="pilot.pt"):
        analyze_run(run, horizon=2, allow_missing_export=False)
    # The explicit exception must continue to require actual training evidence.
    with pytest.raises(FileNotFoundError, match="training-config.toml"):
        analyze_run(run, horizon=2, allow_missing_export=True)


@pytest.mark.parametrize("name", ["final.pt", "latest.pt", "stages/train/metrics.jsonl"])
def test_lite_does_not_allow_missing_core_evidence(tmp_path, name):
    (tmp_path / "training-bundle.json").write_text(json.dumps({"files": {name: "a" * 64}}))
    with pytest.raises(FileNotFoundError):
        analyze_run(tmp_path, horizon=2, allow_missing_export=True)


def test_existing_export_is_still_hash_checked(tmp_path):
    path = tmp_path / f"{tmp_path.name}.pt"
    path.write_bytes(b"bad")
    (tmp_path / "training-bundle.json").write_text(json.dumps({"files": {path.name: "a" * 64}}))
    assert digest(path) != "a" * 64
    with pytest.raises(ValueError, match="hash mismatch"):
        analyze_run(tmp_path, horizon=2, allow_missing_export=True)


def test_reach_comparison_keeps_all_initial_seeds():
    left = [{"seed": 10, "steps": 20, "success": False, "bosses": {"1": "CHAMP", "2": "CHAMP"}},
            {"seed": 11, "steps": 10, "success": False, "bosses": {"1": "CHAMP"}}]
    right = [{**left[0], "bosses": {"1": "CHAMP"}}, left[1]]
    result = binary_pair(left, right, reach=True)
    assert result["paired_seeds"] == 2 and result["net"] == -1
    assert binary_pair(left, right, reach=False)["net"] == 0


def test_no_entry_is_missing_population_not_zero_resources():
    assert entry_summary([{"act_entries": {}}], "2") == {"samples": 0}
