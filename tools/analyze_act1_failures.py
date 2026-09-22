"""Summarize Act 1 failures from a canonical evaluation artifact."""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path


def _outcome(row: dict) -> str:
    if row["success"]:
        return "win"
    if row.get("reason") != "DEATH":
        return "operational_failure"
    return "boss_death" if int(row["floor"]) == 16 else "preboss_death"


def analyze(record: dict) -> dict:
    result = record.get("result")
    if not isinstance(result, dict) or not isinstance(result.get("seed_results"), list):
        raise ValueError("evaluation artifact has no seed_results")
    rows = result["seed_results"]
    if len(rows) != int(result.get("episodes", -1)):
        raise ValueError("seed_results count does not match episodes")
    if len({int(row["seed"]) for row in rows}) != len(rows):
        raise ValueError("evaluation contains duplicate seeds")
    if sum(bool(row["success"]) for row in rows) != int(result.get("successes", -1)):
        raise ValueError("seed_results wins do not match successes")

    outcomes = Counter(_outcome(row) for row in rows)
    boss_rows: dict[str, list[dict]] = {}
    for row in rows:
        boss = str(row["bosses"]["1"])
        boss_rows.setdefault(boss, []).append(row)
    bosses = {}
    for boss, values in sorted(boss_rows.items()):
        counts = Counter(_outcome(row) for row in values)
        bosses[boss] = {
            "assigned_seeds": len(values),
            "wins": counts["win"],
            "boss_deaths": counts["boss_death"],
            "preboss_deaths": counts["preboss_death"],
            "operational_failures": counts["operational_failure"],
            "boss_entries": sum(bool(row.get("entered_bosses")) for row in values),
        }

    early_enemies = Counter()
    for row in rows:
        if _outcome(row) != "preboss_death":
            continue
        enemies = tuple(row.get("last_context", {}).get("enemy_ids", ()))
        early_enemies[" + ".join(enemies) if enemies else "UNKNOWN"] += 1
    elite_names = {"LAGAVULIN", "GREMLIN_NOB", "SENTRY + SENTRY + SENTRY"}
    elite_deaths = sum(early_enemies[name] for name in elite_names)

    return {
        "schema": "sls-act1-failure-analysis-v1",
        "episodes": len(rows),
        "outcomes": dict(sorted(outcomes.items())),
        "failure_share": {
            "boss_death": outcomes["boss_death"] / max(1, len(rows) - outcomes["win"]),
            "preboss_death": outcomes["preboss_death"] / max(1, len(rows) - outcomes["win"]),
        },
        "bosses": bosses,
        "death_floors": dict(result.get("death_floor_distribution", {})),
        "preboss_last_encounters": dict(early_enemies.most_common()),
        "preboss_elite_deaths": elite_deaths,
        "preboss_elite_death_share": elite_deaths / max(1, outcomes["preboss_death"]),
        "boss_action_metrics": result.get("boss_action_metrics", {}),
        "runtime_quality": {
            key: int(result.get(key, 0))
            for key in (
                "backend_errors", "backend_truncations", "timeouts", "step_limits",
                "cycle_limits", "self_loops",
            )
        },
        "instrumentation_gaps": [
            "per-seed boss-entry HP, potion, relic and deck state",
            "per-seed shop, card-reward, path-room and campfire decisions",
            "per-seed tactical action sequence and legal alternatives",
        ],
        "interpretation_limits": (
            "Deck and aggregate action correlations are observational; this artifact "
            "cannot separate pre-boss preparation from combat execution causally."
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("evaluation", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    payload = args.evaluation.read_bytes()
    record = json.loads(payload)
    report = {
        **analyze(record),
        "source": str(args.evaluation.resolve()),
        "source_sha256": hashlib.sha256(payload).hexdigest(),
    }
    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        temporary = args.output.with_suffix(args.output.suffix + ".tmp")
        temporary.write_text(rendered, encoding="utf-8")
        temporary.replace(args.output)
    print(rendered, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
