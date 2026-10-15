"""Conservative stage-one evidence mapping; never a simulator certificate."""

from __future__ import annotations

import hashlib
import json
import zipfile
from collections import Counter
from pathlib import Path
from typing import Any

from sls.audit.bytecode_inventory import branch_review_targets
from sls.audit.trajectory_reader import read_trajectory

FLOW_CASES = {
    "NORMAL_NEOW": ("NORMAL_RESOURCES", "LEGAL_NEOW_CHOICES", "NEOW_RNG"),
    "MAP_AND_ENCOUNTER_POOLS": ("MAP_EDGES", "ROOM_POOLS", "EVENT_PREDICATES", "BURNING_ELITE"),
    "SHOP": ("BUY_CARD_RELIC_POTION", "REMOVE_CARD", "UNAFFORDABLE", "LEAVE", "RESTOCK"),
    "REWARDS": ("GOLD", "CARD_TAKE_SKIP", "POTION_FULL_SLOT", "RELIC", "LINKED_REWARDS"),
    "ACT1_TO_ACT2": ("BOSS_REWARD", "HEAL", "PERMANENT_RESOURCES", "ACT_RNG"),
    "ACT2_TO_ACT3": ("BOSS_REWARD", "HEAL", "PERMANENT_RESOURCES", "ACT_RNG"),
    "A20_ORDERED_DOUBLE_BOSSES": tuple(f"{a}_THEN_{b}:MIN_3_TRACES" for a in
        ("AWAKENED_ONE", "TIME_EATER", "DONU_DECA") for b in
        ("AWAKENED_ONE", "TIME_EATER", "DONU_DECA") if a != b),
    "RUBY_KEY_COST": ("RECALL_INSTEAD_OF_REST_OR_UPGRADE", "ALREADY_OWNED"),
    "SAPPHIRE_KEY_COST": ("LINKED_RELIC_FORFEIT", "MATRYOSHKA",
                          "HUNGRY_FACE", "ALREADY_OWNED"),
    "EMERALD_KEY_AND_BURNING_ELITE": ("ELITE_BUFF", "VICTORY_REWARD", "TAKE_KEY", "RESOURCE_COST"),
    "ACT3_TO_ACT4": ("FIRST_BOSS_NOT_SUCCESS", "SECOND_BOSS_MISSING_KEYS", "ALL_KEYS_ENTRY", "NO_NEOW_RESET"),
    "ACT4_ROUTE": ("REST", "SHOP", "SHIELD_SPEAR:MIN_3_TRACES", "HEART:MIN_3_TRACES"),
    "TERMINAL_AND_HEART_SUCCESS": ("HEART_DEATH", "PLAYER_DEATH", "FAIRY_REVIVE", "LIZARD_TAIL_REVIVE",
                                   "SIMULTANEOUS_DEATH", "ORDINARY_VICTORY_NOT_HEART_SUCCESS",
                                   "COMPLETE_PUBLIC_STATE_ACTIONS_RNG_AND_CLEANUP"),
    "CHECKPOINT_RNG": ("ALL_REACHABLE_SCREENS", "SAME_ACTION_AFTER_RESTORE", "EACH_ACT_MIN_25_RESTORES"),
}


def file_identity(path: Path) -> dict[str, Any]:
    return {"path": str(path.resolve()), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}


def verify_bytecode(inventory: dict, stock_jar: Path) -> None:
    """Reject stale class mappings even when the enclosing JAR field was relabelled."""
    if inventory.get("schema") != "sls-stock-bytecode-inventory-v1":
        raise ValueError("unsupported bytecode inventory")
    if inventory.get("stock_jar_sha256") != file_identity(stock_jar)["sha256"]:
        raise ValueError("stock JAR identity mismatch")
    with zipfile.ZipFile(stock_jar) as archive:
        for rows in inventory["categories"].values():
            for row in rows:
                for cls in row["stock_classes"]:
                    raw = archive.read(cls["class_name"].replace(".", "/") + ".class")
                    if hashlib.sha256(raw).hexdigest() != cls["class_sha256"]:
                        raise ValueError("stock class identity mismatch")


