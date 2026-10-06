"""Launch one verified, recoverable validation batch, bounded to 30 minutes."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

from tools.run_original_canary import original_runtime_paths
from tools.verify_oracle import inspect, runtime_smoke


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--oracle", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--scenes", nargs="+", required=True)
    parser.add_argument("--timeout", type=float, default=1800)
    args = parser.parse_args()
    if not 0 < args.timeout <= 1800:
        raise ValueError("controlled batch timeout must be <=30 minutes")
    report = inspect(args.oracle)
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    if manifest["stock_jar_sha256"] != report["dependencies"]["game"]:
        raise ValueError("stock source identity differs from Oracle build")
    name = "spirecomm/parity/act2-scenes.json"
    if hashlib.sha256(args.manifest.read_bytes()).hexdigest() != report["members"][name]:
        raise ValueError("scene manifest differs from source-built Oracle")
    _, game = original_runtime_paths(None)
    with (game / "desktop-1.0.jar").open("rb") as stream:
        actual_stock = hashlib.file_digest(stream, "sha256").hexdigest()
    if actual_stock != manifest["stock_jar_sha256"]:
        raise ValueError("installed stock JAR differs from reviewed sources")
    ready = {scene["id"] for scene in manifest["scenes"] if "initial" in scene}
    if set(args.scenes) - ready:
        raise ValueError("unknown or unprepared scenes; no game launched")
    command = [Path(sys.executable).as_posix(), (Path(__file__).with_name("capture_act2_encounter_batch.py")).as_posix(),
               "--output", args.output.resolve().as_posix(), "--manifest", args.manifest.resolve().as_posix(),
               "--scenes", *args.scenes]
    result = runtime_smoke(args.oracle.resolve(), args.output.resolve(), "validation", None,
                           args.timeout, capture_command=command)
    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
