"""Verify frozen key-room inputs before launching a recoverable stock probe."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import zipfile
from pathlib import Path

from tools.run_original_canary import original_runtime_paths
from tools.verify_oracle import inspect, runtime_smoke


def scene_floor(scene: dict, payload: dict) -> int:
    if scene.get("floor_policy") == "ACT2_MAP_Y_PLUS_18":
        return int(payload["_parity_run"]["current_map_y"]) + 18
    return int(scene["floor"])


def root_path(payload: dict) -> list[list[int]]:
    nodes = {(node["x"], node["y"]): node for node in payload["game_state"]["map"]}
    paths = {point: [list(point)] for point, node in nodes.items() if point[1] == 0 and node["children"]}
    for point in sorted(nodes, key=lambda value: (value[1], value[0])):
        if point not in paths:
            continue
        for child in nodes[point]["children"]:
            target = (child["x"], child["y"])
            # Stock exports the virtual boss endpoint (3,16) after row14.
            # It is outside the ordinary-room graph used by this witness.
            if point[1] == 14 and target == (3, 16):
                continue
            if target[1] != point[1] + 1:
                raise ValueError("non-layered stock map edge")
            if target in nodes and target not in paths:
                paths[target] = paths[point] + [list(target)]
    target = (payload["_parity_run"]["current_map_x"], payload["_parity_run"]["current_map_y"])
    if target not in paths or not 0 <= target[1] <= 14:
        raise ValueError("controlled stock room is not root reachable")
    return paths[target]


def validate_manifest(manifest: dict, selected: list[str]) -> list[dict]:
    schema = manifest.get("schema")
    if schema not in {"sls-key-room-scenes-v1", "sls-key-room-scenes-v2"}:
        raise ValueError("unsupported key-room schema")
    v2 = schema == "sls-key-room-scenes-v2"
    if v2 and (manifest.get("map_node_policy") != "FIRST_ROOT_REACHABLE_ROOM"
               or manifest.get("room_rng_policy") != "STOCK_SEED_PLUS_DERIVED_FLOOR_FIVE_STREAMS"):
        raise ValueError("undeclared node selection policy")
    scenes = manifest["scenes"]
    identifiers = [scene["id"] for scene in scenes]
    if len(set(identifiers)) != len(identifiers) or not selected or len(set(selected)) != len(selected):
        raise ValueError("duplicate or empty scene selection")
    if set(selected) - set(identifiers):
        raise ValueError("unknown scene")
    seeds = []
    for scene in scenes:
        initial = scene["initial"]
        if v2 and (scene.get("floor_policy") != "ACT2_MAP_Y_PLUS_18" or "floor" in scene):
            raise ValueError("missing or ambiguous floor derivation")
        if (scene["act"] != 2 or scene["ascension"] != 20
                or scene["room"] not in {"REST", "TREASURE"}
                or not 0 < initial["hp"] <= initial["max_hp"] <= 1000
                or not initial["deck"] or not initial["relics"] or initial["gold"] < 0
                or not scene["seeds"] or not scene["actions"]):
            raise ValueError("invalid controlled initial state")
        if any(type(initial[key]) is not bool for key in
               ("final_act_available", "ruby_key", "sapphire_key")):
            raise ValueError("key flags must be booleans")
        allowed = {"REST", "RECALL"} if scene["room"] == "REST" else {
            "OPEN_CHEST", "TAKE_BLUE_KEY", "TAKE_RELIC"}
        if set(scene["actions"]) - allowed:
            raise ValueError("unsupported stock action script")
        seeds.extend(scene["seeds"])
    lower, upper = (131200330, 131200360) if v2 else (131200300, 131200330)
    if any(type(seed) is not int or not lower <= seed < upper for seed in seeds):
        raise ValueError("undeclared diagnostic namespace")
    if len(set(seeds)) != len(seeds):
        raise ValueError("seed collision")
    return [scene for scene in scenes if scene["id"] in selected]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--oracle", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--scenes", nargs="+", required=True)
    parser.add_argument("--timeout", type=float, default=600)
    args = parser.parse_args()
    if not 0 < args.timeout <= 1800:
        raise ValueError("timeout must be <=30 minutes")
    if args.output.exists() or args.output.with_suffix(".launch.json").exists():
        raise FileExistsError("evidence output exists")
    report = inspect(args.oracle)
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    validate_manifest(manifest, args.scenes)
    filename = "fullrun-key-acquisition-r2.json" if manifest["schema"] == "sls-key-room-scenes-v2" else "fullrun-key-acquisition-r1.json"
    name = "spirecomm/parity/" + filename
    if hashlib.sha256(args.manifest.read_bytes()).hexdigest() != report["members"].get(name):
        raise ValueError("manifest differs from source-built Oracle")
    _, game = original_runtime_paths(None)
    stock = game / "desktop-1.0.jar"
    actual = hashlib.sha256(stock.read_bytes()).hexdigest()
    if actual != manifest["stock_jar_sha256"] or actual != report["dependencies"]["game"]:
        raise ValueError("stock identity mismatch")
    required = {"rooms.RestRoom", "rooms.CampfireUI", "ui.campfire.RecallOption",
                "vfx.campfire.CampfireRecallEffect", "vfx.ObtainKeyEffect", "dungeons.TheCity",
                "rooms.TreasureRoom", "rooms.AbstractRoom", "rewards.RewardItem",
                "rewards.chests.AbstractChest"}
    if manifest["schema"] == "sls-key-room-scenes-v2":
        required |= {"map.MapRoomNode", "dungeons.AbstractDungeon"}
    evidence = manifest["source_evidence"]
    if set(evidence) != {"com.megacrit.cardcrawl." + key for key in required}:
        raise ValueError("missing stock source evidence")
    with zipfile.ZipFile(stock) as archive:
        for name, row in evidence.items():
            digest = hashlib.sha256(archive.read(name.replace(".", "/") + ".class")).hexdigest()
            if digest != row["class_sha256"]:
                raise ValueError("stock class mismatch")
    os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    command = [Path(sys.executable).as_posix(), Path(__file__).with_name("capture_key_room_batch.py").resolve().as_posix(),
               "--manifest", args.manifest.resolve().as_posix(), "--output", args.output.resolve().as_posix(),
               "--scenes", *args.scenes]
    result = runtime_smoke(args.oracle.resolve(), args.output.resolve(), "validation", None,
                           args.timeout, capture_command=command)
    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
