"""CommunicationMod entry point for one canonical paired FullRun validation."""

from __future__ import annotations

import argparse
from functools import partial
import os
from pathlib import Path
import sys
from contextlib import nullcontext


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.validation import TruthBundleRecorder, run_paired
from sls.validation.policies import deterministic_action
from sls.validation.truth import file_hash, native_build_metadata
from sls.validation.runtime import OriginalRuntimeGuard


PROFILES = CURRICULUM_PROFILES_BY_ID


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=int(os.environ.get("SLS_PARITY_SEED", "0")))
    parser.add_argument(
        "--profile",
        choices=sorted(PROFILES),
        default=os.environ.get("SLS_PARITY_PROFILE", "IRONCLAD_A0_HEART"),
    )
    parser.add_argument("--max-steps", type=int, default=int(os.environ.get("SLS_PARITY_MAX_STEPS", "10000")))
    parser.add_argument("--variant", type=int, default=0)
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(os.environ.get("SLS_PARITY_OUTPUT", "validation-results/full-run.json")),
    )
    parser.add_argument("--without-rng", action="store_true")
    parser.add_argument("--truth-root", type=Path)
    parser.add_argument("--runtime-journal", type=Path)
    parser.add_argument("--require-clean", action="store_true")
    args = parser.parse_args()
    if args.require_clean:
        import subprocess
        status = subprocess.run(
            ["git", "status", "--porcelain"], cwd=ROOT, check=True,
            capture_output=True, text=True,
        ).stdout
        if status.strip():
            parser.error("--require-clean refuses an authoritative capture from a dirty worktree")
    profile = PROFILES[args.profile]
    game_root = Path(os.environ.get("SLS_GAME_ROOT", r"D:\Steam\steamapps\common\SlayTheSpire"))
    external = ROOT / "external" / "original-game"
    workshop = game_root.parents[1] / "workshop" / "content" / "646570"
    jar_paths = {
        "game": game_root / "desktop-1.0.jar",
        "ModTheSpire": workshop / "1605060445" / "ModTheSpire.jar",
        "BaseMod": workshop / "1605833019" / "BaseMod.jar",
        "CommunicationMod": workshop / "2131373661" / "CommunicationMod.jar",
        "Oracle": game_root / "mods" / "SpirecommParity.jar",
    }
    recorder = None
    if args.truth_root:
        recorder = TruthBundleRecorder(
            args.truth_root.resolve(), seed=args.seed, profile_id=profile.profile_id,
            policy_id=f"deterministic-action-v1:variant-{args.variant}", repository_root=ROOT,
            jar_paths=jar_paths, autosave=game_root / "saves" / "IRONCLAD.autosave",
            instrumentation_schema="spirecomm-parity-v10",
            policy_hash=file_hash(ROOT / "src" / "sls" / "validation" / "policies.py"),
            native_build=native_build_metadata(ROOT),
            launch={
                "java": str(game_root / "jre" / "bin" / "javaw.exe"),
                "skip_launcher": True, "skip_intro": os.environ.get("SLS_SKIP_INTRO", "1") == "1",
                "fps_limit": int(os.environ.get("SLS_ORIGINAL_FPS", "60")),
                "mods": ["basemod", "CommunicationMod", "spirecomm-parity"],
            },
        )
    protocol_log = os.environ.get("SLS_PROTOCOL_LOG")
    transport = StdioTransport(
        log_path=Path(protocol_log).resolve() if protocol_log else None,
        event_sink=None if recorder is None else recorder.record_protocol,
    )
    guard = OriginalRuntimeGuard(args.runtime_journal) if args.runtime_journal else nullcontext()
    with guard:
        original = OriginalBackend(OriginalSession(transport), profile)
        try:
            trace = run_paired(
                original, SimulatorBackend(profile), seed=args.seed, max_steps=args.max_steps,
                include_rng=not args.without_rng, recorder=recorder,
                selector=partial(deterministic_action, variant=args.variant),
            )
        finally:
            original.return_to_menu()
        if recorder is not None:
            outcome = trace.steps[-1].terminal_kind if trace.steps else None
            bundle = recorder.finalize(complete=trace.complete, outcome=outcome, error=trace.error)
            print(f"TRUTH_BUNDLE {bundle}", file=sys.stderr, flush=True)
    path = trace.write(args.output)
    print(
        f"PARITY seed={args.seed} steps={len(trace.steps)} "
        f"complete={trace.complete} matches={trace.matches} output={path}",
        file=sys.stderr,
        flush=True,
    )
    return 0 if trace.matches and trace.complete else 1


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        result = main()
    except BaseException as error:
        write_completion(2, entry="capture", error=f"{type(error).__name__}: {error}", argv=sys.argv)
        raise
    else:
        write_completion(result, entry="capture")
        raise SystemExit(result)
