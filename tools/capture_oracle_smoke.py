"""Protocol subprocess for bounded Oracle runtime qualification; no model required."""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT))

from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.curriculum import IRONCLAD_A20_ACT1
from tools.capture_original_card_batch import _enter_combat


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("production", "validation"), required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = {"schema": "sls-oracle-runtime-smoke-v1", "mode": args.mode,
              "bounded_check_only": True, "normal_start_win_rate_measured": False}
    backend = None
    status = 2
    try:
        session = OriginalSession()
        backend = OriginalBackend(session=session, profile=IRONCLAD_A20_ACT1)
        backend.reset(0)
        result["initial"] = session.payload
        _enter_combat(backend)
        payload = session.payload
        assert payload is not None
        assert payload.get("_parity_schema") == "spirecomm-parity-v11"
        assert payload.get("_oracle_contract") == "sls-oracle-mode-v1"
        assert payload.get("_oracle_mode") == args.mode
        result["combat"] = payload
        commands = set(payload.get("available_commands", []))
        diagnostic_keys = {"_rng", "_continuation", "_timing_evidence", "math_seed"}
        if args.mode == "production":
            assert not diagnostic_keys.intersection(payload)
            assert "parity_card" not in commands and "parity_scenario" not in commands
        else:
            assert diagnostic_keys.issubset(payload)
            assert "parity_card" in commands
            result["card_probe"] = session.execute("parity_card STRIKE_RED 0")
            assert result["card_probe"].get("_parity_scenario")
        backend.return_to_menu()
        assert not (session.payload or {}).get("in_game")
        result["returned_to_menu"] = True
        result["status"] = "PASS"
        status = 0
    except BaseException as error:
        result["status"] = "FAIL"
        result["error"] = f"{type(error).__name__}: {error}"
        raise
    finally:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        marker = os.environ.get("SLS_RUN_COMPLETION")
        if marker:
            Path(marker).write_text(json.dumps({"exit_code": status, "status": result.get("status")}), encoding="utf-8")
    return status


if __name__ == "__main__":
    raise SystemExit(main())
