"""Prepare, inspect and CPU-validate the normal-Neow Act2 A/B/C comparison.

This entry never trains the real model, submits a job, pushes code or launches
the stock game. Local readiness and server CUDA qualification remain separate.
"""

from __future__ import annotations

import argparse
import copy
import gzip
import json
import math
import os
import subprocess
import sys
import tomllib
from pathlib import Path

# Unconditional: this is a local CPU preparation command, even on a CUDA host.
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
os.environ["SLS_TEST_CUDA"] = "0"
for _name in ("OMP_NUM_THREADS", "MKL_NUM_THREADS", "OPENBLAS_NUM_THREADS"):
    os.environ[_name] = "2"

from sls.audit.development_seeds import seed_intervals
from sls.rl.training_contract import (
    ROOT,
    local_source_digest,
    native_artifact,
    native_source_digest,
    sha256_file,
    training_implementation_digest,
)
from tools.prepare_act12_long_run import toml_text

PARENT = "local/runs/ironclad-a20-act1-win-90m-continuation/final.pt"
PARENT_SHA = "274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0"
PARENT_STEPS = 90_013_696
PERIODIC = (8_000_100_000_000, 8_000_100_002_048)
CONFIRMATION = (8_000_101_000_000, 8_000_101_004_096)
SEALED = (9_000_000_000_000, 9_000_000_004_096)
TESTS = (
    "tests/rl/test_normalstart_study.py", "tests/rl/test_v6_finalization.py",
    "tests/rl/test_normalstart_preparation.py",
    "tests/rl/test_normalstart_release.py", "tests/audit/test_normalstart_act12_gate.py",
    "tests/rl/test_ppo_math.py", "tests/rl/test_advantage_domain_diagnostics.py",
    "tests/rl/test_numerical_shutdown.py", "tests/model/test_action_reference_memory.py",
)
VALIDATION_SOURCES = (*TESTS, "tools/prepare_act12_normalstart_study.py",
                      "tools/preflight_training.py", "tools/benchmark_workers.py",
                      "tools/prepare_and_train.py", "tools/check_training_configs.py",
                      "tools/qualify_normalstart_act12.py", "tools/publish_normalstart_release.py",
                      "tools/verify_act12_foundation.py")


def _json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    os.replace(temporary, path)


def _write_identical_or_new(path, text, *, bind_release=False):
    if path.exists() and path.read_text(encoding="utf-8") != text:
        old = tomllib.loads(path.read_text(encoding="utf-8"))
        new = tomllib.loads(text)
        for config in (old, new):
            config["run"].pop("foundation_evidence", None)
            config["warm_start"].pop("simulator_transition", None)
        if not bind_release or old != new or (ROOT / new["run"]["output"]).exists():
            raise FileExistsError(f"refuse to overwrite a different or started study config: {path}")
        path.write_text(text, encoding="utf-8")
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        path.write_text(text, encoding="utf-8")


def check_seed_reservations(root=ROOT, *, excluded=(), allocations=None):
    """Scan actual artifacts and preregistered configs, including compressed JSON.

    No evaluation episode is executed here. A malformed evidence file fails the
    scan rather than silently freeing its seeds.
    """
    allocations = allocations if allocations is not None else [
        ("periodic", *PERIODIC), ("confirmation", *CONFIRMATION),
        ("ppo", 210_000_000, 250_000_128), ("adaptation", 260_000_000, 260_000_512)]
    inputs = []
    excluded = {Path(p).resolve() for p in excluded}
    for directory in ("configs", "local/runs", "local/reports", "tests/fixtures/evidence"):
        for path in sorted((root / directory).rglob("*")):
            if not path.is_file() or path.resolve() in excluded:
                continue
            if path.name in {f"ironclad_a20_act12_normalstart_{arm}_r1.toml" for arm in "abc"}:
                continue
            if not path.name.endswith((".json", ".jsonl", ".json.gz", ".jsonl.gz", ".toml")):
                continue
            raw = path.read_bytes()
            text = (gzip.decompress(raw) if path.suffix == ".gz" else raw).decode("utf-8-sig")
            name = path.name.removesuffix(".gz")
            if name.endswith(".toml"):
                records = [tomllib.loads(text)]
            elif name.endswith(".jsonl"):
                records = [json.loads(line) for line in text.splitlines() if line.strip()]
            else:
                try:
                    records = [json.loads(text)]
                except ValueError as error:
                    if not text.startswith("sbatch "):
                        raise ValueError(f"unreadable seed inventory input: {path}") from error
                    # Historical dry-run receipts have a .json suffix but
                    # contain only a shell command. Reserve every integer in
                    # them; referenced configs are scanned independently.
                    import re
                    records = [{"seeds": [int(token) for token in re.findall(r"\d+", text)]}]
            for record in records:
                for low, high in seed_intervals(record):
                    for label, start, end in allocations:
                        if low < end and start < high:
                            raise ValueError(f"{label} seed collision with {path}: [{low},{high})")
            inputs.append({"path": path.relative_to(root).as_posix(), "sha256": sha256_file(path)})
    return {"schema": "sls-normalstart-study-seeds-v1", "scanned_inputs": inputs,
            "periodic_seeds": PERIODIC, "confirmation_seeds": CONFIRMATION,
            "reserved_final_seeds": SEALED, "final_set_used": False,
            "paired_ppo_seed": 210_000_000, "paired_ppo_reserved_seeds": [210_000_000, 250_000_128],
            "adaptation_seed_range": [260_000_000, 260_000_512],
            "adaptation_random_seed": 260_000_000,
            "pairing": "A/B/C share PPO initialization seed and resets; policies may subsequently diverge"}


