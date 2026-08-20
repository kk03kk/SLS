"""CommunicationMod entry point for a non-acceptance Original payload survey."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport
from sls.backends.simulator import IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART
from sls.validation.policies import PRIORITY
from sls.validation.truth import TruthBundleRecorder, file_hash, native_build_metadata


PROFILES = {p.profile_id: p for p in (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
)}


def choose_original(decision, variant: int):
    if not decision.actions:
        return None
    ordered = sorted(
        decision.actions,
        key=lambda action: (PRIORITY.get(action.kind, 1000), action.candidate_id),
    )
    return ordered[variant % len(ordered)]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--profile", choices=sorted(PROFILES), default="IRONCLAD_A0_HEART")
    parser.add_argument("--max-steps", type=int, default=30)
    parser.add_argument("--max-act", type=int, default=1)
    parser.add_argument("--variant", type=int, default=0)
    parser.add_argument("--truth-root", type=Path, default=ROOT / "validation-results" / "survey")
    args = parser.parse_args()
    game_root = Path(os.environ.get("SLS_GAME_ROOT", r"D:\Steam\steamapps\common\SlayTheSpire"))
    workshop = game_root.parents[1] / "workshop" / "content" / "646570"
    recorder = TruthBundleRecorder(
        args.truth_root, seed=args.seed, profile_id=args.profile,
        policy_id=f"original-survey-v1:{args.variant}", evidence_class="LIVE_FULLRUN",
        capture_mode="ORIGINAL_SURVEY", acceptance_eligible=False,
        instrumentation_schema="spirecomm-parity-v10", repository_root=ROOT,
        autosave=game_root / "saves" / "IRONCLAD.autosave",
        jar_paths={
            "game": game_root / "desktop-1.0.jar",
            "ModTheSpire": workshop / "1605060445" / "ModTheSpire.jar",
            "BaseMod": workshop / "1605833019" / "BaseMod.jar",
            "CommunicationMod": workshop / "2131373661" / "CommunicationMod.jar",
            "Oracle": game_root / "mods" / "SpirecommParity.jar",
        },
        policy_hash=file_hash(ROOT / "src" / "sls" / "validation" / "policies.py"),
        native_build=native_build_metadata(ROOT),
        launch={
            "java": str(game_root / "jre" / "bin" / "javaw.exe"),
            "skip_launcher": True, "skip_intro": os.environ.get("SLS_SKIP_INTRO", "1") == "1",
            "fps_limit": int(os.environ.get("SLS_ORIGINAL_FPS", "60")),
            "mods": ["basemod", "CommunicationMod", "spirecomm-parity"],
        },
    )
    transport = StdioTransport(event_sink=recorder.record_protocol)
    backend = OriginalBackend(OriginalSession(transport), PROFILES[args.profile])
    complete = False
    error = None
    outcome = None
    try:
        decision = backend.reset(args.seed)
        for sequence in range(args.max_steps + 1):
            terminal = decision.terminal or decision.observation.run.act > args.max_act
            action = None if terminal else choose_original(decision, args.variant)
            commands = () if action is None else backend.command_sequence(action)
            recorder.record_survey_boundary(
                sequence=sequence, original_payload=backend.raw_payload,
                original_decision=decision, action=action, commands=commands,
                terminal_kind="SURVEY_LIMIT" if decision.observation.run.act > args.max_act else None,
            )
            if terminal:
                complete = True
                outcome = "SURVEY_LIMIT" if not decision.terminal else "TERMINAL"
                break
            if sequence == args.max_steps:
                outcome = "STEP_LIMIT"
                break
            decision = backend.step(action).decision
            recorder.mark_last_action_executed(
                backend.last_executed_commands, backend.last_validation_evidence,
            )
    except Exception as exception:
        error = f"{type(exception).__name__}: {exception}"
    finally:
        try:
            backend.return_to_menu()
        except Exception as exception:
            error = error or f"return_to_menu failed: {exception}"
    bundle = recorder.finalize(complete=complete, outcome=outcome, error=error)
    print(f"SURVEY_BUNDLE {bundle}", file=sys.stderr, flush=True)
    return 0 if error is None else 2


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        result = main()
    except BaseException as error:
        write_completion(2, entry="survey", error=f"{type(error).__name__}: {error}", argv=sys.argv)
        raise
    else:
        write_completion(result, entry="survey")
        raise SystemExit(result)
