"""Read recorded trajectories without importing an inference runtime."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def read_trajectory(path: str | Path) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    records = [json.loads(line) for line in Path(path).read_text(encoding='utf-8').splitlines()
               if line.strip()]
    if not records or records[0].get('record_type') != 'metadata':
        raise ValueError('trajectory metadata record is missing')
    if records[0].get('schema') != 'sls-policy-trajectory-v2':
        raise ValueError('unsupported trajectory schema')
    boundaries = records[1:]
    if any(record.get('record_type') != 'boundary' for record in boundaries):
        raise ValueError('trajectory contains a non-boundary data record')
    return records[0], boundaries
