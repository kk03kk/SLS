"""Protected real-game Web Inspector acceptance; never uses the simulator."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))

from sls.runtime.live_setup import default_game_dir
from tools.configure_live_inspector import _command_path, default_config_path
from tools.run_original_canary import (
    BackupJournal,
    launcher_command,
    stop_owned_and_restore,
)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-dir", type=Path, default=default_game_dir())
    parser.add_argument("--seed", type=int, default=131100070)
    parser.add_argument("--port", type=int, default=8876)
    parser.add_argument("--timeout", type=float, default=1200)
    parser.add_argument("--max-actions", type=int, default=2000)
    parser.add_argument("--hold-terminal", type=float, default=0)
    parser.add_argument("--output", type=Path, default=ROOT / "local/reports/visual-inspector" / datetime.now().strftime("live-%Y%m%d-%H%M%S"))
    args = parser.parse_args()
    if not args.game_dir:
        parser.error("Original game not found")
    game = args.game_dir
    running = subprocess.run(["powershell", "-NoProfile", "-Command",
                              "Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java|SlayTheSpire' -and $_.CommandLine -match 'SlayTheSpire|ModTheSpire|desktop-1.0|mts-launcher' } | Select-Object -ExpandProperty ProcessId"],
                             capture_output=True, text=True, check=True,
                             creationflags=subprocess.CREATE_NO_WINDOW)
    if running.stdout.strip():
        raise RuntimeError("close existing Original game before protected acceptance")
    output = args.output.resolve()
    if output.exists():
        raise FileExistsError(output)
    output.mkdir(parents=True)
    config = default_config_path()
    journal = BackupJournal(output / "journal.json")
    for target in (config, config.parent.parent / "mod_lists.json", game / "info.displayconfig"):
        journal.backup(target)
    for name in ("preferences", "betaPreferences", "saves", "runs"):
        journal.backup_tree(game / name)
    artifact = ROOT / "model/ironclad-a20-act1-90m-endpoint.pt"
    mts = game.parents[1] / "workshop/content/646570/1605060445/ModTheSpire.jar"
    process = None
    result = {"status": "FAILED", "source": "original-production-game", "seed": args.seed,
              "artifact_sha256": hashlib.sha256(artifact.read_bytes()).hexdigest()}
    tracked = [ROOT / "src/sls/runtime" / name for name in
               ("controller.py", "inspector.py", "visual.py", "ownership.py", "artifact.py")]
    tracked.extend((ROOT / "src/sls/runtime/web/dist").rglob("*"))
    tracked.append(ROOT / "src/sls/backends/original/live.py")
    tracked.extend((ROOT / "src/sls/backends/original").glob("*.py"))
    tracked.extend((ROOT / "tools/play_live_inspector.py", Path(__file__)))
    result["source_files"] = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
                              for p in tracked if p.is_file()}
    result["oracle_jar_sha256"] = hashlib.sha256((game / "mods/SpirecommParity.jar").read_bytes()).hexdigest()
    (output / "identity.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    url = f"http://127.0.0.1:{args.port}"

    def request(path: str, payload: dict | None = None):
        data = json.dumps(payload).encode() if payload is not None else None
        req = urllib.request.Request(url + path, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as response:
            return json.load(response)

    try:
        command = " ".join((_command_path(Path(sys.executable)),
                            _command_path(ROOT / "tools/play_live_inspector.py"),
                            _command_path(artifact), "--wait-for-neow --no-open-browser",
                            f"--port {args.port} --delay 0.2 --card-reward-preview 0",
                            "--log", _command_path(output / "actions.jsonl")))
        config.write_text(f"command={command}\nrunAtGameStart=true\nmaxInitializationTimeout=900\nverbose=true\n", encoding="utf-8")
        with (output / "game.out").open("wb") as out, (output / "game.err").open("wb") as err:
            process = subprocess.Popen(launcher_command(game, mts, oracle_mode="production"),
                                       cwd=game, env={**os.environ, "PYTHONPYCACHEPREFIX": str(ROOT / "local/build/python-cache"),
                                                     "PYTHONPATH": os.pathsep.join((str(ROOT), str(ROOT / "src")))},
                                       stdout=out, stderr=err, creationflags=subprocess.CREATE_NO_WINDOW)
        journal.data["pid"] = process.pid
        journal._flush()
        deadline = time.monotonic() + args.timeout
        state = None
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError(f"Original process exited {process.returncode}")
            try:
                state = request("/api/state")
                break
            except (urllib.error.URLError, TimeoutError):
                time.sleep(.5)
        if state is None:
            raise TimeoutError("Inspector did not start")
        request("/api/control", {"command": "start", "model_id": state["selected_model_id"],
                                 "new_run": True, "seed": str(args.seed)})
        stepped = False
        paused_after_step = False
        auto_started = False
        pause_requested = False
        pause_verified = False
        snapshots = output / "states.jsonl"
        last_revision = -1
        while time.monotonic() < deadline:
            state = request("/api/state")
            if state["revision"] != last_revision:
                with snapshots.open("a", encoding="utf-8") as stream:
                    stream.write(json.dumps(state, ensure_ascii=True) + "\n")
                last_revision = state["revision"]
            if state["status"] in {"ERROR", "SETUP_ERROR", "STOPPED"}:
                raise RuntimeError(state.get("error") or state["status"])
            if state["status"] == "PAUSED" and not stepped:
                request("/api/control", {"command": "step", "boundary_id": state["boundary_id"]})
                stepped = True
            elif state["status"] == "PAUSED" and stepped and state["visual"]["history_count"] == 1 and not auto_started:
                paused_after_step = True
                request("/api/control", {"command": "resume", "boundary_id": state["boundary_id"]})
                auto_started = True
            elif auto_started and not pause_requested and state.get("visual", {}).get("history_count", 0) >= 20:
                request("/api/control", {"command": "pause"})
                pause_requested = True
            elif pause_requested and not pause_verified and state["status"] == "PAUSED":
                time.sleep(.5)
                unchanged = request("/api/state")
                assert unchanged["boundary_id"] == state["boundary_id"]
                assert unchanged["actions"] == state["actions"]
                assert unchanged["visual"]["history_count"] == state["visual"]["history_count"]
                pause_verified = True
                request("/api/control", {"command": "resume", "boundary_id": unchanged["boundary_id"]})
            if state.get("terminal", False) or state.get("visual", {}).get("history_count", 0) >= args.max_actions:
                history = []
                while len(history) < state["visual"]["history_count"]:
                    history.extend(request(f"/api/history?offset={len(history)}&limit=100")["entries"])
                result.update({"status": "PASS" if state["terminal"] else "PARTIAL_MAX_ACTIONS",
                               "single_step_confirmed": paused_after_step,
                               "pause_retained_scores_and_boundary": pause_verified,
                               "final_state": state, "decisions": len(history),
                               "map_nodes": len(state["visual"]["map"]),
                               "all_actions_acknowledged": all(e["confirmation"] == "ACK" for e in history)})
                (output / "history.json").write_text(json.dumps(history, indent=2), encoding="utf-8")
                time.sleep(max(0, min(60, args.hold_terminal)))
                break
            time.sleep(.25)
        else:
            raise TimeoutError("Original-game acceptance timed out")
    except BaseException as error:
        result["error"] = f"{type(error).__name__}: {error}"
        raise
    finally:
        try:
            request("/api/control", {"command": "stop"})
            time.sleep(.5)
        except (OSError, TimeoutError) as error:
            result["shutdown_notice"] = f"{type(error).__name__}: {error}"
        stop_owned_and_restore(journal, process)
        result["recovery"] = journal.data["status"]
        result["source_unchanged"] = all(hashlib.sha256((ROOT / p).read_bytes()).hexdigest() == sha
                                         for p, sha in result["source_files"].items())
        if not result["source_unchanged"]:
            result["status"] = "SOURCE_CHANGED_REPLAY_REQUIRED"
        (output / "result.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
        print(json.dumps({"result": str(output / "result.json"), "status": result["status"], "recovery": result["recovery"]}))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
