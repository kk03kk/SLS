"""Record acquisition routes without promoting native pool membership to stock parity."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend, native  # noqa: E402
from sls.content.registry import load_content_registry  # noqa: E402
from sls.content.scope import ironclad_scope  # noqa: E402
from sls.curriculum import IRONCLAD_A20_ACT1  # noqa: E402
from sls.rl.training_contract import native_artifact, sha256_file  # noqa: E402

PINNED_JAR = "cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673"
CARD_DIRECT = {
    "STRIKE_RED": "Ironclad starter deck", "DEFEND_RED": "Ironclad starter deck",
    "BASH": "Ironclad starter deck", "ASCENDERS_BANE": "A10+ starting curse",
    "BURN": "Hexaghost", "DAZED": "Sentries / Reckless Charge",
    "SLIMED": "Act1 slimes / Slime Boss", "WOUND": "Wild Strike / Power Through",
    "CURSE_OF_THE_BELL": "Neow boss swap -> Calling Bell -> onEquip",
}
CARD_LATER = {
    "APPARITION": "Ghosts (Act2)", "BITE": "Vampires (Act2)",
    "JAX": "Drug Dealer (Act2)", "RITUAL_DAGGER": "Nest (Act2)",
    "NECRONOMICURSE": "Cursed Tome (Act2) -> Necronomicon",
}
RELIC_DIRECT = {
    "BURNING_BLOOD": "Ironclad starter relic", "NEOWS_LAMENT": "Neow",
    "GOLDEN_IDOL": "Golden Idol event", "ODD_MUSHROOM": "Mushrooms event combat",
    "SPIRIT_POOP": "Bonfire Spirits: curse sacrifice", "WARPED_TONGS": "Accursed Blacksmith",
    **dict.fromkeys(("CULTIST_HEADPIECE", "FACE_OF_CLERIC", "GREMLIN_VISAGE",
                     "NLOTHS_HUNGRY_FACE", "SSSERPENT_HEAD"), "Face Trader"),
}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--stock-source-root", type=Path,
                        default=ROOT / "local/audits/stock-decompilation-tree/desktop-1.0/source/com/megacrit/cardcrawl")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise ValueError("use a new evidence file")
    if sha256_file(args.stock_jar) != PINNED_JAR:
        raise ValueError("stock JAR is not the reviewed authority")
    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("current native build required")
    source_files = (
        "dungeons/AbstractDungeon.java", "dungeons/Exordium.java", "dungeons/TheCity.java",
        "helpers/CardLibrary.java", "helpers/RelicLibrary.java", "helpers/PotionHelper.java", "neow/NeowReward.java",
        "characters/Ironclad.java", "relics/CallingBell.java", "relics/Necronomicon.java",
        "events/city/Ghosts.java", "events/city/Vampires.java", "events/city/DrugDealer.java",
        "events/city/Nest.java", "events/city/CursedTome.java",
        "events/shrines/FaceTrader.java", "events/shrines/Bonfire.java",
        "events/shrines/AccursedBlacksmith.java", "events/exordium/GoldenIdolEvent.java",
        "events/exordium/Mushrooms.java", "monsters/exordium/Hexaghost.java",
        "monsters/exordium/Sentry.java", "monsters/exordium/SlimeBoss.java",
        "cards/red/WildStrike.java", "cards/red/PowerThrough.java",
        "cards/red/RecklessCharge.java",
    )
    projections = {path: sha256_file(args.stock_source_root / path) for path in source_files}
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    backend.reset(0)
    pools = backend.raw_state["ordered_pools"]
    registry = load_content_registry().categories
    scope = ironclad_scope(20)
    card_pools = {
        kind: set(native.transform_selection_probe(0, kind, exclude)["pool"])
        for kind, exclude in (("colored", "BASH"), ("colorless", "APPARITION"),
                              ("curse", "ASCENDERS_BANE"))
    }
    cards = []
    for card_id in scope["cards"]["ids"]:
        if card_id in CARD_DIRECT:
            row = {"status": "DIRECT_ROUTE_REVIEWED_EFFECT_UNQUALIFIED", "routes": [CARD_DIRECT[card_id]]}
        elif card_id in CARD_LATER:
            row = {"status": "NO_ACT1_ROUTE_FOUND_NOT_EXHAUSTIVELY_EXCLUDED",
                   "routes": [CARD_LATER[card_id]],
                   "remaining": "Prove exclusion from every creation/transform/event route before removing the obligation."}
        else:
            memberships = [kind for kind, ids in card_pools.items() if card_id in ids]
            if not memberships:
                raise ValueError(f"unclassified card: {card_id}")
            row = {"status": "POOL_ROUTE_CANDIDATE", "pool_memberships": memberships,
                   "routes": {"colored": ["rewards", "shop", "Neow", "transform", "combat generation"],
                              "colorless": ["shop", "Neow colorless", "transform", "colorless combat generation"],
                              "curse": ["random curse", "curse transform", "events / Cursed Key"]}[memberships[0]]}
        row["source_references"] = ["helpers/CardLibrary.java", "dungeons/AbstractDungeon.java"]
        if card_id in CARD_DIRECT:
            row["source_references"].append({
                "STRIKE_RED": "characters/Ironclad.java", "DEFEND_RED": "characters/Ironclad.java",
                "BASH": "characters/Ironclad.java", "ASCENDERS_BANE": "dungeons/AbstractDungeon.java",
                "BURN": "monsters/exordium/Hexaghost.java", "DAZED": "monsters/exordium/Sentry.java",
                "SLIMED": "monsters/exordium/SlimeBoss.java", "WOUND": "cards/red/WildStrike.java",
                "CURSE_OF_THE_BELL": "relics/CallingBell.java",
            }[card_id])
        elif card_id in CARD_LATER:
            row["source_references"].append({
                "APPARITION": "events/city/Ghosts.java", "BITE": "events/city/Vampires.java",
                "JAX": "events/city/DrugDealer.java", "RITUAL_DAGGER": "events/city/Nest.java",
                "NECRONOMICURSE": "relics/Necronomicon.java",
            }[card_id])
        cards.append({"content_id": card_id, **row})
    relic_ordinals = {row["ordinal"]: row["id"] for row in registry["relics"]}
    relic_pools = {name: {relic_ordinals[item] for item in values}
                   for name, values in pools.items() if name.endswith("_relics")}
    relics = []
    for relic_id in scope["relics"]["ids"]:
        memberships = [name for name, ids in relic_pools.items() if relic_id in ids]
        if relic_id in RELIC_DIRECT:
            status, routes = "DIRECT_ROUTE_REVIEWED_EFFECT_UNQUALIFIED", [RELIC_DIRECT[relic_id]]
        elif memberships:
            status, routes = "POOL_ROUTE_CANDIDATE", memberships
        else:
            status, routes = "SPECIAL_OR_FALLBACK_ROUTE_UNRESOLVED", []
        relics.append({"content_id": relic_id, "status": status, "routes": routes,
                       "source_references": ["helpers/RelicLibrary.java", "dungeons/AbstractDungeon.java", "neow/NeowReward.java"],
                       "policy_acquisition": "EXCLUDED" if relic_id == "PRISMATIC_SHARD" else "NOT_EXCLUDED_BY_CONTENT_SCOPE",
                       "remaining": "Stock canSpawn, ownership, floor and acquisition effects remain branch obligations."})
    potions = [{"content_id": item, "status": "ORDERED_STOCK_POOL_REVIEWED_EFFECT_UNQUALIFIED",
                "routes": ["drops", "shop", "Neow", "Lab / Woman in Blue", "Entropic Brew"],
                "remaining": "Capacity, Sozu, rarity and event eligibility must be checked at each consumer."}
               for item in scope["potions"]["ids"]]
    result = {
        "schema": "sls-act1-acquisition-review-v1", "profile_id": "IRONCLAD_A20_ACT1",
        "stock_jar_sha256": PINNED_JAR, "native_source_sha256": artifact["source_sha256"],
        "native_artifact_sha256": artifact["sha256"], "scope_sha256": scope["scope_sha256"],
        "stock_projection_sha256": projections,
        "qualification": "Source review plus native inventory. Decompiled projections are identified, "
                         "not regenerated from the JAR here. Pool membership is not proof of stock "
                         "reachability, identical probabilities, effects or complete branch coverage. "
                         "No conservative audit target is removed by this report.",
        "assumptions": ["standard run without daily/custom modifiers", "unlocked Ironclad content", "declared Prismatic Shard restriction"],
        "categories": {"cards": cards, "relics": relics, "potions": potions},
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({category: {status: sum(row["status"] == status for row in rows)
                               for status in sorted({row["status"] for row in rows})}
                      for category, rows in result["categories"].items()}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
