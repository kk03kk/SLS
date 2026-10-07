"""Freeze stock-sourced shared combat probes; these are not full-flow certification."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from tools.prepare_fullrun_audit import ROOT, allocate_seeds, recorded_seeds


def scenes() -> list[dict]:
    rows = []
    def play(target=None, index=1):
        return {"kind": "play", "card_index": index} | ({"target_index": target} if target is not None else {})
    def end(count):
        return [{"kind": "end_turn"} for _ in range(count)]
    def add(identifier, encounter, card, classes, actions, obligation, **initial):
        rows.append({"id": identifier, "encounter": encounter, "ascension": 20, "act": 2, "floor": 20,
                     "initial": {"hp": 5000, "max_hp": 5000, "energy": 10, "block": 0,
                                 "hand": [card], "draw": ["Defend_R", "Strike_R"], **initial},
                     "stock_classes": classes.split(),
                     "stock_methods": {name: (["use"] if name.startswith("cards.")
                                               else ["takeTurn"] if name.startswith("monsters.")
                                               else ["update"] if name.startswith("actions.")
                                               else ["use"] if name.startswith("potions.")
                                               else [])
                                       for name in classes.split()},
                     "actions": actions, "obligation": obligation,
                     "status": "READY_FOR_CONTROLLED_EXECUTION",
                     "qualification_scope": "ISOLATED_MECHANISM_NOT_DUNGEON_FLOW"})
    add("shared-multihit", "SNAKE_PLANT", "Twin Strike",
        "cards.red.TwinStrike monsters.city.SnakePlant powers.MalleablePower", [play(0)] + end(1),
        "Two queued hits independently trigger Malleable")
    add("shared-thorns", "THREE_BYRDS", "Flame Barrier",
        "cards.red.FlameBarrier powers.FlameBarrierPower monsters.city.Byrd", [play()] + end(3),
        "Attack-triggered thorns and next-turn removal; lethal branch not guaranteed")
    add("shared-artifact", "SNECKO", "Panacea",
        "cards.colorless.Panacea monsters.city.Snecko", [play()] + end(6),
        "Artifact application and later enemy debuff consumption")
    add("shared-duration", "SNAKE_PLANT", "Shockwave",
        "cards.red.Shockwave monsters.city.SnakePlant", [play()] + end(3),
        "Weak/Vulnerable application and decrement at actual turn boundaries")
    add("shared-draw-shuffle", "SNAKE_PLANT", "Pommel Strike",
        "cards.red.PommelStrike actions.GameActionManager", [play(0)] + end(2),
        "Damage precedes draw; subsequent turn exhausts draw pile and shuffles discard",
        draw=["Defend_R"])
    add("shared-generated-copy", "SNAKE_PLANT", "Anger",
        "cards.red.Anger", [play(0)] + end(2),
        "Stat-equivalent generated copy enters discard and later shuffle")
    add("shared-exhaust", "SNAKE_PLANT", "Fiend Fire",
        "cards.red.FiendFire actions.unique.FiendFireAction", [play(0)] + end(1),
        "Exhaust remaining hand through stock action and resolve one hit per exhausted card",
        hand=["Fiend Fire"] * 3)
    add("shared-double-tap", "SNAKE_PLANT", "Double Tap",
        "cards.red.DoubleTap powers.DoubleTapPower cards.red.TwinStrike",
        [play(), play(0)] + end(1),
        "Duplicate attack queue; mixed-hand ordering is itself compared",
        hand=["Double Tap", "Twin Strike"])
    add("shared-potion-flight", "THREE_BYRDS", "Defend_R",
        "monsters.city.Byrd powers.FlightPower potions.FirePotion", [{"kind": "potion", "potion_index": 0,
                                                   "target_index": 0}] + end(1),
        "Potion damage follows stock Flight interaction", potion="Fire Potion")
    add("shared-reaper-death", "SHELLED_PARASITE_AND_FUNGI", "Reaper",
        "cards.red.Reaper actions.unique.VampireDamageAllEnemiesAction monsters.exordium.FungiBeast",
        [play()] + end(1), "AoE actual damage, Fungi death debuff and missing-health healing order",
        hp=4500, monster_hp={"FUNGI_BEAST": 1})
    add("shared-corruption", "SNAKE_PLANT", "Corruption",
        "cards.red.Corruption", [play()] + end(1),
        "Power persists and changes next-turn skill costs without changing stored base costs")
    add("shared-hp-energy", "SNAKE_PLANT", "Bloodletting",
        "cards.red.Bloodletting", [play()] + end(1),
        "HP-loss action and energy gain; distinguish HP loss from normal attack damage", hp=4500)
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--identities", type=Path, nargs="+", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("immutable scene inventory already exists")
    identities = [json.loads(path.read_text(encoding="utf-8")) for path in args.identities]
    jars = {row["stock_jar_sha256"] for row in identities}
    if len(jars) != 1:
        raise ValueError("stock identities disagree")
    classes = {}
    for identity in identities:
        for name, row in identity["classes"].items():
            compact = {key: row[key] for key in ("class_sha256", "bytecode_sha256")}
            if name in classes and classes[name] != compact:
                raise ValueError("stock class sources disagree")
            classes[name] = compact
    rows = scenes()
    paths = list((ROOT / "docs/results").rglob("*.json")) + list((ROOT / "local/audits").rglob("*.json"))
    seeds = allocate_seeds(len(rows), recorded_seeds(paths))
    for row, allocation in zip(rows, seeds, strict=True):
        row["seeds"] = allocation
        row["source_evidence"] = {}
        for short in row["stock_classes"]:
            name = "com.megacrit.cardcrawl." + short
            row["source_evidence"][name] = classes[name]
    result = {"schema": "sls-fullrun-scenes-v1", "stock_jar_sha256": jars.pop(),
              "scenes": rows, "training_gate": "NOT_QUALIFIED", "final_holdout_used": False}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(result, stream, indent=2)
        stream.write("\n")
    print("12 isolated probes / 36 collision-checked seeds; no passing results inferred")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
