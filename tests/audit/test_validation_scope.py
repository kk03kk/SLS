import copy
import hashlib
import json
import zipfile

import pytest

from sls.audit.bytecode_inventory import branch_review_targets
from sls.audit.validation_scope import (
    map_scope,
    natural_witnesses,
    verified_stock_projections,
    verify_bytecode,
)


def ledger():
    return {"profile": "IRONCLAD_A20_HEART", "ascension": 20, "obligations": [
        {"id": "cards:ANGER", "category": "cards", "content_id": "ANGER",
         "status": "FIXED_REGRESSION", "missing_branches": ["UPGRADE"],
         "evidence": ["missing-private.json"]},
        {"id": "relics:PRISMATIC_SHARD", "category": "relics",
         "content_id": "PRISMATIC_SHARD", "policy_excluded": True,
         "status": "UNREVIEWED"}]}


def test_mapping_preserves_unknowns_gaps_and_missing_private_evidence(tmp_path):
    (tmp_path / "src/sls/content").mkdir(parents=True)
    for name in ("scope.py", "scope.json"):
        (tmp_path / "src/sls/content" / name).write_text("policy")
    original = ledger()
    saved = copy.deepcopy(original)
    result = map_scope(original, {"categories": {}}, tmp_path)
    assert original == saved
    assert result["candidate_count"] == 2
    assert not result["stage1_complete"] and not result["inherited_passes"]
    assert result["summary"]["missing_evidence_files"] == 1
    card, shard = result["obligations"]
    assert card["legacy_missing_branches"] == ["UPGRADE"]
    assert card["reachability"] == "UNCONFIRMED"
    assert card["qualification"] == "NOT_QUALIFIED"
    assert shard["exclusions"][0]["operation"] == "ACQUIRE"
    assert "POOL_AND_RNG" in shard["exclusions"][0]["retained"]


@pytest.mark.parametrize("change", ["profile", "duplicate", "exclusion"])
def test_rejects_profile_duplicate_and_unsupported_exclusions(tmp_path, change):
    data = ledger()
    if change == "profile":
        data["profile"] = "IRONCLAD_A0_FULLRUN"
    elif change == "duplicate":
        data["obligations"].append(data["obligations"][0])
    else:
        data["obligations"][0]["policy_excluded"] = True
    with pytest.raises(ValueError):
        map_scope(data, {"categories": {}}, tmp_path)


def test_relabelled_jar_hash_cannot_hide_changed_class(tmp_path):
    jar = tmp_path / "stock.jar"
    with zipfile.ZipFile(jar, "w") as archive:
        archive.writestr("game/Card.class", b"original")
    inventory = {"schema": "sls-stock-bytecode-inventory-v1",
                 "stock_jar_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
                 "categories": {"cards": [{"stock_classes": [{"class_name": "game.Card",
                     "class_sha256": hashlib.sha256(b"original").hexdigest()}]}]}}
    verify_bytecode(inventory, jar)
    with zipfile.ZipFile(jar, "w") as archive:
        archive.writestr("game/Card.class", b"changed")
    with pytest.raises(ValueError, match="JAR"):
        verify_bytecode(inventory, jar)
    inventory["stock_jar_sha256"] = hashlib.sha256(jar.read_bytes()).hexdigest()
    with pytest.raises(ValueError, match="class"):
        verify_bytecode(inventory, jar)


def test_branch_targets_retain_method_offset_and_all_switch_edges():
    targets = branch_review_targets("""  public void use();
       0: ifeq          20
      10: tableswitch   { // 0 to 1
                     0: 32
                     1: 40
               default: 45
          }
  private boolean canSpawn();
       0: ifnonnull    10
  void packageMethod() throws java.io.IOException;
       0: ifeq         8
  static {};
       0: lookupswitch { // 1
                    -2: 16
               default: 18
          }
""")
    assert targets[0]["edges"] == ["FALLTHROUGH", "20"]
    assert targets[1]["edges"] == ["0:32", "1:40", "default:45"]
    assert targets[2]["method"] == "private boolean canSpawn();"
    assert targets[3]["method"] == "void packageMethod() throws java.io.IOException;"
    assert targets[4]["edges"] == ["-2:16", "default:18"]


