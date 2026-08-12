import subprocess
import sys
import unittest
from pathlib import Path

from spirecomm.content import load_content_registry
from spirecomm.envs.vocab import IRONCLAD_CARD_IDS
from spirecomm.simulator.catalog import ACT1_ENCOUNTERS


ROOT = Path(__file__).resolve().parents[1]


class ContentRegistryTests(unittest.TestCase):
    def test_frozen_category_denominators(self):
        registry = load_content_registry()
        self.assertEqual(
            {name: len(items) for name, items in registry.categories.items()},
            {
                "characters": 4,
                "cards": 370,
                "relics": 180,
                "potions": 43,
                "monsters": 65,
                "encounters": 63,
                "events": 56,
            },
        )
        self.assertEqual(sum(len(items) for items in registry.categories.values()), 781)

    def test_every_item_has_valid_status_and_unique_identity(self):
        registry = load_content_registry()  # Constructor performs schema validation.
        for category, items in registry.categories.items():
            self.assertTrue(items, category)
            self.assertNotIn("INVALID", {item["id"] for item in items})

    def test_inherited_slices_are_partial_not_claimed_complete(self):
        registry = load_content_registry()
        for card_id in IRONCLAD_CARD_IDS:
            item = registry.get("cards", card_id)
            self.assertEqual(item["implementation"], "partial")
            self.assertEqual(item["evidence"], "unit")
        for encounter_id in ACT1_ENCOUNTERS:
            self.assertEqual(
                registry.get("encounters", encounter_id)["implementation"], "partial"
            )
        self.assertFalse(any(
            item["implementation"] == "implemented"
            for items in registry.categories.values() for item in items
        ))

    def test_generator_reproduces_committed_artifact(self):
        result = subprocess.run(
            [sys.executable, "scripts/build_content_registry.py", "--check"],
            cwd=ROOT,
            text=True,
            capture_output=True,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
