"""Allocated research stages. CPU is explicit default; never auto-select a GPU."""
from __future__ import annotations

import argparse
import json
import os
import random
import signal
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stage", choices=("gpu-gate", "collect", "control", "curriculum", "evaluate"))
    parser.add_argument("--parent", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--device", choices=("cpu", "cuda"), default="cpu")
    args = parser.parse_args()
    if args.stage != "gpu-gate" and not os.environ.get("SLURM_JOB_ID"):
        raise RuntimeError("registered production stages require a Slurm allocation; local runs are qualification only")
    if args.device == "cpu":
        if os.environ.get("CUDA_VISIBLE_DEVICES") != "-1":
            raise RuntimeError("CPU launch must disable CUDA before importing Torch")
    elif not os.environ.get("SLURM_JOB_ID"):
        raise RuntimeError("GPU research requires an allocated compute node")
    import torch

    from sls.curriculum import IRONCLAD_A20_ACT2
    from sls.diagnostics.cpu import write_json
    from sls.model import ModelConfig, PolicyBatch
    from sls.research.bank import NaturalBank, NaturalInitializer
    from sls.research.checkpoint import save
    from sls.research.collect import collect_bank
    from sls.research.diagnostics import ResearchDiagnostics
    from sls.research.protocol import (
        NATIVE_SHA256,
        PARENT_SHA256,
        PROTOCOL,
        RANGES,
        paired,
        should_stop,
    )
    from sls.rl import PPOConfig, PPOTrainer, ShardedWorkerPool
    from sls.rl.checkpoint import policy_from_training_checkpoint
    from sls.rl.critic_warmup import CriticWarmupConfig
    from sls.rl.evaluate import evaluate
    from sls.rl.training_contract import (
        git_state,
        native_artifact,
        native_source_digest,
        sha256_file,
    )

    torch.set_num_threads(1)
    torch.use_deterministic_algorithms(True)
    if native_source_digest() != NATIVE_SHA256 or native_artifact()["source_sha256"] != NATIVE_SHA256:
        raise ValueError("source/build native identity mismatch")
    if sha256_file(args.parent) != PARENT_SHA256:
        raise ValueError("parent weights identity mismatch")
    config_path = ROOT / "configs/research/act2_learning_r1.json"
    config = json.loads(config_path.read_text(encoding="utf-8"))
    if config["protocol"] != json.loads(json.dumps(PROTOCOL)):
        raise ValueError("preregistered protocol mismatch")
    ppo = PPOConfig(**config["ppo"])
    payload = torch.load(args.parent, map_location="cpu", weights_only=False)
    parent = policy_from_training_checkpoint(payload, device=args.device)
    if parent.config.to_dict() != ModelConfig(**config["model"]).to_dict():
        raise ValueError("parent architecture differs from frozen configuration")
    stopped = False
    def request_stop(signum, frame):
        nonlocal stopped
        stopped = True
    signal.signal(signal.SIGTERM, request_stop)
    signal.signal(signal.SIGINT, request_stop)
    deadline = float(os.environ.get("SLS_RESEARCH_DEADLINE", "inf"))
    def stop():
        return stopped or time.time() >= deadline
    out = args.output
    out.mkdir(parents=True, exist_ok=True)

    def evaluation(model, role, label):
        path = out / "evaluations" / f"{role}-{label}.json"
        if path.exists():
            raise FileExistsError("evaluation namespace already exists")
        start, end = RANGES[role]
        results = []
        chunks = []
        for first in range(start, end, 128):
            if stop():
                raise InterruptedError("evaluation stopped; partial evaluations are not paired evidence")
            result = evaluate(model, IRONCLAD_A20_ACT2, tuple(range(first, min(first + 128, end))),
                              device=args.device, max_steps=4096, max_boundary_visits=4,
                              environment_shards=16 if args.device == "cuda" else 1,
                              stop_requested=stop, failure_progress_scale=0.)
            if result.backend_truncations or result.backend_errors or result.timeouts or result.episodes != min(128, end - first):
                raise RuntimeError("evaluation execution health gate failed")
            results.extend(result.seed_results)
            from dataclasses import asdict
            chunks.append(asdict(result))
        if [row["seed"] for row in results] != list(range(start, end)):
            raise ValueError("evaluation seed alignment mismatch")
        write_json(path, {"role": role, "label": label, "seed_range": [start, end], "seed_results": results,
            "chunks": chunks, "note": "Boss exposure and loops retain actual entry denominators; chunk rates are not averaged"})
        return results

    def arrival(rows):
        return ["2" in row["act_entries"] for row in rows]

    if args.stage == "gpu-gate":
        if (out / "gpu-gate.json").exists() or (out / "qualification-base.pt").exists():
            raise FileExistsError("qualification artifacts already exist")
        from sls.backends.simulator import SimulatorBackend
        from sls.research.bank import score
        backend = SimulatorBackend(IRONCLAD_A20_ACT2)
        decision = backend.reset(RANGES["training"][0])
        cpu = policy_from_training_checkpoint(payload, device="cpu")
        cpu_output = score(cpu, decision, cpu.initial_memory(1, "cpu"), start=True)
        gpu_output = score(parent, decision, parent.initial_memory(1, args.device), start=True)
        torch.testing.assert_close(cpu_output.logits, gpu_output.logits.cpu(), atol=2e-4, rtol=2e-4)
        torch.testing.assert_close(cpu_output.next_memory, gpu_output.next_memory.cpu(), atol=2e-4, rtol=2e-4)
        # Same-device recurrent recomputation, legal probabilities, finite critic backward.
        batch = PolicyBatch.from_decisions([decision], parent.config).to(args.device)
        parent.train()
        output = parent(*batch.model_inputs())
        output.value.square().mean().backward()
        if not all(torch.isfinite(p.grad).all() for p in parent.parameters() if p.grad is not None):
            raise RuntimeError("nonfinite GPU gradients")
        from dataclasses import replace

        from sls.rl import VectorWorkerPool
        small = replace(ppo, rollout_steps=4, recurrent_sequence_length=2,
                        minibatch_sequences=2, epochs=1, final_kl_samples=8)
        first = PPOTrainer(policy_from_training_checkpoint(payload, device=args.device),
            VectorWorkerPool(IRONCLAD_A20_ACT2, 2), small, device=args.device, seed=RANGES["training"][0])
        second = PPOTrainer(policy_from_training_checkpoint(payload, device=args.device),
            VectorWorkerPool(IRONCLAD_A20_ACT2, 2), small, device=args.device, seed=RANGES["training"][0],
            research_hooks=ResearchDiagnostics(2, prefix_workers=2))
        torch.manual_seed(37)
        first_rollout = first.collect()
        torch.manual_seed(37)
        second_rollout = second.collect()
        if not torch.equal(first_rollout.action_indices, second_rollout.action_indices):
            raise RuntimeError("GPU diagnostic action equivalence failed")
        first_metrics, second_metrics = first.optimize(first_rollout), second.optimize(second_rollout)
        if first_metrics != second_metrics or any(not torch.equal(v, second.model.state_dict()[k]) for k, v in first.model.state_dict().items()):
            raise RuntimeError("GPU diagnostic loss/update equivalence failed")
        for key, state in first.optimizer.state_dict()["state"].items():
            for field, value in state.items():
                if not torch.equal(value, second.optimizer.state_dict()["state"][key][field]):
                    raise RuntimeError("GPU diagnostic Adam equivalence failed")
        from sls.rl import load_checkpoint, save_checkpoint
        save_checkpoint(out / "qualification-base.pt", first)
        expected = first.collect()
        restored = PPOTrainer(policy_from_training_checkpoint(payload, device=args.device),
            VectorWorkerPool(IRONCLAD_A20_ACT2, 2), small, device=args.device, seed=RANGES["training"][0])
        load_checkpoint(out / "qualification-base.pt", restored)
        actual = restored.collect()
        if not torch.equal(expected.action_indices, actual.action_indices) or not torch.equal(expected.returns, actual.returns):
            raise RuntimeError("GPU native/memory/RNG checkpoint continuation equivalence failed")
        write_json(out / "gpu-gate.json", {"passed": True, "native": native_artifact(),
            "torch": str(torch.__version__), "device": args.device, "parent_sha256": PARENT_SHA256,
            "cpu_gpu_tolerance": 2e-4, "diagnostic_actions_losses_parameters_exact": True,
            "qualification_student_decisions": 32,
            "diagnostic_adam_and_checkpoint_continuation_exact": True,
            "research_diagnostics": second.research_hooks.records,
            "scope": "forward/memory/finite_backward and same-device diagnostic update equivalence"})
    elif args.stage == "collect":
        collect_bank(out / "bank", parent, ppo, stop_requested=stop)
        write_json(out / "bank-qualification.json", NaturalBank(out / "bank").qualify(stop_requested=stop))
    elif args.stage in {"control", "curriculum"}:
        bank = NaturalBank(out / "bank")
        qualification = json.loads((out / "bank-qualification.json").read_text())
        if not qualification["passed"] or qualification["bank_sha256"] != bank.identity:
            raise ValueError("bank has not passed independent qualification")
        arm = args.stage
        arm_root = out / arm
        if arm_root.exists():
            raise FileExistsError("arm output already exists; explicit exact resume is required")
        arm_root.mkdir()
        periodic_parent_path = out / "evaluations/periodic-parent.json"
        reference = json.loads(periodic_parent_path.read_text())["seed_results"] if periodic_parent_path.exists() else evaluation(parent, "periodic", "parent")
        model = policy_from_training_checkpoint(payload, device=args.device)
        random.seed(config["training_rng_seed"])
        torch.manual_seed(config["training_rng_seed"])
        if args.device == "cuda":
            torch.cuda.manual_seed_all(config["training_rng_seed"])
        initializer = NaturalInitializer(bank, probability=.25 if arm == "curriculum" else 0., seed=config["sampler_rng_seed"])
        diagnostics = ResearchDiagnostics(64, prefix_workers=config["diagnostic_prefix_workers"])
        experiment = {"arm": arm, "config_sha256": sha256_file(config_path), "bank_sha256": bank.identity}
        history = []
        with ShardedWorkerPool(IRONCLAD_A20_ACT2, 64, shard_count=16, crash_dump_dir=arm_root / "crashes") as workers:
            trainer = PPOTrainer(model, workers, ppo, device=args.device, seed=RANGES["training"][0],
                training_seed_limit=RANGES["training"][1], git_commit=git_state()["commit"],
                training_config_digest=sha256_file(config_path),
                critic_warmup=CriticWarmupConfig(**config["critic_warmup"]),
                episode_initializer=initializer, research_hooks=diagnostics)
            trainer.environment_steps = config["parent_environment_steps"]
            save(arm_root / "initial.pt", trainer, experiment)
            reason = "fixed_budget"
            last_update_seconds = 0.
            try:
                for update in range(1, 129):
                    if stop() or time.time() + max(300., 1.5 * last_update_seconds) >= deadline:
                        reason = "wall_time_or_signal"
                        break
                    update_begin = time.perf_counter()
                    metrics = trainer.train_update()
                    last_update_seconds = time.perf_counter() - update_begin
                    if trainer.update != update:
                        raise RuntimeError("update accounting mismatch")
                    if update % 32 == 0:
                        save(arm_root / f"update-{update:03d}.pt", trainer, experiment)
                        if update == 32:
                            for name, value in model.state_dict().items():
                                if not name.startswith("value_head.") and not torch.equal(value.cpu(), payload["model"][name]):
                                    raise RuntimeError("critic warmup changed frozen actor/backbone parameters")
                            if arm == "curriculum":
                                control = torch.load(out / "control/update-032.pt", map_location="cpu", weights_only=False)["base"]
                                current = torch.load(arm_root / "update-032.pt", map_location="cpu", weights_only=False)["base"]
                                def identical(a, b):
                                    if isinstance(a, torch.Tensor):
                                        return isinstance(b, torch.Tensor) and torch.equal(a, b)
                                    if isinstance(a, dict):
                                        return isinstance(b, dict) and a.keys() == b.keys() and all(identical(a[k], b[k]) for k in a)
                                    if isinstance(a, (list, tuple)):
                                        return type(a) is type(b) and len(a) == len(b) and all(identical(x, y) for x, y in zip(a, b))
                                    return a == b
                                if not identical(control, current):
                                    raise RuntimeError("two arms differ before the distribution intervention")
                                write_json(arm_root / "warmup-equivalence.json", {"passed": True,
                                    "scope": "full base checkpoint: model/optimizer/RNG/memory/native/limits/critic pending", "update": 32})
                        results = evaluation(model, "periodic", f"{arm}-{update:03d}")
                        comparison = paired(arrival(reference), arrival(results))
                        history.append(comparison)
                        write_json(arm_root / f"update-{update:03d}.json", {"metrics": metrics,
                            "retention": comparison, "sampler_counts": initializer.counts,
                            "sampler_phase_counts": initializer.phase_counts,
                            "prefix_seconds": initializer.prefix_seconds, "prefix_decisions": initializer.prefix_decisions,
                            "diagnostic_seconds": diagnostics.seconds})
                        if should_stop(history):
                            reason = "preregistered_retention_stop"
                            break
                    print(json.dumps({"arm": arm, "update": update, "metrics": metrics}), flush=True)
            except BaseException as error:
                reason = "execution_failure"
                write_json(arm_root / "failure.json", {"error": repr(error), "update": trainer.update,
                    "resume": "use only prior completed periodic checkpoints; interrupted state is ineligible"})
                raise
            finally:
                if reason != "execution_failure":
                    save(arm_root / "latest.pt", trainer, experiment)
                write_json(arm_root / "completion.json", {"updates": trainer.update,
                    "reason": reason, "student_decisions": trainer.environment_steps - config["parent_environment_steps"],
                    "bank_sha256": bank.identity, "shared_teacher_decisions": bank.manifest["teacher_decisions"],
                    "sampler_counts": initializer.counts, "sampler_phase_counts": initializer.phase_counts,
                    "prefix_decisions": initializer.prefix_decisions, "prefix_seconds": initializer.prefix_seconds,
                    "diagnostic_seconds": diagnostics.seconds})
            if reason == "wall_time_or_signal":
                raise InterruptedError("arm saved at update boundary; paired experiment incomplete")
    else:
        common = min(json.loads((out / arm / "completion.json").read_text())["updates"] for arm in ("control", "curriculum"))
        common -= common % 32
        if common < 32:
            raise ValueError("no common budget checkpoint available")
        reference = evaluation(parent, "confirmation", "parent")
        comparisons = {}
        arm_results = {}
        for arm in ("control", "curriculum"):
            endpoint = out / arm / f"update-{common:03d}.pt"
            research = torch.load(endpoint, map_location="cpu", weights_only=False)
            from sls.research.checkpoint import SCHEMA
            from sls.research.protocol import digest
            bank_identity = sha256_file(out / "bank/manifest.json")
            expected_experiment = {"arm": arm, "config_sha256": sha256_file(config_path), "bank_sha256": bank_identity}
            if (research["schema"] != SCHEMA or research["protocol_sha256"] != digest(PROTOCOL)
                    or research["experiment"] != expected_experiment
                    or research["base"]["trainer"]["update"] != common
                    or research["base"]["trainer"]["environment_steps"] != 90013696 + common * 16384
                    or research["base"]["contract"]["native_source_sha256"] != NATIVE_SHA256):
                raise ValueError("endpoint experiment/environment/budget identity mismatch")
            model = policy_from_training_checkpoint(research["base"], device=args.device)
            results = evaluation(model, "confirmation", arm)
            arm_results[arm] = results
            comparisons[arm] = {"endpoint_sha256": sha256_file(endpoint),
                "joint_completion": paired([r["success"] for r in reference], [r["success"] for r in results]),
                "act2_arrival": paired(arrival(reference), arrival(results))}
        curriculum = comparisons["curriculum"]
        versus_control = {
            "joint_completion": paired([r["success"] for r in arm_results["control"]], [r["success"] for r in arm_results["curriculum"]]),
            "act2_arrival": paired(arrival(arm_results["control"]), arrival(arm_results["curriculum"]))}
        positive = curriculum["joint_completion"]["paired_ci_95"][0] > 0 and curriculum["joint_completion"]["exact_p_two_sided"] < .05
        distribution_gain = versus_control["joint_completion"]["paired_ci_95"][0] > 0 and versus_control["joint_completion"]["exact_p_two_sided"] < .05
        retained = curriculum["act2_arrival"]["paired_lower_one_sided_95"] > -.03
        decision = ("promising_expand_candidate" if positive and retained and distribution_gain else
                    "joint_gain_but_distribution_effect_unconfirmed" if positive and retained else
                    "joint_gain_retention_failed" if positive else
                    "no_positive_gain_in_this_trial" if curriculum["joint_completion"]["paired_ci_95"][1] <= 0 else
                    "evidence_insufficient")
        write_json(out / "comparison.json", {"schema": "sls-act2-paired-exploration-v1", "common_updates": common,
            "comparisons": comparisons, "claim": "one training seed; exploratory development evaluation",
            "curriculum_vs_control": versus_control,
            "decision": decision, "recommend_expand": positive and retained and distribution_gain,
            "recommendation_rule": "exploratory; require joint gain vs parent and control (paired CI lower>0, exact p<.05), plus parent retention lower>-.03; never auto-expand"})


if __name__ == "__main__":
    main()
