"""Alternate frozen-source CPU stages and fixed-action replay; never run training."""
from __future__ import annotations

import argparse
import ast
import hashlib
import json
import os
import statistics
import subprocess
import sys
import time
from collections.abc import Mapping
from dataclasses import fields, is_dataclass
from enum import Enum
from functools import lru_cache
from pathlib import Path
from typing import Any

os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]


import sls.contracts.observation as observations
import sls.rl.episode_limit as limits_module
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action, Decision
from sls.diagnostics.cpu import (
    PROFILE,
    cpu_runtime,
    decision_from_record,
    decision_record,
    digest,
    identity,
    read_history,
    sha256_file,
    validated_corpus,
    write_json,
)
from sls.model import PolicyBatch, encode_decision
from sls.rl.episode_limit import EpisodeLimitState

OBS = "src/sls/contracts/observation.py"
LIMIT = "src/sls/rl/episode_limit.py"


def source(ref, path):
    if ref.startswith("directory:"):
        return (Path(ref.removeprefix("directory:")) / path).read_text(encoding="utf-8")
    if ref == "working-tree":
        return (ROOT / path).read_text(encoding="utf-8")
    return subprocess.check_output(["git", "show", f"{ref}:{path}"], cwd=ROOT).decode("utf-8")


def functions(source_text):
    """Extract only pure helpers, preserving frozen implementations verbatim."""
    tree = ast.parse(source_text)
    nodes = [node for node in tree.body if isinstance(node, ast.FunctionDef)]
    nodes += [node for node in tree.body if isinstance(node, ast.Assign) and
              any(isinstance(t, ast.Name) and t.id in {
                  "_FORBIDDEN_PUBLIC_FIELDS", "_SORT_JSON_ENCODER", "_PAYLOAD_JSON_ENCODER"}
                  for t in node.targets)]
    namespace = dict(Any=Any, Mapping=Mapping, fields=fields, is_dataclass=is_dataclass,
                     Enum=Enum, lru_cache=lru_cache, json=json, hashlib=hashlib,
                     Decision=Decision, OBSERVATION_SCHEMA_VERSION=observations.OBSERVATION_SCHEMA_VERSION)
    exec(compile(ast.Module(nodes, type_ignores=[]), "<frozen-benchmark-functions>", "exec"), namespace)
    return namespace


def implementations(ref):
    texts = {path: source(ref, path) for path in (OBS, LIMIT)}
    ob, ep = functions(texts[OBS]), functions(texts[LIMIT])
    return {"ob": ob, "ep": ep, "identity": {
        "ref": ref, "sources": {p: hashlib.sha256(t.encode()).hexdigest() for p, t in texts.items()}}}


def activate(implementation):
    observations._json_value = implementation["ob"]["_json_value"]
    observations._assert_public_tree = implementation["ob"]["_assert_public_tree"]
    limits_module.policy_boundary_fingerprint = implementation["ep"]["policy_boundary_fingerprint"]


def tensor_digest(batch):
    digestor = hashlib.sha256()
    for tensor in batch.model_inputs():
        digestor.update(str((tensor.dtype, tuple(tensor.shape))).encode())
        digestor.update(tensor.contiguous().numpy().tobytes())
    return digestor.hexdigest()


def replay(trajectories, verify=False):
    evidence = []
    for trajectory, rows, outcome in trajectories:
        backend = SimulatorBackend(PROFILE)
        decision = backend.reset(trajectory["seed"])
        limiter = EpisodeLimitState.initial(decision)
        for row in rows:
            public = decision_record(decision)
            if verify and public != {k: row[k] for k in ("observation", "actions")}:
                raise AssertionError("fixed-action replay/public boundary mismatch")
            batch = PolicyBatch.from_decisions((decision,))
            if verify:
                restored = SimulatorBackend(PROFILE).load_checkpoint(backend.checkpoint())
                if decision_record(restored) != public:
                    raise AssertionError("checkpoint restore changed boundary")
            transition = backend.step(Action.from_dict(row["chosen_action"]))
            reason = None if transition.terminated or transition.truncated else limiter.observe(
                transition.decision, max_steps=4096, max_boundary_visits=4)
            if verify:
                evidence.append({"public": digest(public), "tensor": tensor_digest(batch),
                                 "limiter": limiter.to_dict(), "limit": reason,
                                 "terminated": transition.terminated, "truncated": transition.truncated,
                                 "reward": transition.reward, "info": dict(transition.info)})
                if row["terminal"] != bool(transition.terminated or transition.truncated or reason):
                    raise AssertionError("replay terminal semantics differ")
            decision = transition.decision
        if verify and decision.observation.to_dict() != outcome["final_observation"]:
            raise AssertionError("final observation mismatch")
    return digest(evidence) if verify else None


