"""Validated inventory of shared combat mechanics and their evidence."""

from __future__ import annotations

import json
from importlib.resources import files


IMPLEMENTATION_STATUSES = {"unimplemented", "partial", "implemented"}
EVIDENCE_STATUSES = {"none", "upstream", "unit", "oracle_trace"}


class CombatMechanicsMatrix:
    def __init__(self, payload: dict):
        self.payload = payload
        self._validate()

    @property
    def mechanics(self) -> tuple[dict, ...]:
        return tuple(self.payload["mechanics"])

    def get(self, mechanic_id: str) -> dict:
        for mechanic in self.mechanics:
            if mechanic["id"] == mechanic_id:
                return mechanic
        raise KeyError(f"unknown combat mechanic: {mechanic_id}")

    def counts(self) -> dict[str, int]:
        result = {"total": len(self.mechanics)}
        for mechanic in self.mechanics:
            status = mechanic["implementation"]
            result[status] = result.get(status, 0) + 1
        return result

    def _validate(self) -> None:
        if self.payload.get("schema_version") != 1:
            raise ValueError("unsupported combat mechanics schema")
        required = {
            "id", "area", "description", "implementation", "evidence",
            "evidence_files", "notes",
        }
        ids = []
        for mechanic in self.payload.get("mechanics", []):
            if set(mechanic) != required:
                raise ValueError(f"invalid mechanic fields for {mechanic.get('id')}")
            if mechanic["implementation"] not in IMPLEMENTATION_STATUSES:
                raise ValueError(f"invalid implementation status for {mechanic['id']}")
            if mechanic["evidence"] not in EVIDENCE_STATUSES:
                raise ValueError(f"invalid evidence status for {mechanic['id']}")
            if mechanic["implementation"] == "unimplemented" and mechanic["evidence"] == "unit":
                raise ValueError(f"unimplemented mechanic cannot have unit evidence: {mechanic['id']}")
            ids.append(mechanic["id"])
        if not ids or len(ids) != len(set(ids)):
            raise ValueError("combat mechanic IDs must be present and unique")


def load_combat_mechanics_matrix() -> CombatMechanicsMatrix:
    resource = files("spirecomm.simulator").joinpath("combat_mechanics.json")
    return CombatMechanicsMatrix(json.loads(resource.read_text(encoding="utf-8")))
