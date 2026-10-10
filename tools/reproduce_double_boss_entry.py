"""Replay stock-derived first-boss fixtures to second entry; no model inference."""

import argparse
import copy
import hashlib
import json
import re
import zipfile
from collections import Counter
from pathlib import Path

from sls.audit.act2_differential import (
    comparison_projection,
    direct_projection,
    production_combat_projection,
)
from sls.audit.awakened_flow import (
    require_awakened_first_lifecycle,
    require_awakened_second_lifecycle,
)
from sls.audit.boss_flow import require_initial_boss_witness
from sls.audit.stock_clock import verify_sealed_oracle
from sls.content.normalize import (
    normalize_card_id,
    normalize_content_id,
    normalize_monster_id,
)


def enum_ids(path, marker):
    body = path.read_text().split(marker, 1)[1].split('};', 1)[0]
    result, index = {}, 0
    for part in re.sub(r'//[^\n]*', '', body).split(','):
        if not part.strip():
            continue
        match = re.fullmatch(r'([A-Z][A-Z0-9_]*)(?:\s*=\s*(0x[0-9A-Fa-f]+|\d+))?', part.strip())
        if match is None:
            raise ValueError('unsupported native enum')
        if match[2] is not None:
            index = int(match[2], 0)
        result[match[1]], index = index, index + 1
    return result