def make_config(base, arm):
    result = copy.deepcopy(base)
    result.pop("start_curriculum", None)
    run = result["run"]
    run.pop("foundation_evidence", None)  # Old mixed qualification is inapplicable.
    run.update(seed=210_000_000, output=f"local/runs/ironclad-a20-act12-normalstart-{arm.lower()}-r1",
               benchmark=f"local/runs/preparation/act12-normalstart-{arm.lower()}-r1/benchmark.json",
               periodic_evaluation_seed_start=PERIODIC[0], periodic_evaluation_seed_count=PERIODIC[1] - PERIODIC[0],
               final_evaluation_seed_start=CONFIRMATION[0], final_evaluation_seed_count=CONFIRMATION[1] - CONFIRMATION[0],
               final_evaluation_role="normal-start-development-confirmation")
    # Missing reviewed source transition deliberately blocks warm start. Never
    # attach the old transition or manufacture a VERIFIED_RULE_CORRECTION flag.
    result["warm_start"].pop("simulator_transition", None)
    result["stages"]["train"].update(target_environment_steps=PARENT_STEPS + math.ceil(1_000_000 / 16384) * 16384,
        evaluate_every_steps=250_000, checkpoint_every_steps=250_000)
    result["ppo"].update(advantage_normalization="global" if arm == "B" else "domain",
                           gradient_diagnostic_interval=50)
    if arm == "C":
        from dataclasses import asdict

        from sls.rl.value_adaptation import ValueAdaptationConfig
        result["value_adaptation"] = asdict(ValueAdaptationConfig(seed_start=260_000_000, random_seed=260_000_000))
    return result


def review_simulator_evidence():
    from sls.audit.validation_scope import natural_witnesses
    from sls.rl.normalstart_release import RELEASE, validate_qualification

    released = ROOT / RELEASE / "qualification.json"
    if released.is_file():
        qualification = validate_qualification(released, sha256_file(released))
        return {"status": qualification["qualification"], "source": qualification["native_source_sha256"],
                "qualification": {"path": str(released), "sha256": sha256_file(released)},
                "scope": qualification["scope"], "scope_is_exhaustive": False,
                "raw_and_conditional_differences_retained": True, "blockers": [],
                "boundary_summary": qualification["boundary_summary"], "facts": qualification["facts"]}

    summary_path = ROOT / "local/reports/a20-simulator-validation/act12-replay/summary.json"
    if not summary_path.is_file():
        return {"status": "MISSING_PRIVATE_EVIDENCE", "blockers": ["CURRENT_ACT12_CORPUS_MISSING"]}
    summary = json.loads(summary_path.read_text(encoding="utf-8"))
    if summary["native_source_sha256"] != native_source_digest():
        return {"status": "STALE_SOURCE", "blockers": ["ACT12_REPLAY_SOURCE_CHANGED"]}
    verified = 0
    def verify(value):
        nonlocal verified
        if isinstance(value, dict):
            if "path" in value and "sha256" in value:
                if sha256_file(Path(value["path"])) != value["sha256"]:
                    raise ValueError(f"simulator evidence changed: {value['path']}")
                verified += 1
            for child in value.values():
                verify(child)
        elif isinstance(value, list):
            for child in value:
                verify(child)
    verify(summary)
    build = json.loads(Path(summary["oracle_build"]["path"]).read_text(encoding="utf-8"))
    receipts = [natural_witnesses(Path(item["batch"]["path"]), build["dependencies"]["game"])[1]
                for item in summary["natural_receipts"]]
    return {"status": "NOT_QUALIFIED", "source": summary["native_source_sha256"],
            "summary": {"path": str(summary_path), "sha256": sha256_file(summary_path)},
            "verified_file_identities": verified, "recovered_batches": len(receipts),
            "raw_and_conditional_differences_retained": True,
            "blockers": summary["blockers"], "scope": "REVIEW_EXISTING_OFFLINE_EVIDENCE_ONLY"}


