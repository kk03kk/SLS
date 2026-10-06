"""Compare existing production paths in two DLLs, in isolated processes."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--worker", action="store_true")
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite source preservation evidence")
    if args.worker:
        from sls.backends.simulator import native
        rows = []
        for ascension in (0, 20):
            for seed in (131100000, 131100031, 8000011000000, 8000011000127):
                run = native.LightspeedRunState()
                run.reset(seed, ascension)
                for step in range(256):
                    state = run.snapshot()
                    actions = run.legal_actions()
                    rows.append({"seed": seed, "ascension": ascension, "step": step,
                                 "state": state, "actions": actions})
                    if not actions:
                        break
                    run.step(actions[0]["bits"])
        args.output.write_text(json.dumps(rows, sort_keys=True) + "\n", encoding="utf-8")
        return 0
    if args.baseline is None:
        raise ValueError("baseline directory required")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    results = []
    sources = {}
    artifacts = {}
    for role, directory in (("baseline", args.baseline), ("current", Path("local/build/native/cpython-312"))):
        output = args.output.with_name(args.output.stem + "-" + role + ".json")
        environment = {**os.environ, "SLS_NATIVE_BUILD_DIR": str(directory.resolve())}
        sources[role] = subprocess.check_output([
            sys.executable, "-c", "from sls.backends.simulator import native; print(native.NATIVE_SOURCE_SHA256)"
        ], env=environment, text=True).strip()
        binary = next(directory.glob("_lightspeed*.pyd"))
        artifacts[role] = hashlib.sha256(binary.read_bytes()).hexdigest()
        subprocess.run([sys.executable, str(Path(__file__).resolve()), "--worker", "--output", str(output)],
                       env=environment, check=True)
        results.append(output)
    left, right = [path.read_bytes() for path in results]
    result = {"schema": "sls-probe-source-preservation-v1", "identical": left == right,
              "baseline_sha256": hashlib.sha256(left).hexdigest(),
              "current_sha256": hashlib.sha256(right).hexdigest(),
              "boundaries": len(json.loads(left)),
              "sources": sources, "artifacts": artifacts,
              "scope": "A0/A20 normal full-run paths, first-legal-action scripts, four fixed seeds each",
              "limitation": "bounded runtime evidence plus source diff review; not complete Act2 parity"}
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))
    return 0 if left == right else 1


if __name__ == "__main__":
    raise SystemExit(main())
