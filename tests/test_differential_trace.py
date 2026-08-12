import copy
import tempfile
import unittest
from pathlib import Path

from spirecomm.differential import (
    compare_battles, compare_rng_states, infer_act1_encounter, load_trace,
    record_episode, replay_trace,
)
from spirecomm.differential.trace import _command_action
from spirecomm.envs import SimulatorSTSEnv


class DifferentialTraceTests(unittest.TestCase):
    def test_rng_diff_reports_first_stream_and_state_field(self):
        expected = {
            "ai": {"counter": 3, "seed0": 10, "seed1": 11},
            "shuffle": {"counter": 1, "seed0": 20, "seed1": 21},
        }
        actual = copy.deepcopy(expected)
        actual["shuffle"]["counter"] = 2
        actual["shuffle"]["seed0"] = 99
        differences = compare_rng_states(expected, actual)
        self.assertEqual(len(differences), 1)
        self.assertEqual(differences[0].path, "rng.shuffle.counter")
        self.assertEqual((differences[0].expected, differences[0].actual), (1, 2))

    def test_all_act1_original_monster_groups_map_to_catalog_encounters(self):
        groups = {
            "CULTIST": ["Cultist"], "JAW_WORM": ["JawWorm"],
            "TWO_LOUSE": ["RedLouse", "GreenLouse"],
            "SMALL_SLIMES": ["SpikeSlime_S", "AcidSlime_M"],
            "BLUE_SLAVER": ["BlueSlaver"],
            "GREMLIN_GANG": ["MadGremlin", "SneakyGremlin", "FatGremlin", "GremlinWizard"],
            "LOOTER": ["Looter"], "LARGE_SLIME": ["AcidSlime_L"],
            "LOTS_OF_SLIMES": ["SpikeSlime_S"] * 3 + ["AcidSlime_S"] * 2,
            "EXORDIUM_THUGS": ["GreenLouse", "RedSlaver"],
            "EXORDIUM_WILDLIFE": ["JawWorm", "AcidSlime_M"],
            "RED_SLAVER": ["RedSlaver"],
            "THREE_LOUSE": ["RedLouse", "GreenLouse", "RedLouse"],
            "TWO_FUNGI_BEASTS": ["FungiBeast", "FungiBeast"],
            "GREMLIN_NOB": ["GremlinNob"], "LAGAVULIN": ["Lagavulin"],
            "THREE_SENTRIES": ["Sentry", "Sentry", "Sentry"],
            "SLIME_BOSS": ["SlimeBoss"], "THE_GUARDIAN": ["TheGuardian"],
            "HEXAGHOST": ["Hexaghost"],
        }
        for encounter, monster_ids in groups.items():
            with self.subTest(encounter):
                self.assertEqual(
                    infer_act1_encounter([{"id": value} for value in monster_ids]),
                    encounter,
                )

        self.assertEqual(
            infer_act1_encounter([
                {"id": "FuzzyLouseNormal"},
                {"id": "FuzzyLouseDefensive"},
            ]),
            "TWO_LOUSE",
        )

    def test_protocol_commands_preserve_all_agent_action_fields(self):
        self.assertEqual(_command_action("potion use 2 1"), {
            "kind": "potion", "card_index": None, "potion_index": 2,
            "target_index": 1, "choice_index": None,
        })
        self.assertEqual(_command_action("potion discard 0"), {
            "kind": "discard_potion", "card_index": None, "potion_index": 0,
            "target_index": None, "choice_index": None,
        })
        self.assertEqual(_command_action("confirm")["kind"], "proceed")
        self.assertEqual(_command_action("skip")["kind"], "cancel")

    def test_recorded_trace_replays_exactly(self):
        try:
            source = SimulatorSTSEnv()
            replay = SimulatorSTSEnv()
        except RuntimeError as exc:
            self.skipTest(str(exc))
        try:
            with tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / "trace.json"
                record_episode(
                    source,
                    lambda _observation, info: len(info["legal_actions"]) - 1,
                    path,
                    seed=99,
                )
                differences = replay_trace(replay, load_trace(path))
                self.assertEqual(differences, [])
        finally:
            source.close()
            replay.close()

    def test_strict_trace_detects_legal_action_reward_and_outcome_drift(self):
        source = SimulatorSTSEnv()
        replay = SimulatorSTSEnv()
        try:
            with tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / "trace.json"
                trace = record_episode(
                    source,
                    lambda _observation, info: len(info["legal_actions"]) - 1,
                    path,
                    seed=101,
                )

                changed = copy.deepcopy(trace)
                changed["initial_legal_actions"].pop()
                self.assertEqual(
                    replay_trace(replay, changed)[0].path,
                    "initial_legal_actions.length",
                )

                changed = copy.deepcopy(trace)
                changed["steps"][0]["rng"]["ai"]["counter"] += 1
                self.assertEqual(
                    replay_trace(replay, changed)[0].path,
                    "step[1].rng.ai.counter",
                )

                changed = copy.deepcopy(trace)
                changed["steps"][0]["reward"] += 1.0
                self.assertEqual(replay_trace(replay, changed)[0].path, "step[1].reward")

                changed = copy.deepcopy(trace)
                changed["outcome"] = (
                    "PLAYER_VICTORY"
                    if trace["outcome"] != "PLAYER_VICTORY" else "PLAYER_LOSS"
                )
                self.assertEqual(replay_trace(replay, changed)[0].path, "outcome")
        finally:
            source.close()
            replay.close()

    def test_battle_diff_compares_move_id_potions_and_relic_counters(self):
        env = SimulatorSTSEnv(encounter="JAW_WORM")
        try:
            _, info = env.reset(
                seed=7,
                options={"potions": ["Fire Potion"], "relics": ["Burning Blood"]},
            )
            actual = info["battle"]
            original_form = copy.deepcopy(actual)
            original_form["enemies"][0]["move_id"] = 1
            self.assertEqual(compare_battles(original_form, actual), [])

            mutations = (
                ("battle.enemies[0].move_id", lambda battle: battle["enemies"][0].update(move_id=2)),
                ("battle.potions[0].can_use", lambda battle: battle["potions"][0].update(can_use=False)),
                ("battle.relics[0].counter", lambda battle: battle["relics"][0].update(counter=9)),
            )
            for expected_path, mutate in mutations:
                with self.subTest(expected_path):
                    changed = copy.deepcopy(original_form)
                    mutate(changed)
                    self.assertEqual(compare_battles(changed, actual)[0].path, expected_path)
        finally:
            env.close()


if __name__ == "__main__":
    unittest.main()
