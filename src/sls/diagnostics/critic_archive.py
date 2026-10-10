"""Independent stopped-run evidence checks; never restore a training process."""
from __future__ import annotations

import hashlib
import json
import math
import statistics
import tarfile
from collections import Counter
from pathlib import Path, PurePosixPath


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def extract_archive(archive: Path, destination: Path, *, verify_existing=False):
    """Verify every member and extract into a new directory without links."""
    require(destination.is_dir() if verify_existing else not destination.exists(),
            "archive destination already exists or verification directory missing")
    inventory, seen = [], set()
    with tarfile.open(archive, "r:gz") as stream:
        for member in stream:
            rel = PurePosixPath(member.name)
            require(not rel.is_absolute() and ".." not in rel.parts
                    and "\\" not in member.name and ":" not in member.name,
                    "unsafe archive path")
            require(member.isfile() or member.isdir(), "archive links/devices are forbidden")
            require(member.name not in seen, "duplicate archive member")
            seen.add(member.name)
            path = destination.joinpath(*rel.parts)
            require(path.resolve().is_relative_to(destination.resolve()), "archive path escapes destination")
            if member.isdir():
                if verify_existing:
                    require(path.is_dir(), "missing archive directory")
                else:
                    path.mkdir(parents=True, exist_ok=True)
                continue
            data = stream.extractfile(member).read()
            require(len(data) == member.size, "short archive member")
            if verify_existing:
                require(path.is_file() and sha(path) == hashlib.sha256(data).hexdigest(),
                        "existing extraction hash mismatch")
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                with path.open("xb") as target:
                    target.write(data)
            inventory.append({"name": member.name, "bytes": len(data),
                              "sha256": hashlib.sha256(data).hexdigest()})
    return {"sha256": sha(archive), "files": inventory}


def paired_binary(reference, candidate, key):
    require(set(reference) == set(candidate), "paired seeds differ")
    lost = sum(bool(reference[s][key]) and not bool(candidate[s][key]) for s in reference)
    gained = sum(not bool(reference[s][key]) and bool(candidate[s][key]) for s in reference)
    n = lost + gained
    p = min(1., 2 * sum(math.comb(n, k) for k in range(min(lost, gained) + 1)) / 2**n) if n else 1.
    return {"paired_seeds": len(reference), "lost": lost, "gained": gained,
            "net": gained - lost, "exact_mcnemar_p": p}


def verify_curve(rows, parent_steps, stride, periodic_seeds):
    updates = [r for r in rows if "update_seconds" in r]
    require([r["update"] for r in updates] == list(range(1, len(updates) + 1)),
            "missing or duplicate training update")
    require(all(r["environment_steps"] == parent_steps + r["update"] * stride for r in updates),
            "incorrect environment-step accounting")
    require([r.get("critic_warmup_active", 0) for r in updates] == [1.] * 32 + [0.] * (len(updates) - 32),
            "warmup phase differs from registered 32-update window")
    reference, curve = None, []
    for r in rows:
        require(not r.get("terminations_backend_truncated", 0), "backend fault in training targets")
        if "evaluation" not in r:
            continue
        e = r["evaluation"]
        require(not any(e.get(k, 0) for k in ("backend_errors", "backend_truncations", "timeouts")),
                "evaluation execution health failed")
        seeds = e["seed_results"]
        require([s["seed"] for s in seeds] == list(range(*periodic_seeds)), "evaluation seeds differ")
        normalized = {s["seed"]: {**s, "reached_act2": "2" in s["act_entries"]} for s in seeds}
        require(sum(s["success"] for s in seeds) == e["successes"], "success aggregate mismatch")
        require(sum(s["reached_act2"] for s in normalized.values()) == e["reached_act2"],
                "Act2 aggregate mismatch")
        require(sum(s["reason"] == "cycle_limit" for s in seeds) == e["cycle_limits"],
                "cycle aggregate mismatch")
        require(len(seeds) == e["episodes"], "evaluation episode count mismatch")
        if reference is None:
            require(r.get("baseline") is True, "frozen reference evaluation missing")
            reference = normalized
        entries = [s["act_entries"]["2"] for s in seeds if "2" in s["act_entries"]]
        cycles = Counter(f"{s['floor']}:{s['terminal_screen']}" for s in seeds if s["reason"] == "cycle_limit")
        boss_entries = {k: v["entries"] for k, v in e["boss_action_metrics"].items()}
        curve.append({"steps": r["environment_steps"], "episodes": e["episodes"],
                      "wins": e["successes"], "reached_act2": e["reached_act2"],
                      "cycles": e["cycle_limits"], "cycle_contexts": dict(cycles),
                      "median_failure_floor": e["median_failure_floor"],
                      "paired_success": paired_binary(reference, normalized, "success"),
                      "paired_reached_act2": paired_binary(reference, normalized, "reached_act2"),
                      "boss_encounter_entries": boss_entries,
                      "actual_act2_boss_entries": sum(v for k, v in boss_entries.items() if k.startswith("ACT_2:")),
                      "boss_assignment_counts": e["boss_attempts"],
                      "act2_entry_mean_deck_size": statistics.mean(len(s["deck"]) for s in entries) if entries else None,
                      "act2_entry_mean_hp": statistics.mean(s["hp"] for s in entries) if entries else None,
                      "act2_entry_mean_value": statistics.mean(s["value_shaped"] for s in entries) if entries else None})
    return updates, curve


