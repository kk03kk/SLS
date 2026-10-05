"""Safely configure CommunicationMod to launch the local live inspector."""

from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
import tempfile
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def default_config_path() -> Path:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if not local_app_data:
        raise RuntimeError("LOCALAPPDATA is not set")
    return Path(local_app_data) / "ModTheSpire" / "CommunicationMod" / "config.properties"


def _backup(path: Path) -> Path:
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    backup = path.with_name(f"{path.name}.bak-{stamp}")
    shutil.copy2(path, backup)
    return backup


def _atomic_write(path: Path, text: str) -> None:
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.", suffix=".tmp", dir=path.parent,
    )
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            stream.write(text)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        if temporary.exists():
            temporary.unlink()


def _replace_properties(text: str, updates: dict[str, str]) -> str:
    lines = text.splitlines()
    seen: set[str] = set()
    output: list[str] = []
    for line in lines:
        stripped = line.lstrip()
        if not stripped or stripped.startswith(('#', '!')) or "=" not in line:
            output.append(line)
            continue
        key = line.split("=", 1)[0].strip()
        if key in updates:
            if key not in seen:
                output.append(f"{key}={updates[key]}")
                seen.add(key)
        else:
            output.append(line)
    for key, value in updates.items():
        if key not in seen:
            output.append(f"{key}={value}")
    return "\n".join(output) + "\n"


def _validate_config(text: str) -> None:
    targeted = {"command": 0, "runAtGameStart": 0}
    for line in text.splitlines():
        stripped = line.lstrip()
        if not stripped or stripped.startswith(('#', '!')):
            continue
        if "=" not in line:
            continue
        key = line.split("=", 1)[0].strip()
        if key in targeted:
            targeted[key] += 1
    if any(count != 1 for count in targeted.values()):
        raise ValueError(
            "CommunicationMod config must contain exactly one command and "
            "runAtGameStart property"
        )


def _command_path(path: Path) -> str:
    # CommunicationMod uses java.util.Properties, where the drive colon must
    # be escaped. Forward slashes avoid backslash escape ambiguities.
    value = str(path.resolve()).replace("\\", "/").replace(":", "\\:")
    return f'"{value}"' if any(character.isspace() for character in value) else value


def configure(
    config: Path,
    *,
    python: Path,
    artifact: Path | None = None,
    port: int = 8765,
    delay: float = 1.0,
) -> tuple[Path, str]:
    required = {
        "CommunicationMod config": config,
        "Python": python,
        "inspector": ROOT / "tools" / "play_live_inspector.py",
    }
    if artifact is not None:
        required["policy artifact"] = artifact
    for label, path in required.items():
        if not path.is_file():
            raise FileNotFoundError(f"{label} does not exist: {path}")
    if not 1 <= port <= 65535:
        raise ValueError("port must be between 1 and 65535")
    if not 0.0 <= delay <= 10.0:
        raise ValueError("delay must be between zero and ten")
    original = config.read_text(encoding="utf-8")
    if "\x00" in original:
        raise ValueError("CommunicationMod config is not readable text")
    _validate_config(original)
    log = ROOT / "local" / "logs" / "live-inspector.jsonl"
    command_parts = [
        _command_path(python),
        _command_path(ROOT / "tools" / "play_live_inspector.py"),
    ]
    if artifact is not None:
        command_parts.append(_command_path(artifact))
    command_parts.extend((
        "--device cpu",
        f"--log {_command_path(log)}",
        "--wait-for-neow --wait-timeout 900",
        f"--port {port} --delay {delay:.1f}",
    ))
    command = " ".join(command_parts)
    updated = _replace_properties(original, {
        "command": command,
        "runAtGameStart": "true",
        "maxInitializationTimeout": "900",
    })
    backup = _backup(config)
    _atomic_write(config, updated)
    return backup, command


def restore(config: Path, backup: Path) -> Path:
    if not config.is_file():
        raise FileNotFoundError(f"CommunicationMod config does not exist: {config}")
    if not backup.is_file():
        raise FileNotFoundError(f"backup does not exist: {backup}")
    restored_text = backup.read_text(encoding="utf-8")
    if "\x00" in restored_text:
        raise ValueError("backup is not readable text")
    _validate_config(restored_text)
    current_backup = _backup(config)
    _atomic_write(config, restored_text)
    return current_backup


def _load_mod_list(path: Path) -> tuple[dict, list[str]]:
    if not path.is_file():
        raise FileNotFoundError(f"ModTheSpire mod list does not exist: {path}")
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or not isinstance(data.get("lists"), dict):
        raise ValueError("ModTheSpire mod list has an unexpected format")
    selected = data["lists"].get(data.get("defaultList"))
    if not isinstance(selected, list) or not all(isinstance(item, str) for item in selected):
        raise ValueError("ModTheSpire default list is missing or invalid")
    return data, selected


def select_required_mods(path: Path) -> Path:
    """Add the three required Mods to the user's active default list."""

    data, selected = _load_mod_list(path)
    wanted = ("BaseMod.jar", "CommunicationMod.jar", "SpirecommParity.jar")
    existing = {name.lower() for name in selected}
    missing = [name for name in wanted if name.lower() not in existing]
    if not missing:
        return path
    selected.extend(missing)
    backup = _backup(path)
    _atomic_write(path, json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    return backup


def restore_mod_list(path: Path, backup: Path) -> Path:
    if not path.is_file() or not backup.is_file():
        raise FileNotFoundError("ModTheSpire list or its backup does not exist")
    data = json.loads(backup.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or not isinstance(data.get("lists"), dict):
        raise ValueError("ModTheSpire backup has an unexpected format")
    safety_backup = _backup(path)
    _atomic_write(path, backup.read_text(encoding="utf-8"))
    return safety_backup


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path)
    parser.add_argument("--python", type=Path, default=Path(sys.executable))
    parser.add_argument(
        "--artifact", type=Path,
        help="optional artifact to add and preselect in the dashboard",
    )
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--delay", type=float, default=1.0)
    parser.add_argument("--restore", type=Path)
    parser.add_argument("--restore-mods", type=Path)
    parser.add_argument(
        "--select-mods", action="store_true",
        help="also add required Mods to the active ModTheSpire list",
    )
    args = parser.parse_args()
    config = args.config or default_config_path()
    if args.restore is not None or args.restore_mods is not None:
        if args.restore is not None:
            safety_backup = restore(config, args.restore)
            print(f"Restored: {config}")
            print(f"Previous config backup: {safety_backup}")
        if args.restore_mods is not None:
            mod_list = config.parent.parent / "mod_lists.json"
            safety_backup = restore_mod_list(mod_list, args.restore_mods)
            print(f"Restored: {mod_list}")
            print(f"Previous mod list backup: {safety_backup}")
        return 0
    mod_list = config.parent.parent / "mod_lists.json"
    if args.select_mods:
        _load_mod_list(mod_list)
    backup, command = configure(
        config,
        python=args.python,
        artifact=args.artifact,
        port=args.port,
        delay=args.delay,
    )
    print(f"Configured: {config}")
    print(f"Backup: {backup}")
    print(f"Command: {command}")
    if args.select_mods:
        mod_backup = select_required_mods(mod_list)
        print(f"Mod list: {mod_list}")
        print(f"Mod list backup: {mod_backup}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
