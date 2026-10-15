"""Source-bound differential replay of normal-start stock key-route records."""

import argparse
import hashlib
import json
from pathlib import Path

from sls.backends.simulator import native
from sls.curriculum import ironclad_fullrun_profile
from sls.rl.training_contract import native_source_digest
from tools.replay_act2_system_batch import replay
from tools.verify_oracle import read_verified_launch


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--oracle-build", type=Path, required=True)
    parser.add_argument("--measured-runtime", action="store_true")
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite differential evidence")
    capture = json.loads(args.capture.read_text())
    if (capture.get("schema") != "sls-fullrun-stock-key-route-v1"
            or not capture.get("execution_complete") or capture.get("execution_error")
            or capture.get("oracle_mode") != "validation"
            or capture.get("profile_id") != "IRONCLAD_A20_HEART"
            or capture.get("model_sha256") != "ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c"
            or capture.get("capture_tool_sha256") != hashlib.sha256(
                Path(__file__).with_name("capture_fullrun_key_route.py").read_bytes()).hexdigest()):
        raise ValueError("incomplete or unidentified stock key-route capture")
    if native_source_digest() != native.NATIVE_SOURCE_SHA256:
        raise ValueError("stale native build")
    launch_path = args.capture.with_suffix(".launch.json")
    launch = read_verified_launch(launch_path)
    build = json.loads(args.oracle_build.read_text())
    if (launch["mode"] != "validation" or launch["recovery_status"] != "RECOVERED"
            or launch["completion"]["exit_code"] != 0):
        raise ValueError("stock batch launch/recovery did not complete")
    if (build.get("schema") != "sls-oracle-build-v1" or build.get("used_existing_oracle") is not False
            or launch["oracle_sha256"] != build["output_sha256"]
            or capture["stock_jar_sha256"] != build["dependencies"]["game"]):
        raise ValueError("unidentified Oracle build or stock game source")
    rows = []
    for row in capture["runs"]:
        records = [json.loads(line) for line in Path(row["stock_capture"]).read_text().splitlines()]
        if not records or (records[0]["observation"]["run"]["act"],
                           records[0]["observation"]["run"]["floor"],
                           records[0]["observation"]["screen"]) != (1, 0, "NEOW"):
            raise ValueError("key route lacks a normal-start witness")
        if any(record["selection"] not in {"PUBLIC_KEY_CHOICE", "PUBLIC_PATH_TO_BURNING_ELITE",
                                           "FROZEN_POLICY_FALLBACK"} for record in records):
            raise ValueError("unknown key-route selection contract")
        rows.append(replay(dict(row, script_action_unavailable=None),
                           measured_runtime=args.measured_runtime,
                           profile=ironclad_fullrun_profile(20, require_heart=True)))
    result = {
        "schema": "sls-fullrun-key-route-differential-v1", "runs": rows,
        "native_source_sha256": native_source_digest(), "training_gate": "NOT_QUALIFIED",
        "policy_contract": capture["purpose"],
        "stock_capture_sha256": hashlib.sha256(args.capture.read_bytes()).hexdigest(),
        "launch_evidence_sha256": hashlib.sha256(launch_path.read_bytes()).hexdigest(),
        "oracle_sha256": launch["oracle_sha256"],
        "oracle_build_sha256": hashlib.sha256(args.oracle_build.read_bytes()).hexdigest(),
        "audit_sources": {name: hashlib.sha256(Path(name).read_bytes()).hexdigest() for name in (
            "tools/replay_fullrun_key_route.py", "tools/replay_act2_system_batch.py",
            "src/sls/audit/decision_identity.py", "src/sls/audit/act2_differential.py")},
    }
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps([{
        "seed": row["seed"], "status": row["status"], "boundaries_checked": row["boundaries_checked"],
        "first_divergence": (None if row["first_divergence"] is None else {
            k: row["first_divergence"].get(k) for k in ("boundary", "classification", "screen", "differences")
        }),
    } for row in rows]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
