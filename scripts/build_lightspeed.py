"""Reproducibly build the native sts_lightspeed bridge on Windows.

The compiler and build tools are installed into an ignored repository-local
cache. No Visual Studio C++ workload is required.
"""

from __future__ import annotations

import os
import subprocess
import sys
import sysconfig
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
CACHE = ROOT / ".native-build"
# Version the tool directory so a Windows process holding an older compiler
# executable cannot prevent installing or repairing the pinned toolchain.
TOOLS = CACHE / "tools-pinned-v1"
SOURCE = CACHE / "sts_lightspeed"
BUILD = CACHE / "build-pinned-v1"
COMMIT = "7476a81954020087da31d41d16fddf475746ec2d"
PATCHES = ROOT / "simulator" / "native" / "patches"


def run(*args: object, cwd: Path | None = None, env: dict[str, str] | None = None) -> None:
    command = [str(arg) for arg in args]
    print("+", subprocess.list2cmdline(command), flush=True)
    subprocess.run(command, cwd=cwd, env=env, check=True)


def main() -> int:
    CACHE.mkdir(parents=True, exist_ok=True)
    required_tools = (
        TOOLS / "cmake" / "data" / "bin" / "cmake.exe",
        TOOLS / "bin" / "ninja.exe",
        TOOLS / "ziglang" / "zig.exe",
        TOOLS / "pybind11" / "share" / "cmake" / "pybind11",
    )
    if not all(path.exists() for path in required_tools):
        run(
            sys.executable,
            "-m",
            "pip",
            "install",
            "--disable-pip-version-check",
            "--no-input",
            "--upgrade",
            "--target",
            TOOLS,
            "cmake==4.4.2",
            "ninja==1.13.0",
            "ziglang==0.16.0",
            "pybind11==3.1.0",
        )
    else:
        print("+ native build tools already installed (pinned versions)", flush=True)

    if not SOURCE.exists():
        run(
            "git",
            "clone",
            "--recurse-submodules",
            "https://github.com/gamerpuppy/sts_lightspeed.git",
            SOURCE,
        )
    commit_is_local = subprocess.run(
        ["git", "cat-file", "-e", f"{COMMIT}^{{commit}}"],
        cwd=SOURCE,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    ).returncode == 0
    if not commit_is_local:
        run("git", "fetch", "origin", COMMIT, cwd=SOURCE)
    else:
        print(f"+ pinned sts_lightspeed commit already available: {COMMIT}", flush=True)
    run("git", "checkout", "--detach", COMMIT, cwd=SOURCE)
    run("git", "submodule", "update", "--init", "--recursive", cwd=SOURCE)
    for patch in sorted(PATCHES.glob("*.patch")):
        reverse_check = subprocess.run(
            ["git", "apply", "--reverse", "--check", str(patch)],
            cwd=SOURCE,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        if reverse_check.returncode != 0:
            run("git", "apply", "--check", patch, cwd=SOURCE)
            run("git", "apply", patch, cwd=SOURCE)

    cmake = TOOLS / "cmake" / "data" / "bin" / "cmake.exe"
    ninja = TOOLS / "bin" / "ninja.exe"
    zig = TOOLS / "ziglang" / "zig.exe"
    pybind_dir = TOOLS / "pybind11" / "share" / "cmake" / "pybind11"
    python_include = Path(sysconfig.get_paths()["include"])
    python_library = Path(sys.base_prefix) / "libs" / f"python{sys.version_info.major}{sys.version_info.minor}.lib"

    build_env = dict(os.environ)
    build_env["ZIG_EXECUTABLE"] = zig.as_posix()
    # A killed Zig linker can leave its cache lock owned by a Windows zombie
    # process. Per-invocation caches keep the next build recoverable without a
    # reboot; all of them remain under the ignored native-build directory.
    zig_session_cache = CACHE / "zig-sessions" / str(os.getpid())
    zig_global_cache = zig_session_cache / "global"
    zig_local_cache = zig_session_cache / "local"
    zig_global_cache.mkdir(parents=True, exist_ok=True)
    zig_local_cache.mkdir(parents=True, exist_ok=True)
    build_env["ZIG_GLOBAL_CACHE_DIR"] = str(zig_global_cache)
    build_env["ZIG_LOCAL_CACHE_DIR"] = str(zig_local_cache)
    run(
        cmake,
        "-S", ROOT / "simulator" / "native",
        "-B", BUILD,
        "-G", "Ninja",
        f"-DCMAKE_MAKE_PROGRAM={ninja}",
        f"-DCMAKE_TOOLCHAIN_FILE={ROOT / 'simulator' / 'native' / 'zig-windows-toolchain.cmake'}",
        f"-DLIGHTSPEED_SOURCE_DIR={SOURCE}",
        f"-Dpybind11_DIR={pybind_dir}",
        f"-DPython_EXECUTABLE={sys.executable}",
        f"-DPython_INCLUDE_DIR={python_include}",
        f"-DPython_LIBRARY={python_library}",
        "-DCMAKE_BUILD_TYPE=Release",
        env=build_env,
    )
    run(cmake, "--build", BUILD, "--parallel", str(os.cpu_count() or 4), env=build_env)
    run(
        sys.executable,
        "-c",
        "from spirecomm.simulator import LightspeedBattle; "
        "assert LightspeedBattle is not None; print('native bridge: OK')",
        cwd=ROOT,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
