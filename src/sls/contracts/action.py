"""Canonical semantic actions shared by every SLS backend and policy.

Candidate list positions are deliberately not part of action identity.  A backend
may order legal candidates however it likes; policies score the candidates that
are present at the current decision boundary.
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass
from enum import Enum
from typing import Any, Mapping


ACTION_SCHEMA_VERSION = 1


class ActionKind(str, Enum):
    CHOOSE_NEOW_OPTION = "CHOOSE_NEOW_OPTION"
    PLAY_CARD = "PLAY_CARD"
    END_TURN = "END_TURN"
    USE_POTION = "USE_POTION"
    DISCARD_POTION = "DISCARD_POTION"
    CHOOSE_MAP_NODE = "CHOOSE_MAP_NODE"
    CHOOSE_CARD_REWARD = "CHOOSE_CARD_REWARD"
    TAKE_SINGING_BOWL = "TAKE_SINGING_BOWL"
    SKIP_CARD_REWARD = "SKIP_CARD_REWARD"
    TAKE_REWARD = "TAKE_REWARD"
    SKIP_REWARD = "SKIP_REWARD"
    BUY_CARD = "BUY_CARD"
    BUY_RELIC = "BUY_RELIC"
    BUY_POTION = "BUY_POTION"
    REMOVE_CARD = "REMOVE_CARD"
    LEAVE_SHOP = "LEAVE_SHOP"
    CHOOSE_EVENT_OPTION = "CHOOSE_EVENT_OPTION"
    REST = "REST"
    UPGRADE_CARD = "UPGRADE_CARD"
    LIFT = "LIFT"
    DIG = "DIG"
    RECALL = "RECALL"
    OPEN_CHEST = "OPEN_CHEST"
    TAKE_BLUE_KEY = "TAKE_BLUE_KEY"
    CHOOSE_BOSS_RELIC = "CHOOSE_BOSS_RELIC"
    SELECT_CARD = "SELECT_CARD"
    CONFIRM = "CONFIRM"
    CANCEL = "CANCEL"
    PROCEED = "PROCEED"


_REQUIRED_FIELDS: dict[ActionKind, tuple[str, ...]] = {
    ActionKind.CHOOSE_NEOW_OPTION: ("option_id",),
    ActionKind.PLAY_CARD: ("subject_id",),
    ActionKind.USE_POTION: ("subject_id",),
    ActionKind.DISCARD_POTION: ("subject_id",),
    ActionKind.CHOOSE_MAP_NODE: ("node_id",),
    ActionKind.CHOOSE_CARD_REWARD: ("subject_id",),
    ActionKind.TAKE_SINGING_BOWL: ("option_id",),
    ActionKind.SKIP_CARD_REWARD: ("option_id",),
    ActionKind.TAKE_REWARD: ("reward_id",),
    ActionKind.BUY_CARD: ("subject_id",),
    ActionKind.BUY_RELIC: ("subject_id",),
    ActionKind.BUY_POTION: ("subject_id",),
    ActionKind.REMOVE_CARD: ("subject_id",),
    ActionKind.CHOOSE_EVENT_OPTION: ("option_id",),
    ActionKind.UPGRADE_CARD: ("subject_id",),
    ActionKind.CHOOSE_BOSS_RELIC: ("subject_id",),
    ActionKind.TAKE_BLUE_KEY: ("reward_id",),
    ActionKind.SELECT_CARD: ("subject_id",),
}


@dataclass(frozen=True, slots=True)
class Action:
    """One legal semantic candidate.

    ``subject_id`` is an observation-scoped object identity (card instance,
    potion slot, shop item, relic option, ...).  Content IDs alone are not used
    when two distinct instances can coexist.  ``metadata`` is public descriptive
    input for an action encoder and may never contain backend commands or RNG.
    """

    kind: ActionKind
    subject_id: str | None = None
    target_id: str | None = None
    option_id: str | None = None
    node_id: str | None = None
    reward_id: str | None = None
    metadata: tuple[tuple[str, int | float | bool | str], ...] = ()
    schema_version: int = ACTION_SCHEMA_VERSION

    def __post_init__(self) -> None:
        if self.schema_version != ACTION_SCHEMA_VERSION:
            raise ValueError(f"unsupported action schema {self.schema_version}")
        for field_name in _REQUIRED_FIELDS.get(self.kind, ()):
            if not getattr(self, field_name):
                raise ValueError(f"{self.kind.value} requires {field_name}")
        keys = [key for key, _ in self.metadata]
        if any(not isinstance(key, str) for key in keys):
            raise ValueError("action metadata keys must be strings")
        if keys != sorted(keys) or len(keys) != len(set(keys)):
            raise ValueError("action metadata must have unique, sorted keys")
        for key, value in self.metadata:
            if not isinstance(value, (int, float, bool, str)):
                raise ValueError(f"action metadata value must be scalar: {key}")
            if isinstance(value, float) and not math.isfinite(value):
                raise ValueError(f"action metadata value must be finite: {key}")
        _reject_private_keys(dict(self.metadata))

    @property
    def candidate_id(self) -> str:
        """Canonical action identity, independent of candidate-list position."""

        return json.dumps(self.to_dict(), sort_keys=True, separators=(",", ":"))

    def to_dict(self) -> dict[str, Any]:
        return {
            "schema_version": self.schema_version,
            "kind": self.kind.value,
            "subject_id": self.subject_id,
            "target_id": self.target_id,
            "option_id": self.option_id,
            "node_id": self.node_id,
            "reward_id": self.reward_id,
            "metadata": dict(self.metadata),
        }

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> "Action":
        metadata = value.get("metadata", {})
        if not isinstance(metadata, Mapping):
            raise ValueError("action metadata must be an object")
        return cls(
            kind=ActionKind(str(value["kind"])),
            subject_id=value.get("subject_id"),
            target_id=value.get("target_id"),
            option_id=value.get("option_id"),
            node_id=value.get("node_id"),
            reward_id=value.get("reward_id"),
            metadata=tuple(sorted(metadata.items())),
            schema_version=int(value.get("schema_version", -1)),
        )


def validate_candidate_set(actions: tuple[Action, ...]) -> None:
    identities = [action.candidate_id for action in actions]
    if len(identities) != len(set(identities)):
        raise ValueError("legal action candidates must be semantically unique")


def _reject_private_keys(value: Mapping[str, Any]) -> None:
    forbidden = {"seed", "rng", "command", "bits", "backend_action", "internal"}
    for key in value:
        normalized = str(key).lower().lstrip("_")
        if normalized in forbidden or normalized.startswith("rng_"):
            raise ValueError(f"private action metadata is forbidden: {key}")
