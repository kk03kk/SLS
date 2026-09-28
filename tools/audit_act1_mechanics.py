"""Bounded stock-bytecode comparisons for RNG, transform selection and gremlin moves."""

from __future__ import annotations

import argparse
import json
import os
import struct
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.audit.semantic_coverage import validate_semantic_coverage  # noqa: E402
from sls.backends.simulator import native  # noqa: E402
from sls.content.registry import load_content_registry  # noqa: E402
from sls.rl.training_contract import native_artifact, sha256_file  # noqa: E402

PINNED_JAR = "cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673"
SEEDS = [0, 1, 2, 3, 42, 20260926, 6000000000000, 7000000000000,
         (1 << 63) - 1, 1 << 63, (1 << 64) - 1]


def normalized_state(value: dict) -> dict:
    return {"counter": int(value["counter"]),
            "seed0": int(value["seed0"]), "seed1": int(value["seed1"])}


def normalized_values(value: dict) -> dict:
    float_keys = {"unit_float", "float_range", "float_between"}
    return {key: struct.pack("!f", item).hex() if key in float_keys else item
            for key, item in value.items()}


def attach_branch_evidence(coverage: dict, result: dict) -> dict:
    validate_semantic_coverage(coverage)
    if result.get("passed") is not True:
        raise ValueError("failed probe cannot promote a coverage obligation")
    for field in ("stock_jar_sha256", "native_source_sha256"):
        if coverage.get(field) != result.get(field):
            raise ValueError(f"branch evidence differs from coverage {field}")
    reviewed = json.loads(json.dumps(coverage))
    for row in reviewed["obligations"]:
        movement = (row["category"] == "monsters"
                    and row["content_id"] in {"GREMLIN_NOB", "SHIELD_GREMLIN", "SNEAKY_GREMLIN"}
                    and str(row.get("java_method", "")).strip() == "protected void getMove(int);")
        system = row["category"] == "systems" and row["content_id"] in {"RNG_STREAMS", "CARD_POOLS"}
        if not (movement or system) or row["status"] not in {"UNREVIEWED", "BRANCH_PARTIAL"}:
            continue
        row["status"] = "BRANCH_PARTIAL"
        row["branch_evidence"] = {
            "stock_jar_sha256": result["stock_jar_sha256"],
            "native_source_sha256": result["native_source_sha256"],
            "native_artifact_sha256": result["native_artifact_sha256"],
            "probe_source_sha256": result["probe_source_sha256"],
            "comparisons_sha256": result["comparisons_sha256"],
            "covered": "A20 selected move byte over declared histories and rolls" if movement else
                       "Mixed RNG core/Java shuffle or supplied-pool transform selection",
            "remaining": "Full state/intent/damage, acquisition, consumers and interactions remain unqualified",
        }
    return reviewed


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--java", type=Path, default=Path("java"))
    parser.add_argument("--javac", type=Path, default=Path("javac"))
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--coverage", type=Path, help="current method baseline to annotate with partial evidence")
    args = parser.parse_args()
    stock_jar = args.stock_jar.resolve()
    if sha256_file(stock_jar) != PINNED_JAR:
        raise ValueError("stock JAR is not the reviewed authority")
    if args.output_dir.exists():
        raise ValueError("use a new output directory to preserve evidence")
    built = native_artifact()
    if built is None:
        raise RuntimeError("current native artifact required")
    output = args.output_dir.resolve()
    output.mkdir(parents=True)
    classes = output / "classes"
    classes.mkdir()
    source = ROOT / "tools/java/StockAct1MechanicsProbe.java"
    compile_result = subprocess.run(
        [str(args.javac), "-proc:none", "-cp", str(stock_jar), "-d", str(classes), str(source)],
        capture_output=True, timeout=60,
    )
    (output / "javac.stdout.log").write_bytes(compile_result.stdout)
    (output / "javac.stderr.log").write_bytes(compile_result.stderr)
    compile_result.check_returncode()
    registry = load_content_registry().categories["cards"]
    game_ids = {row["id"]: row["game_id"] for row in registry}
    rows, expected = [], []
    for seed in SEEDS:
        rows.append({"mode": "rng", "seed": str(seed)})
        probe = native.rng_probe(seed)
        expected.append({"initial": normalized_state(probe["initial"]),
                         "final": normalized_state(probe["final"]),
                         "values": normalized_values(probe["values"]),
                         "shuffle": native.shuffle_probe(seed)})
    for kind, absent in (("colored", "BASH"), ("colorless", "APPARITION"),
                         ("curse", "ASCENDERS_BANE")):
        pool = native.transform_selection_probe(0, kind, absent)["pool"]
        for excluded in [*pool, absent]:
            for seed in SEEDS[:8]:
                probe = native.transform_selection_probe(seed, kind, excluded)
                rows.append({"mode": "transform", "seed": str(seed), "kind": kind,
                             "pool": [game_ids[item] for item in pool], "exclude": game_ids[excluded]})
                initial = native.rng_probe(seed)["initial"]
                expected.append({"initial": normalized_state(initial),
                                 "final": normalized_state(probe["final"]),
                                 "selected": game_ids[probe["selected"]]})
    for monster in ("GremlinNob", "GremlinTsundere", "GremlinThief"):
        histories = [[], [3], [1], [2], [1, 1], [2, 1], [1, 2]] if monster == "GremlinNob" else [[]]
        for history in histories:
            for roll in range(100):
                rows.append({"mode": "move", "seed": "0", "monster": monster,
                             "history": history, "roll": roll})
                state = normalized_state(native.rng_probe(0)["initial"])
                expected.append({"initial": state, "final": state,
                                 "selected": native.act1_gremlin_move_probe(monster, history, roll)})
    for alive in ([True], [True, False, False], [True, True, False],
                  [True, False, True], [True, True, True, True]):
        for seed in SEEDS:
            rows.append({"mode": "block", "seed": str(seed), "alive": alive})
            probe = native.act1_gremlin_block_probe(seed, alive)
            expected.append({"initial": normalized_state(native.rng_probe(seed)["initial"]),
                             "final": normalized_state(probe["final"]), "blocks": probe["blocks"]})
    input_path = output / "input.json"
    input_path.write_text(json.dumps(rows) + "\n", encoding="utf-8")
    completed = subprocess.run(
        [str(args.java), "-cp", os.pathsep.join((str(classes), str(stock_jar))),
         "StockAct1MechanicsProbe", str(input_path)],
        cwd=output, capture_output=True, timeout=60,
    )
    (output / "stock.stdout.log").write_bytes(completed.stdout)
    (output / "stock.stderr.log").write_bytes(completed.stderr)
    completed.check_returncode()
    json_lines = [line.split("=", 1)[1] for line in completed.stdout.decode("utf-8").splitlines()
                  if line.startswith("SLS_STOCK_JSON=")]
    if len(json_lines) != 1:
        raise ValueError("stock probe must return exactly one result batch")
    actual = json.loads(json_lines[0])
    if len(actual) != len(expected):
        raise ValueError("stock result batch is incomplete")
    comparisons = []
    for index, (row, left, right) in enumerate(zip(rows, actual, expected, strict=True)):
        left["initial"] = normalized_state(left["initial"])
        left["final"] = normalized_state(left["final"])
        if row["mode"] == "rng":
            left["values"] = normalized_values(left["values"])
        comparisons.append({"case_index": index, "mode": row["mode"], "passed": left == right,
                            "stock": left, "native": right})
    (output / "comparisons.json").write_text(json.dumps(comparisons, indent=2) + "\n", encoding="utf-8")
    counts = {mode: sum(row["mode"] == mode for row in comparisons) for mode in ("rng", "transform", "move", "block")}
    failures = [row["case_index"] for row in comparisons if not row["passed"]]
    result = {
        "schema": "sls-act1-mechanics-parity-v1", "stock_jar_sha256": PINNED_JAR,
        "probe_source_sha256": sha256_file(source), "native_source_sha256": built["source_sha256"],
        "native_artifact_sha256": built["sha256"], "input_sha256": sha256_file(input_path),
        "comparisons_sha256": sha256_file(output / "comparisons.json"),
        "counts": counts, "passed": not failures, "failed_case_indices": failures,
        "qualification": "Independent stock method execution on supplied test state. "
                         "Transform pool ordering and full acquisition are not certified; "
                         "Block targeting uses empty power lists; gremlin constructors, takeTurn, "
                         "damage/death and hooks are not exercised. "
                         "RNG checks cover nine mixed operations and Java shuffle, not all stream consumers.",
    }
    (output / "summary.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    if args.coverage is not None:
        coverage = attach_branch_evidence(json.loads(args.coverage.read_text(encoding="utf-8")), result)
        (output / "reviewed-coverage.json").write_text(json.dumps(coverage, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))
    return int(bool(failures))


if __name__ == "__main__":
    raise SystemExit(main())
