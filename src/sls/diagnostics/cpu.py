"""CPU-only, public-history diagnostics. No optimizer or training restoration."""
from __future__ import annotations

import gzip
import hashlib
import json
import math
import os
import platform
import statistics
import tomllib
from collections import Counter, defaultdict, deque
from dataclasses import fields
from pathlib import Path

import torch

from sls.contracts import Action, Decision, Observation
from sls.contracts.observation import (
    Card,
    Enemy,
    MapNode,
    Player,
    PublicEntity,
    RunContext,
    ScreenType,
    ShopItem,
    validate_policy_observation,
)
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.rl.checkpoint import policy_from_training_checkpoint
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.reward import curriculum_terminal_reward, shape_curriculum_reward
from sls.rl.training_contract import (
    git_state,
    native_artifact,
    native_source_digest,
    sha256_file,
    training_implementation_digest,
)

CORPUS_SCHEMA = "sls-cpu-natural-corpus-v1"
TRAJECTORY_SCHEMA = "sls-cpu-public-history-v1"
COMPARISON_SCHEMA = "sls-cpu-matched-state-v1"
SEED_START, SEED_COUNT = 132100000, 32
PROFILE = IRONCLAD_A20_ACT2


def cpu_runtime(device: str = "cpu") -> dict:
    if device != "cpu":
        raise ValueError("CPU diagnostics require --device cpu")
    if os.environ.get("CUDA_VISIBLE_DEVICES") != "-1":
        raise RuntimeError("launch with CUDA_VISIBLE_DEVICES=-1 before importing torch")
    torch.set_num_threads(1)
    torch.use_deterministic_algorithms(True)
    return {"device": "cpu", "torch": str(torch.__version__),
            "python": platform.python_version(), "threads": 1,
            "cuda_visible_devices": "-1", "platform": platform.platform()}


def canonical(value) -> bytes:
    return json.dumps(value, sort_keys=True, ensure_ascii=True,
                      separators=(",", ":"), allow_nan=False).encode()


def digest(value) -> str:
    return hashlib.sha256(canonical(value)).hexdigest()


def write_json(path: Path, payload) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8") as stream:
        json.dump(payload, stream, ensure_ascii=False, indent=2, allow_nan=False)
        stream.write("\n")


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def read_history(path: Path) -> tuple[list[dict], dict]:
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        records = [json.loads(line) for line in stream]
    if not records or records[0].get("schema") != TRAJECTORY_SCHEMA:
        raise ValueError("unsupported public history schema")
    rows, outcome = records[1:-1], records[-1]
    if outcome.get("record_type") != "outcome":
        raise ValueError("history is incomplete")
    for i, row in enumerate(rows):
        if row.get("step") != i or row.get("record_type") != "decision":
            raise ValueError("non-contiguous public history")
        validate_policy_observation(row["observation"])
        expected = {f.name for f in fields(Observation)}
        if set(row["observation"]) != expected:
            raise ValueError("public observation fields do not match current contract")
        action = Action.from_dict(row["chosen_action"])
        if action.to_dict() not in [Action.from_dict(a).to_dict() for a in row["actions"]]:
            raise ValueError("history selected an illegal action")
        if i != len(rows) - 1 and row["terminal"]:
            raise ValueError("history crosses an episode boundary")
    return rows, outcome


def decision_record(decision: Decision) -> dict:
    return {"observation": decision.observation.to_dict(),
            "actions": [a.to_dict() for a in decision.actions]}


def decision_from_record(row: dict) -> Decision:
    raw = dict(row["observation"])
    validate_policy_observation(raw)
    raw.pop("schema_version")
    expected = {f.name for f in fields(Observation) if f.init}
    if set(raw) != expected:
        raise ValueError("public observation fields do not match current contract")
    raw["player"], raw["run"] = Player(**raw["player"]), RunContext(**raw["run"])
    raw["screen"] = ScreenType(raw["screen"])
    groups = {Card: ("deck", "hand", "draw_pile", "discard_pile", "exhaust_pile"),
              Enemy: ("enemies",), MapNode: ("map_nodes",), ShopItem: ("shop_items",),
              PublicEntity: ("powers", "relics", "potions", "choice_options",
                             "selected_cards", "reward_options", "event_options",
                             "rest_options", "boss_relic_options")}
    for cls, names in groups.items():
        for name in names:
            values = []
            # Legacy _json_value renders an empty tuple as an empty object.
            for item in raw[name]:
                value = dict(item)
                if "properties" in value:
                    value["properties"] = tuple(sorted(value["properties"].items()))
                if "outgoing_node_ids" in value:
                    value["outgoing_node_ids"] = tuple(value["outgoing_node_ids"])
                values.append(cls(**value))
            raw[name] = tuple(values)
    raw["public_context"] = tuple(sorted(raw["public_context"].items()))
    return Decision(Observation(**raw), tuple(Action.from_dict(a) for a in row["actions"]),
                    not bool(row["actions"]))


