import unittest
from collections import Counter

from spirecomm.checkpoints import export_combat_checkpoint
from spirecomm.envs import SimulatorSTSEnv


NORMAL_ENCOUNTERS = (
    "CULTIST", "JAW_WORM", "TWO_LOUSE", "SMALL_SLIMES", "BLUE_SLAVER",
    "GREMLIN_GANG", "LOOTER", "LARGE_SLIME", "LOTS_OF_SLIMES",
    "EXORDIUM_THUGS", "EXORDIUM_WILDLIFE", "RED_SLAVER", "THREE_LOUSE",
    "TWO_FUNGI_BEASTS",
)


def combat(env):
    return env.payload["game_state"]["combat_state"]


def active_monsters(env):
    return [monster for monster in combat(env)["monsters"] if not monster["is_gone"]]


def end_turn(env, info):
    index = next(
        index for index, action in enumerate(info["legal_actions"])
        if action["kind"] == "end_turn"
    )
    return env.step(index)


def durable_env(encounter, seed=0, *, piles=None):
    source = SimulatorSTSEnv(encounter=encounter)
    target = SimulatorSTSEnv()
    options = {} if piles is None else {"piles": piles}
    source.reset(seed=seed, options=options)
    checkpoint = export_combat_checkpoint(source.payload)
    game = checkpoint["game_state"]
    player = game["combat_state"]["player"]
    player["current_hp"] = player["max_hp"] = 999
    game["current_hp"] = game["max_hp"] = 999
    _, info = target.reset(options={"checkpoint": checkpoint})
    source.close()
    return target, info


class Act1NormalCompositionTests(unittest.TestCase):
    def test_all_fourteen_normal_encounters_have_original_compositions(self):
        exact = {
            "CULTIST": ["CULTIST"],
            "JAW_WORM": ["JAW_WORM"],
            "BLUE_SLAVER": ["BLUE_SLAVER"],
            "LOOTER": ["LOOTER"],
            "RED_SLAVER": ["RED_SLAVER"],
            "TWO_FUNGI_BEASTS": ["FUNGI_BEAST", "FUNGI_BEAST"],
        }
        louses = {"RED_LOUSE", "GREEN_LOUSE"}
        weak_wildlife = louses | {"SPIKE_SLIME_M", "ACID_SLIME_M"}
        strong_humanoid = {"CULTIST", "RED_SLAVER", "BLUE_SLAVER", "LOOTER"}
        strong_wildlife = {"FUNGI_BEAST", "JAW_WORM"}
        gremlins = {
            "MAD_GREMLIN", "SNEAKY_GREMLIN", "FAT_GREMLIN",
            "SHIELD_GREMLIN", "GREMLIN_WIZARD",
        }

        for encounter in NORMAL_ENCOUNTERS:
            env = SimulatorSTSEnv(encounter=encounter)
            try:
                for seed in range(16):
                    env.reset(seed=seed)
                    ids = [monster["monster_id"] for monster in active_monsters(env)]
                    if encounter in exact:
                        self.assertEqual(ids, exact[encounter], (encounter, seed))
                    elif encounter == "TWO_LOUSE":
                        self.assertEqual(len(ids), 2)
                        self.assertLessEqual(set(ids), louses)
                    elif encounter == "THREE_LOUSE":
                        self.assertEqual(len(ids), 3)
                        self.assertLessEqual(set(ids), louses)
                    elif encounter == "SMALL_SLIMES":
                        self.assertIn(ids, [
                            ["SPIKE_SLIME_S", "ACID_SLIME_M"],
                            ["ACID_SLIME_S", "SPIKE_SLIME_M"],
                        ])
                    elif encounter == "LARGE_SLIME":
                        self.assertIn(ids, [["SPIKE_SLIME_L"], ["ACID_SLIME_L"]])
                    elif encounter == "LOTS_OF_SLIMES":
                        self.assertEqual(
                            Counter(ids), Counter({"SPIKE_SLIME_S": 3, "ACID_SLIME_S": 2})
                        )
                    elif encounter == "GREMLIN_GANG":
                        counts = Counter(ids)
                        self.assertEqual(len(ids), 4)
                        self.assertLessEqual(set(ids), gremlins)
                        self.assertLessEqual(counts["MAD_GREMLIN"], 2)
                        self.assertLessEqual(counts["SNEAKY_GREMLIN"], 2)
                        self.assertLessEqual(counts["FAT_GREMLIN"], 2)
                        self.assertLessEqual(counts["SHIELD_GREMLIN"], 1)
                        self.assertLessEqual(counts["GREMLIN_WIZARD"], 1)
                    elif encounter == "EXORDIUM_THUGS":
                        self.assertIn(ids[0], weak_wildlife)
                        self.assertIn(ids[1], strong_humanoid)
                    elif encounter == "EXORDIUM_WILDLIFE":
                        self.assertIn(ids[0], strong_wildlife)
                        self.assertIn(ids[1], weak_wildlife)
            finally:
                env.close()


