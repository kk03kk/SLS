"""Portable source-bound checks for the finite normal-start study release."""
from __future__ import annotations

import json
from pathlib import Path

from sls.rl.training_contract import native_source_digest, sha256_file

RELEASE = "tests/fixtures/evidence/act12-normalstart"


def validate_qualification(path: Path, expected_sha: str):
    from tools.qualify_normalstart_act12 import gate_digest
    if sha256_file(path) != expected_sha:
        raise ValueError("normal-start qualification hash changed")
    report = json.loads(path.read_text(encoding="utf-8"))
    facts = report.get("facts", {})
    explanations = facts.get("contract_explanations", [])
    if (report.get("schema") != "sls-normalstart-act12-qualification-v1"
            or report.get("qualification") != "PASS_CONTRACT_BOUNDED_ACT12"
            or report.get("profile") != "IRONCLAD_A20_ACT2" or report.get("ascension") != 20
            or report.get("start_distribution") != "NATURAL_RUN_START" or report.get("blockers")
            or report.get("native_source_sha256") != native_source_digest()
            or report.get("gate_source_sha256") != gate_digest()
            or facts.get("controlled_obligations") != 63 or facts.get("system_obligations") != 9
            or facts.get("natural_canaries") != 8 or facts.get("supplementary_elite_trajectories") != 3
            or report.get("scope_is_exhaustive") is not False
            or report.get("full_803_scope_qualification") is not False
            or not report.get("input_sha256") or len(report.get("canary_roles", [])) != 8
            or len(report.get("recoveries", [])) != 8
            or facts.get("system_unconditional_matches", 0) + sum(r.get("role") == "system" for r in explanations) != 9
            or facts.get("natural_unconditional_matches", 0) + sum(r.get("role") == "natural" for r in explanations) != 8
            or any(row.get("status") != "RECOVERED" or not row.get("backup_bytes_verified") for row in report["recoveries"])
            or any(row.get("unconditional_match") is not False for row in facts.get("contract_explanations", []))):
        raise ValueError("normal-start qualification missing, stale or outside its bounded scope")
    return report


def release_bindings(root: Path):
    qualification = root / RELEASE / "qualification.json"
    transition = root / RELEASE / "simulator-transition.json"
    if not qualification.is_file() or not transition.is_file():
        raise ValueError("normal-start release evidence is not published")
    report = validate_qualification(qualification, sha256_file(qualification))
    binding = {"evidence": f"{RELEASE}/simulator-transition.json", "evidence_sha256": sha256_file(transition)}
    from sls.rl.act1_transfer import validate_curriculum_simulator_transition
    validate_curriculum_simulator_transition({"transfer_kind": "curriculum-stage", "simulator_transition": binding},
        "1e30bb6cfa32f600cd63c59983c14ab8928fac4452c1586d630295d2c6ff9453", report["native_source_sha256"], root=root)
    value = json.loads(transition.read_text(encoding="utf-8"))
    if value.get("evidence", {}).get("qualification.json") != sha256_file(qualification):
        raise ValueError("source transition does not bind this qualification")
    return {"qualification": {"path": f"{RELEASE}/qualification.json", "sha256": sha256_file(qualification)},
            "transition": binding}
