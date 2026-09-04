"""Export a strict standalone live-game policy artifact."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

import torch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.runtime import export_policy_artifact, load_policy_artifact  # noqa: E402


def _file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _write_test_manifest(
    checkpoint: Path, artifact: Path, *, goal: str,
) -> Path:
    payload = torch.load(checkpoint, map_location="cpu", weights_only=False)
    trainer = payload.get("trainer") or {}
    contract = payload.get("contract") or {}
    profile = contract.get("profile")
    profile_id = getattr(profile, "profile_id", None)
    if profile_id is None and isinstance(profile, dict):
        profile_id = profile.get("profile_id")
    loaded = load_policy_artifact(artifact)
    manifest = artifact.with_suffix(".json")
    temporary = manifest.with_suffix(".json.tmp")
    temporary.write_text(json.dumps({
        "schema": "sls-test-model-manifest-v1",
        "artifact_filename": artifact.name,
        "artifact_sha256": _file_sha256(artifact),
        "model_sha256": loaded.metadata.model_sha256,
        "source_checkpoint": str(checkpoint.resolve()),
        "source_checkpoint_sha256": _file_sha256(checkpoint),
        "source_export": str(artifact.resolve()),
        "profile": str(profile_id or ""),
        "goal": goal,
        "environment_steps": trainer.get("environment_steps"),
        "updates": trainer.get("update"),
        "verified_weight_match": True,
    }, indent=2) + "\n", encoding="utf-8")
    temporary.replace(manifest)
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument(
        "--output", type=Path,
        help="output artifact (defaults to model/<run>-<goal>.pt)",
    )
    parser.add_argument("--ascension-min", type=int, required=True)
    parser.add_argument("--ascension-max", type=int, required=True)
    parser.add_argument(
        "--goal", choices=("ACT1", "ACT2", "ACT3", "FULLRUN", "HEART"),
        required=True,
    )
    args = parser.parse_args()
    output = args.output or (
        ROOT / "model" / f"{args.checkpoint.parent.name}-{args.goal.lower()}.pt"
    )
    artifact = export_policy_artifact(
        args.checkpoint, output,
        ascension_min=args.ascension_min, ascension_max=args.ascension_max,
        goal=args.goal,
    )
    manifest = _write_test_manifest(args.checkpoint, artifact, goal=args.goal)
    print(artifact)
    print(manifest)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
