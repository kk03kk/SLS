from __future__ import annotations

import pytest

from sls.backends.original import LiveGameBackend, OriginalSession
from sls.curriculum import EpisodeHorizon


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
        "in_game": True, "ready_for_command": True, "available_commands": ["choose"],
        "game_state": {
            "class": "IRONCLAD", "ascension_level": 0, "act": 1, "floor": 0,
            "gold": 99, "current_hp": 80, "max_hp": 80, "deck": [],
            "relics": [], "potions": [], "map": [], "screen_type": "EVENT",
            "screen_state": {}, "choice_list": choices, "_parity_run": {},
        },
    }


def test_live_backend_attaches_without_resetting_or_starting() -> None:
    payload = game_payload(["A", "B", "C", "D"])
    payload["game_state"]["ascension_level"] = 17
    transport = ScriptedTransport([payload])
    backend = LiveGameBackend(OriginalSession(transport))
    decision = backend.attach()
    assert transport.sent == ["ready"]
    assert decision.observation.run.ascension == 17
    assert backend.profile.horizon is EpisodeHorizon.HEART
    assert backend.profile.profile_id == "IRONCLAD_A17_HEART"


def test_live_backend_rejects_an_already_owned_prismatic_shard() -> None:
    payload = game_payload(["A", "B", "C", "D"])
    payload["game_state"]["relics"] = [{"id": "PrismaticShard", "counter": -1}]
    backend = LiveGameBackend(OriginalSession(ScriptedTransport([payload])))
    with pytest.raises(ValueError, match="PRISMATIC_SHARD"):
        backend.attach()


def test_live_backend_requires_an_active_run() -> None:
    payload = {"in_game": False, "ready_for_command": True, "available_commands": ["start"]}
    backend = LiveGameBackend(OriginalSession(ScriptedTransport([payload])))
    with pytest.raises(RuntimeError, match="start or continue"):
        backend.attach()
