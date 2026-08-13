import unittest
from copy import deepcopy

from spirecomm.envs.original_sts_env import (
    OriginalSTSEnv,
    generate_legal_actions,
    rich_battle_state,
)


def card(name, playable, target, cost=1):
    return {
        "id": name,
        "name": name,
        "type": "ATTACK" if target else "SKILL",
        "rarity": "BASIC",
        "upgrades": 0,
        "has_target": target,
        "cost": cost,
        "uuid": name,
        "is_playable": playable,
        "exhausts": False,
    }


def combat_payload(player_hp=80, enemy_hp=40):
    return {
        "available_commands": ["play", "end", "state"],
        "ready_for_command": True,
        "in_game": True,
        "game_state": {
            "current_hp": player_hp,
            "max_hp": 80,
            "floor": 1,
            "act": 1,
            "gold": 99,
            "seed": 1,
            "class": "IRONCLAD",
            "ascension_level": 0,
            "relics": [],
            "deck": [],
            "map": [],
            "potions": [],
            "act_boss": "The Guardian",
            "is_screen_up": False,
            "screen_type": "NONE",
            "screen_state": {},
            "room_phase": "COMBAT",
            "room_type": "MonsterRoom",
            "combat_state": {
                "turn": 1,
                "cards_discarded_this_turn": 0,
                "player": {
                    "max_hp": 80,
                    "current_hp": player_hp,
                    "block": 0,
                    "energy": 3,
                    "powers": [],
                    "orbs": [],
                },
                "monsters": [
                    {
                        "name": "Jaw Worm",
                        "id": "JawWorm",
                        "max_hp": 40,
                        "current_hp": enemy_hp,
                        "block": 0,
                        "intent": "ATTACK",
                        "half_dead": False,
                        "is_gone": False,
                        "move_adjusted_damage": 11,
                        "move_hits": 1,
                        "powers": [],
                    }
                ],
                "hand": [
                    card("Strike", True, True),
                    card("Defend", True, False),
                    card("Bash", False, True, 2),
                ],
                "draw_pile": [],
                "discard_pile": [],
                "exhaust_pile": [],
                "limbo": [],
            },
        },
    }


class FakeTransport:
    def __init__(self, payloads):
        self.payloads = iter(payloads)
        self.sent = []

    def send(self, command):
        self.sent.append(command)

    def receive(self):
        return next(self.payloads)


class OriginalSTSEnvTests(unittest.TestCase):
    def test_reset_starts_ironclad_a0_with_requested_seed(self):
        menu = {
            "available_commands": ["start", "state"],
            "ready_for_command": True,
            "in_game": False,
        }
        transport = FakeTransport([menu, combat_payload()])
        env = OriginalSTSEnv(transport=transport)

        env.reset(seed=123456789)

        self.assertEqual(transport.sent, ["ready", "start IRONCLAD 0 123456789"])

    def test_return_to_menu_reuses_received_menu_for_next_seed(self):
        initial = combat_payload()
        initial["available_commands"].append("reset_run")
        transition = deepcopy(initial)
        transition["available_commands"].append("wait")
        transition["game_state"]["room_phase"] = "COMPLETE"
        transition["game_state"].pop("combat_state")
        menu = {
            "available_commands": ["start", "state"],
            "ready_for_command": True,
            "in_game": False,
        }
        second = combat_payload()
        second["game_state"]["seed"] = 22
        transport = FakeTransport([initial, transition, menu, second])
        env = OriginalSTSEnv(transport=transport)

        env.reset(seed=11)
        env.return_to_menu()
        env.reset(seed=22)

        self.assertEqual(
            transport.sent,
            ["ready", "reset_run", "wait 100", "start IRONCLAD 0 22"],
        )

    def test_return_to_menu_is_noop_when_game_over_already_reached_menu(self):
        menu = {
            "available_commands": ["start", "state"],
            "ready_for_command": True,
            "in_game": False,
        }
        second = combat_payload()
        transport = FakeTransport([second])
        env = OriginalSTSEnv(transport=transport)
        env._ready_sent = True
        env._parse(menu)

        env.return_to_menu()
        env.reset(seed=22)

        self.assertEqual(transport.sent, ["start IRONCLAD 0 22"])

    def test_parses_requested_battle_fields(self):
        state = rich_battle_state(combat_payload())
        self.assertEqual(state["player"]["hp"], 80)
        self.assertEqual(state["player"]["energy"], 3)
        self.assertEqual([item["name"] for item in state["hand"]], ["Strike", "Defend", "Bash"])
        self.assertEqual(state["enemies"][0]["hp"], 40)
        self.assertEqual(state["enemies"][0]["intent"], "ATTACK")

    def test_generates_only_playable_cards_and_live_targets(self):
        actions = generate_legal_actions(combat_payload())
        self.assertEqual(
            [action.command for action in actions],
            ["play 1 0", "play 2", "end"],
        )

    def test_random_compatible_step_never_sends_an_invalid_action(self):
        initial = combat_payload()
        after_play = combat_payload(enemy_hp=34)
        finished = deepcopy(after_play)
        finished["available_commands"] = ["proceed", "state"]
        finished["game_state"]["room_phase"] = "COMPLETE"
        finished["game_state"].pop("combat_state")

        transport = FakeTransport([initial, after_play, finished])
        env = OriginalSTSEnv(transport=transport)
        observation, info = env.reset(seed=7)
        self.assertTrue(env.observation_space.contains(observation))
        self.assertEqual(transport.sent, ["ready"])

        observation, _, terminated, _, info = env.step(0)
        self.assertFalse(terminated)
        self.assertEqual(transport.sent[-1], "play 1 0")

        end_index = next(
            index for index, action in enumerate(info["legal_actions"])
            if action["command"] == "end"
        )
        _, _, terminated, _, _ = env.step(end_index)
        self.assertTrue(terminated)
        self.assertEqual(transport.sent[-1], "end")

    def test_out_of_range_action_is_not_transmitted(self):
        transport = FakeTransport([combat_payload()])
        env = OriginalSTSEnv(transport=transport)
        env.reset()
        before = list(transport.sent)
        with self.assertRaises(ValueError):
            env.step(127)
        self.assertEqual(transport.sent, before)

    def test_parity_mod_rng_state_is_lifted_to_trace_payload(self):
        payload = combat_payload()
        payload["game_state"]["_rng"] = {
            "ai": {"counter": 3, "seed0": 4, "seed1": 5}
        }
        env = OriginalSTSEnv(transport=FakeTransport([payload]))
        env.reset()
        self.assertEqual(env.payload["_rng"], payload["game_state"]["_rng"])


if __name__ == "__main__":
    unittest.main()
