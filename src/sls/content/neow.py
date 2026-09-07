"""Public Neow offer semantics, without sampling any reward outcome.

The ordered native enums are mirrored explicitly and tested against Neow.h.
Binary fields encode categories, not ordinal magnitudes or strategy advice.
"""

NEOW_BONUSES = (
    "THREE_CARDS", "ONE_RANDOM_RARE_CARD", "REMOVE_CARD", "UPGRADE_CARD",
    "TRANSFORM_CARD", "RANDOM_COLORLESS", "THREE_SMALL_POTIONS",
    "RANDOM_COMMON_RELIC", "TEN_PERCENT_HP_BONUS", "THREE_ENEMY_KILL",
    "HUNDRED_GOLD", "RANDOM_COLORLESS_2", "REMOVE_TWO", "ONE_RARE_RELIC",
    "THREE_RARE_CARDS", "TWO_FIFTY_GOLD", "TRANSFORM_TWO_CARDS",
    "TWENTY_PERCENT_HP_BONUS", "BOSS_RELIC",
)
NEOW_DRAWBACKS = (
    "NONE", "TEN_PERCENT_HP_LOSS", "NO_GOLD", "CURSE", "PERCENT_DAMAGE",
    "LOSE_STARTER_RELIC",
)
NEOW_NUMERIC_FIELDS = tuple(
    f"neow_{group}_{name.lower()}"
    for group, names in (("bonus", NEOW_BONUSES), ("drawback", NEOW_DRAWBACKS))
    for name in names
)


def neow_properties(bonus: str | int, drawback: str | int) -> tuple[tuple[str, bool], ...]:
    if type(bonus) is int:
        if not 0 <= bonus < len(NEOW_BONUSES):
            raise ValueError("invalid native Neow bonus")
        bonus = NEOW_BONUSES[bonus]
    if type(drawback) is int:
        if not 1 <= drawback <= len(NEOW_DRAWBACKS):
            raise ValueError("invalid native Neow drawback")
        drawback = NEOW_DRAWBACKS[drawback - 1]
    # Stock folds the starter-relic cost into BOSS_RELIC rather than drawback.
    if bonus == "BOSS_RELIC" and drawback == "NONE":
        drawback = "LOSE_STARTER_RELIC"
    if bonus not in NEOW_BONUSES or drawback not in NEOW_DRAWBACKS:
        raise ValueError(f"unknown public Neow offer: {bonus}/{drawback}")
    return (
        (f"neow_bonus_{bonus.lower()}", True),
        (f"neow_drawback_{drawback.lower()}", True),
    )
