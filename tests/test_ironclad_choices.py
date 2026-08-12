import unittest

from spirecomm.envs import SimulatorSTSEnv
from spirecomm.envs.vocab import CARD_ID_TO_INDEX


class IroncladChoiceTests(unittest.TestCase):
    def setUp(self):
        self.env = SimulatorSTSEnv(encounter="SLIME_BOSS")

    def tearDown(self):
        self.env.close()

    def enter_choice(self, piles):
        _, info = self.env.reset(seed=1, options={"piles": piles})
        play = next(
            index
            for index, action in enumerate(info["legal_actions"])
            if action["kind"] == "play" and action["card_index"] == 1
        )
        observation, _, _, _, info = self.env.step(play)
        self.assertTrue(all(action["kind"] == "choose" for action in info["legal_actions"]))
        return observation, info

    def choose_underlying_index(self, info, choice_index):
        action = next(
            index
            for index, legal in enumerate(info["legal_actions"])
            if legal["choice_index"] == choice_index
        )
        return self.env.step(action)

    def test_warcry_exposes_hand_choice_and_moves_selected_card_to_draw_top(self):
        observation, info = self.enter_choice({
            "hand": ["WARCRY", "STRIKE_RED"],
            "draw_pile": ["DEFEND_RED"],
            "discard_pile": [],
            "exhaust_pile": [],
        })
        self.assertEqual(info["battle"]["choice"]["task"], "WARCRY")
        self.assertEqual(info["battle"]["choice"]["source"], "HAND")
        self.assertEqual(observation["choice_count"], 2)
        self.assertEqual(
            list(observation["choice_card_ids"][:2]),
            [CARD_ID_TO_INDEX["STRIKE_RED"], CARD_ID_TO_INDEX["DEFEND_RED"]],
        )
        _, _, _, _, info = self.choose_underlying_index(info, 1)
        self.assertEqual([card["id"] for card in info["battle"]["draw_pile"]], ["DEFEND_RED"])

    def test_hand_selection_cards_apply_the_selected_index(self):
        cases = (
            ("TRUE_GRIT+1", "EXHAUST_ONE"),
            ("BURNING_PACT", "EXHAUST_ONE"),
            ("ARMAMENTS", "ARMAMENTS"),
            ("DUAL_WIELD", "DUAL_WIELD"),
        )
        for card_id, task in cases:
            with self.subTest(card_id=card_id):
                selected_id = "BASH" if card_id == "DUAL_WIELD" else "DEFEND_RED"
                observation, info = self.enter_choice({
                    "hand": [card_id, "STRIKE_RED", selected_id],
                    "draw_pile": ["ANGER", "BASH", "CLEAVE"],
                    "discard_pile": [],
                    "exhaust_pile": [],
                })
                self.assertEqual(info["battle"]["choice"]["task"], task)
                _, _, _, _, after = self.choose_underlying_index(info, 1)
                battle = after["battle"]
                if card_id.startswith("TRUE_GRIT") or card_id == "BURNING_PACT":
                    self.assertIn(selected_id, {card["id"] for card in battle["exhaust_pile"]})
                elif card_id == "ARMAMENTS":
                    defend = next(card for card in battle["hand"] if card["id"] == selected_id)
                    self.assertEqual(defend["upgrades"], 1)
                else:
                    self.assertEqual(sum(card["id"] == selected_id for card in battle["hand"]), 2)

    def test_exhume_exposes_exhaust_pile_and_returns_selected_card(self):
        _, info = self.enter_choice({
            "hand": ["EXHUME"],
            "draw_pile": [],
            "discard_pile": [],
            "exhaust_pile": ["STRIKE_RED", "BASH"],
        })
        self.assertEqual(info["battle"]["choice"]["source"], "EXHAUST_PILE")
        _, _, _, _, info = self.choose_underlying_index(info, 1)
        self.assertEqual([card["id"] for card in info["battle"]["hand"]], ["BASH"])


if __name__ == "__main__":
    unittest.main()
