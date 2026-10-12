"""Build the shipped Inspector UI with dependencies confined to local/build."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--node", default=shutil.which("node"))
    parser.add_argument("--pnpm-script", type=Path, help="optional pnpm.cjs when pnpm is not on PATH")
    args = parser.parse_args()
    if not args.node:
        parser.error("Node.js is required for UI development, not for running the shipped Inspector")
    runner = [args.node, str(args.pnpm_script)] if args.pnpm_script else [shutil.which("pnpm") or "pnpm"]
    source = ROOT / "src/sls/runtime/web"
    work = ROOT / "local/build/inspector-web"
    work.mkdir(parents=True, exist_ok=True)
    for path in source.iterdir():
        if path.is_file() and not path.name.startswith("."):
            shutil.copy2(path, work / path.name)
    subprocess.run([*runner, "install", "--frozen-lockfile", "--store-dir", str(ROOT / "local/build/pnpm-store")], cwd=work, check=True, env={**os.environ, "CI": "true"})
    subprocess.run([args.node, "node_modules/typescript/bin/tsc", "--noEmit"], cwd=work, check=True)
    subprocess.run([args.node, "node_modules/vite/bin/vite.js", "build"], cwd=work, check=True)
    shutil.copy2(source / "THIRD_PARTY_LICENSES.txt", work / "dist/THIRD_PARTY_LICENSES.txt")
    shipped = source / "dist"
    shipped.mkdir(exist_ok=True)
    built = {p.relative_to(work / "dist") for p in (work / "dist").rglob("*") if p.is_file()}
    for path in shipped.rglob("*"):
        if path.is_file() and path.relative_to(shipped) not in built:
            if not path.resolve().is_relative_to(shipped.resolve()):
                raise RuntimeError("UI output resolves outside the shipped directory")
            path.unlink()  # Retire only stale generated bundle files.
    shutil.copytree(work / "dist", shipped, dirs_exist_ok=True)
    receipt = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
               for p in shipped.rglob("*") if p.is_file()}
    (work / "build-receipt.json").write_text(json.dumps(receipt, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(shipped), "files": len(receipt)}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
