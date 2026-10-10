"""Condition on identical natural combat states; stop at the battle boundary."""
from __future__ import annotations

import gzip
import json
from collections import defaultdict

from sls.contracts import Action, ActionKind, ScreenType
from sls.diagnostics.cpu import (
    PROFILE,
    decision_from_record,
    decision_record,
    digest,
    identity,
    read_history,
    score,
    validated_corpus,
    write_json,
)
from sls.model.encoding import ACTION_TYPE_IDS
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.training_contract import ROOT, source_sha256


def battle_outcome(transition, limit, escaped):
    """Do not confuse diagnostic censoring, escape, or a battle exit with run success."""
    if transition.truncated:
        return "BACKEND_TRUNCATED"
    if transition.terminated:
        return "RUN_SUCCESS" if transition.info.get("success") else "DEATH"
    if limit:
        return limit.upper()
    if transition.decision.observation.screen is not ScreenType.COMBAT:
        return "ESCAPED_BATTLE" if escaped else "SURVIVED_BATTLE_EXIT"
    return None


def select_battle_states(states, count):
    eligible = [s for s in states if s["stratum"].startswith("act2:COMBAT:")]
    bosses = [s for s in eligible if any(b in s["stratum"].split(":")[-1].split("+")
              for b in ("THE_COLLECTOR", "BRONZE_AUTOMATON", "THE_CHAMP"))]
    boss_ids = {s["id"] for s in bosses}
    ordered = sorted(bosses, key=lambda s: s["id"]) + sorted(
        (s for s in eligible if s["id"] not in boss_ids), key=lambda s: s["id"])
    return ordered[:count]


def probe_battles(directory, output, models, runtime, *, count=12, max_steps=256):
    from sls.backends.simulator import SimulatorBackend

    manifest = validated_corpus(directory)
    environment = identity(runtime)
    environment["diagnostic_source_sha256"] = {p: source_sha256(ROOT / p) for p in (
        "src/sls/diagnostics/battle_probe.py", "src/sls/diagnostics/cpu.py", "tools/probe_same_battle.py")}
    selected = select_battle_states(manifest["states"], count)
    groups = defaultdict(dict)
    for state in selected:
        groups[state["trajectory"]][state["step"]] = state
    results = []
    for trajectory in manifest["trajectories"]:
        wanted = groups.get(trajectory["id"])
        if not wanted:
            continue
        rows, _ = read_history(directory / trajectory["public_path"])
        for label, entry in models.items():
            model = entry["model"]
            memory, pa, pr = model.initial_memory(1, "cpu"), 0, 0.
            for row in rows[:max(wanted) + 1]:
                decision = decision_from_record(row)
                policy = score(model, decision, memory, pa, pr, row["step"] == 0)
                prefix_memory = policy.next_memory
                if row["step"] in wanted:
                    state = wanted[row["step"]]
                    with gzip.open(directory / state["private_path"], "rt", encoding="utf-8") as stream:
                        private = json.load(stream)
                    backend = SimulatorBackend(PROFILE)
                    current = backend.load_checkpoint(private["native"])
                    if decision_record(current) != decision_record(decision) or digest(decision_record(current)) != state["public_sha256"]:
                        raise ValueError("battle public/private state mismatch")
                    limits = EpisodeLimitState.from_dict(private["limits"])
                    initial = {"state": state["id"], "model": label, "stratum": state["stratum"],
                               "source_model": trajectory["model"], "public_sha256": state["public_sha256"],
                               "history_steps": row["step"], "initial_hp": current.observation.player.current_hp,
                               "initial_value_shaped": float(policy.value[0]),
                               "initial_probabilities": policy.logits.softmax(1)[0].tolist()}
                    trace, escaped, reason = [], False, "DIAGNOSTIC_STEP_LIMIT"
                    for _ in range(max_steps):
                        action = current.actions[int(policy.logits.argmax(1)[0])]
                        escaped |= action.kind is ActionKind.USE_POTION and any(
                            p.instance_id == action.subject_id and p.content_id == "SMOKE_BOMB"
                            for p in current.observation.potions)
                        trace.append({**decision_record(current), "chosen_action": action.to_dict(),
                                      "value_shaped": float(policy.value[0])})
                        transition = backend.step(action)
                        limit = None if transition.terminated or transition.truncated else limits.observe(
                            transition.decision, max_steps=entry["ppo"]["max_episode_steps"],
                            max_boundary_visits=entry["ppo"]["max_boundary_visits"])
                        current = transition.decision
                        boundary = battle_outcome(transition, limit, escaped)
                        if boundary:
                            reason = boundary
                            break
                        policy = score(model, current, policy.next_memory,
                                       ACTION_TYPE_IDS[action.kind.value] + 1, float(transition.reward))
                    initial.update(reason=reason, completed=reason not in ("DIAGNOSTIC_STEP_LIMIT", "BACKEND_TRUNCATED"),
                                   decisions=len(trace), final_hp=current.observation.player.current_hp,
                                   final_screen=current.observation.screen.value,
                                   run_success=bool(transition.info.get("success")), trace=trace)
                    results.append(initial)
                    print(json.dumps({"battle": state["id"][:12], "model": label, "reason": reason}), flush=True)
                memory, pa, pr = prefix_memory, ACTION_TYPE_IDS[Action.from_dict(row["chosen_action"]).kind.value] + 1, float(row["raw_reward"])
    report = {"schema": "sls-natural-same-battle-v1", **environment,
              "corpus_native_source_sha256": manifest["native_source_sha256"],
              "models": {label: entry["identity"] for label, entry in models.items()},
              "selection": "Act2 combat: all available boss states first, then fixed state SHA256 order",
              "selected_states": selected, "max_decisions": max_steps, "results": results,
              "limitations": ["Research corpus and selected finite states, not formal win rates or independent episodes.",
                              "SURVIVED_BATTLE_EXIT is a battle boundary, not a completed Act2 run.",
                              "HP at exit can include post-battle healing; it is not damage taken.",
                              "Values retain full-run shaped-return targets; battle exit has no MC calibration target."]}
    write_json(output, report)
    return report
