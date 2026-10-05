from __future__ import annotations

import importlib
from dataclasses import replace

import pytest

from sls.contracts import (
    Action,
    ActionKind,
    Card,
    Decision,
    Enemy,
    Observation,
    Player,
    RunContext,
    ScreenType,
    Transition,
)
from sls.curriculum import IRONCLAD_A0_ACT1, IRONCLAD_A0_FULLRUN
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
    assert result.mean_reward == 1.0


def test_evaluation_failure_reward_includes_terminal_floor(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = importlib.import_module("sls.rl.evaluate")

    class LossBackend(_VictoryBackend):
        def step(self, _action: Action) -> Transition:
            return Transition(
                Decision(_observation(1, ScreenType.GAME_OVER), (), True),
                reward=-1.0,
                terminated=True,
                info={
                    "reason": "DEATH",
                    "success": False,
                    "terminal_outcome": "PLAYER_LOSS",
                },
            )

    monkeypatch.setattr(module, "SimulatorBackend", LossBackend)
    result = module.evaluate(
        _model(), IRONCLAD_A0_FULLRUN, (10**12,), max_steps=1,
    )

    assert result.mean_reward == pytest.approx(-1.0 + 0.8 * 6 / 50)
    assert result.median_failure_floor == 6


def test_evaluation_limit_failure_does_not_receive_floor_credit(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = importlib.import_module("sls.rl.evaluate")

    class NeverEndsBackend(_VictoryBackend):
        def step(self, _action: Action) -> Transition:
            return Transition(
                Decision(
                    _observation(1, ScreenType.COMBAT),
                    (Action(ActionKind.END_TURN),),
                ),
                reward=0.0,
                terminated=False,
            )

    monkeypatch.setattr(module, "SimulatorBackend", NeverEndsBackend)
    result = module.evaluate(
        _model(), IRONCLAD_A0_FULLRUN, (10**12,), max_steps=1,
    )

    assert result.mean_reward == -1.0
    assert result.step_limits == 1


def test_evaluation_rejects_success_before_act_three(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = importlib.import_module("sls.rl.evaluate")

    class PrematureVictory(_VictoryBackend):
        terminal_act = 2

    monkeypatch.setattr(module, "SimulatorBackend", PrematureVictory)
    with pytest.raises(RuntimeError, match="without a real Act 3 victory"):
        module.evaluate(_model(), IRONCLAD_A0_FULLRUN, (10**12,), max_steps=1)


def test_evaluation_records_boss_tactical_metrics(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    module = importlib.import_module("sls.rl.evaluate")
    defend = Card("HAND:0", "DEFEND_RED", "HAND", 0, 1, 1, True)
    guardian = Enemy(
        "MONSTER:0", "THE_GUARDIAN", 200, 250, 0, "ATTACK", 8, 2,
    )
    boss_observation = Observation(
        player=Player("IRONCLAD", 40, 80, 0, 3, 3),
        run=RunContext(
            0, 1, 16, 99, False, False, False, "THE_GUARDIAN",
        ),
        screen=ScreenType.COMBAT,
        hand=(defend,),
        enemies=(guardian,),
    )

    class BossBackend:
        def __init__(self, _profile: object) -> None:
            pass

        def reset(self, _seed: int) -> Decision:
            return Decision(
                boss_observation,
                (Action(ActionKind.PLAY_CARD, subject_id=defend.instance_id),),
            )

        def step(self, _action: Action) -> Transition:
            terminal = Observation(
                player=Player("IRONCLAD", 0, 80, 0, 0, 3),
                run=boss_observation.run,
                screen=ScreenType.GAME_OVER,
            )
            return Transition(
                Decision(terminal, (), True), -1.0, True,
                info={"reason": "DEATH", "success": False},
            )

    monkeypatch.setattr(module, "SimulatorBackend", BossBackend)
    result = module.evaluate(_model(), IRONCLAD_A0_ACT1, (7,), max_steps=1)
    metrics = result.boss_action_metrics["ACT_1:THE_GUARDIAN"]

    assert metrics["entries"] == 1
    assert metrics["decisions"] == 1
    assert metrics["play_card_actions"] == 1
    assert metrics["defend_red_actions"] == 1
    assert metrics["block_deficit_decisions"] == 1
    assert metrics["defend_red_on_block_deficit_rate"] == 1.0


@pytest.mark.parametrize(("encounter", "monster"), (
    ("AUTOMATON", "BRONZE_AUTOMATON"),
    ("CHAMP", "THE_CHAMP"),
    ("COLLECTOR", "THE_COLLECTOR"),
    ("DONU_AND_DECA", "DONU"),
    ("DONU_AND_DECA", "DECA"),
))
def test_boss_metrics_resolve_encounter_to_monster(monkeypatch, encounter, monster):
    module = importlib.import_module("sls.rl.evaluate")

    class BossBackend(_VictoryBackend):
        def reset(self, _seed):
            observation = _observation(2, ScreenType.COMBAT)
            return Decision(replace(
                observation,
                run=replace(observation.run, visible_boss_id=encounter),
                enemies=(Enemy("MONSTER:0", monster, 100, 100, 0, "ATTACK", 8, 1),),
            ), (Action(ActionKind.END_TURN),))

    monkeypatch.setattr(module, "SimulatorBackend", BossBackend)
    result = module.evaluate(_model(), IRONCLAD_A0_FULLRUN, (7,), max_steps=1)
    assert result.boss_action_metrics[f"ACT_2:{encounter}"]["entries"] == 1


@pytest.mark.parametrize("boss_started", (False, True))
def test_slime_split_metrics_require_a_boss_entry(monkeypatch, boss_started):
    module = importlib.import_module("sls.rl.evaluate")

    class SlimeBackend(_VictoryBackend):
        def decision(self, monster):
            observation = _observation(1, ScreenType.COMBAT)
            return Decision(replace(
                observation,
                run=replace(observation.run, visible_boss_id="SLIME_BOSS"),
                enemies=(Enemy("MONSTER:0", monster, 20, 40, 0, "ATTACK", 8, 1),),
            ), (Action(ActionKind.END_TURN),))

        def reset(self, _seed):
            self.steps = 0
            return self.decision("SLIME_BOSS" if boss_started else "ACID_SLIME_L")

        def step(self, action):
            self.steps += 1
            if self.steps == 1:
                return Transition(self.decision("ACID_SLIME_L"), 0.0, False)
            return super().step(action)

    monkeypatch.setattr(module, "SimulatorBackend", SlimeBackend)
    result = module.evaluate(_model(), IRONCLAD_A0_FULLRUN, (7,), max_steps=2)
    if boss_started:
        metrics = result.boss_action_metrics["ACT_1:SLIME_BOSS"]
        assert metrics["entries"] == 1
        assert metrics["decisions"] == 2
    else:
        assert "ACT_1:SLIME_BOSS" not in result.boss_action_metrics
