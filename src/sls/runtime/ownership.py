"""Process-lifetime exclusive ownership of the stock-game action channel."""

from __future__ import annotations

import os
from pathlib import Path
from typing import BinaryIO

_owners: dict[Path, BinaryIO] = {}


def acquire_live_owner(path: Path) -> None:
    """OS locks release on process death; never delete a potentially active lock."""
    path = path.resolve()
    if path in _owners:
        raise RuntimeError("another controller already owns the Original game channel")
    path.parent.mkdir(parents=True, exist_ok=True)
    stream = path.open("a+b")
    if stream.tell() == 0:
        stream.write(b"0")
        stream.flush()
    stream.seek(0)
    try:
        if os.name == "nt":
            import msvcrt

            msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
        else:
            import fcntl

            fcntl.flock(stream.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError as error:
        stream.close()
        raise RuntimeError("another controller already owns the Original game channel") from error
    _owners[path] = stream
