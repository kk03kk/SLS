from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src"
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

from sls.backends.original.adapter import adapt_original
from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.contracts import ActionKind


def _write_completion(exit_code: int, error: str | None = None) -> None:
    path = os.environ.get("SLS_RUN_COMPLETION")
    if path:
        Path(path).write_text(
            json.dumps({"exit_code": exit_code, "error": error}, indent=2) + "\n",
            encoding="utf-8",
        )


def _enter_combat(backend: OriginalBackend) -> None:
    decision = backend.reset(0)
    for _ in range(40):
        if decision.observation.screen == "COMBAT":
            return
        if decision.terminal or not decision.actions:
            raise RuntimeError("could not reach the initial one-monster combat")
        preferred = next(
            (
                action for action in decision.actions
                if action.kind in {
                    ActionKind.CHOOSE_NEOW_OPTION,
                    ActionKind.CHOOSE_MAP_NODE,
                    ActionKind.SELECT_CARD,
                    ActionKind.CONFIRM,
                }
            ),
            decision.actions[0],
        )
        decision = backend.step(preferred).decision
    raise RuntimeError("initial combat was not reached within 40 decisions")


def _play_probe_card(session: OriginalSession, payload: dict) -> None:
    adapted = adapt_original(payload)
    candidates = [
        action for action in adapted.decision.actions
        if action.kind is ActionKind.PLAY_CARD and action.subject_id == "HAND:1"
    ]
    if not candidates:
        raise RuntimeError("card probe did not expose HAND:1 as playable")
    commands = adapted.commands[candidates[0].candidate_id]
    for command in commands:
        session.execute(command)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--cards", nargs="*")
    args = parser.parse_args()
    registry = json.loads(
        (ROOT / "src" / "sls" / "content" / "registry.json").read_text(
            encoding="utf-8",
        ),
    )["categories"]["cards"]
    card_ids = args.cards or [str(row["id"]) for row in registry]
    result = {"cards": card_ids, "completed": []}
    try:
        session = OriginalSession()
        backend = OriginalBackend(session=session)
        _enter_combat(backend)
        for card_id in card_ids:
            for upgraded in (0, 1):
                payload = session.execute(
                    f"parity_card {card_id} {upgraded}",
                )
                _play_probe_card(session, payload)
                result["completed"].append(f"{card_id}:{upgraded}")
        backend.return_to_menu()
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(
            json.dumps(result, indent=2) + "\n", encoding="utf-8",
        )
        _write_completion(0)
        return 0
    except BaseException as error:
        result["error"] = f"{type(error).__name__}: {error}"
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(
            json.dumps(result, indent=2) + "\n", encoding="utf-8",
        )
        _write_completion(2, result["error"])
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
