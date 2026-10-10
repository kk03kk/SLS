"""Capture actual stock room options/effects after one controlled initial setup."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import time
from dataclasses import asdict
from pathlib import Path

from sls.backends.original.adapter import adapt_original
from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A20_HEART
from tools.capture_original_card_batch import _write_completion
from tools.run_key_room_batch import root_path, validate_manifest


def select_action(payload: dict, requested: str):
    adapted = adapt_original(payload)
    matches = [action for action in adapted.decision.actions if
               (action.kind.value == requested if requested != "TAKE_RELIC" else
                action.kind == ActionKind.TAKE_REWARD and
                (action.reward_id or "").startswith("reward-relic:"))]
    if len(matches) != 1:
        raise ValueError(f"expected one {requested} action, found {len(matches)}")
    return matches[0], adapted.commands[matches[0].candidate_id]


def collect(session, predicate, description):
    deadline = time.monotonic() + 30
    last = None
    for _ in range(300):
        last = session.execute("state")
        if predicate(last):
            return last
        if time.monotonic() >= deadline:
            break
    raise TimeoutError(f"unfinished stock boundary: {description}; last={last}")


def ready_for(payload, requested):
    try:
        select_action(payload, requested)
        return payload.get("_stock_direct", {}).get("act") == 2
    except (ValueError, KeyError):
        return False


def post_choice_ready(payload, requested, initial):
    if "proceed" not in payload.get("available_commands", []):
        return False
    if requested == "TAKE_BLUE_KEY":
        return payload.get("_parity_run", {}).get("sapphire_key") is True
    if requested == "TAKE_RELIC":
        return len(payload.get("_stock_direct", {}).get("relics", [])) == len(initial["relics"]) + 1
    if requested == "RECALL" and payload.get("_parity_run", {}).get("ruby_key") is not True:
        return False
    return "choose" not in payload.get("available_commands", [])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--scenes", nargs="+", required=True)
    args = parser.parse_args()
    os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    scenes = validate_manifest(manifest, args.scenes)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # Reserve before connecting. Partial evidence belongs to this invocation only.
    with args.output.open("x", encoding="utf-8") as stream:
        stream.write("{}\n")
    result = dict(schema="sls-key-room-capture-v1", purpose="CONTROLLED_NOT_NATURAL_NOT_WIN_RATE",
                  manifest_sha256=hashlib.sha256(args.manifest.read_bytes()).hexdigest(),
                  stock_jar_sha256=manifest["stock_jar_sha256"], runs=[], execution_complete=False)

    def flush():
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")

    try:
        session = OriginalSession()
        backend = OriginalBackend(session=session, profile=IRONCLAD_A20_HEART)
        for scene in scenes:
            for seed in scene["seeds"]:
                decision = backend.reset(seed)
                prefix = []
                for _ in range(40):
                    if decision.observation.screen == "COMBAT":
                        break
                    if decision.terminal or not decision.actions:
                        raise RuntimeError("no initial combat reachable")
                    action = next((a for a in decision.actions if a.kind in {
                        ActionKind.CHOOSE_NEOW_OPTION, ActionKind.CHOOSE_MAP_NODE,
                        ActionKind.SELECT_CARD, ActionKind.CONFIRM}), decision.actions[0])
                    prefix.append(dict(raw=backend.raw_payload, action=asdict(action)))
                    decision = backend.step(action).decision
                else:
                    raise RuntimeError("initial combat budget exhausted")
                before = session.payload
                if before.get("_oracle_mode") != "validation" or "parity_key_room" not in before.get(
                        "available_commands", []):
                    raise ValueError("validation command unavailable")
                row = dict(scene=scene, seed=seed, normal_prefix=prefix, before=before,
                           setup_command=f"parity_key_room {scene['id']} {args.manifest.stem}", boundaries=[], actions=[])
                result["runs"].append(row)
                row["setup_response"] = session.execute(row["setup_command"])
                initial = collect(session, lambda p: ready_for(p, scene["actions"][0]), "room entry")
                row["boundaries"].append(initial)
                if manifest["schema"] == "sls-key-room-scenes-v2":
                    row["initial_root_path"] = root_path(initial)
                flush()
                for index, requested in enumerate(scene["actions"]):
                    action, commands = select_action(row["boundaries"][-1], requested)
                    row["actions"].append(dict(requested=requested, actual=asdict(action), commands=commands))
                    for command in commands:
                        session.execute(command)
                    if index + 1 < len(scene["actions"]):
                        following = scene["actions"][index + 1]
                        boundary = collect(session, lambda p: ready_for(p, following), following)
                    else:
                        # Observe a settled real room boundary; do not fabricate key flags.
                        boundary = collect(session, lambda p: post_choice_ready(
                            p, requested, scene["initial"]), "post-choice effect completion")
                    row["boundaries"].append(boundary)
                    flush()
                if scene["room"] == "REST":
                    automatic = []
                    folded = backend._fold_protocol_only_boundaries(row["boundaries"][-1], automatic)
                    folded = backend._settle_command_boundary(folded, automatic)
                    if adapt_original(folded).decision.observation.screen != "MAP":
                        raise ValueError("rest completion did not reach actual stock map")
                    row["automatic_map_commands"] = automatic
                    row["automatic_map_boundary"] = folded
                row["capture_complete"] = True
                flush()
        backend.return_to_menu()
        result["execution_complete"] = True
        flush()
        _write_completion(0)
        return 0
    except BaseException as error:
        result["execution_error"] = f"{type(error).__name__}: {error}"
        flush()
        _write_completion(2, result["execution_error"])
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