def run(corpus, output, baseline_ref, candidate_ref, groups):
    runtime = cpu_runtime()
    manifest = validated_corpus(corpus)
    variants = {"baseline": implementations(baseline_ref), "candidate": implementations(candidate_ref)}
    selected = []
    for state in manifest["states"]:
        trajectory = next(t for t in manifest["trajectories"] if t["id"] == state["trajectory"])
        rows, _ = read_history(corpus / trajectory["public_path"])
        selected.append(decision_from_record(rows[state["step"]]))
    trajectories = []
    for model in manifest["models"]:
        trajectory = next(t for t in manifest["trajectories"] if t["model"] == model)
        rows, outcome = read_history(corpus / trajectory["public_path"])
        trajectories.append((trajectory, rows, outcome))
    encoded = [encode_decision(d) for d in selected]
    # One single-worker batch per state and groups of eight expose real padding.
    chunks = [encoded[i:i+8] for i in range(0, len(encoded), 8)]
    semantic = {}
    for label, variant in variants.items():
        activate(variant)
        boundaries = []
        for d in selected:
            observations.validate_policy_observation(d.observation.to_dict())
            boundaries.append({"public": decision_record(d),
                               "fingerprint": limits_module.policy_boundary_fingerprint(d),
                               "tensor": tensor_digest(PolicyBatch.from_decisions((d,)))})
        semantic[label] = {"boundaries": digest(boundaries), "replay": replay(trajectories, verify=True)}
    if semantic["baseline"] != semantic["candidate"]:
        raise AssertionError("optimization failed semantic acceptance")

    def observe():
        for d in selected:
            observations.validate_policy_observation(d.observation.to_dict())

    def fingerprint():
        for d in selected:
            limits_module.policy_boundary_fingerprint(d)

    def encode():
        for d in selected:
            encode_decision(d)

    def batch():
        for chunk in chunks:
            PolicyBatch.from_encoded(chunk)

    stages = {"observation": observe, "fingerprint": fingerprint, "encoding": encode,
              "batch_assembly": batch, "full_replay": lambda: replay(trajectories)}
    measurements = {label: {s: [] for s in stages} for label in variants}
    orders = []
    for group in range(groups):
        order = ["baseline", "candidate"] if group % 2 == 0 else ["candidate", "baseline"]
        orders.append(order)
        for label in order:
            activate(variants[label])
            for stage, task in stages.items():
                task()  # Equal warmup for both implementations, excluded from timing.
                repeats = 1 if stage == "full_replay" else 4
                start = time.perf_counter_ns()
                for _ in range(repeats):
                    task()
                measurements[label][stage].append((time.perf_counter_ns() - start) / repeats / 1e6)
        print(json.dumps({"benchmark_group": group + 1, "order": order}), flush=True)
    summary = {}
    for stage in stages:
        a = statistics.median(measurements["baseline"][stage])
        b = statistics.median(measurements["candidate"][stage])
        summary[stage] = {"baseline_median_ms": a, "candidate_median_ms": b,
                          "improvement_percent": 100 * (1 - b/a)}
    result = {"schema": "sls-cpu-stage-benchmark-v1", **identity(runtime),
              "corpus_sha256": sha256_file(corpus / "manifest.json"),
              "implementations": {k: v["identity"] for k, v in variants.items()},
              "semantic": semantic, "groups": groups, "orders": orders,
              "measurements_ms": measurements, "summary": summary,
              "fixed_replay_trajectories": [t[0]["id"] for t in trajectories],
              "fixed_replay_decisions": sum(len(t[1]) for t in trajectories),
              "states": len(selected), "entity_counts": [e.entity_count for e in encoded],
              "candidate_counts": [e.action_count for e in encoded],
              "padding": [{"batch_size": len(c),
                           "entity_slots": len(c) * max(e.entity_count for e in c),
                           "entity_used": sum(e.entity_count for e in c),
                           "candidate_slots": len(c) * max(e.action_count for e in c),
                           "candidate_used": sum(e.action_count for e in c)} for c in chunks],
              "scope": "LOCAL_CPU_FIXED_ACTION_BACKEND_ENCODING_BATCH_LIMITER_REPLAY_WITHOUT_POLICY_INFERENCE; NOT_NUS_SPEEDUP"}
    write_json(output, result)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--baseline-ref", default="80bfb37a806c18154ab38f6a845160fbf59634ce")
    parser.add_argument("--candidate-ref", default="working-tree")
    parser.add_argument("--groups", type=int, default=7)
    args = parser.parse_args()
    if args.groups < 7 or args.output.exists():
        parser.error("at least seven groups and a new output path are required")
    run(args.corpus, args.output, args.baseline_ref, args.candidate_ref, args.groups)


if __name__ == "__main__":
    main()
