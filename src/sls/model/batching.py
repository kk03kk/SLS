"""Collision-free structural batching for canonical FullRun decisions."""

from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Iterable, Mapping

import torch

from sls.contracts import Decision, Observation
from sls.model.encoding import (
    ACTION_TYPE_IDS,
    CATEGORICAL_FIELD_IDS,
    CATEGORICAL_FIELDS,
    ENTITY_TYPE_IDS,
    NUMERIC_FIELD_IDS,
    NUMERIC_FIELDS,
    SCREEN_GROUP_IDS,
    SCREEN_TO_GROUP,
    categorical_token,
    content_token,
)

REFERENCE_FIELDS = ("subject_id", "target_id", "option_id", "node_id", "reward_id")


@dataclass(frozen=True, slots=True)
class EncodedDecision:
    screen_type: torch.Tensor
    entity_numeric: torch.Tensor
    entity_numeric_present: torch.Tensor
    entity_types: torch.Tensor
    entity_content: torch.Tensor
    entity_categories: torch.Tensor
    entity_adjacency: torch.Tensor
    action_numeric: torch.Tensor
    action_numeric_present: torch.Tensor
    action_types: torch.Tensor
    action_references: torch.Tensor
    action_reference_mask: torch.Tensor

    @property
    def entity_count(self) -> int:
        return int(self.entity_types.shape[0])

    @property
    def action_count(self) -> int:
        return int(self.action_types.shape[0])


@dataclass(frozen=True, slots=True)
class PolicyBatch:
    screen_types: torch.Tensor
    entity_numeric: torch.Tensor
    entity_numeric_present: torch.Tensor
    entity_types: torch.Tensor
    entity_content: torch.Tensor
    entity_categories: torch.Tensor
    entity_adjacency: torch.Tensor
    entity_padding: torch.Tensor
    action_numeric: torch.Tensor
    action_numeric_present: torch.Tensor
    action_types: torch.Tensor
    action_references: torch.Tensor
    action_reference_mask: torch.Tensor
    action_padding: torch.Tensor

    @classmethod
    def from_decisions(cls, decisions: Iterable[Decision], config: object | None = None) -> "PolicyBatch":
        return cls.from_encoded([encode_decision(value) for value in decisions])

    @classmethod
    def from_encoded(cls, encoded: Iterable[EncodedDecision]) -> "PolicyBatch":
        encoded = list(encoded)
        if not encoded:
            raise ValueError("policy batch cannot be empty")
        if any(value.action_count == 0 for value in encoded):
            raise ValueError("policy batches require non-terminal decisions with legal actions")
        b = len(encoded)
        e = max(item.entity_count for item in encoded)
        a = max(item.action_count for item in encoded)
        nf, cf, rf = len(NUMERIC_FIELDS), len(CATEGORICAL_FIELDS), len(REFERENCE_FIELDS)
        entity_numeric = torch.zeros(b, e, nf)
        entity_numeric_present = torch.zeros(b, e, nf, dtype=torch.bool)
        entity_types = torch.zeros(b, e, dtype=torch.long)
        entity_content = torch.zeros(b, e, 2, dtype=torch.long)
        entity_categories = torch.zeros(b, e, cf, dtype=torch.long)
        entity_adjacency = torch.zeros(b, e, e, dtype=torch.bool)
        entity_padding = torch.ones(b, e, dtype=torch.bool)
        action_numeric = torch.zeros(b, a, nf)
        action_numeric_present = torch.zeros(b, a, nf, dtype=torch.bool)
        action_types = torch.zeros(b, a, dtype=torch.long)
        action_references = torch.zeros(b, a, rf, dtype=torch.long)
        action_reference_mask = torch.zeros(b, a, rf, dtype=torch.bool)
        action_padding = torch.ones(b, a, dtype=torch.bool)
        screen_types = torch.zeros(b, dtype=torch.long)
        for index, item in enumerate(encoded):
            ec, ac = item.entity_count, item.action_count
            entity_numeric[index, :ec] = item.entity_numeric
            entity_numeric_present[index, :ec] = item.entity_numeric_present
            entity_types[index, :ec] = item.entity_types
            entity_content[index, :ec] = item.entity_content
            entity_categories[index, :ec] = item.entity_categories
            entity_adjacency[index, :ec, :ec] = item.entity_adjacency
            entity_padding[index, :ec] = False
            action_numeric[index, :ac] = item.action_numeric
            action_numeric_present[index, :ac] = item.action_numeric_present
            action_types[index, :ac] = item.action_types
            action_references[index, :ac] = item.action_references
            action_reference_mask[index, :ac] = item.action_reference_mask
            action_padding[index, :ac] = False
            screen_types[index] = item.screen_type
        return cls(
            screen_types, entity_numeric, entity_numeric_present, entity_types, entity_content,
            entity_categories, entity_adjacency, entity_padding, action_numeric,
            action_numeric_present, action_types, action_references,
            action_reference_mask, action_padding,
        )

    def to(self, device: torch.device | str) -> "PolicyBatch":
        target = torch.device(device)
        values = self.model_inputs()
        if target.type != "cuda":
            return PolicyBatch(*(value.to(target) for value in values))

        # Fourteen tiny host-to-device copies dominate small policy batches.
        # Coalesce fields by dtype, transfer three contiguous buffers, then
        # recover zero-copy views with their original shapes.
        moved: list[torch.Tensor | None] = [None] * len(values)
        for dtype in {value.dtype for value in values}:
            positions = [index for index, value in enumerate(values) if value.dtype == dtype]
            flat = torch.cat([values[index].reshape(-1) for index in positions]).to(
                target, non_blocking=True,
            )
            offset = 0
            for index in positions:
                count = values[index].numel()
                moved[index] = flat[offset:offset + count].view(values[index].shape)
                offset += count
        return PolicyBatch(*(value for value in moved if value is not None))

    def model_inputs(self) -> tuple[torch.Tensor, ...]:
        return tuple(getattr(self, name) for name in self.__dataclass_fields__)


