import json
import unittest
from pathlib import Path

from spirecomm.simulator import _lightspeed


VECTORS = Path(__file__).with_name("fixtures") / "rng_vectors.jsonl"
SHUFFLE_VECTORS = Path(__file__).with_name("fixtures") / "shuffle_vectors.jsonl"


class RngParityTests(unittest.TestCase):
    def test_native_java_collections_shuffle_matches_original_jdk_vectors(self):
        vectors = [
            json.loads(line)
            for line in SHUFFLE_VECTORS.read_text().splitlines()
            if line
        ]
        self.assertGreaterEqual(len(vectors), 5)
        for expected in vectors:
            with self.subTest(seed_bits=expected["seed_bits"]):
                self.assertEqual(
                    _lightspeed.shuffle_probe(int(expected["seed_bits"])),
                    expected["values"],
                )

    def test_native_rng_matches_original_game_vectors(self):
        vectors = [json.loads(line) for line in VECTORS.read_text().splitlines() if line]
        self.assertGreaterEqual(len(vectors), 5)
        for expected in vectors:
            with self.subTest(seed_bits=expected["seed_bits"]):
                actual = _lightspeed.rng_probe(int(expected["seed_bits"]))
                self.assertEqual(actual["seed_bits"], int(expected["seed_bits"]))
                self.assertEqual(actual["initial"], {
                    "counter": expected["initial"]["counter"],
                    "seed0": int(expected["initial"]["seed0"]),
                    "seed1": int(expected["initial"]["seed1"]),
                })
                self.assertEqual(actual["values"]["range_999"], expected["values"]["range_999"])
                self.assertEqual(actual["values"]["between_5_12"], expected["values"]["between_5_12"])
                self.assertEqual(actual["values"]["long_range"], int(expected["values"]["long_range"]))
                self.assertEqual(actual["values"]["random_long"], int(expected["values"]["random_long"]))
                self.assertEqual(actual["values"]["boolean"], expected["values"]["boolean"])
                self.assertEqual(actual["values"]["chance_0_375"], expected["values"]["chance_0_375"])
                self.assertAlmostEqual(actual["values"]["unit_float"], float(expected["values"]["unit_float"]), places=7)
                self.assertAlmostEqual(actual["values"]["float_range"], float(expected["values"]["float_range"]), places=6)
                self.assertAlmostEqual(actual["values"]["float_between"], float(expected["values"]["float_between"]), places=6)
                self.assertEqual(actual["final"], {
                    "counter": expected["final"]["counter"],
                    "seed0": int(expected["final"]["seed0"]),
                    "seed1": int(expected["final"]["seed1"]),
                })


if __name__ == "__main__":
    unittest.main()
