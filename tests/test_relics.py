import unittest

from spirecomm.checkpoints import export_combat_checkpoint
from spirecomm.envs import SimulatorSTSEnv


class RelicTests(unittest.TestCase):
    def test_relics_are_applied_before_battle_start_with_counters(self):
        env = SimulatorSTSEnv()
        try:
            _, info = env.reset(
                seed=1,
                options={
                    "relics": [
                        "Anchor", "Bag of Preparation", "Vajra",
                        {"id": "Happy Flower", "counter": 2},
                    ]
                },
            )
            battle = info["battle"]
            self.assertEqual([r["id"] for r in battle["relics"]], [
                "Anchor", "Bag of Preparation", "Vajra", "Happy Flower",
            ])
            self.assertNotIn("Burning Blood", [r["id"] for r in battle["relics"]])
            self.assertEqual(battle["player"]["block"], 10)
            self.assertEqual(battle["player"]["energy"], 4)
            self.assertEqual(len(battle["hand"]), 7)
            self.assertEqual(
                next(p["amount"] for p in battle["player"]["powers"] if p["id"] == "STRENGTH"),
                1,
            )
            self.assertEqual(battle["relics"][-1]["counter"], 0)
        finally:
            env.close()

    def test_sacred_bark_doubles_potion_effect(self):
        env = SimulatorSTSEnv()
        try:
            _, info = env.reset(
                seed=5,
                options={"relics": ["Sacred Bark"], "potions": ["Fire Potion"]},
            )
            hp_before = info["battle"]["enemies"][0]["hp"]
            action = next(
                index for index, value in enumerate(info["legal_actions"])
                if value["kind"] == "potion"
            )
            _, _, _, _, info = env.step(action)
            self.assertEqual(info["battle"]["enemies"][0]["hp"], hp_before - 40)
        finally:
            env.close()

    def test_checkpoint_preserves_relic_identity_and_live_counter(self):
        source = SimulatorSTSEnv()
        clone = SimulatorSTSEnv()
        try:
            source.reset(
                seed=9,
                options={"relics": [{"id": "Happy Flower", "counter": 2}]},
            )
            checkpoint = export_combat_checkpoint(source.payload)
            _, clone_info = clone.reset(options={"checkpoint": checkpoint})
            self.assertEqual(source._info()["battle"]["relics"], clone_info["battle"]["relics"])
            self.assertEqual(source.payload["_rng"], clone.payload["_rng"])
        finally:
            source.close()
            clone.close()


if __name__ == "__main__":
    unittest.main()
