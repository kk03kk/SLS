import unittest

from spirecomm.envs import SimulatorSTSEnv
from spirecomm.envs.vocab import normalize_power_id


# Immediate, unblocked damage against one enemy at zero Strength. Conditional
# attacks get dedicated stateful tests later.
ATTACK_DAMAGE = {
    "ANGER": (6, 8),
    "BASH": (8, 10),
    "BLUDGEON": (32, 42),
    "BODY_SLAM": (0, 0),
    "CARNAGE": (20, 28),
    "CLASH": (14, 18),
    "CLEAVE": (8, 11),
    "CLOTHESLINE": (12, 14),
    "DROPKICK": (5, 8),
    "FEED": (10, 12),
    "FIEND_FIRE": (28, 40),  # four other opening-hand cards
    "HEAVY_BLADE": (14, 14),
    "HEMOKINESIS": (15, 20),
    "IMMOLATE": (21, 28),
    "IRON_WAVE": (5, 7),
    "POMMEL_STRIKE": (9, 10),
    "PUMMEL": (8, 10),
    "RAMPAGE": (8, 8),
    "REAPER": (4, 5),
    "RECKLESS_CHARGE": (7, 10),
    "SEARING_BLOW": (12, 16),
    "SEVER_SOUL": (16, 22),
    "STRIKE_RED": (6, 9),
    "SWORD_BOOMERANG": (9, 12),
    "THUNDERCLAP": (4, 7),
    "TWIN_STRIKE": (10, 14),
    "UPPERCUT": (13, 13),
    "WHIRLWIND": (15, 24),  # three energy
    "WILD_STRIKE": (12, 17),
}


def play_first_copy(card_id: str, upgrades: int = 0):
    env = SimulatorSTSEnv(encounter="SLIME_BOSS")
    card = {"id": card_id, "upgrades": upgrades}
    _, info = env.reset(seed=321, options={"deck": [card] * 10})
    before = info["battle"]
    action = next(
        index
        for index, legal in enumerate(info["legal_actions"])
        if legal["kind"] == "play"
    )
    _, _, terminated, _, info = env.step(action)
    if terminated:
        raise AssertionError(f"{card_id} unexpectedly ended Slime Boss combat")
    return env, before, info["battle"]


class IroncladAttackTests(unittest.TestCase):
    def test_base_and_upgraded_immediate_damage(self):
        for card_id, expected_versions in ATTACK_DAMAGE.items():
            for upgrades, expected_damage in enumerate(expected_versions):
                with self.subTest(card_id=card_id, upgrades=upgrades):
                    env, before, after = play_first_copy(card_id, upgrades)
                    try:
                        damage = (
                            before["enemies"][0]["hp"]
                            - after["enemies"][0]["hp"]
                        )
                        self.assertEqual(damage, expected_damage)
                    finally:
                        env.close()

    def test_searing_blow_preserves_multiple_upgrade_count(self):
        env, _, after = play_first_copy("SEARING_BLOW", upgrades=3)
        try:
            played = after["discard_pile"][0]
            self.assertEqual(played["upgrades"], 3)
        finally:
            env.close()

    def test_attack_side_effects(self):
        cases = {
            "BASH": {"VULNERABLE": 2},
            "CLOTHESLINE": {"WEAK": 2},
            "THUNDERCLAP": {"VULNERABLE": 1},
            "UPPERCUT": {"VULNERABLE": 1, "WEAK": 1},
        }
        for card_id, expected_powers in cases.items():
            with self.subTest(card_id=card_id):
                env, _, after = play_first_copy(card_id)
                try:
                    powers = {
                        normalize_power_id(power["id"]): power["amount"]
                        for power in after["enemies"][0]["powers"]
                    }
                    for power_id, amount in expected_powers.items():
                        self.assertEqual(powers.get(power_id), amount)
                finally:
                    env.close()

    def test_attack_generated_status_and_self_damage(self):
        for card_id, pile_name, generated_id in (
            ("WILD_STRIKE", "draw_pile", "WOUND"),
            ("RECKLESS_CHARGE", "draw_pile", "DAZED"),
            ("IMMOLATE", "discard_pile", "BURN"),
        ):
            with self.subTest(card_id=card_id):
                env, _, after = play_first_copy(card_id)
                try:
                    self.assertIn(
                        generated_id,
                        {card["id"] for card in after[pile_name]},
                    )
                finally:
                    env.close()

        env, before, after = play_first_copy("HEMOKINESIS")
        try:
            self.assertEqual(before["player"]["hp"] - after["player"]["hp"], 2)
        finally:
            env.close()


if __name__ == "__main__":
    unittest.main()
