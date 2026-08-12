"""High-speed headless Gymnasium environment backed by sts_lightspeed."""

from __future__ import annotations

from typing import Any

from spirecomm.envs.base_sts_env import BaseSTSEnv
from spirecomm.envs.codec import is_combat_payload
from spirecomm.envs.contracts import BattleBackend
from spirecomm.simulator.lightspeed_backend import LightspeedBackend


class SimulatorSTSEnv(BaseSTSEnv):
    """One Ironclad combat using the same agent API as OriginalSTSEnv."""

    def __init__(
        self,
        backend: BattleBackend | None = None,
        *,
        encounter: str = "JAW_WORM",
        ascension: int = 0,
    ) -> None:
        super().__init__()
        self.backend = backend or LightspeedBackend(encounter=encounter, ascension=ascension)

    def reset(self, *, seed: int | None = None, options: dict[str, Any] | None = None):
        requested_seed = seed
        gym_seed = None if seed is None else int(seed) % (2**63)
        super().reset(seed=gym_seed)
        if seed is None:
            seed = int(self.np_random.integers(0, 2**63 - 1))
        payload = self.backend.reset(seed=seed, options=options)
        if not is_combat_payload(payload):
            raise RuntimeError("Simulator reset did not produce a combat state")
        self._accept_payload(payload)
        if not self.legal_actions:
            raise RuntimeError("Simulator combat contains no legal actions")
        self._begin_reward_tracking()
        info = self._info()
        info["seed"] = seed if requested_seed is None else requested_seed
        return self._observation(), info

    def step(self, action: int):
        selected = self._validate_action_index(action)
        payload = self.backend.step(selected)
        terminated = not is_combat_payload(payload)
        self._accept_payload(payload)

        if terminated:
            self.legal_actions = []
            reward = 0.0
        else:
            if not self.legal_actions:
                raise RuntimeError("Non-terminal simulator state contains no legal actions")
            reward = self._combat_reward()

        return self._observation(), reward, terminated, False, self._info()

    def close(self) -> None:
        self.backend.close()
        super().close()
