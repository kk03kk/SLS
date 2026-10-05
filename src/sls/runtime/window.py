"""Open the loopback inspector in a dedicated desktop browser window."""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
import webbrowser
from pathlib import Path


def edge_executable() -> Path | None:
    """Find the installed Edge binary without depending on a fixed Steam path."""

    candidates: list[Path] = []
    for name in ("PROGRAMFILES(X86)", "PROGRAMFILES", "LOCALAPPDATA"):
        base = os.environ.get(name)
        if base:
            candidates.append(Path(base) / "Microsoft" / "Edge" / "Application" / "msedge.exe")
    found = shutil.which("msedge") or shutil.which("msedge.exe")
    if found:
        candidates.append(Path(found))
    return next((path for path in candidates if path.is_file()), None)


def open_inspector_window(url: str) -> str:
    """Prefer a standalone Edge app window; fall back to the default browser."""

    if sys.platform == "win32":
        edge = edge_executable()
        if edge is not None:
            try:
                subprocess.Popen(
                    [str(edge), f"--app={url}", "--new-window"],
                    stdin=subprocess.DEVNULL,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
            except OSError:
                pass
            else:
                return "window"
    webbrowser.open(url)
    return "browser"
