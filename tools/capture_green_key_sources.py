"""Seal stock bytecode inputs for green-key qualification; no game execution."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import zipfile
from pathlib import Path

CLASSES = (
    "dungeons.AbstractDungeon", "rooms.MonsterRoom", "rooms.MonsterRoomElite",
    "rooms.AbstractRoom", "rewards.RewardItem", "vfx.ObtainKeyEffect",
)


def sha(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--stock-sha256", required=True)
    parser.add_argument("--javap", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    actual = sha(args.stock_jar.read_bytes())
    if actual != args.stock_sha256:
        raise ValueError("stock JAR differs from pinned input")
    args.output.mkdir(parents=True, exist_ok=False)
    report = dict(schema="sls-green-key-source-evidence-v1", stock_jar_sha256=actual,
                  javap_binary_sha256=sha(args.javap.read_bytes()),
                  scope="STATIC_BYTECODE_ONLY_NOT_RUNTIME_QUALIFICATION", classes=[])
    with zipfile.ZipFile(args.stock_jar) as archive:
        for short_name in CLASSES:
            name = "com.megacrit.cardcrawl." + short_name
            command = [str(args.javap), "-classpath", str(args.stock_jar), "-c", "-p", name]
            result = subprocess.run(command, check=True, capture_output=True)
            text = result.stdout.decode("utf-8", errors="strict").replace("\r\n", "\n")
            output = args.output / (short_name + ".txt")
            with output.open("x", encoding="utf-8", newline="\n") as stream:
                stream.write(text)
            report["classes"].append(dict(name=name,
                class_sha256=sha(archive.read(name.replace(".", "/") + ".class")),
                disassembly_sha256=sha(output.read_bytes()), command=command))
    with (args.output / "manifest.json").open("x", encoding="utf-8") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps(dict(classes=len(report["classes"]), stock_jar_sha256=actual)))


if __name__ == "__main__":
    main()
