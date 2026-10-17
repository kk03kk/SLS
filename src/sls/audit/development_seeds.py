"""Collision-checked development allocations kept separate from final evaluation."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

DEVELOPMENT_START = 8_000_000_000_000
DEVELOPMENT_END = 9_000_000_000_000


def seed_intervals(value):
    """Conservatively interpret known seed lists/ranges and start/count fields.

    Unknown count fields reserve one million seeds rather than just the first
    seed. This may refuse a usable allocation, but cannot silently reuse it.
    """
    if isinstance(value, dict):
        closed_probe = value.get('schema') in {
            'sls-frozen-parent-cpu-probe-v1',
            'sls-frozen-parent-complete-return-probe-v1',
        }
        for key, child in value.items():
            if "seed" in key.lower():
                if type(child) is int and (key == "seed" or key.endswith("_seed")):
                    if (closed_probe and key == 'next_seed' and type(value.get('seed_start')) is int
                            and child > value['seed_start']):
                        continue  # exclusive progress endpoint, not a sampled seed
                    yield child, child + 1
                elif type(child) is int and key.endswith("seed_start"):
                    prefix = key.removesuffix("seed_start")
                    counts = [value.get(prefix + suffix) for suffix in ("seed_count", "episodes", "seeds")]
                    count = next((n for n in counts if type(n) is int and n > 0), None)
                    # This completed probe records the exclusive next unused
                    # seed. A resumable run's progress is not a reservation. A
                    # declared reservation still takes priority over progress;
                    # without either, retain the conservative million fallback.
                    if count is None and closed_probe and key == 'seed_start':
                        following = value.get('next_seed')
                        if type(following) is int and following > child:
                            count = following - child
                    if count is None:
                        count = 1_000_000
                    yield child, child + count
                elif isinstance(child, list) and all(type(n) is int for n in child):
                    if len(child) == 2 and ("range" in key or "reserved" in key or "seeds" in key):
                        yield min(child), max(child) + (0 if "range" in key or "reserved" in key else 1)
                    else:
                        yield from ((n, n + 1) for n in child)
            yield from seed_intervals(child)
    elif isinstance(value, list):
        for child in value:
            yield from seed_intervals(child)


def register(root: Path, start: int, count: int, paths: list[Path]) -> dict:
    end = start + count
    if count <= 0 or start < DEVELOPMENT_START or end > DEVELOPMENT_END:
        raise ValueError("allocation must stay inside development namespace [8e12,9e12)")
    inputs = []
    for path in sorted(set(paths)):
        payload = path.read_bytes()
        data = json.loads(payload.decode("utf-8-sig"))
        for low, high in seed_intervals(data):
            if low < end and start < high:
                raise ValueError(f"development seed collision with {path}: [{low},{high})")
        inputs.append({"path": str(path.resolve()), "sha256": hashlib.sha256(payload).hexdigest()})
    return {"schema": "sls-development-seed-allocation-v1", "purpose": "A20_SIMULATOR_INVARIANTS",
            "profile": "IRONCLAD_A20_HEART", "seed_range": [start, end], "count": count,
            "development_namespace": [DEVELOPMENT_START, DEVELOPMENT_END],
            "training_or_final_evaluation": False, "scanned_inputs": inputs,
            "workspace": str(root.resolve())}
