"""Capture frozen Act1-reference stock production canaries through Act2."""

from __future__ import annotations

import argparse
import hashlib
import json
import time
from pathlib import Path

from sls.audit.act2_differential import pending_stock_intents
from sls.backends.original import ORIGINAL_EXECUTION_CONTRACT, OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.curriculum import IRONCLAD_A20_ACT2
from tools.capture_original_card_batch import _write_completion
from tools.run_original_canary import original_runtime_paths


class ProductionBackend(OriginalBackend):
    @staticmethod
    def require_isolation(raw):
        if raw.get('_oracle_mode') != 'production' or any(
            name in raw for name in ('_rng', '_stock_direct', '_parity_scenario',
                                     '_timing_evidence', '_continuation')
        ) or any(str(command).startswith('parity_') for command in raw['available_commands']):
            raise ValueError('production isolation failure at decision boundary')

    def reset(self, seed):
        decision = super().reset(seed)
        self.require_isolation(self.raw_payload)
        if pending_stock_intents(self.raw_payload):
            raise ValueError('unstable production monster intent')
        return decision

    def step(self, action):
        transition = super().step(action)
        self.require_isolation(self.raw_payload)
        if pending_stock_intents(self.raw_payload):
            raise ValueError('unstable production monster intent')
        return transition


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seeds", type=int, nargs="+", required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite production evidence")
    startup = args.output.with_suffix(".startup.jsonl")
    def mark(stage):
        with startup.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps({"stage": stage, "unix_time": time.time()}) + "\n")
    mark("HANDSHAKE_BEFORE_TORCH")
    # CommunicationMod permits only 10s for the initial child handshake.
    # Connect before importing Torch or loading the frozen weights.
    session = OriginalSession()
    # Import Torch before starting StdioTransport's background readline:
    # Windows TextIO inspection during imports can contend with that reader.
    session.transport.send("ready")
    import torch
    mark("TORCH_IMPORTED")

    from sls.diagnostics import capture_policy_trajectory
    from sls.runtime import load_policy_artifact

    torch.set_num_threads(1)
    mark("TORCH_THREADS_SET")
    artifact = load_policy_artifact(args.artifact, device="cpu")
    mark("FROZEN_ARTIFACT_LOADED")
    if (artifact.metadata.environment_profile["profile_id"] != "IRONCLAD_A20_ACT1"
            or artifact.metadata.model_sha256 !=
                "ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c"):
        raise ValueError("reference must be the frozen90 model with A20 Act1 training provenance")
    first_payload = session.receive_ready()
    session.payload = first_payload
    mark("MAIN_MENU_RECEIVED")
    # The unseeded main menu deliberately carries no dungeon instrumentation.
    if "_rng" in first_payload or first_payload.get("_oracle_mode", "production") != "production":
        raise ValueError("production isolation failure before starting a run")
    _, game = original_runtime_paths(None)
    stock_sha = hashlib.sha256((game / "desktop-1.0.jar").read_bytes()).hexdigest()
    identity = {"profile_id": "IRONCLAD_A20_ACT2", "trained_profile_id": "IRONCLAD_A20_ACT1",
                "evaluation_contract": "sls-frozen-act1-reference-act2-v1",
                "frozen_model_sha256": artifact.metadata.model_sha256,
                "stock_jar_sha256": stock_sha, "oracle_mode": "production",
                "original_execution_contract": ORIGINAL_EXECUTION_CONTRACT}
    result = {"schema": "sls-act2-production-batch-v1", "runs": [],
              "purpose": "FLOW_DIAGNOSTIC_NOT_WIN_RATE_ESTIMATE", "environment": identity}
    try:
        backend = ProductionBackend(session=session, profile=IRONCLAD_A20_ACT2)
        for seed in args.seeds:
            output = args.output.with_name(args.output.stem + f"-{seed}.jsonl")
            journal = output.with_suffix(".actions.jsonl")
            if output.exists() or journal.exists():
                raise FileExistsError("refuse to overwrite a production trajectory")
            row = capture_policy_trajectory(
                backend, artifact, backend_name="original", seed=seed, output=output,
                journal=journal, max_actions=4096, diagnostic_state=True,
                environment_identity=identity)
            raw = backend.raw_payload
            if raw.get("_oracle_mode") != "production" or "_rng" in raw or "_stock_direct" in raw:
                raise ValueError("production isolation failure")
            if not row["terminal"]:
                raise RuntimeError("incomplete production canary")
            result["runs"].append(dict(row, seed=seed,
                sha256=hashlib.sha256(output.read_bytes()).hexdigest()))
            args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        backend.return_to_menu()
        result["execution_complete"] = True
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        _write_completion(0)
        return 0
    except BaseException as error:
        result["execution_error"] = f"{type(error).__name__}: {error}"
        failure = args.output.with_suffix(".failure.json")
        with failure.open("x", encoding="utf-8") as stream:
            json.dump(session.payload, stream, indent=2)
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        _write_completion(2, result["execution_error"])
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
