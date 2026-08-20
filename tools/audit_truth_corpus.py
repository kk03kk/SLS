"""Offline audit and clustering for every local Original truth artifact."""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import sys
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "tools"))

from replay_truth import replay
from sls.validation.truth import load_bundle, value_hash


def payload_fingerprint(payload: dict[str, Any]) -> str:
    game = payload.get("game_state") or {}
    shape = {
        "top": sorted(payload), "game": sorted(game),
        "screen_state": sorted((game.get("screen_state") or {})),
        "combat": sorted((game.get("combat_state") or {})),
    }
    return value_hash(shape)


def audit(root: Path) -> dict[str, Any]:
    bundles = sorted(
        path.parent for path in root.rglob("manifest.json")
        if not path.parent.name.endswith(".partial")
    )
    counts: Counter[str] = Counter()
    screens: Counter[str] = Counter()
    fingerprints: dict[str, set[str]] = defaultdict(set)
    clusters: dict[str, list[dict[str, Any]]] = defaultdict(list)
    invalid: list[dict[str, str]] = []
    results: list[dict[str, Any]] = []
    for bundle in bundles:
        try:
            manifest, boundaries = load_bundle(bundle)
            for boundary in boundaries:
                screen = str(boundary["cursor"]["screen"])
                screens[screen] += 1
                fingerprints[screen].add(payload_fingerprint(boundary["raw_original_payload"]))
            matched, difference = replay(bundle)
            status = "MATCH" if matched else str((difference or {}).get("status") or "DIFFERENCE")
            counts[status] += 1
            result = {
                "bundle": bundle.name, "schema": manifest["schema"], "status": status,
                "evidence_class": manifest.get("evidence_class"),
                "capture_mode": manifest.get("capture_mode", "PAIRED"),
                "acceptance_eligible": bool(manifest.get("acceptance_eligible", False)),
                "boundaries": len(boundaries), "difference": difference,
            }
            results.append(result)
            if difference and difference.get("cluster_key"):
                clusters[str(difference["cluster_key"])].append({
                    "bundle": bundle.name, "step": difference["step"],
                    "status": status, "category": difference.get("category"),
                    "first_field_path": difference.get("first_field_path"),
                    "nearest_anchor": difference.get("nearest_anchor"),
                })
        except Exception as error:
            invalid.append({"bundle": str(bundle), "error": f"{type(error).__name__}: {error}"})
    return {
        "schema": "sls-truth-corpus-audit-v1", "root": str(root.resolve()),
        "summary": {"bundles": len(bundles), **dict(sorted(counts.items())), "invalid": len(invalid)},
        "coverage": {
            "screens": dict(sorted(screens.items())),
            "payload_schema_variants": {key: len(value) for key, value in sorted(fingerprints.items())},
        },
        "clusters": [
            {"cluster_key": key, "occurrences": values}
            for key, values in sorted(clusters.items())
        ],
        "results": results, "invalid": invalid,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path, nargs="?", default=ROOT / "validation-results" / "truth")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    report = audit(args.root)
    text = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    if report["invalid"]:
        return 2
    return 0 if not report["summary"].get("DIFFERENCE", 0) and not report["summary"].get("INCONCLUSIVE", 0) else 1


if __name__ == "__main__":
    raise SystemExit(main())