def recorded_action_bits(action):
    """Translate recorded semantic combat actions; reject UI or malformed input."""
    if action.get('kind') == 'end_turn':
        return 2147483648
    if action.get('kind') != 'play':
        raise ValueError('unsupported recorded combat action')
    card, target = action.get('card_index'), action.get('target_index')
    if type(card) is not int or type(target) is not int or not 1 <= card <= 10 or not 0 <= target <= 4:
        raise ValueError('invalid recorded combat indices')
    return card - 1 | (target << 16)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', type=Path, required=True)
    parser.add_argument('--oracle-build', type=Path, required=True)
    parser.add_argument('--manifest', type=Path,
                        default=Path('native/oracle/resources/spirecomm/parity/fullrun-double-boss-entry-r1.json'))
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--seeds', type=int, nargs='+', help='Explicit subset; original capture stays intact')
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    data = json.loads(args.capture.read_text(encoding='utf-8'))
    source_runs = data['runs']
    if args.seeds is not None and (len(set(args.seeds)) != len(args.seeds)
            or not set(args.seeds) <= {r['seed'] for r in source_runs}):
        raise ValueError('duplicate or unknown explicit seed selection')
    selected_runs = [r for r in source_runs if args.seeds is None or r['seed'] in args.seeds]
    launch = json.loads(args.capture.with_suffix('.launch.json').read_text(encoding='utf-8'))
    build = json.loads(args.oracle_build.read_text(encoding='utf-8'))
    jar = args.oracle_build.with_name(args.oracle_build.name.removesuffix('.build.json') + '.jar')
    verify_sealed_oracle(jar, build)
    if (not data.get('execution_complete') or data.get('execution_error')
            or launch['completion']['exit_code'] != 0 or launch['recovery_status'] != 'RECOVERED'
            or launch['oracle_sha256'] != build['output_sha256']
            or data['stock_jar_sha256'] != build['dependencies']['game']):
        raise ValueError('original capture did not complete and recover')
    from sls.backends.simulator import native
    from sls.rl.training_contract import native_source_digest

    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError('stale native binary')
    root = Path(__file__).resolve().parents[1]
    constants = root / 'native/simulator/include/constants'
    cards = enum_ids(constants / 'Cards.h', 'enum class CardId : std::uint16_t {')
    relics = enum_ids(constants / 'Relics.h', 'enum class RelicId : std::uint8_t {')
    encounters = enum_ids(constants / 'MonsterEncounters.h', 'enum class MonsterEncounter : std::int8_t {')
    template = json.loads((root / 'tests/fixtures/regressions/act2-smoke-victory-heal-131100063.json').read_text())['before']
    runs = []
    for row in selected_runs:
        scene, stock = row['scene'], row['boundaries'][0]
        resource = 'spirecomm/parity/' + args.manifest.name
        with zipfile.ZipFile(jar) as archive:
            manifest = json.loads(archive.read(resource))
        if (scene not in manifest['scenes'] or row['seed'] not in scene['seeds']
                or data['scene_manifest_sha256'] != build['members'][resource]
                or hashlib.sha256(args.manifest.read_bytes()).hexdigest() != build['members'][resource]):
            raise ValueError('undeclared scene/seed or stale source manifest')
        require_initial_boss_witness(stock, scene)
        if scene['encounter'] not in ('TIME_EATER', 'DONU_AND_DECA', 'AWAKENED_ONE') or row['actions'] != scene['actions']:
            raise ValueError('only frozen A20 ordered boss entry scripts supported')
        if scene['encounter'] == 'AWAKENED_ONE':
            require_awakened_first_lifecycle(row)
        if scene['initial']['boss_order'][1] == 'AWAKENED_ONE':
            require_awakened_second_lifecycle(row)
        setup = scene['initial']
        run = native.LightspeedRunState()
        run.reset(row['seed'], 20)
        initial = run.snapshot()
        initial['run_state'].update(act=3, floor=scene['floor'],
                                    second_boss=encounters[setup['boss_order'][1]])
        initial['derived_rng']['map']['act'] = 3
        initial['progress_state'] = copy.deepcopy(template['progress_state'])
        initial['progress_state'].update(current_room=6, boss=encounters[scene['encounter']])
        initial['screen_info'] = copy.deepcopy(template['screen_info'])
        initial['screen_info']['encounter'] = encounters[scene['encounter']]
        initial['rng'] = copy.deepcopy(row['before']['_rng'])
        if 'card_rng_counter' in setup:
            initial['rng']['card'] = copy.deepcopy(stock['_rng']['card'])
        initial['player_state'].update(current_hp=setup['hp'], max_hp=setup['max_hp'], gold=setup.get('gold',99))
        for color, key in (('red_key', 'RUBY'), ('green_key', 'EMERALD'), ('blue_key', 'SAPPHIRE')):
            initial['player_state'][color] = key in setup.get('keys', [])

        deck = stock['_stock_direct']['master_deck']
        if any(c['upgrades'] > 1 and normalize_card_id(c['id']) != 'SEARING_BLOW' for c in deck):
            raise ValueError('unhandled permanent growth')
        initial['player_state']['deck'] = [{'id': cards[normalize_card_id(c['id'])],
                                           'upgraded': bool(c['upgrades']),
                                           'misc': c['upgrades'] if normalize_card_id(c['id']) == 'SEARING_BLOW'
                                           else 0} for c in deck]
        initial['player_state']['relics'] = [{'id': relics[normalize_content_id(r['id'])],
                                             'data': max(0, r['counter'])}
                                            for r in stock['_stock_direct']['relics']]
        # Stock MawBank uses usedUp, not its display counter (-1 while active).
        # This frozen scene installs a fresh copy and spends no gold before
        # entry, so its native active flag is1. Never apply this to arbitrary
        # captured relics whose usedUp state has not been independently proved.
        for r in initial['player_state']['relics']:
            if r['id'] == relics['MAW_BANK']:
                r['data'] = 1
        battle = native.LightspeedBattle()
        battle.reset_encounter_probe(row['seed'], scene['encounter'], initial['rng'],
                                     20, 3, scene['floor'], scene['id'])
        battle.set_player_health(setup['hp'], setup['max_hp'])
        battle.set_card_piles(setup['hand'], setup['draw'], [], [])
        battle.set_potions([])
        state = battle.snapshot()
        state['game_state']['gold'] = setup.get('gold',99)
        state['game_state']['combat_state']['player']['_internal']['gold'] = setup.get('gold',99)
        # set_potions is an isolated probe API with a default three-slot
        # capacity. Pair the actual A20 stock capacity explicitly in this
        # initial fixture; do not hide a surplus slot in the differencer.
        state['game_state']['combat_state']['_internal']['potion_capacity'] = len(stock['_stock_direct']['potions'])
        prototype = native.LightspeedBattle()
        prototype.reset(row['seed'], scene['encounter'], 20,
                        relics=[normalize_content_id(n) for n in setup['relics']], replace_relics=True)
        for key in ('relic_bits0', 'relic_bits1'):
            state['game_state']['combat_state']['player']['_internal'][key] = (
                prototype.snapshot()['game_state']['combat_state']['player']['_internal'][key])
        for monster in state['game_state']['combat_state']['monsters']:
            identifier = normalize_monster_id(monster['id'])
            if identifier in setup.get('monster_hp', {}):
                monster['current_hp'] = setup['monster_hp'][identifier]
        initial['combat_checkpoint'] = {'game_state': state['game_state'], 'rng': state['_rng']}
        run.load_state(initial)
        before = run.snapshot()
        entry_action = next(i for i, a in enumerate(row['actions']) if a['kind'] == 'proceed_to_second_boss')
        for action in row['resolved_actions'][:entry_action]:
            bits = recorded_action_bits(action)
            if not any(a['bits'] == bits for a in run.legal_actions()):
                raise ValueError('first combat scripted action unavailable')
            run.step(bits)
        after = run.snapshot()
        stock_initial = stock['_stock_direct']
        own = before['combat_checkpoint']['game_state']['combat_state']['player']
        initial_pairs = {
            'gold': (stock['game_state']['gold'], before['player_state']['gold']),
            'combat_gold': (stock['game_state']['gold'], before['combat_checkpoint']['game_state']['gold']),
            'hp': (stock_initial['player']['current_hp'], own['current_hp']),
            'max_hp': (stock_initial['player']['max_hp'], own['max_hp']),
            'rng': (stock['_rng'], {**before['rng'], **before['combat_checkpoint']['rng']}),
            'monsters': ([m['current_hp'] for m in stock['game_state']['combat_state']['monsters']],
                         [m['current_hp'] for m in before['combat_checkpoint']['game_state']['combat_state']['monsters']]),
        }
        initial_battle = native.LightspeedBattle()
        initial_battle.load_checkpoint(before['combat_checkpoint'])
        initial_view = production_combat_projection(initial_battle)
        initial_pairs['direct_extended'] = (direct_projection(stock, stock=True),
                                            direct_projection(initial_view, stock=False, extended=True))
        initial_pairs['public_and_actions'] = (comparison_projection(stock, stock=True),
                                              comparison_projection(initial_view, stock=False, extended_direct=True))
        initial_comparisons = {k: {'stock': p[0], 'native': p[1], 'equal': p[0] == p[1]}
                               for k, p in initial_pairs.items()}
        if 'combat_checkpoint' not in after:
            restored = native.LightspeedRunState()
            restored.load_state(after)
            runs.append({'seed': row['seed'], 'initial': before, 'after': after,
                         'initial_comparisons': initial_comparisons,
                         'legal_actions': run.legal_actions(), 'restored_legal_actions': restored.legal_actions(),
                         'restored': restored.snapshot(),
                         'first_difference': 'SECOND_BATTLE_NOT_INITIALIZED',
                         'comparisons': {}, 'scope': 'CONTROLLED_TRANSITION_FAILURE_NOT_FULL_PARITY'})
            continue
        target = row['boundaries'][entry_action + 1]
        direct = target['_stock_direct']
        player = after['combat_checkpoint']['game_state']['combat_state']['player']
        comparisons = {
            'floor': (direct['floor'], after['run_state']['floor']),
            'hp': (direct['player']['current_hp'], player['current_hp']),
            'max_hp': (direct['player']['max_hp'], player['max_hp']),
            'gold': (target['game_state']['gold'], after['player_state']['gold']),
            'deck': (dict(Counter(normalize_card_id(c['id']) for c in direct['master_deck'])),
                     dict(Counter(c['id'] for c in after['public_inventory']['deck']))),
            'rng': (target['_rng'], {**after['rng'], **after['combat_checkpoint']['rng']}),
            'monsters': ([normalize_monster_id(m['id']) for m in target['game_state']['combat_state']['monsters']],
                         [m['id'] for m in after['combat_checkpoint']['game_state']['combat_state']['monsters']]),
        }
        second_battle = native.LightspeedBattle()
        second_battle.load_checkpoint(after['combat_checkpoint'])
        second_view = production_combat_projection(second_battle)
        comparisons['direct_extended'] = (direct_projection(target, stock=True),
                                          direct_projection(second_view, stock=False, extended=True))
        try:
            stock_public = comparison_projection(target, stock=True)
        except ValueError as error:
            stock_public = {'qualification_error': str(error)}
        comparisons['public_and_actions'] = (stock_public, comparison_projection(
            second_view, stock=False, extended_direct=True))
        runs.append({'seed': row['seed'], 'initial': before, 'after': after,
                     'initial_comparisons': initial_comparisons,
                     'comparisons': {k: {'stock': v[0], 'native': v[1], 'equal': v[0] == v[1]}
                                     for k, v in comparisons.items()},
                     'scope': 'ENUMERATED_SECOND_ENTRY_FIELDS_INITIAL_NOT_FULLY_QUALIFIED'})
    result = {'native_source_sha256': native.NATIVE_SOURCE_SHA256,
              'capture_sha256': hashlib.sha256(args.capture.read_bytes()).hexdigest(), 'runs': runs,
              'source_runs': len(source_runs), 'selected_seeds': [r['seed'] for r in selected_runs],
              'excluded_seeds': [r['seed'] for r in source_runs if r not in selected_runs],
              'source_file_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps([{'seed': r['seed'], 'first_difference': r.get('first_difference'),
                      'unequal': [k for k, v in r['comparisons'].items()
                                                    if not v['equal']]} for r in runs]))


if __name__ == '__main__':
    main()
