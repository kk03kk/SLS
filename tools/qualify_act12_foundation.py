"""Bind foundation evidence to final source; incomplete evidence cannot qualify."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

from sls.backends.simulator import native
from sls.rl.training_contract import (
    local_source_digest,
    native_source_digest,
    training_validation_digest,
)
from tools.summarize_act2_qualification import rollup


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


CHECK_FILES = ("pytest.xml", "ruff.txt", "content.txt", "vocabulary.txt", "configs.txt")


def validation_source_identity() -> dict:
    project = Path(__file__).resolve().parents[1]
    # Publication receipts refer back to these checks. Exclude that metadata
    # from the fingerprint to avoid a self-referential hash; regression
    # fixtures and all executable test/tool sources remain included.
    publication = project / "tests/fixtures/evidence/act12-foundation"
    paths = ["tools", *(p.relative_to(project).as_posix()
                        for p in (project / "tests").rglob("*")
                        if p.is_file() and not p.is_relative_to(publication)
                        and "__pycache__" not in p.parts and p.suffix not in {".pyc", ".pyo"})]
    return {"native_source_sha256": native_source_digest(),
            "training_validation_sha256": training_validation_digest(),
            "verification_source_sha256": local_source_digest(paths)}


def validation_manifest(root: Path) -> dict:
    return {"schema": "sls-foundation-local-validation-v1", **validation_source_identity(),
            "checks": {name: digest(root / name) for name in CHECK_FILES}}


def validation_matches(root: Path, receipt: dict) -> bool:
    try:
        started = json.loads((root / "validation-start.json").read_text())
        return started == validation_source_identity() and receipt == validation_manifest(root)
    except (OSError, ValueError):
        return False


def timing_contract_row(unconditional: dict, conditional: dict, *, natural: bool, source: str) -> bool:
    """Full conditional evidence can explain a difference, never erase its status."""
    if (conditional.get("native_source_sha256", source) != source
            or conditional.get("seed") != unconditional.get("seed")
            or conditional.get("first_divergence") is not None
            or not conditional.get("terminal_reason_verified")):
        return False
    if natural:
        return (conditional.get("status") == "CONDITIONAL_PUBLIC_TRAJECTORY_MATCH"
                and bool(conditional.get("clock_inputs"))
                and conditional.get("trajectory_sha256") == unconditional.get("trajectory_sha256")
                and conditional.get("evaluation_environment") == unconditional.get("evaluation_environment")
                and all(conditional.get(k) for k in (
                    "stock_clock_log_sha256", "oracle_build_manifest_sha256", "launch_evidence_sha256")))
    inputs = conditional.get("controlled_runtime_inputs", ())
    return (conditional.get("status") == "CONDITIONAL_SYSTEM_BRANCH_MATCH" and bool(inputs)
            and conditional.get("stock_action_unavailable") is None
            and all(set(row.get("stock_measured_inputs", {})) == {"discovery_retrieval_updates"}
                    and type(row["stock_measured_inputs"]["discovery_retrieval_updates"]) is int
                    and 1 <= row["stock_measured_inputs"]["discovery_retrieval_updates"] <= 180
                    for row in inputs))


def qualify(root: Path, *, revision: str | None = None) -> dict:
    source, implementation = native_source_digest(), training_validation_digest()
    blockers, inputs = [], {}

    def read(name: str) -> dict | None:
        path = root / name
        if not path.is_file():
            blockers.append(f"missing required evidence: {name}")
            return None
        inputs[name] = digest(path)
        return json.loads(path.read_text(encoding="utf-8"))

    checks = read("validation-final.json")
    if checks and not validation_matches(root, checks):
        blockers.append("local regression/check evidence differs from final source or recorded outputs")

    frozen = read(f"freeze-{revision}.json" if revision else
                  "freeze-final.json" if (root / "freeze-final.json").is_file() else "freeze.json")
    selection = read("selection-final.json")
    if frozen and (frozen["native_source_sha256"] != source
                   or frozen.get("training_validation_sha256") != implementation):
        blockers.append("frozen source differs from current implementation")
    if native.NATIVE_SOURCE_SHA256 != source:
        blockers.append("native binary/source identity mismatch")
    boundary = read(f"boundaries-{revision}.json" if revision else
                    "boundaries-release.json" if (root / "boundaries-release.json").is_file()
                    else "boundaries-final.json")
    if boundary:
        if (boundary["native_source_sha256"] != source
                or boundary.get("training_validation_sha256") != implementation
                or boundary["decisions"] < 10000 or boundary["continuous_restores"] < 100
                or not all(any(k.startswith(f"ACT{act}:") for k in boundary["restore_screen_counts"])
                           for act in (1, 2))
                or not boundary.get("controlled_multi_selection")
                or not all(r["passed"] and r["continuous_actions"] >= 4
                           for r in boundary["controlled_multi_selection"])):
            blockers.append("legal boundary/continuous restoration coverage incomplete or stale")
    migration = read("migration-final.json")
    if migration and (not migration.get("passed") or not migration.get("source_file_unchanged")
                      or migration.get("diagnostic_native_sha256") != source):
        blockers.append("v5/v6 migration compatibility not verified on current native")
    if revision == "v6":
        soak = read("v6/mixed-soak.json")
        if (not boundary or boundary.get("policy_architecture") != "sls-recurrent-relational-policy-v6"
                or not soak or not soak.get("passed")
                or soak.get("architecture") != "sls-recurrent-relational-policy-v6"
                or soak.get("native_source_sha256") != source
                or soak.get("training_validation_sha256") != implementation
                or not soak.get("exact_resume_checks")
                or not all(soak["exact_resume_checks"].values())
                or soak.get("updates", 0) < 8
                or soak.get("new_decisions", 0) < 16000
                or not soak.get("binding_parameter_change_norms")
                or not all(v > 0 for v in soak["binding_parameter_change_norms"].values())
                or soak.get("source_checkpoint_sha256") != migration.get("source_checkpoint_sha256")):
            blockers.append("v6 actual-parent mixed training, bindings or exact recovery not verified")
    controlled = read("controlled-final.json")
    systems = [read(f"systems-final-{i}.json") for i in range(3)]
    conditional_systems = [read(f"systems-conditional-{i}.json") for i in range(3)]
    for plain, conditional in zip(systems, conditional_systems):
        if plain and conditional and (conditional.get("native_source_sha256") != source
                                      or plain.get("stock_capture_sha256") != conditional.get("stock_capture_sha256")):
            blockers.append("conditional system report source/capture mismatch")
    supplemental_launches = []
    if (root / "system-replacements.json").is_file():
        replacements = read("system-replacements.json")
        for replacement in replacements["rows"]:
            index = replacement["old_batch_index"]
            new = read(replacement["unconditional"])
            conditional = read(replacement["conditional"])
            if (not new or not conditional or new["native_source_sha256"] != source
                    or conditional["native_source_sha256"] != source
                    or new["stock_capture_sha256"] != conditional["stock_capture_sha256"]):
                blockers.append("invalid system replacement source/capture")
                continue
            seed = replacement["seed"]
            for destination, report in ((systems[index], new), (conditional_systems[index], conditional)):
                candidates = [r for r in report["runs"] if r["seed"] == seed]
                if len(candidates) != 1 or not any(r["seed"] == seed for r in destination["runs"]):
                    blockers.append("system replacement seed missing or ambiguous")
                    continue
                destination["runs"] = [r for r in destination["runs"] if r["seed"] != seed] + candidates
            supplemental_launches.append(replacement["capture_basename"])
    natural = [read(f"natural-final-{r['seed']}.json") for r in selection["runs"]] if selection else []
    differential = None
    contract_explanations = []
    if selection and controlled and all(systems) and len(natural) == 8 and all(natural):
        differential = rollup([controlled], systems, natural, selection, source)
        remaining = list(differential["blockers"])
        if frozen.get("discovery_environment_contract") == "DISCOVERY_FIXED_RETRIEVAL_UPDATES_14_V1":
            conditional_by_seed = {r["seed"]: r for report in conditional_systems if report
                                   and report["native_source_sha256"] == source for r in report["runs"]}
            rows = {r["seed"]: r for report in systems for r in report["runs"]}
            explained = []
            for seed in differential["controlled_missing_or_failed"]:
                if seed in rows and seed in conditional_by_seed and timing_contract_row(
                    rows[seed], conditional_by_seed[seed], natural=False, source=source
                ):
                    explained.append(seed)
            if set(explained) == set(differential["controlled_missing_or_failed"]):
                remaining = [b for b in remaining if b != "controlled obligations missing, incomplete or divergent"]
            contract_explanations.extend({"seed": s, "role": "controlled", "unconditional_match": False,
                                          "full_clock_conditioned_replay": True} for s in explained)
            natural_by_seed = {r["seed"]: r for r in natural}
            explained_natural = []
            for seed in differential["production_missing_or_failed"]:
                conditional = read(f"natural-conditional-{seed}.json")
                if conditional and timing_contract_row(natural_by_seed[seed], conditional, natural=True, source=source):
                    explained_natural.append(seed)
            if set(explained_natural) == set(differential["production_missing_or_failed"]):
                remaining = [b for b in remaining if b != "production canaries missing or divergent"]
            contract_explanations.extend({"seed": s, "role": "natural", "unconditional_match": False,
                                          "full_clock_conditioned_replay": True} for s in explained_natural)
        blockers.extend(remaining)
        explained_seeds = {r["seed"] for r in contract_explanations if r["role"] == "natural"}
        if any(not r.get("terminal_reason_verified") and r["seed"] not in explained_seeds for r in natural):
            blockers.append("natural terminal outcomes not verified")
    gremlin = read("gremlin-leader-final.json")
    if (not gremlin or gremlin.get("native_source_sha256") != source or len(gremlin.get("runs", ())) < 3
            or any(r.get("status") != "HARNESS_MATCH" or r.get("first_divergence") is not None
                   for r in gremlin.get("runs", ()))):
        blockers.append("third Act2 elite supplemental live parity incomplete")
    coverage = read("stock-coverage.json")
    if coverage and (not coverage.get("coverage_complete") or coverage.get("native_source_sha256") != source
                     or coverage.get("selection_sha256") != inputs.get("selection-final.json")):
        blockers.append("eight natural canary roles not witnessed on frozen selection")
    recoveries = []
    natural_launches = sorted({r.get("capture_basename", "natural-0") for r in natural if r})
    required_launches = ["controlled-stock", *(f"systems-{i}" for i in range(3)), *natural_launches,
                 gremlin.get("capture_basename", "gremlin-leader-stock") if gremlin else "gremlin-leader-stock",
                 *sorted(set(supplemental_launches))]
    for name in required_launches:
        launch = read(name + ".launch.json")
        if not launch:
            continue
        if launch.get("execution_error") and (root / (name + ".launch.review.json")).is_file():
            from tools.verify_oracle import read_verified_launch
            read(name + ".launch.review.json")
            launch = read_verified_launch(root / (name + ".launch.json"))
        path = Path(launch["recovery_journal"])
        journal = json.loads(path.read_text(encoding="utf-8"))
        mismatches = []
        for entry in journal["entries"]:
            target = Path(entry["target"])
            if ((entry["existed"] and (not target.is_file() or digest(target) != entry["sha256"]))
                    or (not entry["existed"] and target.exists())):
                mismatches.append(str(target))
        for tree in journal.get("protected_trees", ()):
            actual = sorted(p.relative_to(Path(tree["root"])).as_posix()
                            for p in Path(tree["root"]).rglob("*") if p.is_file())
            if actual != tree["files"]:
                mismatches.append(tree["root"])
        recovered = (journal["status"] == "RECOVERED" and not journal.get("recovery_failures")
                     and not mismatches and launch["recovery_status"] == "RECOVERED")
        recoveries.append({"batch": name, "journal_sha256": digest(path), "recovered": recovered,
                           "current_user_file_mismatches": mismatches})
        if (not recovered or launch.get("execution_error")
                or (launch.get("completion") or {}).get("exit_code") != 0
                or (frozen and launch["oracle_sha256"] != frozen["oracle_sha256"])):
            blockers.append(f"stock execution/recovery/source failure: {name}")
    retained_failures = []
    for path in sorted(root.glob("*.launch.json")):
        name = path.name.removesuffix(".launch.json")
        if name in required_launches:
            continue
        launch = json.loads(path.read_text())
        if launch.get("execution_error") is None and (launch.get("completion") or {}).get("exit_code") == 0:
            continue
        inputs[path.name] = digest(path)
        journal_path = Path(launch["recovery_journal"])
        journal = json.loads(journal_path.read_text())
        mismatches = []
        for entry in journal["entries"]:
            target = Path(entry["target"])
            if ((entry["existed"] and (not target.is_file() or digest(target) != entry["sha256"]))
                    or (not entry["existed"] and target.exists())):
                mismatches.append(str(target))
        for tree in journal.get("protected_trees", ()):
            actual = sorted(p.relative_to(Path(tree["root"])).as_posix()
                            for p in Path(tree["root"]).rglob("*") if p.is_file())
            if actual != tree["files"]:
                mismatches.append(tree["root"])
        recovered = (journal["status"] == "RECOVERED" and not journal.get("recovery_failures")
                     and launch["recovery_status"] == "RECOVERED" and not mismatches)
        retained_failures.append({"batch": name, "launch_sha256": digest(path),
            "original_execution_error": launch.get("execution_error"),
            "completion": launch.get("completion"), "journal_sha256": digest(journal_path),
            "recovered": recovered, "qualification": "FAILED_ATTEMPT_NOT_COUNTED_AS_PASS"})
        if not recovered:
            blockers.append(f"failed attempt user-file recovery incomplete: {name}")
    xml_path = root / "pytest.xml"
    if xml_path.is_file():
        inputs["pytest.xml"] = digest(xml_path)
        suites = ET.parse(xml_path).getroot().iter("testsuite")
        if any(int(s.get("errors", 0)) or int(s.get("failures", 0)) for s in suites):
            blockers.append("full regression failed")
        # Historical A0 policy is outside the A20 task; never use its skip as evidence.
        skipped = [case.attrib for case in ET.parse(xml_path).getroot().iter("testcase")
                   if case.find("skipped") is not None]
        if any(case.get("classname") != "tests.diagnostics.test_seed_audit" for case in skipped):
            blockers.append("unexplained skipped regression evidence")
    else:
        blockers.append("missing full regression evidence")
        skipped = []
    for name, marker in (("ruff.txt", "All checks passed!"), ("content.txt", "content registry: OK"),
                         ("vocabulary.txt", "policy vocabulary: OK"), ("configs.txt", "configurations valid")):
        path = root / name
        if not path.is_file() or marker not in path.read_text(encoding="utf-8"):
            blockers.append(f"missing or failed local check: {name}")
        elif path.is_file():
            inputs[name] = digest(path)
            if name == "configs.txt":
                match = re.search(r"(\d+)/(\d+) configurations valid", path.read_text(encoding="utf-8"))
                if not match or int(match[1]) != int(match[2]) or int(match[2]) == 0:
                    blockers.append("configuration validation was not fully successful")
    return {"schema": "sls-act12-foundation-qualification-v1", "native_source_sha256": source,
            "training_validation_sha256": implementation,
            "original_source_sha256": local_source_digest(("src/sls/backends/original", "native/oracle")),
            "audit_source_sha256": local_source_digest(("src/sls/audit", "tools/qualify_act12_foundation.py",
                "tools/replay_act2_system_batch.py", "tools/replay_act2_encounter_batch.py",
                "tools/replay_act2_production_trajectory.py", "tools/verify_act12_foundation.py",
                "tools/verify_oracle.py", "tools/capture_original_card_batch.py")),
            "input_sha256": inputs, "differential": differential, "recoveries": recoveries,
            "retained_failed_attempts": retained_failures,
            "excluded_historical_skips": skipped,
            "blockers": sorted(set(blockers)),
            "qualification": "BLOCKED" if blockers else (
                "PASS_CONTRACT_BOUNDED_ACT12" if contract_explanations else "PASS_BOUNDED_ACT12"),
            "discovery_environment_contract": frozen.get("discovery_environment_contract") if frozen else None,
            "contract_explanations": contract_explanations,
            "scope": "A20 normal start to Act2 boss clear; 72 controlled obligations and 8 natural coverage canaries",
            "limits": ["Finite evidence, not exhaustive game-combination proof",
                       "Conditional Discovery replay is not an unconditional match",
                       "v6 efficacy untrained; finite representation checks are not an injectivity proof"
                       if revision == "v6" else "Default v5 capacity limitations retained; v6 efficacy untrained",
                       "No production training or automatic server submission"]}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--revision", choices=("v6",))
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("immutable qualification receipt exists")
    report = qualify(args.root, revision=args.revision)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: report[k] for k in ("qualification", "blockers")}))
    return 0 if not report["blockers"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
