"""Replay frozen semantic scripts in stock validation from normal Neow."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.act2_differential import pending_stock_intents
from sls.audit.decision_identity import choice_aliases, mapped_action
from sls.backends.original import ORIGINAL_EXECUTION_CONTRACT, OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.curriculum import IRONCLAD_A20_ACT2
from tools.capture_original_card_batch import _write_completion


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--scripts", type=Path, required=True)
    parser.add_argument("--seeds", type=int, nargs="+", required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite stock system evidence")
    manifest = json.loads((args.scripts / "manifest.json").read_text(encoding="utf-8"))
    scripts = {row["seed"]: row for row in manifest["runs"]}
    for seed in args.seeds:
        path = args.scripts / f"{seed}.jsonl"
        if hashlib.sha256(path.read_bytes()).hexdigest() != scripts[seed]["sha256"]:
            raise ValueError("frozen script identity mismatch")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    result = {"schema": "sls-act2-stock-system-capture-v1", "runs": [],
              "expected_source": "STOCK_RAW_OBJECTS_AND_ACTIONS", "initial_state": "NORMAL_A20_NEOW",
              "original_execution_contract": ORIGINAL_EXECUTION_CONTRACT,
              "script_contract": "sls-controlled-semantic-ordinal-v2"}
    try:
        backend = OriginalBackend(session=OriginalSession(), profile=IRONCLAD_A20_ACT2)
        for seed in args.seeds:
            # This child must handshake with CommunicationMod within 10s.
            # Loading Torch just to read JSONL can exceed that deadline.
            records = [json.loads(line) for line in (args.scripts / f"{seed}.jsonl").read_text(
                encoding="utf-8").splitlines() if line.strip()]
            if records[0]["backend"] != "simulator" or records[0]["seed"] != seed:
                raise ValueError("frozen script metadata mismatch")
            requested = records[1:]
            decision = backend.reset(seed)
            if backend.raw_payload.get("_oracle_mode") != "validation":
                raise ValueError("system controlled scripts require validation mode")
            output = args.output.with_name(args.output.stem + f"-{seed}.jsonl")
            row = {"seed": seed, "script_sha256": scripts[seed]["sha256"],
                   "stock_capture": output.as_posix(), "script_action_unavailable": None}
            result["runs"].append(row)
            with output.open("x", encoding="utf-8") as stream:
                for index, request in enumerate(requested):
                    if pending_stock_intents(backend.raw_payload):
                        raise ValueError('unstable stock system monster intent')
                    chosen = request["chosen_action"]
                    mapping_error = None
                    if chosen is not None:
                        try:
                            # Frozen requests prescribe semantic indices, not
                            # native-generated expected card properties. Only
                            # differing UI identifier schemes require a proved
                            # property-preserving alias. The differential still
                            # compares every candidate property separately.
                            if choice_aliases(request["observation"]) != choice_aliases(decision.observation.to_dict()):
                                chosen = mapped_action(chosen, request["observation"], decision.observation.to_dict())
                        except ValueError as error:
                            mapping_error = str(error)
                    stream.write(json.dumps({"boundary": index, "observation": decision.observation.to_dict(),
                        "actions": [a.to_dict() for a in decision.actions], "terminal": decision.terminal,
                        "requested_action": chosen, "stock_raw": backend.raw_payload,
                        "previous_action_validation_evidence": backend.last_validation_evidence}, sort_keys=True) + "\n")
                    stream.flush()
                    if chosen is None:
                        break
                    matches = [a for a in decision.actions if a.to_dict() == chosen]
                    if mapping_error is not None or len(matches) != 1:
                        row["script_action_unavailable"] = index
                        row["mapping_error"] = mapping_error
                        break
                    decision = backend.step(matches[0]).decision
            row.update(boundaries=index + 1, terminal=decision.terminal,
                       sha256=hashlib.sha256(output.read_bytes()).hexdigest(),
                       reached_act=decision.observation.run.act)
            args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        backend.return_to_menu()
        result["execution_complete"] = True
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        _write_completion(0)
        return 0
    except BaseException as error:
        result["execution_error"] = f"{type(error).__name__}: {error}"
        with args.output.with_suffix(".failure.json").open("x", encoding="utf-8") as stream:
            json.dump(backend.session.payload if "backend" in locals() else None, stream, indent=2)
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        _write_completion(2, result["execution_error"])
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
