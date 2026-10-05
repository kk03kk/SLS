from __future__ import annotations

import argparse
import hashlib
import json
import os
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

_MEMBERS = {
    "cards": "spirecomm/parity/scenario-card-allowlist.tsv",
    "potions": "spirecomm/parity/scenario-potion-allowlist.tsv",
    "relics": "spirecomm/parity/scenario-relic-allowlist.tsv",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Expand parity-oracle resources to every registered object.",
    )
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--registry", type=Path, default=(
        ROOT / "src" / "sls" / "content" / "registry.json"
    ))
    parser.add_argument("--output", type=Path, required=True)
    return parser.parse_args()


def build(source_path: Path, registry_path: Path, output_path: Path) -> dict:
    if source_path.resolve() == output_path.resolve():
        raise ValueError("--output must be separate from the source Oracle")
    categories = json.loads(
        registry_path.read_text(encoding="utf-8"),
    )["categories"]
    replacements = {
        member: "".join(
            f"{row['id']}\t{row['game_id']}\n" for row in categories[category]
        ).encode("utf-8")
        for category, member in _MEMBERS.items()
    }
    source_sha256 = hashlib.sha256(source_path.read_bytes()).hexdigest()
    # Validate before creating any output, including when reusing an output path.
    with zipfile.ZipFile(source_path) as source:
        missing = sorted(set(replacements) - set(source.namelist()))
        if missing:
            raise ValueError("source Oracle missing allowlist resources: " + ", ".join(missing))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=output_path.parent) as temporary:
        staged = Path(temporary) / output_path.name
        with zipfile.ZipFile(source_path) as source, zipfile.ZipFile(
            staged, "w", compression=zipfile.ZIP_DEFLATED,
        ) as output:
            for info in source.infolist():
                output.writestr(info, replacements.get(info.filename, source.read(info)))
        os.replace(staged, output_path)
    return {
        "source": str(source_path.resolve()),
        "output": str(output_path.resolve()),
        "source_sha256": source_sha256,
        "output_sha256": hashlib.sha256(output_path.read_bytes()).hexdigest(),
        "counts": {category: len(categories[category]) for category in _MEMBERS},
    }


def main() -> int:
    args = parse_args()
    print(json.dumps(build(args.source, args.registry, args.output), indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
