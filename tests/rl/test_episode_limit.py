from __future__ import annotations

from sls.contracts import Action, ActionKind, Decision, MapNode, Observation, Player, RunContext, ScreenType
from sls.rl.episode_limit import EpisodeLimitState, policy_boundary_fingerprint


def _decision(*, reverse: bool = False) -> Decision:
    nodes = (
        MapNode("map:0:1", 0, 1, "M", True),
        MapNode("map:1:1", 1, 1, "?", True),
    )
    actions = (
        Action(ActionKind.CHOOSE_MAP_NODE, node_id="map:0:1"),
        Action(ActionKind.CHOOSE_MAP_NODE, node_id="map:1:1"),
    )
    return Decision(
        Observation(
            Player("IRONCLAD", 80, 80, 0, 0, 3),
            RunContext(0, 1, 0, 99, False, False, False, "SLIME_BOSS"),
            ScreenType.MAP,
            map_nodes=tuple(reversed(nodes)) if reverse else nodes,
        ),
        tuple(reversed(actions)) if reverse else actions,
    )


def test_policy_boundary_fingerprint_ignores_candidate_and_entity_order() -> None:
    assert policy_boundary_fingerprint(_decision()) == policy_boundary_fingerprint(_decision(reverse=True))


def test_episode_limit_allows_four_visits_and_fails_on_fourth_repeat() -> None:
    decision = _decision()
    state = EpisodeLimitState.initial(decision)
    for _ in range(3):
        assert state.observe(decision, max_steps=512, max_boundary_visits=4) is None
    assert state.observe(decision, max_steps=512, max_boundary_visits=4) == "cycle_limit"


def test_episode_step_limit_is_terminal_at_exact_limit() -> None:
    state = EpisodeLimitState.initial(_decision())
    state.steps = 511
    assert state.observe(_decision(), max_steps=512, max_boundary_visits=999) == "step_limit"


def test_episode_limit_state_round_trip() -> None:
    state = EpisodeLimitState.initial(_decision())
    state.steps = 17
    assert EpisodeLimitState.from_dict(state.to_dict()).to_dict() == state.to_dict()