def natural_batch(tmp_path):
    journal = tmp_path / "journal.json"
    journal.write_text(json.dumps({"status": "RECOVERED", "recovery_failures": [], "entries": []}))
    path = tmp_path / "trajectory.jsonl"
    observation = {"run": {"ascension": 20}, "deck": [{"card_id": "ANGER"}], "hand": [],
                   "relics": [], "potions": [], "enemies": [],
                   "shop_items": [{"content_id": "PRISMATIC_SHARD"}]}
    records = [{"record_type": "metadata", "schema": "sls-policy-trajectory-v2", "backend": "original",
                "seed": 123, "environment": {"profile_id": "IRONCLAD_A20_ACT2", "stock_jar_sha256": "stock"}},
               {"record_type": "boundary", "screen": "NEOW", "act": 1, "floor": 0,
                "observation": observation, "terminal": False,
                "candidate_actions": [{"type": "PROCEED"}], "chosen_action": {"type": "PROCEED"}},
               {"record_type": "boundary", "screen": "GAME_OVER", "act": 2, "floor": 20,
                "observation": observation, "terminal": True}]
    path.write_text("\n".join(json.dumps(r) for r in records), encoding="utf-8")
    batch = tmp_path / "batch.json"
    batch.write_text(json.dumps({"schema": "sls-act2-production-batch-v1", "execution_complete": True,
                                "runs": [{"output": str(path), "seed": 123, "boundaries": 2,
                                          "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}]}))
    batch.with_suffix(".launch.json").write_text(json.dumps({"mode": "production",
        "completion": {"exit_code": 0}, "recovery_status": "RECOVERED",
        "recovery_journal": str(journal), "oracle_sha256": "oracle"}))
    return batch, path, records, journal


def test_natural_witness_never_promotes_visible_shop_offer(tmp_path):
    batch, _, _, _ = natural_batch(tmp_path)
    witnesses, receipt = natural_witnesses(batch, "stock")
    assert set(witnesses) == {"cards:ANGER"}
    assert receipt["trajectories"][0]["reached_act"] == 2


@pytest.mark.parametrize("fault", ["recovery", "hash", "profile", "illegal", "duplicate", "neow", "terminal"])
def test_invalid_natural_evidence_rejected(tmp_path, fault):
    batch, path, records, journal = natural_batch(tmp_path)
    data = json.loads(batch.read_text())
    if fault == "recovery":
        journal.write_text('{"status":"RECOVERED","recovery_failures":["disk error"]}')
    elif fault == "hash":
        path.write_text("changed")
    elif fault == "duplicate":
        data["runs"].append(data["runs"][0])
    else:
        if fault == "profile":
            records[0]["environment"]["profile_id"] = "IRONCLAD_A0_FULLRUN"
        elif fault == "illegal":
            records[1]["chosen_action"] = {"type": "INJECT"}
        elif fault == "neow":
            records[1]["floor"] = 1
        else:
            records[-1]["terminal"] = False
        path.write_text("\n".join(json.dumps(r) for r in records), encoding="utf-8")
        data["runs"][0]["sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
    batch.write_text(json.dumps(data))
    with pytest.raises(ValueError):
        natural_witnesses(batch, "stock")


def test_shared_flow_projection_requires_both_class_and_text_hashes(tmp_path):
    jar = tmp_path / "stock.jar"
    with zipfile.ZipFile(jar, "w") as archive:
        archive.writestr("com/megacrit/cardcrawl/rooms/Room.class", b"stock class")
    directory = tmp_path / "sources" / "flow"
    directory.mkdir(parents=True)
    projection = directory / "rooms.Room.txt"
    projection.write_text("  public void use();\n       0: ifeq 20\n", encoding="utf-8")
    identity = {"stock_jar_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
                "classes": {"com.megacrit.cardcrawl.rooms.Room": {
                    "class_sha256": hashlib.sha256(b"stock class").hexdigest(),
                    "bytecode_sha256": hashlib.sha256(projection.read_bytes()).hexdigest()}}}
    (directory / "identity.json").write_text(json.dumps(identity))
    groups = verified_stock_projections(tmp_path / "sources", jar)
    assert groups[0]["classes"][0]["branch_review_targets"][0]["offset"] == 0
    projection.write_text("relabelled source")
    with pytest.raises(ValueError, match="projection"):
        verified_stock_projections(tmp_path / "sources", jar)
