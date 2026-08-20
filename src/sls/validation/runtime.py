"""Recoverable management of the local Original-game validation runtime."""

from __future__ import annotations

from contextlib import AbstractContextManager
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
from typing import Any, Iterable, Mapping


RUNTIME_JOURNAL_SCHEMA = "sls-original-runtime-journal-v1"


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _write(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def write_completion(exit_code: int, **details: Any) -> None:
    marker = os.environ.get("SLS_RUN_COMPLETION")
    if not marker:
        return
    target = Path(marker)
    temporary = target.with_suffix(target.suffix + ".tmp")
    _write(temporary, {
        "schema": "sls-original-run-completion-v1", "exit_code": int(exit_code),
        "finished_at": datetime.now(timezone.utc).isoformat(), **details,
    })
    os.replace(temporary, target)


class RuntimeJournal:
    """Write-ahead backup journal. Recovery is idempotent after hard crashes."""

    def __init__(self, path: Path) -> None:
        self.path = path
        self.data: dict[str, Any] = {
            "schema": RUNTIME_JOURNAL_SCHEMA, "status": "PREPARING", "entries": [],
            "created_at": datetime.now(timezone.utc).isoformat(),
        }
        _write(self.path, self.data)

    @classmethod
    def open(cls, path: Path) -> "RuntimeJournal":
        instance = cls.__new__(cls)
        instance.path = path
        instance.data = json.loads(path.read_text(encoding="utf-8"))
        if instance.data.get("schema") != RUNTIME_JOURNAL_SCHEMA:
            raise ValueError("unsupported Original runtime journal")
        return instance

    def _flush(self) -> None:
        temporary = self.path.with_suffix(".tmp")
        _write(temporary, self.data)
        os.replace(temporary, self.path)

    def backup(self, source: Path, backup_root: Path) -> None:
        source = source.resolve()
        if any(Path(entry["target"]) == source for entry in self.data["entries"]):
            return
        relative = f"{len(self.data['entries']):03d}-{source.name}"
        backup = backup_root / relative
        existed = source.is_file()
        if existed:
            backup.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, backup)
        self.data["entries"].append({
            "target": str(source), "backup": str(backup), "existed": existed,
            "original_sha256": _sha256(source) if existed else None,
        })
        self._flush()

    def mark_active(self) -> None:
        self.data["status"] = "ACTIVE"
        self._flush()

    def record_process(
        self, *, pid: int, executable: Path, command: Iterable[str],
        started_at: str | None = None, identity: Mapping[str, Any] | None = None,
    ) -> None:
        self.data["process"] = {
            "pid": int(pid), "executable": str(executable.resolve()),
            "command": list(command),
            "started_at": started_at or datetime.now(timezone.utc).isoformat(),
            "creation_date": None if identity is None else identity.get("CreationDate"),
            "observed_command_line": None if identity is None else identity.get("CommandLine"),
        }
        self._flush()

    def recover(self) -> None:
        errors: list[str] = []
        for entry in reversed(self.data["entries"]):
            target, backup = Path(entry["target"]), Path(entry["backup"])
            try:
                if entry["existed"]:
                    if not backup.is_file():
                        raise FileNotFoundError(f"missing backup {backup}")
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(backup, target)
                    actual = _sha256(target)
                    if actual != entry["original_sha256"]:
                        raise OSError(
                            f"restored hash mismatch: expected {entry['original_sha256']}, got {actual}"
                        )
                elif target.exists():
                    target.unlink()
            except OSError as error:
                errors.append(f"{target}: {error}")
        self.data["status"] = "RECOVERY_FAILED" if errors else "RECOVERED"
        self.data["recovered_at"] = datetime.now(timezone.utc).isoformat()
        self.data["recovery_errors"] = errors
        self._flush()
        if errors:
            raise RuntimeError("Original runtime recovery failed: " + "; ".join(errors))


class OriginalRuntimeGuard(AbstractContextManager["OriginalRuntimeGuard"]):
    """Protect user autosaves during a live validation process."""

    def __init__(self, journal: Path) -> None:
        self.journal_path = journal
        self.journal: RuntimeJournal | None = None

    def __enter__(self) -> "OriginalRuntimeGuard":
        self.journal = RuntimeJournal.open(self.journal_path)
        if self.journal.data["status"] not in {"ACTIVE", "PREPARING"}:
            raise RuntimeError(f"runtime journal is not active: {self.journal.data['status']}")
        return self

    def __exit__(self, exc_type: Any, exc: Any, traceback: Any) -> None:
        assert self.journal is not None
        self.journal.recover()


def recover_pending(journal_root: Path, *, refuse_live_process: bool = True) -> list[Path]:
    recovered: list[Path] = []
    if not journal_root.exists():
        return recovered
    for path in sorted(journal_root.glob("*/journal.json")):
        journal = RuntimeJournal.open(path)
        if journal.data.get("status") in {"PREPARING", "ACTIVE", "RECOVERY_FAILED"}:
            if refuse_live_process and owned_process_matches(journal):
                raise RuntimeError(
                    f"refusing to restore Original files while owned game process is alive: {path}"
                )
            journal.recover()
            recovered.append(path)
    return recovered


