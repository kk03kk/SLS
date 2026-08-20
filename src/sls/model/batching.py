"""Deterministic batching for canonical FullRun decisions."""

from __future__ import annotations

from dataclasses import dataclass
import hashlib
import math
from typing import Iterable, Mapping

import torch

from sls.contracts import Action, ActionKind, Decision, Observation, PublicEntity
from sls.model.transformer import ModelConfig


ENTITY_TYPES = (
    "PLAYER", "RUN", "CARD", "ENEMY", "POWER", "RELIC", "POTION",
    "MAP_NODE", "CHOICE", "REWARD", "SHOP_ITEM", "EVENT_OPTION",
    "REST_OPTION", "BOSS_RELIC",
)
ENTITY_TYPE_IDS = {name: index for index, name in enumerate(ENTITY_TYPES)}
ACTION_TYPE_IDS = {
    kind.value: index
    for index, kind in enumerate(ActionKind)
}


@dataclass(frozen=True, slots=True)
class EncodedDecision:
    entity_features: torch.Tensor
    entity_types: torch.Tensor
    action_features: torch.Tensor
    action_types: torch.Tensor

    @property
    def entity_count(self) -> int:
        return int(self.entity_types.shape[0])

    @property
    def action_count(self) -> int:
        return int(self.action_types.shape[0])


@dataclass(frozen=True, slots=True)
class PolicyBatch:
    entity_features: torch.Tensor
    entity_types: torch.Tensor
    entity_padding: torch.Tensor
    action_features: torch.Tensor
    action_types: torch.Tensor
    action_padding: torch.Tensor

    @classmethod
    def from_decisions(
        cls,
        decisions: Iterable[Decision],
        config: ModelConfig = ModelConfig(),
    ) -> "PolicyBatch":
        encoded = [encode_decision(value, config) for value in decisions]
        if not encoded:
            raise ValueError("policy batch cannot be empty")
        if any(value.action_count == 0 for value in encoded):
            raise ValueError("policy batches require non-terminal decisions with legal actions")
        max_entities = max(value.entity_count for value in encoded)
        max_actions = max(value.action_count for value in encoded)
        batch_size = len(encoded)
        entity_features = torch.zeros(
            batch_size, max_entities, config.entity_feature_dim, dtype=torch.float32,
        )
        entity_types = torch.zeros(batch_size, max_entities, dtype=torch.long)
        entity_padding = torch.ones(batch_size, max_entities, dtype=torch.bool)
        action_features = torch.zeros(
            batch_size, max_actions, config.action_feature_dim, dtype=torch.float32,
        )
        action_types = torch.zeros(batch_size, max_actions, dtype=torch.long)
        action_padding = torch.ones(batch_size, max_actions, dtype=torch.bool)
        for index, value in enumerate(encoded):
            entity_features[index, :value.entity_count] = value.entity_features
            entity_types[index, :value.entity_count] = value.entity_types
            entity_padding[index, :value.entity_count] = False
            action_features[index, :value.action_count] = value.action_features
            action_types[index, :value.action_count] = value.action_types
            action_padding[index, :value.action_count] = False
        return cls(
            entity_features, entity_types, entity_padding,
            action_features, action_types, action_padding,
        )

    def to(self, device: torch.device | str) -> "PolicyBatch":
        return PolicyBatch(*(
            value.to(device, non_blocking=True)
            for value in (
                self.entity_features, self.entity_types, self.entity_padding,
                self.action_features, self.action_types, self.action_padding,
            )
        ))

    def model_inputs(self) -> tuple[torch.Tensor, ...]:
        return (
            self.entity_features, self.entity_types, self.entity_padding,
            self.action_features, self.action_types, self.action_padding,
        )


def _digest(value: str) -> bytes:
    return hashlib.blake2b(value.encode("utf-8"), digest_size=16).digest()


def _add_categorical(vector: list[float], family: str, value: object) -> None:
    digest = _digest(f"{family}\0{value}")
    index = int.from_bytes(digest[:8], "little") % len(vector)
    vector[index] += 1.0 if digest[8] & 1 else -1.0


def _add_scalar(vector: list[float], family: str, key: str, value: object) -> None:
    if isinstance(value, bool):
        numeric = 1.0 if value else 0.0
    elif isinstance(value, (int, float)):
        raw = float(value)
        numeric = math.copysign(math.log1p(abs(raw)), raw)
    else:
        _add_categorical(vector, f"{family}:{key}", value)
        return
    digest = _digest(f"{family}\0{key}")
    index = int.from_bytes(digest[:8], "little") % len(vector)
    vector[index] += numeric


def _features(
    dimension: int,
    family: str,
    *,
    reference: str | None = None,
    content: str | None = None,
    values: Mapping[str, object] | None = None,
) -> list[float]:
    vector = [0.0] * dimension
    if reference is not None:
        _add_categorical(vector, "reference", reference)
    if content is not None:
        _add_categorical(vector, "content", content)
    for key, value in sorted((values or {}).items()):
        if value is not None:
            _add_scalar(vector, family, str(key), value)
    return vector


