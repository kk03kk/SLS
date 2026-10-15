"""Prepared recipes differ only in the registered treatment and run identity."""

import copy
import json
import tomllib

import pytest

from sls.rl.value_adaptation import config_from_payload
from tools.prepare_act12_normalstart_study import (
    CONFIRMATION,
    PERIODIC,
    ROOT,
    check_seed_reservations,
    make_config,
)


def test_abc_recipes_preserve_ppo_and_remove_entry_and_stale_qualification():
    base = tomllib.loads((ROOT / "configs/train/ironclad_a20_act12_real_entry_20m_v6_r1.toml").read_text(encoding="utf-8"))
    original = copy.deepcopy(base)
    arms = [make_config(base, arm) for arm in "ABC"]
    assert base == original
    for config in arms:
        assert "start_curriculum" not in config
        assert "foundation_evidence" not in config["run"]
        assert "simulator_transition" not in config["warm_start"]
        assert config["model"] == base["model"]
        assert config["warm_start"]["checkpoint_sha256"] == base["warm_start"]["checkpoint_sha256"]
        assert config["run"]["seed"] == 210_000_000
        assert config["run"]["periodic_evaluation_seed_start"] == PERIODIC[0]
        assert config["run"]["final_evaluation_seed_start"] == CONFIRMATION[0]
        assert not config["run"]["selection_progress_guard"]
        assert config["stages"]["train"]["target_environment_steps"] == 90_013_696 + 1_015_808
        unchanged = {k: v for k, v in config["ppo"].items() if k in base["ppo"]}
        assert unchanged == base["ppo"]
    assert arms[0]["ppo"]["advantage_normalization"] == arms[2]["ppo"]["advantage_normalization"] == "domain"
    assert arms[1]["ppo"]["advantage_normalization"] == "global"
    c = config_from_payload(arms[2])
    assert c.episodes == 512 and c.fit_episodes == 384 and c.max_decisions == 524_288
    assert config_from_payload(arms[0]) is None
    arms[2]["value_adaptation"]["seed_start"] = arms[2]["run"]["seed"]
    with pytest.raises(ValueError, match="overlap"):
        config_from_payload(arms[2])


@pytest.mark.parametrize("payload", [
    {"seed": PERIODIC[0]}, {"seed_range": list(CONFIRMATION)},
    {"periodic_evaluation_seed_start": PERIODIC[0] - 1, "periodic_evaluation_seed_count": 2},
    {"seed": 210_000_002}, {"seed": 260_000_002},
])
def test_seed_inventory_refuses_prior_use_or_reservation(tmp_path, payload):
    path = tmp_path / "configs" / "earlier.json"
    path.parent.mkdir()
    path.write_text(json.dumps(payload), encoding="utf-8")
    with pytest.raises(ValueError, match="collision"):
        check_seed_reservations(tmp_path)


def test_seed_inventory_keeps_final_set_sealed_and_refuses_unreadable_evidence(tmp_path):
    empty = check_seed_reservations(tmp_path)
    assert empty["final_set_used"] is False
    assert empty["reserved_final_seeds"][0] == 9_000_000_000_000
    directory = tmp_path / "local/reports"
    directory.mkdir(parents=True)
    path = directory / "evaluation.json"
    path.write_text("unfinished evidence", encoding="utf-8")
    with pytest.raises(ValueError, match="unreadable"):
        check_seed_reservations(tmp_path)
