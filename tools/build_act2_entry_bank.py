"""Capture and independently replay normal-start legal prefixes to real Act2 entries."""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
from collections import Counter
from pathlib import Path

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import PolicyBatch
from sls.rl.start_curriculum import SCHEMA
from sls.rl.training_contract import native_source_digest
from sls.runtime import load_policy_artifact


def verify_entries(bank):
    for row in bank["entries"]:
        backend = SimulatorBackend(IRONCLAD_A20_ACT2)
        decision = backend.reset(row["seed"])
        for candidate in row["actions"]:
            action = next((a for a in decision.actions if a.candidate_id == candidate), None)
            if action is None or decision.terminal or decision.observation.run.act != 1:
                raise ValueError("entry prefix has an illegal action or passed the entry")
            decision = backend.step(action).decision
        if (decision.terminal or decision.observation.run.act != 2
                or decision.observation.screen.value != "MAP"
                or json.dumps(backend.checkpoint(), sort_keys=True, allow_nan=False)
                != json.dumps(row["checkpoint"], sort_keys=True, allow_nan=False)):
            raise ValueError(f"entry checkpoint differs from independent legal replay: seed={row['seed']}")


def revalidate_bank(path: Path) -> dict:
    """Create new evidence after replaying every original prefix; retain old bytes.

    No different checkpoints are accepted. This supports an independently
    verified source change outside the captured successful prefixes, rather
    than overwriting their historical generating identity.
    """
    raw = path.read_bytes()
    bank = json.loads(gzip.decompress(raw))
    source = native_source_digest()
    if bank.get("schema") != SCHEMA:
        raise ValueError("unknown bank schema")
    verify_entries(bank)
    if native_source_digest() != source:
        raise ValueError("source changed during bank revalidation")
    return {**bank, "native_source_sha256": source,
            "generator_native_source_sha256": bank.get(
                "generator_native_source_sha256", bank["native_source_sha256"]),
            "revalidation": {"parent_bank_sha256": hashlib.sha256(raw).hexdigest(),
                             "parent_native_source_sha256": bank["native_source_sha256"],
                             "native_source_sha256": source,
                             "entries_replayed": len(bank["entries"]),
                             "checkpoints_changed": 0,
                             "outcome_counts_source": "ORIGINAL_GENERATION_NOT_REMEASURED"}}


def build_serial(args):
    artifact = load_policy_artifact(args.artifact, device=args.device)
    model = artifact.model.eval()
    entries, outcomes = [], Counter()
    source = native_source_digest()
    with torch.no_grad():
        for seed in range(args.seed_start, args.seed_start + args.seeds):
            backend = SimulatorBackend(IRONCLAD_A20_ACT2)
            decision = backend.reset(seed)
            memory = model.initial_memory(1, args.device)
            previous_action = torch.zeros(1, dtype=torch.long, device=args.device)
            previous_reward = torch.zeros(1, device=args.device)
            actions, visits = [], Counter()
            for _ in range(4096):
                if decision.terminal:
                    outcomes["terminal_before_entry"] += 1
                    break
                if decision.observation.run.act == 2:
                    if decision.observation.screen.value != "MAP":
                        raise ValueError("first Act2 boundary is not map")
                    entries.append({"seed": seed, "actions": actions,
                                    "checkpoint": backend.checkpoint()})
                    outcomes["reached_entry"] += 1
                    break
                signature = json.dumps(decision.observation.to_dict(), sort_keys=True)
                visits[signature] += 1
                if visits[signature] > 4:
                    outcomes["cycle_limit"] += 1
                    break
                batch = PolicyBatch.from_decisions((decision,)).to(args.device)
                output = model(*batch.model_inputs(), memory=memory,
                               previous_action_types=previous_action, previous_rewards=previous_reward)
                index = int(output.logits.argmax())
                action = decision.actions[index]
                actions.append(action.candidate_id)
                transition = backend.step(action)
                memory = output.next_memory
                previous_action = batch.action_types[:, index] + 1
                previous_reward = torch.tensor([transition.reward], device=args.device)
                decision = transition.decision
            else:
                outcomes["step_limit"] += 1
            if (seed - args.seed_start + 1) % 32 == 0:
                print(json.dumps({"seeds": seed - args.seed_start + 1, "outcomes": dict(outcomes)}), flush=True)
    bank = {"schema": SCHEMA, "profile": IRONCLAD_A20_ACT2.profile_id,
            "native_source_sha256": source, "model_sha256": artifact.metadata.model_sha256,
            "seed_start": args.seed_start, "seed_count": args.seeds,
            "selection": "ALL_REACHED_ENTRIES_NO_OUTCOME_FILTER",
            "policy_memory": "RESET_AT_NEW_EPISODE", "generator_policy": "GREEDY_FROZEN_90M",
            "generator_device": args.device,
            "outcomes": dict(outcomes), "entries": entries}
    if not entries:
        raise ValueError("no real Act2 entries captured")
    verify_entries(bank)
    if native_source_digest() != source:
        raise ValueError("source changed while building entry bank")
    return bank


