import unittest

import numpy as np

from spirecomm.envs import SimulatorSTSEnv
from spirecomm.simulator.catalog import ACT1_ENCOUNTERS, IRONCLAD_CARDS
from spirecomm.envs.vocab import CARD_ID_TO_INDEX


class Act1CoverageTests(unittest.TestCase):
    def test_catalog_has_all_ironclad_cards_and_act1_encounters(self):
        self.assertEqual(len(IRONCLAD_CARDS), 75)
        self.assertEqual(len(set(IRONCLAD_CARDS)), 75)
        self.assertEqual(len(ACT1_ENCOUNTERS), 20)
        self.assertEqual(len(set(ACT1_ENCOUNTERS)), 20)

    def test_every_card_can_be_loaded_base_and_upgraded(self):
        env = SimulatorSTSEnv()
        try:
            for card_id in IRONCLAD_CARDS:
                deck = [
                    {"id": card_id, "upgrades": 0},
                    {"id": card_id, "upgrades": 1},
                ]
                _, info = env.reset(seed=7, options={"deck": deck})
                cards = info["battle"]["hand"] + info["battle"]["draw_pile"]
                self.assertEqual({card["upgrades"] for card in cards}, {0, 1}, card_id)
        finally:
            env.close()

    def test_numeric_observation_distinguishes_card_and_upgrade(self):
        env = SimulatorSTSEnv()
        try:
            observation, _ = env.reset(
                seed=11,
                options={"deck": [
                    {"id": "BASH", "upgrades": 0},
                    {"id": "BASH", "upgrades": 1},
                ]},
            )
            self.assertEqual(
                set(observation["hand_card_ids"][:2]),
                {CARD_ID_TO_INDEX["BASH"]},
            )
            self.assertEqual(set(observation["hand_upgrades"][:2]), {0, 1})
        finally:
            env.close()

    def test_every_act1_encounter_runs_to_a_legal_terminal_state(self):
        rng = np.random.default_rng(19)
        for encounter_index, encounter in enumerate(ACT1_ENCOUNTERS):
            env = SimulatorSTSEnv(encounter=encounter)
            try:
                _, info = env.reset(seed=1000 + encounter_index)
                for _ in range(2000):
                    mask = env.action_masks()
                    self.assertGreater(int(mask.sum()), 0, encounter)
                    action = int(rng.choice(np.flatnonzero(mask)))
                    _, _, terminated, truncated, info = env.step(action)
                    if terminated or truncated:
                        self.assertIn(
                            info["outcome"], {"PLAYER_VICTORY", "PLAYER_LOSS"}
                        )
                        break
                else:
                    self.fail(f"{encounter} did not terminate")
            finally:
                env.close()


if __name__ == "__main__":
    unittest.main()
