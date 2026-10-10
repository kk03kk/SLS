"""Replay stock semantic actions from normal Neow and report the first divergence."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.act2_differential import pending_stock_intents
from sls.audit.card_parity import structured_differences
from sls.audit.decision_identity import canonical_projection, mapped_action
from sls.audit.trajectory_reader import read_trajectory
from sls.backends.original import ORIGINAL_EXECUTION_CONTRACT
from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.training_contract import native_source_digest
from tools.capture_act2_production_batch import ProductionBackend


def replay(path: Path, *, clock_witnesses: list[dict] | None = None) -> dict:
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("stale native artifact")
    metadata, boundaries = read_trajectory(path)
    identity = metadata.get("environment", {})
    if identity.get('original_execution_contract') != ORIGINAL_EXECUTION_CONTRACT:
        raise ValueError('stale original execution contract; recapture stable boundaries')
    if (identity.get("profile_id") != IRONCLAD_A20_ACT2.profile_id
            or identity.get("evaluation_contract") != "sls-frozen-act1-reference-act2-v1"
            or metadata["backend"] != "original"):
        raise ValueError("requires an explicitly identified frozen-reference stock Act2 trajectory")
    if not boundaries or not boundaries[-1]["terminal"]:
        raise ValueError("incomplete trajectory is an execution failure")
    initial = boundaries[0]
    if (initial["screen"], initial["act"], initial["floor"], initial["observation"]["run"]["ascension"]) != ("NEOW", 1, 0, 20):
        raise ValueError("requires a witnessed normal A20 Neow initial boundary")
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(metadata["seed"])
    first = None
    clock_inputs = []
    clocks = list(clock_witnesses or ())
    last_info = None
    terminal_reason_verified = False
    for index, original in enumerate(boundaries):
        ProductionBackend.require_isolation(original['diagnostic_state'])
        if pending_stock_intents(original['diagnostic_state']):
            first = {'boundary': index, 'classification': 'UNSTABLE_STOCK_BOUNDARY',
                     'pending_monster_slots': pending_stock_intents(original['diagnostic_state'])}
            break
        expected = canonical_projection(original["observation"], original["candidate_actions"], original["terminal"])
        actual = canonical_projection(decision.observation.to_dict(), [a.to_dict() for a in decision.actions], decision.terminal)
        differences = structured_differences(expected, actual)
        if differences:
            first = {"boundary": index, "screen": original["screen"], "act": original["act"],
                     "floor": original["floor"], "differences": differences,
                     "stock": expected, "native": actual}
            break
        if original["terminal"]:
            expected_outcome = {'reason': original['terminal_reason'], 'success': original['success']}
            actual_outcome = ({'reason': last_info.get('reason'), 'success': last_info.get('success')}
                              if last_info is not None else None)
            if actual_outcome != expected_outcome:
                first = {'boundary': index, 'classification': 'TERMINAL_OUTCOME_DIFFERENCE',
                         'stock': expected_outcome, 'native': actual_outcome}
            else:
                terminal_reason_verified = True
            break
        chosen = mapped_action(original["chosen_action"], original["observation"], decision.observation.to_dict())
        matches = [a for a in decision.actions if a.to_dict() == chosen]
        if len(matches) != 1:
            raise ValueError("stock semantic action cannot be unambiguously replayed")
        evidence = None
        choice = (backend.checkpoint().get('public_combat', {}).get('choice', {})
                  if clock_witnesses is not None else {})
        if clock_witnesses is not None and choice.get('task') == 'DISCOVERY':
            if not clocks:
                raise ValueError('missing independent stock Discovery clock witness')
            clock = clocks.pop(0)
            if (clock['seed'] != metadata['seed'] or clock['floor'] != original['floor']
                    or not 1 <= clock['updates'] <= 120):
                raise ValueError('stock Discovery clock witness context mismatch')
            evidence = {'discovery_retrieval_updates': clock['updates']}
            clock_inputs.append({'boundary': index, **clock})
        transition = backend.step(matches[0], validation_evidence=evidence)
        decision, last_info = transition.decision, transition.info
    if clock_witnesses is not None and not first and clocks:
        raise ValueError('unused stock Discovery clock witnesses')
    result = {"schema": "sls-act2-production-replay-v1", "seed": metadata["seed"],
            "trajectory_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "native_source_sha256": native_source_digest(), "policy": metadata["policy"],
            "evaluation_environment": identity, "boundaries_checked": index + 1,
            "first_divergence": first,
            "terminal_reason_verified": terminal_reason_verified,
            "status": ("UNSTABLE_STOCK_BOUNDARY" if first.get('classification') == 'UNSTABLE_STOCK_BOUNDARY'
                       else "SEMANTIC_DIFFERENCE") if first else "TRAJECTORY_MATCH",
            "rng_comparison": "NOT_EXPOSED_IN_PRODUCTION",
            "purpose": "FLOW_DIAGNOSTIC_NOT_WIN_RATE_ESTIMATE"}
    if clock_witnesses is not None:
        result['schema'] = 'sls-act2-clock-conditioned-replay-v1'
        result['clock_inputs'] = clock_inputs
        result['purpose'] = 'CONDITIONAL_RULES_DIAGNOSTIC_NOT_UNCONDITIONAL_PRODUCTION_PASS'
        if not first:
            result['status'] = 'CONDITIONAL_PUBLIC_TRAJECTORY_MATCH'
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("trajectory", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite production differential evidence")
    result = replay(args.trajectory)
    suffix = "-" + str(result["seed"])
    if not args.trajectory.stem.endswith(suffix):
        raise ValueError("trajectory is not associated with a named production batch")
    batch_path = args.trajectory.with_name(args.trajectory.stem[:-len(suffix)] + ".json")
    batch = json.loads(batch_path.read_text(encoding="utf-8"))
    launch_path = batch_path.with_suffix(".launch.json")
    launch = json.loads(launch_path.read_text(encoding="utf-8"))
    associated = [r for r in batch["runs"] if r["seed"] == result["seed"]]
    if (not batch.get("execution_complete") or batch.get("execution_error")
            or len(associated) != 1 or associated[0]["sha256"] != result["trajectory_sha256"]
            or launch["mode"] != "production" or launch["recovery_status"] != "RECOVERED"
            or launch["completion"]["exit_code"] != 0):
        raise ValueError("production batch execution/recovery identity failure")
    result["batch_sha256"] = hashlib.sha256(batch_path.read_bytes()).hexdigest()
    result["launch_evidence_sha256"] = hashlib.sha256(launch_path.read_bytes()).hexdigest()
    result["oracle_build_sha256"] = launch["oracle_sha256"]
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: result[k] for k in ("seed", "status", "boundaries_checked")}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
