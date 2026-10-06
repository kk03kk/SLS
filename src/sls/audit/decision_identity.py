"""Explicit UI choice-index aliases; preserve every material property and mask."""

from __future__ import annotations

import copy
import json


def choice_aliases(observation: dict) -> dict[str, str]:
    rows = observation.get("choice_options") or []
    aliases = {row["instance_id"]: f"CHOICE:{index}" for index, row in enumerate(rows)}
    if len(aliases) != len(rows):
        raise ValueError("duplicate public choice identity")
    return aliases


def canonical_projection(observation: dict, actions: list[dict], terminal: bool) -> dict:
    observation = copy.deepcopy(observation)
    actions = copy.deepcopy(actions)
    aliases = choice_aliases(observation)
    for row in observation.get("choice_options") or []:
        row["instance_id"] = aliases[row["instance_id"]]
    for action in actions:
        if action.get("subject_id") in aliases:
            action["subject_id"] = aliases[action["subject_id"]]
    return {"observation": observation, "actions": sorted(actions, key=lambda a: json.dumps(a, sort_keys=True)),
            "terminal": terminal}


def mapped_action(action: dict, source_observation: dict, target_observation: dict) -> dict:
    action = dict(action)
    source = choice_aliases(source_observation)
    target = {alias: identifier for identifier, alias in choice_aliases(target_observation).items()}
    subject = action.get("subject_id")
    if subject in source:
        alias = source[subject]
        if alias not in target:
            raise ValueError("missing corresponding public choice")
        source_rows = source_observation["choice_options"]
        target_rows = target_observation["choice_options"]
        index = int(alias.split(":")[1])
        before, after = dict(source_rows[index]), dict(target_rows[index])
        before.pop("instance_id")
        after.pop("instance_id")
        if before != after:
            raise ValueError("choice alias would hide different card properties")
        action["subject_id"] = target[alias]
    return action
