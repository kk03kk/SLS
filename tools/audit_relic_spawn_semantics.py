"""Original/native Act-1 canSpawn evidence for scoped relics."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "tools"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport  # noqa: E402
from sls.backends.simulator import IRONCLAD_A0_ACT1, native  # noqa: E402
from sls.contracts import ScreenType  # noqa: E402
from sls.content.normalize import normalize_content_id  # noqa: E402
from sls.rl.training_contract import canonical_digest  # noqa: E402
from sls.validation.policies import deterministic_action  # noqa: E402


ARTIFACT = ROOT / "configs" / "validation" / "ironclad_a0_relic_semantics.json"
MECHANISM_ARTIFACT = ROOT / "configs" / "validation" / "ironclad_a0_mechanism_semantics.json"
BOTTLE_PRESET = {
    "BOTTLED_FLAME": "VALID_ATTACK",
    "BOTTLED_LIGHTNING": "VALID_SKILL",
    "BOTTLED_TORNADO": "VALID_POWER",
}
SHOP_SENSITIVE = {"MAW_BANK", "OLD_COIN", "SMILING_MASK", "THE_COURIER"}
CAMPFIRE = {"GIRYA", "PEACE_PIPE", "SHOVEL"}
BOTTLES = {"BOTTLED_FLAME", "BOTTLED_LIGHTNING", "BOTTLED_TORNADO"}
FLOOR_SENSITIVE = {
    "ANCIENT_TEA_SET", "CERAMIC_FISH", "DARKSTONE_PERIAPT", "DREAM_CATCHER",
    "FROZEN_EGG", "JUZU_BRACELET", "MATRYOSHKA", "MEAL_TICKET",
    "MEAT_ON_THE_BONE", "MOLTEN_EGG", "OMAMORI", "POTION_BELT",
    "PRAYER_WHEEL", "PRESERVED_INSECT", "QUESTION_CARD", "REGAL_PILLOW",
    "SINGING_BOWL", "TINY_CHEST", "TOXIC_EGG", "WING_BOOTS",
}
COUNTER_CASES = {
    "ANCIENT_TEA_SET": [-2],
    "DU_VU_DOLL": [0, 2],
    "LIZARD_TAIL": [-2],
    "MATRYOSHKA": [2, 1, -2],
    "MAW_BANK": [1, -2],
    "NEOWS_LAMENT": [3, 1, -2],
    "NLOTHS_HUNGRY_FACE": [1, -2],
    "OMAMORI": [2, 1, 0],
    "WING_BOOTS": [3, 1, -2],
}
REWARD_CALLBACKS = {
    "BUSTED_CROWN": "changeNumberOfCardsInReward",
    "QUESTION_CARD": "changeNumberOfCardsInReward",
    "NLOTHS_GIFT": "changeRareCardRewardChance",
}
HEAL_RELICS = {"MAGIC_FLOWER", "MARK_OF_THE_BLOOM"}
POLICY_NEUTRAL_CALLBACKS = {
    "ANCHOR": ("justEnteredRoom",),
    "FOSSILIZED_HELIX": ("justEnteredRoom",),
    "MEMBERSHIP_CARD": ("onEnterRoom",),
    "SMILING_MASK": ("onEnterRoom",),
    "THE_COURIER": ("onEnterRoom",),
    "BLACK_STAR": ("onEnterRoom", "onVictory"),
    "CENTENNIAL_PUZZLE": ("justEnteredRoom", "onVictory"),
    "CURSED_KEY": ("justEnteredRoom",),
    "STONE_CALENDAR": ("justEnteredRoom",),
}
VICTORY_COUNTER_RELICS = {
    "CAPTAINS_WHEEL", "HORN_CLEAT", "KUNAI", "LETTER_OPENER",
    "ORNAMENTAL_FAN", "POCKETWATCH", "SHURIKEN", "STONE_CALENDAR",
    "VELVET_CHOKER",
}
CAMPFIRE_CASES = {
    "COFFEE_DRIPPER": ("REST", "SMITH"),
    "FUSION_HAMMER": ("REST", "SMITH"),
    "GIRYA": ("AVAILABLE", "MAX"),
    "PEACE_PIPE": ("AVAILABLE", "EMPTY"),
    "SHOVEL": ("DEFAULT",),
}
CAMPFIRE_CALLBACK = {
    "COFFEE_DRIPPER": "canUseCampfireOption",
    "FUSION_HAMMER": "canUseCampfireOption",
    "GIRYA": "addCampfireOption",
    "PEACE_PIPE": "addCampfireOption",
    "SHOVEL": "addCampfireOption",
}
RESOURCE_CALLBACKS = {
    "BLOODY_IDOL": "onGainGold",
    "ETERNAL_FEATHER": "onEnterRoom",
    "SSSERPENT_HEAD": "onEnterRoom",
}
CARD_USE_FIELDS = {
    "BIRD_FACED_URN": ("hp_delta",),
    "BLUE_CANDLE": ("hp_delta", "exhaust"),
    "INK_BOTTLE": ("counter", "drawn"),
    "KUNAI": ("counter", "dexterity"),
    "LETTER_OPENER": ("counter", "monster_hp_delta"),
    "MEDICAL_KIT": ("exhaust",),
    "NUNCHAKU": ("counter", "energy_bonus"),
    "ORNAMENTAL_FAN": ("counter", "block_delta"),
    "PEN_NIB": ("counter", "pen_nib"),
    "SHURIKEN": ("counter", "strength"),
    "MUMMIFIED_HAND": ("zero_card",),
    "ORANGE_PELLETS": ("weak",),
    "NECRONOMICON": ("check_before", "check_after", "duplicated"),
}
BOOLEAN_CARD_USE_FIELDS = {"exhaust", "check_before", "check_after", "duplicated"}
STRING_CARD_USE_FIELDS = {"zero_card"}
OBTAIN_CARD_CALLBACKS = {
    "CERAMIC_FISH": ("onObtainCard",),
    "DARKSTONE_PERIAPT": ("onObtainCard",),
    "FROZEN_EGG": ("onObtainCard", "onPreviewObtainCard"),
    "MOLTEN_EGG": ("onObtainCard", "onPreviewObtainCard"),
    "TOXIC_EGG": ("onObtainCard", "onPreviewObtainCard"),
}
HP_LOSS_RELICS = {"CENTENNIAL_PUZZLE", "RUNIC_CUBE", "SELF_FORMING_CLAY"}
VICTORY_RESOURCE_CALLBACKS = {
    "BLACK_BLOOD": ("onVictory",),
    "BURNING_BLOOD": ("onVictory",),
    "FACE_OF_CLERIC": ("onVictory",),
    "MEAT_ON_THE_BONE": ("onBloodied", "onNotBloodied", "onTrigger"),
}
DAMAGE_CALLBACKS = {
    "STRIKE_DUMMY": "atDamageModify",
    "THE_BOOT": "onAttackToChangeDamage",
}
SHUFFLE_RELICS = {"SUNDIAL", "THE_ABACUS"}
SPECIAL_RESOURCE_CALLBACKS = {
    "TOY_ORNITHOPTER": ("onUsePotion",),
    "MAW_BANK": ("onEnterRoom", "onSpendGold"),
}
TURN_STATE_CALLBACKS = {
    "ANCIENT_TEA_SET": ("onEnterRestRoom",),
    "ART_OF_WAR": ("onUseCard", "onVictory"),
    "POCKETWATCH": ("onPlayCard",),
    "VELVET_CHOKER": ("canPlay", "onPlayCard"),
}
TURN_STATE_FIELDS = {
    "ANCIENT_TEA_SET": ("energy_delta",),
    "ART_OF_WAR": ("attack_bonus", "skill_bonus"),
    "POCKETWATCH": ("counter",),
    "VELVET_CHOKER": ("counter", "can_play"),
}
END_TURN_CALLBACKS = {
    "NILRYS_CODEX": ("onPlayerEndTurn",),
    "ORICHALCUM": ("onPlayerEndTurn", "onPlayerGainedBlock", "onVictory"),
    "STONE_CALENDAR": ("onPlayerEndTurn",),
    "SLAVERS_COLLAR": ("beforeEnergyPrep", "onVictory"),
}
END_TURN_FIELDS = {
    "NILRYS_CODEX": ("option_count",),
    "ORICHALCUM": ("block_delta", "rounded_block"),
    "STONE_CALENDAR": ("monster_hp_delta",),
    "SLAVERS_COLLAR": ("elite_energy_bonus", "persistent_energy_delta"),
}
TRIGGER_CALLBACKS = {
    "CHAMPION_BELT": ("onTrigger",),
    "CHARONS_ASHES": ("onExhaust",),
    "DEAD_BRANCH": ("onExhaust",),
    "GREMLIN_HORN": ("onMonsterDeath",),
    "HAND_DRILL": ("onBlockBroken",),
    "LIZARD_TAIL": ("onTrigger",),
    "RED_SKULL": ("onBloodied", "onNotBloodied", "onVictory"),
    "UNCEASING_TOP": ("onRefreshHand",),
}
TRIGGER_FIELDS = {
    "CHAMPION_BELT": ("weak",),
    "CHARONS_ASHES": ("monster_hp_delta",),
    "DEAD_BRANCH": ("hand_delta",),
    "GREMLIN_HORN": ("hand_delta", "energy_delta"),
    "HAND_DRILL": ("vulnerable",),
    "LIZARD_TAIL": ("hp_after", "counter"),
    "RED_SKULL": ("strength_on", "strength_after"),
    "UNCEASING_TOP": ("hand_delta",),
}
WORLD_CALLBACKS = {
    "CURSED_KEY": ("onChestOpen",),
    "DU_VU_DOLL": ("onMasterDeckChange",),
    "MATRYOSHKA": ("onChestOpen",),
    "MEAL_TICKET": ("justEnteredRoom",),
    "NLOTHS_HUNGRY_FACE": ("onChestOpenAfter",),
}
WORLD_FIELDS = {
    "CURSED_KEY": ("delta",),
    "DU_VU_DOLL": ("value",),
    "MATRYOSHKA": ("delta", "value"),
    "MEAL_TICKET": ("delta",),
    "NLOTHS_HUNGRY_FACE": ("delta", "used"),
}
EQUIP_CALLBACKS = {
    "ASTROLABE": ("onEquip",),
    "BOTTLED_FLAME": ("onEquip", "onUnequip"),
    "BOTTLED_LIGHTNING": ("onEquip", "onUnequip"),
    "BOTTLED_TORNADO": ("onEquip", "onUnequip"),
    "CALLING_BELL": ("onEquip",),
    "CAULDRON": ("onEquip",),
    "DOLLYS_MIRROR": ("onEquip",),
    "EMPTY_CAGE": ("onEquip",),
    "ORRERY": ("onEquip",),
    "PANDORAS_BOX": ("onEquip",),
    "TINY_HOUSE": ("onEquip",),
}
EQUIP_FIELDS = {
    "ASTROLABE": ("option_count", "affected"),
    "BOTTLED_FLAME": ("option_count", "affected", "marked", "unmarked"),
    "BOTTLED_LIGHTNING": ("option_count", "affected", "marked", "unmarked"),
    "BOTTLED_TORNADO": ("option_count", "affected", "marked", "unmarked"),
    "CALLING_BELL": ("affected", "relic_rewards"),
    "CAULDRON": ("potion_rewards",),
    "DOLLYS_MIRROR": ("option_count", "affected"),
    "EMPTY_CAGE": ("option_count", "affected"),
    "ORRERY": ("card_rewards",),
    "PANDORAS_BOX": ("affected",),
    "TINY_HOUSE": (
        "max_hp_delta", "upgraded_delta", "gold_reward",
        "potion_rewards", "card_rewards",
    ),
}


def cases(relic_id: str) -> list[tuple[int, bool, str]]:
    if relic_id in BOTTLE_PRESET:
        return [(1, False, "NONE"), (1, False, BOTTLE_PRESET[relic_id])]
    if relic_id == "BLACK_BLOOD":
        return [(1, False, "NONE"), (1, False, "BURNING_BLOOD")]
    if relic_id in CAMPFIRE:
        return [(1, False, "NONE"), (1, False, "CAMPFIRE_TWO")]
    if relic_id in SHOP_SENSITIVE:
        return [(1, False, "NONE"), (1, True, "NONE")]
    if relic_id in FLOOR_SENSITIVE:
        return [(1, False, "NONE"), (17, False, "NONE")]
    return [(1, False, "NONE")]


def capture(seed: int, artifact: Path) -> dict[str, Any]:
    payload = json.loads(artifact.read_text(encoding="utf-8"))
    entries = list(payload["entries"])
    mechanism = json.loads(MECHANISM_ARTIFACT.read_text(encoding="utf-8"))
    damage_evidence = next(
        item for item in mechanism["entries"] if item["id"] == "damage_buffer_intangible"
    )
    for relic_id, callback in (("TORII", "onAttacked"), ("TUNGSTEN_ROD", "onLoseHpLast")):
        entry = next(item for item in entries if item["id"] == relic_id)
        scenario = {
            "callback": callback,
            "scope": "MECHANISM_DAMAGE_BUFFER_INTANGIBLE",
            "artifact": MECHANISM_ARTIFACT.relative_to(ROOT).as_posix(),
            "artifact_sha256": mechanism["audit_sha256"],
            "effect_sha256": damage_evidence["effect_sha256"],
        }
        existing = next(
            (item for item in entry.get("callback_scenarios", ())
             if item.get("callback") == callback
             and item.get("scope") == "MECHANISM_DAMAGE_BUFFER_INTANGIBLE"),
            None,
        )
        if existing is not None:
            existing.clear()
            existing.update(scenario)
            continue
        entry.setdefault("callback_scenarios", []).append(scenario)
        entry["remaining_callbacks"].remove(callback)
        entry["covered_callbacks"] = sorted(entry["covered_callbacks"] + [callback])
        entry["callback_complete"] = not entry["remaining_callbacks"]
    philosopher = next(item for item in entries if item["id"] == "PHILOSOPHERS_STONE")
    if "onSpawnMonster" in philosopher["remaining_callbacks"]:
        philosopher.setdefault("callback_scenarios", []).append({
            "callback": "onSpawnMonster",
            "scope": "FIRST_TURN_STOCK_MONSTER_LIFECYCLE",
            "effect_sha256": philosopher["effect_sha256"],
            "setup_digest": philosopher["setup_digest"],
        })
        philosopher["remaining_callbacks"].remove("onSpawnMonster")
        philosopher["covered_callbacks"] = sorted(
            philosopher["covered_callbacks"] + ["onSpawnMonster"]
        )
        philosopher["callback_complete"] = not philosopher["remaining_callbacks"]
    targets = [item for item in entries if "canSpawn" in item["remaining_callbacks"]]
    unequip_targets = [
        item for item in entries
        if "onUnequip" in item["remaining_callbacks"] and item["id"] not in BOTTLES
    ]
    counter_targets = [
        item for item in entries if "setCounter" in item["remaining_callbacks"]
    ]
    reward_targets = [
        item for item in entries
        if item["id"] in REWARD_CALLBACKS
        and REWARD_CALLBACKS[item["id"]] in item["remaining_callbacks"]
    ]
    heal_targets = [
        item for item in entries
        if item["id"] in HEAL_RELICS and "onPlayerHeal" in item["remaining_callbacks"]
    ]
    neutral_targets = [
        (entry, callback)
        for entry in entries
        for callback in POLICY_NEUTRAL_CALLBACKS.get(str(entry["id"]), ())
        if callback in entry["remaining_callbacks"]
    ]
    victory_targets = [
        entry for entry in entries
        if entry["id"] in VICTORY_COUNTER_RELICS
        and "onVictory" in entry["remaining_callbacks"]
    ]
    campfire_targets = [
        entry for entry in entries
        if entry["id"] in CAMPFIRE_CASES
        and CAMPFIRE_CALLBACK[entry["id"]] in entry["remaining_callbacks"]
    ]
    resource_targets = [
        entry for entry in entries
        if entry["id"] in RESOURCE_CALLBACKS
        and RESOURCE_CALLBACKS[entry["id"]] in entry["remaining_callbacks"]
    ]
    card_use_targets = [
        entry for entry in entries
        if entry["id"] in CARD_USE_FIELDS
        and "onUseCard" in entry["remaining_callbacks"]
    ]
    obtain_card_targets = [
        entry for entry in entries
        if entry["id"] in OBTAIN_CARD_CALLBACKS
        and any(
            callback in entry["remaining_callbacks"]
            for callback in OBTAIN_CARD_CALLBACKS[entry["id"]]
        )
    ]
    hp_loss_targets = [
        entry for entry in entries
        if entry["id"] in HP_LOSS_RELICS
        and "wasHPLost" in entry["remaining_callbacks"]
    ]
    victory_resource_targets = [
        entry for entry in entries
        if entry["id"] in VICTORY_RESOURCE_CALLBACKS
        and any(
            callback in entry["remaining_callbacks"]
            for callback in VICTORY_RESOURCE_CALLBACKS[entry["id"]]
        )
    ]
    damage_targets = [
        entry for entry in entries
        if entry["id"] in DAMAGE_CALLBACKS
        and DAMAGE_CALLBACKS[entry["id"]] in entry["remaining_callbacks"]
    ]
    shuffle_targets = [
        entry for entry in entries
        if entry["id"] in SHUFFLE_RELICS
        and "onShuffle" in entry["remaining_callbacks"]
    ]
    special_resource_targets = [
        entry for entry in entries
        if entry["id"] in SPECIAL_RESOURCE_CALLBACKS
        and any(
            callback in entry["remaining_callbacks"]
            for callback in SPECIAL_RESOURCE_CALLBACKS[entry["id"]]
        )
    ]
    turn_state_targets = [
        entry for entry in entries
        if entry["id"] in TURN_STATE_CALLBACKS
        and any(
            callback in entry["remaining_callbacks"]
            for callback in TURN_STATE_CALLBACKS[entry["id"]]
        )
    ]
    end_turn_targets = [
        entry for entry in entries
        if entry["id"] in END_TURN_CALLBACKS
        and any(
            callback in entry["remaining_callbacks"]
            for callback in END_TURN_CALLBACKS[entry["id"]]
        )
    ]
    trigger_targets = [
        entry for entry in entries
        if entry["id"] in TRIGGER_CALLBACKS
        and any(
            callback in entry["remaining_callbacks"]
            for callback in TRIGGER_CALLBACKS[entry["id"]]
        )
    ]
    world_targets = [
        entry for entry in entries
        if entry["id"] in WORLD_CALLBACKS
        and any(
            callback in entry["remaining_callbacks"]
            for callback in WORLD_CALLBACKS[entry["id"]]
        )
    ]
    equip_targets = [
        entry for entry in entries
        if entry["id"] in EQUIP_CALLBACKS
        and any(
            callback in entry["remaining_callbacks"]
            for callback in EQUIP_CALLBACKS[entry["id"]]
        )
    ]
    session = OriginalSession(StdioTransport())
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    try:
        decision = backend.reset(seed)
        for _ in range(40):
            if decision.observation.screen is ScreenType.COMBAT:
                break
            decision = backend.step(deterministic_action(decision, decision)).decision
        else:
            raise RuntimeError("Original did not reach first combat")

        for index, entry in enumerate(targets, 1):
            relic_id = str(entry["id"])
            evidence = []
            for floor, shop, preset in cases(relic_id):
                original = session.execute(
                    f"parity_relic_spawn {relic_id} {floor} {str(shop).lower()} {preset}"
                )
                scenario = dict(original.get("_parity_scenario") or {})
                expected = str(scenario.get("spawn_result") or "").lower() == "true"
                battle = native.LightspeedBattle()
                actual = bool(battle.relic_can_spawn_probe(
                    seed, relic_id, floor, shop, preset,
                ))
                if expected != actual:
                    raise RuntimeError(
                        f"canSpawn mismatch: {relic_id} floor={floor} "
                        f"shop={shop} preset={preset}: Original={expected} native={actual}"
                    )
                row = {
                    "floor": floor, "shop_room": shop, "preset": preset,
                    "result": expected,
                    "setup_digest": str(scenario.get("setup_digest") or ""),
                }
                row["effect_sha256"] = canonical_digest(row)
                evidence.append(row)
            entry.setdefault("callback_scenarios", []).append({
                "callback": "canSpawn", "scope": "ACT1", "cases": evidence,
                "effect_sha256": canonical_digest(
                    [item["effect_sha256"] for item in evidence]
                ),
            })
            entry["remaining_callbacks"].remove("canSpawn")
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"] + ["canSpawn"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(f"RELIC_SPAWN_AUDIT {index}/{len(targets)} {relic_id}", file=sys.stderr)

        for index, entry in enumerate(unequip_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_unequip {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                "energy_delta": int(scenario["energy_delta"]),
                "hand_delta": int(scenario["hand_delta"]),
                "necronomicurse_count": int(scenario["necronomicurse_count"]),
            }
            battle = native.LightspeedBattle()
            actual = dict(battle.relic_unequip_probe(seed, relic_id))
            if expected != actual:
                raise RuntimeError(
                    f"onUnequip mismatch: {relic_id}: Original={expected} native={actual}"
                )
            evidence = {
                "callback": "onUnequip", "scope": "PERSISTENT_NEXT_COMBAT",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove("onUnequip")
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"] + ["onUnequip"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(f"RELIC_UNEQUIP_AUDIT {index}/{len(unequip_targets)} {relic_id}", file=sys.stderr)

        for index, entry in enumerate(counter_targets, 1):
            relic_id = str(entry["id"])
            rows = []
            for value in COUNTER_CASES[relic_id]:
                original = session.execute(f"parity_relic_counter {relic_id} {value}")
                scenario = dict(original.get("_parity_scenario") or {})
                expected = int(scenario["counter"])
                battle = native.LightspeedBattle()
                actual = int(battle.relic_counter_probe(relic_id, value))
                if expected != actual:
                    raise RuntimeError(
                        f"setCounter mismatch: {relic_id} value={value}: "
                        f"Original={expected} native={actual}"
                    )
                row = {"input": value, "counter": expected}
                row["effect_sha256"] = canonical_digest(row)
                rows.append(row)
            evidence = {
                "callback": "setCounter", "scope": "REACHABLE_COUNTER_VALUES",
                "cases": rows,
                "effect_sha256": canonical_digest([row["effect_sha256"] for row in rows]),
            }
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove("setCounter")
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"] + ["setCounter"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(f"RELIC_COUNTER_AUDIT {index}/{len(counter_targets)} {relic_id}", file=sys.stderr)

        for index, entry in enumerate(reward_targets, 1):
            relic_id = str(entry["id"])
            callback = REWARD_CALLBACKS[relic_id]
            original = session.execute(f"parity_relic_reward {relic_id} 3")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = int(scenario["result"])
            battle = native.LightspeedBattle()
            actual = int(battle.relic_reward_scalar_probe(relic_id, 3))
            if expected != actual:
                raise RuntimeError(
                    f"{callback} mismatch: {relic_id}: Original={expected} native={actual}"
                )
            evidence = {
                "callback": callback, "scope": "ACT1_CARD_REWARD",
                "input": 3, "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"] + [callback])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(f"RELIC_REWARD_AUDIT {index}/{len(reward_targets)} {relic_id}", file=sys.stderr)

        for index, entry in enumerate(heal_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_heal {relic_id} 10")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = int(scenario["result"])
            battle = native.LightspeedBattle()
            actual = int(battle.relic_heal_probe(seed, relic_id, 10))
            if expected != actual:
                raise RuntimeError(
                    f"onPlayerHeal mismatch: {relic_id}: Original={expected} native={actual}"
                )
            evidence = {
                "callback": "onPlayerHeal", "scope": "COMBAT_HEAL",
                "input": 10, "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove("onPlayerHeal")
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"] + ["onPlayerHeal"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(f"RELIC_HEAL_AUDIT {index}/{len(heal_targets)} {relic_id}", file=sys.stderr)

        for index, (entry, callback) in enumerate(neutral_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(
                f"parity_relic_neutral {relic_id} {callback}"
            )
            scenario = dict(original.get("_parity_scenario") or {})
            expected = str(scenario.get("policy_state_unchanged") or "").lower() == "true"
            battle = native.LightspeedBattle()
            actual = bool(battle.relic_policy_neutral_probe(relic_id, callback))
            if not expected or actual != expected:
                raise RuntimeError(
                    f"policy-neutral mismatch: {relic_id}.{callback}: "
                    f"Original={expected} native={actual}"
                )
            evidence = {
                "callback": callback,
                "scope": "POLICY_OBSERVABLE_STATE",
                "result": {"policy_state_unchanged": True},
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove(callback)
            entry["covered_callbacks"] = sorted(
                entry["covered_callbacks"] + [callback]
            )
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_NEUTRAL_AUDIT {index}/{len(neutral_targets)} "
                f"{relic_id}.{callback}", file=sys.stderr,
            )

        for index, entry in enumerate(victory_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_victory {relic_id} 2")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = int(scenario["counter"])
            battle = native.LightspeedBattle()
            actual = int(battle.relic_victory_counter_probe(relic_id, 2))
            if expected != actual:
                raise RuntimeError(
                    f"onVictory counter mismatch: {relic_id}: "
                    f"Original={expected} native={actual}"
                )
            evidence = {
                "callback": "onVictory", "scope": "COMBAT_VICTORY_RESET",
                "initial_counter": 2, "counter": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove("onVictory")
            entry["covered_callbacks"] = sorted(
                entry["covered_callbacks"] + ["onVictory"]
            )
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_VICTORY_AUDIT {index}/{len(victory_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(campfire_targets, 1):
            relic_id = str(entry["id"])
            callback = CAMPFIRE_CALLBACK[relic_id]
            rows = []
            for preset in CAMPFIRE_CASES[relic_id]:
                original = session.execute(
                    f"parity_relic_campfire {relic_id} {preset}"
                )
                scenario = dict(original.get("_parity_scenario") or {})
                expected = {
                    "result": str(scenario["result"]).lower() == "true",
                    "usable": str(scenario["usable"]).lower() == "true",
                    "option_type": str(scenario["option_type"]),
                }
                battle = native.LightspeedBattle()
                actual = dict(battle.relic_campfire_probe(relic_id, preset))
                if expected != actual:
                    raise RuntimeError(
                        f"{callback} mismatch: {relic_id} preset={preset}: "
                        f"Original={expected} native={actual}"
                    )
                row = {"preset": preset, **expected}
                row["effect_sha256"] = canonical_digest(row)
                rows.append(row)
            evidence = {
                "callback": callback, "scope": "ACT1_CAMPFIRE_OPTIONS",
                "cases": rows,
                "effect_sha256": canonical_digest(
                    [row["effect_sha256"] for row in rows]
                ),
            }
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove(callback)
            entry["covered_callbacks"] = sorted(
                entry["covered_callbacks"] + [callback]
            )
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_CAMPFIRE_AUDIT {index}/{len(campfire_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(resource_targets, 1):
            relic_id = str(entry["id"])
            callback = RESOURCE_CALLBACKS[relic_id]
            original = session.execute(f"parity_relic_resource {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                "hp_delta": int(scenario["hp_delta"]),
                "gold_delta": int(scenario["gold_delta"]),
            }
            battle = native.LightspeedBattle()
            actual = dict(battle.relic_resource_probe(seed, relic_id))
            if expected != actual:
                raise RuntimeError(
                    f"{callback} mismatch: {relic_id}: "
                    f"Original={expected} native={actual}"
                )
            evidence = {
                "callback": callback, "scope": "ACT1_RESOURCE_TRANSITION",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove(callback)
            entry["covered_callbacks"] = sorted(
                entry["covered_callbacks"] + [callback]
            )
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_RESOURCE_AUDIT {index}/{len(resource_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(card_use_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_card_use {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                field: (
                    str(scenario[field]).lower() == "true"
                    if field in BOOLEAN_CARD_USE_FIELDS
                    else str(scenario[field])
                    if field in STRING_CARD_USE_FIELDS
                    else int(scenario[field])
                )
                for field in CARD_USE_FIELDS[relic_id]
            }
            battle = native.LightspeedBattle()
            native_result = dict(battle.relic_card_use_probe(seed, relic_id))
            actual = {field: native_result[field] for field in CARD_USE_FIELDS[relic_id]}
            for field in STRING_CARD_USE_FIELDS & set(CARD_USE_FIELDS[relic_id]):
                expected[field] = normalize_content_id(expected[field])
                actual[field] = normalize_content_id(str(actual[field]))
            if expected != actual:
                raise RuntimeError(
                    f"onUseCard mismatch: {relic_id}: "
                    f"Original={expected} native={actual}"
                )
            evidence = {
                "callback": "onUseCard", "scope": "FOCUSED_LEGAL_CARD_SEQUENCE",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove("onUseCard")
            entry["covered_callbacks"] = sorted(
                entry["covered_callbacks"] + ["onUseCard"]
            )
            if relic_id == "NECRONOMICON" and "checkTrigger" in entry["remaining_callbacks"]:
                entry["remaining_callbacks"].remove("checkTrigger")
                entry["covered_callbacks"] = sorted(
                    entry["covered_callbacks"] + ["checkTrigger"]
                )
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_CARD_USE_AUDIT {index}/{len(card_use_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(obtain_card_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_obtain_card {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            fields = (
                "obtain_upgrades", "preview_upgrades", "hp_delta",
                "max_hp_delta", "gold_delta",
            )
            expected = {field: int(scenario[field]) for field in fields}
            battle = native.LightspeedBattle()
            actual = dict(battle.relic_obtain_card_probe(seed, relic_id))
            if expected != actual:
                raise RuntimeError(
                    f"obtain-card mismatch: {relic_id}: "
                    f"Original={expected} native={actual}"
                )
            callbacks = [
                callback for callback in OBTAIN_CARD_CALLBACKS[relic_id]
                if callback in entry["remaining_callbacks"]
            ]
            evidence = {
                "callbacks": callbacks, "scope": "FOCUSED_CARD_ACQUISITION",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            for callback in callbacks:
                entry["remaining_callbacks"].remove(callback)
                entry["covered_callbacks"].append(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_OBTAIN_CARD_AUDIT {index}/{len(obtain_card_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(hp_loss_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_hp_loss {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                field: int(scenario[field])
                for field in ("hp_delta", "drawn", "next_turn_block")
            }
            battle = native.LightspeedBattle()
            actual = dict(battle.relic_hp_loss_probe(seed, relic_id))
            if expected != actual:
                raise RuntimeError(
                    f"wasHPLost mismatch: {relic_id}: "
                    f"Original={expected} native={actual}"
                )
            evidence = {
                "callback": "wasHPLost", "scope": "FOCUSED_COMBAT_HP_LOSS",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove("wasHPLost")
            entry["covered_callbacks"] = sorted(
                entry["covered_callbacks"] + ["wasHPLost"]
            )
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_HP_LOSS_AUDIT {index}/{len(hp_loss_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(victory_resource_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_victory_resource {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                "hp_delta": int(scenario["hp_delta"]),
                "max_hp_delta": int(scenario["max_hp_delta"]),
            }
            battle = native.LightspeedBattle()
            actual = dict(battle.relic_victory_resource_probe(seed, relic_id))
            if expected != actual:
                raise RuntimeError(
                    f"victory-resource mismatch: {relic_id}: "
                    f"Original={expected} native={actual}"
                )
            callbacks = [
                callback for callback in VICTORY_RESOURCE_CALLBACKS[relic_id]
                if callback in entry["remaining_callbacks"]
            ]
            evidence = {
                "callbacks": callbacks, "scope": "FOCUSED_COMBAT_VICTORY",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            for callback in callbacks:
                entry["remaining_callbacks"].remove(callback)
                entry["covered_callbacks"].append(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_VICTORY_RESOURCE_AUDIT {index}/"
                f"{len(victory_resource_targets)} {relic_id}", file=sys.stderr,
            )

        for index, entry in enumerate(damage_targets, 1):
            relic_id = str(entry["id"])
            callback = DAMAGE_CALLBACKS[relic_id]
            original = session.execute(f"parity_relic_damage {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = int(scenario["damage"])
            battle = native.LightspeedBattle()
            actual = int(battle.relic_damage_probe(seed, relic_id))
            if expected != actual:
                raise RuntimeError(
                    f"damage modifier mismatch: {relic_id}: "
                    f"Original={expected} native={actual}"
                )
            evidence = {
                "callback": callback, "scope": "FOCUSED_ATTACK_DAMAGE",
                "damage": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"] + [callback])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(f"RELIC_DAMAGE_AUDIT {index}/{len(damage_targets)} {relic_id}", file=sys.stderr)

        for index, entry in enumerate(shuffle_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_shuffle {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                field: int(scenario[field])
                for field in ("counter", "energy_delta", "block_delta")
            }
            battle = native.LightspeedBattle()
            actual = dict(battle.relic_shuffle_probe(seed, relic_id))
            if expected != actual:
                raise RuntimeError(
                    f"onShuffle mismatch: {relic_id}: Original={expected} native={actual}"
                )
            evidence = {
                "callback": "onShuffle", "scope": "FOCUSED_DRAW_PILE_SHUFFLE",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            entry["remaining_callbacks"].remove("onShuffle")
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"] + ["onShuffle"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(f"RELIC_SHUFFLE_AUDIT {index}/{len(shuffle_targets)} {relic_id}", file=sys.stderr)

        for index, entry in enumerate(special_resource_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_special_resource {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                "toy_heal": int(scenario["toy_heal"]),
                "first_gain": int(scenario["first_gain"]),
                "second_gain": int(scenario["second_gain"]),
                "used": str(scenario["used"]).lower() == "true",
            }
            battle = native.LightspeedBattle()
            actual = dict(battle.relic_special_resource_probe(seed, relic_id))
            if expected != actual:
                raise RuntimeError(
                    f"special resource mismatch: {relic_id}: "
                    f"Original={expected} native={actual}"
                )
            callbacks = [
                callback for callback in SPECIAL_RESOURCE_CALLBACKS[relic_id]
                if callback in entry["remaining_callbacks"]
            ]
            evidence = {
                "callbacks": callbacks, "scope": "FOCUSED_RESOURCE_LIFECYCLE",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            for callback in callbacks:
                entry["remaining_callbacks"].remove(callback)
                entry["covered_callbacks"].append(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_SPECIAL_RESOURCE_AUDIT {index}/"
                f"{len(special_resource_targets)} {relic_id}", file=sys.stderr,
            )

        for index, entry in enumerate(turn_state_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_turn_state {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                field: (
                    str(scenario[field]).lower() == "true"
                    if field == "can_play" else int(scenario[field])
                )
                for field in TURN_STATE_FIELDS[relic_id]
            }
            battle = native.LightspeedBattle()
            native_result = dict(battle.relic_turn_state_probe(seed, relic_id))
            actual = {field: native_result[field] for field in TURN_STATE_FIELDS[relic_id]}
            if expected != actual:
                raise RuntimeError(
                    f"turn-state mismatch: {relic_id}: Original={expected} native={actual}"
                )
            callbacks = [
                callback for callback in TURN_STATE_CALLBACKS[relic_id]
                if callback in entry["remaining_callbacks"]
            ]
            evidence = {
                "callbacks": callbacks, "scope": "FOCUSED_TURN_LIFECYCLE",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            for callback in callbacks:
                entry["remaining_callbacks"].remove(callback)
                entry["covered_callbacks"].append(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_TURN_STATE_AUDIT {index}/{len(turn_state_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(end_turn_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_end_turn {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                field: int(scenario[field]) for field in END_TURN_FIELDS[relic_id]
            }
            battle = native.LightspeedBattle()
            native_result = dict(battle.relic_end_turn_probe(seed, relic_id))
            actual = {field: native_result[field] for field in END_TURN_FIELDS[relic_id]}
            if expected != actual:
                raise RuntimeError(
                    f"end-turn mismatch: {relic_id}: Original={expected} native={actual}"
                )
            callbacks = [
                callback for callback in END_TURN_CALLBACKS[relic_id]
                if callback in entry["remaining_callbacks"]
            ]
            evidence = {
                "callbacks": callbacks, "scope": "FOCUSED_END_TURN_LIFECYCLE",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            for callback in callbacks:
                entry["remaining_callbacks"].remove(callback)
                entry["covered_callbacks"].append(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_END_TURN_AUDIT {index}/{len(end_turn_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(trigger_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_trigger {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                field: int(scenario[field]) for field in TRIGGER_FIELDS[relic_id]
            }
            battle = native.LightspeedBattle()
            native_result = dict(battle.relic_trigger_probe(seed, relic_id))
            actual = {field: native_result[field] for field in TRIGGER_FIELDS[relic_id]}
            if expected != actual:
                raise RuntimeError(
                    f"trigger mismatch: {relic_id}: Original={expected} native={actual}"
                )
            callbacks = [
                callback for callback in TRIGGER_CALLBACKS[relic_id]
                if callback in entry["remaining_callbacks"]
            ]
            evidence = {
                "callbacks": callbacks, "scope": "FOCUSED_TRIGGER_LIFECYCLE",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            for callback in callbacks:
                entry["remaining_callbacks"].remove(callback)
                entry["covered_callbacks"].append(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_TRIGGER_AUDIT {index}/{len(trigger_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(world_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_world {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                field: (
                    str(scenario[field]).lower() == "true"
                    if field == "used" else int(scenario[field])
                )
                for field in WORLD_FIELDS[relic_id]
            }
            battle = native.LightspeedBattle()
            native_result = dict(battle.relic_world_probe(seed, relic_id))
            actual = {field: native_result[field] for field in WORLD_FIELDS[relic_id]}
            if expected != actual:
                raise RuntimeError(
                    f"world mismatch: {relic_id}: Original={expected} native={actual}"
                )
            callbacks = [
                callback for callback in WORLD_CALLBACKS[relic_id]
                if callback in entry["remaining_callbacks"]
            ]
            evidence = {
                "callbacks": callbacks, "scope": "FOCUSED_WORLD_LIFECYCLE",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            for callback in callbacks:
                entry["remaining_callbacks"].remove(callback)
                entry["covered_callbacks"].append(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_WORLD_AUDIT {index}/{len(world_targets)} {relic_id}",
                file=sys.stderr,
            )

        for index, entry in enumerate(equip_targets, 1):
            relic_id = str(entry["id"])
            original = session.execute(f"parity_relic_equip {relic_id}")
            scenario = dict(original.get("_parity_scenario") or {})
            expected = {
                field: int(scenario[field]) for field in EQUIP_FIELDS[relic_id]
            }
            battle = native.LightspeedBattle()
            native_result = dict(battle.relic_equip_probe(seed, relic_id))
            actual = {field: native_result[field] for field in EQUIP_FIELDS[relic_id]}
            if expected != actual:
                raise RuntimeError(
                    f"equip mismatch: {relic_id}: Original={expected} native={actual}"
                )
            callbacks = [
                callback for callback in EQUIP_CALLBACKS[relic_id]
                if callback in entry["remaining_callbacks"]
            ]
            evidence = {
                "callbacks": callbacks, "scope": "FULL_EQUIP_UI_LIFECYCLE",
                "result": expected,
                "setup_digest": str(scenario.get("setup_digest") or ""),
            }
            evidence["effect_sha256"] = canonical_digest(evidence)
            entry.setdefault("callback_scenarios", []).append(evidence)
            for callback in callbacks:
                entry["remaining_callbacks"].remove(callback)
                entry["covered_callbacks"].append(callback)
            entry["covered_callbacks"] = sorted(entry["covered_callbacks"])
            entry["callback_complete"] = not entry["remaining_callbacks"]
            print(
                f"RELIC_EQUIP_AUDIT {index}/{len(equip_targets)} {relic_id}",
                file=sys.stderr,
            )
    finally:
        backend.return_to_menu()

    payload.pop("audit_sha256", None)
    payload["audit_sha256"] = canonical_digest(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--artifact", type=Path, default=ARTIFACT)
    args = parser.parse_args()
    payload = capture(args.seed, args.artifact)
    args.artifact.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        code = main()
    except BaseException as error:
        write_completion(2, entry="relic-spawn-audit", error=f"{type(error).__name__}: {error}", argv=sys.argv)
        raise
    else:
        write_completion(code, entry="relic-spawn-audit")
        raise SystemExit(code)
