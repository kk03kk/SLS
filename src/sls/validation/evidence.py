"""Strict evidence sufficiency and comparison classification for Original truth."""

from __future__ import annotations

from typing import Any, Mapping

from sls.contracts.continuation import continuation_original
from sls.validation.truth import value_hash


KNOWN_SCREENS = {
    "NEOW", "MAP", "COMBAT", "COMBAT_REWARD", "CARD_REWARD", "EVENT",
    "REST", "SHOP", "TREASURE", "BOSS_REWARD", "ACT_TRANSITION", "GAME_OVER",
}


def original_evidence_gaps(
    payload: Mapping[str, Any], *, canonical_screen: str,
) -> list[dict[str, str]]:
    """Return missing validation evidence without substituting gameplay defaults."""

    game = payload.get("game_state") or {}
    gaps: list[dict[str, str]] = []

    def require(mapping: Mapping[str, Any], key: str, path: str) -> None:
        if key not in mapping:
            gaps.append({"code": f"MISSING_{key.upper()}", "path": path})

    if canonical_screen not in KNOWN_SCREENS:
        gaps.append({"code": "UNKNOWN_CANONICAL_SCREEN", "path": "$.game_state.screen_type"})
    if payload.get("_parity_schema") and payload.get("_parity_schema") not in {
        "spirecomm-parity-v2", "spirecomm-parity-v3", "spirecomm-parity-v4",
    }:
        gaps.append({"code": "UNSUPPORTED_INSTRUMENTATION_SCHEMA", "path": "$._parity_schema"})
    rng = payload.get("_rng") or game.get("_rng")
    if not isinstance(rng, Mapping):
        gaps.append({"code": "MISSING_RNG_EVIDENCE", "path": "$._rng"})
    else:
        for stream in (
            "ai", "card_random", "card", "event", "math_util", "merchant", "misc",
            "monster_hp", "monster", "potion", "relic", "shuffle", "treasure",
        ):
            require(rng, stream, f"$._rng.{stream}")
    continuation = payload.get("_continuation") or game.get("_continuation")
    if not isinstance(continuation, Mapping):
        gaps.append({"code": "MISSING_CONTINUATION_EVIDENCE", "path": "$._continuation"})
    if str(game.get("screen_type") or "").upper() == "GRID":
        selection = continuation_original(payload)
        for key in ("card_selection_source", "card_selection_task", "card_selection_count"):
            if selection.get(key) in {None, "", 0}:
                gaps.append({"code": f"MISSING_{key.upper()}", "path": f"$._continuation.{key}"})
    parity_run = payload.get("_parity_run") or game.get("_parity_run")
    if not isinstance(parity_run, Mapping):
        gaps.append({"code": "MISSING_RUN_EVIDENCE", "path": "$._parity_run"})
    else:
        for key in ("ruby_key", "emerald_key", "sapphire_key", "burning_elite_x", "burning_elite_y"):
            require(parity_run, key, f"$._parity_run.{key}")
        if int(game.get("floor", 0) or 0) > 0:
            require(parity_run, "current_map_x", "$._parity_run.current_map_x")
            require(parity_run, "current_map_y", "$._parity_run.current_map_y")
    if canonical_screen == "COMBAT":
        monsters = ((game.get("combat_state") or {}).get("monsters") or ())
        intents = payload.get("_monster_intents")
        if not isinstance(intents, list) or len(intents) != len(monsters):
            gaps.append({"code": "MISSING_MONSTER_INTENTS", "path": "$._monster_intents"})
        elif any(not isinstance(item, Mapping) or "intent" not in item for item in intents):
            gaps.append({"code": "INCOMPLETE_MONSTER_INTENTS", "path": "$._monster_intents"})
        elif any(str(item.get("intent") or "").upper() == "DEBUG" for item in intents):
            gaps.append({"code": "UNSETTLED_MONSTER_INTENT", "path": "$._monster_intents"})
        elif any(
            str(item.get("intent") or "").upper().startswith("ATTACK")
            and (
                "damage" not in item or "hits" not in item
                or int(item.get("damage", -1)) < 0 or int(item.get("hits", 0)) < 1
            )
            for item in intents
        ):
            gaps.append({
                "code": "MISSING_ADJUSTED_MONSTER_INTENT_DAMAGE",
                "path": "$._monster_intents",
            })
    if canonical_screen == "COMBAT_REWARD":
        state = game.get("screen_state") or {}
        rewards = (state.get("rewards") or []) if isinstance(state, Mapping) else []
        card_reward_count = sum(
            str(item.get("reward_type") or "").upper() == "CARD"
            for item in rewards if isinstance(item, Mapping)
        )
        groups = payload.get("_combat_reward_cards")
        if card_reward_count and (
            not isinstance(groups, list)
            or len(groups) < card_reward_count
            or any(not isinstance(group, list) or not group for group in groups[:card_reward_count])
        ):
            gaps.append({
                "code": "MISSING_COMBAT_REWARD_CARD_OPTIONS",
                "path": "$._combat_reward_cards",
            })
    scenario = payload.get("_parity_scenario")
    if scenario is not None:
        if not isinstance(scenario, Mapping):
            gaps.append({"code": "LEGACY_SCENARIO_EVIDENCE", "path": "$._parity_scenario"})
        else:
            for key in ("scenario_id", "source", "setup_digest"):
                require(scenario, key, f"$._parity_scenario.{key}")
    return gaps


def comparison_category(differences: Mapping[str, Any]) -> str | None:
    if not differences:
        return None
    paths = tuple(differences)
    if any(path.startswith("continuation:") for path in paths):
        return "CONTINUATION"
    if any("rng" in path.lower() for path in paths):
        return "RNG"
    if any(path.startswith(("observation:", "actions:")) for path in paths):
        return "ADAPTER_CONTRACT"
    return "SIMULATOR_TRANSITION"


def cluster_key(
    *, profile: str, screen: str, category: str, differences: Mapping[str, Any],
    preceding_action: str | None,
) -> str | None:
    if not differences:
        return None
    first = sorted(differences)[0]
    return value_hash({
        "profile": profile, "screen": screen, "category": category,
        "path": first, "preceding_action": preceding_action,
    })


def comparison_result(
    *, evidence_class: str, profile: str, screen: str, act: int, floor: int,
    differences: Mapping[str, Any], evidence_gaps: list[dict[str, str]],
    preceding_action: str | None, occurrence_signature: str | None,
) -> dict[str, Any]:
    category = "EVIDENCE_GAP" if evidence_gaps else comparison_category(differences)
    status = "INCONCLUSIVE" if evidence_gaps else "DIFFERENCE" if differences else "MATCH"
    values: Mapping[str, Any] = differences
    if evidence_gaps:
        values = {f"evidence:{gap['path']}": (None, gap["code"]) for gap in evidence_gaps}
    if occurrence_signature is None and values:
        from sls.validation.truth import difference_signature
        occurrence_signature = difference_signature(
            evidence_class=evidence_class, profile=profile, screen=screen,
            act=act, floor=floor, category=category or "MATCH", values=values,
            preceding_action=preceding_action,
        )
    return {
        "status": status, "category": category, "differences": dict(differences),
        "evidence_gaps": evidence_gaps,
        "occurrence_signature": occurrence_signature,
        "cluster_key": cluster_key(
            profile=profile, screen=screen, category=category or "MATCH",
            differences=values, preceding_action=preceding_action,
        ),
    }
