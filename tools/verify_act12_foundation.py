"""Source-bound legal trajectory and continuous checkpoint validation, without training."""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from dataclasses import asdict
from pathlib import Path

import torch

from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import PolicyBatch
from sls.rl.evaluate import evaluate
from sls.rl.training_contract import native_source_digest, training_validation_digest
from sls.runtime import load_policy_artifact


def verify(artifact_path: Path, boundaries: int) -> dict:
    artifact = load_policy_artifact(artifact_path, device="cpu")
    model = artifact.model.eval()
    counts, restore_counts = Counter(), Counter()
    decision_count = restore_count = 0
    seeds = []
    traces = hashlib.sha256()
    with torch.no_grad():
        for seed in range(132200000, 132210000):
            seeds.append(seed)
            backend = SimulatorBackend(IRONCLAD_A20_ACT2)
            decision = backend.reset(seed)
            memory = model.initial_memory(1)
            previous_action = torch.zeros(1, dtype=torch.long)
            previous_reward = torch.zeros(1)
            visits: Counter[str] = Counter()
            # Multiple simultaneous restorations exercise subsequent actions,
            # including leaving the selection boundary, rather than one step.
            branches: list[tuple[SimulatorBackend, int, str]] = []
            for step in range(4096):
                if decision.terminal:
                    break
                signature = json.dumps(decision.observation.to_dict(), sort_keys=True)
                visits[signature] += 1
                if visits[signature] > 4:
                    counts["CYCLE_LIMIT"] += 1
                    break
                screen = decision.observation.screen.value
                key = f"ACT{decision.observation.run.act}:{screen}"
                if backend.checkpoint().get("_policy_multi_selection") is not None:
                    key += ":MULTI_SELECTION"
                counts[key] += 1
                ids = [a.candidate_id for a in decision.actions]
                if len(ids) != len(set(ids)) or set(ids) != set(backend._candidate_bits):
                    raise ValueError("candidate IDs incomplete or ambiguous")
                batch = PolicyBatch.from_decisions((decision,))
                if not all(torch.isfinite(t).all() for t in batch.model_inputs()):
                    raise ValueError("nonfinite policy input")
                output = model(*batch.model_inputs(), memory=memory,
                               previous_action_types=previous_action, previous_rewards=previous_reward)
                if not all(torch.isfinite(t).all() for t in
                           (output.logits, output.value, output.state, output.next_memory)):
                    raise ValueError("nonfinite policy output")
                index = int(output.logits.argmax())
                action = decision.actions[index]
                if step % 40 == 0 or decision.observation.choice_options:
                    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
                    restored_decision = restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint())))
                    if restored_decision != decision or restored.checkpoint() != backend.checkpoint():
                        raise ValueError("checkpoint roundtrip changed decision/state")
                    branches.append((restored, 0, key))
                transition = backend.step(action)
                ongoing = []
                for restored, length, start_key in branches:
                    branch = restored.step(action)
                    if branch != transition or restored.checkpoint() != backend.checkpoint():
                        raise ValueError("continuous restoration diverged")
                    length += 1
                    if length >= 4 or transition.terminated:
                        restore_count += 1
                        restore_counts[start_key] += 1
                    else:
                        ongoing.append((restored, length, start_key))
                branches = ongoing
                traces.update(json.dumps(action.to_dict(), sort_keys=True).encode())
                decision_count += 1
                memory = output.next_memory
                previous_action = batch.action_types[:, index] + 1
                previous_reward = torch.tensor([transition.reward])
                decision = transition.decision
                if transition.terminated:
                    break
            print(json.dumps({"seed": seed, "decisions": decision_count,
                              "continuous_restores": restore_count}), flush=True)
            if decision_count >= boundaries and restore_count >= 100:
                break
    return {"schema": "sls-act12-foundation-boundaries-v1", "native_source_sha256": native_source_digest(),
            "model_sha256": artifact.metadata.model_sha256, "decisions": decision_count,
            "continuous_restores": restore_count, "continuation_length": "4 actions or authoritative terminal",
            "screen_counts": dict(counts), "restore_screen_counts": dict(restore_counts),
            "seeds": seeds, "action_trace_sha256": traces.hexdigest(),
            "scope": "legal deterministic policy trajectories; not exhaustive candidate or stock parity proof",
            "training_updates": 0}


def verify_multi_selection() -> list[dict]:
    """Normal A20 start with guided legal actions; separate from policy canaries."""
    from sls.contracts import ActionKind

    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(344)
    # Neow colorless choice, Purity, first map node: all public legal candidates.
    for _ in range(3):
        decision = backend.step(decision.actions[0]).decision
    for _ in range(20):
        cards = [c for c in decision.observation.hand if c.card_id == "PURITY"]
        if cards:
            decision = backend.step(next(a for a in decision.actions
                                         if a.subject_id == cards[0].instance_id)).decision
            break
        decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.END_TURN)).decision
    else:
        raise ValueError("normal-start Purity selection was not reached")
    decision = backend.step(next(a for a in decision.actions if a.subject_id == "CHOICE:1")).decision
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    restored_decision = restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint())))
    if restored_decision != decision:
        raise ValueError("pending multi-selection roundtrip failed")
    for kind in (ActionKind.SELECT_CARD, ActionKind.CONFIRM, ActionKind.END_TURN, ActionKind.END_TURN):
        action = next(a for a in decision.actions if a.kind is kind)
        left, right = backend.step(action), restored.step(action)
        if left != right or backend.checkpoint() != restored.checkpoint():
            raise ValueError("multi-selection continuous restore failed")
        PolicyBatch.from_decisions((left.decision,))
        decision = left.decision
    return [{"act": 1, "ascension": 20, "seed": 344,
             "source": "NORMAL_START_GUIDED_LEGAL_ACTIONS_NOT_FROZEN_POLICY_CANARY",
             "pending_selected_order": [1], "continuous_actions": 4, "passed": True}]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--boundaries", type=int, default=10000)
    parser.add_argument("--scout", action="store_true")
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("immutable evidence exists")
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("native artifact/source mismatch")
    torch.set_num_threads(2)
    implementation = training_validation_digest()
    if args.scout:
        artifact = load_policy_artifact(args.artifact, device="cpu")
        result = evaluate(artifact.model, IRONCLAD_A20_ACT2,
                          tuple(range(8000011000000, 8000011000128)), max_steps=4096,
                          progress_callback=lambda done, total, decisions:
                          print(json.dumps(dict(done=done, total=total, decisions=decisions)), flush=True))
        report = {"profile": "IRONCLAD_A20_ACT2", "model_sha256": artifact.metadata.model_sha256,
                  "simulator": {"native_source_sha256": native_source_digest()}, "result": asdict(result)}
    else:
        if args.boundaries < 10000:
            raise ValueError("qualification requires at least 10000 boundaries")
        report = verify(args.artifact, args.boundaries)
        report["controlled_multi_selection"] = verify_multi_selection()
    if training_validation_digest() != implementation:
        raise ValueError("implementation changed during validation; rerun final source")
    report["training_validation_sha256"] = implementation
    report["tool_sha256"] = hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