def parent_cpu_check():
    import torch

    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import IRONCLAD_A20_ACT2
    from sls.model import PolicyBatch
    from sls.rl.checkpoint import policy_from_training_checkpoint
    from sls.rl.model_migration import migrate_v5_role_policy

    path = ROOT / PARENT
    if not path.is_file():
        return {"status": "MISSING_PRIVATE_PARENT"}
    if sha256_file(path) != PARENT_SHA:
        raise ValueError("original 90M parent checksum changed")
    with torch.random.fork_rng(devices=[]):
        payload = torch.load(path, weights_only=False, map_location="cpu")
        old = policy_from_training_checkpoint(payload).eval()
        new, migration = migrate_v5_role_policy(payload)
        new.eval()
        memory = [old.initial_memory(1), new.initial_memory(1)]
        backend = SimulatorBackend(IRONCLAD_A20_ACT2)
        decision = backend.reset(260_000_600)
        maxima = {"logits": 0., "value": 0., "next_memory": 0.}
        action, reward = torch.zeros(1, dtype=torch.long), torch.zeros(1)
        decisions = 0
        with torch.no_grad():
            for step in range(32):
                batch = PolicyBatch.from_decisions([decision], old.config)
                outputs = [model(*batch.model_inputs(), memory=m, episode_start_mask=torch.tensor([step == 0]),
                            previous_action_types=action, previous_rewards=reward)
                           for model, m in zip((old, new), memory, strict=True)]
                for name in maxima:
                    error = float((getattr(outputs[0], name) - getattr(outputs[1], name)).abs().max())
                    maxima[name] = max(maxima[name], error)
                    if error > 1e-5:
                        raise ValueError(f"90M migration prediction differs: {name}={error}")
                index = int(outputs[0].logits.argmax(-1))
                transition = backend.step(decision.actions[index].candidate_id)
                decisions += 1
                action = batch.action_types[:, index] + 1
                reward = torch.tensor([transition.reward], dtype=torch.float32)
                decision = transition.decision
                memory = [output.next_memory for output in outputs]
                if transition.terminated or transition.truncated:
                    break
    if sha256_file(path) != PARENT_SHA:
        raise ValueError("parent changed during CPU review")
    transfers = {}
    from types import SimpleNamespace

    from sls.rl.act1_transfer import initialize_act1_weights
    from sls.rl.value_adaptation import state_digest
    for arm in "ABC":
        config_path = ROOT / f"configs/train/ironclad_a20_act12_normalstart_{arm.lower()}_r1.toml"
        if not config_path.exists():
            continue
        config = tomllib.loads(config_path.read_text(encoding="utf-8"))
        if "simulator_transition" not in config["warm_start"]:
            continue
        trainer = SimpleNamespace(model=new, workers=SimpleNamespace(profile=IRONCLAD_A20_ACT2),
                                  optimizer=SimpleNamespace(state={}), environment_steps=0, update=0)
        initialize_act1_weights(trainer, config, root=ROOT)
        transfers[arm] = {"environment_steps": trainer.environment_steps,
                          "migrated_state_sha256": state_digest(new.state_dict())}
    if transfers and (len(transfers) != 3 or len({row["migrated_state_sha256"] for row in transfers.values()}) != 1):
        raise ValueError("A/B/C actual 90M initial weights differ")
    return {"status": "BOUNDED_CPU_MIGRATION_VERIFIED", "parent_sha256": PARENT_SHA,
            "decisions": decisions, "seed": 260_000_600, "max_abs_error": maxima,
            "actual_warm_start_transfers": transfers,
            "migration": migration, "learning_claim": False}


