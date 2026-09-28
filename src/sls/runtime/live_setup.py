"""Read-only checks for the local Slay the Spire demo prerequisites."""

from __future__ import annotations

import json
import os
import re
import shlex
import zipfile
from pathlib import Path
from typing import Any

from sls.runtime.artifact import load_policy_artifact
from sls.runtime.inspector import discover_policy_artifacts


def default_game_dir() -> Path | None:
    """Find common and configured Steam libraries without scanning entire drives."""

    roots = [Path(value) for value in os.environ.get("SLS_STEAM_LIBRARIES", "").split(os.pathsep) if value]
    if os.name == "nt":
        try:
            import winreg

            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, r"Software\Valve\Steam") as key:
                roots.append(Path(winreg.QueryValueEx(key, "SteamPath")[0]))
        except (FileNotFoundError, OSError):
            pass
        for drive in "CDEFGH":
            roots.extend((Path(f"{drive}:/Steam"), Path(f"{drive}:/Program Files (x86)/Steam"), Path(f"{drive}:/SteamLibrary")))
    for root in roots:
        candidates = [root]
        vdf = root / "steamapps" / "libraryfolders.vdf"
        if vdf.is_file():
            try:
                content = vdf.read_text(encoding="utf-8")
            except (OSError, UnicodeError):
                content = ""
            for raw in re.findall(r'"path"\s+"([^"]+)"', content, flags=re.IGNORECASE):
                candidates.append(Path(raw.replace("\\\\", "\\")))
        for library in dict.fromkeys(candidates):
            candidate = library / "steamapps" / "common" / "SlayTheSpire"
            if (candidate / "desktop-1.0.jar").is_file():
                return candidate
    return None


def inspect_live_setup(
    *, game_dir: Path | None, config: Path, model_root: Path,
    workshop_dir: Path | None = None, mod_lists_path: Path | None = None,
) -> dict[str, Any]:
    """Report what is present without starting the game or changing files."""

    game = game_dir.resolve() if game_dir is not None else None
    game_ok = game is not None and (game / "desktop-1.0.jar").is_file()
    directories = []
    if game is not None:
        directories.append(game / "mods")
    if workshop_dir is not None:
        directories.append(workshop_dir)
    elif game is not None:
        directories.append(game.parent.parent / "workshop" / "content" / "646570")

    jars: dict[str, Path] = {}
    for directory in directories:
        if directory.is_dir():
            for path in directory.rglob("*.jar"):
                jars[path.name.lower()] = path
    mods = {
        "ModTheSpire": "modthespire.jar" in jars or (
            game is not None and (game / "mts-launcher.jar").is_file()
        ),
        "BaseMod": "basemod.jar" in jars,
        "CommunicationMod": "communicationmod.jar" in jars,
        "Observation Oracle": "spirecommparity.jar" in jars,
    }
    selection_path = mod_lists_path or config.parent.parent / "mod_lists.json"
    selected_mods = False
    try:
        lists = json.loads(selection_path.read_text(encoding="utf-8"))
        current = lists.get("lists", {}).get(lists.get("defaultList"), [])
        selected_mods = {
            "basemod.jar", "communicationmod.jar", "spirecommparity.jar",
        } <= {str(name).lower() for name in current}
    except (OSError, UnicodeError, ValueError, AttributeError, TypeError):
        pass
    oracle_compatible = False
    oracle = jars.get("spirecommparity.jar")
    if oracle is not None:
        try:
            with zipfile.ZipFile(oracle) as archive:
                names = set(archive.namelist())
            oracle_compatible = {
                "spirecomm/parity/CardStatePatch$AddRewardPreviews.class",
                "spirecomm/parity/EventStatePatch.class",
            } <= names
        except (OSError, zipfile.BadZipFile):
            pass
    models = discover_policy_artifacts((model_root,))
    valid_models = []
    invalid_models = []
    for model in models:
        try:
            load_policy_artifact(Path(str(model["path"])))
        except (OSError, RuntimeError, TypeError, ValueError, KeyError) as error:
            invalid_models.append({"name": model["name"], "error": f"{type(error).__name__}: {error}"})
        else:
            valid_models.append(model)
    try:
        properties = config.read_text(encoding="utf-8")
    except (OSError, UnicodeError):
        properties = ""
    settings = {
        line.split("=", 1)[0].strip(): line.split("=", 1)[1].strip()
        for line in properties.splitlines()
        if "=" in line and not line.lstrip().startswith(("#", "!"))
    }
    command = settings.get("command", "").replace("\\:", ":")
    try:
        executable = Path(shlex.split(command, posix=False)[0].strip('"'))
    except (IndexError, ValueError):
        executable = Path("")
    expected_script = Path(__file__).resolve().parents[3] / "tools" / "play_live_inspector.py"
    configured = (
        settings.get("runAtGameStart", "").lower() == "true"
        and expected_script.as_posix().lower() in command.lower()
        and executable.is_file()
    )
    issues = []
    if not game_ok:
        issues.append("未找到游戏 desktop-1.0.jar；用 --game-dir 指定 Steam 安装目录。")
    for name, present in mods.items():
        if not present:
            issues.append(f"缺少 {name} JAR。")
    if mods["Observation Oracle"] and not oracle_compatible:
        issues.append("Observation Oracle 缺少当前所需的事件或奖励观测补丁。")
    if not selected_mods:
        issues.append("ModTheSpire 默认列表尚未选中 BaseMod、CommunicationMod 和 SpirecommParity。")
    if not configured:
        issues.append("CommunicationMod 尚未指向本仓库控制台；运行 tools/configure_live_inspector.py。")
    if not valid_models:
        issues.append("model/ 中没有通过权重校验的 v5 导出策略工件。")
    return {
        "game_dir": str(game) if game else None,
        "game": bool(game_ok),
        "mods": mods,
        "selected_mods": selected_mods,
        "oracle_patches_present": oracle_compatible,
        "communication_config": str(config),
        "communication_config_exists": config.is_file(),
        "inspector_configured": configured,
        "model_count": len(models),
        "verified_model_count": len(valid_models),
        "invalid_models": invalid_models,
        "models": [
            {"name": model["name"], "goal": model["goal"], "ascension_min": model["ascension_min"], "ascension_max": model["ascension_max"]}
            for model in models
        ],
        "issues": issues,
        "ready": not issues,
    }
