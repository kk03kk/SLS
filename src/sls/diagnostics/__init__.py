"""Policy-visible diagnostic utilities with lazy model-runtime imports."""

from importlib import import_module

__all__ = ["capture_policy_trajectory", "compare_trajectories", "read_trajectory"]


def __getattr__(name):
    if name not in __all__:
        raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
    value = getattr(import_module("sls.diagnostics.canary"), name)
    globals()[name] = value
    return value
