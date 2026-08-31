from __future__ import annotations

import argparse
import json
import shutil
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


def main() -> int:
    args = parse_args()
    categories = json.loads(
        args.registry.read_text(encoding="utf-8"),
    )["categories"]
    replacements = {
        member: "".join(
            f"{row['id']}\t{row['game_id']}\n" for row in categories[category]
        ).encode("utf-8")
        for category, member in _MEMBERS.items()
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=args.output.parent) as temporary:
        staged = Path(temporary) / args.output.name
        with zipfile.ZipFile(args.source) as source, zipfile.ZipFile(
            staged, "w", compression=zipfile.ZIP_DEFLATED,
        ) as output:
            for info in source.infolist():
                output.writestr(info, replacements.get(info.filename, source.read(info)))
        shutil.move(staged, args.output)
    print(json.dumps({
        "source": str(args.source.resolve()),
        "output": str(args.output.resolve()),
        "counts": {category: len(categories[category]) for category in _MEMBERS},
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
