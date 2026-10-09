"""Allocated-node full-weight acceptance; never invoked on the workstation."""
from __future__ import annotations

import argparse
import json
import math
import os
import random
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))


def legal_action(decision, kind, subject_id):
    """Use the unique current candidate, including schema and public metadata."""
    matches = [action for action in decision.actions
               if action.kind == kind and action.subject_id == subject_id]
    if len(matches) != 1:
        raise RuntimeError(f"expected one legal {kind.value} for {subject_id}, got {len(matches)}")
    return matches[0]


def verify_grid(backend, restored, fixture, check_encoded_selection):
    """Stock-bound GRID checks, callable without loading a model or CUDA."""
    from sls.contracts import Action, ActionKind

    fixture_action = Action.from_dict(fixture["action"])
    first = backend.load_checkpoint(fixture["before"])
    partial = backend.step(legal_action(first, fixture_action.kind, fixture_action.subject_id)).decision
    if (partial.observation.to_dict()["selected_cards"] != fixture["stock_partial_selection"]
            or len(partial.observation.reward_options) != fixture["stock_candidate_count"]
            or partial.observation.deck != first.observation.deck):
        raise RuntimeError("stock GRID partial projection mismatch")
    check_encoded_selection(partial)
    legal_action(partial, fixture_action.kind, fixture_action.subject_id)
    snapshot = backend.checkpoint()
    restored_partial = restored.load_checkpoint(snapshot)
    restored.step(legal_action(restored_partial, fixture_action.kind, fixture_action.subject_id))
    undo = backend.step(legal_action(partial, fixture_action.kind, fixture_action.subject_id)).decision
    if undo.observation.to_dict() != first.observation.to_dict() or restored.checkpoint() != backend.checkpoint():
        raise RuntimeError("GRID cancellation/restoration mismatch")
    options = first.observation.reward_options
    duplicates = next((x.instance_id, y.instance_id) for i, x in enumerate(options)
                      for y in options[i+1:] if x.content_id == y.content_id and x.instance_id != y.instance_id)
    current = undo
    for subject in duplicates:
        current = backend.step(legal_action(current, ActionKind.REMOVE_CARD, subject)).decision
    committed = current
    if len(backend.checkpoint()["public_inventory"]["deck"]) != len(fixture["before"]["public_inventory"]["deck"]) - 2:
        raise RuntimeError("GRID distinct-instance commit mismatch")
    if any(a.kind.value == "REMOVE_CARD" for a in committed.actions):
        raise RuntimeError("GRID final pick did not commit")



