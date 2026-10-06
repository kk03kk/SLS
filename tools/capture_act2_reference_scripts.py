"""Freeze normal-start actions for flow probes; native states are not stock expectations."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import torch

from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.diagnostics import capture_policy_trajectory
from sls.rl.training_contract import native_source_digest
from sls.runtime import load_policy_artifact


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--seeds", type=int, nargs="+", required=True)
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=False)
    torch.set_num_threads(1)
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("stale native artifact")
    artifact = load_policy_artifact(args.artifact, device="cpu")
    if artifact.metadata.environment_profile["profile_id"] != "IRONCLAD_A20_ACT1":
        raise ValueError("reference must preserve its A20 Act1 training identity")
    rows = []
    for seed in args.seeds:
        output = args.output_dir / f"{seed}.jsonl"
        result = capture_policy_trajectory(
            SimulatorBackend(IRONCLAD_A20_ACT2), artifact, backend_name="simulator", seed=seed,
            output=output, max_actions=4096, diagnostic_state=True,
            environment_identity={"profile_id": "IRONCLAD_A20_ACT2",
                                  "trained_profile_id": "IRONCLAD_A20_ACT1",
                                  "evaluation_contract": "sls-frozen-act1-reference-act2-v1",
                                  "native_source_sha256": native_source_digest()})
        rows.append(dict(result, seed=seed, sha256=hashlib.sha256(output.read_bytes()).hexdigest()))
        print(json.dumps({"seed": seed, "act": result["act"], "terminal": result["terminal"]}), flush=True)
    (args.output_dir / "manifest.json").write_text(json.dumps({
        "schema": "sls-act2-reference-action-scripts-v1", "model_sha256": artifact.metadata.model_sha256,
        "expected_results_source": "STOCK_ONLY; these trajectories specify actions and diagnostic selection",
        "runs": rows,
    }, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
