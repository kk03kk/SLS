import hashlib
import json
from types import SimpleNamespace

import pytest

from tools import audit_simulator_seeds
from tools.verify_fullrun_audit_evidence import BOSSES, verify_evidence


def _evidence(tmp_path):
    seed_audit = tmp_path / "seeds.json"
    seed_audit.write_text(json.dumps({
        "schema": "sls-simulator-seed-invariants-v1", "requested_seeds": 10_000,
        "completed_seeds": 10_000, "passed": True, "failures": [],
        "seed_range": [0, 10_000],
    }), encoding="utf-8")
    paths = []
    for seed in range(30):
        path = tmp_path / f"comparison-{seed}.json"
        path.write_text(json.dumps({
            "schema": "sls-policy-trajectory-comparison-v2", "passed": True,
            "contract_match": True, "seed_match": True, "backend_match": True,
            "trajectory_complete": True, "first_divergence": None,
            "matched_boundaries": 100, "simulator_boundaries": 100,
            "original_boundaries": 100, "seed": seed, "bosses": sorted(BOSSES),
            **{f"{backend}_sha256": hashlib.sha256(f"{backend}:{seed}".encode()).hexdigest()
               for backend in ("simulator", "original")},
        }), encoding="utf-8")
        paths.append(path)
    return paths, seed_audit


def test_complete_distinct_evidence_passes(tmp_path):
    paths, audit = _evidence(tmp_path)
    assert verify_evidence(paths, audit)["passed"] is True


def test_repeated_report_cannot_satisfy_independent_trajectory_count(tmp_path):
    paths, audit = _evidence(tmp_path)
    result = verify_evidence([paths[0]] * 30, audit)
    assert result["passed"] is False
    assert result["trajectory_comparisons"] == 1


@pytest.mark.parametrize("mutation", [
    {"trajectory_complete": False}, {"matched_boundaries": 0},
    {"original_boundaries": 99}, {"seed": 0}, {"seed": 2**64},
    {"simulator_sha256": None}, {"passed": False, "matched": True},
])
def test_incomplete_or_duplicate_evidence_fails(tmp_path, mutation):
    paths, audit = _evidence(tmp_path)
    row = json.loads(paths[1].read_text(encoding="utf-8"))
    paths[1].write_text(json.dumps({**row, **mutation}), encoding="utf-8")
    assert verify_evidence(paths, audit)["passed"] is False


@pytest.mark.parametrize("mutation", [
    {"completed_seeds": 9999}, {"failures": [{"seed": 1}]},
    {"seed_range": [0, 9999]}, {"schema": "unknown"},
])
def test_inconsistent_seed_audit_cannot_pass_by_flag_alone(tmp_path, mutation):
    paths, audit = _evidence(tmp_path)
    row = json.loads(audit.read_text(encoding="utf-8"))
    audit.write_text(json.dumps({**row, **mutation}), encoding="utf-8")
    assert verify_evidence(paths, audit)["passed"] is False


def test_seed_audit_accepts_terminal_on_last_allowed_action(monkeypatch):
    class Backend:
        def __init__(self, _profile):
            pass

        def reset(self, _seed):
            return SimpleNamespace(terminal=False, actions=[SimpleNamespace(candidate_id="end")])

        def checkpoint(self):
            return {}

        def load_checkpoint(self, _state):
            return self.reset(0)

        def step(self, _action):
            return SimpleNamespace(truncated=False, decision=SimpleNamespace(terminal=True, actions=[]))

    monkeypatch.setattr(audit_simulator_seeds, "SimulatorBackend", Backend)
    monkeypatch.setattr(audit_simulator_seeds, "_decision_payload", lambda decision: {"terminal": decision.terminal})
    result = audit_simulator_seeds.audit_seeds(0, 1, max_actions=1)
    assert result["passed"] is True
    assert result["total_actions"] == 1


@pytest.mark.parametrize("count,limit", [(0, 1), (-1, 1), (1, 0), (1, -1)])
def test_seed_audit_requires_positive_workload(count, limit):
    with pytest.raises(ValueError, match="positive"):
        audit_simulator_seeds.audit_seeds(0, count, max_actions=limit)
