import hashlib
import json

import pytest

from tools.capture_original_card_batch import _write_completion
from tools.verify_oracle import (
    capture_command_property,
    read_log_tail,
    read_verified_launch,
    review_completed_capture,
)


def test_completion_writer_publishes_complete_json_atomically(tmp_path, monkeypatch):
    path = tmp_path / "completion.json"
    monkeypatch.setenv("SLS_RUN_COMPLETION", str(path))
    _write_completion(0)
    assert json.loads(path.read_text()) == {"exit_code": 0, "error": None}
    assert not path.with_suffix(".tmp").exists()


def test_java_properties_preserves_windows_capture_paths():
    assert capture_command_property([r"D:\Anaconda\envs\DL\python.exe", r"D:\SLS\capture.py"]) == (
        r"D\:\\Anaconda\\envs\\DL\\python.exe D\:\\SLS\\capture.py")
    assert capture_command_property(["D:/Anaconda/python.exe"]) == r"D\:/Anaconda/python.exe"


def race_capture(tmp_path, *, terminal=True):
    def write(name, value):
        path = tmp_path / name
        path.write_text(json.dumps(value))
        return path
    raw = tmp_path / "raw.jsonl"
    raw.write_text(json.dumps({"terminal": terminal, "requested_action": None}) + "\n")
    journal = write("journal.json", {"status": "RECOVERED", "recovery_failures": []})
    write("completion.json", {"exit_code": 0, "error": None})
    (tmp_path / "stdout.log").write_text("Child process has died...")
    (tmp_path / "stderr.log").write_text("")
    capture = write("capture.json", {"execution_complete": True, "runs": [{
        "stock_capture": str(raw), "sha256": hashlib.sha256(raw.read_bytes()).hexdigest(),
        "terminal": terminal, "script_action_unavailable": None, "boundaries": 1}]})
    write("capture.launch.json", {"completion": None, "recovery_journal": str(journal),
                                   "execution_error": "CommunicationMod child exited without completion; logs: test",
                                   "recovery_status": "RECOVERED"})
    return capture


def test_review_preserves_failure_and_requires_unchanged_terminal_raw_evidence(tmp_path):
    capture = race_capture(tmp_path)
    original = capture.with_suffix(".launch.json").read_bytes()
    review_completed_capture(capture)
    verified = read_verified_launch(capture.with_suffix(".launch.json"))
    assert verified["completion"]["exit_code"] == 0
    assert verified["original_execution_error"]
    assert capture.with_suffix(".launch.json").read_bytes() == original
    (tmp_path / "raw.jsonl").write_text("altered")
    with pytest.raises(ValueError, match="invalid"):
        read_verified_launch(capture.with_suffix(".launch.json"))


def test_zero_completion_does_not_certify_partial_capture(tmp_path):
    capture = race_capture(tmp_path, terminal=False)
    with pytest.raises(ValueError, match="incomplete"):
        review_completed_capture(capture)


def test_large_sparse_log_is_read_with_a_bounded_tail(tmp_path, monkeypatch):
    path = tmp_path / 'large.log'
    with path.open('wb') as stream:
        stream.seek(200 * 1024 * 1024)
        stream.write(b'Child process has died...')
    monkeypatch.setattr(type(path), 'read_bytes', lambda _: pytest.fail('must not read the entire log'))
    result = read_log_tail(path, 1024)
    assert len(result) == 1024
    assert result.endswith(b'Child process has died...')
