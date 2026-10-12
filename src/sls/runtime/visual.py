"""Read-only, confirmed public-game telemetry, separate from policy inputs."""

from __future__ import annotations

import time
from collections import Counter
from copy import deepcopy
from dataclasses import asdict
from typing import Any

from sls.contracts import Decision


def public_context(backend: Any, decision: Decision) -> dict[str, Any]:
    """Whitelist display metadata; never publish the raw Oracle payload."""
    try:
        raw = backend.raw_payload
    except (AttributeError, RuntimeError):
        raw = {}
    game = raw.get("game_state") or {}
    run = game.get("_parity_run") or raw.get("_parity_run") or {}
    state = game.get("screen_state") or {}
    combat = game.get("combat_state") or {}
    x, y = run.get("current_map_x"), run.get("current_map_y")
    node = None
    if isinstance(x, int) and isinstance(y, int) and y >= 0:
        node = "map:boss" if y >= 15 else f"map:{x}:{y}"
    return {
        "node_id": node,
        "seed": str(game["seed"]) if game.get("seed") is not None else None,
        "turn": combat.get("turn"),
        "event_name": state.get("event_name") or state.get("event_id"),
        "event_body": state.get("body_text"),
        "room_phase": game.get("room_phase"),
        "room_type": game.get("room_type") or game.get("room_class"),
        "source": "original-public-state" if raw else "unavailable",
    }


class VisualHistory:
    """Observe confirmations without executing actions or advancing policy memory."""

    def __init__(self) -> None:
        self.entries: list[dict[str, Any]] = []
        self.route: list[str] = []
        self.nodes: dict[str, dict[str, Any]] = {}
        self.current: dict[str, Any] = {}
        self.pending: dict[str, Any] | None = None

    def observe(self, backend: Any, decision: Decision) -> None:
        self.current = public_context(backend, decision)
        node = self.current["node_id"]
        if node and (not self.route or self.route[-1] != node):
            self.route.append(node)
        if node:
            info = self.nodes.setdefault(node, {
                "node_id": node, "floor": decision.observation.run.floor,
                "hp_before": decision.observation.player.current_hp,
                "decisions": 0,
            })
            info["hp_latest"] = decision.observation.player.current_hp
            info["phase"] = decision.observation.screen.value
            info["enemies"] = [e.monster_id for e in decision.observation.enemies] or info.get("enemies", [])
            if self.current.get("turn") is not None:
                info["last_observed_turn"] = self.current["turn"]
            if self.current.get("room_phase") == "COMPLETE" and info.get("enemies"):
                info["result"] = "Victory"

    def begin(self, record: dict[str, Any], actions: list[dict[str, Any]]) -> None:
        self.pending = {
            "step": len(self.entries) + 1,
            "timestamp": record["time_unix"],
            "boundary_id": record["boundary_id"],
            "node_id": self.current.get("node_id"),
            "floor": record["floor"], "act": record["act"],
            "screen": record["screen"], "observation": record["observation"],
            "actions": deepcopy(actions),
            "selected_action": record["selected_action"],
            "selected_candidate_id": record["candidate_id"],
            "selection_source": record["selection_source"],
            "value": record["value"], "confirmation": "PENDING",
        }

    def confirm(self, record: dict[str, Any], backend: Any, decision: Decision) -> dict[str, Any] | None:
        if self.pending is None:
            return None
        entry = self.pending
        entry.update({
            "confirmation": "ACK", "confirmed_at": time.time(),
            "resulting_boundary_id": record["observed_boundary_id"],
            "resulting_observation": decision.observation.to_dict(),
            "reward": record["previous_reward"], "terminal": decision.terminal,
        })
        before, after = entry["observation"], entry["resulting_observation"]
        changes: dict[str, Any] = {"gold_delta": after["run"]["gold"] - before["run"]["gold"]}
        for collection, field in (("deck", "card_id"), ("relics", "content_id"), ("potions", "content_id")):
            old = Counter(item[field] for item in before[collection])
            new = Counter(item[field] for item in after[collection])
            changes[collection + "_added"] = list((new - old).elements())
            changes[collection + "_removed"] = list((old - new).elements())
        entry["inventory_changes"] = changes
        node = entry["node_id"]
        if node in self.nodes:
            self.nodes[node]["decisions"] += 1
        self.entries.append(entry)
        self.pending = None
        self.observe(backend, decision)
        return deepcopy(entry)

    def snapshot(self, decision: Decision | None) -> dict[str, Any]:
        nodes = [asdict(node) for node in decision.observation.map_nodes] if decision else []
        # Boss is an explicit public destination; this adds a display node only.
        if any("map:boss" in node["outgoing_node_ids"] for node in nodes) and not any(node["node_id"] == "map:boss" for node in nodes):
            nodes.append({"node_id": "map:boss", "x": 3, "y": 15, "visible_room_type": "BOSS", "reachable": False, "outgoing_node_ids": []})
        return {
            "context": deepcopy(self.current), "map": nodes,
            "route": list(self.route), "nodes": deepcopy(self.nodes),
            "history_count": len(self.entries),
            "pending_action": deepcopy(self.pending),
        }
