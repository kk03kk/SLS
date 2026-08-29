from __future__ import annotations

import json
from pathlib import Path
import subprocess

from sls.content.scope import (
    IRONCLAD_A0_SCOPE_ID,
    IRONCLAD_A0_SCOPE_PATH,
    UnsupportedContentPolicy,
    filter_policy_offers,
    filter_policy_shop,
    load_ironclad_a0_scope,
)
from sls.contracts import Action, ActionKind, PublicEntity, ShopItem


ROOT = Path(__file__).resolve().parents[2]
PYTHON = Path(__import__("sys").executable)


def test_committed_ironclad_scope_is_current_and_exactly_scoped() -> None:
    subprocess.run(
        (str(PYTHON), str(ROOT / "tools" / "generate_content_scope.py"), "--check"),
        cwd=ROOT, check=True,
    )
    scope = load_ironclad_a0_scope()
    assert scope["scope_id"] == IRONCLAD_A0_SCOPE_ID
    assert len(scope["cards"]["red"]) == 75
    assert len(scope["cards"]["colorless_reward"]) == 35
    assert set(scope["cards"]["statuses"]) == {"BURN", "DAZED", "SLIMED", "WOUND"}
    assert "VOID" not in scope["cards"]["ids"]
    assert "ASCENDERS_BANE" not in scope["cards"]["ids"]
    assert "SURVIVOR" not in scope["cards"]["ids"]
    assert "BULLET_TIME" not in scope["cards"]["ids"]
    assert "CONCENTRATE" not in scope["cards"]["ids"]
    assert "BRUTALITY" in scope["cards"]["ids"]
    assert "COMBUST" in scope["cards"]["ids"]
    assert len(scope["potions"]["ids"]) == 33
    assert "STANCE_POTION" not in scope["potions"]["ids"]
    assert "PRISMATIC_SHARD" in scope["relics"]["ids"]
    assert scope["policy_excluded_content_ids"] == ["PRISMATIC_SHARD"]
    assert len(scope["encounters"]["act1"]) == 20
    assert {"SLIME_BOSS", "THE_GUARDIAN", "HEXAGHOST"} <= set(
        scope["encounters"]["act1"]
    )
    assert len(scope["events"]["act1_base"]) == 11
    assert len(scope["events"]["act1_shrines"]) == 6
    assert len(scope["events"]["a0_one_time_candidates"]) == 14
    assert len(scope["monsters"]["act1"]) == 25
    assert {"CULTIST", "GREMLIN_NOB", "HEXAGHOST", "THE_GUARDIAN"} <= set(
        scope["monsters"]["act1"]
    )


def test_scope_file_digest_is_deterministic() -> None:
    first = IRONCLAD_A0_SCOPE_PATH.read_bytes()
    parsed = json.loads(first)
    assert parsed == load_ironclad_a0_scope()
    subprocess.run(
        (str(PYTHON), str(ROOT / "tools" / "generate_content_scope.py"), "--check"),
        cwd=ROOT, check=True,
    )
    assert IRONCLAD_A0_SCOPE_PATH.read_bytes() == first


def test_shop_filter_hides_prismatic_without_renumbering_or_rewriting_mapping() -> None:
    shard = ShopItem("shop-relic:0", "PRISMATIC_SHARD", "RELIC", 150, False)
    akabeko = ShopItem("shop-relic:1", "AKABEKO", "RELIC", 151, False)
    buy_shard = Action(ActionKind.BUY_RELIC, subject_id=shard.instance_id)
    buy_akabeko = Action(ActionKind.BUY_RELIC, subject_id=akabeko.instance_id)
    leave = Action(ActionKind.LEAVE_SHOP)
    mapping = {
        buy_shard.candidate_id: 111,
        buy_akabeko.candidate_id: 222,
        leave.candidate_id: 333,
    }

    items, actions, filtered = filter_policy_shop(
        (shard, akabeko), (buy_shard, buy_akabeko, leave), mapping,
    )

    assert items == (akabeko,)
    assert actions == (buy_akabeko, leave)
    assert filtered == {buy_akabeko.candidate_id: 222, leave.candidate_id: 333}
    assert items[0].instance_id == "shop-relic:1"


def test_reward_filter_is_profile_scoped_and_preserves_other_candidate_identity() -> None:
    shard = PublicEntity("reward-relic:0", "PRISMATIC_SHARD")
    akabeko = PublicEntity("reward-relic:1", "AKABEKO")
    take_shard = Action(ActionKind.TAKE_REWARD, reward_id=shard.instance_id)
    take_akabeko = Action(ActionKind.TAKE_REWARD, reward_id=akabeko.instance_id)
    skip = Action(ActionKind.SKIP_REWARD)
    mapping = {
        take_shard.candidate_id: 11,
        take_akabeko.candidate_id: 22,
        skip.candidate_id: 33,
    }

    items, actions, filtered = filter_policy_offers(
        (shard, akabeko), (take_shard, take_akabeko, skip), mapping,
    )
    assert items == (akabeko,)
    assert actions == (take_akabeko, skip)
    assert filtered == {take_akabeko.candidate_id: 22, skip.candidate_id: 33}
    assert items[0].instance_id == "reward-relic:1"

    # A future character scope can explicitly support the relic without
    # changing registries, native pools, or this filtering primitive.
    all_content = UnsupportedContentPolicy(frozenset())
    assert filter_policy_offers(
        (shard,), (take_shard,), {take_shard.candidate_id: 11},
        policy=all_content,
    ) == ((shard,), (take_shard,), {take_shard.candidate_id: 11})