def prepare(output, *, validate=False, bind_release=False):
    import torch
    torch.set_num_threads(2)
    if torch.cuda.is_available():
        raise RuntimeError("local preparation must not expose CUDA")
    output = Path(output).resolve()
    if not output.is_relative_to((ROOT / "local/reports").resolve()):
        raise ValueError("preparation reports must remain under local/reports")
    seeds = check_seed_reservations(excluded=[output / "seeds.json", output / "preparation.json"])
    output.mkdir(parents=True, exist_ok=True)
    base = tomllib.loads((ROOT / "configs/train/ironclad_a20_act12_real_entry_20m_v6_r1.toml").read_text(encoding="utf-8"))
    configs = {}
    from sls.rl.normalstart_release import RELEASE, release_bindings
    bindings = release_bindings(ROOT) if (ROOT / RELEASE / "simulator-transition.json").exists() else None
    if bind_release and bindings is None:
        raise ValueError("cannot bind absent rule release evidence")
    from tools.check_training_configs import validate as validate_config
    for arm in "ABC":
        config = make_config(base, arm)
        if bindings:
            config["run"]["foundation_evidence"] = bindings["qualification"]
            config["warm_start"]["simulator_transition"] = bindings["transition"]
        path = ROOT / f"configs/train/ironclad_a20_act12_normalstart_{arm.lower()}_r1.toml"
        problems = validate_config(path, config)
        if problems:
            raise ValueError(problems)
        _write_identical_or_new(path, toml_text(config), bind_release=bind_release)
        configs[arm] = {"path": path.relative_to(ROOT).as_posix(), "sha256": sha256_file(path)}
    _json(output / "seeds.json", seeds)
    result = {"schema": "sls-act12-normalstart-preparation-v1", "cpu_only": True,
              "local_threads": 2, "local_max_workers": 2, "configs": configs,
              "native_source_sha256": native_source_digest(), "native_artifact": native_artifact(),
              "training_implementation_sha256": training_implementation_digest(),
              "validation_sources_sha256": local_source_digest(VALIDATION_SOURCES),
              "parent": {"path": PARENT, "sha256": PARENT_SHA, "environment_steps": PARENT_STEPS},
              "seed_registry": {"path": str(output / "seeds.json"), "sha256": sha256_file(output / "seeds.json")},
              "pilot_minimum_new_decisions": 1_015_808,
              "accounting": "C collector decisions + PPO decisions; final partial budget rounds up to a whole PPO rollout",
              "future_budget_options_only": [20_000_000, 40_000_000],
              "engineering": {"status": "NOT_RUN"}, "server": {"status": "NOT_RUN"},
              "learning": {"status": "NOT_RUN"}, "simulator": review_simulator_evidence(),
              "launch_ready": False,
              "local_ready_for_server_checks": False,
              "blockers": ["CURRENT_ACT12_SIMULATOR_QUALIFICATION_MISSING", "REVIEWED_PARENT_TO_CURRENT_SOURCE_TRANSITION_MISSING",
                           "SERVER_FORMAL_SCALE_MEMORY_AND_RECOVERY_NOT_RUN"]}
    if bindings:
        result["blockers"] = ["SERVER_FORMAL_SCALE_MEMORY_AND_RECOVERY_NOT_RUN"]
    if validate:
        xml = output / "cpu-tests.xml"
        command = [sys.executable, "-m", "pytest", *TESTS, "-q", "--tb=short", f"--junitxml={xml}"]
        with (output / "cpu-tests.log").open("w", encoding="utf-8") as log:
            completed = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        result["engineering"] = {"status": "PASSED" if completed.returncode == 0 else "FAILED",
                                  "command": command, "exit_code": completed.returncode,
                                  "junit_path": str(xml),
                                  "junit_sha256": sha256_file(xml) if xml.is_file() else None}
        if xml.is_file():
            import xml.etree.ElementTree as ET
            suites = list(ET.parse(xml).getroot().iter("testsuite"))
            result["engineering"]["tests"] = {key: sum(int(s.attrib.get(key, 0)) for s in suites)
                                               for key in ("tests", "failures", "errors", "skipped")}
        result["parent_cpu_check"] = parent_cpu_check()
    parent_check = result.get("parent_cpu_check", {})
    parent_verified = (parent_check.get("status") == "BOUNDED_CPU_MIGRATION_VERIFIED"
                       and len(parent_check.get("actual_warm_start_transfers", {})) == 3)
    result["local_ready_for_server_checks"] = bool(bindings and result["engineering"]["status"] == "PASSED" and parent_verified)
    if validate and not parent_verified:
        result["blockers"].append("ACTUAL_PARENT_INITIALIZATION_NOT_VERIFIED")
    if (result["training_implementation_sha256"] != training_implementation_digest()
            or result["validation_sources_sha256"] != local_source_digest(VALIDATION_SOURCES)
            or result["native_source_sha256"] != native_source_digest()
            or result["native_artifact"]["sha256"] != native_artifact()["sha256"]):
        raise ValueError("source changed during preparation; rerun on the final source")
    commands = {
        "status": "READY_FOR_SERVER_CHECKS" if result["local_ready_for_server_checks"] else "PREPARED_NOT_EXECUTED_BLOCKED_BY_EVIDENCE",
        "prerequisites": result["blockers"],
        "workflow": "local validation -> authorized GitHub push -> server pull -> formal-scale checks -> A/B/C pilot",
        "server_pull": "git pull --ff-only origin main",
        "formal_scale": [f"python tools/submit_slurm.py benchmark --config {configs[arm]['path']} --benchmark-layouts 64:16 "
                         f"--benchmark-output local/runs/preparation/act12-normalstart-{arm.lower()}-r1/benchmark.json"
                         for arm in "ABC"],
        "formal_exact_resume_within_allocated_gpu": [
            f"python tools/preflight_training.py --skip-build --config {configs[arm]['path']} "
            f"--benchmark local/runs/preparation/act12-normalstart-{arm.lower()}-r1/benchmark.json "
            f"--output local/runs/preparation/act12-normalstart-{arm.lower()}-r1/worker-resume.json"
            for arm in "ABC"],
        "pilot": [f"python tools/submit_slurm.py train --prepare --config {configs[arm]['path']} --time 24:00:00"
                  for arm in "ABC"],
        "same_allocation_short_soak": [f"python tools/train_full_run.py --stage train --config {configs[arm]['path']} --stop-after-additional-steps 1015808"
                                       for arm in "ABC"],
        "limitations": ["Run formal-scale benchmark and exact-resume checks before the pilot; local readiness is not CUDA qualification.",
                        "Formal CUDA memory and exact recovery require the server; local CPU checks do not substitute.",
                        "C may stop with NO_ADAPTATION_GAIN; it is not automatically replaced by A.",
                        "20M/40M are not scheduled or automatically promoted."]}
    _json(output / "server-commands.json", commands)
    _json(output / "preparation.json", result)
    return result


