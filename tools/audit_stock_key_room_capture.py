"""Bind completed raw stock probes and summarize observed resource changes.

This is stock capture integrity, not simulator parity or a win-rate estimate.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path

from sls.audit.stock_clock import verify_sealed_oracle
from tools.capture_key_room_batch import post_choice_ready, select_action


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def audit(capture: Path, build_path: Path, manifest_path: Path, selected: list[str] | None = None) -> dict:
    data = json.loads(capture.read_text(encoding="utf-8"))
    launch = json.loads(capture.with_suffix(".launch.json").read_text(encoding="utf-8"))
    build = json.loads(build_path.read_text(encoding="utf-8"))
    jar = build_path.with_name(build_path.name.removesuffix(".build.json") + ".jar")
    verify_sealed_oracle(jar, build)
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if selected is not None and (not selected or len(set(selected)) != len(selected)
                                or set(selected) - {scene["id"] for scene in manifest["scenes"]}):
        raise ValueError("unknown or duplicate scene selection")
    resource = "spirecomm/parity/fullrun-key-acquisition-r1.json"
    if (data.get("schema") != "sls-key-room-capture-v1" or not data.get("execution_complete")
            or data.get("execution_error") or launch["completion"]["exit_code"] != 0
            or launch["recovery_status"] != "RECOVERED"
            or launch["oracle_sha256"] != build["output_sha256"]
            or sha(manifest_path) != build["members"][resource]
            or data["manifest_sha256"] != build["members"][resource]
            or data["stock_jar_sha256"] != build["dependencies"]["game"]):
        raise ValueError("incomplete or unbound stock capture")
    rows, seen = [], set()
    for row in data["runs"]:
        scene, seed = row["scene"], row["seed"]
        if selected is not None and scene["id"] not in selected:
            continue
        identity = (scene["id"], seed)
        if (scene not in manifest["scenes"] or seed not in scene["seeds"] or identity in seen
                or not row.get("capture_complete") or len(row["actions"]) != len(scene["actions"])
                or len(row["boundaries"]) != len(row["actions"]) + 1):
            raise ValueError("duplicate, undeclared or incomplete case")
        seen.add(identity)
        for index, action in enumerate(row["actions"]):
            expected, commands = select_action(row["boundaries"][index], scene["actions"][index])
            if (action["requested"] != scene["actions"][index]
                    or action["commands"] != list(commands)
                    or action["actual"]["kind"] != expected.kind.value
                    or action["actual"]["option_id"] != expected.option_id
                    or action["actual"]["reward_id"] != expected.reward_id):
                raise ValueError("recorded action differs from actual legal option")
        before, after = row["boundaries"][0], row["boundaries"][-1]
        if not post_choice_ready(after, scene["actions"][-1], scene["initial"]):
            raise ValueError("stock choice effect not completed at recorded terminal boundary")
        direct0, direct1 = before["_stock_direct"], after["_stock_direct"]
        initial = scene["initial"]
        if (direct0["act"] != 2 or direct0["floor"] != scene["floor"]
                or direct0["dungeon_class"].rsplit(".", 1)[-1] != "TheCity"
                or direct0["player"]["current_hp"] != initial["hp"]
                or direct0["player"]["max_hp"] != initial["max_hp"]
                or before["game_state"]["gold"] != initial["gold"]
                or before["_parity_run"]["ruby_key"] != initial["ruby_key"]
                or before["_parity_run"]["sapphire_key"] != initial["sapphire_key"]
                or Counter(card["id"] for card in direct0["master_deck"]) != Counter(initial["deck"])
                or Counter(relic["id"] for relic in direct0["relics"]) != Counter(initial["relics"])):
            raise ValueError("stock initial state differs from frozen setup")
        rows.append(dict(scene=scene["id"], seed=seed,
                         initial_keys=before["_parity_run"], final_keys=after["_parity_run"],
                         hp_before=direct0["player"]["current_hp"], hp_after=direct1["player"]["current_hp"],
                         initial_relics=direct0["relics"], final_relics=direct1["relics"],
                         deck_equal=direct0["master_deck"] == direct1["master_deck"],
                         rng_equal=before["_rng"] == after["_rng"],
                         initial_choices=before["game_state"].get("choice_list"),
                         initial_reward_state=before.get("_stock_reward_state"),
                         final_reward_state=after.get("_stock_reward_state")))
    selected_ids = {scene for scene, _ in seen}
    expected_cases = {(scene["id"], seed) for scene in manifest["scenes"]
                      if scene["id"] in selected_ids for seed in scene["seeds"]}
    if not seen or seen != expected_cases or (selected is not None and selected_ids != set(selected)):
        raise ValueError("missing declared seed cases")
    return dict(schema="sls-stock-key-room-summary-v1", simulator_comparison="NOT_YET_PERFORMED",
                capture_sha256=sha(capture), launch_sha256=sha(capture.with_suffix(".launch.json")),
                build_sha256=sha(build_path), manifest_sha256=sha(manifest_path),
                case_count=len(rows), runs=rows)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture", type=Path, required=True)
    parser.add_argument("--oracle-build", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--scenes", nargs="+")
    args = parser.parse_args()
    result = audit(args.capture, args.oracle_build, args.manifest, args.scenes)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(result, stream, indent=2)
        stream.write("\n")
    print(json.dumps({"cases": result["case_count"], "simulator_comparison": result["simulator_comparison"]}))


if __name__ == "__main__":
    main()
