"""Evidence-backed training gate for policy transfer rather than RNG trajectories."""

from __future__ import annotations

import json
from pathlib import Path
import subprocess
from typing import Any

from sls.content.scope import ROOT, ironclad_a0_scope_hash, policy_excluded_content_ids
from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl.training_contract import canonical_digest, native_source_digest, source_sha256
from sls.validation.transfer import POLICY_TRANSFER_SCHEMA


POLICY_TRANSFER_EVIDENCE_SCHEMA = "sls-policy-transfer-evidence-v1"
STOCHASTIC_CATEGORIES = {
    "draw_shuffle", "card_rewards", "potion_rewards", "relic_rewards",
    "random_events", "encounters",
}


def _git_state() -> tuple[str, bool]:
    commit = subprocess.run(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, check=True,
        capture_output=True, text=True,
    ).stdout.strip()
    status = subprocess.run(
        ["git", "status", "--porcelain"], cwd=ROOT, check=True,
        capture_output=True, text=True,
    ).stdout
    return commit, not bool(status.strip())


def _load_evidence(gate: dict[str, Any]) -> dict[str, Any]:
    configured = gate.get("evidence")
    if not configured:
        raise ValueError("policy-transfer gate has no evidence artifact")
    path = Path(str(configured))
    if not path.is_absolute():
        path = ROOT / path
    evidence = json.loads(path.read_text(encoding="utf-8"))
    if evidence.get("schema") != POLICY_TRANSFER_EVIDENCE_SCHEMA:
        raise ValueError("unsupported policy-transfer evidence")
    supplied = evidence.get("evidence_sha256")
    unsigned = dict(evidence)
    unsigned.pop("evidence_sha256", None)
    if supplied != canonical_digest(unsigned):
        raise ValueError("policy-transfer evidence digest mismatch")
    if gate.get("evidence_sha256") != supplied:
        raise ValueError("policy-transfer gate is not bound to the evidence artifact")
    return evidence


