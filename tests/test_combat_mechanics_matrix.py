import unittest
import json
from pathlib import Path

from spirecomm.simulator.mechanics import load_combat_mechanics_matrix


ROOT = Path(__file__).resolve().parents[1]


class CombatMechanicsMatrixTests(unittest.TestCase):
    def test_matrix_has_stable_denominator_and_required_areas(self):
        matrix = load_combat_mechanics_matrix()
        self.assertEqual(matrix.counts(), {
            "total": 50,
            "partial": 46,
            "unimplemented": 3,
            "implemented": 1,
        })
        self.assertEqual(
            {item["area"] for item in matrix.mechanics},
            {
                "action_queue", "damage", "resources", "powers",
                "card_zones", "choices", "lifecycle", "stances_orbs",
                "randomness",
            },
        )

    def test_all_evidence_files_exist(self):
        matrix = load_combat_mechanics_matrix()
        for item in matrix.mechanics:
            for relative_path in item["evidence_files"]:
                self.assertTrue((ROOT / relative_path).is_file(), (item["id"], relative_path))

    def test_known_upstream_holes_are_not_claimed_implemented(self):
        matrix = load_combat_mechanics_matrix()
        for mechanic_id in (
            "orb.slots", "orb.channel_evoke", "orb.passive_focus"
        ):
            self.assertEqual(matrix.get(mechanic_id)["implementation"], "unimplemented")
            self.assertEqual(matrix.get(mechanic_id)["evidence"], "none")

    def test_matrix_is_bound_to_reference_simulator_commit(self):
        matrix = load_combat_mechanics_matrix()
        reference = json.loads((ROOT / "reference_build.json").read_text(encoding="utf-8"))
        self.assertEqual(
            matrix.payload["target_source_commit"],
            reference["simulator_base"]["commit"],
        )


if __name__ == "__main__":
    unittest.main()
