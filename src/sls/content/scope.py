"""Versioned policy-visible content scopes.

The native simulator keeps the complete base-game registries and pools so its
RNG remains comparable with stock.  A content scope controls only what a
course may expose to a policy; it must never be used to delete internal pool
entries.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable, Mapping, TypeVar

CONTENT_SCOPE_SCHEMA = "sls-content-scope-v1"
IRONCLAD_A0_SCOPE_ID = "sls-ironclad-a0-fullrun-content-v2"
IRONCLAD_A0_SCOPE_PATH = Path(__file__).with_name("scope.json")


def canonical_scope_digest(value: Mapping[str, Any]) -> str:
    unsigned = dict(value)
    unsigned.pop("scope_sha256", None)
    encoded = json.dumps(
        unsigned, sort_keys=True, separators=(",", ":"), ensure_ascii=False,
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def load_ironclad_a0_scope() -> dict[str, Any]:
    payload = json.loads(IRONCLAD_A0_SCOPE_PATH.read_text(encoding="utf-8"))
    if payload.get("schema") != CONTENT_SCOPE_SCHEMA:
        raise ValueError("unsupported content scope schema")
    if payload.get("scope_id") != IRONCLAD_A0_SCOPE_ID:
        raise ValueError("unexpected Ironclad A0 content scope")
    if payload.get("scope_sha256") != canonical_scope_digest(payload):
        raise ValueError("Ironclad A0 content scope digest mismatch")
    return payload


def ironclad_a0_scope_hash() -> str:
    return str(load_ironclad_a0_scope()["scope_sha256"])


def validate_scope_source_hashes(root: Path | None = None) -> None:
    """Prove the committed scope was derived from the current source inputs."""

    repository = root or Path(__file__).resolve().parents[3]
    expected = load_ironclad_a0_scope().get("source_sha256")
    if not isinstance(expected, Mapping) or not expected:
        raise ValueError("Ironclad A0 content scope has no source provenance")
    mismatches: list[str] = []
    for relative, claimed in sorted(expected.items()):
        path = repository / str(relative)
        if not path.is_file():
            mismatches.append(f"{relative}=MISSING")
            continue
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != claimed:
            mismatches.append(f"{relative}={actual}")
    if mismatches:
        raise ValueError(
            "Ironclad A0 scope source provenance mismatch: " + ", ".join(mismatches)
        )


def policy_excluded_content_ids() -> frozenset[str]:
    return frozenset(map(
        str, load_ironclad_a0_scope().get("policy_excluded_content_ids", ()),
    ))


def policy_content_is_visible(content_id: str) -> bool:
    return str(content_id) not in policy_excluded_content_ids()


@dataclass(frozen=True, slots=True)
class UnsupportedContentPolicy:
    """Single policy boundary for content intentionally outside training.

    Excluded content stays in stock/native internal pools when required for RNG
    fidelity, but it may not be offered to a policy.  Attaching after excluded
    content was already acquired is rejected because the resulting observation
    and reward pools are outside the supported MDP.
    """

    excluded_content_ids: frozenset[str]

    @classmethod
    def ironclad(cls) -> "UnsupportedContentPolicy":
        return cls(policy_excluded_content_ids())

    def supports(self, content_id: str) -> bool:
        return str(content_id) not in self.excluded_content_ids

    def validate_observation(self, observation: object) -> None:
        relics = tuple(getattr(observation, "relics", ()))
        owned = sorted({
            str(getattr(relic, "content_id"))
            for relic in relics
            if not self.supports(str(getattr(relic, "content_id")))
        })
        if owned:
            raise ValueError(
                "cannot attach to a run that already owns unsupported content: "
                + ", ".join(owned)
            )


_ActionT = TypeVar("_ActionT")
_ItemT = TypeVar("_ItemT")
_MappingT = TypeVar("_MappingT")


def filter_policy_shop(
    shop_items: Iterable[_ItemT],
    actions: Iterable[_ActionT],
    action_mapping: Mapping[str, _MappingT],
    *, policy: UnsupportedContentPolicy | None = None,
) -> tuple[tuple[_ItemT, ...], tuple[_ActionT, ...], dict[str, _MappingT]]:
    """Hide excluded shop content without changing native/raw slot identities."""

    return filter_policy_offers(
        shop_items, actions, action_mapping, policy=policy,
    )


def filter_policy_offers(
    items: Iterable[_ItemT],
    actions: Iterable[_ActionT],
    action_mapping: Mapping[str, _MappingT],
    *, policy: UnsupportedContentPolicy | None = None,
) -> tuple[tuple[_ItemT, ...], tuple[_ActionT, ...], dict[str, _MappingT]]:
    """Filter unsupported acquisitions while preserving every raw identity.

    Content remains in registries and backend RNG pools.  Only policy-visible
    offers and actions that reference those exact offer instances are hidden.
    Supplying a policy explicitly keeps this usable by future character scopes.
    """

    content_policy = policy or UnsupportedContentPolicy.ironclad()
    items = tuple(items)
    hidden_instances = {
        str(getattr(item, "instance_id"))
        for item in items
        if not content_policy.supports(str(getattr(item, "content_id")))
    }
    visible_items = tuple(
        item for item in items if str(getattr(item, "instance_id")) not in hidden_instances
    )
    visible_actions = tuple(
        action for action in actions
        if not hidden_instances.intersection({
            str(getattr(action, "subject_id", "")),
            str(getattr(action, "reward_id", "")),
            str(getattr(action, "option_id", "")),
        })
    )
    visible_candidate_ids = {
        str(getattr(action, "candidate_id")) for action in visible_actions
    }
    visible_mapping = {
        key: value for key, value in action_mapping.items()
        if key in visible_candidate_ids
    }
    return visible_items, visible_actions, visible_mapping


def filter_policy_key_acquisitions(
    items: Iterable[_ItemT],
    actions: Iterable[_ActionT],
    action_mapping: Mapping[str, _MappingT],
    *,
    allow_keys: bool,
) -> tuple[tuple[_ItemT, ...], tuple[_ActionT, ...], dict[str, _MappingT]]:
    """Hide key-only policy choices outside Heart profiles.

    This is a projection boundary only: native key flags, burning elites,
    reward construction, RNG consumption, and vocabulary entries are retained.
    """

    items = tuple(items)
    actions = tuple(actions)
    if allow_keys:
        return items, actions, dict(action_mapping)

    def key_action(action: _ActionT) -> bool:
        raw_kind = getattr(action, "kind", "")
        kind = str(getattr(raw_kind, "value", raw_kind))
        reward_id = str(getattr(action, "reward_id", "") or "")
        return kind in {"RECALL", "TAKE_BLUE_KEY"} or reward_id.startswith("reward-key:")

    visible_actions = tuple(action for action in actions if not key_action(action))
    visible_ids = {str(getattr(action, "candidate_id")) for action in visible_actions}
    visible_mapping = {
        key: value for key, value in action_mapping.items() if key in visible_ids
    }
    visible_items = tuple(
        item for item in items
        if "KEY" not in str(getattr(item, "content_id", "")).upper()
        and not str(getattr(item, "instance_id", "")).startswith("reward-key:")
    )
    return visible_items, visible_actions, visible_mapping