def _number(value: object, *, path: str) -> float:
    if isinstance(value, bool):
        return float(value)
    if not isinstance(value, (int, float)):
        raise ValueError(f"policy numeric field at {path} is not numeric: {value!r}")
    raw = float(value)
    return math.copysign(math.log1p(abs(raw)), raw)


def _features(values: Mapping[str, object], *, path: str) -> tuple[list[float], list[bool], list[int]]:
    numeric = [0.0] * len(NUMERIC_FIELDS)
    present = [False] * len(NUMERIC_FIELDS)
    categories = [0] * len(CATEGORICAL_FIELDS)
    for key, value in values.items():
        if value is None:
            continue
        if key in NUMERIC_FIELD_IDS:
            idx = NUMERIC_FIELD_IDS[key]
            numeric[idx] = _number(value, path=f"{path}.{key}")
            present[idx] = True
        elif key in CATEGORICAL_FIELD_IDS:
            categories[CATEGORICAL_FIELD_IDS[key]] = categorical_token(str(value), path=f"{path}.{key}")
        else:
            raise ValueError(f"unknown policy field at {path}: {key}")
    return numeric, present, categories


def encode_decision(decision: Decision, config: object | None = None) -> EncodedDecision:
    observation: Observation = decision.observation
    rows: list[tuple[str, str, str | None, Mapping[str, object]]] = []

    def add(kind: str, reference: str, content: str | None, values: Mapping[str, object]) -> None:
        rows.append((kind, reference, content, values))

    player = observation.player
    add("PLAYER", "player", player.character_id, {
        "current_hp": player.current_hp, "max_hp": player.max_hp, "block": player.block,
        "energy": player.energy, "max_energy": player.max_energy,
    })
    run = observation.run
    add("RUN", "run", None, {
        "screen": observation.screen.value, "ascension": run.ascension, "act": run.act,
        "floor": run.floor, "gold": run.gold, "ruby_key": run.has_ruby_key,
        "emerald_key": run.has_emerald_key, "sapphire_key": run.has_sapphire_key,
        "visible_boss": run.visible_boss_id, **dict(observation.public_context),
    })
    for card in observation.deck + observation.hand + observation.draw_pile + observation.discard_pile + observation.exhaust_pile:
        add("CARD", card.instance_id, card.card_id, {
            "zone": card.zone, "upgrades": card.upgrades, "base_cost": card.base_cost,
            "current_cost": card.current_cost, "playable": card.playable,
            "visible_order": card.visible_order, **dict(card.properties),
        })
    for enemy in observation.enemies:
        add("ENEMY", enemy.instance_id, enemy.monster_id, {
            "current_hp": enemy.current_hp, "max_hp": enemy.max_hp, "block": enemy.block,
            "intent": enemy.intent, "intent_damage": enemy.intent_damage,
            "intent_hits": enemy.intent_hits, **dict(enemy.properties),
        })
    for kind, values in (
        ("POWER", observation.powers), ("RELIC", observation.relics),
        ("POTION", observation.potions), ("CHOICE", observation.choice_options),
        ("CHOICE", observation.selected_cards),
        ("REWARD", observation.reward_options), ("EVENT_OPTION", observation.event_options),
        ("REST_OPTION", observation.rest_options), ("BOSS_RELIC", observation.boss_relic_options),
    ):
        for value in values:
            add(kind, value.instance_id, value.content_id, dict(value.properties))
    for node in observation.map_nodes:
        add("MAP_NODE", node.node_id, None, {
            "x": node.x, "y": node.y, "reachable": node.reachable,
            "outgoing_count": len(node.outgoing_node_ids), "item_type": node.visible_room_type,
        })
    known_nodes = {node.node_id for node in observation.map_nodes}
    for outgoing in sorted({
        item for node in observation.map_nodes for item in node.outgoing_node_ids
    } - known_nodes):
        parts = outgoing.split(":")
        x = int(parts[1]) if len(parts) > 2 and parts[1].lstrip("-").isdigit() else 0
        y = int(parts[2]) if len(parts) > 2 and parts[2].lstrip("-").isdigit() else 15
        add("MAP_NODE", outgoing, None, {
            "x": x, "y": y, "reachable": False, "outgoing_count": 0,
        })
    for item in observation.shop_items:
        add("SHOP_ITEM", item.instance_id, item.content_id, {
            "item_type": item.item_type, "price": item.price, "sold": item.sold,
            **dict(item.properties),
        })

    existing_references = {reference for _, reference, _, _ in rows}
    for option_id in sorted({
        action.option_id for action in decision.actions
        if action.option_id is not None and action.option_id not in existing_references
    }):
        tail = option_id.rsplit(":", 1)[-1]
        ordinal = int(tail) if tail.lstrip("-").isdigit() else 0
        add("CHOICE", option_id, "OPTION", {"option_ordinal": ordinal})

    references: dict[str, int] = {}
    numeric_rows, present_rows, type_rows, content_rows, category_rows = [], [], [], [], []
    for index, (kind, reference, content, values) in enumerate(rows):
        if reference in references:
            raise ValueError(f"duplicate policy entity reference: {reference}")
        references[reference] = index
        numeric, present, categories = _features(values, path=f"entity[{reference}]")
        base_content, variant = content_token(content)
        numeric_rows.append(numeric)
        present_rows.append(present)
        type_rows.append(ENTITY_TYPE_IDS[kind])
        content_rows.append((base_content, variant))
        category_rows.append(categories)

    adjacency = torch.zeros(len(rows), len(rows), dtype=torch.bool)
    for power in observation.powers:
        adjacency[references[power.instance_id], references[power.owner_id]] = True
    for node in observation.map_nodes:
        source = references[node.node_id]
        for outgoing in node.outgoing_node_ids:
            adjacency[source, references[outgoing]] = True

    action_numeric, action_present, action_types = [], [], []
    action_references, action_masks = [], []
    for action in decision.actions:
        numeric, present, _ = _features(dict(action.metadata), path=f"action[{action.kind.value}]")
        refs, masks = [], []
        for field in REFERENCE_FIELDS:
            value = getattr(action, field)
            if value is not None and value in references:
                refs.append(references[value])
                masks.append(True)
            elif value is not None:
                raise ValueError(f"unresolved action {field}: {value}")
            else:
                refs.append(0)
                masks.append(False)
        action_numeric.append(numeric)
        action_present.append(present)
        action_types.append(ACTION_TYPE_IDS[action.kind.value])
        action_references.append(refs)
        action_masks.append(masks)

    return EncodedDecision(
        torch.tensor(SCREEN_GROUP_IDS[SCREEN_TO_GROUP[observation.screen.value]]),
        torch.tensor(numeric_rows, dtype=torch.float32), torch.tensor(present_rows, dtype=torch.bool),
        torch.tensor(type_rows, dtype=torch.long), torch.tensor(content_rows, dtype=torch.long),
        torch.tensor(category_rows, dtype=torch.long), adjacency,
        torch.tensor(action_numeric, dtype=torch.float32).reshape(-1, len(NUMERIC_FIELDS)),
        torch.tensor(action_present, dtype=torch.bool).reshape(-1, len(NUMERIC_FIELDS)),
        torch.tensor(action_types, dtype=torch.long),
        torch.tensor(action_references, dtype=torch.long).reshape(-1, len(REFERENCE_FIELDS)),
        torch.tensor(action_masks, dtype=torch.bool).reshape(-1, len(REFERENCE_FIELDS)),
    )
