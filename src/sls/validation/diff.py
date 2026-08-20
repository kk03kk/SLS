"""Recursive canonical comparison with stable field paths."""

from __future__ import annotations

from collections.abc import Mapping, Sequence
from typing import Any


def differences(left: Any, right: Any, path: str = "$") -> dict[str, tuple[Any, Any]]:
    result: dict[str, tuple[Any, Any]] = {}
    if isinstance(left, Mapping) and isinstance(right, Mapping):
        for key in sorted(set(left) | set(right), key=str):
            child = f"{path}.{key}"
            if key not in left:
                result[child] = (None, right[key])
            elif key not in right:
                result[child] = (left[key], None)
            else:
                result.update(differences(left[key], right[key], child))
        return result
    if (
        isinstance(left, Sequence)
        and isinstance(right, Sequence)
        and not isinstance(left, (str, bytes))
        and not isinstance(right, (str, bytes))
    ):
        if len(left) != len(right):
            result[f"{path}.length"] = (len(left), len(right))
        for index, (left_item, right_item) in enumerate(zip(left, right)):
            result.update(differences(left_item, right_item, f"{path}[{index}]"))
        return result
    if left != right:
        result[path] = (left, right)
    return result
