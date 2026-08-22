from __future__ import annotations

import pytest


torch = pytest.importorskip("torch")

from sls.contracts import (
    Action,
    ActionKind,
    Card,
    Decision,
    MapNode,
    Observation,
    Player,
    RunContext,
    ScreenType,
)
from sls.model import ModelConfig, Policy, PolicyBatch
from sls.model.batching import encode_decision
from sls.model.encoding import NUMERIC_FIELD_IDS, policy_vocabulary


def test_policy_scores_the_current_candidate_set() -> None:
    decision = Decision(
        Observation(
            Player("IRONCLAD", 80, 80, 0, 3, 3),
            RunContext(0, 1, 0, 99, False, False, False),
            ScreenType.NEOW,
        ),
        (
            Action(ActionKind.CHOOSE_NEOW_OPTION, option_id="neow:0"),
            Action(ActionKind.CHOOSE_NEOW_OPTION, option_id="neow:1"),
        ),
    )
    config = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4)
    batch = PolicyBatch.from_decisions((decision,), config)
    output = Policy(config)(*batch.model_inputs())
    assert output.logits.shape == (1, 2)
    assert output.value.shape == (1,)
    assert batch.action_reference_mask[0, :, 2].tolist() == [True, True]
    assert batch.action_references[0, 0, 2] != batch.action_references[0, 1, 2]


def _combat_decision(*, reverse_actions: bool = False, reverse_entities: bool = False) -> Decision:
    cards = (
        Card("HAND:0", "STRIKE_RED", "HAND", 0, 1, 1, True),
        Card("HAND:1", "STRIKE_RED", "HAND", 0, 1, 0, True),
    )
    actions = (
        Action(ActionKind.PLAY_CARD, subject_id="HAND:0", target_id="MONSTER:0"),
        Action(ActionKind.PLAY_CARD, subject_id="HAND:1", target_id="MONSTER:0"),
        Action(ActionKind.END_TURN),
    )
    if reverse_actions:
        actions = tuple(reversed(actions))
    if reverse_entities:
        cards = tuple(reversed(cards))
    from sls.contracts import Enemy
    return Decision(
        Observation(
            Player("IRONCLAD", 70, 80, 0, 3, 3),
            RunContext(0, 1, 1, 99, False, False, False, "SLIME_BOSS"),
            ScreenType.COMBAT,
            hand=cards,
            enemies=(Enemy("MONSTER:0", "CULTIST", 48, 48, 0, "BUFF", 0, 0),),
        ),
        actions,
    )


def test_candidate_reordering_only_reorders_semantic_logits() -> None:
    torch.manual_seed(7)
    config = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4)
    policy = Policy(config).eval()
    first = _combat_decision()
    second = _combat_decision(reverse_actions=True)
    first_logits = policy(*PolicyBatch.from_decisions((first,)).model_inputs()).logits[0]
    second_logits = policy(*PolicyBatch.from_decisions((second,)).model_inputs()).logits[0]
    first_by_id = {action.candidate_id: first_logits[index] for index, action in enumerate(first.actions)}
    for index, action in enumerate(second.actions):
        assert torch.allclose(
            second_logits[index], first_by_id[action.candidate_id], rtol=0.0, atol=1e-6,
        )


def test_entity_reordering_preserves_semantic_logits() -> None:
    torch.manual_seed(11)
    config = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4)
    policy = Policy(config).eval()
    first = _combat_decision()
    second = _combat_decision(reverse_entities=True)
    first_logits = policy(*PolicyBatch.from_decisions((first,)).model_inputs()).logits
    second_logits = policy(*PolicyBatch.from_decisions((second,)).model_inputs()).logits
    assert torch.allclose(first_logits, second_logits, rtol=0.0, atol=1e-6)


def test_duplicate_card_instances_resolve_to_distinct_entity_tokens() -> None:
    encoded = encode_decision(_combat_decision())
    subject_column = 0
    assert encoded.action_reference_mask[:2, subject_column].tolist() == [True, True]
    assert encoded.action_references[0, subject_column] != encoded.action_references[1, subject_column]


def test_numeric_zero_is_distinct_from_missing() -> None:
    encoded = encode_decision(_combat_decision())
    visible_order = NUMERIC_FIELD_IDS["visible_order"]
    playable = NUMERIC_FIELD_IDS["playable"]
    assert not encoded.entity_numeric_present[2, visible_order]
    assert encoded.entity_numeric_present[2, playable]


def test_map_outgoing_edges_preserve_destinations() -> None:
    observation = Observation(
        Player("IRONCLAD", 80, 80, 0, 0, 3),
        RunContext(0, 1, 1, 99, False, False, False),
        ScreenType.MAP,
        map_nodes=(
            MapNode("map:0:0", 0, 0, "M", True, ("map:1:1",)),
            MapNode("map:1:1", 1, 1, "?", False),
        ),
    )
    encoded = encode_decision(Decision(
        observation, (Action(ActionKind.CHOOSE_MAP_NODE, node_id="map:0:0"),),
    ))
    # PLAYER and RUN precede map nodes.
    assert encoded.entity_adjacency[2, 3]
    assert encoded.entity_adjacency.sum() == 1


def test_unknown_content_fails_instead_of_hashing() -> None:
    decision = _combat_decision()
    bad = Decision(
        Observation(
            decision.observation.player, decision.observation.run, ScreenType.COMBAT,
            hand=(Card("HAND:0", "NOT_A_BASE_GAME_CARD", "HAND", 0, 1, 1, True),),
            enemies=decision.observation.enemies,
        ),
        (Action(ActionKind.PLAY_CARD, subject_id="HAND:0", target_id="MONSTER:0"),),
    )
    with pytest.raises(ValueError, match="unknown policy content ID"):
        encode_decision(bad)


def test_unknown_metadata_and_dangling_references_fail() -> None:
    decision = _combat_decision()
    with pytest.raises(ValueError, match="unknown policy field"):
        encode_decision(Decision(
            decision.observation,
            (Action(ActionKind.END_TURN, metadata=(("mystery", 1),)),),
        ))
    with pytest.raises(ValueError, match="unresolved action target_id"):
        encode_decision(Decision(
            decision.observation,
            (Action(
                ActionKind.PLAY_CARD, subject_id="HAND:0", target_id="MONSTER:404",
            ),),
        ))


def test_registry_ids_have_unique_exact_tokens() -> None:
    content = policy_vocabulary()["content"]
    assert len(content) == len(set(content))
