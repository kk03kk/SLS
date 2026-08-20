"""Build the canonical native simulator on Windows or Linux."""

from __future__ import annotations

import argparse
import os
import platform
import shutil
import subprocess
import sys
import sysconfig
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / ".build"
TOOLS = CACHE / "tools"
SOURCE = ROOT / "cpp" / "simulator"
BUILD = CACHE / f"cmake-{sys.platform}-{sys.implementation.cache_tag}"
OUTPUT = CACHE / "native" / sys.implementation.cache_tag


def run(*args: object, env: dict[str, str] | None = None) -> None:
    command = [str(arg) for arg in args]
    print("+", subprocess.list2cmdline(command), flush=True)
    subprocess.run(command, cwd=ROOT, env=env, check=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jobs", type=int, default=min(os.cpu_count() or 4, 8))
    args = parser.parse_args()
    if args.jobs <= 0:
        parser.error("--jobs must be positive")
    if sys.version_info < (3, 12):
        raise RuntimeError("SLS requires Python 3.12 or newer")
    if platform.system() not in {"Windows", "Linux"}:
        raise RuntimeError("the native simulator supports Windows and Linux")
    if not (SOURCE / "SLS_VENDOR.json").is_file():
        raise RuntimeError("canonical simulator source is incomplete")

    suffix = ".exe" if platform.system() == "Windows" else ""
    cmake = TOOLS / "cmake" / "data" / "bin" / f"cmake{suffix}"
    if platform.system() == "Linux":
        cmake = TOOLS / "bin" / "cmake"
    ninja = TOOLS / "bin" / f"ninja{suffix}"
    pybind_dir = TOOLS / "pybind11" / "share" / "cmake" / "pybind11"
    zig = TOOLS / "ziglang" / "zig.exe"
    required = [cmake, ninja, pybind_dir]
    if platform.system() == "Windows":
        required.append(zig)
    if not all(path.exists() for path in required):
        run(
            sys.executable, "-m", "pip", "install",
            "--disable-pip-version-check", "--no-input", "--upgrade",
            "--target", TOOLS,
            "cmake==4.4.2", "ninja==1.13.0", "pybind11==3.1.0",
            *(["ziglang==0.16.0"] if platform.system() == "Windows" else []),
        )

    OUTPUT.mkdir(parents=True, exist_ok=True)
    build_env = dict(os.environ)
    configure: list[object] = [
        cmake, "-S", SOURCE, "-B", BUILD, "-G", "Ninja",
        f"-DCMAKE_MAKE_PROGRAM={ninja}", f"-Dpybind11_DIR={pybind_dir}",
        f"-DPython_EXECUTABLE={sys.executable}",
        f"-DSLS_NATIVE_OUTPUT_DIR={OUTPUT}", "-DCMAKE_BUILD_TYPE=Release",
    ]
    if platform.system() == "Windows":
        python_include = Path(sysconfig.get_paths()["include"])
        python_library = Path(sys.base_prefix) / "libs" / (
            f"python{sys.version_info.major}{sys.version_info.minor}.lib"
        )
        build_env["ZIG_EXECUTABLE"] = zig.as_posix()
        configure.extend([
            f"-DCMAKE_TOOLCHAIN_FILE={SOURCE / 'cmake' / 'zig-windows-toolchain.cmake'}",
            f"-DPython_INCLUDE_DIR={python_include}",
            f"-DPython_LIBRARY={python_library}",
        ])
    elif not (os.environ.get("CXX") or shutil.which("c++") or shutil.which("g++")):
        raise RuntimeError("Linux native build requires CXX, c++, or g++")

    run(*configure, env=build_env)
    run(cmake, "--build", BUILD, "--parallel", args.jobs, env=build_env)
    run(
        sys.executable, "-c",
        "from sls.backends.simulator.native import LightspeedRunState; "
        "assert LightspeedRunState; print('native simulator: OK')",
        env={**build_env, "PYTHONPATH": str(ROOT / "src")},
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
