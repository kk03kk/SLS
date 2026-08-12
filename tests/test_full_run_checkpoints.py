import json
import tempfile
import unittest
from pathlib import Path

from spirecomm.checkpoints import (
    FULL_RUN_ORDERED_POOLS,
    FULL_RUN_RNG_STREAMS,
    export_full_run_checkpoint,
    load_full_run_checkpoint,
    save_full_run_checkpoint,
    validate_full_run_checkpoint,
)
from spirecomm.simulator import _lightspeed


def sample_rng():
    return {
        name: {"counter": index, "seed0": index + 1, "seed1": index + 2}
        for index, name in enumerate(FULL_RUN_RNG_STREAMS)
    }


def sample_pools():
    return {name: [f"{name}:0", f"{name}:1"] for name in FULL_RUN_ORDERED_POOLS}


def sample_derived_rng():
    return {"map": {
        "algorithm": "sts.RandomXS128/Map.fromSeed:v1",
        "base_seed": 123,
        "derived_seed": 124,
        "act": 1,
        "ascension": 0,
        "assign_burning_elite": True,
    }}


def sample_run_screen():
    return {
        "player_state": {"current_hp": 80},
        "progress_state": {
            "screen_state": 5,
            "screen_continuation_serialized": True,
        },
        "screen_info": {"screen_state": 5, "complete": True},
    }


