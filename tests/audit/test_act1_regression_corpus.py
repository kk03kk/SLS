from __future__ import annotations

import json
from types import SimpleNamespace

import pytest

from sls.rl.training_contract import sha256_file
from tools import replay_act1_corpus


def _case(tmp_path, monkeypatch):
    monkeypatch.setattr(replay_act1_corpus, "ROOT", tmp_path)
    stock = tmp_path / "stock.jar"
    stock.write_bytes(b"pinned stock")
    original = tmp_path / "original.jsonl"
    records = [
        {"record_type": "metadata", "schema": "sls-policy-trajectory-v2",
         "backend": "original", "seed": 42},
        {"record_type": "boundary", "terminal": True},
    ]
    original.write_text("\n".join(map(json.dumps, records)) + "\n", encoding="utf-8")
    corpus = {
        "schema": "sls-act1-regression-corpus-v1", "profile_id": "IRONCLAD_A20_ACT1",
        "stock_jar_sha256": sha256_file(stock), "model_sha256": "weights",
        "cases": [{"seed": 42, "original_path": original.name,
                   "original_sha256": sha256_file(original)}],
    }
    artifact = SimpleNamespace(metadata=SimpleNamespace(
        model_sha256="weights", environment_profile={"profile_id": "IRONCLAD_A20_ACT1"},
    ))
    return corpus, stock, artifact, original


def test_corpus_rejects_changed_stock_evidence(tmp_path, monkeypatch):
    corpus, stock, artifact, original = _case(tmp_path, monkeypatch)
    replay_act1_corpus.validate_corpus(corpus, stock, artifact)
    original.write_text(original.read_text() + "\n", encoding="utf-8")
    with pytest.raises(ValueError, match="trajectory hash mismatch"):
        replay_act1_corpus.validate_corpus(corpus, stock, artifact)


def test_corpus_cannot_count_one_route_twice(tmp_path, monkeypatch):
    corpus, stock, artifact, _ = _case(tmp_path, monkeypatch)
    corpus["cases"].append(dict(corpus["cases"][0]))
    with pytest.raises(ValueError, match="distinct seeds and evidence"):
        replay_act1_corpus.validate_corpus(corpus, stock, artifact)


def test_corpus_rejects_wrong_policy_and_wrong_jar(tmp_path, monkeypatch):
    corpus, stock, artifact, _ = _case(tmp_path, monkeypatch)
    artifact.metadata.model_sha256 = "different"
    with pytest.raises(ValueError, match="policy weights differ"):
        replay_act1_corpus.validate_corpus(corpus, stock, artifact)
    stock.write_bytes(b"different jar")
    with pytest.raises(ValueError, match="stock JAR differs"):
        replay_act1_corpus.validate_corpus(corpus, stock, artifact)
