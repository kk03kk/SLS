import unittest

from spirecomm.envs import SimulatorSTSEnv
from spirecomm.envs.codec import generate_legal_actions


def action_index(info, kind, potion_index, target_index=None):
    for index, action in enumerate(info["legal_actions"]):
        if (
            action["kind"] == kind
            and action["potion_index"] == potion_index
            and action["target_index"] == target_index
        ):
            return index
    raise AssertionError(f"missing action: {kind=} {potion_index=} {target_index=}")


class PotionTests(unittest.TestCase):
    IRONCLAD_POOL = (
        "Blood Potion", "Elixir Potion", "Heart Of Iron", "Block Potion",
        "Dexterity Potion", "Energy Potion", "Explosive Potion", "Fire Potion",
        "Strength Potion", "Swift Potion", "Weak Potion", "Fear Potion",
        "Attack Potion", "Skill Potion", "Power Potion", "Colorless Potion",
        "Flex Potion", "Speed Potion", "Blessing Of The Forge", "Regen Potion",
        "Ancient Potion", "Liquid Bronze", "Gamblers Brew", "Essence Of Steel",
        "Duplication Potion", "Distilled Chaos", "Liquid Memories", "Cultist Potion",
        "Fruit Juice", "Snecko Oil", "Fairy Potion", "Smoke Bomb", "Entropic Brew",
    )

    def test_simulator_enumerates_only_legal_potion_actions(self):
        env = SimulatorSTSEnv()
        try:
            observation, info = env.reset(
                seed=1,
                options={"potions": ["Fire Potion", "Block Potion", "Fairy Potion"]},
            )
            potion_actions = [
                action for action in info["legal_actions"]
                if action["potion_index"] is not None
            ]
            self.assertEqual(
                [(a["kind"], a["potion_index"], a["target_index"]) for a in potion_actions],
                [
                    ("potion", 0, 0),
                    ("discard_potion", 0, None),
                    ("potion", 1, None),
                    ("discard_potion", 1, None),
                    ("discard_potion", 2, None),
                ],
            )
            self.assertEqual(int(observation["potion_count"]), 3)
            self.assertEqual(observation["potion_usable"].tolist(), [1, 1, 0, 0, 0])
        finally:
            env.close()

    def test_targeted_and_untargeted_potions_execute_and_are_consumed(self):
        env = SimulatorSTSEnv()
        try:
            _, info = env.reset(
                seed=7, options={"potions": ["Fire Potion", "Block Potion"]}
            )
            hp_before = info["battle"]["enemies"][0]["hp"]
            _, _, _, _, info = env.step(action_index(info, "potion", 0, 0))
            self.assertEqual(info["battle"]["enemies"][0]["hp"], hp_before - 20)
            self.assertEqual(info["battle"]["potions"][0]["id"], "Potion Slot")

            _, _, _, _, info = env.step(action_index(info, "potion", 1))
            self.assertEqual(info["battle"]["player"]["block"], 12)
            self.assertEqual(info["battle"]["potions"][1]["id"], "Potion Slot")
            self.assertFalse(any(a["potion_index"] is not None for a in info["legal_actions"]))
        finally:
            env.close()

    def test_original_payload_potion_commands_use_communicationmod_syntax(self):
        payload = {
            "ready_for_command": True,
            "available_commands": ["potion", "end"],
            "game_state": {
                "room_phase": "COMBAT",
                "potions": [
                    {
                        "id": "Fire Potion", "can_use": True,
                        "can_discard": True, "requires_target": True,
                    },
                    {
                        "id": "Potion Slot", "can_use": False,
                        "can_discard": False, "requires_target": False,
                    },
                ],
                "combat_state": {
                    "monsters": [
                        {"current_hp": 10, "is_gone": False, "half_dead": False}
                    ]
                },
            },
        }
        actions = generate_legal_actions(payload)
        self.assertEqual(
            [action.command for action in actions],
            ["potion use 0 0", "potion discard 0", "end"],
        )

    def test_multi_card_potion_selection_is_sequential_and_never_illegal(self):
        env = SimulatorSTSEnv()
        try:
            _, info = env.reset(seed=11, options={"potions": ["Elixir Potion"]})
            use = next(
                index for index, action in enumerate(info["legal_actions"])
                if action["kind"] == "potion"
            )
            _, _, _, _, info = env.step(use)
            self.assertEqual(info["battle"]["choice"]["task"], "EXHAUST_MANY")
            self.assertEqual(
                [a["kind"] for a in info["legal_actions"]],
                ["choose"] * len(info["battle"]["hand"]) + ["proceed"],
            )

            choose_first = next(
                index for index, action in enumerate(info["legal_actions"])
                if action["kind"] == "choose" and action["choice_index"] == 0
            )
            _, _, _, _, info = env.step(choose_first)
            self.assertTrue(info["battle"]["choice"]["options"][0]["selected"])
            self.assertFalse(any(
                action["kind"] == "choose" and action["choice_index"] == 0
                for action in info["legal_actions"]
            ))

            confirm = next(
                index for index, action in enumerate(info["legal_actions"])
                if action["kind"] == "proceed"
            )
            _, _, _, _, info = env.step(confirm)
            self.assertIsNone(info["battle"]["choice"]["task"])
            self.assertEqual(len(info["battle"]["exhaust_pile"]), 1)
        finally:
            env.close()

    def test_smoke_bomb_escapes_non_boss_and_is_not_legal_in_boss_combat(self):
        env = SimulatorSTSEnv()
        boss = SimulatorSTSEnv(encounter="SLIME_BOSS")
        try:
            _, info = env.reset(seed=13, options={"potions": ["Smoke Bomb"]})
            use = next(
                index for index, action in enumerate(info["legal_actions"])
                if action["kind"] == "potion"
            )
            _, _, terminated, _, info = env.step(use)
            self.assertTrue(terminated)
            self.assertEqual(info["outcome"], "ESCAPED")

            _, boss_info = boss.reset(seed=13, options={"potions": ["Smoke Bomb"]})
            self.assertFalse(any(
                action["kind"] == "potion" for action in boss_info["legal_actions"]
            ))
            self.assertTrue(any(
                action["kind"] == "discard_potion"
                for action in boss_info["legal_actions"]
            ))
        finally:
            env.close()
            boss.close()

    def test_every_ironclad_pool_potion_has_a_safe_first_action_path(self):
        for potion in self.IRONCLAD_POOL:
            with self.subTest(potion=potion):
                env = SimulatorSTSEnv()
                try:
                    _, info = env.reset(seed=3, options={"potions": [potion]})
                    usable = [
                        index for index, action in enumerate(info["legal_actions"])
                        if action["kind"] == "potion"
                    ]
                    if potion == "Fairy Potion":
                        self.assertEqual(usable, [])
                        continue
                    self.assertTrue(usable)
                    _, _, terminated, _, info = env.step(usable[0])
                    for _ in range(12):
                        if terminated or env.payload["game_state"]["input_state"] == "PLAYER_NORMAL":
                            break
                        proceed = [
                            index for index, action in enumerate(info["legal_actions"])
                            if action["kind"] == "proceed"
                        ]
                        action = proceed[0] if proceed else 0
                        _, _, terminated, _, info = env.step(action)
                    self.assertTrue(
                        terminated or
                        env.payload["game_state"]["input_state"] == "PLAYER_NORMAL"
                    )
                finally:
                    env.close()


if __name__ == "__main__":
    unittest.main()
