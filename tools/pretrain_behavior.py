"""Behavior-clone the relational policy from a generated teacher corpus."""

from __future__ import annotations

import argparse
from pathlib import Path
import random
import sys
from typing import Iterable, Mapping

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend  # noqa: E402
from sls.model import ModelConfig, Policy, PolicyBatch, encode_decision  # noqa: E402
from sls.rl.demonstrations import load_teacher_corpus  # noqa: E402
from sls.runtime import save_policy_artifact  # noqa: E402
from sls.model.encoding import vocabulary_hash  # noqa: E402
from sls.rl.training_contract import (  # noqa: E402
    canonical_digest, git_state, native_source_digest,
)
from sls.rl.training_mode import TrainingMode  # noqa: E402


def _encoded_examples(examples: Iterable[Mapping[str, object]]):  # type: ignore[no-untyped-def]
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    for example in examples:
        decision = backend.load_checkpoint(example["checkpoint"])  # type: ignore[arg-type]
        candidate_id = str(example["candidate_id"])
        matches = [
            index for index, action in enumerate(decision.actions)
            if action.candidate_id == candidate_id
        ]
        if len(matches) != 1:
            raise ValueError("teacher label is not uniquely legal at its checkpoint")
        yield encode_decision(decision), matches[0]


@torch.no_grad()
def _accuracy(model: Policy, examples, device: str, batch_size: int) -> float:  # type: ignore[no-untyped-def]
    model.eval()
    correct = total = 0
    rows = list(_encoded_examples(examples))
    for start in range(0, len(rows), batch_size):
        chunk = rows[start:start + batch_size]
        batch = PolicyBatch.from_encoded([item[0] for item in chunk]).to(device)
        predictions = model(*batch.model_inputs()).logits.argmax(dim=1).cpu().tolist()
        correct += sum(int(prediction == item[1]) for prediction, item in zip(predictions, chunk))
        total += len(chunk)
    model.train()
    return correct / max(1, total)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("corpus", type=Path)
    parser.add_argument("--validation-corpus", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--artifact-output", type=Path, required=True)
    parser.add_argument("--device", default="cuda")
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--batch-size", type=int, default=128)
    args = parser.parse_args()
    training = load_teacher_corpus(args.corpus)
    validation = load_teacher_corpus(args.validation_corpus)
    training_seeds = set(range(int(training["seed_start"]), int(training["seed_start"]) + int(training["seed_count"])))
    validation_seeds = set(range(int(validation["seed_start"]), int(validation["seed_start"]) + int(validation["seed_count"])))
    if training_seeds & validation_seeds:
        raise ValueError("behavior-cloning train and validation seeds overlap")
    examples = list(training["examples"])
    validation_examples = list(validation["examples"])
    model = Policy(ModelConfig()).to(args.device)
    baseline_accuracy = _accuracy(
        model, validation_examples, args.device, args.batch_size,
    )
    optimizer = torch.optim.AdamW(model.parameters(), lr=3e-4)
    generator = random.Random(0)
    model.train()
    for epoch in range(args.epochs):
        generator.shuffle(examples)
        total = 0.0
        batches = 0
        for start in range(0, len(examples), args.batch_size):
            rows = list(_encoded_examples(examples[start:start + args.batch_size]))
            encoded = [item[0] for item in rows]
            labels = [item[1] for item in rows]
            batch = PolicyBatch.from_encoded(encoded).to(args.device)
            output = model(*batch.model_inputs())
            loss = torch.nn.functional.cross_entropy(
                output.logits, torch.tensor(labels, device=args.device),
            )
            optimizer.zero_grad(set_to_none=True)
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 0.5)
            optimizer.step()
            total += float(loss.detach())
            batches += 1
        print({"epoch": epoch + 1, "loss": total / max(1, batches)})
    validation_accuracy = _accuracy(
        model, validation_examples, args.device, args.batch_size,
    )
    if validation_accuracy < baseline_accuracy + 0.05:
        raise RuntimeError(
            "behavior cloning did not improve held-out accuracy by at least 5 points: "
            f"baseline={baseline_accuracy:.4f}, trained={validation_accuracy:.4f}"
        )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    provenance = {
        "training_mode": TrainingMode.EXPERIMENTAL.value,
        "policy_transfer_verified": False,
        "git_commit": str(git_state()["commit"]),
        "native_source_sha256": native_source_digest(),
        "encoding_schema": model.config.to_dict()["encoding_schema"],
        "vocabulary_sha256": vocabulary_hash(),
        "training_config_sha256": canonical_digest({
            "epochs": args.epochs, "batch_size": args.batch_size,
            "model": model.config.to_dict(),
        }),
        "teacher_corpus_sha256": training["corpus_sha256"],
        "validation_corpus_sha256": validation["corpus_sha256"],
        "teacher_successes": int(training["teacher_successes"]),
        "rejected_labels": int(training["rejected_labels"]),
        "teacher_examples": len(examples),
    }
    torch.save({
        "schema": "sls-behavior-pretrain-v1", "model_config": model.config.to_dict(),
        "model": model.state_dict(), "corpus": str(args.corpus.resolve()),
        "validation_corpus": str(args.validation_corpus.resolve()),
        "baseline_validation_accuracy": baseline_accuracy,
        "validation_accuracy": validation_accuracy,
        "provenance": provenance,
    }, args.output)
    args.artifact_output.parent.mkdir(parents=True, exist_ok=True)
    save_policy_artifact(
        model, args.artifact_output, ascension_min=0, ascension_max=0, goal="ACT1",
        provenance=provenance,
    )
    print({
        "baseline_validation_accuracy": baseline_accuracy,
        "validation_accuracy": validation_accuracy,
        "artifact": str(args.artifact_output),
    })
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