def window_summary(rows):
    keys = ("update_seconds", "collect_seconds", "collect_encode_seconds", "collect_transition_seconds",
            "collect_worker_step_seconds", "collect_policy_seconds", "optimize_seconds", "entropy",
            "advantage_scale_combat", "advantage_scale_choice", "advantage_scale_run", "value",
            "value_explained_variance", "gradient_clip_fraction", "terminations_success", "terminations_cycle_limit")
    return {"updates": len(rows), "first_update": rows[0]["update"], "last_update": rows[-1]["update"],
            "means": {k: statistics.mean(r[k] for r in rows if k in r) for k in keys if any(k in r for r in rows)}}


def audit_run(run: Path, parent: Path):
    import torch

    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import IRONCLAD_A20_ACT2
    from sls.model import PolicyBatch
    from sls.rl.checkpoint import policy_from_training_checkpoint
    from sls.rl.episode_limit import EpisodeLimitState
    from sls.rl.training_contract import native_source_digest, source_sha256

    manifest = json.loads((run / "run-manifest.json").read_text(encoding="utf-8"))
    require(native_source_digest() == manifest["native_source_sha256"], "native restore requires archived source")
    require(sha(parent) == manifest["initialization"]["parent_checkpoint_sha256"], "wrong frozen parent")
    require(source_sha256(run / "training-config.toml") == manifest["config_sha256"], "config hash mismatch")
    rows = [json.loads(line) for line in (run / "stages/train/metrics.jsonl").read_text(encoding="utf-8").splitlines()]
    # Reject NaN/Infinity anywhere in JSON metric evidence.
    json.dumps(rows, allow_nan=False)
    parent_steps = manifest["initialization"]["parent_environment_steps"]
    updates, curve = verify_curve(rows, parent_steps, 16384, manifest["periodic_evaluation_seeds"])
    parent_payload = torch.load(parent, map_location="cpu", weights_only=False)
    parent_model = parent_payload["model"]
    checkpoints = []
    for path in sorted(run.rglob("*.pt")):
        payload = torch.load(path, map_location="cpu", weights_only=False)
        policy_from_training_checkpoint(payload)
        contract, trainer, warm = payload["contract"], payload["trainer"], payload["critic_warmup"]
        for key in ("native_source_sha256", "encoding_schema", "vocabulary_sha256", "ppo", "model"):
            require(contract[key] == manifest[key], "checkpoint/manifest mismatch: " + key)
        require(contract["git_commit"] == manifest["git"]["commit"], "checkpoint source commit mismatch")
        require(contract["training_config_sha256"] == manifest["training_identity_sha256"], "training identity mismatch")
        require(warm["completed_updates"] == 32 and warm["phase"] == "PPO" and not any(warm["pending"]),
                "checkpoint warmup phase mismatch")
        update = trainer["update"]
        require(0 < update <= len(updates), "checkpoint ahead of logged updates")
        require(trainer["environment_steps"] == updates[update - 1]["environment_steps"], "checkpoint step mismatch")
        for reason, count in trainer["termination_counts"].items():
            require(count == sum(r.get("terminations_" + reason, 0) for r in updates[:update]),
                    "checkpoint termination counter mismatch")
        require(trainer["episodes"] == sum(trainer["termination_counts"].values()), "checkpoint episode count mismatch")
        require(len(payload["environments"]) == contract["workers"], "checkpoint worker count mismatch")
        require(tuple(trainer["memory"].shape) == (64, manifest["model"]["recurrent_hidden_dim"])
                and bool(torch.isfinite(trainer["memory"]).all()), "invalid saved recurrent memory")
        require(len(trainer["episode_limits"]) == 64, "invalid saved limiter count")
        restored_public = []
        for native, limiter in zip(payload["environments"], trainer["episode_limits"], strict=True):
            EpisodeLimitState.from_dict(limiter)
            backend = SimulatorBackend(IRONCLAD_A20_ACT2)
            decision = backend.load_checkpoint(native)
            # Exercise public projection and policy encoding, never use hidden native fields as inputs.
            restored_public.append(hashlib.sha256(json.dumps(decision.observation.to_dict(), sort_keys=True).encode()).hexdigest())
            for tensor in PolicyBatch.from_decisions((decision,)).model_inputs():
                require(bool(torch.isfinite(tensor).all()), "nonfinite restored public encoding")
        for state in payload["optimizer"]["state"].values():
            for value in state.values():
                if isinstance(value, torch.Tensor):
                    require(bool(torch.isfinite(value).all()), "nonfinite optimizer tensor")
        distances = {}
        for group in ("actor", "value"):
            names = [k for k in parent_model if k.startswith("value_head.") == (group == "value")]
            require(all(bool(torch.isfinite(payload["model"][k]).all()) for k in names), "nonfinite model")
            delta = sum(float((payload["model"][k].double() - parent_model[k].double()).square().sum()) for k in names)
            norm = sum(float(parent_model[k].double().square().sum()) for k in names)
            distances[group] = {"l2_change": math.sqrt(delta), "relative_l2_change": math.sqrt(delta / norm)}
        checkpoints.append({"path": path.relative_to(run).as_posix(), "sha256": sha(path),
                            "steps": trainer["environment_steps"], "update": update,
                            "episodes": trainer["episodes"], "termination_counts": trainer["termination_counts"],
                            "native_restores_checked": len(restored_public), "restored_observation_sha256": restored_public,
                            "warmup_discarded_states": warm["discarded_states"], "weight_distances": distances})
    selected = json.loads((run / "stages/train/selection/best_progress.json").read_text(encoding="utf-8"))
    best = next(c for c in checkpoints if c["path"].endswith("best_progress.pt"))
    require(best["sha256"] == selected["checkpoint_sha256"] and best["steps"] == selected["environment_steps"],
            "selected checkpoint mismatch")
    peak = max(c["wins"] for c in curve)
    require(selected["successes"] == peak and selected["environment_steps"] == next(c["steps"] for c in curve if c["wins"] == peak),
            "earliest maximum selection mismatch")
    latest = next(c for c in checkpoints if c["path"] == "latest.pt")
    ppo = updates[32:]
    return {"schema": "sls-stopped-critic-audit-v1", "identity": manifest,
            "last_logged_steps": updates[-1]["environment_steps"], "last_logged_update": len(updates),
            "new_logged_decisions": updates[-1]["environment_steps"] - parent_steps,
            "new_saved_decisions": latest["steps"] - parent_steps,
            "unsaved_logged_decisions": updates[-1]["environment_steps"] - latest["steps"],
            "budget_fraction_logged": (updates[-1]["environment_steps"] - parent_steps) / 20000000,
            "periodic_curve": curve, "checkpoints": checkpoints,
            "warmup_window": window_summary(updates[:32]), "early_ppo": window_summary(ppo[:50]),
            "late_ppo": window_summary(ppo[-50:]),
            "logged_update_wall_seconds": sum(r["update_seconds"] for r in updates),
            "completed_training_episodes": updates[-1]["episodes"],
            "training_successes": sum(r["terminations_success"] for r in updates),
            "training_successes_warmup": sum(r["terminations_success"] for r in updates[:32]),
            "warmup_first_loss": updates[0]["warmup_loss"], "warmup_last_loss": updates[31]["warmup_loss"],
            "missing_completion_evidence": [n for n in ("final.pt", "training-bundle.json", "reference-evaluation.json",
                "endpoint-evaluation.json", "final-evaluation.json") if not (run / n).exists()],
            "limitations": ["Stopped partial budget; no fixed 20M endpoint or development confirmation.",
                "Repeated periodic seeds and best selection are development evidence, not final holdout.",
                "No budget-matched no-warmup control; no isolated warmup causal claim.",
                "No warmup-boundary checkpoint/probe supplied; actor freeze verified in source, not from archived boundary weights.",
                "Archive hash establishes local identity, not authenticity against a missing server-side digest."]}
