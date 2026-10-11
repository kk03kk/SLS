import json

import pytest

from tools.qualify_act12_foundation import (
    CHECK_FILES,
    validation_manifest,
    validation_matches,
)


def test_check_receipt_rejects_changed_output_and_source(tmp_path, monkeypatch):
    for name in CHECK_FILES:
        (tmp_path / name).write_text("passed")
    monkeypatch.setattr("tools.qualify_act12_foundation.native_source_digest", lambda: "native")
    monkeypatch.setattr("tools.qualify_act12_foundation.training_validation_digest", lambda: "training")
    monkeypatch.setattr("tools.qualify_act12_foundation.local_source_digest", lambda _: "checks")
    receipt = validation_manifest(tmp_path)
    (tmp_path / "validation-start.json").write_text(json.dumps({
        k: receipt[k] for k in ("native_source_sha256", "training_validation_sha256", "verification_source_sha256")}))
    assert validation_matches(tmp_path, receipt)
    (tmp_path / "pytest.xml").write_text("changed")
    assert not validation_matches(tmp_path, receipt)
    (tmp_path / "pytest.xml").write_text("passed")
    monkeypatch.setattr("tools.qualify_act12_foundation.training_validation_digest", lambda: "new")
    assert not validation_matches(tmp_path, receipt)


@pytest.mark.parametrize("name", CHECK_FILES)
def test_check_receipt_requires_every_check(tmp_path, name):
    for other in CHECK_FILES:
        if other != name:
            (tmp_path / other).write_text("passed")
    assert not validation_matches(tmp_path, {})
