from __future__ import annotations

import importlib

import pytest

from sls.contracts import (
    Action,
    ActionKind,
    Decision,
    Observation,
    Player,
    RunContext,
    ScreenType,
    Transition,
)
from sls.curriculum import IRONCLAD_A0_FULLRUN
from sls.model import ModelConfig, Policy


def _observation(act: int, screen: ScreenType) -> Observation:
    return Observation(
        player=Player("IRONCLAD", 17, 80, 0, 0, 3),
        run=RunContext(0, act, 6, 99, False, False, False),
        screen=screen,
    )


class _VictoryBackend:
    terminal_act = 3

    def __init__(self, _profile: object) -> None:
        pass

    def reset(self, _seed: int) -> Decision:
        return Decision(
            _observation(1, ScreenType.COMBAT),
            (Action(ActionKind.END_TURN),),
        )

    def step(self, _action: Action) -> Transition:
        return Transition(
            Decision(
                _observation(self.terminal_act, ScreenType.GAME_OVER), (), True,
            ),
            reward=1.0,
            terminated=True,
            info={
                "reason": "GAME_VICTORY",
                "success": True,
                "terminal_outcome": "PLAYER_VICTORY",
            },
        )


def _model() -> Policy:
    return Policy(ModelConfig(
        embedding_dim=32,
        transformer_layers=1,
        attention_heads=4,
        feedforward_dim=64,
    ))


def test_evaluation_success_is_a_real_fullrun_victory(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = importlib.import_module("sls.rl.evaluate")
    monkeypatch.setattr(module, "SimulatorBackend", _VictoryBackend)

    result = module.evaluate(_model(), IRONCLAD_A0_FULLRUN, (10**12,), max_steps=1)

    assert result.successes == 1
    assert result.success_rate == 1.0
    assert result.reached_act3 == 1


def test_evaluation_rejects_success_before_act_three(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = importlib.import_module("sls.rl.evaluate")

    class PrematureVictory(_VictoryBackend):
        terminal_act = 2

    monkeypatch.setattr(module, "SimulatorBackend", PrematureVictory)
    with pytest.raises(RuntimeError, match="without a real Act 3 victory"):
        module.evaluate(_model(), IRONCLAD_A0_FULLRUN, (10**12,), max_steps=1)
