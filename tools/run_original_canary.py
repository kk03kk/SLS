"""Launch one recoverable Original-game policy trajectory canary."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


class BackupJournal:
    """Durable, idempotent backup of every user/runtime file we mutate."""

    def __init__(self, path: Path) -> None:
        self.path = path
        self.data: dict[str, Any] = {"status": "ACTIVE", "entries": []}
        self._flush()

    @classmethod
    def open(cls, path: Path) -> BackupJournal:
        instance = cls.__new__(cls)
        instance.path = path
        instance.data = json.loads(path.read_text(encoding="utf-8"))
        return instance

    def _flush(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        temporary = self.path.with_suffix(".tmp")
        temporary.write_text(
            json.dumps(self.data, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        os.replace(temporary, self.path)

    def backup(self, target: Path) -> None:
        target = target.resolve()
        if any(Path(entry["target"]) == target for entry in self.data["entries"]):
            return
        existed = target.is_file()
        backup = self.path.parent / "backups" / f"{len(self.data['entries']):04d}-{target.name}"
        if existed:
            backup.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(target, backup)
        self.data["entries"].append({
            "target": str(target), "backup": str(backup), "existed": existed,
            "sha256": _sha256(target) if existed else None,
        })
        self._flush()

    def restore(self) -> None:
        failures: list[str] = []
        for entry in reversed(self.data["entries"]):
            target, backup = Path(entry["target"]), Path(entry["backup"])
            try:
                if entry["existed"]:
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(backup, target)
                    if _sha256(target) != entry["sha256"]:
                        raise OSError("restored hash mismatch")
                elif target.exists():
                    target.unlink()
            except OSError as error:
                failures.append(f"{target}: {error}")
        self.data["status"] = "RECOVERY_FAILED" if failures else "RECOVERED"
        self.data["recovery_failures"] = failures
        self._flush()
        if failures:
            raise RuntimeError("Original runtime recovery failed: " + "; ".join(failures))


def launcher_command(game_root: Path, mod_the_spire: Path) -> list[str]:
    return [
        str(game_root / "jre" / "bin" / "javaw.exe"),
        "-Xmx2G", "-Dfile.encoding=UTF-8", "-jar", str(mod_the_spire),
        "--skip-launcher", "--skip-intro", "--mods",
        "basemod,CommunicationMod,spirecomm-parity",
    ]


def _all_user_files(game_root: Path) -> list[Path]:
    return [
        path for name in ("preferences", "betaPreferences", "saves")
        for path in (game_root / name).rglob("*") if path.is_file()
    ]


def _recover_pending(root: Path) -> None:
    for path in sorted(root.glob("*/journal.json")):
        journal = BackupJournal.open(path)
        if journal.data.get("status") in {"ACTIVE", "RECOVERY_FAILED"}:
            pid = int(journal.data.get("pid") or 0)
            if pid:
                query = subprocess.run(
                    ["powershell", "-NoProfile", "-Command", f"Get-Process -Id {pid} -ErrorAction SilentlyContinue"],
                    capture_output=True,
                )
                if query.returncode == 0 and query.stdout:
                    raise RuntimeError(f"owned Original process {pid} is still alive; refusing recovery")
            journal.restore()


def main() -> int:
    local = Path(os.environ["LOCALAPPDATA"]) / "ModTheSpire"
    parser = argparse.ArgumentParser()
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--action-journal", type=Path, required=True)
    parser.add_argument("--max-actions", type=int)
    parser.add_argument("--timeout", type=int, default=1800)
    parser.add_argument("--game-root", type=Path, default=Path(r"D:\Steam\steamapps\common\SlayTheSpire"))
    parser.add_argument("--python", type=Path, default=Path(sys.executable))
    args = parser.parse_args()

    runtime_root = ROOT / "runs" / "canary" / "runtime-backups"
    _recover_pending(runtime_root)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    run_root = runtime_root / stamp
    journal = BackupJournal(run_root / "journal.json")
    config = local / "CommunicationMod" / "config.properties"
    mod_list = local / "mod_lists.json"
    display = args.game_root / "info.displayconfig"
    mod_dir = args.game_root / "mods"
    oracle = ROOT / ".build" / "oracle" / "SpirecommParity.jar"
    target_oracle = mod_dir / "SpirecommParity.jar"
    mts = args.game_root.parents[1] / "workshop" / "content" / "646570" / "1605060445" / "ModTheSpire.jar"
    required = [args.artifact, args.python, oracle, mts, display]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        journal.restore()
        raise FileNotFoundError("missing Original canary prerequisite: " + ", ".join(missing))
    extra_mods = [path for path in mod_dir.glob("*.jar") if path.name != target_oracle.name]
    for target in [config, mod_list, display, target_oracle, *extra_mods, *_all_user_files(args.game_root)]:
        journal.backup(target)
    completion = run_root / "completion.json"
    stdout_path = run_root / "original.stdout.log"
    stderr_path = run_root / "original.stderr.log"
    process: subprocess.Popen[bytes] | None = None
    marker: dict[str, Any] | None = None
    try:
        mod_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(oracle, target_oracle)
        for path in extra_mods:
            path.unlink()
        command = [
            args.python.resolve().as_posix(),
            (ROOT / "tools" / "capture_policy_trajectory.py").resolve().as_posix(),
            "original", args.artifact.resolve().as_posix(),
            "--seed", str(args.seed), "--output", args.output.resolve().as_posix(),
            "--journal", args.action_journal.resolve().as_posix(),
        ]
        if args.max_actions is not None:
            command.extend(("--max-actions", str(args.max_actions)))
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
                    raise RuntimeError(f"Original exited before canary completion: {process.returncode}")
                time.sleep(0.25)
            if marker is None:
                raise TimeoutError(f"Original canary timed out after {args.timeout}s")
    finally:
        # Restore protected saves/config while Java is alive, before Steam Cloud's exit scan.
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
        "completion": marker, "runtime_journal": str(journal.path),
        "stdout": str(stdout_path), "stderr": str(stderr_path),
    }, indent=2))
    return int((marker or {}).get("exit_code", 2))


if __name__ == "__main__":
    raise SystemExit(main())
