"""Read and validate the committed content registry artifact."""

from __future__ import annotations

import json
from importlib.resources import files
from typing import Iterable


class ContentRegistry:
    def __init__(self, payload: dict):
        self.payload = payload
        self._validate()

    @property
    def categories(self) -> dict[str, list[dict]]:
        return self.payload["categories"]

    def items(self, category: str) -> tuple[dict, ...]:
        return tuple(self.categories[category])

    def get(self, category: str, content_id: str) -> dict:
        for item in self.categories[category]:
            if item["id"] == content_id:
                return item
        raise KeyError(f"unknown {category} ID: {content_id}")

    def counts(self, field: str = "implementation") -> dict[str, dict[str, int]]:
        result = {}
        for category, items in self.categories.items():
            counts: dict[str, int] = {"total": len(items)}
            for item in items:
                value = item[field]
                counts[value] = counts.get(value, 0) + 1
            result[category] = counts
        return result

    def _validate(self) -> None:
        if self.payload.get("schema_version") != 1:
            raise ValueError("unsupported content registry schema")
        vocabulary = self.payload["status_vocabulary"]
        required = {"id", "ordinal", "implementation", "evidence", "evidence_files"}
        for category, items in self.categories.items():
            ids: list[str] = []
            ordinals: list[int] = []
            for item in items:
                if set(item) != required:
                    raise ValueError(f"invalid fields for {category}/{item.get('id')}")
                if item["implementation"] not in vocabulary["implementation"]:
                    raise ValueError(f"invalid implementation status for {category}/{item['id']}")
                if item["evidence"] not in vocabulary["evidence"]:
                    raise ValueError(f"invalid evidence status for {category}/{item['id']}")
                ids.append(item["id"])
                ordinals.append(item["ordinal"])
            if len(ids) != len(set(ids)) or len(ordinals) != len(set(ordinals)):
                raise ValueError(f"duplicate ID or ordinal in {category}")


def load_content_registry() -> ContentRegistry:
    resource = files("spirecomm.content").joinpath("registry.json")
    return ContentRegistry(json.loads(resource.read_text(encoding="utf-8")))
