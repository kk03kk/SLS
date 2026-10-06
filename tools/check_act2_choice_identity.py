"""Prove a witnessed UI choice alias leaves every policy input tensor unchanged."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.card_parity import structured_differences
from sls.audit.decision_identity import canonical_projection, mapped_action
from sls.backends.original.adapter import adapt_original
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model.batching import PolicyBatch
from sls.rl.training_contract import native_source_digest
from tools.replay_act2_system_batch import stock_runtime_inputs


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path)
    parser.add_argument("--seed", type=int, required=True)
    parser.add_argument("--boundary", type=int, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite identity proof")
    rows = [json.loads(line) for line in args.capture.read_text(encoding="utf-8").splitlines()]
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(args.seed)
    for index, row in enumerate(rows[:args.boundary]):
        expected = canonical_projection(row["observation"], row["actions"], row["terminal"])
        actual = canonical_projection(decision.observation.to_dict(), [a.to_dict() for a in decision.actions], decision.terminal)
        if structured_differences(expected, actual):
            raise ValueError("prior material divergence prevents a choice alias proof")
        chosen = mapped_action(row["requested_action"], row["observation"], decision.observation.to_dict())
        evidence = stock_runtime_inputs(row, rows[index + 1])
        decision = backend.step(next(a for a in decision.actions if a.to_dict() == chosen),
                                validation_evidence=evidence).decision
    stock = adapt_original(rows[args.boundary]["stock_raw"]).decision
    left, right = PolicyBatch.from_decisions([stock]), PolicyBatch.from_decisions([decision])
    identical = all(getattr(left, key).equal(getattr(right, key)) for key in left.__dataclass_fields__)
    result = {"schema": "sls-choice-identity-proof-v1", "seed": args.seed, "boundary": args.boundary,
        "stock_capture_sha256": hashlib.sha256(args.capture.read_bytes()).hexdigest(),
        "native_source_sha256": native_source_digest(), "all_policy_batch_fields_identical": identical,
        "tensor_hashes": {key: hashlib.sha256(getattr(left, key).numpy().tobytes()).hexdigest()
                          for key in left.__dataclass_fields__},
        "scope": "witnessed ordered choice-ID alpha-renaming only; no HP/cost/mask/RNG normalization"}
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    return 0 if identical else 1


if __name__ == "__main__":
    raise SystemExit(main())
