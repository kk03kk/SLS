"""Inspect a source-built Oracle and optionally run a recoverable bounded game smoke."""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
import zipfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools.build_oracle import ORACLE, resource_payloads, sha256, source_files
from tools.configure_live_inspector import _command_path
from tools.run_original_canary import (
    BackupJournal,
    _all_user_files,
    _recover_pending,
    launcher_command,
    original_runtime_paths,
)


def inspect(oracle: Path) -> dict:
    report = json.loads(oracle.with_suffix(".build.json").read_text(encoding="utf-8"))
    if report.get("schema") != "sls-oracle-build-v1" or report.get("used_existing_oracle") is not False:
        raise ValueError("Oracle lacks full source-build evidence")
    if sha256(oracle) != report["output_sha256"]:
        raise ValueError("Oracle output SHA256 mismatch")
    import hashlib

    current = {path.relative_to(ORACLE).as_posix(): sha256(path)
               for path in source_files() + [ORACLE / "ModTheSpire.json"]}
    current.update({"resources/" + name: hashlib.sha256(payload).hexdigest()
                    for name, payload in resource_payloads().items() if name != "ModTheSpire.json"})
    if current != report.get("sources"):
        raise ValueError("Oracle source/build identity mismatch; rebuild from current source")
    with zipfile.ZipFile(oracle) as archive:
        if set(archive.namelist()) != set(report["members"]):
            raise ValueError("Oracle member inventory mismatch")
        if any(hashlib.sha256(archive.read(name)).hexdigest() != digest for name, digest in report["members"].items()):
            raise ValueError("Oracle member SHA256 mismatch")
    return report


def require_no_running_game(game: Path, mts: Path) -> None:
    query = subprocess.run([
        "powershell", "-NoProfile", "-Command",
        "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
        "@(Get-CimInstance Win32_Process | Where-Object { "
        "$_.Name -in @('java.exe','javaw.exe','SlayTheSpire.exe') } | "
        "Select-Object ProcessId,Name,CommandLine) | ConvertTo-Json -Compress",
    ], check=True, capture_output=True, text=True, encoding="utf-8-sig")
    processes = json.loads(query.stdout.strip() or "[]")
    if isinstance(processes, dict):
        processes = [processes]
    identifiers = (game.as_posix().lower(), mts.as_posix().lower())
    for process in processes:
        command = str(process.get("CommandLine") or "").replace("\\", "/").lower()
        if process.get("Name", "").lower() == "slaythespire.exe" or any(value in command for value in identifiers):
            raise RuntimeError("close the running game before Oracle qualification; no files changed")


