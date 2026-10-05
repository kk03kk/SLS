"""Tests for the policy-intervention tool's selector and paired statistics.

The tool exists so that a single decision point can be turned into a causal
estimate. A selector that silently matches nothing would turn an intervened arm
into an accidental control run, so a mistyped spec must be observable rather than
silent.
"""

from __future__ import annotations

import sys
from dataclasses import dataclass
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from intervene_policy_decision import (  # noqa: E402
    CONTROL_LABEL,
    Intervention,
    exact_mcnemar,
)


@dataclass(frozen=True)
class _Kind:
    value: str


@dataclass(frozen=True)
class _Action:
    kind_name: str
    option_id: str | None = None
    subject_id: str | None = None
    target_id: str | None = None

    @property
    def kind(self) -> _Kind:
        return _Kind(self.kind_name)


@dataclass(frozen=True)
class _Screen:
    value: str


@dataclass(frozen=True)
class _Observation:
    screen: _Screen


@dataclass(frozen=True)
class _Decision:
    observation: _Observation
    actions: tuple[_Action, ...]


def _decision(screen: str, *actions: _Action) -> _Decision:
    return _Decision(_Observation(_Screen(screen)), tuple(actions))


def test_selector_parsing_rejects_malformed_specs() -> None:
    for spec in ("", "NEOW", "NEOW:option", "NEOW:nonsense:x"):
        with pytest.raises(ValueError):
            Intervention.parse(spec)
    assert Intervention.parse("neow:option:event-option:1").screen == "NEOW"


def test_option_selector_matches_only_the_named_offer() -> None:
    intervention = Intervention.parse("NEOW:option:event-option:2")
    decision = _decision(
        "NEOW",
        _Action("CHOOSE_NEOW_OPTION", option_id="event-option:1"),
        _Action("CHOOSE_NEOW_OPTION", option_id="event-option:2"),
    )
    assert intervention.index_for(decision) == 1


def test_selector_is_scoped_to_its_screen() -> None:
    intervention = Intervention.parse("NEOW:option:event-option:0")
    decision = _decision(
        "CARD_REWARD", _Action("CHOOSE_CARD_REWARD", option_id="event-option:0"),
    )
    assert intervention.index_for(decision) is None


def test_kind_and_prefix_selectors() -> None:
    decision = _decision(
        "CARD_REWARD",
        _Action("CHOOSE_CARD_REWARD", subject_id="reward-card:0:1"),
        _Action("SKIP_CARD_REWARD", option_id="reward-card:0"),
    )
    assert Intervention.parse("CARD_REWARD:kind:SKIP_CARD_REWARD").index_for(decision) == 1
    assert Intervention.parse(
        "CARD_REWARD:subject-prefix:reward-card:0"
    ).index_for(decision) == 0
    assert Intervention.parse("CARD_REWARD:target-none").index_for(decision) == 0


def test_missing_selector_reports_no_match_instead_of_guessing() -> None:
    decision = _decision("NEOW", _Action("CHOOSE_NEOW_OPTION", option_id="event-option:1"))
    assert Intervention.parse("NEOW:option:event-option:9").index_for(decision) is None


def test_control_label_is_not_parsed_as_an_intervention() -> None:
    with pytest.raises(ValueError):
        Intervention.parse(CONTROL_LABEL)


def test_exact_mcnemar_matches_hand_computed_values() -> None:
    assert exact_mcnemar(0, 0) == 1.0
    assert exact_mcnemar(1, 1) == 1.0
    # 1 vs 9 discordant pairs: two-sided exact binomial p = 2 * (C(10,0)+C(10,1))/2^10
    assert exact_mcnemar(1, 9) == pytest.approx(2 * (1 + 10) / 1024)
    assert exact_mcnemar(9, 1) == pytest.approx(exact_mcnemar(1, 9))
    assert exact_mcnemar(20, 60) < 0.001
