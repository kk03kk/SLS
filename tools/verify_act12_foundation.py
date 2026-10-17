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


def verify(artifact_path: Path | None, boundaries: int, *, checkpoint: Path | None = None,
           seed_start: int = 132200000) -> dict:
    migration = None
    if checkpoint:
        from sls.rl.model_migration import migrate_v5_role_policy
        from sls.rl.training_contract import sha256_file

        model, migration = migrate_v5_role_policy(torch.load(checkpoint, map_location="cpu", weights_only=False))
        identity = {"source_checkpoint_sha256": sha256_file(checkpoint), "migration": migration}
    else:
        artifact = load_policy_artifact(artifact_path, device="cpu")
        model, identity = artifact.model, {"model_sha256": artifact.metadata.model_sha256}
    model.eval()
    counts, restore_counts = Counter(), Counter()
    decision_count = restore_count = 0
    seeds = []
    traces = hashlib.sha256()
    witnesses = {}
    with torch.no_grad():
        for seed in range(seed_start, seed_start + 10000):
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
            pending_witnesses = {}
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
                if key not in witnesses and key not in pending_witnesses:
                    pending_witnesses[key] = {
                        'seed':seed,'step':step,'decision':decision,
                        'checkpoint':json.loads(json.dumps(backend.checkpoint())), 'steps':[]}
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
                if not branches and (counts[key] == 1 or step % 40 == 0 or decision.observation.choice_options):
                    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
                    restored_decision = restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint())))
                    if restored_decision != decision or restored.checkpoint() != backend.checkpoint():
                        raise ValueError("checkpoint roundtrip changed decision/state")
                    branches.append((restored, 0, key))
                transition = backend.step(action)
                if pending_witnesses:
                    expected_state = json.loads(json.dumps(backend.checkpoint()))
                    finished = []
                    for witness_key, witness in pending_witnesses.items():
                        witness['steps'].append((action,transition,expected_state))
                        if len(witness['steps']) >= 4 or transition.terminated:
                            witnesses[witness_key] = witness
                            finished.append(witness_key)
                    for witness_key in finished:
                        del pending_witnesses[witness_key]
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
    # With only two live instances, a rare screen can occur while the existing
    # four-action branch is busy. Replay its separately captured continuous
    # witness afterwards; do not waive the screen or count an unfinished branch.
    branches.clear()
    ongoing.clear()
    backend = restored = None
    deferred = []
    for key in counts:
        if not key.startswith('ACT') or restore_counts[key]:
            continue
        witness = witnesses.get(key)
        if witness is None:
            raise ValueError(f'no complete continuous witness for {key}')
        restored = SimulatorBackend(IRONCLAD_A20_ACT2)
        if restored.load_checkpoint(witness['checkpoint']) != witness['decision']:
            raise ValueError('deferred restore changed initial decision')
        if json.loads(json.dumps(restored.checkpoint())) != witness['checkpoint']:
            raise ValueError('deferred restore changed hidden initial state')
        hashes = []
        for action, expected_transition, expected_state in witness['steps']:
            if (restored.step(action) != expected_transition
                    or json.loads(json.dumps(restored.checkpoint())) != expected_state):
                raise ValueError('deferred continuous restoration diverged')
            hashes.append(hashlib.sha256(json.dumps(expected_state,sort_keys=True).encode()).hexdigest())
        restore_count += 1
        restore_counts[key] += 1
        deferred.append({'screen':key,'source_seed':witness['seed'],'source_step':witness['step'],
            'continuous_actions':len(witness['steps']), 'hidden_state_sha256':hashes,
            'actions':[a.to_dict() for a,_,_ in witness['steps']]})
        restored = None
    return {"schema": "sls-act12-foundation-boundaries-v1", "native_source_sha256": native_source_digest(),
            "profile": asdict(IRONCLAD_A20_ACT2), "device": "cpu", "seed_start": seed_start,
            **identity, "policy_architecture": model.config.architecture, "decisions": decision_count,
            "continuous_restores": restore_count, "continuation_length": "4 actions or authoritative terminal",
            "screen_counts": dict(counts), "restore_screen_counts": dict(restore_counts),
            "seeds": seeds, "action_trace_sha256": traces.hexdigest(),
            "deferred_restore_witnesses": deferred,
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
    inputs = parser.add_mutually_exclusive_group(required=True)
    inputs.add_argument("--artifact", type=Path)
    inputs.add_argument("--checkpoint", type=Path, help="Validate the explicitly migrated v6 parent policy")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--boundaries", type=int, default=10000)
    parser.add_argument("--seed-start", type=int, default=132200000)
    parser.add_argument("--scout", action="store_true")
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("immutable evidence exists")
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("native artifact/source mismatch")
    torch.set_num_threads(2)
    implementation = training_validation_digest()
    if args.scout:
        if args.checkpoint:
            raise ValueError("scout requires an inference artifact")
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
        report = verify(args.artifact, args.boundaries, checkpoint=args.checkpoint, seed_start=args.seed_start)
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
