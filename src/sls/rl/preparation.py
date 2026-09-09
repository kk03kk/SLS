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


def workload_contract(config: dict) -> str:
    return canonical_digest({
        "profile": asdict(CURRICULUM_PROFILES_BY_ID[config["run"]["profile"]]),
        "model": config["model"], "ppo": config["ppo"],
        "deterministic": config["run"].get("deterministic", True),
    })


def preparation_contract(config: dict, torch_module: object) -> str:
    runtime = runtime_contract(torch_module)
    # Device names and node placement affect throughput, not semantic evidence.
    runtime.pop("cuda_device", None)
    runtime.pop("cuda_device_count", None)
    return canonical_digest({
        "workload": workload_contract(config),
        "native": native_source_digest(),
        "training": training_validation_digest(),
        "tools": local_source_digest((
            "tools/prepare_and_train.py", "tools/preflight_training.py",
            "tools/benchmark_workers.py",
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
            or layout.get("workload_contract") != workload_contract(config)):
        raise ValueError("Act1 preparation is missing or stale; submit train --prepare")
    return report
