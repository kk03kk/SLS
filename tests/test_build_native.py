from __future__ import annotations

import os
from pathlib import Path
from subprocess import CompletedProcess

import pytest

from tools.build_native import (
    build_tool_paths,
    configure_command,
    native_build_command,
    sanitizer_environment,
)


@pytest.mark.parametrize(
    ("system", "cmake_name", "ninja_name"),
    (("Linux", "cmake", "ninja"), ("Windows", "cmake.exe", "ninja.exe")),
)
def test_cmake_uses_packaged_binary_on_every_platform(
    tmp_path: Path, system: str, cmake_name: str, ninja_name: str,
) -> None:
    paths = build_tool_paths(system, tmp_path / "tools")
    assert paths.cmake == tmp_path / "tools" / "cmake" / "data" / "bin" / cmake_name
    assert paths.ninja == tmp_path / "tools" / "bin" / ninja_name
    assert paths.cmake in paths.required(system)
    assert tmp_path / "tools" / "bin" / "cmake" not in paths.required(system)


def test_linux_configure_uses_same_required_cmake_binary(tmp_path: Path) -> None:
    paths = build_tool_paths("Linux", tmp_path / "tools")
    source, build, output = tmp_path / "source", tmp_path / "build", tmp_path / "output"
    command = configure_command(
        "Linux", paths, source=source, build=build, output=output,
        python_executable="/home/h/hengzhi/venvs/sls/bin/python",
    )

    assert command[0] == paths.cmake
    assert paths.cmake in paths.required("Linux")
    assert f"-DCMAKE_MAKE_PROGRAM={paths.ninja}" in command
    assert f"-Dpybind11_DIR={paths.pybind_dir}" in command
    assert "-DPython_EXECUTABLE=/home/h/hengzhi/venvs/sls/bin/python" in command
    assert f"-DSLS_NATIVE_OUTPUT_DIR={output}" in command
    assert "-DCMAKE_BUILD_TYPE=Release" in command
    assert "-DSLS_NATIVE_SOURCE_SHA256=UNKNOWN" in command
    assert "-DSLS_GIT_COMMIT=UNKNOWN" in command
    assert "-DSLS_ENABLE_SANITIZERS=OFF" in command
    assert native_build_command(paths, 12, build=build) == [
        paths.cmake, "--build", build, "--parallel", 12,
    ]


def test_windows_configure_keeps_zig_toolchain_and_python_paths(tmp_path: Path) -> None:
    paths = build_tool_paths("Windows", tmp_path / "tools")
    source = tmp_path / "source"
    include = tmp_path / "python" / "include"
    library = tmp_path / "python" / "libs" / "python312.lib"
    command = configure_command(
        "Windows", paths, source=source, build=tmp_path / "build",
        output=tmp_path / "output", python_executable=r"D:\envs\DL\python.exe",
        python_include=include, python_library=library,
    )

    assert command[0] == tmp_path / "tools" / "cmake" / "data" / "bin" / "cmake.exe"
    assert paths.zig in paths.required("Windows")
    assert f"-DCMAKE_MAKE_PROGRAM={paths.ninja}" in command
    assert f"-Dpybind11_DIR={paths.pybind_dir}" in command
    assert f"-DCMAKE_TOOLCHAIN_FILE={source / 'cmake' / 'zig-windows-toolchain.cmake'}" in command
    assert f"-DPython_INCLUDE_DIR={include}" in command
    assert f"-DPython_LIBRARY={library}" in command
    assert r"-DPython_EXECUTABLE=D:\envs\DL\python.exe" in command


def test_linux_sanitizer_flag_is_explicit(tmp_path: Path) -> None:
    paths = build_tool_paths("Linux", tmp_path / "tools")
    command = configure_command(
        "Linux", paths, source=tmp_path / "source", build=tmp_path / "build",
        output=tmp_path / "output", source_digest="digest", git_commit="commit",
        sanitizers=True,
    )
    assert "-DSLS_NATIVE_SOURCE_SHA256=digest" in command
    assert "-DSLS_GIT_COMMIT=commit" in command
    assert "-DSLS_ENABLE_SANITIZERS=ON" in command


def test_sanitizer_environment_preloads_cxx_runtime_for_exceptions(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch,
) -> None:
    asan = tmp_path / "libasan.so"
    stdcpp = tmp_path / "libstdc++.so.6"
    asan.touch()
    stdcpp.touch()

    def compiler_query(command: tuple[str, str], **_: object) -> CompletedProcess[str]:
        library = command[1].removeprefix("-print-file-name=")
        path = {asan.name: asan, stdcpp.name: stdcpp}[library]
        return CompletedProcess(command, 0, stdout=f"{path}\n", stderr="")

    monkeypatch.setattr("tools.build_native.subprocess.run", compiler_query)
    configured = sanitizer_environment("c++", {"LD_PRELOAD": "/existing.so"})

    assert configured["LD_PRELOAD"] == os.pathsep.join(
        (str(asan), str(stdcpp), "/existing.so")
    )
    assert configured["ASAN_OPTIONS"] == "detect_leaks=0"
