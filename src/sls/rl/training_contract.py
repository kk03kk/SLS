"""Cross-platform training provenance and source contracts."""

from __future__ import annotations

import hashlib
import importlib
import json
from pathlib import Path
import subprocess
import sys
from typing import Iterable, Mapping


ROOT = Path(__file__).resolve().parents[3]
TRAINING_CHECKPOINT_SCHEMA = "sls-full-run-ppo-v3"
ACT1_PRODUCTION_READINESS_LOCK = (
    ROOT / "configs" / "validation" / "act1_training_readiness.lock.json"
)
ACT1_PRODUCTION_READINESS_LEVEL = "TRAINING_READY"
NATIVE_SOURCE_PATHS = (
    "cpp/simulator",
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


def readiness_settings(run: Mapping[str, object]) -> tuple[Path, str]:
    """Return the explicit lock contract for a readiness-gated run."""

    missing = [key for key in ("readiness_lock", "readiness_level") if key not in run]
    if missing:
        raise ValueError(
            "readiness-required training config is missing explicit field(s): "
            + ", ".join(missing)
        )
    return ROOT / str(run["readiness_lock"]), str(run["readiness_level"])


def _git(*args: str) -> str:
    result = subprocess.run(
        ("git", *args), cwd=ROOT, check=True, capture_output=True, text=True,
        encoding="utf-8",
    )
    return result.stdout.strip()


def git_state() -> dict[str, object]:
    return {
        "commit": _git("rev-parse", "HEAD"),
        "branch": _git("branch", "--show-current"),
        "dirty": bool(_git("status", "--porcelain", "--untracked-files=all")),
    }


def git_index_digest(paths: Iterable[str]) -> str:
    """Digest tracked Git blobs, independent of checkout line endings."""

    entries = _git("ls-files", "-s", "--", *tuple(paths)).splitlines()
    if not entries:
        raise RuntimeError("training contract contains no tracked files")
    payload = "\n".join(sorted(entries)) + "\n"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def native_source_digest() -> str:
    return git_index_digest(NATIVE_SOURCE_PATHS)


def native_artifact() -> dict[str, str] | None:
    try:
        module = importlib.import_module("sls.backends.simulator.native")
    except ImportError:
        return None
    origin = getattr(module, "__file__", None)
    if origin is None:
        return None
    path = Path(origin).resolve()
    return {"path": str(path), "sha256": sha256_file(path)}


def runtime_contract(torch_module: object) -> dict[str, object]:
    return {
        "python_cache_tag": sys.implementation.cache_tag,
        "torch": str(getattr(torch_module, "__version__")),
        "cuda": getattr(getattr(torch_module, "version"), "cuda"),
    }


def canonical_digest(value: object) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()
