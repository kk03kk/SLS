"""Launch preparation locates only a hash-verified parent, preserving other files."""

import hashlib

import pytest

from tools.launch_act12_v6 import locate_parent


def configuration(payload=b"frozen-parent"):
    return {"warm_start": {"checkpoint": "local/runs/parent90m/final.pt",
                           "checkpoint_sha256": hashlib.sha256(payload).hexdigest()}}


def test_launch_finds_matching_lineage_without_overwriting_other_files(tmp_path):
    directory = tmp_path / "local/runs/previous90m"
    directory.mkdir(parents=True)
    (directory / "wrong.pt").write_bytes(b"wrong")
    (directory / "latest.pt").write_bytes(b"frozen-parent")
    target = locate_parent(configuration(), root=tmp_path)
    assert target.read_bytes() == b"frozen-parent"
    assert (directory / "wrong.pt").read_bytes() == b"wrong"
    assert locate_parent(configuration(), root=tmp_path) == target


def test_launch_preserves_mismatching_existing_parent(tmp_path):
    target = tmp_path / configuration()["warm_start"]["checkpoint"]
    target.parent.mkdir(parents=True)
    target.write_bytes(b"other-model")
    with pytest.raises(ValueError, match="preserved"):
        locate_parent(configuration(), root=tmp_path)
    assert target.read_bytes() == b"other-model"


def test_launch_missing_parent_is_explicit(tmp_path):
    with pytest.raises(FileNotFoundError, match="No job has been submitted"):
        locate_parent(configuration(), root=tmp_path)
