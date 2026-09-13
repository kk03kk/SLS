"""Semantic preparation evidence shared by launch and training entry points."""

from __future__ import annotations

import json
import tomllib
from dataclasses import asdict
from pathlib import Path

from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.rl.training_contract import (
    ROOT,
    canonical_digest,
    local_source_digest,
    native_source_digest,
    runtime_contract,
    training_validation_digest,
)


def read_config(path: Path) -> dict:
    return tomllib.loads(path.read_text(encoding="utf-8"))


def training_seed_limit(run: dict) -> int:
    """Keep earlier held-out ranges excluded when a continuation adds new ones."""
    limit = min(int(run["periodic_evaluation_seed_start"]),
                int(run["final_evaluation_seed_start"]),
                int(run.get("diagnostic_evaluation_seed_start", run["periodic_evaluation_seed_start"])),
                int(run.get("training_seed_limit", run["periodic_evaluation_seed_start"])))
    if limit <= int(run["seed"]):
        raise ValueError("training seed limit must exceed the initial training seed")
    return limit


def workload_contract(config: dict) -> str:
    return canonical_digest({
        "profile": asdict(CURRICULUM_PROFILES_BY_ID[config["run"]["profile"]]),
        "model": config["model"], "ppo": config["ppo"],
        "deterministic": config["run"].get("deterministic", True),
    })


def benchmark_matches_workload(config: dict, layout: dict) -> bool:
    if layout.get("workload_contract") == workload_contract(config):
        return True
    # Changing Adam's scalar LR does not change tensor shapes or update work.
    # Retain the measured report verbatim; never relabel it as a fresh benchmark.
    parent = config["run"].get("continuation_from")
    visited: set[Path] = set()
    while parent:
        path = (ROOT / parent / "training-config.toml").resolve()
        if path in visited:
            return False
        visited.add(path)
        original = read_config(path)
        comparable = {**config, "ppo": {**config["ppo"],
                                      "learning_rate": original["ppo"]["learning_rate"]}}
        if workload_contract(comparable) != workload_contract(original):
            return False
        if layout.get("workload_contract") == workload_contract(original):
            return True
        parent = original["run"].get("continuation_from")
    return False


def preparation_contract(config: dict, torch_module: object) -> str:
    runtime = runtime_contract(torch_module)
    # Device names and node placement affect throughput, not semantic evidence.
    runtime.pop("cuda_device", None)
    runtime.pop("cuda_device_count", None)
    return canonical_digest({
        "workload": workload_contract(config),
        "native": native_source_digest(),
        "training": training_validation_digest(),
        "continuation_parent": config["run"].get("continuation_checkpoint_sha256"),
        **({"warm_start": config["warm_start"]} if "warm_start" in config else {}),
        "tools": local_source_digest((
            "tools/prepare_and_train.py", "tools/preflight_training.py",
            "tools/benchmark_workers.py",
            "tools/initialize_act1_continuation.py",
        )),
        "runtime": runtime,
    })


def require_preparation(config: dict, torch_module: object) -> dict:
    benchmark = ROOT / config["run"]["benchmark"]
    report = json.loads(benchmark.with_name("ready.json").read_text(encoding="utf-8"))
    layout = json.loads(benchmark.read_text(encoding="utf-8"))
    if (report.get("ok") is not True
            or report.get("contract") != preparation_contract(config, torch_module)
            or report.get("layout") != [layout["selected_workers"], layout["selected_shards"]]
            or not benchmark_matches_workload(config, layout)):
        raise ValueError("Act1 preparation is missing or stale; submit train --prepare")
    return report
