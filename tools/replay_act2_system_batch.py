"""Compare stock system boundaries, including restored native continuation."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.act2_differential import direct_projection, pending_stock_intents
from sls.audit.card_parity import structured_differences
from sls.audit.decision_identity import canonical_projection, mapped_action
from sls.backends.original import ORIGINAL_EXECUTION_CONTRACT
from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.training_contract import native_source_digest


def stock_runtime_inputs(before: dict, after: dict) -> dict:
    """Derive animation input from stock counters, never from native results."""
    start = before['stock_raw'].get('_timing_evidence', {})
    end = after['stock_raw'].get('_timing_evidence', {})
    measured = {}
    if start.get('discovery_completion_serial', 0) != end.get('discovery_completion_serial', 0):
        updates = end['discovery_retrieval_updates']
        if not isinstance(updates, int) or not 1 <= updates <= 180:
            raise ValueError('invalid independent stock Discovery timing')
        measured['discovery_retrieval_updates'] = updates
    declared = dict(after.get('previous_action_validation_evidence', {}))
    if set(declared) - {'discovery_retrieval_updates', 'card_soul_cost_reset_count'}:
        raise ValueError('unknown controlled runtime input')
    if declared.get('discovery_retrieval_updates') != measured.get('discovery_retrieval_updates') and declared:
        raise ValueError('adapter timing disagrees with independent stock counters')
    if 'card_soul_cost_reset_count' in declared:
        souls = before['stock_raw'].get('_continuation', {}).get('active_card_souls', ())
        pending = {s['card_uuid'] for s in souls if s.get('destination') == 'DISCARD_PILE'}
        hand = after['stock_raw']['game_state'].get('combat_state', {}).get('hand', ())
        count = sum(c.get('uuid') in pending for c in hand)
        if count != declared['card_soul_cost_reset_count']:
            raise ValueError('adapter Soul count disagrees with independent stock objects')
        measured['card_soul_cost_reset_count'] = count
    return measured


def replay(row: dict) -> dict:
    path = Path(row["stock_capture"])
    if hashlib.sha256(path.read_bytes()).hexdigest() != row["sha256"]:
        raise ValueError("stock raw system capture hash mismatch")
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(row["seed"])
    first = None
    transitions = 0
    reward_boundaries = 0
    restore_checks = 0
    records = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]
    if not records or len(records) != row['boundaries']:
        raise ValueError('missing system boundaries or inconsistent capture count')
    if row.get('terminal') and (not records[-1]['terminal'] or records[-1]['requested_action'] is not None):
        raise ValueError('declared completion lacks a terminal stock boundary')
    actor_terminal = None
    if 'actor_trajectory_sha256' in row:
        actor_path = path.with_suffix('.actor.jsonl')
        if hashlib.sha256(actor_path.read_bytes()).hexdigest() != row['actor_trajectory_sha256']:
            raise ValueError('stock actor trajectory identity mismatch')
        actor_records = [json.loads(line) for line in actor_path.read_text(encoding='utf-8').splitlines()]
        if len(actor_records) != len(records) + 1 or not actor_records[-1]['terminal']:
            raise ValueError('stock actor completion is missing')
        actor_terminal = actor_records[-1]
    elif row.get('terminal'):
        # Legacy v7 captures have no actor sidecar. Certify death only from
        # independent stock engine truth; do not infer success from the adapter.
        raw = records[-1]['stock_raw']
        game = raw['game_state']
        if (game.get('screen_type') == 'GAME_OVER'
                and game.get('screen_state', {}).get('victory') is False
                and game.get('current_hp') == 0
                and raw.get('_stock_direct', {}).get('player', {}).get('current_hp') == 0
                and raw.get('_continuation', {}).get('continuation_kind') == 'DEATH'):
            actor_terminal = {'terminal_reason': 'DEATH', 'success': False}
    native_terminal_info = {}
    timing_conditions = []
    for index, stock in enumerate(records):
        if pending_stock_intents(stock['stock_raw']):
            first = {'boundary': index, 'classification': 'UNSTABLE_STOCK_BOUNDARY',
                     'pending_monster_slots': pending_stock_intents(stock['stock_raw'])}
            break
        original = canonical_projection(stock["observation"], stock["actions"], stock["terminal"])
        original["rng"] = stock["stock_raw"]["_rng"]
        snapshot = backend.validation_snapshot()
        actual = canonical_projection(decision.observation.to_dict(), [a.to_dict() for a in decision.actions], decision.terminal)
        actual["rng"] = dict(snapshot.rng_streams)
        if snapshot.public_state.get("combat_state"):
            if "_stock_direct" not in stock["stock_raw"]:
                raise ValueError("stock combat lacks independent object evidence")
            original["stock_objects"] = direct_projection(stock["stock_raw"], stock=True)
            actual["stock_objects"] = direct_projection({"game_state": snapshot.public_state}, stock=False)
        differences = structured_differences(original, actual)
        if differences:
            first = {"boundary": index, "differences": differences,
                     "screen": stock["observation"]["screen"], "stock": original, "native": actual}
            break
        transitions += stock["observation"]["run"]["act"] == 2
        reward_boundaries += stock["observation"]["screen"] in {"COMBAT_REWARD", "CARD_REWARD", "BOSS_REWARD"}
        action = stock["requested_action"]
        if action is None:
            break
        if row["script_action_unavailable"] == index:
            break
        if action not in stock["actions"]:
            raise ValueError("requested stock action was not legal and not declared unavailable")
        action = mapped_action(action, stock["observation"], decision.observation.to_dict())
        checkpoint = backend.checkpoint()
        restored = SimulatorBackend(IRONCLAD_A20_ACT2)
        restored_decision = restored.load_checkpoint(json.loads(json.dumps(checkpoint)))
        if json.dumps(restored.checkpoint(), sort_keys=True) != json.dumps(checkpoint, sort_keys=True):
            first = {"boundary": index, "classification": "CHECKPOINT_RESTORATION_DIFFERENCE"}
            break
        current_action = next(a for a in decision.actions if a.to_dict() == action)
        restored_action = next(a for a in restored_decision.actions if a.to_dict() == action)
        evidence = stock_runtime_inputs(stock, records[index + 1]) if index + 1 < len(records) else {}
        if evidence:
            timing_conditions.append({"boundary": index, "stock_measured_inputs": evidence})
        transition = backend.step(current_action, validation_evidence=evidence)
        decision = transition.decision
        native_terminal_info = transition.info
        restored.step(restored_action, validation_evidence=evidence)
        if restored.checkpoint() != backend.checkpoint():
            first = {"boundary": index, "classification": "CHECKPOINT_CONTINUATION_DIFFERENCE"}
            break
        restore_checks += 1
    complete = bool(row.get("terminal")) and row["script_action_unavailable"] is None
    terminal_reason_verified = False
    if first is None and complete and actor_terminal is not None:
        stock_outcome = {'reason': actor_terminal['terminal_reason'], 'success': actor_terminal['success']}
        native_outcome = {'reason': native_terminal_info.get('reason'),
                          'success': bool(native_terminal_info.get('success'))}
        if stock_outcome != native_outcome:
            first = {'boundary': index, 'classification': 'TERMINATION_REASON_DIFFERENCE',
                     'stock': stock_outcome, 'native': native_outcome}
        else:
            terminal_reason_verified = True
    return {"seed": row["seed"], "first_divergence": first, "boundaries_checked": index + 1,
            "act2_boundaries": transitions, "reward_boundaries": reward_boundaries,
            "checkpoint_replays": restore_checks, "stock_action_unavailable": row["script_action_unavailable"],
            "terminal_reason_verified": terminal_reason_verified,
            "controlled_runtime_inputs": timing_conditions,
            "qualification_scope": "STOCK_MEASURED_TIMING_CONDITIONAL; NOT_PRODUCTION_RNG_CERTIFICATION",
            "status": ("UNSTABLE_STOCK_BOUNDARY" if first.get('classification') == 'UNSTABLE_STOCK_BOUNDARY'
                       else "SEMANTIC_DIFFERENCE") if first else (
                "SYSTEM_BRANCH_MATCH" if complete else "INCOMPLETE_PREFIX_MATCH")}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite system differential evidence")
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("stale native artifact")
    capture = json.loads(args.capture.read_text(encoding="utf-8"))
    if not capture.get("execution_complete") or capture.get("execution_error"):
        raise ValueError("stock system execution did not complete")
    if capture.get('original_execution_contract') != ORIGINAL_EXECUTION_CONTRACT:
        raise ValueError('stale original execution contract; recapture stable boundaries')
    launch_path = args.capture.with_suffix(".launch.json")
    launch = json.loads(launch_path.read_text(encoding="utf-8"))
    if (launch["mode"] != "validation" or launch["recovery_status"] != "RECOVERED"
            or launch["completion"]["exit_code"] != 0):
        raise ValueError("stock system launch/recovery did not complete")
    rows = [replay(row) for row in capture["runs"]]
    args.output.write_text(json.dumps({"schema": "sls-act2-system-differential-v1",
        "native_source_sha256": native_source_digest(), "runs": rows,
        "stock_capture_sha256": hashlib.sha256(args.capture.read_bytes()).hexdigest(),
        "launch_evidence_sha256": hashlib.sha256(launch_path.read_bytes()).hexdigest(),
        "oracle_build_sha256": launch["oracle_sha256"],
        "training_gate": "NOT_QUALIFIED"}, indent=2) + "\n", encoding="utf-8")
    print(json.dumps([{k: r[k] for k in ("seed", "status", "boundaries_checked")} for r in rows]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
