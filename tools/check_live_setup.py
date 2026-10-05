"""Check what is still needed before demonstrating a model in the Steam game."""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.runtime.live_setup import default_game_dir, inspect_live_setup  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-dir", type=Path, default=default_game_dir())
    parser.add_argument("--workshop-dir", type=Path)
    parser.add_argument("--config", type=Path, default=Path(os.environ.get("LOCALAPPDATA", "")) / "ModTheSpire" / "CommunicationMod" / "config.properties")
    parser.add_argument("--models-root", type=Path, default=ROOT / "model")
    args = parser.parse_args()
    result = inspect_live_setup(
        game_dir=args.game_dir, workshop_dir=args.workshop_dir,
        config=args.config, model_root=args.models_root,
    )
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["ready"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
