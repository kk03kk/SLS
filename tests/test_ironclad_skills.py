import unittest

from spirecomm.envs import SimulatorSTSEnv
from spirecomm.envs.vocab import normalize_power_id


def play_skill(card_id: str, upgrades: int = 0):
    env = SimulatorSTSEnv(encounter="SLIME_BOSS")
    card = {"id": card_id, "upgrades": upgrades}
    _, info = env.reset(seed=42, options={"deck": [card] * 10})
    before = info["battle"]
    action = next(
        index
        for index, legal in enumerate(info["legal_actions"])
        if legal["kind"] == "play"
    )
    _, _, terminated, _, info = env.step(action)
    if terminated:
        raise AssertionError(f"{card_id} unexpectedly ended combat")
    return env, before, info["battle"]


def powers_by_id(owner):
    return {
        normalize_power_id(power["id"]): power["amount"]
        for power in owner["powers"]
    }


class IroncladSkillTests(unittest.TestCase):
    def test_block_values(self):
        expected = {
            "DEFEND_RED": (5, 8),
            "SHRUG_IT_OFF": (8, 11),
            "FLAME_BARRIER": (12, 16),
            "POWER_THROUGH": (15, 20),
            "IMPERVIOUS": (30, 40),
            "SECOND_WIND": (20, 28),  # four other skills at 5/7 each
        }
        for card_id, versions in expected.items():
            for upgrades, block in enumerate(versions):
                with self.subTest(card_id=card_id, upgrades=upgrades):
                    env, _, after = play_skill(card_id, upgrades)
                    try:
                        self.assertEqual(after["player"]["block"], block)
                    finally:
                        env.close()

    def test_energy_draw_and_self_hp_values(self):
        cases = {
            "BLOODLETTING": ((3, 5, 4), (3, 6, 4)),
            "SEEING_RED": ((0, 4, 4), (0, 5, 4)),
            "OFFERING": ((6, 5, 7), (6, 5, 9)),
            "BATTLE_TRANCE": ((0, 3, 7), (0, 3, 8)),
            "SHRUG_IT_OFF": ((0, 2, 5), (0, 2, 5)),
        }
        for card_id, versions in cases.items():
            for upgrades, (hp_loss, energy, hand_size) in enumerate(versions):
                with self.subTest(card_id=card_id, upgrades=upgrades):
                    env, before, after = play_skill(card_id, upgrades)
                    try:
                        self.assertEqual(before["player"]["hp"] - after["player"]["hp"], hp_loss)
                        self.assertEqual(after["player"]["energy"], energy)
                        self.assertEqual(len(after["hand"]), hand_size)
                    finally:
                        env.close()

    def test_player_and_enemy_power_values(self):
        player_cases = {
            "FLEX": (("STRENGTH", 2), ("STRENGTH", 4)),
            "FLAME_BARRIER": (("FLAME_BARRIER", 4), ("FLAME_BARRIER", 6)),
            "BATTLE_TRANCE": (("NO_DRAW", 1), ("NO_DRAW", 1)),
        }
        for card_id, versions in player_cases.items():
            for upgrades, (power_id, amount) in enumerate(versions):
                with self.subTest(card_id=card_id, upgrades=upgrades):
                    env, _, after = play_skill(card_id, upgrades)
                    try:
                        self.assertEqual(powers_by_id(after["player"]).get(power_id), amount)
                    finally:
                        env.close()

        enemy_cases = {
            "DISARM": ({"STRENGTH": -2}, {"STRENGTH": -3}),
            "INTIMIDATE": ({"WEAK": 1}, {"WEAK": 2}),
            "SHOCKWAVE": (
                {"WEAK": 3, "VULNERABLE": 3},
                {"WEAK": 5, "VULNERABLE": 5},
            ),
        }
        for card_id, versions in enemy_cases.items():
            for upgrades, expected in enumerate(versions):
                with self.subTest(card_id=card_id, upgrades=upgrades):
                    env, _, after = play_skill(card_id, upgrades)
                    try:
                        actual = powers_by_id(after["enemies"][0])
                        for power_id, amount in expected.items():
                            self.assertEqual(actual.get(power_id), amount)
                    finally:
                        env.close()

    def test_generated_wounds_and_exhaust(self):
        for upgrades in (0, 1):
            env, _, after = play_skill("POWER_THROUGH", upgrades)
            try:
                self.assertEqual(
                    sum(card["id"] == "WOUND" for card in after["hand"]), 2
                )
            finally:
                env.close()

            env, _, after = play_skill("IMPERVIOUS", upgrades)
            try:
                self.assertEqual(len(after["exhaust_pile"]), 1)
            finally:
                env.close()


if __name__ == "__main__":
    unittest.main()
