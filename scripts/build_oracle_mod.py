"""Build the content-free original-game parity instrumentation mod."""

from __future__ import annotations

import os
import hashlib
import shutil
import subprocess
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "oracle_mod" / "src" / "main" / "java"
BUILD = ROOT / ".oracle-mod-build"
CLASSES = BUILD / "classes"
OUTPUT = ROOT / "oracle_mod" / "build" / "SpirecommParity.jar"
STEAM = Path(os.environ.get("STS_STEAM_DIR", r"D:\steam"))
GAME = STEAM / "steamapps" / "common" / "SlayTheSpire" / "desktop-1.0.jar"
WORKSHOP = STEAM / "steamapps" / "workshop" / "content" / "646570"
MTS = WORKSHOP / "1605060445" / "ModTheSpire.jar"
BASEMOD = WORKSHOP / "1605833019" / "BaseMod.jar"
COMMUNICATION = WORKSHOP / "2131373661" / "CommunicationMod.jar"


def main() -> int:
    dependencies = (GAME, MTS, BASEMOD, COMMUNICATION)
    missing = [str(path) for path in dependencies if not path.is_file()]
    if missing:
        raise FileNotFoundError("Missing oracle dependencies: " + ", ".join(missing))
    shutil.rmtree(BUILD, ignore_errors=True)
    CLASSES.mkdir(parents=True)
    sources = sorted(SOURCE.rglob("*.java"))
    subprocess.run(
        [
            "javac", "--release", "8", "-encoding", "UTF-8", "-parameters",
            "-cp", os.pathsep.join(str(path) for path in dependencies),
            "-d", str(CLASSES), *(str(path) for path in sources),
        ],
        check=True,
    )
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    def add(archive: zipfile.ZipFile, path: Path, name: str) -> None:
        entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
        entry.compress_type = zipfile.ZIP_DEFLATED
        entry.external_attr = 0o100644 << 16
        archive.writestr(entry, path.read_bytes())

    with zipfile.ZipFile(OUTPUT, "w") as archive:
        add(archive, ROOT / "oracle_mod" / "ModTheSpire.json", "ModTheSpire.json")
        for path in sorted(CLASSES.rglob("*.class")):
            add(archive, path, path.relative_to(CLASSES).as_posix())
    digest = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
    print(f"{OUTPUT}\nsha256={digest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
