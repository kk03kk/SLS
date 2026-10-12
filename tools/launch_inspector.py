"""Open an existing inspector, or configure and launch the stock modded game."""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT))

from sls.runtime.live_setup import default_game_dir, inspect_live_setup
from sls.runtime.window import open_inspector_window
from tools.configure_live_inspector import (
    configure,
    default_config_path,
    select_required_mods,
)
from tools.run_original_canary import launcher_command


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-dir", type=Path, default=default_game_dir())
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--configure-only", action="store_true")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535:
        parser.error("port must be between 1 and 65535")
    url = f"http://127.0.0.1:{args.port}"
    try:
        with urllib.request.urlopen(url + "/api/state", timeout=2) as response:
            state = json.load(response)
        if state.get("schema") != "sls-live-inspector-state-v1":
            raise RuntimeError("port is occupied by another service")
    except urllib.error.URLError:
        pass
    else:
        open_inspector_window(url)
        print(f"Opened existing controller: {url}")
        return 0
    if sys.platform != "win32":
        parser.error("stock-game launcher currently supports Windows only")
    game = args.game_dir
    if game is None:
        parser.error("Slay the Spire not found; use --game-dir")
    # Do not start a second game/controller or rewrite config under a live game.
    query = subprocess.run(
        ["powershell", "-NoProfile", "-Command",
         "Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java|SlayTheSpire' -and $_.CommandLine -match 'SlayTheSpire|ModTheSpire|desktop-1.0|mts-launcher' } | Select-Object -ExpandProperty ProcessId"],
        capture_output=True, text=True, check=True,
        creationflags=subprocess.CREATE_NO_WINDOW,
    )
    if query.stdout.strip():
        raise RuntimeError("game is already running without a reachable inspector; close it and relaunch with this command")
    config = default_config_path()
    ready = inspect_live_setup(game_dir=game, config=config, model_root=ROOT / "model")
    required = ready["game"] and all(ready["mods"].values()) and not ready["invalid_models"] and ready["model_count"] > 0
    if not required:
        raise RuntimeError(json.dumps(ready, ensure_ascii=False))
    backup, _command = configure(config, python=Path(sys.executable), port=args.port)
    select_required_mods(config.parent.parent / "mod_lists.json")
    print(f"CommunicationMod configured; original config preserved: {backup}")
    if args.configure_only:
        print("Start the game through ModTheSpire; it will open the inspector automatically.")
        return 0
    mts = game.parents[1] / "workshop" / "content" / "646570" / "1605060445" / "ModTheSpire.jar"
    if not mts.is_file():
        raise FileNotFoundError(f"ModTheSpire launcher not found: {mts}; start through your existing ModTheSpire launcher")
    logs = ROOT / "local" / "logs"
    logs.mkdir(parents=True, exist_ok=True)
    environment = os.environ.copy()
    environment["PYTHONPYCACHEPREFIX"] = str(ROOT / "local" / "build" / "python-cache")
    environment["PYTHONPATH"] = os.pathsep.join((str(ROOT), str(ROOT / "src")))
    with (logs / "inspector-game.out").open("ab") as out, (logs / "inspector-game.err").open("ab") as err:
        process = subprocess.Popen(launcher_command(game, mts, oracle_mode="production"),
                                   cwd=game, env=environment, stdout=out, stderr=err,
                                   creationflags=subprocess.CREATE_NO_WINDOW)
    print(f"Original game launched (PID {process.pid}); CommunicationMod opens {url}. Select the 90M model there.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
