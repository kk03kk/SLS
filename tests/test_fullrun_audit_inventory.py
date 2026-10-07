import hashlib
import json
import zipfile
from pathlib import Path

import pytest

from tools.prepare_fullrun_audit import allocate_seeds, build_inventory, recorded_seeds
from tools.run_act2_encounter_batch import verify_scene_sources


def test_complete_registry_is_conservative_and_act4_is_explicit():
    ledger = build_inventory()
    rows = {row["id"]: row for row in ledger["obligations"]}
    assert len(rows) == len(ledger["obligations"])
    assert rows["encounters:THE_HEART"]["acts"] == [4]
    assert rows["encounters:SHIELD_AND_SPEAR"]["acts"] == [4]
    assert rows["cards:ANGER"]["candidate_basis"] == "DECLARED_SCOPE_OR_ACT4"
    assert rows["cards:ACCURACY"]["reachability"] == "UNVERIFIED"
    assert rows["relics:PRISMATIC_SHARD"]["policy_excluded"]
    assert all(row["status"] == "UNREVIEWED" and not row["evidence"] for row in rows.values())
    assert not ledger["final_holdout_used"]
    assert not ledger["inherited_passes"]


def test_allocation_refuses_collision_and_preserves_order():
    assert allocate_seeds(2, set()) == [[131200000, 131200001, 131200002],
                                       [131200003, 131200004, 131200005]]
    with pytest.raises(ValueError, match="collision"):
        allocate_seeds(2, {131200004})
    with pytest.raises(ValueError):
        allocate_seeds(0, set())


def test_seed_records_include_nested_scene_and_run(tmp_path: Path):
    path = tmp_path / "manifest.json"
    path.write_text('{"scenes":[{"seeds":[131200000,131200001]}],"runs":[{"seed":131200002}]}')
    assert recorded_seeds([path]) == {131200000, 131200001, 131200002}


def test_shared_manifest_preserves_provenance_and_does_not_claim_victory_flow():
    data = json.loads(Path("native/oracle/resources/spirecomm/parity/fullrun-scenes.json").read_text())
    rows = data["scenes"]
    assert len(rows) == 12
    assert [seed for row in rows for seed in row["seeds"]] == list(range(131200000, 131200036))
    assert not data["final_holdout_used"]
    assert data["training_gate"] == "NOT_QUALIFIED"
    assert all(row["qualification_scope"] == "ISOLATED_MECHANISM_NOT_DUNGEON_FLOW" for row in rows)
    for row in rows:
        assert set(row["source_evidence"]) == {"com.megacrit.cardcrawl." + name for name in row["stock_classes"]}
        assert all(len(digest) == 64 for evidence in row["source_evidence"].values()
                   for digest in evidence.values())


def test_first_divergence_probe_does_not_reuse_shared_seeds():
    data = json.loads(Path("native/oracle/resources/spirecomm/parity/fullrun-regressions.json").read_text())
    assert data["scenes"][0]["seeds"] == [131200036, 131200037, 131200038]
    assert data["scenes"][0]["qualification_scope"] == "ISOLATED_MECHANISM_NOT_PRODUCTION_RNG_HISTORY"


def test_source_guard_reads_actual_stock_class_bytes(tmp_path):
    jar = tmp_path / "stock.jar"
    name = "com.megacrit.cardcrawl.cards.red.Anger"
    with zipfile.ZipFile(jar, "w") as archive:
        archive.writestr(name.replace(".", "/") + ".class", b"stock-class")
    scene = {"id": "generated", "stock_classes": ["cards.red.Anger"],
             "source_evidence": {name: {"class_sha256": hashlib.sha256(b"stock-class").hexdigest()}}}
    verify_scene_sources({"scenes": [scene]}, ["generated"], jar)
    scene["source_evidence"][name]["class_sha256"] = "0" * 64
    with pytest.raises(ValueError, match="stale"):
        verify_scene_sources({"scenes": [scene]}, ["generated"], jar)
