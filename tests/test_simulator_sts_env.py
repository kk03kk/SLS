import unittest

import numpy as np

from spirecomm.envs import OriginalSTSEnv, SimulatorSTSEnv


class SimulatorSTSEnvTests(unittest.TestCase):
    def setUp(self):
        try:
            self.env = SimulatorSTSEnv()
        except RuntimeError as exc:
            self.skipTest(str(exc))

    def tearDown(self):
        if hasattr(self, "env"):
            self.env.close()

    def test_agent_facing_spaces_match_original(self):
        original = OriginalSTSEnv(transport=object())
        self.assertEqual(original.action_space.n, self.env.action_space.n)
        self.assertEqual(original.observation_space.keys(), self.env.observation_space.keys())
        for key in original.observation_space:
            self.assertEqual(
                repr(original.observation_space[key]),
                repr(self.env.observation_space[key]),
            )

    def test_seed_is_reproducible(self):
        other = SimulatorSTSEnv()
        try:
            observation_a, info_a = self.env.reset(seed=123)
            observation_b, info_b = other.reset(seed=123)
            for key in observation_a:
                np.testing.assert_array_equal(observation_a[key], observation_b[key])
            self.assertEqual(info_a["battle"], info_b["battle"])
            self.assertEqual(info_a["legal_actions"], info_b["legal_actions"])
        finally:
            other.close()

    def test_invalid_action_index_never_reaches_native_backend(self):
        self.env.reset(seed=1)
        with self.assertRaises(ValueError):
            self.env.step(127)

    def test_random_agent_finishes_battles_without_illegal_actions(self):
        rng = np.random.default_rng(7)
        for seed in range(50):
            observation, info = self.env.reset(seed=seed)
            self.assertTrue(self.env.observation_space.contains(observation))
            for _ in range(1000):
                mask = self.env.action_masks()
                self.assertEqual(int(mask.sum()), len(info["legal_actions"]))
                action = int(rng.choice(np.flatnonzero(mask)))
                observation, _, terminated, truncated, info = self.env.step(action)
                self.assertTrue(self.env.observation_space.contains(observation))
                if terminated or truncated:
                    self.assertIn(info["outcome"], {"PLAYER_VICTORY", "PLAYER_LOSS"})
                    break
            else:
                self.fail(f"seed {seed} did not terminate")


if __name__ == "__main__":
    unittest.main()
