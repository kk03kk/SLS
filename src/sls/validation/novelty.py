"""Coverage fingerprints used to select informative Act 1 validation seeds."""

from __future__ import annotations

from typing import Any, Iterable, Mapping


ENTITY_LISTS = (
    ("card", ("deck", "hand", "draw_pile", "discard_pile", "exhaust_pile"), "card_id"),
    ("enemy", ("enemies",), "monster_id"),
    ("power", ("powers",), "content_id"),
    ("relic", ("relics",), "content_id"),
    ("potion", ("potions",), "content_id"),
)


def coverage_fingerprints(
    observation: Mapping[str, Any], *, cursor: Mapping[str, Any] | None = None,
    continuation: Mapping[str, Any] | None = None,
    selected_action: Mapping[str, Any] | None = None,
) -> set[str]:
    """Return stable, policy-visible/mechanism coverage tokens for one boundary."""

    cursor = cursor or {}
    continuation = continuation or {}
    result = {f"screen:{observation.get('screen', cursor.get('screen', 'UNKNOWN'))}"}
    room = cursor.get("room") or continuation.get("room_class")
    if room:
        result.add(f"room:{room}")
    run = observation.get("run") or {}
    boss = run.get("visible_boss_id") or run.get("boss")
    if boss:
        result.add(f"boss:{boss}")
    event = continuation.get("event_id")
    phase = continuation.get("event_phase")
    if event:
        result.add(f"event:{event}")
        result.add(f"event_phase:{event}:{phase}")
    kind = continuation.get("continuation_kind")
    if kind is not None:
        result.add(f"continuation:{kind}")
    for prefix, fields, content_key in ENTITY_LISTS:
        for field in fields:
            for entity in observation.get(field) or ():
                content = entity.get(content_key)
                if content:
                    result.add(f"{prefix}:{content}")
                if prefix == "enemy" and entity.get("intent"):
                    result.add(f"intent:{entity['intent']}")
    enemies = sorted(
        str(item.get("monster_id")) for item in observation.get("enemies") or ()
        if item.get("monster_id")
    )
    if enemies:
        result.add("encounter:" + "+".join(enemies))
    if selected_action and selected_action.get("kind"):
        result.add(f"action:{selected_action['kind']}")
    return result


def greedy_select(
    candidates: Iterable[Mapping[str, Any]], baseline: Iterable[str], *, count: int,
) -> list[dict[str, Any]]:
    """Select unique seeds by marginal fingerprint gain with stable tie breaking."""

    if count <= 0:
        raise ValueError("selection count must be positive")
    remaining = [dict(item) for item in candidates]
    covered = set(map(str, baseline))
    selected: list[dict[str, Any]] = []
    used_seeds: set[int] = set()
    while len(selected) < count:
        eligible = [item for item in remaining if int(item["seed"]) not in used_seeds]
        if not eligible:
            raise ValueError("not enough unique candidate seeds")
        ranked = sorted(
            eligible,
            key=lambda item: (
                -len(set(map(str, item["fingerprints"])) - covered),
                int(item["seed"]), int(item["variant"]),
            ),
        )
        winner = ranked[0]
        fingerprints = set(map(str, winner["fingerprints"]))
        novel = sorted(fingerprints - covered)
        selected.append({
            "seed": int(winner["seed"]), "variant": int(winner["variant"]),
            "boundary_count": int(winner["boundary_count"]),
            "max_floor": int(winner["max_floor"]),
            "terminal": bool(winner["terminal"]),
            "novelty_count": len(novel), "novel_fingerprints": novel,
        })
        covered.update(fingerprints)
        used_seeds.add(int(winner["seed"]))
        remaining.remove(winner)
    return selected