def install(oracle: Path, game_root: Path | None) -> dict:
    report = inspect(oracle)
    _, game = original_runtime_paths(game_root)
    require_no_running_game(game, game.parents[1] / "workshop/content/646570/1605060445/ModTheSpire.jar")
    installed = game / "mods/SpirecommParity.jar"
    if installed.is_symlink():
        raise ValueError("refuse to replace a symlinked Oracle")
    previous = sha256(installed) if installed.is_file() else None
    if previous == report["output_sha256"]:
        return {"status": "ALREADY_INSTALLED", "sha256": previous}
    backups = ROOT / "local/build/oracle/installed-backups"
    backups.mkdir(parents=True, exist_ok=True)
    backup = backups / f"SpirecommParity-{previous}.jar" if previous else None
    if backup is not None:
        if backup.exists() and sha256(backup) != previous:
            raise ValueError("existing backup hash mismatch")
        if not backup.exists():
            shutil.copy2(installed, backup)
        if sha256(backup) != previous:
            raise ValueError("backup verification failed")
    installed.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=installed.parent, prefix=".oracle-install-") as directory:
        staged = Path(directory) / "candidate.tmp"
        shutil.copy2(oracle, staged)
        if sha256(staged) != report["output_sha256"]:
            raise ValueError("candidate copy hash mismatch")
        os.replace(staged, installed)
    result = {"status": "INSTALLED", "sha256": sha256(installed),
              "previous_sha256": previous, "backup": str(backup) if backup else None,
              "installed": str(installed)}
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    (backups / f"installation-{stamp}.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    return result


def runtime_smoke(oracle: Path, output: Path, mode: str, game_root: Path | None, timeout: float) -> dict:
    if output.exists() or output.with_suffix(".launch.json").exists():
        raise FileExistsError("runtime output exists; choose a new evidence path")
    local, game = original_runtime_paths(game_root)
    mts = game.parents[1] / "workshop/content/646570/1605060445/ModTheSpire.jar"
    require_no_running_game(game, mts)
    display = game / "info.displayconfig"
    for path in (mts, display, game / "jre/bin/javaw.exe"):
        if not path.is_file():
            raise FileNotFoundError(path)
    recovery = ROOT / "local/runs/oracle-qualification/runtime-backups"
    _recover_pending(recovery)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    run = recovery / stamp
    journal = BackupJournal(run / "journal.json")
    config, mod_list = local / "CommunicationMod/config.properties", local / "mod_lists.json"
    mod_dir = game / "mods"
    installed = mod_dir / "SpirecommParity.jar"
    others = [p for p in mod_dir.glob("*.jar") if p != installed]
    for target in [config, mod_list, display, installed, *others, *_all_user_files(game)]:
        journal.backup(target)
    completion = run / "completion.json"
    process = None
    marker = None
    try:
        mod_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(oracle, installed)
        for path in others:
            path.unlink()
        config.parent.mkdir(parents=True, exist_ok=True)
        command = " ".join([_command_path(Path(sys.executable)),
                            _command_path(ROOT / "tools/capture_oracle_smoke.py"),
                            "--mode", mode, "--output", _command_path(output)])
        config.write_text(f"command={command}\nrunAtGameStart=true\nverbose=true\n", encoding="utf-8")
        mod_list.write_text(json.dumps({"defaultList": "<Default>", "lists": {"<Default>": [
            "BaseMod.jar", "CommunicationMod.jar", "SpirecommParity.jar"]}}), encoding="utf-8")
        lines = display.read_text(encoding="utf-8").splitlines()
        if len(lines) < 6:
            raise ValueError("invalid display configuration")
        lines[2] = "60"
        display.write_text("\n".join(lines[:6]) + "\n", encoding="utf-8")
        environment = {**os.environ, "SLS_RUN_COMPLETION": str(completion)}
        with (run / "stdout.log").open("wb") as stdout, (run / "stderr.log").open("wb") as stderr:
            process = subprocess.Popen(launcher_command(game, mts, oracle_mode=mode), cwd=game,
                                       env=environment, stdout=stdout, stderr=stderr)
            journal.data["pid"] = process.pid
            journal._flush()
            deadline = time.monotonic() + timeout
            while time.monotonic() < deadline:
                if completion.is_file():
                    marker = json.loads(completion.read_text(encoding="utf-8"))
                    break
                if process.poll() is not None:
                    raise RuntimeError(f"game exited before smoke: {process.returncode}; logs: {run}")
                time.sleep(.25)
            if marker is None:
                raise TimeoutError(f"Oracle runtime smoke timed out; logs: {run}")
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
    result = {"oracle_sha256": sha256(oracle), "mode": mode, "completion": marker,
              "recovery_journal": str(journal.path), "recovery_status": journal.data["status"]}
    output.with_suffix(".launch.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    if marker.get("exit_code") != 0:
        raise RuntimeError(f"Oracle smoke failed: {output}")
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("oracle", type=Path)
    action = parser.add_mutually_exclusive_group()
    action.add_argument("--runtime", action="store_true", help="temporarily launch the game with backup/recovery")
    action.add_argument("--install", action="store_true", help="install verified JAR with a checked backup; game must be closed")
    parser.add_argument("--mode", choices=("production", "validation"), default="production")
    parser.add_argument("--game-root", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--timeout", type=float, default=240)
    args = parser.parse_args()
    report = inspect(args.oracle)
    if args.install:
        result = install(args.oracle, args.game_root)
    elif args.runtime:
        if args.output is None or args.timeout <= 0:
            parser.error("runtime requires --output and a positive timeout")
        result = runtime_smoke(args.oracle, args.output.resolve(), args.mode, args.game_root, args.timeout)
    else:
        result = {"oracle_sha256": report["output_sha256"], "source_build_verified": True}
    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
