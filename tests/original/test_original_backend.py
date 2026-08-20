from __future__ import annotations

from sls.backends.original import OriginalBackend, OriginalSession
from sls.contracts import ActionKind, ScreenType
from sls.curriculum import IRONCLAD_A0_ACT1


class ScriptedTransport:
    def __init__(self, payloads: list[dict]) -> None:
        self.payloads = iter(payloads)
        self.sent: list[str] = []

    def send(self, command: str) -> None:
        self.sent.append(command)

    def receive(self) -> dict:
        return next(self.payloads)


def game_payload(choices: list[str]) -> dict:
    return {
        "in_game": True,
        "ready_for_command": True,
        "available_commands": ["choose"],
        "game_state": {
            "class": "IRONCLAD",
            "ascension_level": 0,
            "act": 1,
            "floor": 0,
            "gold": 99,
            "current_hp": 80,
            "max_hp": 80,
            "deck": [],
            "relics": [],
            "potions": [],
            "map": [],
            "screen_type": "EVENT",
            "screen_state": {},
            "choice_list": choices,
            "_parity_run": {},
        },
    }


def test_reset_folds_the_original_only_neow_dialog() -> None:
    menu = {
        "in_game": False,
        "ready_for_command": True,
        "available_commands": ["start"],
    }
    transport = ScriptedTransport([menu, game_payload(["Continue"]), game_payload(["A", "B", "C", "D"])])
    backend = OriginalBackend(OriginalSession(transport), IRONCLAD_A0_ACT1)
    decision = backend.reset(0)
    assert transport.sent[0] == "ready"
    assert transport.sent[1].startswith("start IRONCLAD 0 ")
    assert transport.sent[2] == "choose 0"
    assert decision.observation.screen is ScreenType.NEOW
    assert len(decision.actions) == 4
    assert all(action.kind is ActionKind.CHOOSE_NEOW_OPTION for action in decision.actions)
