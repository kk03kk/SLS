"""Offline stock clock evidence checks; no simulator or model imports."""

from __future__ import annotations

import hashlib
import re
import zipfile
from pathlib import Path

MARKER = 'SLS_DISCOVERY_CLOCK_V1'
PATTERN = re.compile(
    r'SLS_DISCOVERY_CLOCK_V1 seed=(\d+) floor=(\d+) serial=(\d+) updates=(\d+)'
)


def stock_clock_rows(payload: str, seed: int) -> list[dict]:
    rows = []
    for line in payload.splitlines():
        if MARKER not in line:
            continue
        # Allow logger prefixes, but never silently discard truncated/invalid witnesses.
        match = PATTERN.fullmatch(line[line.index(MARKER):].strip())
        if match is None:
            raise ValueError('malformed Discovery clock witness')
        rows.append(dict(zip(('seed', 'floor', 'serial', 'updates'), map(int, match.groups()))))
    serials = [row['serial'] for row in rows]
    if len(set(serials)) != len(serials):
        raise ValueError('duplicate Discovery completion serial')
    if any(serial <= 0 for serial in serials) or serials != sorted(serials):
        raise ValueError('out-of-order Discovery completion serial')
    if any(not 1 <= row['updates'] <= 120 for row in rows):
        raise ValueError('invalid Discovery clock count')
    return [row for row in rows if row['seed'] == seed]


def verify_sealed_oracle(oracle: Path, build: dict) -> None:
    """Historical identity only; does not certify current source or game behavior."""
    if (build.get('schema') != 'sls-oracle-build-v1'
            or build.get('used_existing_oracle') is not False
            or not build.get('sources')
            or hashlib.sha256(oracle.read_bytes()).hexdigest() != build.get('output_sha256')):
        raise ValueError('sealed Oracle build identity failure')
    with zipfile.ZipFile(oracle) as archive:
        if (len(archive.namelist()) != len(set(archive.namelist()))
                or set(archive.namelist()) != set(build.get('members', {}))
                or any(hashlib.sha256(archive.read(name)).hexdigest() != digest
                       for name, digest in build['members'].items())):
            raise ValueError('sealed Oracle member identity failure')
