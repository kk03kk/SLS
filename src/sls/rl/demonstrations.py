"""Simulator-generated teacher corpus shared by BC and state reservoirs."""

from __future__ import annotations

import gzip
import json
from pathlib import Path
from typing import Any

from sls.model.encoding import vocabulary_hash
from sls.rl.training_contract import canonical_digest, native_source_digest
from sls.rl.training_mode import TrainingMode, require_artifact_mode


TEACHER_CORPUS_SCHEMA = "sls-teacher-corpus-v1"


def load_teacher_corpus(path: str | Path) -> dict[str, Any]:
    with gzip.open(Path(path), "rt", encoding="utf-8") as stream:
        payload = json.load(stream)
    if payload.get("schema") != TEACHER_CORPUS_SCHEMA:
        raise ValueError("unsupported teacher corpus")
    supplied = payload.get("corpus_sha256")
    unsigned = dict(payload)
    unsigned.pop("corpus_sha256", None)
    if supplied != canonical_digest(unsigned):
        raise ValueError("teacher corpus digest mismatch")
    if payload.get("native_source_sha256") != native_source_digest():
        raise ValueError("teacher corpus native source is stale")
    if payload.get("vocabulary_sha256") != vocabulary_hash():
        raise ValueError("teacher corpus vocabulary is stale")
    if require_artifact_mode(payload, production=False) is not TrainingMode.EXPERIMENTAL:
        raise ValueError("teacher corpus must be explicitly experimental")
    if payload.get("policy_transfer_verified") is not False:
        raise ValueError("teacher corpus cannot claim production policy transfer")
    generation = payload.get("generation_config")
    if not isinstance(generation, dict) or payload.get(
        "generation_config_sha256"
    ) != canonical_digest(generation):
        raise ValueError("teacher corpus generation provenance is invalid")
    if not str(payload.get("git_commit") or ""):
        raise ValueError("teacher corpus Git provenance is missing")
    examples = payload.get("examples")
    if not isinstance(examples, list) or not examples:
        raise ValueError("teacher corpus is empty")
    if int(payload.get("rejected_labels", -1)) != 0 or payload.get("rejections"):
        raise ValueError("teacher corpus contains unmatched native actions")
    if any(
        int(example.get("candidate_match_count", 0)) != 1
        or example.get("checkpoint_sha256") != canonical_digest(example.get("checkpoint"))
        for example in examples
    ):
        raise ValueError("teacher corpus contains ambiguous or corrupted labels")
    return payload
