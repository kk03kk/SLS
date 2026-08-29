"""Read and validate the committed content registry artifact."""

from __future__ import annotations

import json
from importlib.resources import files


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

    def _validate(self) -> None:
        if self.payload.get("schema_version") != 1:
            raise ValueError("unsupported content registry schema")
        required = {"id", "ordinal"}
        allowed = required | {"game_id"}
        for category, items in self.categories.items():
            ids: list[str] = []
            ordinals: list[int] = []
            for item in items:
                if not required.issubset(item) or not set(item).issubset(allowed):
                    raise ValueError(f"invalid fields for {category}/{item.get('id')}")
                ids.append(item["id"])
                ordinals.append(item["ordinal"])
            if len(ids) != len(set(ids)) or len(ordinals) != len(set(ordinals)):
                raise ValueError(f"duplicate ID or ordinal in {category}")


def load_content_registry() -> ContentRegistry:
    resource = files("sls.content").joinpath("registry.json")
    return ContentRegistry(json.loads(resource.read_text(encoding="utf-8")))
