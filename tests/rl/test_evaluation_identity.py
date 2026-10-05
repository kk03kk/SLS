"""Tests for canonical training-config and evaluation-identity recording.

A configuration's training identity is a property of its text, not of the
working copy's newline convention, and a recorded evaluation is only
interpretable next to the settings that produced it.
"""

from __future__ import annotations

from pathlib import Path

from sls.rl.training_contract import (
    evaluation_identity,
    sha256_file,
    training_config_digest,
)


def test_config_digest_ignores_line_ending_convention(tmp_path: Path) -> None:
    lf = tmp_path / "lf.toml"
    crlf = tmp_path / "crlf.toml"
    body = "[run]\nseed = 90000000\n"
    lf.write_bytes(body.encode("utf-8"))
    crlf.write_bytes(body.replace("\n", "\r\n").encode("utf-8"))
    assert training_config_digest(lf) == training_config_digest(crlf)
    # The raw-byte digest still differs, which is exactly the discrepancy this
    # helper removes from run identity.
    assert sha256_file(lf) != sha256_file(crlf)


def test_config_digest_is_byte_identical_to_raw_hash_on_lf_checkout(
    tmp_path: Path,
) -> None:
    path = tmp_path / "config.toml"
    path.write_bytes(b"[run]\nseed = 1\n")
    assert training_config_digest(path) == sha256_file(path)


def test_evaluation_identity_records_simulator_and_inference_settings() -> None:
    identity = evaluation_identity(
        device="cpu", environment_shards=8, ascension=20,
    )
    runtime = identity["runtime"]
    simulator = identity["simulator"]
    assert runtime["environment_shards"] == 8
    # The project's qualification work found outcomes depend on the CPU thread
    # setting, so it must be part of every recorded evaluation.
    assert "cpu_threads" in runtime
    assert "float32_matmul_precision" in runtime
    assert "deterministic_algorithms" in runtime
    assert runtime["gpu"] is None
    assert len(str(simulator["native_source_sha256"])) == 64
    assert simulator["content_scope_id"]