def process_identity(pid: int) -> dict[str, Any] | None:
    """Return a small Windows process identity without a third-party dependency."""

    if os.name != "nt":
        return None
    script = (
        f"$p=Get-CimInstance Win32_Process -Filter \"ProcessId={int(pid)}\";"
        "if($null-ne $p){$p|Select-Object ProcessId,ExecutablePath,CommandLine,CreationDate|"
        "ConvertTo-Json -Compress}"
    )
    result = subprocess.run(
        ["powershell", "-NoProfile", "-Command", script],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    if result.returncode or not result.stdout.strip():
        return None
    try:
        value = json.loads(result.stdout)
    except json.JSONDecodeError:
        return None
    return value if isinstance(value, dict) else None


def owned_process_matches(journal: RuntimeJournal) -> bool:
    recorded = journal.data.get("process") or {}
    if not recorded.get("pid") or not recorded.get("executable"):
        return False
    live = process_identity(int(recorded["pid"]))
    if live is None:
        return False
    executable = str(live.get("ExecutablePath") or "")
    try:
        if Path(executable).resolve() != Path(recorded["executable"]).resolve():
            return False
        if recorded.get("creation_date") and live.get("CreationDate") != recorded["creation_date"]:
            return False
        expected_line = str(recorded.get("observed_command_line") or "").strip()
        if expected_line and str(live.get("CommandLine") or "").strip() != expected_line:
            return False
        return True
    except OSError:
        return False


def prepare_runtime(
    *, repository: Path, game_root: Path, config: Path, python: Path,
    max_steps: int, profile: str, save_files: Iterable[Path], install: bool = True,
    entry: Path | None = None, entry_args: Iterable[str] = (),
    external_owner: bool = False,
) -> Path:
    """Prepare exactly four authoritative mods and return the recovery journal."""

    built_oracle = repository / ".build" / "oracle" / "SpirecommParity.jar"
    mod_dir = game_root / "mods"
    mod_list = config.parent.parent / "mod_lists.json"
    # BaseMod and CommunicationMod are Steam Workshop mods. Copying duplicate
    # jars into ``mods`` makes ModTheSpire load two copies, so only the local
    # instrumentation jar is installed here.
    installed = {"SpirecommParity.jar": built_oracle}
    dependencies = {
        "BaseMod.jar": repository / "external" / "original-game" / "BaseMod.jar",
        "CommunicationMod.jar": repository / "external" / "original-game" / "CommunicationMod.jar",
    }
    missing = [str(path) for path in (*installed.values(), *dependencies.values()) if not path.is_file()]
    if not python.is_file():
        missing.append(str(python))
    if missing:
        raise FileNotFoundError("missing Original runtime prerequisite: " + ", ".join(missing))
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    root = repository / "validation-results" / "runtime-journals" / stamp
    backup_root = root / "backups"
    journal = RuntimeJournal(root / "journal.json")
    targets = [config, mod_list, *save_files, *(mod_dir / name for name in installed)]
    # Extra jars are disabled only for the authoritative run and restored later.
    extra_mods = [path for path in mod_dir.glob("*.jar") if path.name not in installed]
    targets.extend(extra_mods)
    for target in targets:
        journal.backup(target, backup_root)
    if install:
        mod_dir.mkdir(parents=True, exist_ok=True)
        for name, source in installed.items():
            shutil.copy2(source, mod_dir / name)
        for extra in extra_mods:
            extra.unlink()
        config.parent.mkdir(parents=True, exist_ok=True)
        if entry is None:
            command = (
                f"{python.as_posix()} {repository.as_posix()}/tools/validate_full_run.py "
                f"--profile {profile} --max-steps {max_steps} "
                f"--truth-root {repository.as_posix()}/validation-results/truth "
                f"--runtime-journal {journal.path.as_posix()}"
            )
        else:
            parts = [python.as_posix(), entry.as_posix(), *entry_args]
            if not external_owner:
                parts.extend(("--runtime-journal", journal.path.as_posix()))
            command = " ".join(parts)
        escaped = command.replace(":", "\\:")
        config.write_text(
            f"command={escaped}\nrunAtGameStart=true\nverbose=true\n",
            encoding="utf-8",
        )
        _write(mod_list, {
            "defaultList": "<Default>",
            "lists": {"<Default>": [
                "BaseMod.jar", "CommunicationMod.jar", "SpirecommParity.jar",
            ]},
        })
    journal.data["runtime"] = {
        "repository": str(repository), "game_root": str(game_root),
        "python": str(python),
        "mods": {name: _sha256(mod_dir / name) if (mod_dir / name).is_file() else None for name in installed},
        "dependencies": {name: _sha256(path) for name, path in dependencies.items()},
        "disabled_mods": [path.name for path in extra_mods],
        "mod_list": str(mod_list),
        "owner": "EXTERNAL_LAUNCHER" if external_owner else "VALIDATOR_CHILD",
    }
    journal.mark_active()
    return journal.path
