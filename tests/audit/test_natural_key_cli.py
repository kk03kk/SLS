"""Reject unsafe evidence replacement and invalid budgets before game access."""
import sys

import pytest

from tools import capture_natural_key_flow, replay_natural_key_flow


def no_session():
    raise AssertionError('game session must not be opened')


@pytest.mark.parametrize('limit',['0','257'])
def test_invalid_budget_rejected_before_session_or_output_creation(monkeypatch,tmp_path,limit):
    output = tmp_path/'new.json'
    monkeypatch.setattr(sys,'argv',['capture','--seed','1','--output',str(output),'--max-actions',limit])
    monkeypatch.setattr(capture_natural_key_flow,'OriginalSession',no_session)
    with pytest.raises(ValueError,match='diagnostic limit'):
        capture_natural_key_flow.main()
    assert not output.exists()


def test_existing_capture_cannot_be_replaced_or_start_game(monkeypatch,tmp_path):
    output = tmp_path/'history.json'
    output.write_text('preserved')
    monkeypatch.setattr(sys,'argv',['capture','--seed','1','--output',str(output)])
    monkeypatch.setattr(capture_natural_key_flow,'OriginalSession',no_session)
    with pytest.raises(FileExistsError,match='overwrite'):
        capture_natural_key_flow.main()
    assert output.read_text() == 'preserved'


def test_existing_replay_is_preserved_before_capture_or_oracle_read(monkeypatch,tmp_path):
    output = tmp_path/'replay.json'
    output.write_text('preserved')
    monkeypatch.setattr(sys,'argv',['replay','--capture','absent','--oracle','absent','--output',str(output)])
    with pytest.raises(FileExistsError,match='overwrite'):
        replay_natural_key_flow.main()
    assert output.read_text() == 'preserved'


def test_cuda_device_is_rejected_before_session_or_output(monkeypatch,tmp_path):
    output = tmp_path/'never-created.json'
    monkeypatch.setattr(sys,'argv',['capture','--seed','1','--output',str(output),'--device','cuda'])
    monkeypatch.setattr(capture_natural_key_flow,'OriginalSession',no_session)
    with pytest.raises(SystemExit) as error:
        capture_natural_key_flow.main()
    assert error.value.code == 2 and not output.exists()
