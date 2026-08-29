"""Evaluate one production artifact independently in Original and simulator."""

from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.backends.original import OriginalBackend  # noqa: E402
from sls.backends.simulator import SimulatorBackend  # noqa: E402
from sls.curriculum import (  # noqa: E402
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3,
    ironclad_fullrun_profile,
)
from sls.model import PolicyBatch  # noqa: E402
from sls.runtime import load_policy_artifact  # noqa: E402
from sls.validation.transfer import BackendPolicySummary, PolicyTransferReport  # noqa: E402
from sls.validation.transfer_gate import verify_policy_transfer_gate  # noqa: E402
from sls.rl.training_contract import canonical_digest  # noqa: E402


@torch.no_grad()
def evaluate_backend(model, backend, seeds, device: str, max_actions: int):  # type: ignore[no-untyped-def]
    started = time.perf_counter()
    rows = []
    action_counts: Counter[str] = Counter()
    diagnostics = Counter()
    for seed in seeds:
        print(f"policy-transfer {backend.__class__.__name__} seed={seed} start", file=sys.stderr, flush=True)
        decision = backend.reset(seed)
        for actions in range(1, max_actions + 1):
            if decision.terminal or not decision.actions:
                diagnostics["empty_decisions"] += 1
                rows.append((False, decision.observation.run.floor, actions - 1, "EMPTY_DECISION"))
                break
            batch = PolicyBatch.from_decisions((decision,)).to(device)
            output = model(*batch.model_inputs())
            action = decision.actions[int(output.logits[0].argmax())]
            action_counts[action.kind.value] += 1
            if actions % 50 == 0:
                print(
                    f"policy-transfer {backend.__class__.__name__} seed={seed} "
                    f"actions={actions} floor={decision.observation.run.floor}",
                    file=sys.stderr, flush=True,
                )
            try:
                transition = backend.step(action)
            except (KeyError, RuntimeError, ValueError):
                diagnostics["invalid_actions"] += 1
                rows.append((False, decision.observation.run.floor, actions, "INVALID_ACTION"))
                break
            decision = transition.decision
            if transition.terminated or transition.truncated:
                if transition.truncated:
                    diagnostics["backend_truncations"] += 1
                rows.append((
                    bool(transition.info.get("success")),
                    decision.observation.run.floor, actions,
                    str(transition.info.get("reason") or (
                        "TRUNCATED" if transition.truncated else "TERMINAL"
                    )),
                ))
                break
        else:
            rows.append((False, decision.observation.run.floor, max_actions, "TIMEOUT"))
        print(
            f"policy-transfer {backend.__class__.__name__} seed={seed} done "
            f"actions={rows[-1][2]} floor={rows[-1][1]} reason={rows[-1][3]}",
            file=sys.stderr, flush=True,
        )
    return rows, action_counts, diagnostics, time.perf_counter() - started


def summarize(name: str, result) -> BackendPolicySummary:  # type: ignore[no-untyped-def]
    rows, actions, diagnostics, elapsed_seconds = result
    return BackendPolicySummary(
        name, len(rows), sum(item[0] for item in rows),
        sum(item[1] for item in rows) / len(rows),
        sum(item[2] for item in rows) / len(rows),
        dict(sorted(actions.items())),
        dict(sorted(Counter(str(item[1]) for item in rows).items())),
        dict(sorted(Counter(item[3] for item in rows).items())),
        elapsed_seconds,
        diagnostics["invalid_actions"], diagnostics["empty_decisions"],
        diagnostics["backend_truncations"],
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--ascension", type=int, default=0)
    parser.add_argument("--seeds", type=int, nargs="+", required=True)
    parser.add_argument("--device", default="cpu")
    parser.add_argument("--max-actions", type=int, default=2_048)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--maximum-success-rate-gap", type=float, default=0.20)
    parser.add_argument("--maximum-mean-floor-gap", type=float, default=3.0)
    parser.add_argument("--maximum-action-tv", type=float, default=0.20)
    parser.add_argument(
        "--gate", type=Path,
        default=ROOT / "runs/policy_transfer_v1.json",
    )
    args = parser.parse_args()
    # Canary evaluation is the promotion path for a still-experimental model;
    # live deployment keeps the loader's production-only default.
    loaded = load_policy_artifact(
        args.artifact, device=args.device, allow_experimental=True,
    )
    if not loaded.metadata.ascension_min <= args.ascension <= loaded.metadata.ascension_max:
        raise ValueError("policy artifact does not cover the requested ascension")
    if loaded.metadata.goal == "HEART":
        profile = ironclad_fullrun_profile(args.ascension, require_heart=True)
    elif args.ascension != 0:
        raise ValueError("non-Heart curriculum transfer currently supports A0 only")
    else:
        profile = {
            "ACT1": IRONCLAD_A0_ACT1,
            "ACT2": IRONCLAD_A0_ACT2,
            "ACT3": IRONCLAD_A0_ACT3,
        }[loaded.metadata.goal]
    verified_gate = verify_policy_transfer_gate(
        args.gate, profile_id=profile.profile_id, require_canary=False,
    )
    original = OriginalBackend(profile=profile)
    original_completed = False
    try:
        original_rows = evaluate_backend(
            loaded.model, original, args.seeds, args.device, args.max_actions,
        )
        original_completed = True
    finally:
        if original_completed:
            original.return_to_menu()
    simulator_rows = evaluate_backend(
        loaded.model, SimulatorBackend(profile), args.seeds, args.device, args.max_actions,
    )
    report = PolicyTransferReport(
        summarize("ORIGINAL", original_rows), summarize("SIMULATOR", simulator_rows),
    ).to_dict()
    report.update({
        "artifact_sha256": hashlib.sha256(args.artifact.read_bytes()).hexdigest(),
        "profile": profile.profile_id,
        "gate_sha256": verified_gate["gate_sha256"],
        "evidence_sha256": verified_gate["evidence_sha256"],
        "seeds": list(args.seeds),
        "thresholds": {
            "maximum_success_rate_gap": args.maximum_success_rate_gap,
            "maximum_mean_floor_gap": args.maximum_mean_floor_gap,
            "maximum_action_tv": args.maximum_action_tv,
        },
    })
    report["accepted"] = bool(
        abs(float(report["success_rate_delta"])) <= args.maximum_success_rate_gap
        and abs(float(report["mean_floor_delta"])) <= args.maximum_mean_floor_gap
        and float(report["action_distribution_total_variation"]) <= args.maximum_action_tv
        and all(
            int(report[backend][field]) == 0
            for backend in ("original", "simulator")
            for field in ("invalid_actions", "empty_decisions", "backend_truncations")
        )
    )
    report["report_sha256"] = canonical_digest(report)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0 if report["accepted"] else 1


if __name__ == "__main__":
    from sls.validation.runtime import write_completion

    try:
        code = main()
    except Exception as error:
        write_completion(
            2, entry="policy-transfer", error=f"{type(error).__name__}: {error}",
            argv=sys.argv,
        )
        raise
    else:
        write_completion(code, entry="policy-transfer")
        raise SystemExit(code)