class Act1NormalMoveTests(unittest.TestCase):
    def trace_moves(self, encounter, seed, turns, monster_index=0):
        env, info = durable_env(encounter, seed)
        try:
            observed = []
            for _ in range(turns):
                observed.append(combat(env)["monsters"][monster_index]["move_id"])
                _, _, terminated, _, info = end_turn(env, info)
                if terminated:
                    break
            return observed
        finally:
            env.close()

    def test_cultist_incants_once_then_ritual_scales_dark_strike(self):
        env, info = durable_env("CULTIST")
        try:
            monster = active_monsters(env)[0]
            self.assertEqual(monster["move_id"], "CULTIST_INCANTATION")
            _, _, _, _, info = end_turn(env, info)
            monster = active_monsters(env)[0]
            self.assertEqual(monster["move_id"], "CULTIST_DARK_STRIKE")
            self.assertEqual(next(p["amount"] for p in monster["powers"] if p["id"] == "Ritual"), 3)
            damages = []
            for _ in range(3):
                damages.append(active_monsters(env)[0]["move_adjusted_damage"])
                _, _, _, _, info = end_turn(env, info)
            self.assertEqual(damages, [6, 9, 12])
        finally:
            env.close()

    def test_jaw_worm_move_history_constraints_hold_across_seeds(self):
        for seed in range(16):
            moves = self.trace_moves("JAW_WORM", seed, 14)
            self.assertEqual(moves[0], "JAW_WORM_CHOMP")
            for i, move in enumerate(moves):
                if i and move in {"JAW_WORM_CHOMP", "JAW_WORM_BELLOW"}:
                    self.assertNotEqual(move, moves[i - 1], (seed, moves))
                if i >= 2 and move == "JAW_WORM_THRASH":
                    self.assertNotEqual(moves[i - 2:i], [move, move], (seed, moves))

    def test_fungi_louse_and_blue_slaver_repetition_limits(self):
        cases = (
            ("TWO_FUNGI_BEASTS", 2, "FUNGI_BEAST_BITE", "FUNGI_BEAST_GROW", 1),
            ("TWO_LOUSE", 2, "_BITE", "_SPIT_WEB", 2),
            ("BLUE_SLAVER", 1, "BLUE_SLAVER_STAB", "BLUE_SLAVER_RAKE", 2),
        )
        for encounter, count, attack, alternate, alternate_limit in cases:
            for seed in range(12):
                for monster_index in range(count):
                    moves = self.trace_moves(encounter, seed, 12, monster_index)
                    normalized = [
                        "attack" if attack in move else "alternate" if alternate in move else move
                        for move in moves
                    ]
                    self.assertNotIn(["attack"] * 3, [normalized[i:i + 3] for i in range(len(normalized) - 2)])
                    self.assertNotIn(
                        ["alternate"] * (alternate_limit + 1),
                        [normalized[i:i + alternate_limit + 1] for i in range(len(normalized) - alternate_limit)],
                    )

    def test_looter_mugs_twice_then_escapes_through_valid_route(self):
        for seed in range(12):
            moves = self.trace_moves("LOOTER", seed, 6)
            self.assertEqual(moves[:2], ["LOOTER_MUG", "LOOTER_MUG"])
            self.assertIn(moves[2], {"LOOTER_LUNGE", "LOOTER_SMOKE_BOMB"})
            expected = (
                ["LOOTER_LUNGE", "LOOTER_SMOKE_BOMB", "LOOTER_ESCAPE"]
                if moves[2] == "LOOTER_LUNGE"
                else ["LOOTER_SMOKE_BOMB", "LOOTER_ESCAPE"]
            )
            self.assertEqual(moves[2:], expected)

    def test_red_slaver_entangles_at_most_once_and_never_opens_with_it(self):
        for seed in range(24):
            moves = self.trace_moves("RED_SLAVER", seed, 18)
            self.assertEqual(moves[0], "RED_SLAVER_STAB")
            self.assertLessEqual(moves.count("RED_SLAVER_ENTANGLE"), 1, (seed, moves))
            for move in {"RED_SLAVER_STAB", "RED_SLAVER_SCRAPE"}:
                self.assertNotIn([move] * 3, [moves[i:i + 3] for i in range(len(moves) - 2)])

    def test_gremlin_fixed_moves_and_wizard_charge_cycle(self):
        # seed + floor drives encounter composition in the base game.  Seed 2
        # contains one of each gremlin needed by this state-machine assertion.
        env, info = durable_env("GREMLIN_GANG", seed=2)
        try:
            by_id = {monster["monster_id"]: i for i, monster in enumerate(active_monsters(env))}
            expected_fixed = {
                "FAT_GREMLIN": "FAT_GREMLIN_SMASH",
                "MAD_GREMLIN": "MAD_GREMLIN_SCRATCH",
                "SNEAKY_GREMLIN": "SNEAKY_GREMLIN_PUNCTURE",
            }
            wizard_moves = []
            for _ in range(7):
                monsters = combat(env)["monsters"]
                for monster_id, move_id in expected_fixed.items():
                    self.assertEqual(monsters[by_id[monster_id]]["move_id"], move_id)
                wizard_moves.append(monsters[by_id["GREMLIN_WIZARD"]]["move_id"])
                _, _, _, _, info = end_turn(env, info)
            self.assertEqual(wizard_moves, [
                "GREMLIN_WIZARD_CHARGING", "GREMLIN_WIZARD_CHARGING",
                "GREMLIN_WIZARD_ULTIMATE_BLAST", "GREMLIN_WIZARD_CHARGING",
                "GREMLIN_WIZARD_CHARGING", "GREMLIN_WIZARD_CHARGING",
                "GREMLIN_WIZARD_ULTIMATE_BLAST",
            ])
        finally:
            env.close()

    def test_large_slimes_split_into_matching_small_and_medium_children(self):
        found = {}
        for seed in range(32):
            source = SimulatorSTSEnv(encounter="LARGE_SLIME")
            target = SimulatorSTSEnv()
            try:
                source.reset(seed=seed, options={"piles": {
                    "hand": ["ANGER"], "draw_pile": ["STRIKE_RED"],
                    "discard_pile": [], "exhaust_pile": [],
                }})
                checkpoint = export_combat_checkpoint(source.payload)
                parent = checkpoint["game_state"]["combat_state"]["monsters"][0]
                parent_id = parent["monster_id"]
                if parent_id in found:
                    continue
                parent["current_hp"] = parent["max_hp"] // 2 + 3
                _, info = target.reset(options={"checkpoint": checkpoint})
                play = next(i for i, action in enumerate(info["legal_actions"]) if action["kind"] == "play")
                _, _, _, _, info = target.step(play)
                self.assertTrue(active_monsters(target)[0]["move_id"].endswith("_SPLIT"))
                _, _, _, _, info = end_turn(target, info)
                children = active_monsters(target)
                child_ids = [monster["monster_id"] for monster in children]
                prefix = "ACID" if parent_id.startswith("ACID") else "SPIKE"
                self.assertEqual(child_ids, [f"{prefix}_SLIME_M", f"{prefix}_SLIME_M"])
                found[parent_id] = True
                if len(found) == 2:
                    break
            finally:
                source.close()
                target.close()
        self.assertEqual(set(found), {"ACID_SLIME_L", "SPIKE_SLIME_L"})


if __name__ == "__main__":
    unittest.main()
