"""Filesystem containment shared by login-node tools; standard library only."""

from pathlib import Path

from sls.evidence_paths import repository_evidence_path


def repository_path(root: Path, value: str | Path) -> Path:
    return repository_evidence_path(root, value)