def build(args):
    if args.batch_size == 1:
        return build_serial(args)
    artifact = load_policy_artifact(args.artifact, device=args.device)
    model = artifact.model.eval()
    entries, outcomes = [], Counter()
    source = native_source_digest()
    with torch.no_grad():
        for start in range(args.seed_start, args.seed_start + args.seeds, args.batch_size):
            seeds = list(range(start, min(start + args.batch_size, args.seed_start + args.seeds)))
            backends = [SimulatorBackend(IRONCLAD_A20_ACT2) for _ in seeds]
            decisions = [backend.reset(seed) for backend, seed in zip(backends, seeds)]
            memory = model.initial_memory(len(seeds), args.device)
            previous_actions = torch.zeros(len(seeds), dtype=torch.long, device=args.device)
            previous_rewards = torch.zeros(len(seeds), device=args.device)
            prefixes, visits = [[] for _ in seeds], [Counter() for _ in seeds]
            active = list(range(len(seeds)))
            for _ in range(4096):
                remaining = []
                for i in active:
                    decision = decisions[i]
                    if decision.terminal:
                        outcomes["terminal_before_entry"] += 1
                    elif decision.observation.run.act == 2:
                        if decision.observation.screen.value != "MAP":
                            raise ValueError("first Act2 boundary is not map")
                        entries.append({"seed": seeds[i], "actions": prefixes[i],
                                        "checkpoint": backends[i].checkpoint()})
                        outcomes["reached_entry"] += 1
                    else:
                        signature = json.dumps(decision.observation.to_dict(), sort_keys=True)
                        visits[i][signature] += 1
                        if visits[i][signature] > 4:
                            outcomes["cycle_limit"] += 1
                        else:
                            remaining.append(i)
                active = remaining
                if not active:
                    break
                batch = PolicyBatch.from_decisions(tuple(decisions[i] for i in active)).to(args.device)
                output = model(*batch.model_inputs(), memory=memory[active],
                               previous_action_types=previous_actions[active],
                               previous_rewards=previous_rewards[active])
                indices = output.logits.argmax(dim=-1).cpu().tolist()
                memory[active] = output.next_memory
                for position, (i, index) in enumerate(zip(active, indices)):
                    action = decisions[i].actions[index]
                    prefixes[i].append(action.candidate_id)
                    transition = backends[i].step(action)
                    decisions[i] = transition.decision
                    previous_actions[i] = batch.action_types[position, index] + 1
                    previous_rewards[i] = transition.reward
            else:
                outcomes["step_limit"] += len(active)
            print(json.dumps({"seeds": start - args.seed_start + len(seeds),
                              "outcomes": dict(outcomes)}), flush=True)
    bank = {"schema": SCHEMA, "profile": IRONCLAD_A20_ACT2.profile_id,
            "native_source_sha256": source, "model_sha256": artifact.metadata.model_sha256,
            "seed_start": args.seed_start, "seed_count": args.seeds,
            "selection": "ALL_REACHED_ENTRIES_NO_OUTCOME_FILTER",
            "policy_memory": "RESET_AT_NEW_EPISODE", "generator_policy": "GREEDY_FROZEN_90M",
            "generator_device": args.device, "generator_batch_size": args.batch_size,
            "outcomes": dict(outcomes), "entries": sorted(entries, key=lambda row: row["seed"])}
    if not entries or sum(outcomes.values()) != args.seeds:
        raise ValueError("entry collection missing seed outcomes")
    verify_entries(bank)
    if native_source_digest() != source:
        raise ValueError("source changed while building entry bank")
    return bank


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seed-start", type=int, default=132500000)
    parser.add_argument("--seeds", type=int, default=2048)
    parser.add_argument("--verify-only", action="store_true")
    parser.add_argument("--replay-from", type=Path,
                        help="Write a new source-bound bank after exact replay; never edit the parent")
    parser.add_argument("--device", default="cpu")
    parser.add_argument("--batch-size", type=int, default=1)
    args = parser.parse_args()
    torch.set_num_threads(2)
    if args.verify_only and args.replay_from:
        raise ValueError("verification and new revalidation are distinct operations")
    if args.verify_only:
        raw = args.output.read_bytes()
        bank = json.loads(gzip.decompress(raw))
        if bank["native_source_sha256"] != native_source_digest():
            raise ValueError("stale entry bank")
        verify_entries(bank)
    else:
        if (args.artifact is None and args.replay_from is None) or args.output.exists() or args.seeds <= 0 or args.batch_size <= 0:
            raise ValueError("artifact, positive seed count and fresh output required")
        if not 0 <= args.seed_start < args.seed_start + args.seeds <= 2_000_000_000_000:
            raise ValueError("entry bank seeds must stay outside held-out namespaces")
        bank = revalidate_bank(args.replay_from) if args.replay_from else build(args)
        raw = gzip.compress(json.dumps(bank, sort_keys=True, separators=(",", ":")).encode(), mtime=0)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_bytes(raw)
    print(json.dumps({"bank_sha256": hashlib.sha256(raw).hexdigest(),
                      "entries": len(bank["entries"]), "independent_legal_replay": "PASS",
                      "native_source_sha256": bank["native_source_sha256"]}), flush=True)


if __name__ == "__main__":
    main()
