"""Portable evidence reconstructs original checkpoints, never resamples entries."""

import gzip
import hashlib
import json
from pathlib import Path

import pytest

from sls.rl.training_contract import native_source_digest
from tools.build_act2_entry_bank import encode_bank, restore_prefix_fixture


@pytest.mark.parametrize("corrupt", [False, True])
def test_portable_entry_prefix_checkpoint_identity(tmp_path, corrupt):
    path = Path(__file__).resolve().parents[1] / "fixtures/evidence/act12-foundation/entry-prefixes.json.gz"
    fixture = json.loads(gzip.decompress(path.read_bytes()))
    assert len(fixture["entries"]) == 1493
    row = fixture["entries"][0]
    if corrupt:
        row["checkpoint_sha256"] = "0" * 64
    fixture["entries"] = [row]
    fixture["bank_metadata"]["outcomes"]["reached_entry"] = 1
    sample = tmp_path / "sample.json.gz"
    sample.write_bytes(gzip.compress(json.dumps(fixture).encode()))
    if fixture["bank_metadata"]["native_source_sha256"] != native_source_digest():
        # Historical prefixes retain their original source identity. A later
        # rule audit must reject them, not rewrite their hashes or resume gate.
        with pytest.raises(ValueError, match="stale entry-prefix fixture"):
            restore_prefix_fixture(sample)
        return
    if corrupt:
        with pytest.raises(ValueError, match="reconstruction differs"):
            restore_prefix_fixture(sample)
    else:
        bank, expected_sha = restore_prefix_fixture(sample)
        encoded = json.dumps(bank["entries"][0]["checkpoint"], sort_keys=True, separators=(",", ":")).encode()
        assert hashlib.sha256(encoded).hexdigest() == row["checkpoint_sha256"]
        assert expected_sha == fixture["bank_sha256"]


def test_bank_encoding_preserves_original_platform_marker(monkeypatch):
    original = gzip.compress
    unix_header = bytes.fromhex("1f8b0800000000000203")
    windows_header = bytes.fromhex("1f8b080000000000020a")

    def linux_compress(*args, **kwargs):
        raw = original(*args, **kwargs)
        return unix_header + raw[10:]

    monkeypatch.setattr(gzip, "compress", linux_compress)
    payload = {"fixture": "identical content"}
    encoded = encode_bank(payload, original_header=windows_header)
    assert encoded[:10] == windows_header
    assert gzip.decompress(encoded) == json.dumps(payload, sort_keys=True, separators=(",", ":")).encode()
    expected = original(json.dumps(payload, sort_keys=True, separators=(",", ":")).encode(), mtime=0)
    assert encoded == windows_header + expected[10:]
