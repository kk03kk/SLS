import unittest

from spirecomm.checkpoints import export_combat_checkpoint
from spirecomm.envs import SimulatorSTSEnv
from spirecomm.envs.vocab import normalize_power_id


def play_card(env, info, card_id, target_index=0):
    hand_index = next(
        index for index, card in enumerate(info["battle"]["hand"])
        if card["id"] == card_id
    )
    action_index = next(
        index for index, action in enumerate(info["legal_actions"])
        if action["kind"] == "play"
        and action["card_index"] == hand_index + 1
        and (action["target_index"] in (None, target_index))
    )
    return env.step(action_index)


def end_turn(env, info):
    action_index = next(
        index for index, action in enumerate(info["legal_actions"])
        if action["kind"] == "end_turn"
    )
    return env.step(action_index)


class GenericCombatMechanicsTests(unittest.TestCase):
    def test_native_action_queue_top_bottom_and_victory_cleanup_order(self):
        from spirecomm.simulator._lightspeed import action_queue_probe

        result = action_queue_probe()
        self.assertEqual(result["mixed_top_bottom"], [4, 3, 1, 2])
        self.assertEqual(result["post_victory_retained"], [1, 3])

    def test_calm_exit_and_mantra_divinity_transition(self):
        from spirecomm.simulator._lightspeed import stance_mechanics_probe

        result = stance_mechanics_probe()
        self.assertEqual(result["calm_exit"], {"stance": "WRATH", "energy": 2})
        self.assertEqual(result["divinity_entry"], {
            "stance": "DIVINITY", "energy": 5, "mantra": 2,
        })

    def restore_with_monster_block(self, card_id, block):
        source = SimulatorSTSEnv(encounter="SLIME_BOSS")
        restored = SimulatorSTSEnv()
        source.reset(seed=31, options={"piles": {
            "hand": [card_id],
            "draw_pile": ["DEFEND_RED"] * 8,
            "discard_pile": [],
            "exhaust_pile": [],
        }})
        checkpoint = export_combat_checkpoint(source.payload)
        checkpoint["game_state"]["combat_state"]["monsters"][0]["block"] = block
        _, info = restored.reset(options={"checkpoint": checkpoint})
        return source, restored, info

    def test_attack_consumes_enemy_block_before_hp(self):
        source, env, info = self.restore_with_monster_block("STRIKE_RED", 4)
        try:
            before_hp = info["battle"]["enemies"][0]["hp"]
            _, _, terminated, _, info = play_card(env, info, "STRIKE_RED")
            self.assertFalse(terminated)
            enemy = info["battle"]["enemies"][0]
            self.assertEqual(enemy["block"], 0)
            self.assertEqual(before_hp - enemy["hp"], 2)
        finally:
            source.close()
            env.close()

    def test_multi_hit_consumes_block_between_hits(self):
        source, env, info = self.restore_with_monster_block("TWIN_STRIKE", 7)
        try:
            before_hp = info["battle"]["enemies"][0]["hp"]
            _, _, terminated, _, info = play_card(env, info, "TWIN_STRIKE")
            self.assertFalse(terminated)
            enemy = info["battle"]["enemies"][0]
            self.assertEqual(enemy["block"], 0)
            self.assertEqual(before_hp - enemy["hp"], 3)
        finally:
            source.close()
            env.close()

    def test_direct_hp_loss_bypasses_block_and_triggers_rupture(self):
        env = SimulatorSTSEnv(encounter="SLIME_BOSS")
        try:
            _, info = env.reset(seed=32, options={"piles": {
                "hand": ["IRON_WAVE", "RUPTURE", "BLOODLETTING"],
                "draw_pile": ["DEFEND_RED"] * 7,
                "discard_pile": [],
                "exhaust_pile": [],
            }})
            _, _, _, _, info = play_card(env, info, "IRON_WAVE")
            _, _, _, _, info = play_card(env, info, "RUPTURE")
            hp_before = info["battle"]["player"]["hp"]
            block_before = info["battle"]["player"]["block"]
            _, _, _, _, info = play_card(env, info, "BLOODLETTING")
            player = info["battle"]["player"]
            powers = {
                normalize_power_id(power["id"]): power["amount"]
                for power in player["powers"]
            }
            self.assertEqual(hp_before - player["hp"], 3)
            self.assertEqual(player["block"], block_before)
            self.assertEqual(powers["STRENGTH"], 1)
        finally:
            env.close()

    def test_draw_respects_ten_card_hand_limit_without_losing_cards(self):
        env = SimulatorSTSEnv(encounter="SLIME_BOSS")
        try:
            _, info = env.reset(seed=33, options={"piles": {
                "hand": ["BATTLE_TRANCE"] + ["DEFEND_RED"] * 9,
                "draw_pile": ["STRIKE_RED"] * 3,
                "discard_pile": [],
                "exhaust_pile": [],
            }})
            _, _, _, _, info = play_card(env, info, "BATTLE_TRANCE")
            battle = info["battle"]
            self.assertEqual(len(battle["hand"]), 10)
            self.assertEqual(len(battle["draw_pile"]), 2)
            self.assertEqual(len(battle["discard_pile"]), 1)
        finally:
            env.close()

    def test_unplayed_ethereal_card_exhausts_at_end_of_turn(self):
        env = SimulatorSTSEnv(encounter="LAGAVULIN")
        try:
            _, info = env.reset(seed=34, options={"piles": {
                "hand": ["GHOSTLY_ARMOR", "DEFEND_RED"],
                "draw_pile": ["STRIKE_RED"] * 8,
                "discard_pile": [],
                "exhaust_pile": [],
            }})
            _, _, terminated, _, info = end_turn(env, info)
            self.assertFalse(terminated)
            self.assertIn(
                "GHOSTLY_ARMOR",
                {card["id"] for card in info["battle"]["exhaust_pile"]},
            )
        finally:
            env.close()

    def test_turn_start_resets_block_and_recharges_energy(self):
        env = SimulatorSTSEnv(encounter="LAGAVULIN")
        try:
            _, info = env.reset(seed=35, options={"piles": {
                "hand": ["DEFEND_RED"],
                "draw_pile": ["STRIKE_RED"] * 9,
                "discard_pile": [],
                "exhaust_pile": [],
            }})
            _, _, _, _, info = play_card(env, info, "DEFEND_RED")
            self.assertEqual(info["battle"]["player"]["block"], 5)
            self.assertEqual(info["battle"]["player"]["energy"], 2)
            _, _, terminated, _, info = end_turn(env, info)
            self.assertFalse(terminated)
            self.assertEqual(info["battle"]["player"]["block"], 0)
            self.assertEqual(info["battle"]["player"]["energy"], 3)
        finally:
            env.close()

    def test_barricade_retains_block_across_turn_boundary(self):
        env = SimulatorSTSEnv(encounter="LAGAVULIN")
        try:
            _, info = env.reset(seed=36, options={"piles": {
                "hand": [{"id": "BARRICADE", "upgrades": 1}, "DEFEND_RED"],
                "draw_pile": ["STRIKE_RED"] * 8,
                "discard_pile": [],
                "exhaust_pile": [],
            }})
            _, _, _, _, info = play_card(env, info, "BARRICADE")
            _, _, _, _, info = play_card(env, info, "DEFEND_RED")
            _, _, terminated, _, info = end_turn(env, info)
            self.assertFalse(terminated)
            self.assertEqual(info["battle"]["player"]["block"], 5)
        finally:
            env.close()


if __name__ == "__main__":
    unittest.main()
