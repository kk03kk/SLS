"""Deterministic extraction of stock content metadata for semantic audits."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
from typing import Iterable

from sls.content import load_content_registry
from sls.content.scope import ROOT


JAVA_ROOT = ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl"


@dataclass(frozen=True, slots=True)
class JavaSource:
    path: Path
    text: str


def _java_index(directory: str, field: str) -> dict[str, JavaSource]:
    result: dict[str, JavaSource] = {}
    pattern = re.compile(
        rf"public static final String {re.escape(field)}\s*=\s*\"([^\"]+)\""
    )
    for path in (JAVA_ROOT / directory).rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        match = pattern.search(text)
        if match:
            result[match.group(1)] = JavaSource(path, text)
    return result


def java_sources(category: str) -> dict[str, JavaSource]:
    if category == "cards":
        return _java_index("cards", "ID")
    if category == "potions":
        return _java_index("potions", "POTION_ID")
    if category == "relics":
        return _java_index("relics", "ID")
    if category == "events":
        result = _java_index("events", "ID")
        aliases = {
            "Face Trader": "FaceTrader",
            "Match and Keep": "Match and Keep!",
            "Mindbloom": "MindBloom",
            "Nloth": "N'loth",
            "Note For Yourself": "NoteForYourself",
            "Secret Portal": "SecretPortal",
            "Sensory Stone": "SensoryStone",
        }
        for registry_id, java_id in aliases.items():
            result[registry_id] = result[java_id]
        neow = JAVA_ROOT / "neow" / "NeowEvent.java"
        result["NEOW"] = JavaSource(
            neow, neow.read_text(encoding="utf-8", errors="replace"),
        )
        return result
    raise ValueError(f"unsupported Java content category: {category}")


def _balanced_call(text: str, marker: str) -> str:
    start = text.find(marker)
    if start < 0:
        raise ValueError(f"Java source has no {marker} call")
    index = start + len(marker)
    depth = 1
    quoted = False
    escaped = False
    for end in range(index, len(text)):
        char = text[end]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[index:end]
    raise ValueError(f"unterminated Java {marker} call")


def _split_arguments(value: str) -> list[str]:
    result: list[str] = []
    start = 0
    depth = 0
    quoted = False
    escaped = False
    for index, char in enumerate(value):
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char in "([{":
            depth += 1
        elif char in ")]}" :
            depth -= 1
        elif char == "," and depth == 0:
            result.append(value[start:index].strip())
            start = index + 1
    result.append(value[start:].strip())
    return result


def _integer_constant(text: str, expression: str) -> int:
    expression = expression.strip()
    if re.fullmatch(r"-?\d+", expression):
        return int(expression)
    name = expression.rsplit(".", 1)[-1]
    match = re.search(
        rf"(?:public|private|protected)?\s*static final int\s+{re.escape(name)}\s*=\s*(-?\d+)",
        text,
    )
    if match is None:
        raise ValueError(f"cannot resolve Java integer expression: {expression}")
    return int(match.group(1))


def _method(text: str, name: str) -> str:
    marker = re.search(rf"\b(?:void|boolean|int)\s+{re.escape(name)}\s*\([^)]*\)\s*\{{", text)
    if marker is None:
        return ""
    start = marker.end()
    depth = 1
    for end in range(start, len(text)):
        if text[end] == "{":
            depth += 1
        elif text[end] == "}":
            depth -= 1
            if depth == 0:
                return text[start:end]
    raise ValueError(f"unterminated Java method: {name}")


def _assignment(text: str, field: str, default: bool = False) -> bool:
    matches = re.findall(rf"this\.{re.escape(field)}\s*=\s*(true|false)", text)
    return (matches[-1] == "true") if matches else default


def java_card_metadata(source: JavaSource) -> dict[str, object]:
    constructor = _balanced_call(source.text, "super(")
    args = _split_arguments(constructor)
    if len(args) < 9:
        raise ValueError(f"unexpected AbstractCard constructor in {source.path}")
    enum = lambda kind: re.search(rf"AbstractCard\.{kind}\.([A-Z_]+)", constructor)
    card_type, color, rarity, target = (
        enum("CardType"), enum("CardColor"), enum("CardRarity"), enum("CardTarget")
    )
    if not all((card_type, color, rarity, target)):
        raise ValueError(f"incomplete AbstractCard constructor in {source.path}")
    cost = _integer_constant(source.text, args[3])
    upgrade = _method(source.text, "upgrade")
    upgraded_cost_match = re.search(r"upgradeBaseCost\((-?\d+)\)", upgrade)
    upgraded_cost = int(upgraded_cost_match.group(1)) if upgraded_cost_match else cost
    damage_match = re.search(r"this\.baseDamage\s*=\s*(-?\d+)", source.text)
    base_damage = int(damage_match.group(1)) if damage_match else -1
    if re.search(r"this\.baseDamage\s*=\s*this\.misc", source.text):
        misc = re.search(r"this\.misc\s*=\s*(-?\d+)", source.text)
        if misc is None:
            raise ValueError(f"cannot resolve misc-backed damage in {source.path}")
        base_damage = int(misc.group(1))
    upgrade_damage = re.search(r"upgradeDamage\((-?\d+)\)", upgrade)
    upgraded_damage = base_damage + int(upgrade_damage.group(1)) if upgrade_damage else base_damage
    if "4 + this.timesUpgraded" in upgrade:
        upgraded_damage = base_damage + 4
    base = {
        "ethereal": _assignment(source.text[:source.text.find("@Override", source.text.find("super("))], "isEthereal"),
        "innate": _assignment(source.text[:source.text.find("@Override", source.text.find("super("))], "isInnate"),
        "exhaust": _assignment(source.text[:source.text.find("@Override", source.text.find("super("))], "exhaust"),
        "self_retain": _assignment(source.text[:source.text.find("@Override", source.text.find("super("))], "selfRetain"),
    }
    upgraded = {
        key: _assignment(upgrade, {
            "ethereal": "isEthereal", "innate": "isInnate",
            "exhaust": "exhaust", "self_retain": "selfRetain",
        }[key], value)
        for key, value in base.items()
    }
    target_name = target.group(1)
    upgraded_target = re.search(
        r"this\.target\s*=\s*AbstractCard\.CardTarget\.([A-Z_]+)", upgrade,
    )
    upgraded_target_name = upgraded_target.group(1) if upgraded_target else target_name
    return {
        "color": color.group(1), "type": card_type.group(1),
        "rarity": rarity.group(1), "cost": cost,
        "upgraded_cost": upgraded_cost,
        "base_damage": base_damage, "upgraded_base_damage": upgraded_damage,
        "targets_enemy": target_name in {"ENEMY", "SELF_AND_ENEMY"},
        "upgraded_targets_enemy": upgraded_target_name in {"ENEMY", "SELF_AND_ENEMY"},
        **base,
        **{f"upgraded_{key}": value for key, value in upgraded.items()},
        "x_cost": cost == -1,
    }


def java_potion_metadata(source: JavaSource) -> dict[str, object]:
    constructor = _balanced_call(source.text, "super(")
    rarity = re.search(r"AbstractPotion\.PotionRarity\.([A-Z_]+)", constructor)
    if rarity is None:
        raise ValueError(f"potion rarity is missing in {source.path}")
    return {
        "rarity": rarity.group(1),
        # CommunicationMod exposes the stock drag target contract: explicitly
        # targetRequired potions and all thrown potions require a wire target,
        # even when the effect itself ignores it (Explosive/Smoke Bomb).
        "requires_target": (
            _assignment(source.text, "targetRequired")
            or _assignment(source.text, "isThrown")
        ),
    }


def java_relic_metadata(source: JavaSource) -> dict[str, object]:
    constructor = _balanced_call(source.text, "super(")
    tier = re.search(r"AbstractRelic\.RelicTier\.([A-Z_]+)", constructor)
    if tier is None:
        raise ValueError(f"relic tier is missing in {source.path}")
    return {"tier": tier.group(1)}


# Stock relic hooks which can affect policy-visible state or future execution.
# Keep this list explicit: constructor/helper/render methods are deliberately
# excluded, while less common hooks must not disappear from the audit merely
# because most relics do not override them.
RELIC_SEMANTIC_CALLBACKS = (
    "onEquip", "onUnequip", "atPreBattle", "atBattleStart",
    "atBattleStartPreDraw", "atTurnStart", "atTurnStartPostDraw",
    "onPlayerEndTurn", "onUseCard", "onPlayCard", "canPlay",
    "onAttack", "onAttackToChangeDamage", "atDamageModify", "onAttacked",
    "onLoseHp", "onLoseHpLast", "wasHPLost", "onPlayerGainedBlock",
    "onPlayerHeal", "onBloodied", "onNotBloodied", "onExhaust",
    "onShuffle", "onMonsterDeath", "onSpawnMonster", "onBlockBroken",
    "onVictory", "onEnterRoom", "justEnteredRoom", "onEnterRestRoom",
    "onChestOpen", "onChestOpenAfter", "onObtainCard",
    "onPreviewObtainCard", "onMasterDeckChange", "onGainGold",
    "onSpendGold", "onUsePotion", "onRefreshHand", "beforeEnergyPrep",
    "changeNumberOfCardsInReward", "changeRareCardRewardChance",
    "addCampfireOption", "canUseCampfireOption", "canSpawn",
    "checkTrigger", "onTrigger", "setCounter",
)


def java_relic_callbacks(source: JavaSource) -> list[str]:
    """Return the exact semantic hooks overridden by a stock relic."""

    return [
        name for name in RELIC_SEMANTIC_CALLBACKS
        if re.search(
            rf"\b(?:public|protected)\s+[\w<>\[\]]+\s+{re.escape(name)}\s*\(",
            source.text,
        )
    ]


def registry_game_ids(category: str, ids: Iterable[str]) -> dict[str, str]:
    wanted = set(ids)
    entries = load_content_registry().categories[category]
    return {
        str(item["id"]): str(item["game_id"])
        for item in entries if str(item["id"]) in wanted
    }