def check_seed_collisions(roots: list[Path], start: int, count: int) -> list[str]:
    if not 132000000 <= start < start + count <= 133000000:
        raise ValueError("outside registered local diagnostic namespace")
    hits = []

    def walk(value, path, location=""):
        if isinstance(value, dict):
            if type(value.get("seed")) is int and start <= value["seed"] < start + count:
                hits.append(f"{path}:{location}.seed")
            for key in ("seed_range", "diagnostic_seed_range"):
                span = value.get(key)
                if (isinstance(span, list) and len(span) == 2
                        and all(type(v) is int for v in span)
                        and max(start, span[0]) < min(start + count, span[1])):
                    hits.append(f"{path}:{location}.{key}")
            for prefix in ("seed", "diagnostic_seed", "diagnostic_evaluation_seed",
                           "periodic_evaluation_seed", "final_evaluation_seed"):
                a, n = value.get(prefix + "_start"), value.get(prefix + "_count")
                if (type(a) is int and type(n) is int
                        and max(start, a) < min(start + count, a + n)):
                    hits.append(f"{path}:{location}.{prefix}")
            if type(value.get("start")) is int and type(value.get("end")) is int:
                if max(start, value["start"]) < min(start + count, value["end"]):
                    hits.append(f"{path}:{location}.start/end")
            for k, child in value.items():
                walk(child, path, f"{location}.{k}")
        elif isinstance(value, list):
            for child in value:
                walk(child, path, location)

    scanned = set()
    for root in roots:
        for suffix in ("*.json", "*.toml"):
            for path in root.rglob(suffix):
                if path.resolve() in scanned:
                    continue
                scanned.add(path.resolve())
                try:
                    text = path.read_text(encoding="utf-8")
                    walk(tomllib.loads(text) if path.suffix == ".toml" else json.loads(text), path)
                except (UnicodeError, json.JSONDecodeError, tomllib.TOMLDecodeError) as error:
                    raise ValueError(f"unreadable seed registration {path}") from error
    if hits:
        raise ValueError("diagnostic seed collision: " + "; ".join(hits[:20]))
    return sorted(str(p) for p in scanned)


def load_models(checkpoints: dict[str, Path]) -> dict:
    result = {}
    current = native_source_digest()
    for label, path in checkpoints.items():
        payload = torch.load(path, map_location="cpu", weights_only=False)
        model = policy_from_training_checkpoint(payload, device="cpu")
        if not all(bool(torch.isfinite(v).all()) for v in model.state_dict().values()):
            raise ValueError(f"nonfinite model {label}")
        config = payload["contract"]["ppo"]
        if config["gamma"] != 1 or config["failure_progress_scale"] != 0:
            raise ValueError("diagnostics currently require gamma=1 win objective")
        profile = payload["contract"]["profile"]
        result[label] = {"model": model, "ppo": config,
                         "identity": {"checkpoint": str(path.resolve()),
                                      "sha256": sha256_file(path),
                                      "steps": payload["trainer"]["environment_steps"],
                                      "trained_profile": getattr(profile, "profile_id", None)
                                      or profile["profile_id"],
                                      "native_source_sha256": payload["contract"]["native_source_sha256"],
                                      "diagnostic_environment_migration": current != payload["contract"]["native_source_sha256"],
                                      "horizon_transfer": PROFILE.profile_id != (getattr(profile, "profile_id", None) or profile["profile_id"]),
                                      "transfer": "READ_ONLY_WEIGHTS_NOT_TRAINING_RESUME"}}
        result[label]["identity"]["diagnostic_target"] = {
            "profile": PROFILE.profile_id, "gamma": config["gamma"],
            "potential_shaping": config["potential_shaping"],
            "potential_scale": config["potential_scale"],
            "failure_progress_scale": config["failure_progress_scale"],
            "limit_failure_reward": config["limit_failure_reward"],
            "max_episode_steps": config["max_episode_steps"],
            "max_boundary_visits": config["max_boundary_visits"],
            "policy": "GREEDY_NOT_STOCHASTIC_TRAINING"}
        result[label]["identity"]["training_value_target"] = {
            "formula": "VALUE_PLUS_GAE_WITH_ROLLOUT_BOUNDARY_BOOTSTRAP",
            "policy": "STOCHASTIC_TRAINING_POLICY", "gamma": config["gamma"],
            "gae_lambda": config["gae_lambda"], "rollout_steps": config["rollout_steps"],
            "trained_profile": result[label]["identity"]["trained_profile"]}
    return result


