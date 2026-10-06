"""Small stock A20 multi-boundary harness diagnostic, not qualification."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A20_ACT2
from tools.capture_original_card_batch import _write_completion


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--encounters", nargs="+", default=["BOOK_OF_STABBING"])
    parser.add_argument("--turns", type=int, default=3)
    parser.add_argument("--manifest", type=Path)
    parser.add_argument("--scenes", nargs="*")
    args = parser.parse_args()
    from tools.build_oracle import resource_payloads
    allowlist = resource_payloads()["spirecomm/parity/scenario-encounter-allowlist.tsv"].decode()
    allowed = {line.split("\t")[0] for line in allowlist.splitlines()}
    if set(args.encounters) - allowed:
        raise ValueError(f"unknown encounter IDs: {sorted(set(args.encounters) - allowed)}")
    manifest = json.loads(args.manifest.read_text(encoding="utf-8")) if args.manifest else None
    cases = []
    if manifest:
        for scene in manifest["scenes"]:
            if scene.get("initial") and (args.scenes is None or scene["id"] in args.scenes):
                cases.extend((scene, seed) for seed in scene["seeds"])
        if args.scenes and set(args.scenes) - {s["id"] for s, _ in cases}:
            raise ValueError("unknown or unprepared scenes")
    else:
        cases = [(None, 131100000 + i) for i in range(len(args.encounters))]
    if args.output.exists():
        raise FileExistsError("refuse to overwrite stock evidence")
    result = {"schema": "sls-act2-encounter-capture-v1", "purpose": "HARNESS_DIAGNOSTIC", "runs": []}
    if manifest:
        result.update(scene_manifest_sha256=hashlib.sha256(args.manifest.read_bytes()).hexdigest(),
                      stock_jar_sha256=manifest["stock_jar_sha256"])
    try:
        session = OriginalSession()
        backend = OriginalBackend(session=session, profile=IRONCLAD_A20_ACT2)
        for index, (scene, seed) in enumerate(cases):
            encounter = scene["encounter"] if scene else args.encounters[index]
            decision = backend.reset(seed)
            for _ in range(40):
                if decision.observation.screen == "COMBAT":
                    break
                preferred = next((a for a in decision.actions if a.kind in {
                    ActionKind.CHOOSE_NEOW_OPTION, ActionKind.CHOOSE_MAP_NODE,
                    ActionKind.SELECT_CARD, ActionKind.CONFIRM,
                }), decision.actions[0])
                decision = backend.step(preferred).decision
            else:
                raise RuntimeError("failed to enter combat")
            before = backend.raw_payload
            if before.get("_oracle_mode") != "validation":
                raise ValueError("controlled probes require validation mode")
            initial = session.execute(f"parity_act2 {scene['id']}" if scene else
                                      f"parity_encounter {encounter} 20 2 20 harness-{index}")
            row = {"seed": seed, "encounter": encounter, "ascension": 20, "act": 2,
                   "floor": 20, "before": before, "boundaries": [initial], "actions": []}
            result["runs"].append(row)
            if scene:
                row["scene"] = scene
            scripts = scene["actions"] if scene else [{"kind": "end_turn"}] * args.turns
            for action in scripts:
                kind = action["kind"]
                if kind == "end_turn":
                    command = "end"
                elif kind == "play":
                    command = f"play {action['card_index']}"
                    if "target_index" in action:
                        command += f" {action['target_index']}"
                elif kind == "potion":
                    command = f"potion use {action['potion_index']} {action['target_index']}"
                else:
                    raise ValueError("unsupported semantic action")
                row["actions"].append(action)
                row["boundaries"].append(session.execute(command))
            # Flush raw evidence after each completed run; append only in this
            # exclusively-created result, never replace evidence from prior runs.
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        backend.return_to_menu()
        result["execution_complete"] = True
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        _write_completion(0)
        return 0
    except BaseException as error:
        result["execution_error"] = f"{type(error).__name__}: {error}"
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        _write_completion(2, result["execution_error"])
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