def check_ready(output):
    result = json.loads((Path(output) / "preparation.json").read_text(encoding="utf-8"))
    if result["training_implementation_sha256"] != training_implementation_digest():
        raise ValueError("preparation implementation changed")
    if result["native_source_sha256"] != native_source_digest():
        raise ValueError("preparation simulator changed")
    if result["native_artifact"]["sha256"] != native_artifact()["sha256"]:
        raise ValueError("preparation native binary changed")
    if result["validation_sources_sha256"] != local_source_digest(VALIDATION_SOURCES):
        raise ValueError("preparation validation sources changed")
    if sha256_file(Path(result["seed_registry"]["path"])) != result["seed_registry"]["sha256"]:
        raise ValueError("preparation seed registry changed")
    engineering = result["engineering"]
    if engineering["status"] != "PASSED" or sha256_file(Path(engineering["junit_path"])) != engineering["junit_sha256"]:
        raise ValueError("preparation CPU validation missing or changed")
    for entry in result["configs"].values():
        if sha256_file(ROOT / entry["path"]) != entry["sha256"]:
            raise ValueError("prepared config changed")
    if (result.get("parent_cpu_check", {}).get("status") != "BOUNDED_CPU_MIGRATION_VERIFIED"
            or sha256_file(ROOT / result["parent"]["path"]) != result["parent"]["sha256"]):
        raise ValueError("actual parent initialization missing or parent bytes changed")
    if not result.get("local_ready_for_server_checks"):
        raise ValueError("launch blocked: " + ", ".join(result["blockers"]))
    from sls.rl.normalstart_release import release_bindings
    release_bindings(ROOT)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "local/reports/act12-normalstart-study")
    parser.add_argument("--validate", action="store_true")
    parser.add_argument("--check-ready", action="store_true")
    parser.add_argument("--bind-release", action="store_true", help="Bind verified evidence only to unstarted A/B/C configs")
    args = parser.parse_args()
    result = check_ready(args.output) if args.check_ready else prepare(args.output, validate=args.validate, bind_release=args.bind_release)
    print(json.dumps({key: result[key] for key in ("engineering", "simulator", "server", "learning", "launch_ready", "blockers")}, indent=2))
    return int(args.validate and result["engineering"]["status"] != "PASSED")


if __name__ == "__main__":
    raise SystemExit(main())
