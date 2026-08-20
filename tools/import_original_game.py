"""Import locally owned Original-game dependencies into the ignored workspace."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "external" / "original-game"


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--game-jar", type=Path, required=True)
    parser.add_argument("--modthespire", type=Path, required=True)
    parser.add_argument("--basemod", type=Path, required=True)
    parser.add_argument("--communicationmod", type=Path, required=True)
    args = parser.parse_args()
    sources = {
        "desktop-1.0.jar": args.game_jar,
        "ModTheSpire.jar": args.modthespire,
        "BaseMod.jar": args.basemod,
        "CommunicationMod.jar": args.communicationmod,
    }
    missing = [str(path) for path in sources.values() if not path.is_file()]
    if missing:
        parser.error("missing dependency files: " + ", ".join(missing))
    OUTPUT.mkdir(parents=True, exist_ok=True)
    manifest: dict[str, object] = {"schema": "sls-original-dependencies-v1", "files": {}}
    files = manifest["files"]
    assert isinstance(files, dict)
    for name, source in sources.items():
        destination = OUTPUT / name
        shutil.copy2(source.resolve(), destination)
        files[name] = {"sha256": digest(destination), "size": destination.stat().st_size}
    (OUTPUT / "manifest.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    print(OUTPUT / "manifest.json")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
