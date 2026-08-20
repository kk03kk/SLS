from __future__ import annotations

import pytest


torch = pytest.importorskip("torch")

from sls.contracts import (
    Action,
    ActionKind,
    Decision,
    Observation,
    Player,
    RunContext,
    ScreenType,
)
from sls.model import ModelConfig, Policy, PolicyBatch


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
