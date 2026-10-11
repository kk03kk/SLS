"""Run bounded, recoverable stock batches from already frozen foundation inputs."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
import time
from pathlib import Path

from tools.verify_oracle import inspect, read_verified_launch, runtime_smoke


def run(module: str, *arguments: str) -> None:
    subprocess.run([sys.executable, "-m", "tools." + module, *arguments], check=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--oracle", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--wait-for-controlled", action="store_true")
    parser.add_argument("--resume", action="store_true", help="reuse only recovered, source-bound captures")
    args = parser.parse_args()
    inspect(args.oracle)
    root = args.root.resolve()
    def completed_capture(path):
        if not path.exists():
            return False
        if not args.resume:
            raise FileExistsError(f"capture exists; use explicit --resume: {path}")
        data = json.loads(path.read_text())
        launch = read_verified_launch(path.with_suffix(".launch.json"))
        if (not data.get("execution_complete") or data.get("execution_error")
                or launch.get("execution_error") or launch["completion"]["exit_code"] != 0
                or launch["recovery_status"] != "RECOVERED"
                or launch["oracle_sha256"] != hashlib.sha256(args.oracle.read_bytes()).hexdigest()):
            raise ValueError(f"cannot reuse incomplete/failed/source-mismatched capture: {path}")
        return True

    def replay_once(module, output, *arguments):
        if output.exists():
            from sls.rl.training_contract import native_source_digest
            if not args.resume or json.loads(output.read_text())["native_source_sha256"] != native_source_digest():
                raise ValueError(f"existing differential is not reusable: {output}")
        else:
            run(module, *arguments, "--output", str(output))
    if args.wait_for_controlled:
        deadline = time.monotonic() + 1800
        while not (root / "controlled-stock.launch.json").is_file():
            if time.monotonic() > deadline:
                raise TimeoutError("controlled batch still has no recovery receipt")
            time.sleep(1)
        launch = json.loads((root / "controlled-stock.launch.json").read_text())
        if launch["recovery_status"] != "RECOVERED" or launch.get("execution_error"):
            raise ValueError("controlled launch/recovery failed; preserve raw evidence")
    replay_once("replay_act2_encounter_batch", root / "controlled-final.json", str(root / "controlled-stock.json"),
        "--oracle-build", str(args.oracle.with_suffix(".build.json")),
        "--native-view", "production-public")
    supplemental = root / "gremlin-leader-stock.json"
    failed_launch = supplemental.with_suffix(".launch.json")
    if failed_launch.exists() and json.loads(failed_launch.read_text()).get("execution_error"):
        supplemental = root / "gremlin-leader-stock-retry.json"
    if not completed_capture(supplemental):
        runtime_smoke(args.oracle.resolve(), supplemental, "validation", None, 600,
                  capture_command=[sys.executable, str(Path(__file__).with_name("capture_act2_encounter_batch.py")),
                                   "--output", str(supplemental), "--encounters",
                                   "GREMLIN_LEADER", "GREMLIN_LEADER", "GREMLIN_LEADER",
                                   "--seed-start", "132300000", "--turns", "8"])
    from sls.audit.act2_differential import replay_controlled_run
    from sls.rl.training_contract import native_source_digest

    captured = json.loads(supplemental.read_text())
    if not (root / "gremlin-leader-final.json").exists():
        (root / "gremlin-leader-final.json").write_text(json.dumps({
            "native_source_sha256": native_source_digest(),
            "capture_basename": supplemental.stem,
            "stock_capture_sha256": hashlib.sha256(supplemental.read_bytes()).hexdigest(),
            "runs": [replay_controlled_run(r, production_projection=True) for r in captured["runs"]],
            "scope": "SUPPLEMENTAL_THIRD_ELITE; not substituted for the frozen 72 obligations"}, indent=2))
    for index, start in enumerate(range(131100063, 131100072, 3)):
        capture = root / f"systems-{index}.json"
        if not completed_capture(capture):
            run("run_act2_flow_batch", "validation", "--oracle", str(args.oracle),
            "--output", str(capture), "--scripts", str(root / "system-scripts"),
            "--seeds", *(str(s) for s in range(start, start + 3)), "--timeout", "1800")
        replay_once("replay_act2_system_batch", root / f"systems-final-{index}.json", str(capture))
        replay_once("replay_act2_system_batch", root / f"systems-conditional-{index}.json", str(capture), "--measured-runtime")
    replacements = []
    for index in range(3):
        data = json.loads((root / f"systems-{index}.json").read_text())
        for row in data["runs"]:
            if row.get("script_action_unavailable") is None:
                continue
            seed = row["seed"]
            capture = root / f"systems-policy-{seed}.json"
            if not completed_capture(capture):
                run("run_act2_flow_batch", "validation", "--oracle", str(args.oracle),
                    "--output", str(capture), "--artifact", str(args.artifact),
                    "--seeds", str(seed), "--timeout", "1800")
            plain = root / f"systems-policy-final-{seed}.json"
            conditional = root / f"systems-policy-conditional-{seed}.json"
            replay_once("replay_act2_system_batch", plain, str(capture))
            replay_once("replay_act2_system_batch", conditional, str(capture), "--measured-runtime")
            replacements.append({"seed": seed, "old_batch_index": index,
                                 "unconditional": plain.name, "conditional": conditional.name,
                                 "capture_basename": capture.stem})
    replacement_path = root / "system-replacements.json"
    if replacements and not replacement_path.exists():
        replacement_path.write_text(json.dumps({"rows": replacements}, indent=2))
    selection = json.loads((root / "selection-final.json").read_text())
    seeds = [r["seed"] for r in selection["runs"]]
    trajectories = []
    for index in range(2):
        chosen = seeds[index * 4:index * 4 + 4]
        capture = root / f"natural-{index}.json"
        if capture.with_suffix(".launch.json").exists():
            old_launch = json.loads(capture.with_suffix(".launch.json").read_text())
            if old_launch.get("execution_error") is not None or (old_launch.get("completion") or {}).get("exit_code") != 0:
                capture = root / f"natural-{index}-retry.json"
        if not completed_capture(capture):
            run("run_act2_flow_batch", "production", "--oracle", str(args.oracle),
            "--output", str(capture), "--artifact", str(args.artifact),
            "--seeds", *(str(s) for s in chosen), "--timeout", "1800")
        for seed in chosen:
            trajectory = capture.with_name(capture.stem + f"-{seed}.jsonl")
            trajectories.append(str(trajectory))
            replay_once("replay_act2_production_trajectory", root / f"natural-final-{seed}.json", str(trajectory))
            result = json.loads((root / f"natural-final-{seed}.json").read_text())
            if result["first_divergence"] is not None:
                replay_once("replay_act2_clock_conditioned_trajectory", root / f"natural-conditional-{seed}.json",
                            str(trajectory), "--oracle-build", str(args.oracle.with_suffix(".build.json")))
    if not (root / "stock-coverage.json").exists():
        run("check_act2_production_coverage", "--selection", str(root / "selection-final.json"),
            "--trajectories", *trajectories, "--output", str(root / "stock-coverage.json"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