def verified_stock_projections(root: Path, stock_jar: Path) -> list[dict]:
    """Bind existing shared/flow disassemblies to actual stock class bytes."""
    stock_hash = file_identity(stock_jar)["sha256"]
    result = []
    with zipfile.ZipFile(stock_jar) as archive:
        for identity_path in sorted(root.glob("*/identity.json")):
            identity = json.loads(identity_path.read_text(encoding="utf-8"))
            if identity.get("stock_jar_sha256") != stock_hash:
                raise ValueError("flow source stock JAR mismatch")
            classes = []
            for name, hashes in identity["classes"].items():
                raw = archive.read(name.replace(".", "/") + ".class")
                if hashlib.sha256(raw).hexdigest() != hashes["class_sha256"]:
                    raise ValueError("flow source class mismatch")
                projection = identity_path.parent / (name.removeprefix("com.megacrit.cardcrawl.") + ".txt")
                if file_identity(projection)["sha256"] != hashes["bytecode_sha256"]:
                    raise ValueError("flow bytecode projection mismatch")
                classes.append({"class_name": name, "class_sha256": hashes["class_sha256"],
                                "projection": file_identity(projection),
                                "branch_review_targets": branch_review_targets(projection.read_text(encoding="utf-8"))})
            result.append({"identity": file_identity(identity_path), "classes": classes,
                           "status": "SOURCE_BOUND_REVIEW_TARGETS_NOT_PARITY"})
    if not result:
        raise ValueError("no stock source identities found")
    return result


def natural_witnesses(batch_path: Path, stock_sha256: str) -> tuple[dict, dict]:
    """Identify owned/encountered content, never mere offers or injected scenes.

    This is evidence indexing, not parity or independent verification of every
    original-game predicate. Replay must still verify the normal-start sequence.
    """
    batch = json.loads(batch_path.read_text(encoding="utf-8"))
    launch_path = batch_path.with_suffix(".launch.json")
    launch = json.loads(launch_path.read_text(encoding="utf-8"))
    if (batch.get("schema") != "sls-act2-production-batch-v1"
            or not batch.get("execution_complete") or batch.get("execution_error")
            or launch.get("execution_error") or launch.get("mode") != "production"
            or launch.get("recovery_status") != "RECOVERED"
            or launch.get("completion", {}).get("exit_code") != 0):
        raise ValueError("natural batch execution/recovery failure")
    journal_path = Path(launch["recovery_journal"])
    journal = json.loads(journal_path.read_text(encoding="utf-8"))
    if journal.get("status") != "RECOVERED" or journal.get("recovery_failures"):
        raise ValueError("original journal recovery failure")
    # Verify preserved backup bytes, not today's live files, which may have
    # legitimately changed since an old batch completed.
    for entry in journal["entries"]:
        if entry["existed"] and file_identity(Path(entry["backup"]))["sha256"] != entry["sha256"]:
            raise ValueError("original recovery backup hash mismatch")
    witnesses: dict[str, list] = {}
    trajectories = []
    seen = set()
    for run in batch["runs"]:
        path = Path(run["output"])
        identity = file_identity(path)
        if identity["sha256"] != run["sha256"] or identity["sha256"] in seen:
            raise ValueError("natural trajectory hash mismatch or duplicate")
        seen.add(identity["sha256"])
        metadata, boundaries = read_trajectory(path)
        environment = metadata.get("environment", {})
        if (metadata.get("backend") != "original" or metadata.get("seed") != run["seed"]
                or environment.get("profile_id") != "IRONCLAD_A20_ACT2"
                or environment.get("stock_jar_sha256") != stock_sha256
                or not boundaries or not boundaries[-1]["terminal"]
                or len(boundaries) != run["boundaries"]):
            raise ValueError("natural A20 identity or completion missing")
        first = boundaries[0]
        if (first["screen"], first["act"], first["floor"],
                first["observation"]["run"]["ascension"]) != ("NEOW", 1, 0, 20):
            raise ValueError("natural A20 Neow start missing")
        observed = set()
        for boundary in boundaries:
            if not boundary["terminal"] and boundary["chosen_action"] not in boundary["candidate_actions"]:
                raise ValueError("illegal recorded natural action")
            observation = boundary["observation"]
            for key, category, field in (("deck", "cards", "card_id"),
                    ("hand", "cards", "card_id"), ("relics", "relics", "content_id"),
                    ("potions", "potions", "content_id"), ("enemies", "monsters", "monster_id")):
                for entry in observation[key]:
                    if isinstance(entry, dict) and field in entry:
                        observed.add(f"{category}:{entry[field]}")
        trajectories.append({**identity, "seed": run["seed"], "boundaries": len(boundaries),
                             "reached_act": max(b["act"] for b in boundaries)})
        for identifier in observed:
            witnesses.setdefault(identifier, []).append(identity)
    return witnesses, {"batch": file_identity(batch_path), "launch": file_identity(launch_path),
                       "journal": file_identity(journal_path), "oracle_sha256": launch["oracle_sha256"],
                       "trajectories": trajectories,
                       "use": "HISTORICAL_STOCK_REACHABILITY_ONLY_CURRENT_NATIVE_REPLAY_REQUIRED"}