def fixed_actor_probe(model, batch, no_grad):
    """Compare the frozen policy under the collector's eval/no-grad mode.

    train/eval may choose different Transformer kernels even at dropout=0.
    Both snapshots must use the same inference path, not just the same weights.
    The context factory is injectable for pure mocked regression tests.
    """
    model.eval()
    with no_grad():
        output = model(*batch.model_inputs())
        return output.logits.clone(), output.next_memory.clone()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, required=True)
    args = parser.parse_args()
    if sys.platform != "linux" or not os.environ.get("SLURM_JOB_ID"):
        raise RuntimeError("compute gate requires a Linux Slurm allocation")
    from tools.act12_critic20m_contract import validate
    plan, config_path, config = validate(args.plan)
    import torch

    from sls.curriculum import IRONCLAD_A20_ACT2
    from sls.model import ModelConfig, Policy, PolicyBatch
    from sls.rl import (
        PPOConfig,
        PPOTrainer,
        ShardedWorkerPool,
        load_checkpoint,
        save_checkpoint,
    )
    from sls.rl.act1_transfer import initialize_act1_weights
    from sls.rl.critic_warmup import CriticWarmupConfig
    from sls.rl.training_contract import git_state, native_source_digest, sha256_file
    from tools.train_full_run import _training_identity

    if not torch.cuda.is_available():
        raise RuntimeError("compute gate requires CUDA")
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision("high")
    prep = ROOT / Path(config["run"]["benchmark"]).parent
    report = ROOT / config["run"]["compute_gate_report"]
    directory = prep / "compute-gate"
    directory.mkdir()  # Do not silently repeat a failed acceptance.
    started = time.monotonic()
    checks = {}
    identity = _training_identity(config, workers=64, shards=16)
    initial = prep / "initial.pt"
    (prep / "training-config.toml").write_bytes(config_path.read_bytes())

    def exact(a, b):
        if isinstance(a, torch.Tensor):
            return isinstance(b, torch.Tensor) and a.dtype == b.dtype and torch.equal(a.cpu(), b.cpu())
        if isinstance(a, dict):
            return isinstance(b, dict) and a.keys() == b.keys() and all(exact(a[k], b[k]) for k in a)
        if isinstance(a, (list, tuple)):
            return type(a) is type(b) and len(a) == len(b) and all(exact(x, y) for x, y in zip(a, b))
        return a == b

    try:
        from tools.verify_act12_shared_rules import verify as verify_shared_rules
        shared = verify_shared_rules(ROOT)
        checks["shared_rules"] = "PASS"
        checks["shared_rule_evidence"] = shared
        # Existing stock-bound GRID fixture: first select, cancel, distinct
        # instances of the same card, final automatic commit, and restoration.
        from sls.backends.simulator import SimulatorBackend
        fixture = json.loads((ROOT / "tests/fixtures/regressions/act2-empty-cage-grid-131100069.json").read_text())
        from sls.model import encode_decision
        from sls.model.encoding import NUMERIC_FIELD_IDS

        def check_encoded_selection(partial):
            encoded = encode_decision(partial)
            selected_column = encoded.entity_numeric[:, NUMERIC_FIELD_IDS["selected"]]
            if int((selected_column != 0).sum()) < 1:
                raise RuntimeError("GRID selected state missing from policy encoding")

        verify_grid(SimulatorBackend(IRONCLAD_A20_ACT2), SimulatorBackend(IRONCLAD_A20_ACT2),
                    fixture, check_encoded_selection)
        checks["grid"] = "PASS"
        random.seed(config["run"]["seed"])
        torch.manual_seed(config["run"]["seed"])
        with ShardedWorkerPool(IRONCLAD_A20_ACT2, 64, shard_count=16,
                               crash_dump_dir=directory / "crashes") as workers:
            model = Policy(ModelConfig(**config["model"]))
            trainer = PPOTrainer(model, workers, PPOConfig(**config["ppo"]), device="cuda",
                                 seed=config["run"]["seed"], native_contract_digest=native_source_digest(),
                                 git_commit=git_state()["commit"], training_config_digest=identity,
                                 training_seed_limit=config["run"]["training_seed_limit"],
                                 critic_warmup=CriticWarmupConfig(**config["critic_warmup"]))
            initialize_act1_weights(trainer, config, root=ROOT)
            if any(not torch.isfinite(t).all() for t in model.state_dict().values()):
                raise RuntimeError("nonfinite parent weight")
            save_checkpoint(initial, trainer)
            baseline = {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}
            batch = PolicyBatch.from_decisions((trainer.decisions[0],), model.config).to("cuda")
            expected_logits, expected_memory = fixed_actor_probe(model, batch, torch.no_grad)
            target_checks = 0
            def validate_targets(rewards, samples):
                nonlocal target_checks
                if len(rewards) != len(samples):
                    raise RuntimeError("warmup target length mismatch")
                for i, (_, target) in enumerate(samples):
                    # Independent fsum; comparison at the actual float32 loss dtype.
                    expected = torch.tensor(math.fsum(rewards[i:]), dtype=torch.float32)
                    if not torch.equal(expected, torch.tensor(target, dtype=torch.float32)):
                        raise RuntimeError("complete Monte Carlo return mismatch")
                    target_checks += 1
            trainer.warmup_target_validator = validate_targets

            def replay(label):
                checkpoint = directory / (label + ".pt")
                save_checkpoint(checkpoint, trainer)
                expected_metrics = trainer.train_update()
                a = directory / (label + "-expected.pt")
                save_checkpoint(a, trainer)
                load_checkpoint(checkpoint, trainer)
                actual_metrics = trainer.train_update()
                b = directory / (label + "-actual.pt")
                save_checkpoint(b, trainer)
                if (expected_metrics != actual_metrics
                        or not exact(torch.load(a, map_location="cpu", weights_only=False),
                                     torch.load(b, map_location="cpu", weights_only=False))):
                    raise RuntimeError("exact next sample/update replay failed: " + label)
                checks[label] = "PASS"

            trainer.train_update()
            if not any(trainer.critic_warmup.pending):
                raise RuntimeError("no cross-rollout episode observed; acceptance incomplete")
            replay("cross-rollout")
            while trainer.critic_warmup.completed_updates < 31:
                if time.monotonic() - started > 7200:
                    raise TimeoutError("compute gate exceeded two-hour bound")
                trainer.train_update()
            replay("last-warmup")
            for name, parameter in model.named_parameters():
                if not name.startswith("value_head.") and parameter in trainer.optimizer.state:
                    raise RuntimeError("warmup created actor/shared Adam state: " + name)
            for key, value in model.state_dict().items():
                if not key.startswith("value_head.") and not torch.equal(value.cpu(), baseline[key]):
                    raise RuntimeError("warmup modified actor/shared parameter: " + key)
            if not any(not torch.equal(v.cpu(), baseline[k]) for k, v in model.state_dict().items()
                       if k.startswith("value_head.")):
                raise RuntimeError("value head did not update")
            actual_logits, actual_memory = fixed_actor_probe(model, batch, torch.no_grad)
            actor_equal = torch.equal(actual_logits, expected_logits)
            gru_equal = torch.equal(actual_memory, expected_memory)
            checks["fixed_input_probe_mode"] = "eval/no_grad (both snapshots)"
            checks["fixed_input_actor_equal"] = actor_equal
            checks["fixed_input_gru_equal"] = gru_equal
            if not actor_equal or not gru_equal:
                raise RuntimeError("warmup changed fixed-input actor/GRU output")
            checks["actor_and_gru_frozen"] = "PASS"
            replay("first-ppo")
            if not target_checks:
                raise RuntimeError("no complete return targets checked")
            checks["complete_return_states_checked"] = target_checks
            load_checkpoint(initial, trainer)
            if trainer.update or trainer.critic_warmup.completed_updates or trainer.optimizer.state:
                raise RuntimeError("production initial state contains probe learning")
        result = {"schema": "sls-critic20m-compute-gate-v2", "ok": True, "checks": checks,
                  "initial_checkpoint": initial.relative_to(ROOT).as_posix(),
                  "initial_checkpoint_sha256": sha256_file(initial),
                  "training_identity_sha256": identity, "seconds": time.monotonic()-started,
                  "production_probe_updates_discarded": True}
    except BaseException as error:
        report.write_text(json.dumps({"ok": False, "checks": checks, "error": str(error)})+"\n")
        raise
    report.write_text(json.dumps(result, indent=2)+"\n")
    print(json.dumps({"compute_gate": "PASS", "probe_updates_discarded": True}))


if __name__ == "__main__":
    main()
