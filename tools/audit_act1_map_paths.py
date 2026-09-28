"""Compare native Act1 map paths with stock JAR bytecode without opening the game."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend  # noqa: E402
from sls.curriculum import IRONCLAD_A20_ACT1  # noqa: E402
from sls.rl.training_contract import native_artifact, native_source_digest  # noqa: E402


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _stock_map_lines(output: str) -> list[str]:
    lines = []
    room_names = {
        "MonsterRoom": "MONSTER", "MonsterRoomElite": "ELITE",
        "EventRoom": "EVENT", "RestRoom": "REST",
        "ShopRoom": "SHOP", "TreasureRoom": "TREASURE",
    }
    for line in output.splitlines():
        if not re.fullmatch(r"\d+:\d+\|[A-Za-z]+>\d+:\d+(?:,\d+:\d+)*", line):
            continue
        origin, targets = line.split(">", 1)
        coordinates, stock_room = origin.split("|", 1)
        x, y = map(int, coordinates.split(":"))
        if not (0 <= x < 7 and 0 <= y < 15):
            raise ValueError(f"stock map node outside Act1: {line}")
        if stock_room not in room_names:
            raise ValueError(f"unrecognized stock map room: {line}")
        if y == 14:
            if targets != "3:16":
                raise ValueError(f"unexpected stock boss edge: {line}")
            targets = "3:15"  # public protocol names the virtual boss row 15
        lines.append(f"{x}:{y}|{room_names[stock_room]}>{targets}")
    if not lines:
        raise ValueError("stock map probe produced no paths")
    return lines


def _native_map_lines(backend: SimulatorBackend) -> list[str]:
    return [
        f"{node['x']}:{node['y']}|{node['room_type']}>"
        + ",".join(edge.removeprefix("map:") for edge in node["outgoing_node_ids"])
        for node in backend.raw_state["public_map"]
    ]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--seed-count", type=int, default=32)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not 1 <= args.seed_count <= 256:
        raise ValueError("seed count must be between 1 and 256")
    manifest = json.loads((ROOT / "docs/audits/stock-decompilation/manifest.json").read_text(encoding="utf-8"))
    stock_hash = _sha256(args.stock_jar)
    if stock_hash != manifest["authority"]["sha256"]:
        raise ValueError("stock JAR hash differs from pinned audit source")
    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("current native source has no matching built artifact")

    build_dir = ROOT / "local/build/java/stock-act1-map"
    build_dir.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["javac", "-proc:none", "-cp", str(args.stock_jar), "-d", str(build_dir),
         str(ROOT / "tools/java/StockAct1MapProbe.java")],
        check=True, capture_output=True, text=True,
    )
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    for seed in range(args.seed_count):
        stock = subprocess.run(
            ["java", "-cp", f"{build_dir};{args.stock_jar}", "StockAct1MapProbe", str(seed), "rooms"],
            check=True, capture_output=True, text=True,
        )
        backend.reset(seed)
        stock_lines = _stock_map_lines(stock.stdout)
        native_lines = _native_map_lines(backend)
        if stock_lines != native_lines:
            first = next(
                (index for index, pair in enumerate(zip(stock_lines, native_lines))
                 if pair[0] != pair[1]),
                min(len(stock_lines), len(native_lines)),
            )
            raise ValueError(
                f"seed {seed} map room/path differs at node {first}: "
                f"stock={stock_lines[first:first + 1]}, native={native_lines[first:first + 1]}",
            )
    payload = {
        "schema": "sls-stock-act1-map-parity-v1",
        "status": "MATCHED_MAP_ROOMS_AND_PATHS",
        "seed_start": 0,
        "seed_count": args.seed_count,
        "stock_jar_sha256": stock_hash,
        "probe_source_sha256": _sha256(ROOT / "tools/java/StockAct1MapProbe.java"),
        "native_source_sha256": native_source_digest(),
        "native_artifact_sha256": artifact["sha256"],
        "boss_edge_normalization": "stock row 14 -> 16 maps to protocol row 14 -> 15",
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps({"status": payload["status"], "seed_count": args.seed_count}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
