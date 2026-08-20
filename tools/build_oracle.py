"""Build the content-free Original-game validation mod."""

from __future__ import annotations

import hashlib
import os
import shutil
import subprocess
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ORACLE = ROOT / "java" / "oracle-mod"
SOURCE = ORACLE / "src" / "main" / "java"
EXTERNAL = ROOT / "external" / "original-game"
BUILD = ROOT / ".build" / "oracle"
CLASSES = BUILD / "classes"
OUTPUT = BUILD / "SpirecommParity.jar"
DEPENDENCIES = tuple(
    EXTERNAL / name
    for name in (
        "desktop-1.0.jar",
        "ModTheSpire.jar",
        "BaseMod.jar",
        "CommunicationMod.jar",
    )
)


def main() -> int:
    missing = [str(path) for path in DEPENDENCIES if not path.is_file()]
    if missing:
        raise FileNotFoundError(
            "Original-game dependencies have not been imported: " + ", ".join(missing)
        )
    if BUILD.exists():
        shutil.rmtree(BUILD)
    CLASSES.mkdir(parents=True)
    sources = sorted(SOURCE.rglob("*.java"))
    subprocess.run(
        [
            "javac", "--release", "8", "-encoding", "UTF-8", "-parameters",
            "-cp", os.pathsep.join(str(path) for path in DEPENDENCIES),
            "-d", str(CLASSES), *(str(path) for path in sources),
        ],
        check=True,
    )

    def add(archive: zipfile.ZipFile, path: Path, name: str) -> None:
        entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
        entry.compress_type = zipfile.ZIP_DEFLATED
        entry.external_attr = 0o100644 << 16
        archive.writestr(entry, path.read_bytes())

    with zipfile.ZipFile(OUTPUT, "w") as archive:
        add(archive, ORACLE / "ModTheSpire.json", "ModTheSpire.json")
        for path in sorted(CLASSES.rglob("*.class")):
            add(archive, path, path.relative_to(CLASSES).as_posix())
    print(f"{OUTPUT}\nsha256={hashlib.sha256(OUTPUT.read_bytes()).hexdigest()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
