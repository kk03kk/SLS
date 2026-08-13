"""Backend-neutral contracts shared by original-game and simulator envs."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Protocol


MAX_HAND = 20
MAX_ENEMIES = 5
MAX_LEGAL_ACTIONS = 128
MAX_CHOICES = 20
MAX_POTIONS = 5

CHOICE_TASK_NAMES = (
    "ARMAMENTS", "CODEX", "DISCOVERY", "DUAL_WIELD", "EXHAUST_ONE",
    "EXHAUST_MANY", "EXHUME", "FORETHOUGHT", "GAMBLE", "HEADBUTT",
    "HOLOGRAM", "LIQUID_MEMORIES_POTION", "MEDITATE", "NIGHTMARE",
    "RECYCLE", "SECRET_TECHNIQUE", "SECRET_WEAPON", "SEEK", "SETUP",
    "WARCRY", "RETAIN_CARDS", "UNKNOWN",
)
CHOICE_TASK_TO_INDEX = {
    name: index + 1 for index, name in enumerate(CHOICE_TASK_NAMES)
}
CHOICE_SOURCES = ("HAND", "DRAW_PILE", "DISCARD_PILE", "EXHAUST_PILE", "GENERATED")
CHOICE_SOURCE_TO_INDEX = {
    name: index + 1 for index, name in enumerate(CHOICE_SOURCES)
}

INTENT_NAMES = (
    "ATTACK", "ATTACK_BUFF", "ATTACK_DEBUFF", "ATTACK_DEFEND", "BUFF",
    "DEBUFF", "STRONG_DEBUFF", "DEBUG", "DEFEND", "DEFEND_DEBUFF",
    "DEFEND_BUFF", "ESCAPE", "MAGIC", "NONE", "SLEEP", "STUN", "UNKNOWN",
)
INTENT_TO_INDEX = {name: index + 1 for index, name in enumerate(INTENT_NAMES)}


@dataclass(frozen=True)
class LegalAction:
    """One action exposed to an agent.

    ``card_index`` intentionally remains 1-based to preserve the API already
    used by OriginalSTSEnv. ``target_index`` and ``choice_index`` are 0-based.
    ``command`` is optional because only CommunicationMod needs a wire command.
    """

    kind: str
    card_index: int | None = None
    potion_index: int | None = None
    target_index: int | None = None
    choice_index: int | None = None
    command: str | None = None


class BattleBackend(Protocol):
    """Minimal backend required by SimulatorSTSEnv."""

    def reset(
        self,
        *,
        seed: int | None,
        options: dict[str, Any] | None,
    ) -> dict[str, Any]: ...

    def step(self, action: LegalAction) -> dict[str, Any]: ...

    def close(self) -> None: ...
