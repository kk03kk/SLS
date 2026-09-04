"""Inspect and control a live SLS policy from a loopback browser dashboard."""

from __future__ import annotations

import argparse
import json
import signal
import sys
import webbrowser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import LiveGameBackend  # noqa: E402
from sls.runtime import (  # noqa: E402
    InspectorLauncher,
    InteractiveAgentRuntime,
    create_server,
    discover_policy_artifacts,
    load_policy_artifact,
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "artifact", type=Path, nargs="?",
        help="optional additional/preselected exported policy artifact",
    )
    parser.add_argument(
        "--models-root", type=Path, action="append",
        help="model discovery root (repeatable; defaults to model)",
    )
    parser.add_argument("--device", default="cpu")
    parser.add_argument(
        "--log", type=Path,
        default=ROOT / "local" / "logs" / "live-inspector.jsonl",
    )
    parser.add_argument("--low-confidence", type=float, default=0.55)
    parser.add_argument("--delay", type=float, default=1.0)
    parser.add_argument(
        "--card-reward-preview", type=float, default=3.0,
        help="seconds to keep the stock three-card reward screen visible (0-10)",
    )
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--no-open-browser", action="store_true")
    parser.add_argument("--wait-for-neow", action="store_true")
    parser.add_argument("--wait-timeout", type=float, default=600.0)
    parser.add_argument(
        "--list-models", action="store_true",
        help="list discovered exported policies and exit without connecting to the game",
    )
    args = parser.parse_args()

    roots = args.models_root or [ROOT / "model"]
    extras = [args.artifact] if args.artifact is not None else []
    models = discover_policy_artifacts(roots, extra_paths=extras)
    if args.list_models:
        print(json.dumps(models, ensure_ascii=False, indent=2))
        return 0

    def make_runtime(path: Path) -> InteractiveAgentRuntime:
        loaded = load_policy_artifact(path, device=args.device)
        return InteractiveAgentRuntime(
            LiveGameBackend(
                wait_for_neow=args.wait_for_neow,
                wait_timeout_seconds=args.wait_timeout,
                allow_curriculum_goals=True,
            ),
            loaded,
            device=args.device,
            log_path=args.log,
            low_confidence=args.low_confidence,
            delay_seconds=args.delay,
            card_reward_preview_seconds=args.card_reward_preview,
        )

    launcher = InspectorLauncher(
        models,
        make_runtime,
        preselected_path=args.artifact,
    )
    server = create_server(launcher, args.host, args.port)
    server.timeout = 0.25
    stopped = False

    def stop(_number: int, _frame: object) -> None:
        nonlocal stopped
        stopped = True
        try:
            launcher.submit({"command": "stop"})
        except Exception:
            pass

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)
    url = f"http://{args.host}:{args.port}/"
    print(
        f"SLS live test dashboard: {url} ({len(models)} models; waiting for selection)",
        file=sys.stderr,
    )
    if not args.no_open_browser:
        webbrowser.open(url)
    try:
        while not stopped and launcher.state()["status"] != "STOPPED":
            server.handle_request()
    finally:
        server.server_close()
    return 1 if launcher.state()["status"] in {"ERROR", "SETUP_ERROR"} else 0


if __name__ == "__main__":
    raise SystemExit(main())
