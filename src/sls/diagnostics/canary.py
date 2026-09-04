"""Policy-visible recurrent trajectory capture and lock-step comparison."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any, Callable, Mapping, Protocol

import torch

from sls.contracts import Action, Decision, Transition
from sls.model import PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.runtime.artifact import LoadedPolicyArtifact

TRAJECTORY_SCHEMA = "sls-policy-trajectory-v2"
COMPARISON_SCHEMA = "sls-policy-trajectory-comparison-v2"


class PolicyBackend(Protocol):
    def reset(self, seed: int) -> Decision: ...
    def step(self, action: Action) -> Transition: ...


def _json_bytes(value: object) -> bytes:
    return json.dumps(
        value, ensure_ascii=True, sort_keys=True, separators=(",", ":"),
    ).encode("utf-8")


def stable_hash(value: object) -> str:
    return hashlib.sha256(_json_bytes(value)).hexdigest()


def _tensor_bytes(value: torch.Tensor) -> bytes:
    return bytes(value.reshape(-1).view(torch.uint8).tolist())


def tensor_hash(value: torch.Tensor) -> str:
    tensor = value.detach().to(device="cpu", dtype=torch.float32).contiguous()
    return hashlib.sha256(_tensor_bytes(tensor)).hexdigest()


def policy_input_hash(
    batch: PolicyBatch,
    memory: torch.Tensor,
    previous_action_types: torch.Tensor,
    previous_rewards: torch.Tensor,
) -> str:
    digest = hashlib.sha256()
    for tensor in (
        *batch.model_inputs(), memory, previous_action_types, previous_rewards,
    ):
        canonical = tensor.detach().cpu().contiguous()
        digest.update(str(canonical.dtype).encode("ascii"))
        digest.update(_json_bytes(list(canonical.shape)))
        digest.update(_tensor_bytes(canonical))
    return digest.hexdigest()


def _metadata(
    artifact: LoadedPolicyArtifact, *, backend_name: str, seed: int,
) -> dict[str, object]:
    metadata = artifact.metadata
    return {
        "record_type": "metadata",
        "schema": TRAJECTORY_SCHEMA,
        "backend": backend_name,
        "seed": int(seed),
        "policy": {
            "source_git_commit": metadata.source_git_commit,
            "architecture": metadata.model["architecture"],
            "encoding_schema": metadata.encoding_schema,
            "vocabulary_sha256": metadata.vocabulary_sha256,
            "recurrent_memory_size": metadata.recurrent_memory_size,
            "goal": metadata.goal,
        },
        "selection": "DETERMINISTIC_ARGMAX",
        "initial_memory": "ZERO",
        "recurrent_context": "PREVIOUS_ACTION_AND_REWARD",
    }


def _boundary_record(
    *, step: int, decision: Decision, reward: float, reason: str | None,
    success: bool, memory: torch.Tensor, artifact: LoadedPolicyArtifact,
    previous_action_types: torch.Tensor,
    previous_rewards: torch.Tensor,
) -> tuple[dict[str, object], Action | None, torch.Tensor]:
    observation = decision.observation.to_dict()
    actions = sorted((action.to_dict() for action in decision.actions), key=_json_bytes)
    memory_input = tensor_hash(memory)
    base: dict[str, object] = {
        "record_type": "boundary",
        "schema": TRAJECTORY_SCHEMA,
        "step_index": step,
        "screen": decision.observation.screen.value,
        "act": decision.observation.run.act,
        "floor": decision.observation.run.floor,
        "reward_from_previous": float(reward),
        "observation": observation,
        "candidate_actions": actions,
        "observation_sha256": stable_hash(observation),
        "candidate_actions_sha256": stable_hash(actions),
        "memory_input_sha256": memory_input,
        "previous_action_type": int(previous_action_types[0].item()),
        "previous_reward": float(previous_rewards[0].item()),
        "terminal": decision.terminal,
        "terminal_reason": reason,
        "success": bool(success),
    }
    if decision.terminal:
        base.update({
            "chosen_action": None,
            "policy_input_sha256": None,
            "memory_output_sha256": memory_input,
        })
        return base, None, memory
    batch = PolicyBatch.from_decisions((decision,), artifact.model.config).to(memory.device)
    base["policy_input_sha256"] = policy_input_hash(
        batch, memory, previous_action_types, previous_rewards,
    )
    with torch.no_grad():
        output = artifact.model(
            *batch.model_inputs(), memory=memory,
            previous_action_types=previous_action_types,
            previous_rewards=previous_rewards,
        )
    action_index = int(output.logits[0].argmax().item())
    action = decision.actions[action_index]
    next_memory = output.next_memory.detach()
    base.update({
        "chosen_action": action.to_dict(),
        "chosen_action_sha256": stable_hash(action.to_dict()),
        "memory_output_sha256": tensor_hash(next_memory),
        "argmax_logit": float(output.logits[0, action_index].item()),
        "value": float(output.value[0].item()),
    })
    return base, action, next_memory


def capture_policy_trajectory(
    backend: PolicyBackend,
    artifact: LoadedPolicyArtifact,
    *,
    backend_name: str,
    seed: int,
    output: str | Path,
    max_actions: int | None = None,
    journal: str | Path | None = None,
    stop_requested: Callable[[], bool] | None = None,
) -> dict[str, object]:
    """Run one zero-memory argmax policy and durably record every boundary."""

    if max_actions is not None and max_actions <= 0:
        raise ValueError("max_actions must be positive")
    output_path = Path(output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    journal_path = None if journal is None else Path(journal)
    if journal_path is not None:
        journal_path.parent.mkdir(parents=True, exist_ok=True)
    decision = backend.reset(int(seed))
    if not (
        decision.observation.screen.value == "NEOW"
        and decision.observation.run.act == 1
        and decision.observation.run.floor == 0
    ):
        raise RuntimeError("canary must begin at the fresh Neow boundary")
    memory = artifact.model.initial_memory(1, "cpu")
    previous_action_types = torch.zeros(1, dtype=torch.long)
    previous_rewards = torch.zeros(1, dtype=torch.float32)
    reward = 0.0
    reason: str | None = None
    success = False
    actions_taken = 0
    boundaries = 0
    with output_path.open("w", encoding="utf-8") as stream:
        stream.write(json.dumps(_metadata(artifact, backend_name=backend_name, seed=seed)) + "\n")
        stream.flush()
        while True:
            record, action, next_memory = _boundary_record(
                step=boundaries, decision=decision, reward=reward, reason=reason,
                success=success, memory=memory, artifact=artifact,
                previous_action_types=previous_action_types,
                previous_rewards=previous_rewards,
            )
            stream.write(json.dumps(record, sort_keys=True) + "\n")
            stream.flush()
            boundaries += 1
            if action is None or (max_actions is not None and actions_taken >= max_actions):
                break
            if stop_requested is not None and stop_requested():
                break
            intent = {
                "schema": "sls-canary-action-journal-v1", "phase": "INTENT",
                "step_index": boundaries - 1,
                "boundary_sha256": stable_hash({
                    "observation": record["observation"],
                    "candidate_actions": record["candidate_actions"],
                }),
                "chosen_action": action.to_dict(),
                "memory_input_sha256": record["memory_input_sha256"],
                "memory_output_sha256": record["memory_output_sha256"],
                "previous_action_type": record["previous_action_type"],
                "previous_reward": record["previous_reward"],
            }
            if journal_path is not None:
                with journal_path.open("a", encoding="utf-8") as journal_stream:
                    journal_stream.write(json.dumps(intent, sort_keys=True) + "\n")
                    journal_stream.flush()
            transition = backend.step(action)
            decision = transition.decision
            reward = float(transition.reward)
            reason = str(transition.info.get("reason") or "") or None
            success = bool(transition.info.get("success"))
            memory = next_memory
            previous_action_types = torch.tensor(
                [ACTION_TYPE_IDS[action.kind.value] + 1], dtype=torch.long,
            )
            previous_rewards = torch.tensor([reward], dtype=torch.float32)
            actions_taken += 1
            if journal_path is not None:
                ack = {
                    "schema": "sls-canary-action-journal-v1", "phase": "ACK",
                    "step_index": boundaries - 1,
                    "boundary_sha256": intent["boundary_sha256"],
                    "observed_boundary_sha256": stable_hash({
                        "observation": decision.observation.to_dict(),
                        "candidate_actions": sorted(
                            (candidate.to_dict() for candidate in decision.actions),
                            key=_json_bytes,
                        ),
                    }),
                }
                with journal_path.open("a", encoding="utf-8") as journal_stream:
                    journal_stream.write(json.dumps(ack, sort_keys=True) + "\n")
                    journal_stream.flush()
    return {
        "output": str(output_path), "boundaries": boundaries,
        "actions": actions_taken, "terminal": decision.terminal,
        "success": success, "screen": decision.observation.screen.value,
        "act": decision.observation.run.act, "floor": decision.observation.run.floor,
    }


def read_trajectory(path: str | Path) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    records = [
        json.loads(line) for line in Path(path).read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    if not records or records[0].get("record_type") != "metadata":
        raise ValueError("trajectory metadata record is missing")
    if records[0].get("schema") != TRAJECTORY_SCHEMA:
        raise ValueError("unsupported trajectory schema")
    boundaries = records[1:]
    if any(record.get("record_type") != "boundary" for record in boundaries):
        raise ValueError("trajectory contains a non-boundary data record")
    return records[0], boundaries


def _different_paths(left: object, right: object, prefix: str = "") -> list[str]:
    if isinstance(left, Mapping) and isinstance(right, Mapping):
        paths: list[str] = []
        for key in sorted(set(left) | set(right), key=str):
            path = f"{prefix}.{key}" if prefix else str(key)
            if key not in left or key not in right:
                paths.append(path)
            else:
                paths.extend(_different_paths(left[key], right[key], path))
        return paths
    if isinstance(left, list) and isinstance(right, list):
        paths = []
        for index in range(max(len(left), len(right))):
            path = f"{prefix}[{index}]"
            if index >= len(left) or index >= len(right):
                paths.append(path)
            else:
                paths.extend(_different_paths(left[index], right[index], path))
        return paths
    return [] if left == right else [prefix]


def compare_trajectories(
    simulator_path: str | Path, original_path: str | Path,
) -> dict[str, object]:
    sim_meta, sim = read_trajectory(simulator_path)
    original_meta, original = read_trajectory(original_path)
    contract_fields = ("source_git_commit", "architecture", "encoding_schema", "vocabulary_sha256")
    contract_match = all(
        sim_meta["policy"][field] == original_meta["policy"][field]
        for field in contract_fields
    )
    seed_match = sim_meta["seed"] == original_meta["seed"]
    matched = 0
    divergence: dict[str, object] | None = None
    for index in range(min(len(sim), len(original))):
        left, right = sim[index], original[index]
        checks = (
            ("memory_input_sha256", "RECURRENT_MEMORY_DIVERGENCE"),
            ("observation_sha256", "OBSERVATION_DIVERGENCE"),
            ("candidate_actions_sha256", "LEGAL_ACTION_DIVERGENCE"),
            ("chosen_action_sha256", "POLICY_DIVERGENCE"),
            ("memory_output_sha256", "RECURRENT_MEMORY_DIVERGENCE"),
            ("terminal", "EXECUTION_DIVERGENCE"),
            ("success", "EXECUTION_DIVERGENCE"),
        )
        failed = next(((field, kind) for field, kind in checks if left.get(field) != right.get(field)), None)
        if failed is None:
            matched += 1
            continue
        field, kind = failed
        details: dict[str, object] = {"field": field}
        if field == "observation_sha256":
            different_paths = _different_paths(left["observation"], right["observation"])
            details["different_paths"] = different_paths
            if index and sim[index - 1].get("chosen_action") == original[index - 1].get("chosen_action"):
                previous_action = sim[index - 1].get("chosen_action") or {}
                if (
                    previous_action.get("kind") == "SELECT_CARD"
                    and different_paths
                    and all(path.startswith("deck[") and path.endswith(".card_id") for path in different_paths)
                ):
                    kind = "RNG_DIVERGENCE"
                    details["surface_classification"] = "OBSERVATION_DIVERGENCE"
                    details["causal_transition"] = previous_action
        elif field == "candidate_actions_sha256":
            details["simulator_actions"] = left["candidate_actions"]
            details["original_actions"] = right["candidate_actions"]
        divergence = {
            "index": index, "classification": kind, "details": details,
            "simulator": left, "original": right,
            "policy_visible": kind != "BENIGN_PRIVATE_DIFFERENCE",
        }
        break
    if divergence is None and len(sim) != len(original):
        divergence = {
            "index": min(len(sim), len(original)),
            "classification": "EXECUTION_DIVERGENCE",
            "details": {"reason": "trajectory lengths differ"},
            "simulator": sim[min(len(sim), len(original))] if len(sim) > len(original) else None,
            "original": original[min(len(sim), len(original))] if len(original) > len(sim) else None,
            "policy_visible": True,
        }
    bosses = sorted({
        f"ACT_{int(boundary['observation']['run']['act'])}:"
        f"{boundary['observation']['run']['visible_boss_id']}"
        for boundary in sim
        if boundary.get("observation", {}).get("screen") == "COMBAT"
        and boundary.get("observation", {}).get("run", {}).get("visible_boss_id")
        and any(
            enemy.get("monster_id")
            == boundary["observation"]["run"]["visible_boss_id"]
            for enemy in boundary.get("observation", {}).get("enemies", [])
        )
    })
    passed = contract_match and seed_match and divergence is None
    return {
        "schema": COMPARISON_SCHEMA,
        "passed": passed,
        "contract_match": contract_match,
        "seed_match": seed_match,
        "simulator_boundaries": len(sim),
        "original_boundaries": len(original),
        "matched_boundaries": matched,
        "first_divergence": divergence,
        "bosses": bosses,
    }
