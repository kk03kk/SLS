import json
import tempfile
import unittest
from pathlib import Path

from spirecomm.checkpoints import (
    export_combat_checkpoint,
    load_combat_checkpoint,
    save_combat_checkpoint,
)
from spirecomm.envs import SimulatorSTSEnv
from spirecomm.differential import compare_battles


class CombatCheckpointTests(unittest.TestCase):
    def test_controlled_card_piles_preserve_order_and_upgrade_count(self):
        env = SimulatorSTSEnv()
        try:
            _, info = env.reset(seed=1, options={"piles": {
                "hand": [{"id": "BASH", "upgrades": 1}, "DEFEND_RED"],
                "draw_pile": ["ANGER", "STRIKE_RED"],
                "discard_pile": ["WOUND"],
                "exhaust_pile": ["SEARING_BLOW+3"],
            }})
            battle = info["battle"]
            self.assertEqual([card["id"] for card in battle["hand"]], ["BASH", "DEFEND_RED"])
            self.assertEqual([card["id"] for card in battle["draw_pile"]], ["ANGER", "STRIKE_RED"])
            self.assertEqual(battle["discard_pile"][0]["id"], "WOUND")
            self.assertEqual(battle["exhaust_pile"][0]["upgrades"], 3)
        finally:
            env.close()

    def test_simulator_checkpoint_is_complete_and_json_roundtrips(self):
        env = SimulatorSTSEnv(encounter="THREE_SENTRIES")
        try:
            env.reset(seed=123)
            checkpoint = export_combat_checkpoint(env.payload)
            self.assertEqual(checkpoint["schema_version"], 1)
            self.assertEqual(checkpoint["game_state"]["encounter"], "THREE_SENTRIES")
            self.assertEqual(
                set(checkpoint["rng"]),
                {"ai", "monster_hp", "shuffle", "card_random", "misc", "potion"},
            )
            json.dumps(checkpoint)

            with tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / "checkpoint.json"
                save_combat_checkpoint(env.payload, path)
                self.assertEqual(load_combat_checkpoint(path), checkpoint)
        finally:
            env.close()

    def test_checkpoint_restore_preserves_state_rng_actions_and_next_step(self):
        source = SimulatorSTSEnv(encounter="THREE_SENTRIES")
        restored = SimulatorSTSEnv()
        try:
            source.reset(seed=123)
            # Advance through one enemy turn so the checkpoint contains changed
            # HP, monster move history, shuffled piles and advanced RNG streams.
            source.step(len(source.legal_actions) - 1)
            checkpoint = export_combat_checkpoint(source.payload)

            _, restored_info = restored.reset(options={"checkpoint": checkpoint})
            source_info = source._info()
            self.assertEqual(
                compare_battles(source_info["battle"], restored_info["battle"]), []
            )
            self.assertEqual(source.payload["_rng"], restored.payload["_rng"])
            self.assertEqual(
                source_info["legal_actions"], restored_info["legal_actions"]
            )

            # The strongest useful round-trip check: feed both copies the same
            # legal action and require the resulting state and RNG to agree.
            action = len(source.legal_actions) - 1
            _, source_reward, source_done, _, source_next = source.step(action)
            _, restored_reward, restored_done, _, restored_next = restored.step(action)
            self.assertEqual(
                compare_battles(source_next["battle"], restored_next["battle"]), []
            )
            self.assertEqual(source.payload["_rng"], restored.payload["_rng"])
            self.assertEqual(source_next["legal_actions"], restored_next["legal_actions"])
            self.assertEqual(source_reward, restored_reward)
            self.assertEqual(source_done, restored_done)
        finally:
            source.close()
            restored.close()


if __name__ == "__main__":
    unittest.main()
