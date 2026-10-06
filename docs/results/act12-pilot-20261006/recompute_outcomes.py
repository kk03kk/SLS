"""Recompute downloaded outcome evidence without private checkpoint weights."""

# ruff: noqa: E402
import gzip
import hashlib
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[2]))
from tools.analyze_act12_pilot import paired_act12, require_pilot_health
from tools.analyze_reward_screen import outcomes


def main():
    bundle = json.loads((HERE / "training-bundle.json").read_text())
    records = {}
    for name in (
        "reference-evaluation.json",
        "endpoint-evaluation.json",
        "final-evaluation.json",
    ):
        raw = gzip.decompress((HERE / (name + ".gz")).read_bytes())
        assert hashlib.sha256(raw).hexdigest() == bundle["files"][name]
        records[name] = json.loads(raw)
    raw = gzip.decompress((HERE / "metrics.jsonl.gz").read_bytes())
    assert (
        hashlib.sha256(raw).hexdigest() == bundle["files"]["stages/train/metrics.jsonl"]
    )
    metrics = [json.loads(line) for line in raw.splitlines()]
    reference, endpoint, selected = records.values()
    pair = paired_act12(reference, endpoint)
    selected_pair = paired_act12(reference, selected)
    require_pilot_health(
        [record["result"] for record in records.values()]
        + [row["evaluation"] for row in metrics if "evaluation" in row],
        metrics,
    )
    stored = json.loads((HERE / "analysis.json").read_text())
    assert pair == stored["endpoint_vs_parent"]
    assert selected_pair == stored["selected_vs_parent"]
    for name, record in zip(
        ("frozen_parent", "endpoint", "selected"), records.values()
    ):
        computed = outcomes(record["result"], tuple(record["seeds"]), horizon=2)
        assert (
            json.loads(json.dumps(computed)) == stored["confirmation_diagnostics"][name]
        )
    print(
        json.dumps(
            {
                "endpoint_vs_parent": pair,
                "selected_vs_parent": selected_pair,
                "evidence": "Raw outcome hash/aggregate/paired-identity verification; no private model tensor audit here.",
            },
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
