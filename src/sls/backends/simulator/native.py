"""Load the locally built native bridge without committing binary artifacts."""

from __future__ import annotations

import importlib.machinery
import importlib.util
import os
import sys
from pathlib import Path


def _artifact_directory() -> Path:
    configured = os.environ.get("SLS_NATIVE_BUILD_DIR")
    if configured:
        return Path(configured)
    root = Path(__file__).resolve().parents[4]
    return root / ".build" / "native" / sys.implementation.cache_tag


def _load() -> None:
    directory = _artifact_directory()
    candidates = [
        candidate
        for suffix in importlib.machinery.EXTENSION_SUFFIXES
        for candidate in directory.glob(f"_lightspeed*{suffix}")
    ]
    if not candidates:
        raise ImportError(
            "The native simulator is not built. Run "
            "`python tools/build_native.py` from the repository root."
        )
    artifact = sorted(candidates)[0]
    # The compiled initialization symbol is PyInit__lightspeed.
    spec = importlib.util.spec_from_file_location("_lightspeed", artifact)
    if spec is None or spec.loader is None:
        raise ImportError(f"Cannot load native lightspeed module from {artifact}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[__name__] = module
    spec.loader.exec_module(module)
    globals().update(module.__dict__)


_load()
