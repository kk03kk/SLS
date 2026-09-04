from __future__ import annotations

import json
from pathlib import Path

from sls.content.scope import (
    IRONCLAD_A0_SCOPE_ID,
    IRONCLAD_A0_SCOPE_PATH,
    UnsupportedContentPolicy,
    filter_policy_key_acquisitions,
    filter_policy_offers,
    filter_policy_shop,
    load_ironclad_a0_scope,
    validate_scope_source_hashes,
)
from sls.contracts import Action, ActionKind, PublicEntity, ShopItem
from sls.model.encoding import policy_vocabulary

ROOT = Path(__file__).resolve().parents[2]


def test_committed_ironclad_scope_is_current_and_exactly_scoped() -> None:
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
    all_encounters = {
        item for values in scope["encounters"].values() for item in values
    }
    assert len(all_encounters) == 61
    assert {
        "AUTOMATON", "COLLECTOR", "CHAMP",
        "AWAKENED_ONE", "TIME_EATER", "DONU_AND_DECA",
    } <= all_encounters
    assert {"SHIELD_AND_SPEAR", "THE_HEART"}.isdisjoint(all_encounters)
    all_monsters = {item for values in scope["monsters"].values() for item in values}
    assert {
        "BRONZE_AUTOMATON", "THE_COLLECTOR", "THE_CHAMP",
        "AWAKENED_ONE", "TIME_EATER", "DONU", "DECA",
    } <= all_monsters
    assert {"CORRUPT_HEART", "SPIRE_SHIELD", "SPIRE_SPEAR"}.isdisjoint(
        all_monsters
    )
    vocabulary = set(policy_vocabulary()["content"])
    for category in ("cards", "potions", "relics", "events", "encounters", "monsters"):
        scoped_ids = {
            item for values in scope[category].values() for item in values
        }
        assert scoped_ids <= vocabulary, f"{category} has an UNKNOWN vocabulary fallback"


def test_scope_file_digest_is_deterministic() -> None:
    first = IRONCLAD_A0_SCOPE_PATH.read_bytes()
    parsed = json.loads(first)
    assert parsed == load_ironclad_a0_scope()
    assert IRONCLAD_A0_SCOPE_PATH.read_bytes() == first
    validate_scope_source_hashes(ROOT)


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


def test_non_heart_projection_hides_keys_without_changing_other_identities() -> None:
    emerald = PublicEntity("reward-key:emerald", "EMERALD_KEY")
    relic = PublicEntity("reward-relic:0", "AKABEKO")
    take_emerald = Action(ActionKind.TAKE_REWARD, reward_id=emerald.instance_id)
    take_blue = Action(ActionKind.TAKE_BLUE_KEY, reward_id="reward-key:sapphire")
    recall = Action(ActionKind.RECALL, option_id="rest-option:2")
    take_relic = Action(ActionKind.TAKE_REWARD, reward_id=relic.instance_id)
    mapping = {
        take_emerald.candidate_id: 1,
        take_blue.candidate_id: 2,
        recall.candidate_id: 3,
        take_relic.candidate_id: 4,
    }

    items, actions, visible = filter_policy_key_acquisitions(
        (emerald, relic),
        (take_emerald, take_blue, recall, take_relic),
        mapping,
        allow_keys=False,
    )
    assert items == (relic,)
    assert actions == (take_relic,)
    assert visible == {take_relic.candidate_id: 4}
    assert filter_policy_key_acquisitions(
        (emerald,), (take_emerald,), mapping, allow_keys=True,
    )[1] == (take_emerald,)