class FullRunCheckpointTests(unittest.TestCase):
    def test_native_game_context_restores_all_rng_streams_and_ordered_pools(self):
        source = _lightspeed.LightspeedRunState()
        restored = _lightspeed.LightspeedRunState()
        source.reset(0xFEDCBA9876543210, 20)
        saved = source.snapshot()

        self.assertEqual(set(saved["rng"]), set(FULL_RUN_RNG_STREAMS))
        self.assertEqual(set(saved["ordered_pools"]), set(FULL_RUN_ORDERED_POOLS))
        self.assertEqual(saved["run_state"]["ascension"], 20)
        self.assertEqual(
            saved["run_state"]["math_seed"],
            (0xFEDCBA9876543210 - 897897) % 2**64,
        )
        self.assertEqual(
            saved["derived_rng"]["map"],
            {
                "algorithm": "sts.RandomXS128/Map.fromSeed:v1",
                "base_seed": 0xFEDCBA9876543210,
                "derived_seed": 0xFEDCBA9876543211,
                "act": 1,
                "ascension": 20,
                "assign_burning_elite": True,
            },
        )
        self.assertTrue(saved["run_state"]["map"])

        expected_next = source.advance_all_rng()
        source.advance_all_rng()  # Ensure restoration is not a no-op comparison.
        restored.load_state(saved)
        self.assertEqual(restored.snapshot(), saved)
        self.assertEqual(restored.advance_all_rng(), expected_next)

    def test_native_map_derived_seed_wraps_as_unsigned_64_bit(self):
        state = _lightspeed.LightspeedRunState()
        state.reset(2**64 - 1, 0)
        self.assertEqual(state.snapshot()["derived_rng"]["map"]["derived_seed"], 0)

    def test_native_restores_mutable_run_resources_and_progress(self):
        source = _lightspeed.LightspeedRunState()
        source.reset(424242, 7, 10101)
        saved = source.snapshot()
        player = saved["player_state"]
        player["current_hp"] = 37
        player["max_hp"] = 91
        player["gold"] = 444
        player["blue_key"] = True
        player["green_key"] = True
        player["red_key"] = True
        player["potion_capacity"] = 3
        player["potion_count"] = 0
        player["potions"] = [1, 1, 1]  # EMPTY_POTION_SLOT
        player["deck"][0]["upgraded"] = True
        player["deck"][0]["misc"] = 17
        player["relics"][0]["data"] = 9
        progress = saved["progress_state"]
        progress["current_map_x"] = 3
        progress["current_map_y"] = 7
        progress["monster_chance"] = 0.5
        progress["shop_remove_count"] = 3

        restored = _lightspeed.LightspeedRunState()
        restored.load_state(saved)
        actual = restored.snapshot()
        self.assertEqual(actual["player_state"], player)
        self.assertEqual(actual["progress_state"], progress)
        self.assertEqual(actual["rng"], saved["rng"])
        self.assertEqual(actual["ordered_pools"], saved["ordered_pools"])

    def test_neow_checkpoint_branch_replays_same_action_exactly(self):
        direct = _lightspeed.LightspeedRunState()
        restored = _lightspeed.LightspeedRunState()
        direct.reset(1234567, 0, 7654321)
        checkpoint = direct.snapshot()
        self.assertTrue(checkpoint["screen_info"]["complete"])
        self.assertEqual(len(checkpoint["legal_actions"]), 4)
        bits = checkpoint["legal_actions"][0]["bits"]

        restored.load_state(checkpoint)
        self.assertEqual(restored.legal_actions(), checkpoint["legal_actions"])
        self.assertEqual(direct.step(bits), restored.step(bits))

    def test_run_step_rejects_unadvertised_action(self):
        state = _lightspeed.LightspeedRunState()
        state.reset(123, 0, 456)
        with self.assertRaisesRegex(ValueError, "not legal"):
            state.step(0xFFFFFFFF)

    def test_reward_and_map_checkpoints_replay_same_branches(self):
        direct = _lightspeed.LightspeedRunState()
        restored = _lightspeed.LightspeedRunState()
        direct.reset(1234567, 0, 7654321)

        # This fixed seed's first Neow option opens a card reward screen.
        reward_state = direct.step(direct.legal_actions()[0]["bits"])
        self.assertEqual(reward_state["progress_state"]["screen_state"], 2)
        self.assertTrue(reward_state["screen_info"]["complete"])
        restored.load_state(reward_state)
        skip_bits = reward_state["legal_actions"][-1]["bits"]
        direct_after_skip = direct.step(skip_bits)
        restored_after_skip = restored.step(skip_bits)
        self.assertEqual(direct_after_skip, restored_after_skip)

        # Skipping Neow's reward returns to the first map fork. Branching from
        # that checkpoint must choose the same room and consume the same RNG.
        self.assertEqual(direct_after_skip["progress_state"]["screen_state"], 5)
        map_checkpoint = direct_after_skip
        map_restored = _lightspeed.LightspeedRunState()
        map_restored.load_state(map_checkpoint)
        map_bits = map_checkpoint["legal_actions"][0]["bits"]
        self.assertEqual(direct.step(map_bits), map_restored.step(map_bits))

    def test_explicit_math_seed_controls_only_the_math_util_stream(self):
        first = _lightspeed.LightspeedRunState()
        second = _lightspeed.LightspeedRunState()
        first.reset(123, 0, 111)
        second.reset(123, 0, 222)
        first_rng = first.snapshot()["rng"]
        second_rng = second.snapshot()["rng"]
        self.assertNotEqual(first_rng["math_util"], second_rng["math_util"])
        self.assertEqual(
            {name: value for name, value in first_rng.items() if name != "math_util"},
            {name: value for name, value in second_rng.items() if name != "math_util"},
        )

    def test_courier_colored_restock_preserves_purchased_card_type(self):
        examples = {
            "ANGER": "ATTACK",
            "TRUE_GRIT": "SKILL",
            "INFLAME": "POWER",
        }
        for card, expected_type in examples.items():
            with self.subTest(card=card):
                state = _lightspeed.LightspeedRunState()
                state.reset(123456789, 0, 987654321)
                result = state.courier_restock_probe(card)
                self.assertEqual(result["purchased_type"], expected_type)
                self.assertEqual(result["restocked_type"], expected_type)
                if expected_type == "POWER":
                    self.assertNotEqual(result["restocked_rarity"], "COMMON")

                changed = {
                    name
                    for name in result["rng_before"]
                    if result["rng_before"][name] != result["rng_after"][name]
                }
                self.assertEqual(changed, {"card", "math_util", "merchant"})

    def test_schema_is_json_safe_and_roundtrips(self):
        checkpoint = export_full_run_checkpoint(
            reference_build={"game_sha256": "abc", "simulator_commit": "def"},
            run_state={"seed": 123, "math_seed": 456, "floor": 7, "screen": "MAP"},
            rng=sample_rng(),
            derived_rng=sample_derived_rng(),
            ordered_pools=sample_pools(),
            **sample_run_screen(),
            legal_actions=[{"kind": "choose_map_node", "choice_index": 0}],
        )
        self.assertEqual(len(checkpoint["rng"]), 14)
        json.dumps(checkpoint)

        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "full-run.json"
            save_full_run_checkpoint(checkpoint, path)
            self.assertEqual(load_full_run_checkpoint(path), checkpoint)

    def test_native_snapshot_fits_strict_full_run_schema_v2(self):
        state = _lightspeed.LightspeedRunState()
        state.reset(987654321, 10, 123456789)
        native = state.snapshot()
        checkpoint = export_full_run_checkpoint(
            reference_build={"game_sha256": "pinned", "simulator_commit": "pinned"},
            run_state=native["run_state"],
            rng=native["rng"],
            derived_rng=native["derived_rng"],
            ordered_pools=native["ordered_pools"],
            player_state=native["player_state"],
            progress_state=native["progress_state"],
            screen_info=native["screen_info"],
            legal_actions=native["legal_actions"],
        )
        self.assertEqual(checkpoint["schema_version"], 2)
        json.dumps(checkpoint)

    def test_missing_rng_stream_is_rejected(self):
        checkpoint = export_full_run_checkpoint(
            reference_build={"game_sha256": "abc"},
            run_state={"seed": 123, "math_seed": 456},
            rng=sample_rng(),
            derived_rng=sample_derived_rng(),
            ordered_pools=sample_pools(),
            **sample_run_screen(),
            legal_actions=[],
        )
        del checkpoint["rng"]["treasure"]
        with self.assertRaisesRegex(ValueError, "all 14 RNG streams"):
            validate_full_run_checkpoint(checkpoint)

    def test_incomplete_screen_continuation_is_rejected(self):
        checkpoint = export_full_run_checkpoint(
            reference_build={"game_sha256": "abc"},
            run_state={"seed": 123, "math_seed": 456},
            rng=sample_rng(),
            derived_rng=sample_derived_rng(),
            ordered_pools=sample_pools(),
            **sample_run_screen(),
            legal_actions=[],
        )
        checkpoint["screen_info"]["complete"] = False
        checkpoint["progress_state"]["screen_continuation_serialized"] = False
        with self.assertRaisesRegex(ValueError, "serialized screen continuation"):
            validate_full_run_checkpoint(checkpoint)

    def test_invalid_rng_bits_and_missing_pool_are_rejected(self):
        checkpoint = export_full_run_checkpoint(
            reference_build={"game_sha256": "abc"},
            run_state={"seed": 123, "math_seed": 456},
            rng=sample_rng(),
            derived_rng=sample_derived_rng(),
            ordered_pools=sample_pools(),
            **sample_run_screen(),
            legal_actions=[],
        )
        checkpoint["rng"]["ai"]["seed0"] = 2**64
        with self.assertRaisesRegex(ValueError, "invalid seed0"):
            validate_full_run_checkpoint(checkpoint)

        checkpoint["rng"]["ai"]["seed0"] = 1
        del checkpoint["ordered_pools"]["events"]
        with self.assertRaisesRegex(ValueError, "every ordered content pool"):
            validate_full_run_checkpoint(checkpoint)


if __name__ == "__main__":
    unittest.main()
