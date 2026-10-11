"""Resolve relocated evidence without rewriting hash-bound historical records."""

from pathlib import Path


def repository_evidence_path(root: Path, value: str | Path) -> Path:
    root = root.resolve()
    path = (root / value).resolve()
    if not path.is_relative_to(root):
        raise ValueError("path must remain within the repository")
    relative = path.relative_to(root).as_posix()
    for old, new in (
        ("docs/results/", "tests/fixtures/evidence/results/"),
        ("docs/audits/", "tests/fixtures/evidence/audits/"),
        ("runs/archives/", "local/archives/legacy/"),
    ):
        if relative.startswith(old):
            path = (root / (new + relative[len(old):])).resolve()
            break
    else:
        if relative == "docs/training-runtime-contracts.md":
            path = (root / "tests/fixtures/evidence/training-runtime-contracts.md").resolve()
    if not path.is_relative_to(root):
        raise ValueError("path must remain within the repository")
    return path
