"""Filesystem containment shared by login-node tools; standard library only."""

from pathlib import Path


def repository_path(root: Path, value: str | Path) -> Path:
    path = (root / value).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError("path must remain within the repository")
    return path
