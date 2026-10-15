"""Capture a normal-start stock key route with public-only policy overrides."""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import deque
from pathlib import Path

from sls.contracts import ActionKind
from sls.curriculum import ironclad_fullrun_profile
from tools.capture_original_card_batch import _write_completion


def key_route_action(decision):
    """Use only policy-visible choices; never inspect the stock diagnostic state."""
    for predicate in (
        lambda a: a.kind is ActionKind.TAKE_BLUE_KEY,
        lambda a: a.kind is ActionKind.TAKE_REWARD and a.reward_id == "reward-key:emerald",
        lambda a: a.kind is ActionKind.RECALL,
    ):
        action = next((a for a in decision.actions if predicate(a)), None)
        if action is not None:
            return action, "PUBLIC_KEY_CHOICE"
    if not decision.observation.run.has_emerald_key:
        nodes = {n.node_id: n for n in decision.observation.map_nodes}
        targets = {n.node_id for n in nodes.values() if n.visible_room_type == "BURNING_ELITE"}
        candidates = {a.node_id: a for a in decision.actions
                      if a.kind is ActionKind.CHOOSE_MAP_NODE}
        queue = deque((node, node) for node in candidates)
        visited = set()
        while targets and queue:
            node, first = queue.popleft()
            if node in visited:
                continue
            visited.add(node)
            if node in targets:
                return candidates[first], "PUBLIC_PATH_TO_BURNING_ELITE"
            if node in nodes:
                queue.extend((child, first) for child in nodes[node].outgoing_node_ids)
    return None, "FROZEN_POLICY_FALLBACK"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seeds", type=int, nargs="+", required=True)
    parser.add_argument("--max-actions", type=int, default=1024)
    args = parser.parse_args()
    if args.output.exists() or not 0 < args.max_actions <= 4096:
        raise ValueError("requires a fresh output and bounded action count")
    if len(set(args.seeds)) != len(args.seeds) or any(s not in range(131213000, 131213010) for s in args.seeds):
        raise ValueError("requires the development key-route seed range")
    for seed in args.seeds:
        if args.output.with_name(f"{args.output.stem}-{seed}.jsonl").exists():
            raise FileExistsError("refuse to overwrite stock key route")
    from sls.backends.original.session import OriginalSession

    session = OriginalSession()
    session.transport.send("ready")
    # Complete the CommunicationMod handshake before importing Torch.
    import torch

    from sls.diagnostics.canary import _boundary_record
    from sls.model.encoding import ACTION_TYPE_IDS
    from sls.runtime import load_policy_artifact
    from tools.capture_act2_stock_policy_system_batch import ValidationPolicyBackend
    from tools.run_original_canary import original_runtime_paths

    torch.set_num_threads(1)
    artifact = load_policy_artifact(args.artifact, device="cpu")
    if artifact.metadata.model_sha256 != "ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c":
        raise ValueError("requires the frozen90 inference artifact")
    session.payload = session.receive_ready()
    _, game = original_runtime_paths(None)
    result = {
        "schema": "sls-fullrun-stock-key-route-v1", "runs": [],
        "purpose": "NORMAL_START_PUBLIC_KEY_OVERRIDE_NOT_PURE_POLICY_OR_WIN_RATE_CERTIFICATE",
        "oracle_mode": "validation", "profile_id": "IRONCLAD_A20_HEART",
        "model_sha256": artifact.metadata.model_sha256,
        "stock_jar_sha256": hashlib.sha256((game / "desktop-1.0.jar").read_bytes()).hexdigest(),
        "capture_tool_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
    }
    try:
        backend = ValidationPolicyBackend(session=session, profile=ironclad_fullrun_profile(20, require_heart=True))
        for seed in args.seeds:
            path = args.output.with_name(f"{args.output.stem}-{seed}.jsonl")
            decision = backend.reset(seed)
            if (decision.observation.run.act, decision.observation.run.floor,
                    decision.observation.screen.value) != (1, 0, "NEOW"):
                raise ValueError("key route must start at normal A20 Neow")
            memory = artifact.model.initial_memory(1, "cpu")
            previous_types = torch.zeros(1, dtype=torch.long)
            previous_rewards = torch.zeros(1, dtype=torch.float32)
            reward, reason, success = 0.0, None, False
            with path.open("x", encoding="utf-8") as stream:
                for index in range(args.max_actions + 1):
                    prediction, policy_action, next_memory = _boundary_record(
                        step=index, decision=decision, reward=reward, reason=reason,
                        success=success, memory=memory, artifact=artifact,
                        previous_action_types=previous_types, previous_rewards=previous_rewards)
                    override, selection = key_route_action(decision)
                    action = override or policy_action
                    record = {
                        "boundary": index, "observation": decision.observation.to_dict(),
                        "actions": [a.to_dict() for a in decision.actions], "terminal": decision.terminal,
                        "requested_action": action.to_dict() if action else None,
                        "policy_prediction": prediction, "selection": selection,
                        "stock_raw": backend.raw_payload,
                        "previous_action_validation_evidence": backend.last_validation_evidence,
                    }
                    stream.write(json.dumps(record, sort_keys=True) + "\n")
                    stream.flush()
                    if action is None or index == args.max_actions:
                        break
                    if action not in decision.actions:
                        raise ValueError("route override is not a legal public candidate")
                    transition = backend.step(action)
                    decision = transition.decision
                    reward = float(transition.reward)
                    reason = str(transition.info.get("reason") or "") or None
                    success = bool(transition.info.get("success"))
                    memory = next_memory
                    previous_types = torch.tensor([ACTION_TYPE_IDS[action.kind.value] + 1], dtype=torch.long)
                    previous_rewards = torch.tensor([reward], dtype=torch.float32)
            run = decision.observation.run
            result["runs"].append({
                "seed": seed, "stock_capture": path.as_posix(),
                "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "boundaries": index + 1,
                "terminal": decision.terminal, "reason": reason, "act": run.act, "floor": run.floor,
                "keys": {"ruby": run.has_ruby_key, "emerald": run.has_emerald_key,
                         "sapphire": run.has_sapphire_key},
            })
            args.output.write_text(json.dumps(result, indent=2) + "\n")
        backend.return_to_menu()
        result["execution_complete"] = True
        args.output.write_text(json.dumps(result, indent=2) + "\n")
        _write_completion(0)
        return 0
    except BaseException as error:
        result["execution_error"] = f"{type(error).__name__}: {error}"
        args.output.write_text(json.dumps(result, indent=2) + "\n")
        _write_completion(1, str(error))
        raise


if __name__ == "__main__":
    raise SystemExit(main())