@torch.inference_mode()
def score(model, decision, memory, previous_action=0, previous_reward=0.0, start=False):
    return model(*PolicyBatch.from_decisions((decision,)).model_inputs(), memory=memory,
                 episode_start_mask=torch.tensor([start]),
                 previous_action_types=torch.tensor([previous_action]),
                 previous_rewards=torch.tensor([previous_reward], dtype=torch.float32))


def learning_reward(current, transition, ppo, *, limit=None):
    terminal = transition.terminated or transition.truncated or limit is not None
    reward = float(transition.reward)
    if transition.terminated:
        reward = curriculum_terminal_reward(transition.decision.observation, PROFILE,
                                            success=bool(transition.info.get("success")),
                                            failure_progress_scale=ppo["failure_progress_scale"])
    elif transition.truncated or limit:
        reward = ppo["limit_failure_reward"]
    if ppo["potential_shaping"]:
        reward = shape_curriculum_reward(reward, current.observation,
                                         transition.decision.observation, PROFILE,
                                         gamma=ppo["gamma"], scale=ppo["potential_scale"],
                                         terminal=terminal)
    return reward


def complete_returns(rows: list[dict], outcome: dict) -> list[float] | None:
    if not outcome["complete"]:
        return None
    if not rows or not rows[-1]["terminal"]:
        raise ValueError("complete return requires an actual terminal target")
    total, targets = 0.0, []
    for row in reversed(rows):
        reward = row["shaped_reward"]
        if not math.isfinite(reward):
            raise ValueError("nonfinite return")
        # PPO stores shaped rewards as float32 before constructing its targets.
        total += float(torch.tensor(reward, dtype=torch.float32))
        targets.append(total)
    return targets[::-1]


def stratum(row):
    obs = row["observation"]
    enemies = "+".join(e["monster_id"] for e in obs["enemies"])
    return f"act{obs['run']['act']}:{obs['screen']}:{enemies or '-'}"


def selection_sources(row):
    obs = row["observation"]
    sources = {str(e["properties"].get("source", "unspecified"))
               for group in ("choice_options", "selected_cards") for e in obs[group]}
    # Grid selections reference deck instances directly, rather than choice tokens.
    if any(str(a.get("subject_id", "")).startswith("select-card:") for a in row["actions"]):
        sources.add("MASTER_DECK")
    return sources


def select_states(trajectories: list[dict], directory: Path, maximum=64) -> list[dict]:
    groups = defaultdict(list)
    for trajectory in trajectories:
        rows, _ = read_history(directory / trajectory["public_path"])
        for row in rows:
            key = f"{trajectory['model']}:{trajectory['seed']}:{row['step']}"
            groups[stratum(row)].append({"id": hashlib.sha256(key.encode()).hexdigest(),
                                         "trajectory": trajectory["id"], "step": row["step"],
                                         "stratum": stratum(row), "public_sha256": digest({k: row[k] for k in ("observation", "actions")})})
    for values in groups.values():
        values.sort(key=lambda v: v["id"])
    selected = []
    # Round robin ensures rare screens/encounters precede extra common states.
    keys = sorted(groups, key=lambda k: hashlib.sha256(k.encode()).hexdigest())
    depth = 0
    while len(selected) < maximum:
        added = [groups[k][depth] for k in keys if len(groups[k]) > depth]
        if not added:
            break
        selected.extend(added[:maximum - len(selected)])
        depth += 1
    return sorted(selected, key=lambda v: v["id"])


