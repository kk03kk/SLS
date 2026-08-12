"""Backend adapter from the native lightspeed bridge to canonical payloads."""

from __future__ import annotations

from typing import Any

from spirecomm.envs.contracts import LegalAction


class LightspeedBackend:
    def __init__(self, encounter: str = "JAW_WORM", ascension: int = 0) -> None:
        try:
            from spirecomm.simulator._lightspeed import LightspeedBattle
        except ImportError as exc:
            raise RuntimeError(
                "The native lightspeed module is not built. Run "
                "`python scripts/build_lightspeed.py` from the repository root."
            ) from exc
        self.encounter = encounter
        self.ascension = ascension
        self._engine = LightspeedBattle()

    def reset(
        self,
        *,
        seed: int | None,
        options: dict[str, Any] | None,
    ) -> dict[str, Any]:
        options = options or {}
        checkpoint = options.get("checkpoint")
        if checkpoint is not None:
            self._engine.load_checkpoint(checkpoint)
            return self._engine.snapshot()

        encounter = str(options.get("encounter", self.encounter))
        ascension = int(options.get("ascension", self.ascension))
        actual_seed = (0 if seed is None else int(seed)) % (2**64)
        def encode_cards(cards):
            encoded = []
            for card in cards:
                if isinstance(card, dict):
                    card_id = str(card.get("id"))
                    upgrades = int(card.get("upgrades", 0))
                    encoded.append(f"{card_id}+{upgrades}" if upgrades > 0 else card_id)
                else:
                    encoded.append(str(card))
            return encoded

        deck = encode_cards(options.get("deck", []))
        replace_relics = "relics" in options
        relics = []
        for relic in options.get("relics", []):
            if isinstance(relic, dict):
                relic_id = str(relic.get("id"))
                if "counter" in relic:
                    relic_id = f"{relic_id}@{int(relic['counter'])}"
                relics.append(relic_id)
            else:
                relics.append(str(relic))
        self._engine.reset(
            actual_seed, encounter, ascension, deck, relics, replace_relics
        )
        if "current_hp" in options or "max_hp" in options:
            self._engine.set_player_health(
                int(options.get("current_hp", options.get("max_hp", 80))),
                int(options.get("max_hp", 80)),
            )
        piles = options.get("piles")
        if piles is not None:
            self._engine.set_card_piles(
                encode_cards(piles.get("hand", [])),
                encode_cards(piles.get("draw_pile", [])),
                encode_cards(piles.get("discard_pile", [])),
                encode_cards(piles.get("exhaust_pile", [])),
            )
        if "potions" in options:
            self._engine.set_potions([str(potion) for potion in options["potions"]])
        return self._engine.snapshot()

    def step(self, action: LegalAction) -> dict[str, Any]:
        self._engine.step(
            action.kind,
            -1 if action.card_index is None else action.card_index,
            -1 if action.potion_index is None else action.potion_index,
            -1 if action.target_index is None else action.target_index,
            -1 if action.choice_index is None else action.choice_index,
        )
        return self._engine.snapshot()

    def close(self) -> None:
        self._engine = None
