"""Trajectory readers must work without model inference dependencies."""

import json

import pytest

from sls.audit.trajectory_reader import read_trajectory


@pytest.mark.parametrize('records', [[], [{'record_type': 'boundary'}],
                                    [{'record_type': 'metadata', 'schema': 'unknown'}],
                                    [{'record_type': 'metadata', 'schema': 'sls-policy-trajectory-v2'},
                                     {'record_type': 'unexpected'}]])
def test_reject_invalid_trajectory(tmp_path, records):
    path = tmp_path / 'trace.jsonl'
    path.write_text('\n'.join(json.dumps(r) for r in records))
    with pytest.raises(ValueError):
        read_trajectory(path)


def test_preserve_metadata_and_boundaries(tmp_path):
    metadata = {'record_type': 'metadata', 'schema': 'sls-policy-trajectory-v2', 'seed': 123}
    boundary = {'record_type': 'boundary', 'index': 0, 'payload': {'hp': 42}}
    path = tmp_path / 'trace.jsonl'
    path.write_text(json.dumps(metadata) + '\n\n' + json.dumps(boundary) + '\n')
    assert read_trajectory(path) == (metadata, [boundary])