def identity(runtime):
    return {"runtime": runtime, "git": git_state(), "native": native_artifact(),
            "native_source_sha256": native_source_digest(),
            "implementation_sha256": training_implementation_digest(),
            "profile": PROFILE.profile_id, "claim": "DIAGNOSTIC_NOT_FORMAL_WIN_RATE"}


def capture(directory: Path, models: dict, runtime: dict, *, seed_start=SEED_START,
            seed_count=SEED_COUNT, max_steps=4096, max_states=64, scanned=()):
    from sls.backends.simulator import SimulatorBackend

    environment = identity(runtime)
    if directory.exists():
        raise FileExistsError("refuse to overwrite diagnostic evidence")
    directory.mkdir(parents=True)
    (directory / "public").mkdir()
    trajectories = []
    encountered, selected_encounters = set(), set()
    natural_strata, branch_coverage, choice_sources = Counter(), Counter(), set()
    for label, entry in models.items():
        for seed in range(seed_start, seed_start + seed_count):
            backend = SimulatorBackend(PROFILE)
            decision = backend.reset(seed)
            limits = EpisodeLimitState.initial(decision)
            memory = entry["model"].initial_memory(1, "cpu")
            pa, pr, rows, tail = 0, 0.0, [], deque(maxlen=32)
            key = f"{label}-{seed}"
            public = directory / "public" / f"{key}.jsonl.gz"
            with gzip.open(public, "xt", encoding="utf-8") as stream:
                stream.write(canonical({"schema": TRAJECTORY_SCHEMA, "model": label,
                                         "seed": seed, "record_type": "metadata"}).decode() + "\n")
                reason, complete, success = "diagnostic_step_limit", False, False
                for step in range(max_steps):
                    output = score(entry["model"], decision, memory, pa, pr, step == 0)
                    action = decision.actions[int(output.logits.argmax(1)[0])]
                    transition = backend.step(action)
                    limit = None if transition.terminated or transition.truncated else limits.observe(
                        transition.decision, max_steps=entry["ppo"]["max_episode_steps"],
                        max_boundary_visits=entry["ppo"]["max_boundary_visits"])
                    terminal = transition.terminated or transition.truncated or limit is not None
                    row = {"record_type": "decision", "step": step, **decision_record(decision),
                           "chosen_action": action.to_dict(), "raw_reward": float(transition.reward),
                           "shaped_reward": learning_reward(decision, transition, entry["ppo"], limit=limit),
                           "value": float(output.value[0]), "terminal": terminal}
                    stream.write(canonical(row).decode() + "\n")
                    rows.append(row)
                    observation = row["observation"]
                    natural_strata[stratum(row)] += 1
                    encountered.update(e["monster_id"] for e in observation["enemies"])
                    sources = selection_sources(row)
                    if sources:
                        choice_sources.update(sources)
                        branch_coverage[observation["screen"] + ":" + "+".join(sorted(sources))] += 1
                    tail.append({"step": step, "screen": decision.observation.screen.value,
                                 "floor": decision.observation.run.floor,
                                 "selected_cards": decision.observation.to_dict()["selected_cards"],
                                 "action": action.to_dict()})
                    memory, pa, pr = output.next_memory, ACTION_TYPE_IDS[action.kind.value] + 1, float(transition.reward)
                    decision = transition.decision
                    if terminal:
                        reason = limit or transition.info.get("reason") or "backend_truncated"
                        complete = not transition.truncated
                        success = bool(transition.info.get("success"))
                        break
                outcome = {"record_type": "outcome", "complete": complete, "success": success,
                           "reason": reason, "steps": len(rows),
                           "final_observation": decision.observation.to_dict(),
                           "cycle_tail": list(tail) if reason == "cycle_limit" else []}
                stream.write(canonical(outcome).decode() + "\n")
            trajectories.append({"id": key, "model": label, "seed": seed,
                                 "public_path": public.relative_to(directory).as_posix(),
                                 "sha256": sha256_file(public), "outcome": outcome})
            print(json.dumps({"capture": key, "steps": len(rows), "reason": reason}), flush=True)
    selected = select_states(trajectories, directory, max_states)
    by_trajectory = defaultdict(list)
    for state in selected:
        by_trajectory[state["trajectory"]].append(state)
    for trajectory in trajectories:
        if trajectory["id"] not in by_trajectory:
            continue
        rows, _ = read_history(directory / trajectory["public_path"])
        wanted = {s["step"]: s for s in by_trajectory[trajectory["id"]]}
        backend = SimulatorBackend(PROFILE)
        decision = backend.reset(trajectory["seed"])
        limits = EpisodeLimitState.initial(decision)
        for row in rows[:max(wanted) + 1]:
            if decision_record(decision) != {k: row[k] for k in ("observation", "actions")}:
                raise ValueError("natural native replay differs from recorded public history")
            if row["step"] in wanted:
                selected_encounters.update(e["monster_id"] for e in row["observation"]["enemies"])
                state = wanted[row["step"]]
                target = directory / "private" / f"{state['id']}.json.gz"
                target.parent.mkdir(exist_ok=True)
                with gzip.open(target, "xt", encoding="utf-8") as stream:
                    json.dump({"native": backend.checkpoint(), "limits": limits.to_dict()}, stream)
                state["private_path"] = target.relative_to(directory).as_posix()
                state["private_sha256"] = sha256_file(target)
            transition = backend.step(Action.from_dict(row["chosen_action"]))
            decision = transition.decision
            if not transition.terminated and not transition.truncated:
                limits.observe(decision, max_steps=4096, max_boundary_visits=4)
    required = {"THE_GUARDIAN", "HEXAGHOST", "SLIME_BOSS", "THE_CHAMP", "THE_COLLECTOR", "BRONZE_AUTOMATON"}
    manifest = {"schema": CORPUS_SCHEMA, **environment,
                "seed_range": [seed_start, seed_start + seed_count],
                "seed_scan_files": list(scanned), "models": {k: v["identity"] for k, v in models.items()},
                "selection": "SHA256 round-robin act/screen/enemy strata; natural only",
                "trajectories": trajectories, "states": selected,
                "natural_strata": dict(natural_strata),
                "selection_branch_coverage": dict(branch_coverage),
                "missing_known_choice_sources": sorted({"HAND", "MASTER_DECK", "GENERATED",
                    "DISCARD", "EXHAUST", "DRAW"} - choice_sources),
                "missing_selected_boss_contexts": sorted(required - selected_encounters),
                "missing_boss_contexts": sorted(required - encountered)}
    write_json(directory / "manifest.json", manifest)
    return manifest


