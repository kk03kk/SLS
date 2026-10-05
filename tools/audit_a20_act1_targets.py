"""Write a hash-bound, conservative A20 Act1 audit target inventory."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.audit.act1_encounters import compare_stock_act1_encounters  # noqa: E402
from sls.audit.act1_map import (  # noqa: E402
    check_act1_map_structure,
    compare_stock_act1_map_constants,
)
from sls.audit.act1_targets import (  # noqa: E402
    build_a20_act1_targets,
    compare_stock_event_pools,
    compare_stock_ironclad_potion_pool,
)
from sls.backends.simulator import SimulatorBackend  # noqa: E402
from sls.curriculum import IRONCLAD_A20_ACT1  # noqa: E402
from sls.rl.training_contract import native_artifact, native_source_digest  # noqa: E402


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument(
        "--stock-source-root", type=Path,
        default=ROOT / "local/audits/stock-decompilation-tree/desktop-1.0/source/com/megacrit/cardcrawl",
        help="reviewed stock Java projection rooted at com/megacrit/cardcrawl",
    )
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("current native source has no matching built artifact")
    stock_sha256 = _sha256(args.stock_jar)
    decompilation = json.loads(
        (ROOT / "docs/audits/stock-decompilation/manifest.json").read_text(
            encoding="utf-8",
        ),
    )
    if stock_sha256 != decompilation["authority"]["sha256"]:
        raise ValueError("stock JAR differs from the reviewed decompilation source")
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    backend.reset(0)
    payload = build_a20_act1_targets(backend.raw_state["ordered_pools"])
    stock_source_root = args.stock_source_root
    payload["stock_event_pool_projection"] = compare_stock_event_pools(
        stock_source_root,
        backend.raw_state["ordered_pools"],
    )
    payload["stock_encounter_pool_projection"] = compare_stock_act1_encounters(
        stock_source_root / "dungeons/Exordium.java",
        ROOT / "native/simulator/include/constants/MonsterEncounters.h",
    )
    payload["stock_ironclad_potion_pool_projection"] = compare_stock_ironclad_potion_pool(
        stock_source_root,
        ROOT / "native/simulator/include/constants/Potions.h",
    )
    payload["stock_act1_map_chance_projection"] = compare_stock_act1_map_constants(
        stock_source_root / "dungeons/Exordium.java",
        ROOT / "native/simulator/src/game/Map.cpp",
    )
    for seed in range(32):
        backend.reset(seed)
        check_act1_map_structure(backend.raw_state["public_map"])
    payload["native_map_structure_probe"] = {
        "seed_start": 0,
        "seed_count": 32,
        "status": "PASSED_NATIVE_INVARIANTS_NOT_STOCK_PARITY",
    }
    payload["authority"] = {
        "stock_jar_sha256": stock_sha256,
        "stock_abstract_dungeon_projection_sha256": _sha256(
            stock_source_root / "dungeons/AbstractDungeon.java",
        ),
        "stock_exordium_projection_sha256": _sha256(
            stock_source_root / "dungeons/Exordium.java",
        ),
        "stock_potion_helper_projection_sha256": _sha256(
            stock_source_root / "helpers/PotionHelper.java",
        ),
        "native_source_sha256": native_source_digest(),
        "native_artifact_sha256": artifact["sha256"],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(args.output)
    print(json.dumps(payload["summary"], sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
