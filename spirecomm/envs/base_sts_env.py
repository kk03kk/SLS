"""Shared Gymnasium surface for original-game and simulator environments."""

from __future__ import annotations

from typing import Any

import gymnasium as gym
import numpy as np
from gymnasium import spaces

from spirecomm.envs.codec import (
    action_mask,
    actions_info,
    generate_legal_actions,
    parse_battle_observation,
    rich_battle_state,
)
from spirecomm.envs.contracts import (
    CHOICE_SOURCES,
    CHOICE_TASK_NAMES,
    INTENT_NAMES,
    MAX_ENEMIES,
    MAX_HAND,
    MAX_CHOICES,
    MAX_LEGAL_ACTIONS,
    MAX_POTIONS,
    LegalAction,
)
from spirecomm.envs.vocab import CARD_IDS, ENEMY_POWER_IDS, PLAYER_POWER_IDS, POTION_IDS


class BaseSTSEnv(gym.Env):
    """Common observation, action, reward and info contract."""

    metadata = {"render_modes": ["ansi"]}

    def __init__(self) -> None:
        super().__init__()
        self.action_space = spaces.Discrete(MAX_LEGAL_ACTIONS)
        self.observation_space = spaces.Dict(
            {
                "player_hp": spaces.Box(0, 999, shape=(2,), dtype=np.int32),
                "energy": spaces.Discrete(100),
                "energy_per_turn": spaces.Discrete(100),
                "card_draw_per_turn": spaces.Discrete(100),
                "turn": spaces.Discrete(1000),
                "player_block": spaces.Discrete(10000),
                "player_powers": spaces.Box(
                    -999, 999, shape=(len(PLAYER_POWER_IDS),), dtype=np.int16
                ),
                "hand_count": spaces.Discrete(MAX_HAND + 1),
                "hand_costs": spaces.Box(-3, 99, shape=(MAX_HAND,), dtype=np.int16),
                "hand_playable": spaces.MultiBinary(MAX_HAND),
                "hand_card_ids": spaces.MultiDiscrete(
                    np.full(MAX_HAND, len(CARD_IDS), dtype=np.int64)
                ),
                "hand_upgrades": spaces.Box(
                    0, 99, shape=(MAX_HAND,), dtype=np.int16
                ),
                "draw_pile_counts": spaces.Box(
                    0, 999, shape=(len(CARD_IDS),), dtype=np.int16
                ),
                "discard_pile_counts": spaces.Box(
                    0, 999, shape=(len(CARD_IDS),), dtype=np.int16
                ),
                "exhaust_pile_counts": spaces.Box(
                    0, 999, shape=(len(CARD_IDS),), dtype=np.int16
                ),
                "enemy_count": spaces.Discrete(MAX_ENEMIES + 1),
                "enemy_hp": spaces.Box(0, 9999, shape=(MAX_ENEMIES, 2), dtype=np.int32),
                "enemy_block": spaces.Box(
                    0, 9999, shape=(MAX_ENEMIES,), dtype=np.int32
                ),
                "enemy_intents": spaces.MultiDiscrete(
                    np.full(MAX_ENEMIES, len(INTENT_NAMES) + 1, dtype=np.int64)
                ),
                "enemy_powers": spaces.Box(
                    -999,
                    999,
                    shape=(MAX_ENEMIES, len(ENEMY_POWER_IDS)),
                    dtype=np.int16,
                ),
                "choice_task": spaces.Discrete(len(CHOICE_TASK_NAMES) + 1),
                "choice_source": spaces.Discrete(len(CHOICE_SOURCES) + 1),
                "choice_count": spaces.Discrete(MAX_CHOICES + 1),
                "choice_card_ids": spaces.MultiDiscrete(
                    np.full(MAX_CHOICES, len(CARD_IDS), dtype=np.int64)
                ),
                "potion_count": spaces.Discrete(MAX_POTIONS + 1),
                "potion_ids": spaces.MultiDiscrete(
                    np.full(MAX_POTIONS, len(POTION_IDS), dtype=np.int64)
                ),
                "potion_usable": spaces.MultiBinary(MAX_POTIONS),
            }
        )
        self.payload: dict[str, Any] | None = None
        self.legal_actions: list[LegalAction] = []
        self._previous_player_hp = 0
        self._previous_enemy_hp = 0

    def _accept_payload(self, payload: dict[str, Any]) -> None:
        self.payload = payload
        self.legal_actions = generate_legal_actions(payload)

    def _begin_reward_tracking(self) -> None:
        assert self.payload is not None
        battle = rich_battle_state(self.payload)
        self._previous_player_hp = int(battle["player"]["hp"] or 0)
        self._previous_enemy_hp = sum(int(enemy["hp"] or 0) for enemy in battle["enemies"])

    def _combat_reward(self) -> float:
        assert self.payload is not None
        battle = rich_battle_state(self.payload)
        player_hp = int(battle["player"]["hp"] or 0)
        enemy_hp = sum(int(enemy["hp"] or 0) for enemy in battle["enemies"])
        reward = float(
            (self._previous_enemy_hp - enemy_hp)
            - (self._previous_player_hp - player_hp)
        )
        self._previous_player_hp = player_hp
        self._previous_enemy_hp = enemy_hp
        return reward

    def _observation(self):
        assert self.payload is not None
        return parse_battle_observation(self.payload)

    def _info(self) -> dict[str, Any]:
        assert self.payload is not None
        return {
            "battle": rich_battle_state(self.payload),
            "legal_actions": actions_info(self.legal_actions),
            "action_mask": action_mask(self.legal_actions),
            "outcome": self.payload.get("outcome"),
        }

    def _validate_action_index(self, action: int) -> LegalAction:
        if self.payload is None:
            raise RuntimeError("Call reset() before step()")
        if not isinstance(action, (int, np.integer)) or not 0 <= int(action) < len(self.legal_actions):
            raise ValueError(
                f"Action {action!r} is outside current legal range "
                f"[0, {len(self.legal_actions)})"
            )
        return self.legal_actions[int(action)]

    def action_masks(self) -> np.ndarray:
        """sb3-contrib MaskablePPO-compatible mask provider."""

        return action_mask(self.legal_actions).astype(bool)

    def render(self) -> str:
        if self.payload is None:
            return f"{type(self).__name__}(not reset)"
        import json

        return json.dumps(rich_battle_state(self.payload), ensure_ascii=False, indent=2)
