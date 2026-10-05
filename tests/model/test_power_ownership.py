"""Public ownership must survive adapters and affect semantic action scores."""

from dataclasses import replace

import pytest
import torch

from sls.backends.original.adapter import _powers as original_powers
from sls.backends.simulator.environment import _powers as simulator_powers
from sls.contracts import (
    Action,
    ActionKind,
    Card,
    Decision,
    Enemy,
    Observation,
    Player,
    PublicEntity,
    RunContext,
    ScreenType,
)
from sls.model import ModelConfig, Policy, PolicyBatch, encode_decision


def decision(owner):
    return Decision(Observation(
        Player("IRONCLAD", 70, 80, 0, 3, 3),
        RunContext(0, 1, 5, 99, False, False, False), ScreenType.COMBAT,
        hand=(Card("HAND:0", "STRIKE_RED", "HAND", 0, 1, 1, True),),
        enemies=tuple(Enemy(f"MONSTER:{i}", "CULTIST", 40, 40, 0, "BUFF", 0, 0)
                      for i in range(2)),
        powers=(PublicEntity("power", "VULNERABLE", (("amount", 2),), owner_id=owner),),
    ), tuple(Action(ActionKind.PLAY_CARD, subject_id="HAND:0", target_id=f"MONSTER:{i}")
             for i in range(2)))


@pytest.mark.parametrize("owner", (None, "missing", "MONSTER:9"))
def test_missing_or_dangling_power_owner_is_rejected(owner):
    with pytest.raises(ValueError, match="public owner"):
        decision(owner)


@pytest.mark.parametrize("prefix,owner", (
    ("PLAYER_POWER", "player"), ("MONSTER:0:POWER", "MONSTER:0"),
))
def test_both_adapters_supply_same_owner(prefix, owner):
    payload = [{"id": "Strength", "amount": 3}]
    original = original_powers(payload, prefix)
    simulator = simulator_powers(payload, prefix)
    assert original == simulator
    assert original[0].owner_id == owner


def test_player_and_enemy_power_are_distinct_inputs():
    player = encode_decision(decision("player"))
    enemy = encode_decision(decision("MONSTER:0"))
    assert not torch.equal(player.entity_adjacency, enemy.entity_adjacency)


def test_power_owner_changes_target_scores_and_respects_entity_permutations():
    torch.manual_seed(51)
    model = Policy(ModelConfig(embedding_dim=32, transformer_layers=1,
                              attention_heads=4, feedforward_dim=64)).eval()
    first = decision("MONSTER:0")
    second = decision("MONSTER:1")
    reordered = replace(first, observation=replace(
        first.observation, enemies=tuple(reversed(first.observation.enemies)),
    ))
    with torch.no_grad():
        a, b, c = [model(*PolicyBatch.from_decisions((d,)).model_inputs()).logits
                   for d in (first, second, reordered)]
    assert not torch.allclose(a[:, 0], a[:, 1], atol=1e-6, rtol=0)
    assert torch.allclose(a, b.flip(1), atol=1e-6, rtol=0)
    assert torch.allclose(a, c, atol=1e-6, rtol=0)
