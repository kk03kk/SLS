import copy
import json
from pathlib import Path

import pytest

from sls.rl.normalstart_release import RELEASE, release_bindings, validate_qualification
from sls.rl.training_contract import sha256_file

ROOT = Path(__file__).resolve().parents[2]


def test_portable_release_is_source_bound_and_weights_only():
    bindings = release_bindings(ROOT)
    assert bindings["qualification"]["path"] == RELEASE + "/qualification.json"
    value = json.loads((ROOT / bindings["transition"]["evidence"]).read_text(encoding="utf-8"))
    assert value["transfer_mode"] == "weights-only" and not value["exact_resume_allowed"]


@pytest.mark.parametrize("field,value", [
    ("native_source_sha256", "0" * 64), ("ascension", 0), ("profile", "IRONCLAD_A0_ACT2"),
    ("full_803_scope_qualification", True), ("gate_source_sha256", "0" * 64),
])
def test_reject_stale_or_widened_portable_qualification(tmp_path, field, value):
    report = json.loads((ROOT / RELEASE / "qualification.json").read_text(encoding="utf-8"))
    report[field] = value
    path = tmp_path / "qualification.json"
    path.write_text(json.dumps(report), encoding="utf-8")
    with pytest.raises(ValueError):
        validate_qualification(path, sha256_file(path))


def test_reject_condition_explanation_erasure(tmp_path):
    report = json.loads((ROOT / RELEASE / "qualification.json").read_text(encoding="utf-8"))
    report = copy.deepcopy(report)
    report["facts"]["contract_explanations"] = []
    path = tmp_path / "qualification.json"
    path.write_text(json.dumps(report), encoding="utf-8")
    with pytest.raises(ValueError):
        validate_qualification(path, sha256_file(path))
