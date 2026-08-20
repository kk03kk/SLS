"""Restore an official autosave anchor and replay only its semantic suffix."""

from __future__ import annotations

import argparse
import gzip
import json
import os
from pathlib import Path
import shutil
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport
from sls.backends.simulator import (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
    SimulatorBackend,
)
from sls.contracts import Action
from sls.contracts.continuation import continuation_original
from sls.validation.compare import canonical_original, parity_differences
from sls.validation.diff import differences
from sls.validation.evidence import original_evidence_gaps
from sls.validation.policies import action_ids, deterministic_action
from contextlib import nullcontext
from sls.validation.runtime import OriginalRuntimeGuard
from sls.validation.truth import (
    TruthBundleRecorder, file_hash, load_bundle, native_build_metadata,
    resume_verification_boundary, value_hash,
)
PROFILES = {p.profile_id: p for p in (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
)}


def _restore_simulator_at_step(
    bundle: Path, manifest: dict, boundaries: list[dict], step: int,
) -> tuple[SimulatorBackend, object, dict, list[str]]:
    """Rebuild a target boundary from the nearest loadable native anchor."""

    failures: list[str] = []
    anchors = sorted(
        (a for a in manifest.get("anchors", []) if int(a["sequence"]) <= step),
        key=lambda a: int(a["sequence"]), reverse=True,
    )
    for candidate_anchor in anchors:
        try:
            anchor_dir = bundle / candidate_anchor["path"]
            metadata = json.loads((anchor_dir / "metadata.json").read_text(encoding="utf-8"))
            producer = metadata.get("checkpoint_producer") or {}
            producer_abi = producer.get("abi") or producer.get("python_abi")
            if producer_abi and producer_abi != sys.implementation.cache_tag:
                raise ValueError("native checkpoint ABI is incompatible")
            with gzip.open(
                anchor_dir / "simulator-checkpoint.json.gz", "rt", encoding="utf-8",
            ) as stream:
                checkpoint = json.load(stream)
            expected_hash = metadata.get("checkpoint_state_hash")
            if expected_hash and value_hash(checkpoint) != expected_hash:
                raise ValueError("native checkpoint state hash mismatch")
            simulator = SimulatorBackend(PROFILES[manifest["profile_id"]])
            decision = simulator.load_checkpoint(checkpoint)
            for sequence in range(int(candidate_anchor["sequence"]), step):
                selected = boundaries[sequence].get("selected_action")
                if not selected:
                    raise ValueError(f"missing action at step {sequence}")
                decision = simulator.step(Action.from_dict(selected)).decision
            return simulator, decision, candidate_anchor, failures
        except (OSError, ValueError, KeyError, RuntimeError, json.JSONDecodeError) as error:
            failures.append(f"{candidate_anchor['anchor_id']}: {error}")
    raise ValueError("no compatible native anchor: " + "; ".join(failures))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--anchor", required=True)
    parser.add_argument("--to-step", type=int, required=True)
    parser.add_argument("--runtime-journal", type=Path)
    parser.add_argument("--game-root", type=Path, default=Path(r"D:\Steam\steamapps\common\SlayTheSpire"))
    parser.add_argument("--truth-root", type=Path, default=ROOT / "validation-results" / "truth")
    parser.add_argument("--continue-steps", type=int, default=0)
    args = parser.parse_args()
    try:
        manifest, boundaries = load_bundle(args.bundle)
        anchor = next(a for a in manifest["anchors"] if a["anchor_id"] == args.anchor)
        anchor_dir = args.bundle / anchor["path"]
        source = anchor_dir / "original.autosave"
        if not source.is_file():
            raise ValueError("selected anchor has no official autosave")
        start = int(anchor["sequence"])
        if args.to_step < start or args.to_step >= len(boundaries):
            raise ValueError("target step is outside the anchor suffix")
        destination = args.game_root / "saves" / "IRONCLAD.autosave"
        transport = StdioTransport()
        session = OriginalSession(transport)
        backend = OriginalBackend(session, PROFILES[manifest["profile_id"]])
        guard = OriginalRuntimeGuard(args.runtime_journal) if args.runtime_journal else nullcontext()
        with guard:
            try:
                payload = session.connect()
                if payload.get("in_game"):
                    payload = session.execute("reset_run")
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, destination)
                source_backup = anchor_dir / "original.autosave.backUp"
                if source_backup.is_file():
                    shutil.copy2(source_backup, Path(str(destination) + ".backUp"))
                available = {str(item).lower() for item in session.payload.get("available_commands") or ()}
                if "parity_continue" not in available:
                    if "state" not in available:
                        raise RuntimeError("main menu cannot refresh autosave commands")
                    session.execute("state")
                decision = backend.resume()
                expected_gaps = original_evidence_gaps(
                    boundaries[start]["raw_original_payload"],
                    canonical_screen=boundaries[start]["cursor"]["screen"],
                )
                ignored_codes = [item["code"] for item in expected_gaps]
                restored_hash = value_hash(resume_verification_boundary(
                    backend.raw_payload, ignored_evidence_codes=ignored_codes,
                ))
                expected_resume_hash = value_hash(resume_verification_boundary(
                    boundaries[start]["raw_original_payload"],
                    ignored_evidence_codes=ignored_codes,
                ))
                if restored_hash != expected_resume_hash:
                    expected_boundary = boundaries[start]
                    state_diff = differences(
                        expected_boundary["canonical_public_state"], canonical_original(backend.raw_payload),
                    )
                    continuation_diff = differences(
                        expected_boundary["continuation"]["original"],
                        continuation_original(backend.raw_payload),
                    )
                    raise RuntimeError("anchor boundary hash mismatch: " + json.dumps({
                        "expected_hash": expected_resume_hash, "actual_hash": restored_hash,
                        "state_differences": dict(list(state_diff.items())[:20]),
                        "continuation_differences": dict(list(continuation_diff.items())[:20]),
                    }, ensure_ascii=False))
                simulator, simulator_decision, simulator_anchor, restore_failures = (
                    _restore_simulator_at_step(args.bundle, manifest, boundaries, start)
                )
                workshop = args.game_root.parents[1] / "workshop" / "content" / "646570"
                recorder = TruthBundleRecorder(
                    args.truth_root, seed=int(manifest["seed"]),
                    profile_id=manifest["profile_id"], policy_id=manifest["policy_id"],
                    evidence_class="RESUMED_AUTOSAVE", capture_mode="PAIRED",
                    acceptance_eligible=False, instrumentation_schema="spirecomm-parity-v4",
                    repository_root=ROOT, autosave=destination,
                    jar_paths={
                        "game": args.game_root / "desktop-1.0.jar",
                        "Oracle": args.game_root / "mods" / "SpirecommParity.jar",
                        "ModTheSpire": workshop / "1605060445" / "ModTheSpire.jar",
                        "BaseMod": workshop / "1605833019" / "BaseMod.jar",
                        "CommunicationMod": workshop / "2131373661" / "CommunicationMod.jar",
                    },
                    policy_hash=file_hash(ROOT / "src" / "sls" / "validation" / "policies.py"),
                    native_build=native_build_metadata(ROOT),
                    launch={
                        "java": str(args.game_root / "jre" / "bin" / "javaw.exe"),
                        "skip_launcher": True,
                        "skip_intro": os.environ.get("SLS_SKIP_INTRO", "1") == "1",
                        "fps_limit": int(os.environ.get("SLS_ORIGINAL_FPS", "60")),
                        "mods": ["basemod", "CommunicationMod", "spirecomm-parity"],
                    },
                    provenance={
                        "source_run_id": args.bundle.name, "source_anchor": args.anchor,
                        "source_start_step": start, "source_target_step": args.to_step,
                        "ignored_legacy_evidence_codes": ignored_codes,
                        "source_anchor_capability": "RESUME_VERIFIED",
                        "simulator_restore_mode": (
                            "EXACT_CHECKPOINT" if int(simulator_anchor["sequence"]) == start
                            else "ACTION_HISTORY"
                        ),
                        "simulator_restore_anchor": simulator_anchor["anchor_id"],
                        "simulator_restore_range": [int(simulator_anchor["sequence"]), start],
                        "simulator_restore_failures": restore_failures,
                    },
                )
                paired_match = True
                for source_sequence in range(start, args.to_step + 1):
                    observation_diff = differences(
                        decision.observation.to_dict(), simulator_decision.observation.to_dict(),
                    )
                    action_diff = differences(
                        action_ids(decision.actions), action_ids(simulator_decision.actions),
                    )
                    state_diff = parity_differences(
                        backend.raw_payload, simulator.raw_state, drop_dead_neow=True,
                    )
                    selected = boundaries[source_sequence].get("selected_action") if source_sequence < args.to_step else None
                    semantic = None if selected is None else Action.from_dict(selected)
                    commands = () if semantic is None else backend.command_sequence(semantic)
                    record = recorder.record_boundary(
                        sequence=source_sequence - start, original_payload=backend.raw_payload,
                        original_decision=decision, simulator_state=simulator.raw_state,
                        simulator_decision=simulator_decision, action=semantic, commands=commands,
                        observation_diff=observation_diff, action_diff=action_diff,
                        state_diff=state_diff, checkpoint=simulator.checkpoint(), terminal_kind=None,
                    )
                    if source_sequence == start:
                        recorder.mark_initial_resume_verified(
                            source,
                            anchor_dir / "original.autosave.backUp",
                            source_run_id=args.bundle.name, source_anchor_id=args.anchor,
                        )
                    paired_match = paired_match and record["comparison"]["status"] == "MATCH"
                    if source_sequence == args.to_step:
                        break
                    if semantic is None:
                        raise ValueError(f"missing action at step {source_sequence}")
                    decision = backend.step(semantic).decision
                    simulator_decision = simulator.step(semantic).decision
                    recorder.mark_last_action_executed(backend.last_executed_commands)
                target_payload = json.loads(json.dumps(backend.raw_payload))
                continued = 0
                for _ in range(args.continue_steps):
                    if not paired_match or decision.terminal or simulator_decision.terminal:
                        break
                    semantic = deterministic_action(decision, simulator_decision)
                    commands = backend.command_sequence(semantic)
                    recorder.select_last_action(semantic, commands)
                    decision = backend.step(semantic).decision
                    simulator_decision = simulator.step(semantic).decision
                    recorder.mark_last_action_executed(backend.last_executed_commands)
                    continued += 1
                    observation_diff = differences(
                        decision.observation.to_dict(), simulator_decision.observation.to_dict(),
                    )
                    action_diff = differences(
                        action_ids(decision.actions), action_ids(simulator_decision.actions),
                    )
                    state_diff = parity_differences(
                        backend.raw_payload, simulator.raw_state, drop_dead_neow=True,
                    )
                    record = recorder.record_boundary(
                        sequence=args.to_step - start + continued,
                        original_payload=backend.raw_payload, original_decision=decision,
                        simulator_state=simulator.raw_state, simulator_decision=simulator_decision,
                        action=None, commands=(), observation_diff=observation_diff,
                        action_diff=action_diff, state_diff=state_diff,
                        checkpoint=simulator.checkpoint(), terminal_kind=None,
                    )
                    paired_match = record["comparison"]["status"] == "MATCH"
                derived_bundle = recorder.finalize(
                    complete=False,
                    outcome="RESUMED_WINDOW" if paired_match else "FIRST_DIFFERENCE",
                    error=None,
                )
                expected = boundaries[args.to_step]
                target_gaps = original_evidence_gaps(
                    expected["raw_original_payload"], canonical_screen=expected["cursor"]["screen"],
                )
                target_ignored = [item["code"] for item in target_gaps]
                expected_resumable = resume_verification_boundary(
                    expected["raw_original_payload"], ignored_evidence_codes=target_ignored,
                )
                actual_resumable = resume_verification_boundary(
                    target_payload, ignored_evidence_codes=target_ignored,
                )
                diff = differences(expected_resumable, actual_resumable)
                print(json.dumps({
                    "anchor": args.anchor, "target_step": args.to_step,
                    "anchor_hash_verified": True, "matches": not diff,
                    "resume_normalizations": actual_resumable["normalizations"],
                    "differences": diff, "paired_matches": paired_match,
                    "derived_bundle": str(derived_bundle),
                    "ignored_legacy_evidence_codes": target_ignored,
                    "continued_steps": continued,
                }, ensure_ascii=False), file=sys.stderr, flush=True)
                return 0 if not diff and paired_match else 1
            finally:
                backend.return_to_menu()
    except (OSError, ValueError, KeyError, StopIteration, RuntimeError) as error:
        print(json.dumps({"valid": False, "error": str(error)}, ensure_ascii=False), file=sys.stderr)
        from sls.validation.runtime import write_completion
        write_completion(2, entry="resume", error=f"{type(error).__name__}: {error}")
        return 2


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        result = main()
    except BaseException as error:
        write_completion(
            2, entry="resume", error=f"{type(error).__name__}: {error}", argv=sys.argv,
        )
        raise
    else:
        marker = os.environ.get("SLS_RUN_COMPLETION")
        if not marker or not Path(marker).is_file():
            write_completion(result, entry="resume")
        raise SystemExit(result)
