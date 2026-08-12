import unittest

from spirecomm.envs import SimulatorSTSEnv


def monster(payload, index=0):
    return payload["game_state"]["combat_state"]["monsters"][index]


def action_index(info, kind, *, card_index=None, target_index=None):
    for index, action in enumerate(info["legal_actions"]):
        if action["kind"] != kind:
            continue
        if card_index is not None and action["card_index"] != card_index:
            continue
        if target_index is not None and action["target_index"] != target_index:
            continue
        return index
    raise AssertionError(f"missing legal action {kind}")


class Act1EliteStateMachineTests(unittest.TestCase):
    def test_gremlin_nob_opens_bellow_and_enrage_triggers_only_after_activation(self):
        env = SimulatorSTSEnv(encounter="GREMLIN_NOB")
        try:
            _, info = env.reset(
                seed=0,
                options={"piles": {
                    "hand": ["DEFEND_RED"], "draw_pile": ["DEFEND_RED"] * 9,
                    "discard_pile": [], "exhaust_pile": [],
                }},
            )
            self.assertEqual(monster(env.payload)["move_id"], "GREMLIN_NOB_BELLOW")
            env.step(action_index(info, "play", card_index=1))
            self.assertEqual(
                next((p["amount"] for p in monster(env.payload)["powers"] if p["id"] == "Strength"), 0),
                0,
            )
            _, _, _, _, info = env.step(action_index(env._info(), "end_turn"))
            self.assertEqual(
                next(p["amount"] for p in monster(env.payload)["powers"] if p["id"] == "Enrage"),
                2,
            )
            env.step(action_index(info, "play", card_index=1))
            self.assertEqual(
                next(p["amount"] for p in monster(env.payload)["powers"] if p["id"] == "Strength"),
                2,
            )
        finally:
            env.close()

    def test_lagavulin_natural_wake_removes_asleep_metallicize_and_block(self):
        env = SimulatorSTSEnv(encounter="LAGAVULIN")
        try:
            _, info = env.reset(seed=0)
            for _ in range(3):
                _, _, _, _, info = env.step(action_index(info, "end_turn"))
            lagavulin = monster(env.payload)
            self.assertEqual(lagavulin["move_id"], "LAGAVULIN_ATTACK")
            self.assertEqual(lagavulin["block"], 0)
            self.assertNotIn(
                "Asleep", {power["id"] for power in lagavulin["powers"]}
            )
            self.assertNotIn(
                "Metallicize", {power["id"] for power in lagavulin["powers"]}
            )
        finally:
            env.close()

    def test_lagavulin_only_wakes_when_attack_passes_its_eight_block(self):
        env = SimulatorSTSEnv(encounter="LAGAVULIN")
        try:
            _, info = env.reset(seed=2, options={"piles": {
                "hand": ["BASH"], "draw_pile": ["CARNAGE"],
                "discard_pile": [], "exhaust_pile": [],
            }})
            env.step(action_index(info, "play", card_index=1, target_index=0))
            self.assertEqual(monster(env.payload)["move_id"], "LAGAVULIN_SLEEP")
            self.assertIn("Asleep", {p["id"] for p in monster(env.payload)["powers"]})
        finally:
            env.close()

    def test_sentries_alternate_and_first_bolts_add_four_dazed(self):
        env = SimulatorSTSEnv(encounter="THREE_SENTRIES")
        try:
            _, info = env.reset(seed=0)
            self.assertEqual(
                [monster(env.payload, i)["move_id"] for i in range(3)],
                ["SENTRY_BOLT", "SENTRY_BEAM", "SENTRY_BOLT"],
            )
            _, _, _, _, info = env.step(action_index(info, "end_turn"))
            self.assertEqual(
                [monster(env.payload, i)["move_id"] for i in range(3)],
                ["SENTRY_BEAM", "SENTRY_BOLT", "SENTRY_BEAM"],
            )
            discard = env.payload["game_state"]["combat_state"]["discard_pile"]
            self.assertEqual(sum(card["id"] == "DAZED" for card in discard), 4)
            env.step(action_index(info, "end_turn"))
            self.assertEqual(
                [monster(env.payload, i)["move_id"] for i in range(3)],
                ["SENTRY_BOLT", "SENTRY_BEAM", "SENTRY_BOLT"],
            )
        finally:
            env.close()


if __name__ == "__main__":
    unittest.main()
