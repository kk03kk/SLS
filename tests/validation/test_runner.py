from __future__ import annotations

from dataclasses import dataclass

from sls.contracts import Decision, Observation, Player, RunContext, ScreenType
from sls.validation import run_paired, summarize


def observation() -> Observation:
    return Observation(
        Player("IRONCLAD", 80, 80, 0, 0, 3),
        RunContext(0, 1, 0, 99, False, False, False),
        ScreenType.GAME_OVER,
    )


ORIGINAL_RAW = {
    "game_state": {
        "act": 1, "floor": 0, "current_hp": 80, "max_hp": 80,
        "gold": 99, "act_boss": "INVALID", "deck": [], "relics": [],
        "potions": [], "map": [], "_parity_run": {},
    },
    "_rng": {},
}
SIMULATOR_RAW = {
    "public_run": {"act": 1, "floor": 0, "gold": 99, "visible_boss_id": "INVALID"},
    "player_state": {
        "current_hp": 80, "max_hp": 80,
        "red_key": False, "green_key": False, "blue_key": False,
    },
    "public_inventory": {"deck": [], "relics": [], "potions": []},
    "public_map": [], "rng": {},
}


class Original:
    raw_payload = ORIGINAL_RAW

    def reset(self, seed: int) -> Decision:
        return Decision(observation(), (), True)


@dataclass
class Profile:
    profile_id: str = "TEST"


class Simulator:
    raw_state = SIMULATOR_RAW
    profile = Profile()

    def reset(self, seed: int) -> Decision:
        return Decision(observation(), (), True)


def test_terminal_pair_produces_matching_trace() -> None:
    trace = run_paired(Original(), Simulator(), seed=7)
    assert trace.complete
    assert trace.matches
    coverage = summarize((trace,))
    assert coverage.matching_runs == 1
    assert coverage.screens == ("GAME_OVER",)
