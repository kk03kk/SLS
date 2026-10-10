import json
import subprocess
import sys
import zipfile

import pytest

from tools.capture_green_key_sources import CLASSES, main, sha


def setup_arguments(tmp_path, monkeypatch, expected=None):
    jar = tmp_path / 'stock.jar'
    with zipfile.ZipFile(jar, 'x') as archive:
        for name in CLASSES:
            archive.writestr('com/megacrit/cardcrawl/' + name.replace('.', '/') + '.class', name)
    javap = tmp_path / 'javap.exe'
    javap.write_bytes(b'test-tool')
    output = tmp_path / 'evidence'
    monkeypatch.setattr(sys, 'argv', ['capture', '--stock-jar', str(jar), '--stock-sha256',
                                     expected or sha(jar.read_bytes()), '--javap', str(javap),
                                     '--output', str(output)])
    return output


def test_wrong_stock_version_has_no_output_or_subprocess(tmp_path, monkeypatch):
    output = setup_arguments(tmp_path, monkeypatch, '0' * 64)
    with pytest.raises(ValueError, match='pinned input'):
        main()
    assert not output.exists()


def test_capture_binds_all_classes_and_refuses_overwrite(tmp_path, monkeypatch):
    output = setup_arguments(tmp_path, monkeypatch)
    commands = []

    def run(command, **kwargs):
        assert kwargs == {'check': True, 'capture_output': True}
        commands.append(command)
        return subprocess.CompletedProcess(command, 0, b'example\r\n', b'')

    monkeypatch.setattr(subprocess, 'run', run)
    main()
    manifest = json.loads((output / 'manifest.json').read_text())
    assert len(manifest['classes']) == len(commands) == 6
    assert manifest['scope'] == 'STATIC_BYTECODE_ONLY_NOT_RUNTIME_QUALIFICATION'
    assert all(row['class_sha256'] == sha(name.encode())
               for row, name in zip(manifest['classes'], CLASSES))
    assert all(row['disassembly_sha256'] == sha(b'example\n') for row in manifest['classes'])
    original = (output / 'manifest.json').read_bytes()
    with pytest.raises(FileExistsError):
        main()
    assert len(commands) == 6
    assert (output / 'manifest.json').read_bytes() == original
