"""The stock probe must reject malformed inputs before launching the game."""
import copy
import json
from pathlib import Path

import pytest

from tools.capture_key_room_batch import post_choice_ready
from tools.run_key_room_batch import root_path, scene_floor, validate_manifest
from tools.verify_oracle import properties_command

PATH = Path(__file__).resolve().parents[2] / (
    "native/oracle/resources/spirecomm/parity/fullrun-key-acquisition-r1.json")


def manifest():
    return json.loads(PATH.read_text(encoding="utf-8"))


def test_java_properties_command_preserves_ascii_escaping_and_unicode():
    command = 'C\\:/Users/黄/"key room"/capture.py 🗝'
    assert properties_command(command) == 'C\\:/Users/\\u9ec4/"key room"/capture.py \\ud83d\\udddd'
    assert properties_command("python --output /tmp/new.json") == "python --output /tmp/new.json"


def test_blue_key_waits_for_effect_even_if_proceed_is_already_available():
    payload = {"available_commands": ["choose", "proceed"], "_parity_run": {"sapphire_key": False}}
    assert not post_choice_ready(payload, "TAKE_BLUE_KEY", {"relics": ["Burning Blood"]})
    payload["_parity_run"]["sapphire_key"] = True
    assert post_choice_ready(payload, "TAKE_BLUE_KEY", {"relics": ["Burning Blood"]})


def test_frozen_scenes_have_disjoint_seeds_and_stock_actions():
    source = manifest()
    selected = validate_manifest(source, [scene["id"] for scene in source["scenes"]])
    assert len(selected) == 8
    assert sum(len(scene["seeds"]) for scene in selected) == 24
    assert next(s for s in selected if s["id"] == "ruby-recall")["initial"]["ruby_key"] is False
    assert next(s for s in selected if s["id"] == "sapphire-take-key")["initial"]["sapphire_key"] is False


def test_reachable_revision_has_separate_seeds_and_explicit_context():
    source = json.loads(PATH.with_name("fullrun-key-acquisition-r2.json").read_text(encoding="utf-8"))
    selected = validate_manifest(source, [scene["id"] for scene in source["scenes"]])
    assert {seed for scene in selected for seed in scene["seeds"]} == set(range(131200330, 131200354))
    assert scene_floor(selected[0], {"_parity_run": {"current_map_y": 5}}) == 23
    selected[0]["floor"] = 25
    with pytest.raises(ValueError, match="floor derivation"):
        validate_manifest(source, [selected[0]["id"]])


def test_root_witness_requires_full_path_not_just_a_parent():
    source = {"game_state": {"map": [
        {"x": 1, "y": 0, "children": [{"x": 1, "y": 1}]},
        {"x": 1, "y": 1, "children": [{"x": 1, "y": 2}]},
        {"x": 1, "y": 2, "children": []},
        {"x": 0, "y": 1, "children": [{"x": 0, "y": 2}]},
        {"x": 0, "y": 2, "children": []}]},
        "_parity_run": {"current_map_x": 1, "current_map_y": 2}}
    assert root_path(source) == [[1, 0], [1, 1], [1, 2]]
    source["_parity_run"]["current_map_x"] = 0
    with pytest.raises(ValueError, match="not root reachable"):
        root_path(source)


def test_root_witness_handles_stock_virtual_boss_but_rejects_internal_gap():
    nodes = [{"x": 1, "y": y, "children": [{"x": 1, "y": y + 1}]} for y in range(14)]
    nodes.append({"x": 1, "y": 14, "children": [{"x": 3, "y": 16}]})
    source = {"game_state": {"map": nodes}, "_parity_run": {"current_map_x": 1, "current_map_y": 14}}
    assert len(root_path(source)) == 15
    nodes[4]["children"] = [{"x": 1, "y": 6}]
    with pytest.raises(ValueError, match="non-layered"):
        root_path(source)


@pytest.mark.parametrize("failure", ["schema", "collision", "namespace", "unknown", "key", "action"])
def test_rejects_invalid_scene_before_execution(failure):
    source = copy.deepcopy(manifest())
    selection = ["ruby-recall"]
    if failure == "schema":
        source["schema"] = "unbound"
    elif failure == "collision":
        source["scenes"][1]["seeds"][0] = source["scenes"][0]["seeds"][0]
    elif failure == "namespace":
        source["scenes"][0]["seeds"][0] = 132100000
    elif failure == "unknown":
        selection = ["missing"]
    elif failure == "key":
        source["scenes"][0]["initial"]["ruby_key"] = "false"
    elif failure == "action":
        source["scenes"][0]["actions"] = ["SET_RUBY_KEY"]
    with pytest.raises(ValueError):
        validate_manifest(source, selection)