def verify_policy_transfer_gate(
    path: str | Path, *, profile_id: str, require_canary: bool = True,
    require_clean_repository: bool = True,
) -> dict[str, Any]:
    payload = json.loads(Path(path).read_text(encoding="utf-8"))
    supplied_gate_digest = payload.get("gate_sha256")
    unsigned_gate = dict(payload)
    unsigned_gate.pop("gate_sha256", None)
    if supplied_gate_digest != canonical_digest(unsigned_gate):
        raise ValueError("policy-transfer gate digest mismatch")
    if payload.get("schema") != POLICY_TRANSFER_SCHEMA:
        raise ValueError("unsupported policy-transfer gate")
    if payload.get("encoding_schema") != ENCODING_SCHEMA:
        raise ValueError("policy-transfer encoding schema is stale")
    if payload.get("vocabulary_sha256") != vocabulary_hash():
        raise ValueError("policy-transfer vocabulary is stale")
    if set(payload.get("excluded_content_ids") or ()) != set(policy_excluded_content_ids()):
        raise ValueError("policy-transfer excluded-content scope is stale")
    if bool(payload.get("exact_trajectory_required", True)):
        raise ValueError("policy-transfer gate must not require exact RNG trajectories")
    if profile_id not in set(map(str, payload.get("profiles") or ())):
        raise ValueError(f"policy-transfer gate does not cover {profile_id}")
    if not payload.get("deterministic_probe_suites"):
        raise ValueError("policy-transfer gate has no deterministic mechanism probes")
    evidence = _load_evidence(payload)
    if evidence.get("profile") != profile_id:
        raise ValueError("policy-transfer evidence is for another profile")
    if evidence.get("encoding_schema") != ENCODING_SCHEMA or \
            evidence.get("vocabulary_sha256") != vocabulary_hash():
        raise ValueError("policy-transfer evidence policy contract is stale")
    if evidence.get("content_scope_sha256") != ironclad_a0_scope_hash():
        raise ValueError("policy-transfer evidence content scope is stale")
    if evidence.get("native_source_sha256") != native_source_digest():
        raise ValueError("policy-transfer evidence native source is stale")
    commit, clean = _git_state()
    if evidence.get("git_commit") != commit:
        raise ValueError("policy-transfer evidence Git commit is stale")
    if require_clean_repository and not clean:
        raise ValueError("policy-transfer gate requires a clean repository")
    for relative, digest in dict(evidence.get("source_files") or {}).items():
        source = ROOT / str(relative)
        if not source.is_file() or source_sha256(source) != digest:
            raise ValueError(f"policy-transfer evidence source is stale: {relative}")

    acceptance = dict(payload.get("acceptance") or {})
    public = dict(evidence.get("public_contract") or {})
    if not public.get("accepted"):
        raise ValueError("public policy contract evidence is not accepted")
    if int(public.get("boundaries", 0)) < int(acceptance.get("minimum_public_boundaries", 1)):
        raise ValueError("public policy contract evidence is too small")
    for field in ("bosses", "screens", "selected_actions"):
        required = set(map(str, acceptance.get(f"required_{field}") or ()))
        if not required.issubset(set(map(str, public.get(field) or ()))):
            raise ValueError(f"public policy contract evidence lacks required {field}")

    distributions = dict(evidence.get("stochastic_distributions") or {})
    if set(distributions) != STOCHASTIC_CATEGORIES:
        raise ValueError("stochastic distribution evidence lacks required categories")
    minimum_samples = int(acceptance.get("minimum_stochastic_samples_per_category", 2_000))
    minimum_seeds = int(acceptance.get("minimum_stochastic_seeds_per_category", 32))
    maximum_tv = float(payload.get("stochastic_total_variation_limit", 0.05))
    for category, record in distributions.items():
        if not record.get("accepted"):
            raise ValueError(f"stochastic distribution is not accepted: {category}")
        if min(int(record.get("original_samples", 0)), int(record.get("simulator_samples", 0))) < minimum_samples:
            raise ValueError(f"stochastic distribution has too few samples: {category}")
        if min(int(record.get("original_seeds", 0)), int(record.get("simulator_seeds", 0))) < minimum_seeds:
            raise ValueError(f"stochastic distribution has too few seeds: {category}")
        if float(record.get("confidence", 0.0)) < 0.95:
            raise ValueError(f"stochastic distribution confidence is too low: {category}")
        if float(record.get("total_variation_upper_bound", 1.0)) > maximum_tv:
            raise ValueError(f"stochastic distribution TV exceeds threshold: {category}")

    deterministic = dict(evidence.get("deterministic_mechanisms") or {})
    suites = set(map(str, payload["deterministic_probe_suites"]))
    if suites != set(deterministic):
        raise ValueError("deterministic mechanism evidence does not cover the exact suites")
    for suite, record in deterministic.items():
        artifact = ROOT / str(record.get("path") or "")
        if not artifact.is_file():
            raise ValueError(f"deterministic evidence artifact is missing: {suite}")
        artifact_payload = json.loads(artifact.read_text(encoding="utf-8"))
        if artifact_payload.get("audit_sha256") != record.get("audit_sha256"):
            raise ValueError(f"deterministic evidence artifact changed: {suite}")
        if int(record.get("entries", 0)) <= 0:
            raise ValueError(f"deterministic evidence suite is empty: {suite}")
    if require_canary and payload.get("production_requires_original_policy_canary", True):
        canary = dict(evidence.get("policy_canary") or {})
        report_path = ROOT / str(canary.get("path") or "")
        if not report_path.is_file() or source_sha256(report_path) != canary.get("report_sha256"):
            raise ValueError("policy canary report is missing or stale")
        report = json.loads(report_path.read_text(encoding="utf-8"))
        supplied = report.get("report_sha256")
        unsigned = dict(report)
        unsigned.pop("report_sha256", None)
        if supplied != canonical_digest(unsigned):
            raise ValueError("policy canary report digest mismatch")
        if not report.get("accepted") or len(set(report.get("seeds") or ())) < 20:
            raise ValueError("policy canary is not accepted or has too few seeds")
        for backend in ("original", "simulator"):
            summary = dict(report.get(backend) or {})
            if any(int(summary.get(field, 0)) for field in (
                "invalid_actions", "empty_decisions", "backend_truncations",
            )):
                raise ValueError(f"policy canary has unsafe {backend} outcomes")
        if canary.get("artifact_sha256") != report.get("artifact_sha256"):
            raise ValueError("policy canary artifact binding mismatch")
    payload["verified_evidence"] = evidence
    return payload
