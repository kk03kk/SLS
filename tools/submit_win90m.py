"""Submit one verified Win 70M-to-90M job; preparation runs on the GPU node."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from tools.submit_win70m import main

if __name__ == "__main__":
    raise SystemExit(main(
        plan_path=Path(__file__).resolve().parents[1] / "configs/experiments/win-90m-20261001.json",
        receipt_name="win-90m-20261001-submission.json",
    ))
