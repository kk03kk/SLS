"""Check the frozen canary roles against actual stock public trajectories."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.diagnostics.canary import read_trajectory
from sls.rl.training_contract import native_source_digest

BOSSES = {'CHAMP': {'CHAMP', 'THE_CHAMP'}, 'COLLECTOR': {'COLLECTOR', 'THE_COLLECTOR'},
          'AUTOMATON': {'AUTOMATON', 'BRONZE_AUTOMATON'}}
ELITES = {'BOOK_OF_STABBING', 'GREMLIN_LEADER', 'TASKMASTER'}


def witnessed_roles(rows: list[dict]) -> dict:
    selected_room = None
    elites, bosses = set(), set()
    last_combat = None
    for row in rows:
        obs = row['observation']
        action = row['chosen_action']
        if action and action['kind'] == 'CHOOSE_MAP_NODE':
            node = next((n for n in obs['map_nodes'] if n['node_id'] == action['node_id']), None)
            if node is None:
                raise ValueError('chosen public map node absent')
            selected_room = (row['act'], node['visible_room_type'])
        if row['act'] == 2 and row['screen'] == 'COMBAT' and obs['enemies']:
            enemies = {e['monster_id'] for e in obs['enemies']}
            last_combat = {'boundary': row['step_index'], 'floor': row['floor'],
                           'enemies': sorted(enemies), 'selected_room': selected_room}
            if selected_room == (2, 'ELITE'):
                elites.add(row['floor'])
            for boss, ids in BOSSES.items():
                if enemies & ids:
                    bosses.add(boss)
    terminal = rows[-1]
    excluded = ELITES | set().union(*BOSSES.values())
    ordinary_failure = bool(last_combat and terminal['act'] == 2
        and terminal['terminal'] and terminal['terminal_reason'] == 'DEATH'
        and not set(last_combat['enemies']) & excluded
        and last_combat['selected_room'] == (2, 'MONSTER'))
    return {'ordinary_failure': ordinary_failure, 'elite_floors': sorted(elites),
            'boss_entries': sorted(bosses), 'last_act2_combat': last_combat}


def satisfies(role: str, evidence: dict) -> bool:
    if role == 'ACT2_ORDINARY_FAILURE':
        return evidence['ordinary_failure']
    if role == 'ACT2_ELITE_ENTRY':
        return bool(evidence['elite_floors'])
    if role.startswith('ACT2_BOSS_ENTRY:'):
        return role.split(':', 1)[1] in evidence['boss_entries']
    raise ValueError('unknown frozen canary role')


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--selection', type=Path, required=True)
    parser.add_argument('--trajectories', type=Path, nargs='+', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite stock coverage evidence')
    selection = json.loads(args.selection.read_text(encoding='utf-8'))
    # This checks witnessed STOCK roles of an immutable historical selection.
    # It does not rerun native selection or certify its compatibility with HEAD.
    # Preserve both identities instead of silently rebinding the selection.
    expected = {r['seed']: r['role'] for r in selection['runs']}
    if len(expected) != 8:
        raise ValueError('requires the frozen eight seeds')
    results = {}
    for path in args.trajectories:
        metadata, rows = read_trajectory(path)
        seed = metadata['seed']
        if seed not in expected or seed in results:
            raise ValueError('unselected or duplicate stock seed')
        if (metadata['backend'] != 'original' or not rows[-1]['terminal']
                or metadata['policy']['model_sha256'] != selection['model_sha256']
                or (rows[0]['act'], rows[0]['floor'], rows[0]['screen']) != (1, 0, 'NEOW')):
            raise ValueError('requires complete frozen stock normal-start trajectory')
        evidence = witnessed_roles(rows)
        results[seed] = {'seed': seed, 'role': expected[seed],
            'actual_stock_evidence': evidence, 'role_witnessed': satisfies(expected[seed], evidence),
            'trajectory_sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
    missing = sorted(set(expected) - {s for s, r in results.items() if r['role_witnessed']})
    result = {'schema': 'sls-act2-stock-coverage-v1', 'native_source_sha256': native_source_digest(),
              'selection_native_source_sha256': selection['native_source_sha256'],
              'selection_sha256': hashlib.sha256(args.selection.read_bytes()).hexdigest(),
              'runs': [results[s] for s in sorted(results)], 'missing_roles': missing,
              'coverage_complete': not missing,
              'scope': 'coverage only; does not certify rules or estimate win rate'}
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result))
    return 0 if not missing else 1


if __name__ == '__main__':
    raise SystemExit(main())
