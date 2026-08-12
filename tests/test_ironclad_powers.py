import unittest

from spirecomm.envs import SimulatorSTSEnv
from spirecomm.envs.vocab import normalize_power_id


POWER_CASES = {
    "BARRICADE": (("BARRICADE", 1, 0), ("BARRICADE", 1, 1)),
    "BRUTALITY": (("BRUTALITY", 1, 3), ("BRUTALITY", 1, 3)),
    "COMBUST": (("COMBUST", 5, 2), ("COMBUST", 7, 2)),
    "CORRUPTION": (("CORRUPTION", 1, 0), ("CORRUPTION", 1, 1)),
    "DARK_EMBRACE": (("DARK_EMBRACE", 1, 1), ("DARK_EMBRACE", 1, 2)),
    "DEMON_FORM": (("DEMON_FORM", 2, 0), ("DEMON_FORM", 3, 0)),
    "EVOLVE": (("EVOLVE", 1, 2), ("EVOLVE", 2, 2)),
    "FEEL_NO_PAIN": (("FEEL_NO_PAIN", 3, 2), ("FEEL_NO_PAIN", 4, 2)),
    "FIRE_BREATHING": (("FIRE_BREATHING", 6, 2), ("FIRE_BREATHING", 10, 2)),
    "INFLAME": (("STRENGTH", 2, 2), ("STRENGTH", 3, 2)),
    "JUGGERNAUT": (("JUGGERNAUT", 5, 1), ("JUGGERNAUT", 7, 1)),
    "METALLICIZE": (("METALLICIZE", 3, 2), ("METALLICIZE", 4, 2)),
    "RUPTURE": (("RUPTURE", 1, 2), ("RUPTURE", 2, 2)),
}


class IroncladPowerTests(unittest.TestCase):
    def test_power_amount_and_energy_cost(self):
        for card_id, versions in POWER_CASES.items():
            for upgrades, (power_id, amount, energy) in enumerate(versions):
                with self.subTest(card_id=card_id, upgrades=upgrades):
                    env = SimulatorSTSEnv(encounter="SLIME_BOSS")
                    try:
                        card = {"id": card_id, "upgrades": upgrades}
                        _, info = env.reset(seed=17, options={"deck": [card] * 10})
                        action = next(
                            index
                            for index, legal in enumerate(info["legal_actions"])
                            if legal["kind"] == "play"
                        )
                        _, _, _, _, info = env.step(action)
                        player = info["battle"]["player"]
                        powers = {
                            normalize_power_id(power["id"]): power["amount"]
                            for power in player["powers"]
                        }
                        self.assertEqual(powers.get(power_id), amount)
                        self.assertEqual(player["energy"], energy)
                    finally:
                        env.close()

    def test_berserk_changes_energy_per_turn_and_applies_vulnerable(self):
        for upgrades, vulnerable in ((0, 2), (1, 1)):
            with self.subTest(upgrades=upgrades):
                env = SimulatorSTSEnv(encounter="SLIME_BOSS")
                try:
                    card = {"id": "BERSERK", "upgrades": upgrades}
                    observation, info = env.reset(seed=17, options={"deck": [card] * 10})
                    action = next(
                        index
                        for index, legal in enumerate(info["legal_actions"])
                        if legal["kind"] == "play"
                    )
                    observation, _, _, _, info = env.step(action)
                    powers = {
                        normalize_power_id(power["id"]): power["amount"]
                        for power in info["battle"]["player"]["powers"]
                    }
                    self.assertEqual(observation["energy_per_turn"], 4)
                    self.assertEqual(powers.get("VULNERABLE"), vulnerable)
                finally:
                    env.close()


if __name__ == "__main__":
    unittest.main()
