"""Build the complete Oracle from committed sources, without an old Oracle JAR."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ORACLE = ROOT / "native/oracle"
DEPENDENCY_NAMES = ("game", "mod_the_spire", "base_mod", "communication_mod")
SCHEMA = "sls-oracle-build-v1"


def sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def source_files(source: Path = ORACLE) -> list[Path]:
    return sorted((source / "src").rglob("*.java"))


def resource_payloads(source: Path = ORACLE) -> dict[str, bytes]:
    payloads = {"ModTheSpire.json": (source / "ModTheSpire.json").read_bytes()}
    for path in sorted((source / "resources").rglob("*")):
        if path.is_file():
            payloads[path.relative_to(source / "resources").as_posix()] = path.read_bytes()
    required = {f"spirecomm/parity/scenario-{name}-allowlist.tsv"
                for name in ("card", "potion", "relic", "encounter", "event")}
    if not required.issubset(payloads):
        raise ValueError("Oracle source resources are incomplete")
    for name in required:
        seen = set()
        for line in payloads[name].decode("utf-8").splitlines():
            identifier, constructor = line.split("\t")
            if not identifier or not constructor or identifier in seen:
                raise ValueError(f"invalid or duplicate allowlist row: {name}")
            seen.add(identifier)
    return payloads


def deterministic_jar(destination: Path, members: dict[str, bytes]) -> None:
    with zipfile.ZipFile(destination, "w") as archive:
        for name, payload in sorted(members.items()):
            if name.startswith("/") or ".." in Path(name).parts:
                raise ValueError("invalid JAR member")
            entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            entry.create_system = 3
            entry.external_attr = 0o100644 << 16
            entry.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(entry, payload)


def build(*, javac: Path, dependencies: dict[str, Path], output: Path,
          source: Path = ORACLE, force: bool = False) -> dict:
    paths = [javac, *dependencies.values()]
    missing = [str(path) for path in paths if not path.is_file()]
    if missing:
        raise FileNotFoundError("missing build dependencies: " + ", ".join(missing))
    if set(dependencies) != set(DEPENDENCY_NAMES):
        raise ValueError("must supply the four Oracle dependencies")
    if output.resolve().is_relative_to(source.resolve()):
        raise ValueError("output must be outside the Oracle source directory")
    if any(output.resolve() == path.resolve() for path in paths):
        raise ValueError("output must not overwrite a dependency")
    sidecar = output.with_suffix(".build.json")
    if not force and (output.exists() or sidecar.exists()):
        raise FileExistsError("output/evidence exists; use a new path or explicit --force")
    sources = source_files(source)
    if not sources:
        raise ValueError("no Oracle Java sources")
    resources = resource_payloads(source)
    inputs = {path.relative_to(source).as_posix(): sha256(path)
              for path in sources + [source / "ModTheSpire.json"]}
    inputs.update({"resources/" + name: hashlib.sha256(payload).hexdigest()
                   for name, payload in resources.items() if name != "ModTheSpire.json"})
    dependency_hashes = {name: sha256(path) for name, path in dependencies.items()}
    compiler = subprocess.run([str(javac), "-version"], check=True, capture_output=True, text=True)
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=output.parent, prefix="oracle-build-") as temporary:
        directory = Path(temporary)
        classes = directory / "classes"
        classes.mkdir()
        subprocess.run([
            str(javac), "--release", "8", "-encoding", "UTF-8", "-parameters", "-proc:none",
            "-classpath", os.pathsep.join(str(path) for path in dependencies.values()),
            "-d", str(classes), *[str(path) for path in sources],
        ], check=True)
        members = dict(resources)
        for path in sorted(classes.rglob("*.class")):
            payload = path.read_bytes()
            if payload[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(payload[6:8], "big") != 52:
                raise ValueError("Oracle must target Java 8 class version 52")
            members[path.relative_to(classes).as_posix()] = payload
        if not any(name.endswith(".class") for name in members):
            raise ValueError("compiler produced no classes")
        staged = directory / "oracle.jar"
        deterministic_jar(staged, members)
        report = {"schema": SCHEMA, "compiler": (compiler.stdout + compiler.stderr).strip(),
                  "compiler_sha256": sha256(javac), "java_release": 8,
                  "sources": inputs, "dependencies": dependency_hashes,
                  "members": {name: hashlib.sha256(payload).hexdigest()
                              for name, payload in sorted(members.items())},
                  "output_sha256": sha256(staged), "used_existing_oracle": False}
        evidence = directory / "build.json"
        evidence.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        # Compilation completes before touching either final output.
        os.replace(staged, output)
        os.replace(evidence, sidecar)
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--javac", type=Path, default=shutil.which("javac"))
    parser.add_argument("--game-jar", type=Path, required=True)
    parser.add_argument("--mod-the-spire", type=Path, required=True)
    parser.add_argument("--base-mod", type=Path, required=True)
    parser.add_argument("--communication-mod", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=ROOT / "local/build/oracle/SpirecommParity.jar")
    parser.add_argument("--force", action="store_true", help="explicitly replace existing output and build evidence")
    args = parser.parse_args()
    if args.javac is None:
        parser.error("javac not found; supply --javac")
    report = build(javac=args.javac, output=args.output, force=args.force, dependencies={
        "game": args.game_jar, "mod_the_spire": args.mod_the_spire,
        "base_mod": args.base_mod, "communication_mod": args.communication_mod,
    })
    print(json.dumps({"output": str(args.output), "sha256": report["output_sha256"],
                      "classes": sum(name.endswith(".class") for name in report["members"]),
                      "used_existing_oracle": False}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
