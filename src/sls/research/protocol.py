"""Frozen experimental protocol and paired inference (standard library only)."""
from __future__ import annotations

import hashlib
import json
import math
import random
from pathlib import Path

BASE_COMMIT = "ce7713673629f45381da5b195f2fab8dde833505"
NATIVE_SHA256 = "6efbb772958c06d1b9133f374d046d8838eaa256da1a146eaf7e1ee530eefd3d"
PARENT_SHA256 = "274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0"
STRATA = ("entry", "ordinary", "elite", "boss")
RANGES = {
    "training": (6100100000000, 6100101000000),
    "bank": (6100110000000, 6100110002048),
    "periodic": (8000014000000, 8000014000512),
    "confirmation": (8000015000000, 8000015004096),
}
PROTOCOL = {
    "schema": "sls-act2-learning-protocol-v1", "base_commit": BASE_COMMIT,
    "native_sha256": NATIVE_SHA256, "parent_sha256": PARENT_SHA256,
    "updates": 128, "warmup_updates": 32, "workers": 64, "shards": 16,
    "rollout_steps": 256, "student_decisions": 2097152,
    "curriculum_episode_probability": 0.25, "ranges": RANGES,
    "minimum_seeds_per_stratum": 32, "boss_kinds": 3,
    "periodic_updates": 32, "retention_margin": -0.03,
    "stop_p": 0.01, "stop_consecutive": 2,
    "training_seeds": 1, "conclusion": "exploratory",
}


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(",", ":"),
                                     allow_nan=False).encode()).hexdigest()


def paired(reference, candidate, *, bootstrap_samples=10000, seed=20261010):
    """Exact discordant-pair test and deterministic paired percentile interval."""
    if not reference or len(reference) != len(candidate):
        raise ValueError("paired outcomes require equal nonempty seed-aligned arrays")
    if any(type(x) is not bool for x in (*reference, *candidate)):
        raise ValueError("outcomes must be boolean")
    differences = [int(b) - int(a) for a, b in zip(reference, candidate)]
    lost, gained = differences.count(-1), differences.count(1)
    discordant = lost + gained
    k = min(lost, gained)
    # Sum log probabilities to avoid overflow for thousands of discordant pairs.
    p = min(1.0, 2 * sum(math.exp(math.lgamma(discordant + 1) - math.lgamma(i + 1)
                                 - math.lgamma(discordant - i + 1)
                                 - discordant * math.log(2)) for i in range(k + 1)))
    rng = random.Random(seed)
    # Sampling the empirical three-category distribution is an exact paired bootstrap.
    n = len(differences)
    samples = sorted(sum(-1 if (r := rng.randrange(n)) < lost else
                         1 if r < discordant else 0 for _ in range(n)) / n
                     for _ in range(bootstrap_samples))
    return {"n": n, "lost": lost, "gained": gained,
            "difference": (gained - lost) / n, "exact_p_two_sided": p,
            "paired_ci_95": [samples[int(.025 * bootstrap_samples)],
                             samples[min(bootstrap_samples - 1, int(.975 * bootstrap_samples))]],
            "paired_lower_one_sided_95": samples[int(.05 * bootstrap_samples)],
            "bootstrap_samples": bootstrap_samples, "bootstrap_seed": seed}


def should_stop(history):
    return len(history) >= 2 and all(row["difference"] <= -.03 and
                                     row["exact_p_two_sided"] < .01
                                     for row in history[-2:])


def scan_registrations(roots, *, exempt=()):
    """Fail on overlapping registered ranges, seeds or evaluation manifests."""
    import tomllib

    exempt = {Path(p).resolve() for p in exempt}
    scanned = []

    def overlap(a, b, path):
        for name, (start, end) in RANGES.items():
            if max(a, start) < min(b, end):
                raise ValueError(f"seed namespace collision ({name}): {path}: [{a},{b})")

    def walk(value, path):
        if isinstance(value, dict):
            if type(value.get("seed")) is int:
                overlap(value["seed"], value["seed"] + 1, path)
            if type(value.get("start")) is int and type(value.get("end")) is int:
                overlap(value["start"], value["end"], path)
            for key, child in value.items():
                if key == "ranges" and isinstance(child, dict):
                    for span in child.values():
                        if isinstance(span, (list, tuple)) and len(span) == 2 and all(type(x) is int for x in span):
                            overlap(*span, path)
                if key.endswith("_seed_start") or key == "seed_start":
                    count = value.get(key.removesuffix("start") + "count")
                    if type(child) is int and type(count) is int:
                        overlap(child, child + count, path)
                if ("seed" in key or key == "ranges") and isinstance(child, (list, tuple)):
                    if len(child) == 2 and all(type(x) is int for x in child) and child[0] < child[1]:
                        overlap(*child, path)
                    else:
                        for seed in child:
                            if type(seed) is int:
                                overlap(seed, seed + 1, path)
                walk(child, path)
        elif isinstance(value, (list, tuple)):
            for item in value:
                walk(item, path)

    for root in roots:
        root = Path(root)
        for pattern in ("*.json", "*.toml"):
            for path in root.rglob(pattern):
                if path.resolve() in exempt:
                    continue
                text = path.read_text(encoding="utf-8")
                value = tomllib.loads(text) if path.suffix == ".toml" else json.loads(text)
                walk(value, path)
                scanned.append(str(path))
    return sorted(set(scanned))
