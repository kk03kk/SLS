"""Cross-platform training provenance and source contracts."""

from __future__ import annotations

import hashlib
import importlib
import json
import subprocess
import sys
from collections.abc import Iterable
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TRAINING_CHECKPOINT_SCHEMA = "sls-full-run-ppo-v5"
NATIVE_SOURCE_PATHS = (
    "native/simulator",
    "src/sls/backends/simulator",
    "src/sls/content",
    "tools/build_native.py",
)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def source_sha256(path: Path) -> str:
    """Hash text evidence canonically across LF and CRLF checkouts."""

    payload = path.read_bytes().replace(b"\r\n", b"\n")
    return hashlib.sha256(payload).hexdigest()


def _git(*args: str) -> str:
    result = subprocess.run(
        ("git", *args), cwd=ROOT, check=True, capture_output=True, text=True,
        encoding="utf-8",
    )
    return result.stdout.strip()


def git_state() -> dict[str, object]:
    """Return optional Git metadata without making local execution depend on Git."""

    try:
        return {
            "commit": _git("rev-parse", "HEAD"),
            "branch": _git("branch", "--show-current"),
            "dirty": bool(_git("status", "--porcelain", "--untracked-files=all")),
        }
    except (FileNotFoundError, subprocess.CalledProcessError):
        return {"commit": "LOCAL", "branch": "", "dirty": None}


def git_index_digest(paths: Iterable[str]) -> str:
    """Digest tracked Git blobs, independent of checkout line endings."""

    entries = _git("ls-files", "-s", "--", *tuple(paths)).splitlines()
    if not entries:
        raise RuntimeError("training contract contains no tracked files")
    payload = "\n".join(sorted(entries)) + "\n"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def local_source_digest(paths: Iterable[str], *, root: Path = ROOT) -> str:
    """Hash a source tree from local paths, independent of Git and line endings."""

    files: list[Path] = []
    for relative in paths:
        target = root / relative
        if target.is_file():
            files.append(target)
        elif target.is_dir():
            files.extend(
                path for path in target.rglob("*")
                if path.is_file()
                and "__pycache__" not in path.parts
                and path.suffix not in {".pyc", ".pyo"}
            )
    if not files:
        raise RuntimeError("training contract contains no local source files")
    digest = hashlib.sha256()
    for path in sorted(files, key=lambda item: item.relative_to(root).as_posix()):
        relative = path.relative_to(root).as_posix().encode("utf-8")
        payload = path.read_bytes().replace(b"\r\n", b"\n")
        digest.update(len(relative).to_bytes(4, "big"))
        digest.update(relative)
        digest.update(len(payload).to_bytes(8, "big"))
        digest.update(payload)
    return digest.hexdigest()


def native_source_digest() -> str:
    return local_source_digest(NATIVE_SOURCE_PATHS)


def native_artifact() -> dict[str, str] | None:
    try:
        module = importlib.import_module("sls.backends.simulator.native")
    except ImportError:
        return None
    origin = getattr(module, "__file__", None)
    if origin is None:
        return None
    path = Path(origin).resolve()
    embedded_source = str(getattr(module, "NATIVE_SOURCE_SHA256", ""))
    expected_source = native_source_digest()
    if embedded_source != expected_source:
        raise RuntimeError(
            "native simulator is stale or has unverified provenance: "
            f"embedded={embedded_source or 'MISSING'} expected={expected_source}; "
            "run python tools/build_native.py"
        )
    return {
        "path": str(path),
        "sha256": sha256_file(path),
        "source_sha256": embedded_source,
        "git_commit": str(getattr(module, "GIT_COMMIT", "UNKNOWN")),
    }


def runtime_contract(torch_module: object) -> dict[str, object]:
    cuda = getattr(torch_module, "cuda")
    backends = getattr(torch_module, "backends")
    cuda_available = bool(cuda.is_available())
    return {
        "python_cache_tag": sys.implementation.cache_tag,
        "torch": str(getattr(torch_module, "__version__")),
        "cuda": getattr(getattr(torch_module, "version"), "cuda"),
        "cudnn": getattr(backends.cudnn, "version")() if cuda_available else None,
        "cuda_device_count": cuda.device_count() if cuda_available else 0,
        "cuda_device": cuda.get_device_name(0) if cuda_available else None,
        "deterministic_algorithms": bool(
            getattr(torch_module, "are_deterministic_algorithms_enabled")()
        ),
    }


def canonical_digest(value: object) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()
