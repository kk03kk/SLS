"""Small stock A20 multi-boundary harness diagnostic, not qualification."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.reward_boundary import collect_reward_boundary, reward_boundary_ready
from sls.audit.semantic_actions import resolve_target
from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A20_ACT2
from tools.capture_original_card_batch import _write_completion


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--encounters", nargs="+", default=["BOOK_OF_STABBING"])
    parser.add_argument("--turns", type=int, default=3)
    parser.add_argument("--manifest", type=Path)
    parser.add_argument("--scenes", nargs="*")
    args = parser.parse_args()
    from tools.build_oracle import resource_payloads
    allowlist = resource_payloads()["spirecomm/parity/scenario-encounter-allowlist.tsv"].decode()
    allowed = {line.split("\t")[0] for line in allowlist.splitlines()}
    if set(args.encounters) - allowed:
        raise ValueError(f"unknown encounter IDs: {sorted(set(args.encounters) - allowed)}")
    manifest = json.loads(args.manifest.read_text(encoding="utf-8")) if args.manifest else None
    cases = []
    if manifest:
        for scene in manifest["scenes"]:
            if scene.get("initial") and (args.scenes is None or scene["id"] in args.scenes):
                cases.extend((scene, seed) for seed in scene["seeds"])
        if args.scenes and set(args.scenes) - {s["id"] for s, _ in cases}:
            raise ValueError("unknown or unprepared scenes")
    else:
        cases = [(None, 131100000 + i) for i in range(len(args.encounters))]
    if args.output.exists():
        raise FileExistsError("refuse to overwrite stock evidence")
    result = {"schema": "sls-act2-encounter-capture-v1", "purpose": "HARNESS_DIAGNOSTIC", "runs": []}
    if manifest:
        result.update(scene_manifest_sha256=hashlib.sha256(args.manifest.read_bytes()).hexdigest(),
                      stock_jar_sha256=manifest["stock_jar_sha256"])
    try:
        session = OriginalSession()
        backend = OriginalBackend(session=session, profile=IRONCLAD_A20_ACT2)
        for index, (scene, seed) in enumerate(cases):
            encounter = scene["encounter"] if scene else args.encounters[index]
            decision = backend.reset(seed)
            for _ in range(40):
                if decision.observation.screen == "COMBAT":
                    break
                preferred = next((a for a in decision.actions if a.kind in {
                    ActionKind.CHOOSE_NEOW_OPTION, ActionKind.CHOOSE_MAP_NODE,
                    ActionKind.SELECT_CARD, ActionKind.CONFIRM,
                }), decision.actions[0])
                decision = backend.step(preferred).decision
            else:
                raise RuntimeError("failed to enter combat")
            before = backend.raw_payload
            if before.get("_oracle_mode") != "validation":
                raise ValueError("controlled probes require validation mode")
            context_start = None
            if scene and scene.get('actual_dungeon_required'):
                context_start = before
                before = session.execute(f"parity_dungeon {scene['act']} {scene['floor']} {scene['room']}")
            scene_command = ("parity_scene" if manifest and manifest.get("schema") == "sls-fullrun-scenes-v1"
                             else "parity_act2")
            corpus = f" {args.manifest.stem}" if scene_command == "parity_scene" else ""
            initial = session.execute(f"{scene_command} {scene['id']}{corpus}" if scene else
                                      f"parity_encounter {encounter} 20 2 20 harness-{index}")
            if scene and 'boss_order' in scene.get('initial', {}):
                from sls.audit.boss_flow import require_initial_boss_witness
                require_initial_boss_witness(initial, scene)
            row = {"seed": seed, "encounter": encounter, "ascension": 20, "act": 2,
                   "floor": 20, "before": before, "boundaries": [initial], "actions": []}
            if scene:
                row.update(act=scene["act"], floor=scene["floor"])
                if context_start is not None:
                    row['context_start'] = context_start
            result["runs"].append(row)
            if scene:
                row["scene"] = scene
            scripts = scene["actions"] if scene else [{"kind": "end_turn"}] * args.turns
            for action in scripts:
                if action['kind'] == 'proceed_to_second_boss':
                    from sls.audit.boss_flow import (
                        collect_first_boss_victory,
                        collect_second_boss_boundary,
                    )
                    order = scene['initial']['boss_order']
                    reward = collect_first_boss_victory(lambda: session.execute('state'), scene)
                    if 'proceed' not in reward.get('available_commands', []):
                        raise ValueError('stock boss victory does not offer proceed')
                    require_initial_boss_witness(reward, scene)
                    row.setdefault('boss_victory_boundaries', []).append(reward)
                    session.execute('proceed')
                    boundary = collect_second_boss_boundary(
                        lambda: session.execute('state'), first_floor=scene['floor'],
                        second=order[1], third=order[2])
                    row['actions'].append(action)
                    row.setdefault('resolved_actions', []).append(action)
                    row['boundaries'].append(boundary)
                    continue
                if (scene and scene.get('stop_at_reward_boundary')
                        and reward_boundary_ready(row['boundaries'][-1])):
                    break
                resolved = resolve_target(action, row['boundaries'][-1]['game_state']['combat_state']['monsters'])
                kind = resolved["kind"]
                if kind == "end_turn":
                    command = "end"
                elif kind == "play":
                    command = f"play {resolved['card_index']}"
                    if "target_index" in resolved:
                        command += f" {resolved['target_index']}"
                elif kind == "potion":
                    command = f"potion use {resolved['potion_index']} {resolved['target_index']}"
                else:
                    raise ValueError("unsupported semantic action")
                row["actions"].append(action)
                row.setdefault('resolved_actions', []).append(resolved)
                row["boundaries"].append(session.execute(command))
            if scene and scene.get('collect_reward_boundary'):
                row['reward_boundary'] = collect_reward_boundary(lambda: session.execute('state'))
            if scene and scene.get('collect_second_boss_victory'):
                from sls.audit.boss_flow import collect_second_boss_victory
                row['second_boss_victory'] = collect_second_boss_victory(
                    lambda: session.execute('state'), scene)
            if scene and scene.get('collect_victory_room_entry'):
                from sls.audit.boss_flow import collect_victory_room_entry
                if not scene.get('collect_second_boss_victory'):
                    raise ValueError('victory room requires witnessed second boss victory')
                session.execute('proceed')
                row['victory_room_entry'] = collect_victory_room_entry(
                    lambda: session.execute('state'), scene)
            if scene and scene.get('collect_act4_entry'):
                if not scene.get('collect_victory_room_entry'):
                    raise ValueError('Act4 entry requires witnessed VictoryRoom')
                row['victory_dialogue'] = []
                for _ in range(4):
                    before_dialogue = session.execute('state')
                    if 'choose' not in before_dialogue.get('available_commands', []):
                        raise ValueError('stock SpireHeart dialogue action unavailable')
                    row['victory_dialogue'].append(before_dialogue)
                    session.execute('choose 0')
                from sls.audit.boss_flow import collect_act4_map_entry
                row['act4_entry'] = collect_act4_map_entry(lambda: session.execute('state'))
            if scene and scene.get('collect_key_gate_outcome'):
                if scene.get('collect_act4_entry') or not scene.get('collect_victory_room_entry'):
                    raise ValueError('key gate needs exclusive witnessed VictoryRoom flow')
                from sls.audit.boss_flow import collect_key_gate_outcome
                row['key_gate_outcome'] = collect_key_gate_outcome(
                    lambda: session.execute('state'), session.execute, scene['initial']['keys'])
                if row['key_gate_outcome']['status'] == 'ACT4_MAP_ENTRY':
                    row['act4_entry'] = row['key_gate_outcome']['boundary']
            # Flush raw evidence after each completed run; append only in this
            # exclusively-created result, never replace evidence from prior runs.
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        backend.return_to_menu()
        result["execution_complete"] = True
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        _write_completion(0)
        return 0
    except BaseException as error:
        result["execution_error"] = f"{type(error).__name__}: {error}"
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        _write_completion(2, result["execution_error"])
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
