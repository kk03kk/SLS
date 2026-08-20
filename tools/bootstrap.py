"""Install the source checkout and build the native simulator."""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def run(*args: object) -> None:
    command = [str(value) for value in args]
    print("+", subprocess.list2cmdline(command), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--with-model", action="store_true")
    parser.add_argument("--skip-native", action="store_true")
    args = parser.parse_args()
    run(
        sys.executable, "-m", "pip", "install",
        "-r", ROOT / "requirements" / "test.lock",
    )
    if args.with_model:
        run(
            sys.executable, "-m", "pip", "install",
            "-r", ROOT / "requirements" / "model.lock",
        )
    run(sys.executable, "-m", "pip", "install", "-e", ".", "--no-deps")
    if not args.skip_native:
        run(sys.executable, ROOT / "tools" / "build_native.py")
    run(sys.executable, "-m", "pytest")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
