"""Preflight, install, and recover the authoritative Original runtime."""

from __future__ import annotations

import argparse
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.validation.runtime import prepare_runtime, recover_pending


def main() -> int:
    local = Path.home() / "AppData" / "Local" / "ModTheSpire"
    parser = argparse.ArgumentParser()
    parser.add_argument("--game-root", type=Path, default=Path(r"D:\Steam\steamapps\common\SlayTheSpire"))
    parser.add_argument("--config", type=Path, default=local / "CommunicationMod" / "config.properties")
    parser.add_argument("--python", type=Path, default=Path(r"D:\Anaconda\envs\DL\python.exe"))
    parser.add_argument("--profile", default="IRONCLAD_A0_HEART")
    parser.add_argument("--max-steps", type=int, default=10)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--recover", action="store_true")
    parser.add_argument("--segment-bundle", type=Path)
    parser.add_argument("--anchor")
    parser.add_argument("--to-step", type=int)
    args = parser.parse_args()
    journals = ROOT / "validation-results" / "runtime-journals"
    if args.recover:
        for journal in recover_pending(journals):
            print(f"recovered {journal}")
        return 0
    recover_pending(journals)
    save = args.game_root / "saves" / "IRONCLAD.autosave"
    entry = None
    entry_args: tuple[str, ...] = ()
    if args.segment_bundle:
        if not args.anchor or args.to_step is None:
            parser.error("--segment-bundle requires --anchor and --to-step")
        entry = ROOT / "tools" / "replay_original_segment.py"
        entry_args = (
            args.segment_bundle.resolve().as_posix(), "--anchor", args.anchor,
            "--to-step", str(args.to_step), "--game-root", args.game_root.as_posix(),
        )
    journal = prepare_runtime(
        repository=ROOT, game_root=args.game_root, config=args.config,
        python=args.python, max_steps=args.max_steps, profile=args.profile,
        save_files=(save, Path(str(save) + ".backUp")), install=not args.check,
        entry=entry, entry_args=entry_args,
    )
    print(journal)
    if args.check:
        # A check must not leave a journal claiming it owns unchanged files.
        recover_pending(journals)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
