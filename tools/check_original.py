"""Check local Original-game dependencies and Oracle build prerequisites."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import shutil
import subprocess


ROOT = Path(__file__).resolve().parents[1]
EXTERNAL = ROOT / "external" / "original-game"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    manifest_path = EXTERNAL / "manifest.json"
    if not manifest_path.is_file():
        raise FileNotFoundError("run tools/import_original_game.py first")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    for name, expected in manifest["files"].items():
        path = EXTERNAL / name
        if not path.is_file():
            raise FileNotFoundError(path)
        actual = sha256(path)
        if actual != expected["sha256"]:
            raise RuntimeError(f"hash mismatch for {name}: {actual}")
    javac = shutil.which("javac")
    if javac is None:
        raise RuntimeError("Java 8 javac is not on PATH")
    version = subprocess.run(
        [javac, "-version"], capture_output=True, text=True, check=True,
    )
    print((version.stderr or version.stdout).strip())
    print(f"Original dependencies: OK ({len(manifest['files'])} files)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
