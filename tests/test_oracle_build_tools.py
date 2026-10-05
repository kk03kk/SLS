import hashlib
import json
import zipfile
from pathlib import Path

import pytest

from tools.build_full_audit_oracle import _MEMBERS, build


def fixture_files(tmp_path: Path, *, missing: bool = False):
    source, registry = tmp_path / 'source.jar', tmp_path / 'registry.json'
    registry.write_text(json.dumps({'categories': {
        category: [{'id': category.upper(), 'game_id': f'game-{category}'}]
        for category in _MEMBERS
    }}), encoding='utf-8')
    with zipfile.ZipFile(source, 'w') as archive:
        for category, member in _MEMBERS.items():
            if not (missing and category == 'potions'):
                archive.writestr(member, b'old\n')
        archive.writestr('example/Retained.class', b'unchanged-class')
    return source, registry


def test_oracle_expansion_preserves_source_and_unrelated_members(tmp_path):
    source, registry = fixture_files(tmp_path)
    original = source.read_bytes()
    output = tmp_path / 'expanded.jar'
    report = build(source, registry, output)
    assert source.read_bytes() == original
    assert report['source_sha256'] == hashlib.sha256(original).hexdigest()
    assert report['output_sha256'] == hashlib.sha256(output.read_bytes()).hexdigest()
    with zipfile.ZipFile(output) as archive:
        assert archive.read('example/Retained.class') == b'unchanged-class'
        for category, member in _MEMBERS.items():
            assert archive.read(member) == f'{category.upper()}\tgame-{category}\n'.encode()


def test_oracle_rejects_same_resolved_output_without_touching_source(tmp_path):
    source, registry = fixture_files(tmp_path)
    before = source.read_bytes()
    with pytest.raises(ValueError, match='separate'):
        build(source, registry, source.parent / '.' / source.name)
    assert source.read_bytes() == before


def test_oracle_missing_resource_preserves_existing_output(tmp_path):
    source, registry = fixture_files(tmp_path, missing=True)
    output = tmp_path / 'existing.jar'
    output.write_bytes(b'preserve-evidence')
    with pytest.raises(ValueError, match='missing allowlist'):
        build(source, registry, output)
    assert output.read_bytes() == b'preserve-evidence'
