"""Restore an official autosave anchor and replay only its semantic suffix."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import shutil
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport
from sls.backends.simulator import IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART
from sls.contracts import Action
from sls.contracts.continuation import continuation_original
from sls.validation.compare import canonical_original
from sls.validation.diff import differences
from sls.validation.runtime import OriginalRuntimeGuard
from sls.validation.truth import load_bundle, resumable_original_boundary, value_hash


PROFILES = {p.profile_id: p for p in (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
)}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--anchor", required=True)
    parser.add_argument("--to-step", type=int, required=True)
    parser.add_argument("--runtime-journal", type=Path, required=True)
    parser.add_argument("--game-root", type=Path, default=Path(r"D:\Steam\steamapps\common\SlayTheSpire"))
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
        with OriginalRuntimeGuard(args.runtime_journal):
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
                restored_hash = value_hash(resumable_original_boundary(backend.raw_payload))
                expected_resume_hash = anchor.get("resume_boundary_hash") or value_hash(
                    resumable_original_boundary(boundaries[start]["raw_original_payload"])
                )
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
                for sequence in range(start, args.to_step):
                    action = boundaries[sequence].get("selected_action")
                    if not action:
                        raise ValueError(f"missing action at step {sequence}")
                    decision = backend.step(Action.from_dict(action)).decision
                expected = boundaries[args.to_step]
                expected_resumable = resumable_original_boundary(expected["raw_original_payload"])
                actual_resumable = resumable_original_boundary(backend.raw_payload)
                diff = differences(expected_resumable, actual_resumable)
                print(json.dumps({
                    "anchor": args.anchor, "target_step": args.to_step,
                    "anchor_hash_verified": True, "matches": not diff,
                    "resume_normalizations": actual_resumable["normalizations"],
                    "differences": diff,
                }, ensure_ascii=False), file=sys.stderr, flush=True)
                return 0 if not diff else 1
            finally:
                backend.return_to_menu()
    except (OSError, ValueError, KeyError, StopIteration, RuntimeError) as error:
        print(json.dumps({"valid": False, "error": str(error)}, ensure_ascii=False), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
