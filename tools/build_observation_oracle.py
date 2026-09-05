"""Compile public-observation patches into a new Oracle JAR without launching the game."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--javac", type=Path, required=True)
    parser.add_argument("--source", type=Path,
                        default=ROOT / "local/build/oracle/SpirecommParity.jar")
    parser.add_argument("--game-libs", type=Path,
                        default=ROOT / "local/external/original-game")
    parser.add_argument("--output", type=Path,
                        default=ROOT / "local/build/oracle/SpirecommParity-observation-v2.jar")
    args = parser.parse_args()
    if args.source.resolve() == args.output.resolve():
        parser.error("--output must be a new path, separate from the source Oracle")
    java_sources = [ROOT / "native/oracle/src/spirecomm/parity" / name for name in (
        "CardStatePatch.java", "EventStatePatch.java",
    )]
    dependencies = [args.game_libs / name for name in (
        "desktop-1.0.jar", "CommunicationMod.jar", "ModTheSpire.jar",
    )]
    for path in [args.javac, args.source, *java_sources, *dependencies]:
        if not path.is_file():
            raise FileNotFoundError(path)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=args.output.parent, prefix="observation-oracle-") as temp:
        directory = Path(temp)
        subprocess.run([
            str(args.javac), "--release", "8", "-encoding", "UTF-8",
            "-classpath", os.pathsep.join(str(path) for path in dependencies),
            "-d", str(directory / "classes"), *[str(path) for path in java_sources],
        ], check=True)
        classes = {path.relative_to(directory / "classes").as_posix(): path.read_bytes()
                   for path in (directory / "classes").rglob("*.class")}
        staged = directory / "patched.jar"
        with zipfile.ZipFile(args.source) as source, zipfile.ZipFile(
            staged, "w", compression=zipfile.ZIP_DEFLATED,
        ) as output:
            for item in source.infolist():
                if item.filename not in classes:
                    output.writestr(item, source.read(item))
            for name, data in classes.items():
                output.writestr(name, data)
        os.replace(staged, args.output)
    print(json.dumps({
        "output": str(args.output), "classes": sorted(classes),
        "source_java_sha256": {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                               for path in java_sources},
        "output_sha256": hashlib.sha256(args.output.read_bytes()).hexdigest(),
        "game_launched": False,
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
