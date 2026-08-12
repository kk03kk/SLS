import unittest

from spirecomm.checkpoints import export_combat_checkpoint
from spirecomm.envs import SimulatorSTSEnv


def combat(env):
    return env.payload["game_state"]["combat_state"]


def moves(env):
    return [monster["move_id"] for monster in combat(env)["monsters"]]


def end_turn(env, info):
    action = next(
        index for index, value in enumerate(info["legal_actions"])
        if value["kind"] == "end_turn"
    )
    return env.step(action)[-1]


class Act1BossStateMachineTests(unittest.TestCase):
    def test_slime_boss_repeats_goop_prepare_slam_cycle(self):
        env = SimulatorSTSEnv(encounter="SLIME_BOSS")
        try:
            _, info = env.reset(seed=0)
            observed = [moves(env)[0]]
            for _ in range(5):
                info = end_turn(env, info)
                observed.append(moves(env)[0])
            self.assertEqual(observed, [
                "SLIME_BOSS_GOOP_SPRAY", "SLIME_BOSS_PREPARING",
                "SLIME_BOSS_SLAM", "SLIME_BOSS_GOOP_SPRAY",
                "SLIME_BOSS_PREPARING", "SLIME_BOSS_SLAM",
            ])
        finally:
            env.close()

    def test_slime_boss_below_half_splits_into_large_slimes_at_current_hp(self):
        source = SimulatorSTSEnv(encounter="SLIME_BOSS")
        env = SimulatorSTSEnv()
        try:
            source.reset(seed=4, options={"piles": {
                "hand": ["ANGER"], "draw_pile": ["STRIKE_RED"],
                "discard_pile": [], "exhaust_pile": [],
            }})
            checkpoint = export_combat_checkpoint(source.payload)
            boss = checkpoint["game_state"]["combat_state"]["monsters"][0]
            boss["current_hp"] = 70
            _, info = env.reset(options={"checkpoint": checkpoint})
            play = next(
                index for index, value in enumerate(info["legal_actions"])
                if value["kind"] == "play"
            )
            _, _, _, _, info = env.step(play)
            self.assertEqual(moves(env), ["SLIME_BOSS_SPLIT"])
            info = end_turn(env, info)
            monsters = combat(env)["monsters"]
            active = [monster for monster in monsters if not monster["is_gone"]]
            self.assertEqual(
                [monster["monster_id"] for monster in active],
                ["SPIKE_SLIME_L", "ACID_SLIME_L"],
            )
            self.assertEqual([monster["current_hp"] for monster in active], [64, 64])
            self.assertTrue(monsters[1]["is_gone"])
        finally:
            source.close()
            env.close()

    def test_guardian_offensive_cycle_and_mode_shift(self):
        env = SimulatorSTSEnv(encounter="THE_GUARDIAN")
        try:
            _, info = env.reset(seed=0, options={"piles": {
                "hand": ["BLUDGEON"], "draw_pile": ["STRIKE_RED"] * 5,
                "discard_pile": [], "exhaust_pile": [],
            }})
            guardian = combat(env)["monsters"][0]
            self.assertEqual(guardian["move_id"], "THE_GUARDIAN_CHARGING_UP")
            play = next(
                index for index, value in enumerate(info["legal_actions"])
                if value["kind"] == "play"
            )
            _, _, _, _, info = env.step(play)
            guardian = combat(env)["monsters"][0]
            self.assertEqual(guardian["current_hp"], 208)
            self.assertEqual(guardian["block"], 20)
            self.assertEqual(guardian["move_id"], "THE_GUARDIAN_DEFENSIVE_MODE")
            self.assertNotIn("Mode Shift", {p["id"] for p in guardian["powers"]})

            info = end_turn(env, info)
            self.assertEqual(moves(env)[0], "THE_GUARDIAN_ROLL_ATTACK")
            self.assertIn(
                "Sharp Hide",
                {p["id"] for p in combat(env)["monsters"][0]["powers"]},
            )
            info = end_turn(env, info)
            self.assertEqual(moves(env)[0], "THE_GUARDIAN_TWIN_SLAM")
            info = end_turn(env, info)
            guardian = combat(env)["monsters"][0]
            self.assertEqual(guardian["move_id"], "THE_GUARDIAN_WHIRLWIND")
            self.assertEqual(
                next(p["amount"] for p in guardian["powers"] if p["id"] == "Mode Shift"),
                40,
            )
        finally:
            env.close()

    def test_hexaghost_fixed_cycle_and_divider_uses_current_hp(self):
        source = SimulatorSTSEnv(encounter="HEXAGHOST")
        env = SimulatorSTSEnv()
        try:
            source.reset(seed=0)
            checkpoint = export_combat_checkpoint(source.payload)
            player = checkpoint["game_state"]["combat_state"]["player"]
            player["current_hp"] = 999
            player["max_hp"] = 999
            checkpoint["game_state"]["current_hp"] = 999
            checkpoint["game_state"]["max_hp"] = 999
            _, info = env.reset(options={"checkpoint": checkpoint})
            observed = [moves(env)[0]]
            info = end_turn(env, info)
            observed.append(moves(env)[0])
            divider = combat(env)["monsters"][0]
            self.assertEqual(divider["move_base_damage"], 84)
            self.assertEqual(divider["move_hits"], 6)
            for _ in range(7):
                info = end_turn(env, info)
                observed.append(moves(env)[0])
            self.assertEqual(observed, [
                "HEXAGHOST_ACTIVATE", "HEXAGHOST_DIVIDER", "HEXAGHOST_SEAR",
                "HEXAGHOST_TACKLE", "HEXAGHOST_SEAR", "HEXAGHOST_INFLAME",
                "HEXAGHOST_TACKLE", "HEXAGHOST_SEAR", "HEXAGHOST_INFERNO",
            ])
        finally:
            source.close()
            env.close()


if __name__ == "__main__":
    unittest.main()
