from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

from run_original_canary import (
    BackupJournal,
    _all_user_files,
    _recover_pending,
    launcher_command,
)

ROOT = Path(__file__).resolve().parents[1]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run a recoverable stock-game card parity batch.",
    )
    parser.add_argument("--oracle", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--cards", nargs="*")
    parser.add_argument("--timeout", type=int, default=1800)
    parser.add_argument(
        "--game-root", type=Path,
        default=Path(r"D:\Steam\steamapps\common\SlayTheSpire"),
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    local = Path(os.environ["LOCALAPPDATA"]) / "ModTheSpire"
    runtime_root = ROOT / "local" / "runs" / "stock-audit" / "runtime-backups"
    _recover_pending(runtime_root)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    run_root = runtime_root / stamp
    journal = BackupJournal(run_root / "journal.json")
    config = local / "CommunicationMod" / "config.properties"
    mod_list = local / "mod_lists.json"
    display = args.game_root / "info.displayconfig"
    mod_dir = args.game_root / "mods"
    target_oracle = mod_dir / "SpirecommParity.jar"
    mts = (
        args.game_root.parents[1] / "workshop" / "content" / "646570"
        / "1605060445" / "ModTheSpire.jar"
    )
    extra_mods = [path for path in mod_dir.glob("*.jar") if path != target_oracle]
    required = [args.oracle, mts, display]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        journal.restore()
        raise FileNotFoundError("missing Original audit prerequisite: " + ", ".join(missing))
    for target in [
        config, mod_list, display, target_oracle, *extra_mods,
        *_all_user_files(args.game_root),
    ]:
        journal.backup(target)
    completion = run_root / "completion.json"
    stdout_path = run_root / "original.stdout.log"
    stderr_path = run_root / "original.stderr.log"
    process: subprocess.Popen[bytes] | None = None
    marker = None
    try:
        mod_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(args.oracle, target_oracle)
        for path in extra_mods:
            path.unlink()
        command = [
            Path(sys.executable).resolve().as_posix(),
            (ROOT / "tools" / "capture_original_card_batch.py").as_posix(),
            "--output", args.output.resolve().as_posix(),
        ]
        if args.cards:
            command.extend(("--cards", *args.cards))
        config.parent.mkdir(parents=True, exist_ok=True)
        config.write_text(
            "command=" + " ".join(command).replace(":", "\\:")
            + "\nrunAtGameStart=true\nverbose=true\n",
            encoding="utf-8",
        )
        mod_list.write_text(json.dumps({
            "defaultList": "<Default>",
            "lists": {"<Default>": [
                "BaseMod.jar", "CommunicationMod.jar", "SpirecommParity.jar",
            ]},
        }, indent=2) + "\n", encoding="utf-8")
        lines = display.read_text(encoding="utf-8").splitlines()
        if len(lines) < 6:
            raise ValueError("invalid Original display config")
        lines[2] = "60"
        display.write_text("\n".join(lines[:6]) + "\n", encoding="utf-8")
        environment = os.environ.copy()
        environment["SLS_RUN_COMPLETION"] = str(completion)
        with stdout_path.open("wb") as stdout, stderr_path.open("wb") as stderr:
            process = subprocess.Popen(
                launcher_command(args.game_root, mts), cwd=args.game_root,
                env=environment, stdout=stdout, stderr=stderr,
            )
            journal.data["pid"] = process.pid
            journal._flush()
            deadline = time.monotonic() + args.timeout
            while time.monotonic() < deadline:
                if completion.is_file():
                    marker = json.loads(completion.read_text(encoding="utf-8"))
                    break
                if process.poll() is not None:
                    raise RuntimeError(
                        "Original exited before audit completion: "
                        f"{process.returncode}",
                    )
                time.sleep(0.25)
            if marker is None:
                raise TimeoutError(f"Original audit timed out after {args.timeout}s")
    finally:
        try:
            journal.restore()
        finally:
            if process is not None and process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=15)
    print(json.dumps({
        "completion": marker,
        "runtime_journal": str(journal.path),
        "stdout": str(stdout_path),
        "stderr": str(stderr_path),
    }, indent=2))
    return int((marker or {}).get("exit_code", 2))


if __name__ == "__main__":
    raise SystemExit(main())
