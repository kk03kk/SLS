"""Reusable exact-state comparison for diagnostics and continuation evidence."""
from __future__ import annotations


def assert_same(left, right) -> None:
    import torch

    if isinstance(left, torch.Tensor):
        if not isinstance(right, torch.Tensor) or left.dtype != right.dtype or not torch.equal(left.cpu(), right.cpu()):
            raise ValueError('tensor state differs')
    elif isinstance(left, dict):
        if not isinstance(right, dict) or left.keys() != right.keys():
            raise ValueError('state keys differ')
        for key in left:
            assert_same(left[key], right[key])
    elif isinstance(left, (list, tuple)):
        if type(left) is not type(right) or len(left) != len(right):
            raise ValueError('state sequence differs')
        for a, b in zip(left, right, strict=True):
            assert_same(a, b)
    elif left != right:
        raise ValueError('scalar state differs')
