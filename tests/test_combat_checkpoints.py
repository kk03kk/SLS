import json
import tempfile
import unittest
from pathlib import Path

from spirecomm.checkpoints import (
    FULL_RUN_RNG_STREAMS,
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
                set(FULL_RUN_RNG_STREAMS),
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

    def test_checkpoint_preserves_ordered_orbs_and_dark_evoke_amount(self):
        source = SimulatorSTSEnv()
        restored = SimulatorSTSEnv()
        try:
            source.reset(seed=124)
            checkpoint = export_combat_checkpoint(source.payload)
            player = checkpoint["game_state"]["combat_state"]["player"]
            player["max_orbs"] = 3
            player["orbs"] = [
                {"id": "Lightning", "name": "Lightning", "passive_amount": 3, "evoke_amount": 8},
                {"id": "Dark", "name": "Dark", "passive_amount": 6, "evoke_amount": 19},
                {"id": "Empty", "name": "Empty", "passive_amount": 0, "evoke_amount": 0},
            ]
            internal = player["_internal"]
            internal["orb_slots"] = 3
            internal["orbs"] = [4, 1, 0] + [0] * 7
            internal["orb_evoke_amounts"] = [0, 19, 0] + [0] * 7

            _, info = restored.reset(options={"checkpoint": checkpoint})
            self.assertEqual(info["battle"]["player"]["max_orbs"], 3)
            self.assertEqual(
                [(orb["id"], orb["evoke_amount"]) for orb in info["battle"]["player"]["orbs"]],
                [("Lightning", 8), ("Dark", 19), ("Empty", 0)],
            )
            roundtrip = export_combat_checkpoint(restored.payload)
            restored_internal = roundtrip["game_state"]["combat_state"]["player"]["_internal"]
            self.assertEqual(restored_internal["orbs"][:3], [4, 1, 0])
            self.assertEqual(restored_internal["orb_evoke_amounts"][:3], [0, 19, 0])
        finally:
            source.close()
            restored.close()


if __name__ == "__main__":
    unittest.main()