def map_scope(ledger: dict, bytecode: dict, root: Path) -> dict:
    """Keep every candidate and every legacy gap; static links confer no passes.

    Method lists are finite review targets. Their internal branch review remains an
    explicit obligation: listing methods alone cannot establish branch coverage.
    """
    if ledger.get("profile") != "IRONCLAD_A20_HEART" or ledger.get("ascension") != 20:
        raise ValueError("requires Ironclad A20 Heart ledger")
    candidates = ledger["obligations"]
    if len({row["id"] for row in candidates}) != len(candidates):
        raise ValueError("duplicate candidate")
    index = {(category, row["content_id"]): row
             for category, rows in bytecode["categories"].items() for row in rows}
    result = []
    for old in candidates:
        item = index.get((old["category"], old["content_id"]), {})
        classes = item.get("stock_classes", [])
        methods = sorted({method for cls in classes for method in cls["methods"]})
        evidence = []
        for reference in old.get("evidence", []):
            path = root / reference
            evidence.append({"reference": reference,
                             "identity": file_identity(path) if path.is_file() else None,
                             "use": "REPLAY_OR_REVIEW_REQUIRED_NOT_INHERITED_PASS"})
        # Only acquisition is excluded. Offers and their RNG remain in scope.
        excluded = old["id"] == "relics:PRISMATIC_SHARD"
        if old.get("policy_excluded") and not excluded:
            raise ValueError("unreviewed policy exclusion")
        exclusions = ([{"operation": "ACQUIRE", "basis": "PROJECT_SUPPORTED_SCOPE_POLICY",
                        "sources": [file_identity(root / path) for path in (
                            "src/sls/content/scope.json", "src/sls/content/scope.py")],
                        "retained": ["VISIBLE_OFFER", "POOL_AND_RNG"]}] if excluded else [])
        required = ["REACHABILITY_SOURCE_AND_PREDICATES", "SOURCE_BRANCH_REVIEW",
                    "BEFORE_ACTIONS_AFTER_RNG", "RESOURCE_AND_SELECTION_BOUNDARIES"]
        required += list(FLOW_CASES.get(old["content_id"], ())) if old["category"] == "flow" else []
        if old["category"] == "cards":
            required += ["UPGRADE_AND_THRESHOLD", "GENERATE_COPY_EXHAUST_AND_PERMANENT_DECK"]
        if old["category"] in {"encounters", "monsters", "flow", "shared"}:
            required += ["TRIGGER_ORDER_AND_TERMINATION", "RESTORE_SAME_ACTION"]
        result.append({"id": old["id"], "category": old["category"],
                       "candidate_basis": old.get("candidate_basis", "EXPLICIT_FLOW_OR_MECHANISM"),
                       "reachability": "UNCONFIRMED", "exclusions": exclusions,
                       "stock_class_status": item.get("stock_class_status", "REQUIRES_FLOW_SOURCE_MAP"),
                       "stock_classes": classes, "method_review_targets": methods,
                       "bytecode_branch_review_targets": [
                           {"class_name": cls["class_name"], **branch, "status": "MISSING"}
                           for cls in classes for branch in cls.get("branch_review_targets", [])],
                       "native_references": item.get("simulator_references", old.get("native_paths", [])),
                       "required_obligations": [{"id": name, "status": "MISSING"} for name in required],
                       "legacy_missing_branches": old.get("missing_branches", []),
                       "legacy_status": old["status"], "existing_evidence": evidence,
                       "qualification": "NOT_QUALIFIED"})
    return {"schema": "sls-a20-validation-scope-v1", "profile": "IRONCLAD_A20_HEART",
            "purpose": "FINITE_REVIEW_QUEUE_NOT_COMPLETED_BRANCH_INVENTORY_OR_CERTIFICATE",
            "candidate_count": len(result), "unknowns_retained_in_denominator": True,
            "branch_expansion_rule": "ONLY_DIRECT_BRANCHES_OF_FIRST_DIVERGENCE; RECORD_CHANGES",
            "inherited_passes": False, "stage1_complete": False,
            "blockers": ["REACHABILITY_AND_PREDICATE_REVIEW_INCOMPLETE",
                         "SOURCE_BRANCH_TO_SCENE_MAPPING_AND_TRANSITIVE_ACTION_REVIEW_INCOMPLETE"],
            "summary": {"categories": dict(Counter(row["category"] for row in result)),
                        "bytecode_branch_sites": sum(len(row["bytecode_branch_review_targets"]) for row in result),
                        "class_mapping": dict(Counter(row["stock_class_status"] for row in result)),
                        "existing_evidence_references": sum(len(row["existing_evidence"]) for row in result),
                        "missing_evidence_files": sum(e["identity"] is None for row in result
                                                      for e in row["existing_evidence"])},
            "obligations": result}
