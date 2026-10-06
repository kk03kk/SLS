"""Freeze reviewed stock-based controlled obligations, with bytecode identities."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def scenes() -> list[dict]:
    rows = []

    def add(identifier, encounter, classes, actions, **state):
        rows.append({"id": identifier, "encounter": encounter,
                     "stock_classes": classes.split(), "actions": actions,
                     "initial": {"hp": 5000, "energy": 10, "block": 0,
                                 "hand": ["Strike_R", "Defend_R"],
                                 "draw": ["Defend_R", "Strike_R"], **state},
                     "status": "READY_FOR_CONTROLLED_EXECUTION"})

    def end(n):
        return [{"kind": "end_turn"} for _ in range(n)]

    def hits(targets):
        return [{"kind": "play", "card_index": 1, "target_index": t} for t in targets]

    book = "monsters.city.BookOfStabbing powers.PainfulStabsPower"
    add("book-single", "BOOK_OF_STABBING", book, end(1),
        moves={"BOOK_OF_STABBING": {"stock": 2, "native": "BOOK_OF_STABBING_SINGLE_STAB",
                                    "damage": 24, "hits": 1}})
    add("book-growth", "BOOK_OF_STABBING", book, end(5))
    add("book-block-wound", "BOOK_OF_STABBING", book, end(2), block=30)
    byrds = "monsters.city.Byrd powers.FlightPower"
    add("byrd-knockdown", "THREE_BYRDS", byrds, hits([0] * 4) + end(2), hand=["Anger"] * 4)
    add("byrd-potion", "THREE_BYRDS", byrds,
        [{"kind": "potion", "potion_index": 0, "target_index": 0}] + end(1), potion="Fire Potion")
    add("byrd-independence", "THREE_BYRDS", byrds, hits([0, 1, 0, 2]) + end(1), hand=["Anger"] * 4)
    parasite = "monsters.city.ShelledParasite monsters.exordium.FungiBeast powers.PlatedArmorPower powers.SporeCloudPower"
    add("parasite-armor", "SHELLED_PARASITE_AND_FUNGI", parasite, hits([0] * 3) + end(1), hand=["Strike_R"] * 3)
    add("fungi-death", "SHELLED_PARASITE_AND_FUNGI", parasite, hits([1]) + end(1), monster_hp={"FUNGI_BEAST": 6})
    add("fungi-aoe-order", "SHELLED_PARASITE_AND_FUNGI", parasite,
        [{"kind": "play", "card_index": 1}] + end(1), hand=["Cleave"], monster_hp={"FUNGI_BEAST": 1})
    slavers = "monsters.exordium.SlaverBlue monsters.exordium.SlaverRed monsters.city.Taskmaster powers.EntanglePower"
    add("slavers-opener", "SLAVERS", slavers, end(2))
    add("slavers-entangle", "SLAVERS", slavers, end(3),
        moves={"RED_SLAVER": {"stock": 2, "native": "RED_SLAVER_ENTANGLE", "damage": 0, "hits": 0}})
    add("taskmaster-growth", "SLAVERS", slavers, end(5))
    plant = "monsters.city.SnakePlant powers.MalleablePower"
    add("plant-single", "SNAKE_PLANT", plant, hits([0]), hand=["Strike_R"])
    add("plant-multi", "SNAKE_PLANT", plant, hits([0, 0]), hand=["Twin Strike"] * 2)
    add("plant-reset", "SNAKE_PLANT", plant, hits([0, 0, 0]) + end(2), hand=["Strike_R"] * 3)
    for hp in (220, 219):
        add(f"champ-threshold-{hp}", "CHAMP", "monsters.city.Champ", end(3),
            monster_hp={"THE_CHAMP": hp})
    add("collector-cycle", "COLLECTOR", "monsters.city.TheCollector", end(10))
    add("collector-summon", "COLLECTOR", "monsters.city.TheCollector", end(4))
    automaton = "monsters.city.BronzeAutomaton monsters.city.BronzeOrb"
    add("automaton-cycle", "AUTOMATON", automaton, end(13))
    add("automaton-stasis", "AUTOMATON", automaton, end(4))
    for identifier, obligation in (("act-transition", "Act1->Act2 state and RNG continuation"),
                                   ("reward-collection", "stock combat reward collection"),
                                   ("checkpoint-continuation", "native checkpoint restoration and identical action replay")):
        rows.append({"id": identifier, "encounter": None, "stock_classes": ["dungeons.AbstractDungeon", "rooms.AbstractRoom"],
                     "obligation": obligation, "status": "SYSTEM_SCENARIO_PENDING"})
    for index, row in enumerate(rows):
        row["seeds"] = list(range(131100000 + index * 3, 131100003 + index * 3))
        row.update(ascension=20, act=2, floor=20)
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-identity", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("immutable scene inventory already exists")
    identity = json.loads(args.stock_identity.read_text(encoding="utf-8"))
    result = {"schema": "sls-act2-scenes-v1", "stock_jar_sha256": identity["stock_jar_sha256"],
              "stock_sources": identity["classes"], "scenes": scenes(),
              "training_gate": "NOT_QUALIFIED", "final_holdout_used": False}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