def validated_corpus(directory: Path) -> dict:
    manifest = read_json(directory / "manifest.json")
    if manifest.get("schema") != CORPUS_SCHEMA:
        raise ValueError("unsupported corpus schema")
    if manifest["native_source_sha256"] != native_source_digest():
        raise ValueError("corpus native source differs; regenerate, do not silently restore")
    for row in manifest["trajectories"]:
        safe_path(directory, row["public_path"], row["sha256"])
    for row in manifest["states"]:
        safe_path(directory, row["private_path"], row["private_sha256"])
    return manifest


def safe_path(directory, relative, expected):
    path = (directory / relative).resolve()
    if not path.is_relative_to(directory.resolve()) or sha256_file(path) != expected:
        raise ValueError("evidence path or digest mismatch")
    return path


def compare(directory: Path, output: Path, models: dict, runtime: dict, *, max_steps=256):
    from sls.backends.simulator import SimulatorBackend

    manifest = validated_corpus(directory)
    environment = identity(runtime)
    if output.exists():
        raise FileExistsError("refuse to overwrite matched-state results")
    groups = defaultdict(dict)
    for state in manifest["states"]:
        groups[state["trajectory"]][state["step"]] = state
    results, values = [], []
    for trajectory in manifest["trajectories"]:
        wanted = groups.get(trajectory["id"], {})
        if not wanted:
            continue
        rows, outcome = read_history(directory / trajectory["public_path"])
        targets = complete_returns(rows, outcome)
        for label, entry in models.items():
            memory, pa, pr = entry["model"].initial_memory(1, "cpu"), 0, 0.0
            for row in rows[:max(wanted) + 1]:
                decision = decision_from_record(row)
                policy = score(entry["model"], decision, memory, pa, pr, row["step"] == 0)
                prefix_next_memory = policy.next_memory
                if row["step"] in wanted:
                    state = wanted[row["step"]]
                    with gzip.open(directory / state["private_path"], "rt", encoding="utf-8") as stream:
                        private = json.load(stream)
                    backend = SimulatorBackend(PROFILE)
                    restored = backend.load_checkpoint(private["native"])
                    if decision_record(restored) != decision_record(decision):
                        raise ValueError("private checkpoint/public-state alignment failed")
                    if digest(decision_record(decision)) != state["public_sha256"]:
                        raise ValueError("state public digest mismatch")
                    limits = EpisodeLimitState.from_dict(private["limits"])
                    probabilities = policy.logits.softmax(1)[0].tolist()
                    initial = {"state": state["id"], "model": label, "source_model": trajectory["model"],
                               "stratum": state["stratum"], "value_shaped": float(policy.value[0]),
                               "probabilities": probabilities,
                               "actions": [a.to_dict() for a in decision.actions],
                               "memory_history": "CURRENT_MODEL_PUBLIC_BEHAVIOR_PREFIX",
                               "chosen_action": decision.actions[int(policy.logits.argmax(1)[0])].to_dict()}
                    if (targets is not None and label == trajectory["model"] and
                            entry["identity"].get("sha256") == manifest["models"][label].get("sha256")):
                        values.append({"trajectory": trajectory["id"], "step": row["step"],
                                       "act": decision.observation.run.act, "model": label,
                                       "prediction": float(policy.value[0]), "mc_return": targets[row["step"]],
                                       "target_policy": "GREEDY_SOURCE_NOT_STOCHASTIC_TRAINING"})
                    reason, complete, success, total = "diagnostic_step_limit", False, False, 0.0
                    trace = deque(maxlen=32)
                    continuation = decision
                    for step in range(max_steps):
                        action = continuation.actions[int(policy.logits.argmax(1)[0])]
                        transition = backend.step(action)
                        limit = None if transition.terminated or transition.truncated else limits.observe(
                            transition.decision, max_steps=entry["ppo"]["max_episode_steps"],
                            max_boundary_visits=entry["ppo"]["max_boundary_visits"])
                        terminal = transition.terminated or transition.truncated or limit is not None
                        total += float(torch.tensor(learning_reward(continuation, transition, entry["ppo"], limit=limit), dtype=torch.float32))
                        trace.append({"screen": continuation.observation.screen.value,
                                      "floor": continuation.observation.run.floor,
                                      "selected_cards": continuation.observation.to_dict()["selected_cards"],
                                      "action": action.to_dict()})
                        memory_after = policy.next_memory
                        continuation = transition.decision
                        if terminal:
                            reason = limit or transition.info.get("reason") or "backend_truncated"
                            complete, success = not transition.truncated, bool(transition.info.get("success"))
                            break
                        if step + 1 < max_steps:
                            policy = score(entry["model"], continuation, memory_after,
                                           ACTION_TYPE_IDS[action.kind.value] + 1, float(transition.reward))
                    initial["continuation"] = {"steps": step + 1, "complete": complete,
                                               "reason": reason, "success": success,
                                               "shaped_return": total if complete else None,
                                               "final_act": continuation.observation.run.act,
                                               "final_floor": continuation.observation.run.floor,
                                               "final_hp": continuation.observation.player.current_hp,
                                               "cycle_tail": list(trace) if reason == "cycle_limit" else []}
                    results.append(initial)
                    print(json.dumps({"compare": state["id"][:12], "model": label, "reason": reason}), flush=True)
                # Teacher-forced prefix follows the recorded action, not this model's argmax.
                memory, pa, pr = prefix_next_memory, ACTION_TYPE_IDS[Action.from_dict(row["chosen_action"]).kind.value] + 1, float(row["raw_reward"])
    calibrated = {}
    for label in models:
        entries = [v for v in values if v["model"] == label]
        calibrated[label] = {"samples": len(entries),
                             "mse": statistics.mean((v["prediction"] - v["mc_return"]) ** 2 for v in entries) if entries else None}
    result = {"schema": COMPARISON_SCHEMA, **environment,
              "corpus_sha256": sha256_file(directory / "manifest.json"),
              "models": {k: v["identity"] for k, v in models.items()},
              "max_continuation_decisions": max_steps, "states": results,
              "complete_greedy_mc_anchors": values, "mc_summary": calibrated,
              "limitations": "Shared historical behavior prefixes; continuations are conditional diagnostics, not natural win rates. Greedy MC is not stochastic-policy calibration."}
    write_json(output, result)
    return result


