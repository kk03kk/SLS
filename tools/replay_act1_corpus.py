"""Replay pinned stock trajectories against the current native build on CPU.

This is a bounded regression corpus, not an evaluation of model strength or
a certificate that all stock branches are implemented.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch  # noqa: E402

from sls.audit.act1_targets import PROFILE_ID, target_ids  # noqa: E402
from sls.backends.simulator import SimulatorBackend  # noqa: E402
from sls.curriculum import IRONCLAD_A20_ACT1  # noqa: E402
from sls.diagnostics.canary import (  # noqa: E402
    capture_policy_trajectory,
    compare_trajectories,
    read_trajectory,
)
from sls.rl.training_contract import native_artifact, sha256_file  # noqa: E402
from sls.runtime import load_policy_artifact  # noqa: E402


def validate_corpus(corpus: dict, stock_jar: Path, artifact) -> None:
    if corpus.get("schema") != "sls-act1-regression-corpus-v1":
        raise ValueError("unsupported regression corpus")
    if corpus.get("profile_id") != PROFILE_ID:
        raise ValueError("corpus must declare Ironclad A20 Act1")
    if sha256_file(stock_jar) != corpus.get("stock_jar_sha256"):
        raise ValueError("stock JAR differs from corpus authority")
    if artifact.metadata.model_sha256 != corpus.get("model_sha256"):
        raise ValueError("policy weights differ from recorded stock trajectories")
    profile = artifact.metadata.environment_profile or {}
    if profile.get("profile_id") != PROFILE_ID:
        raise ValueError("policy must declare Ironclad A20 Act1")
    cases = corpus.get("cases")
    if not isinstance(cases, list) or not cases:
        raise ValueError("corpus must contain at least one case")
    seeds = set()
    hashes = set()
    for case in cases:
        if case["seed"] in seeds or case["original_sha256"] in hashes:
            raise ValueError("corpus cases must have distinct seeds and evidence")
        seeds.add(case["seed"])
        hashes.add(case["original_sha256"])
        path = ROOT / case["original_path"]
        if sha256_file(path) != case["original_sha256"]:
            raise ValueError(f"stock trajectory hash mismatch: {path}")
        metadata, boundaries = read_trajectory(path)
        if metadata["backend"] != "original" or metadata["seed"] != case["seed"]:
            raise ValueError(f"wrong stock trajectory identity: {path}")
        if not boundaries or not boundaries[-1].get("terminal"):
            raise ValueError(f"stock trajectory is incomplete: {path}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--targets", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    if args.output_dir.exists():
        raise ValueError("use a new output directory; existing audit evidence is preserved")
    artifact = load_policy_artifact(args.artifact, device="cpu")
    corpus = json.loads(args.corpus.read_text(encoding="utf-8"))
    threads = corpus.get("cpu_threads")
    if not isinstance(threads, int) or isinstance(threads, bool) or threads <= 0:
        raise ValueError("corpus must pin a positive CPU thread count")
    torch.set_num_threads(threads)
    validate_corpus(corpus, args.stock_jar, artifact)
    built = native_artifact()
    if built is None:
        raise RuntimeError("current native build is required")
    targets = json.loads(args.targets.read_text(encoding="utf-8"))
    authority = targets["authority"]
    if authority["native_source_sha256"] != built["source_sha256"]:
        raise ValueError("audit targets refer to a different native source")
    if authority["stock_jar_sha256"] != corpus["stock_jar_sha256"]:
        raise ValueError("audit targets refer to a different stock JAR")
    args.output_dir.mkdir(parents=True)
    identity = {"profile_id": PROFILE_ID, "native_source_sha256": built["source_sha256"],
                "native_artifact_sha256": built["sha256"]}
    observed = {category: set() for category in ("cards", "relics", "potions", "monsters")}
    rows = []
    for case in corpus["cases"]:
        output = args.output_dir / f"seed-{case['seed']}-simulator.jsonl"
        capture_policy_trajectory(
            SimulatorBackend(IRONCLAD_A20_ACT1), artifact,
            backend_name="simulator", seed=case["seed"], output=output,
            max_actions=1000, environment_identity=identity,
        )
        comparison = compare_trajectories(output, ROOT / case["original_path"])
        rows.append(comparison)
        (args.output_dir / f"seed-{case['seed']}-comparison.json").write_text(
            json.dumps(comparison, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        if comparison["passed"] and comparison["trajectory_complete"]:
            _, boundaries = read_trajectory(output)
            for boundary in boundaries:
                observation = boundary["observation"]
                for zone in ("deck", "hand", "draw_pile", "discard_pile", "exhaust_pile"):
                    observed["cards"].update(card["card_id"] for card in observation.get(zone, []))
                for category in ("relics", "potions"):
                    observed[category].update(item["content_id"] for item in observation.get(category, []))
                observed["monsters"].update(item["monster_id"] for item in observation.get("enemies", []))
        print(json.dumps({key: comparison[key] for key in (
            "seed", "passed", "matched_boundaries", "trajectory_complete",
        )}), flush=True)
    passed = all(row["passed"] and row["trajectory_complete"] for row in rows)
    report = {
        "schema": "sls-act1-regression-result-v1", "environment": identity,
        "stock_jar_sha256": corpus["stock_jar_sha256"],
        "corpus_sha256": sha256_file(args.corpus), "targets_sha256": sha256_file(args.targets),
        "model_sha256": artifact.metadata.model_sha256,
        "cpu_threads": torch.get_num_threads(),
        "regression_passed": passed, "semantic_qualification_complete": False,
        "qualification": "Observed trajectories only; no branch-completeness or strength claim. "
                         "Historical stock JAR provenance is declared by the pinned corpus, "
                         "not recovered from legacy trajectory metadata. RNG internals are not compared.",
        "matched_boundaries": sum(row["matched_boundaries"] for row in rows),
        "cases": rows,
        "observed_content": {category: sorted(ids) for category, ids in observed.items()},
        "not_observed_candidates": {
            category: sorted(target_ids(targets, category) - ids)
            for category, ids in observed.items()
        },
        "not_inferred": ["events", "encounters", "method branches", "RNG internals"],
    }
    (args.output_dir / "summary.json").write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    return int(not passed)


if __name__ == "__main__":
    raise SystemExit(main())
