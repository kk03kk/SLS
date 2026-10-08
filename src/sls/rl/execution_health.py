"""Execution failures are distinct from unsuccessful policy episodes."""
import math
from collections.abc import Mapping

EXECUTION_FAILURE_FIELDS = ("backend_errors", "backend_truncations", "timeouts")


def finite_payload(value):
    if isinstance(value, float):
        return math.isfinite(value)
    if isinstance(value, Mapping):
        return all(finite_payload(v) for v in value.values())
    if isinstance(value, (list, tuple)):
        return all(finite_payload(v) for v in value)
    return True


def execution_healthy(result):
    return (all(result.get(k, -1) == 0 for k in EXECUTION_FAILURE_FIELDS)
            and finite_payload(result))


def selection_health_fields(record, legacy_fields):
    policy = record.get("execution_health_policy")
    if policy is None:
        return legacy_fields
    if policy != "execution-only-v1":
        raise ValueError("unknown checkpoint execution health policy")
    return EXECUTION_FAILURE_FIELDS