def policy_memory_for_prefix(model, decision, memory, pa, pr, action, raw_reward, start):
    # Re-score because continuation inference must never replace prefix memory.
    out = score(model, decision, memory, pa, pr, start)
    return out.next_memory, ACTION_TYPE_IDS[Action.from_dict(action).kind.value] + 1, float(raw_reward)


def analyze_returns(directory: Path, output: Path):
    manifest = validated_corpus(directory)
    rows = []
    for trajectory in manifest["trajectories"]:
        history, outcome = read_history(directory / trajectory["public_path"])
        targets = complete_returns(history, outcome)
        if targets is None:
            rows.append({"trajectory": trajectory["id"], "complete": False, "reason": outcome["reason"]})
            continue
        errors = [(r["value"] - target) ** 2 for r, target in zip(history, targets)]
        rows.append({"trajectory": trajectory["id"], "model": trajectory["model"],
                     "complete": True, "reason": outcome["reason"], "samples": len(history),
                     "initial_prediction": history[0]["value"], "initial_mc_return": targets[0],
                     "mse": statistics.mean(errors),
                     "by_act": {str(act): {"samples": sum(r["observation"]["run"]["act"] == act for r in history),
                                           "mse": statistics.mean(e for r, e in zip(history, errors) if r["observation"]["run"]["act"] == act)}
                                for act in sorted({r["observation"]["run"]["act"] for r in history})}})
    result = {"schema": "sls-cpu-complete-return-v1", "profile": PROFILE.profile_id,
              "target_policy": "GREEDY_CAPTURE_POLICY", "gamma": 1,
              "reward_precision": "TRAINER_FLOAT32_SHAPED_REWARDS_SUMMED_WITHOUT_BOOTSTRAP",
              "limitation": "Not stochastic training-policy calibration or a probability estimate",
              "corpus_sha256": sha256_file(directory / "manifest.json"), "trajectories": rows,
              "outcome_counts": dict(Counter(t["outcome"]["reason"] for t in manifest["trajectories"]))}
    write_json(output, result)
    return result


