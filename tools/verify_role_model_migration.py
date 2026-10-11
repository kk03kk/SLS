"""Verify v5 -> v6 on legal simulator decisions; never train or export weights."""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def verify(checkpoint: Path, *, seed_count: int = 4, max_steps: int = 64) -> dict:
    import torch

    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import IRONCLAD_A20_ACT2
    from sls.model import PolicyBatch
    from sls.model.encoding import content_token, policy_vocabulary
    from sls.rl.checkpoint import policy_from_training_checkpoint
    from sls.rl.model_migration import migrate_v5_role_policy
    from sls.rl.training_contract import (
        native_artifact,
        native_source_digest,
        sha256_file,
    )

    if not 1 <= seed_count <= 32 or not 1 <= max_steps <= 4096:
        raise ValueError("diagnostic sampling budget outside limits")
    torch.set_num_threads(2)
    torch.manual_seed(1729)
    before_hash = sha256_file(checkpoint)
    payload = torch.load(checkpoint, map_location="cpu", weights_only=False)
    old = policy_from_training_checkpoint(payload)
    migrated, migration = migrate_v5_role_policy(payload)
    migrated.eval()
    digest = native_source_digest()
    artifact = native_artifact()
    if not artifact or artifact.get("source_sha256") != digest:
        raise ValueError("diagnostic native binary does not match current source")
    maxima = {field: 0.0 for field in ("logits", "value", "state", "next_memory")}
    screens: dict[str, int] = {}
    decisions = 0
    with torch.no_grad():
        for seed in range(132000000, 132000000 + seed_count):
            backend = SimulatorBackend(IRONCLAD_A20_ACT2)
            decision = backend.reset(seed)
            memories = [model.initial_memory(1) for model in (old, migrated)]
            previous_action = torch.zeros(1, dtype=torch.long)
            previous_reward = torch.zeros(1)
            for _ in range(max_steps):
                batch = PolicyBatch.from_decisions((decision,))
                outputs = [model(*batch.model_inputs(), memory=memory,
                                 previous_action_types=previous_action, previous_rewards=previous_reward)
                           for model, memory in zip((old, migrated), memories, strict=True)]
                for field in maxima:
                    a, b = (getattr(output, field) for output in outputs)
                    torch.testing.assert_close(a, b, atol=1e-5, rtol=1e-5)
                    maxima[field] = max(maxima[field], float((a - b).abs().max()))
                index = int(outputs[0].logits.argmax())
                if index != int(outputs[1].logits.argmax()):
                    raise ValueError("migration changed a greedy action in verification corpus")
                screens[decision.observation.screen.value] = screens.get(decision.observation.screen.value, 0) + 1
                decisions += 1
                transition = backend.step(decision.actions[index])
                if transition.terminated:
                    break
                memories = [output.next_memory for output in outputs]
                previous_action = batch.action_types[:, index] + 1
                previous_reward = torch.tensor([transition.reward])
                decision = transition.decision
    if sha256_file(checkpoint) != before_hash:
        raise ValueError("source checkpoint changed during verification")
    # No optimizer, original-game process, model export or server action.
    _, variant = content_token("NEOW:OPTION:0")
    return {
        "schema": "sls-role-migration-verification-v1", "passed": True,
        "role": "LOCAL_ENCODING_DIAGNOSTIC_NOT_TRAINING_EFFICACY",
        "source_checkpoint_sha256": before_hash, "source_file_unchanged": True,
        "source_native_sha256": payload["contract"]["native_source_sha256"],
        "diagnostic_native_sha256": digest, "native_artifact": artifact,
        "migration": migration, "decisions": decisions, "screen_counts": screens,
        "seed_start": 132000000, "seed_count": seed_count, "max_steps": max_steps,
        "max_absolute_differences": maxima,
        "legacy_option0_content_alias": policy_vocabulary()["content"][variant],
        "model_exported": False, "optimizer_updates": 0,
        "limitations": ["Current-source environment; not a historical win-rate comparison.",
                        "Finite prefix corpus, not full-game qualification or proof of v6 efficacy.",
                        "Migration preserves legacy predictions; role separation must be learned."],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seeds", type=int, default=4)
    parser.add_argument("--max-steps", type=int, default=64)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite evidence")
    root = Path(__file__).resolve().parents[1] / "local" / "reports"
    if not args.output.resolve().is_relative_to(root.resolve()):
        raise ValueError("diagnostic output must be under local/reports")
    report = verify(args.checkpoint, seed_count=args.seeds, max_steps=args.max_steps)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(report, stream, ensure_ascii=False, indent=2)
    print(json.dumps({key: report[key] for key in ("passed", "decisions", "screen_counts", "max_absolute_differences")}))


if __name__ == "__main__":
    main()
