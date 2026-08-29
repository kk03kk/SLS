"""Build the canonical native simulator on Windows or Linux."""

from __future__ import annotations

import argparse
import hashlib
import os
import platform
import shutil
import subprocess
import sys
import sysconfig
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / ".build"
TOOLS = CACHE / "tools"
SOURCE = ROOT / "cpp" / "simulator"
BUILD = CACHE / f"cmake-{sys.platform}-{sys.implementation.cache_tag}"
OUTPUT = CACHE / "native" / sys.implementation.cache_tag


def _git(*args: str) -> str:
    return subprocess.run(
        ("git", *args), cwd=ROOT, check=True, capture_output=True,
        text=True, encoding="utf-8",
    ).stdout.strip()


def native_source_digest() -> str:
    paths = (
        "cpp/simulator", "src/sls/backends/simulator", "src/sls/content",
        "tools/build_native.py",
    )
    entries = _git("ls-files", "-s", "--", *paths).splitlines()
    if not entries:
        raise RuntimeError("native build provenance contains no tracked files")
    encoded = ("\n".join(sorted(entries)) + "\n").encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


@dataclass(frozen=True, slots=True)
class BuildToolPaths:
    cmake: Path
    ninja: Path
    pybind_dir: Path
    zig: Path

    def required(self, system: str) -> tuple[Path, ...]:
        common = (self.cmake, self.ninja, self.pybind_dir)
        return (*common, self.zig) if system == "Windows" else common


def build_tool_paths(system: str, tools: Path = TOOLS) -> BuildToolPaths:
    if system not in {"Windows", "Linux"}:
        raise ValueError(f"unsupported native build platform: {system}")
    suffix = ".exe" if system == "Windows" else ""
    return BuildToolPaths(
        cmake=tools / "cmake" / "data" / "bin" / f"cmake{suffix}",
        ninja=tools / "bin" / f"ninja{suffix}",
        pybind_dir=tools / "pybind11" / "share" / "cmake" / "pybind11",
        zig=tools / "ziglang" / "zig.exe",
    )


def configure_command(
    system: str,
    paths: BuildToolPaths,
    *,
    source: Path = SOURCE,
    build: Path = BUILD,
    output: Path = OUTPUT,
    python_executable: str = sys.executable,
    python_include: Path | None = None,
    python_library: Path | None = None,
    source_digest: str = "UNKNOWN",
    git_commit: str = "UNKNOWN",
    sanitizers: bool = False,
) -> list[object]:
    command: list[object] = [
        paths.cmake, "-S", source, "-B", build, "-G", "Ninja",
        f"-DCMAKE_MAKE_PROGRAM={paths.ninja}", f"-Dpybind11_DIR={paths.pybind_dir}",
        f"-DPython_EXECUTABLE={python_executable}",
        f"-DSLS_NATIVE_OUTPUT_DIR={output}", "-DCMAKE_BUILD_TYPE=Release",
        f"-DSLS_NATIVE_SOURCE_SHA256={source_digest}",
        f"-DSLS_GIT_COMMIT={git_commit}",
        f"-DSLS_ENABLE_SANITIZERS={'ON' if sanitizers else 'OFF'}",
    ]
    if system == "Windows":
        include = python_include or Path(sysconfig.get_paths()["include"])
        library = python_library or Path(sys.base_prefix) / "libs" / (
            f"python{sys.version_info.major}{sys.version_info.minor}.lib"
        )
        command.extend([
            f"-DCMAKE_TOOLCHAIN_FILE={source / 'cmake' / 'zig-windows-toolchain.cmake'}",
            f"-DPython_INCLUDE_DIR={include}",
            f"-DPython_LIBRARY={library}",
        ])
    return command


def native_build_command(
    paths: BuildToolPaths, jobs: int, *, build: Path = BUILD,
) -> list[object]:
    return [paths.cmake, "--build", build, "--parallel", jobs]


def run(*args: object, env: dict[str, str] | None = None) -> None:
    command = [str(arg) for arg in args]
    print("+", subprocess.list2cmdline(command), flush=True)
    subprocess.run(command, cwd=ROOT, env=env, check=True)


def sanitizer_environment(
    compiler: str, environment: dict[str, str],
) -> dict[str, str]:
    """Preload ASan and the C++ runtime for a sanitized Python extension."""

    runtimes: list[Path] = []
    for library in ("libasan.so", "libstdc++.so.6"):
        result = subprocess.run(
            (compiler, f"-print-file-name={library}"),
            check=True, capture_output=True, text=True,
        )
        runtime = Path(result.stdout.strip())
        if not runtime.is_file():
            raise RuntimeError(
                f"compiler did not provide sanitizer dependency {library}: {runtime}"
            )
        runtimes.append(runtime)
    configured = dict(environment)
    existing = configured.get("LD_PRELOAD")
    preload = os.pathsep.join(str(runtime) for runtime in runtimes)
    configured["LD_PRELOAD"] = preload + (os.pathsep + existing if existing else "")
    configured.setdefault("ASAN_OPTIONS", "detect_leaks=0")
    return configured


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jobs", type=int, default=min(os.cpu_count() or 4, 8))
    parser.add_argument("--sanitizers", action="store_true")
    args = parser.parse_args()
    if args.jobs <= 0:
        parser.error("--jobs must be positive")
    if sys.version_info < (3, 12):
        raise RuntimeError("SLS requires Python 3.12 or newer")
    system = platform.system()
    if system not in {"Windows", "Linux"}:
        raise RuntimeError("the native simulator supports Windows and Linux")
    if not (SOURCE / "SLS_VENDOR.json").is_file():
        raise RuntimeError("canonical simulator source is incomplete")

    paths = build_tool_paths(system)
    if not all(path.exists() for path in paths.required(system)):
        run(
            sys.executable, "-m", "pip", "install",
            "--disable-pip-version-check", "--no-input", "--upgrade",
            "--target", TOOLS,
            "cmake==4.4.2", "ninja==1.13.0", "pybind11==3.1.0",
            *(["ziglang==0.16.0"] if system == "Windows" else []),
        )

    missing = [path for path in paths.required(system) if not path.exists()]
    if missing:
        raise FileNotFoundError(
            "native build tool installation is incomplete: "
            + ", ".join(str(path) for path in missing)
        )

    OUTPUT.mkdir(parents=True, exist_ok=True)
    build_env = dict(os.environ)
    if args.sanitizers and system == "Windows":
        parser.error("--sanitizers is currently supported on Linux only")
    configure = configure_command(
        system, paths, source_digest=native_source_digest(),
        git_commit=_git("rev-parse", "HEAD"), sanitizers=args.sanitizers,
    )
    if system == "Windows":
        build_env["ZIG_EXECUTABLE"] = paths.zig.as_posix()
    else:
        compiler = os.environ.get("CXX") or shutil.which("c++") or shutil.which("g++")
        if not compiler:
            raise RuntimeError("Linux native build requires CXX, c++, or g++")
        if args.sanitizers:
            build_env = sanitizer_environment(compiler, build_env)

    run(*configure, env=build_env)
    run(*native_build_command(paths, args.jobs), env=build_env)
    run(
        sys.executable, "-c",
        "from sls.backends.simulator.native import LightspeedRunState; "
        "assert LightspeedRunState; print('native simulator: OK')",
        env={**build_env, "PYTHONPATH": str(ROOT / "src")},
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
