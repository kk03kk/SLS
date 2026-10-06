"""Recompute controlled A20 diagnostic differences without a running game."""

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.act2_differential import replay_controlled_run
from sls.rl.training_contract import native_source_digest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--oracle-build", type=Path,
                        required=True,
                        help="identify early captures through their source-built launch evidence")
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite differential evidence")
    data = json.loads(args.capture.read_text(encoding="utf-8"))
    if data.get("execution_error") or not data.get("execution_complete"):
        raise ValueError("failed stock execution cannot qualify")
    manifest_path = Path(__file__).resolve().parents[1] / "native/oracle/resources/spirecomm/parity/act2-scenes.json"
    expected_manifest = hashlib.sha256(manifest_path.read_bytes()).hexdigest()
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    build = json.loads(args.oracle_build.read_text(encoding="utf-8"))
    launch = json.loads(args.capture.with_suffix(".launch.json").read_text(encoding="utf-8"))
    if (launch["oracle_sha256"] != build["output_sha256"]
            or launch["mode"] != "validation" or launch["recovery_status"] != "RECOVERED"
            or launch["completion"]["exit_code"] != 0
            or build.get("schema") != "sls-oracle-build-v1"
            or build.get("used_existing_oracle") is not False
            or build["members"]["spirecomm/parity/act2-scenes.json"] != expected_manifest
            or build["dependencies"]["game"] != manifest["stock_jar_sha256"]
            or data.get("stock_jar_sha256", manifest["stock_jar_sha256"]) != manifest["stock_jar_sha256"]):
        raise ValueError("capture/build/launch stock identity or recovery mismatch")
    source_identity = data.get("scene_manifest_sha256")
    if source_identity is None:
        source_identity = build["members"]["spirecomm/parity/act2-scenes.json"]
    if source_identity != expected_manifest:
        raise ValueError("missing or stale stock scene source identity")
    scenes = {s["id"]: s for s in manifest["scenes"]}
    for row in data["runs"]:
        scene = row.get("scene") or {}
        if scene != scenes.get(scene.get("id")) or row["seed"] not in scene["seeds"]:
            raise ValueError("run initial state differs from immutable scene inventory")
        if row["actions"] != scene["actions"]:
            raise ValueError("run action script differs from immutable scene inventory")
    rows = [replay_controlled_run(row) for row in data["runs"]]
    args.output.write_text(json.dumps({"schema": "sls-act2-differential-v1",
        "native_source_sha256": native_source_digest(), "runs": rows,
        "stock_capture_sha256": hashlib.sha256(args.capture.read_bytes()).hexdigest(),
        "oracle_build_sha256": build["output_sha256"],
        "launch_evidence_sha256": hashlib.sha256(args.capture.with_suffix(".launch.json").read_bytes()).hexdigest(),
        "training_gate": "NOT_QUALIFIED"}, indent=2) + "\n", encoding="utf-8")
    print(json.dumps([{k: row[k] for k in ("encounter", "status", "first_divergence")}
                      for row in rows], indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
