"""Read-only qualification of natural resets; not a training sampler."""
from __future__ import annotations

import gzip
import json
import statistics
import time
from collections import Counter

from sls.contracts import Action
from sls.diagnostics.cpu import (
    ACTION_TYPE_IDS,
    PROFILE,
    decision_record,
    digest,
    read_history,
    safe_path,
    score,
    validated_corpus,
    write_json,
)
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.training_contract import git_state, sha256_file


def episode_anchors(states):
    """Earliest available Act2 state per episode, independent of trace length."""
    grouped = {}
    for state in states:
        if not state["stratum"].startswith("act2:"):
            continue
        key = state["trajectory"]
        if key not in grouped or (state["step"], state["id"]) < (grouped[key]["step"], grouped[key]["id"]):
            grouped[key] = state
    return sorted(grouped.values(), key=lambda state: state["id"])


def check_prefix(rows, state):
    step = state["step"]
    if type(step) is not int or not 0 <= step < len(rows):
        raise ValueError("invalid reset boundary")
    if any(row["terminal"] for row in rows[:step]):
        raise ValueError("reset prefix crosses termination")
    public = {key: rows[step][key] for key in ("observation", "actions")}
    if digest(public) != state["public_sha256"]:
        raise ValueError("reset public boundary digest mismatch")
    return public


def qualify_reset(directory, trajectory, state, entry):
    """Replay the actual natural prefix, then independently restore its boundary."""
    from sls.backends.simulator import SimulatorBackend

    rows, _ = read_history(safe_path(directory, trajectory["public_path"], trajectory["sha256"]))
    public = check_prefix(rows, state)
    backend = SimulatorBackend(PROFILE)
    decision = backend.reset(trajectory["seed"])
    limits = EpisodeLimitState.initial(decision)
    model, ppo = entry["model"], entry["ppo"]
    memory = model.initial_memory(1, "cpu")
    previous_action, previous_reward = 0, 0.0
    begin = time.perf_counter()
    for step, row in enumerate(rows[:state["step"]]):
        if decision_record(decision) != {key: row[key] for key in ("observation", "actions")}:
            raise ValueError("natural prefix replay differs")
        policy = score(model, decision, memory, previous_action, previous_reward, step == 0)
        action = Action.from_dict(row["chosen_action"])
        transition = backend.step(action)
        if transition.terminated or transition.truncated:
            raise ValueError("native prefix crosses termination")
        if float(transition.reward) != row["raw_reward"]:
            raise ValueError("public previous reward differs from native replay")
        decision = transition.decision
        if limits.observe(decision, max_steps=ppo["max_episode_steps"],
                          max_boundary_visits=ppo["max_boundary_visits"]) is not None:
            raise ValueError("prefix exhausted episode limits")
        memory = policy.next_memory
        previous_action = ACTION_TYPE_IDS[action.kind.value] + 1
        previous_reward = float(transition.reward)
    prefix_seconds = time.perf_counter() - begin
    with gzip.open(safe_path(directory, state["private_path"], state["private_sha256"]), "rt", encoding="utf-8") as stream:
        private = json.load(stream)
    if digest(backend.checkpoint()) != digest(private["native"]):
        raise ValueError("private native state differs from naturally reached state")
    restored = backend.load_checkpoint(private["native"])
    if decision_record(decision) != public or decision_record(restored) != public:
        raise ValueError("restored boundary differs from naturally reached boundary")
    if limits.to_dict() != private["limits"]:
        raise ValueError("restored episode counters differ from full natural prefix")
    output = score(model, restored, memory, previous_action, previous_reward, state["step"] == 0)
    return {"state": state["id"], "trajectory": trajectory["id"], "seed": trajectory["seed"],
            "prefix_decisions": state["step"], "prefix_cpu_seconds": prefix_seconds,
            "preserved_episode_steps": limits.steps, "boundary_start_mask": state["step"] == 0,
            "boundary_value": float(output.value[0]), "public_sha256": digest(public),
            "recurrent_memory": "CURRENT_MODEL_FULL_PUBLIC_PREFIX_NO_TEACHER_MEMORY",
            "full_private_state_matches_natural_replay": True,
            "natural_replay_and_private_restore_equal": True}


def readiness(directory, output, entry, runtime, maximum=4):
    if output.exists():
        raise FileExistsError("readiness report already exists")
    if not 1 <= maximum <= 64:
        raise ValueError("invalid readiness bounds")
    manifest = validated_corpus(directory)
    anchors = episode_anchors(manifest["states"])
    trajectories = {row["id"]: row for row in manifest["trajectories"]}
    selected = anchors[:maximum]
    checks = [qualify_reset(directory, trajectories[state["trajectory"]], state, entry) for state in selected]
    report = {"schema": "sls-natural-reset-readiness-v1", "git": git_state(), "runtime": runtime,
              "corpus_sha256": sha256_file(directory / "manifest.json"),
              "native_source_sha256": manifest["native_source_sha256"], "model": entry["identity"],
              "claim": "RESEARCH_ONLY_NOT_TRAINING_BANK_OR_WIN_RATE",
              "training_eligible": False,
              "training_blockers": ["Existing diagnostic seeds and states must not enter training",
                                    "No independently registered training-only corpus collected",
                                    "Production sampler and matched experiment not implemented"],
              "selected_act2_states": sum(state["stratum"].startswith("act2:") for state in manifest["states"]),
              "available_episode_anchors": len(anchors),
              "unique_act2_seeds": len({trajectories[state["trajectory"]]["seed"] for state in anchors}),
              "anchor_strata": dict(Counter(state["stratum"] for state in anchors)),
              "prefix_decisions_median": statistics.median(state["step"] for state in anchors) if anchors else None,
              "checks": checks,
              "limitations": ["Earliest selected anchor need not be the Act2 entry",
                              "Different teachers on the same seed are correlated",
                              "CPU prefix measurements include native replay and encoding; not NUS benchmarks",
                              "Prefix replay is extra compute and excluded from student PPO samples",
                              "Natural-start retention must be evaluated separately"]}
    write_json(output, report)
    return report
