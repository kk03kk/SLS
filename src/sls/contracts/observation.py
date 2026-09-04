"""Canonical Original-visible FullRun observation contract."""

from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass, field, fields, is_dataclass
from enum import Enum
from typing import Any

OBSERVATION_SCHEMA_VERSION = 1


class ScreenType(str, Enum):
    NEOW = "NEOW"
    MAP = "MAP"
    COMBAT = "COMBAT"
    CARD_REWARD = "CARD_REWARD"
    COMBAT_REWARD = "COMBAT_REWARD"
    EVENT = "EVENT"
    SHOP = "SHOP"
    TREASURE = "TREASURE"
    REST = "REST"
    BOSS_REWARD = "BOSS_REWARD"
    ACT_TRANSITION = "ACT_TRANSITION"
    GAME_OVER = "GAME_OVER"


PublicScalar = int | float | bool | str


@dataclass(frozen=True, slots=True)
class Player:
    character_id: str
    current_hp: int
    max_hp: int
    block: int
    energy: int
    max_energy: int


@dataclass(frozen=True, slots=True)
class RunContext:
    ascension: int
    act: int
    floor: int
    gold: int
    has_ruby_key: bool
    has_emerald_key: bool
    has_sapphire_key: bool
    visible_boss_id: str | None = None


@dataclass(frozen=True, slots=True)
class PublicEntity:
    """Additive entity token shared by cards, relics, powers and options."""

    instance_id: str
    content_id: str
    properties: tuple[tuple[str, PublicScalar], ...] = ()

    def __post_init__(self) -> None:
        keys = [key for key, _ in self.properties]
        if keys != sorted(keys) or len(keys) != len(set(keys)):
            raise ValueError("entity properties must have unique, sorted keys")
        _assert_public_tree(dict(self.properties))


@dataclass(frozen=True, slots=True)
class Card:
    instance_id: str
    card_id: str
    zone: str
    upgrades: int
    base_cost: int
    current_cost: int
    playable: bool = False
    # Set only when the original UI legitimately reveals pile order.
    visible_order: int | None = None
    properties: tuple[tuple[str, PublicScalar], ...] = ()

    def __post_init__(self) -> None:
        _assert_public_tree(dict(self.properties))
        if self.zone == "DRAW" and self.visible_order is not None:
            if not dict(self.properties).get("order_is_visible", False):
                raise ValueError("hidden draw-pile order may not enter Observation")


@dataclass(frozen=True, slots=True)
class Enemy:
    instance_id: str
    monster_id: str
    current_hp: int
    max_hp: int
    block: int
    intent: str
    intent_damage: int
    intent_hits: int
    properties: tuple[tuple[str, PublicScalar], ...] = ()


@dataclass(frozen=True, slots=True)
class MapNode:
    node_id: str
    x: int
    y: int
    visible_room_type: str | None
    reachable: bool
    outgoing_node_ids: tuple[str, ...] = ()


@dataclass(frozen=True, slots=True)
class ShopItem:
    instance_id: str
    content_id: str
    item_type: str
    price: int
    sold: bool = False
    properties: tuple[tuple[str, PublicScalar], ...] = ()


@dataclass(frozen=True, slots=True)
class Observation:
    player: Player
    run: RunContext
    screen: ScreenType
    deck: tuple[Card, ...] = ()
    hand: tuple[Card, ...] = ()
    draw_pile: tuple[Card, ...] = ()
    discard_pile: tuple[Card, ...] = ()
    exhaust_pile: tuple[Card, ...] = ()
    enemies: tuple[Enemy, ...] = ()
    powers: tuple[PublicEntity, ...] = ()
    relics: tuple[PublicEntity, ...] = ()
    potions: tuple[PublicEntity, ...] = ()
    map_nodes: tuple[MapNode, ...] = ()
    choice_options: tuple[PublicEntity, ...] = ()
    reward_options: tuple[PublicEntity, ...] = ()
    shop_items: tuple[ShopItem, ...] = ()
    event_options: tuple[PublicEntity, ...] = ()
    rest_options: tuple[PublicEntity, ...] = ()
    boss_relic_options: tuple[PublicEntity, ...] = ()
    public_context: tuple[tuple[str, PublicScalar], ...] = ()
    schema_version: int = field(default=OBSERVATION_SCHEMA_VERSION, init=False)

    def __post_init__(self) -> None:
        _assert_public_tree(self.to_dict())

    def to_dict(self) -> dict[str, Any]:
        return _json_value(self)


def validate_policy_observation(value: Mapping[str, Any]) -> None:
    if value.get("schema_version") != OBSERVATION_SCHEMA_VERSION:
        raise ValueError(f"unsupported observation schema {value.get('schema_version')}")
    _assert_public_tree(value)


def _json_value(value: Any) -> Any:
    if isinstance(value, Enum):
        return value.value
    if is_dataclass(value):
        return {
            item.name: _json_value(getattr(value, item.name))
            for item in fields(value)
        }
    if isinstance(value, tuple):
        # Property tuples are represented as JSON objects.
        if all(isinstance(item, tuple) and len(item) == 2 for item in value):
            return {str(key): _json_value(item) for key, item in value}
        return [_json_value(item) for item in value]
    if isinstance(value, list):
        return [_json_value(item) for item in value]
    if isinstance(value, dict):
        return {str(key): _json_value(item) for key, item in value.items()}
    return value


def _assert_public_tree(value: Any, path: str = "observation") -> None:
    forbidden = {
        "seed", "math_seed", "rng", "rng_state", "rng_counter", "internal",
        "future_events", "encounter_queue", "unrevealed_rewards", "backend_action",
        "action_bits", "simulator_state",
    }
    if isinstance(value, Mapping):
        for key, item in value.items():
            normalized = str(key).lower().lstrip("_")
            if normalized in forbidden or normalized.startswith("rng_"):
                raise ValueError(f"hidden field is forbidden at {path}.{key}")
            _assert_public_tree(item, f"{path}.{key}")
    elif isinstance(value, (tuple, list)):
        for index, item in enumerate(value):
            _assert_public_tree(item, f"{path}[{index}]")
