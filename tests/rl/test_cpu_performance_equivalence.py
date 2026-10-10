"""Golden release canonical bytes: Unicode, duplicate lists and mutable values."""
import hashlib
import json
import random
from collections.abc import Mapping
from dataclasses import dataclass

import pytest

from sls.contracts.observation import _assert_public_tree, _json_value
from sls.rl.episode_limit import _canonical, policy_boundary_fingerprint


def release_canonical(value):
    if isinstance(value, Mapping):
        return {str(k): release_canonical(v) for k, v in sorted(value.items())}
    if isinstance(value, (tuple, list)):
        items = [release_canonical(v) for v in value]
        return sorted(items, key=lambda v: json.dumps(v, sort_keys=True, separators=(",", ":")))
    return value


def test_canonical_bytes_match_release_for_adversarial_public_trees():
    rng = random.Random(34819)
    scalars = [None, True, False, 1, -22, 1.5, -0.0, "", "黄", "é", "\\\"\n", "😀"]

    def tree(depth):
        if depth == 0 or rng.random() < .4:
            return rng.choice(scalars)
        if rng.random() < .5:
            return [tree(depth - 1) for _ in range(rng.randrange(5))]
        return {key: tree(depth - 1) for key in rng.sample(["a", "黄", "é", "z"], rng.randrange(5))}

    for value in [tree(5) for _ in range(300)] + [{1: "b", "1": "a"}]:
        # Mixed keys must preserve the release's rejection, rather than being silently normalized.
        try:
            expected = release_canonical(value)
        except TypeError:
            with pytest.raises(TypeError):
                _canonical(value)
            continue
        expected_bytes = json.dumps(expected, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()
        actual_bytes = json.dumps(_canonical(value), sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()
        assert actual_bytes == expected_bytes


def test_observation_metadata_cache_never_caches_values_or_validation():
    @dataclass
    class Value:
        field: list

    value = Value([1])
    assert _json_value(value) == {"field": [1]}
    value.field.append(2)
    assert _json_value(value) == {"field": [1, 2]}
    for name in ("seed", "_RNG", "rng_future", "simulator_state"):
        with pytest.raises(ValueError, match="hidden"):
            _assert_public_tree({"fresh": [{name: 1}]})


def test_fingerprint_matches_exact_release_payload_after_live_changes():
    from dataclasses import replace

    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import IRONCLAD_A20_ACT2

    decision = SimulatorBackend(IRONCLAD_A20_ACT2).reset(132100700)
    for gold in (99, 100, 101):
        decision = replace(decision, observation=replace(decision.observation,
                           run=replace(decision.observation.run, gold=gold)))
        payload = {"observation": release_canonical(decision.observation.to_dict()),
                   "actions": release_canonical([a.to_dict() for a in decision.actions])}
        expected = hashlib.sha256(json.dumps(payload, sort_keys=True, separators=(",", ":"),
                                               ensure_ascii=False).encode()).hexdigest()
        assert policy_boundary_fingerprint(decision) == expected