def verify_greedy(directory: Path, output: Path, models: dict, runtime: dict):
    """Verify one full captured policy trajectory per model after a source change."""
    from sls.backends.simulator import SimulatorBackend

    manifest = validated_corpus(directory)
    results = []
    for label, entry in models.items():
        if entry["identity"]["sha256"] != manifest["models"][label]["sha256"]:
            raise ValueError("greedy equivalence requires the exact captured checkpoint")
        trajectory = next(t for t in manifest["trajectories"] if t["model"] == label)
        rows, outcome = read_history(directory / trajectory["public_path"])
        backend = SimulatorBackend(PROFILE)
        decision = backend.reset(trajectory["seed"])
        limits = EpisodeLimitState.initial(decision)
        memory, pa, pr, deltas = entry["model"].initial_memory(1, "cpu"), 0, 0.0, []
        for row in rows:
            if decision_record(decision) != {k: row[k] for k in ("observation", "actions")}:
                raise ValueError("greedy replay public state/actions differ")
            policy = score(entry["model"], decision, memory, pa, pr, row["step"] == 0)
            action = decision.actions[int(policy.logits.argmax(1)[0])]
            if action.to_dict() != row["chosen_action"]:
                raise ValueError("greedy policy trajectory changed")
            deltas.append(abs(float(policy.value[0]) - row["value"]))
            transition = backend.step(action)
            reason = None if transition.terminated or transition.truncated else limits.observe(
                transition.decision, max_steps=entry["ppo"]["max_episode_steps"],
                max_boundary_visits=entry["ppo"]["max_boundary_visits"])
            if row["terminal"] != bool(transition.terminated or transition.truncated or reason):
                raise ValueError("greedy trajectory termination changed")
            memory, pa, pr = policy.next_memory, ACTION_TYPE_IDS[action.kind.value] + 1, float(transition.reward)
            decision = transition.decision
        if decision.observation.to_dict() != outcome["final_observation"] or max(deltas) > 1e-6:
            raise ValueError("greedy final observation or value changed")
        results.append({"trajectory": trajectory["id"], "decisions": len(rows),
                        "max_value_delta": max(deltas), "reason": outcome["reason"],
                        "limiter": limits.to_dict()})
    result = {"schema": "sls-cpu-greedy-equivalence-v1", **identity(runtime),
              "corpus_sha256": sha256_file(directory / "manifest.json"), "trajectories": results}
    write_json(output, result)
    return result