def _public_entity_row(
    value: PublicEntity, dimension: int, family: str,
) -> list[float]:
    return _features(
        dimension, family, reference=value.instance_id, content=value.content_id,
        values=dict(value.properties),
    )


def _action_row(value: Action, dimension: int) -> list[float]:
    vector = _features(
        dimension, "action", values={"kind": value.kind.value, **dict(value.metadata)},
    )
    for field_name, reference in (
        ("subject", value.subject_id), ("target", value.target_id),
        ("option", value.option_id), ("node", value.node_id),
        ("reward", value.reward_id),
    ):
        if reference is not None:
            _add_categorical(vector, "reference", reference)
            _add_categorical(vector, f"action:{field_name}", reference)
    return vector


def encode_decision(
    decision: Decision,
    config: ModelConfig = ModelConfig(),
) -> EncodedDecision:
    observation: Observation = decision.observation
    if config.entity_type_count < len(ENTITY_TYPES):
        raise ValueError(f"entity_type_count must be at least {len(ENTITY_TYPES)}")
    if config.action_type_count < len(ACTION_TYPE_IDS):
        raise ValueError(f"action_type_count must be at least {len(ACTION_TYPE_IDS)}")

    rows: list[list[float]] = []
    types: list[int] = []

    def add(entity_type: str, row: list[float]) -> None:
        rows.append(row)
        types.append(ENTITY_TYPE_IDS[entity_type])

    player = observation.player
    add("PLAYER", _features(
        config.entity_feature_dim, "player", reference="player", content=player.character_id,
        values={
            "current_hp": player.current_hp, "max_hp": player.max_hp,
            "block": player.block, "energy": player.energy,
            "max_energy": player.max_energy,
        },
    ))
    run = observation.run
    add("RUN", _features(
        config.entity_feature_dim, "run", reference="run", content=observation.screen.value,
        values={
            "ascension": run.ascension, "act": run.act, "floor": run.floor,
            "gold": run.gold, "ruby_key": run.has_ruby_key,
            "emerald_key": run.has_emerald_key, "sapphire_key": run.has_sapphire_key,
            "visible_boss": run.visible_boss_id, **dict(observation.public_context),
        },
    ))
    for card in (
        observation.deck + observation.hand + observation.draw_pile
        + observation.discard_pile + observation.exhaust_pile
    ):
        add("CARD", _features(
            config.entity_feature_dim, "card", reference=card.instance_id, content=card.card_id,
            values={
                "zone": card.zone, "upgrades": card.upgrades,
                "base_cost": card.base_cost, "current_cost": card.current_cost,
                "playable": card.playable, "visible_order": card.visible_order,
                **dict(card.properties),
            },
        ))
    for enemy in observation.enemies:
        add("ENEMY", _features(
            config.entity_feature_dim, "enemy", reference=enemy.instance_id,
            content=enemy.monster_id,
            values={
                "current_hp": enemy.current_hp, "max_hp": enemy.max_hp,
                "block": enemy.block, "intent": enemy.intent,
                "intent_damage": enemy.intent_damage, "intent_hits": enemy.intent_hits,
                **dict(enemy.properties),
            },
        ))
    for entity_type, values in (
        ("POWER", observation.powers), ("RELIC", observation.relics),
        ("POTION", observation.potions), ("CHOICE", observation.choice_options),
        ("REWARD", observation.reward_options), ("EVENT_OPTION", observation.event_options),
        ("REST_OPTION", observation.rest_options),
        ("BOSS_RELIC", observation.boss_relic_options),
    ):
        for value in values:
            add(entity_type, _public_entity_row(value, config.entity_feature_dim, entity_type.lower()))
    for node in observation.map_nodes:
        add("MAP_NODE", _features(
            config.entity_feature_dim, "map", reference=node.node_id,
            content=node.visible_room_type,
            values={
                "x": node.x, "y": node.y, "reachable": node.reachable,
                "outgoing_count": len(node.outgoing_node_ids),
            },
        ))
    for item in observation.shop_items:
        add("SHOP_ITEM", _features(
            config.entity_feature_dim, "shop", reference=item.instance_id,
            content=item.content_id,
            values={
                "item_type": item.item_type, "price": item.price, "sold": item.sold,
                **dict(item.properties),
            },
        ))

    action_rows = [
        _action_row(action, config.action_feature_dim)
        for action in decision.actions
    ]
    return EncodedDecision(
        torch.tensor(rows, dtype=torch.float32),
        torch.tensor(types, dtype=torch.long),
        torch.tensor(action_rows, dtype=torch.float32).reshape(-1, config.action_feature_dim),
        torch.tensor(
            [ACTION_TYPE_IDS[action.kind.value] for action in decision.actions],
            dtype=torch.long,
        ),
    )
