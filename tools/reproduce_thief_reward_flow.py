"""Bounded CPU-only reward repro; controlled initial state, no model or natural-run claim."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import zipfile
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--oracle-build', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    capture = json.loads(args.capture.read_text())
    launch = json.loads(args.capture.with_suffix('.launch.json').read_text())
    from sls.audit.stock_clock import verify_sealed_oracle

    build = json.loads(args.oracle_build.read_text())
    oracle = args.oracle_build.with_name(args.oracle_build.name.removesuffix('.build.json') + '.jar')
    verify_sealed_oracle(oracle, build)
    manifest_digest = hashlib.sha256(args.manifest.read_bytes()).hexdigest()
    manifest = json.loads(args.manifest.read_text())
    resource = 'spirecomm/parity/' + args.manifest.name
    with zipfile.ZipFile(oracle) as archive:
        if hashlib.sha256(archive.read(resource)).hexdigest() != manifest_digest:
            raise ValueError('manifest differs from sealed Oracle resource')
    if (launch.get('oracle_sha256') != build['output_sha256']
            or capture.get('scene_manifest_sha256') != manifest_digest
            or capture.get('stock_jar_sha256') != build['dependencies']['game']
            or manifest.get('stock_jar_sha256') != build['dependencies']['game']):
        raise ValueError('capture/build/manifest source identity mismatch')
    if (not capture.get('execution_complete') or capture.get('execution_error')
            or launch.get('recovery_status') != 'RECOVERED'
            or launch.get('mode') != 'validation' or launch['completion']['exit_code'] != 0):
        raise ValueError('incomplete/unrecovered stock capture')
    from sls.backends.simulator import native
    from sls.content.normalize import normalize_content_id, normalize_potion_id
    from sls.rl.training_contract import native_source_digest

    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError('stale native artifact')
    root = Path(__file__).resolve().parents[1]
    fixture_path = root / 'tests/fixtures/regressions/act2-smoke-victory-heal-131100063.json'
    fixture = json.loads(fixture_path.read_text())['before']
    results = []
    for stock in capture['runs']:
        scene = stock['scene']
        if scene not in manifest['scenes'] or stock['seed'] not in scene['seeds']:
            raise ValueError('undeclared scene or seed')
        if scene.get('actual_dungeon_required'):
            context = stock['boundaries'][0].get('_parity_scenario', {})
            if context.get('actual_dungeon') != 'TheCity' or context.get('act') != 2:
                raise ValueError('missing actual Act2 dungeon witness')
        if scene['encounter'] != 'TWO_THIEVES' or scene['ascension'] != 20:
            raise ValueError('unsupported scene')
        run = native.LightspeedRunState()
        run.reset(stock['seed'], 20)
        initial = run.snapshot()
        initial['run_state']['act'] = 2
        initial['run_state']['floor'] = 20
        initial['derived_rng']['map']['act'] = 2
        fresh_card_rarity_factor = initial['progress_state']['card_rarity_factor']
        # Reuse only the existing full-run COMBAT continuation representation.
        # Player inventory/deck and seed belong to this fresh controlled run.
        initial['progress_state'] = copy.deepcopy(fixture['progress_state'])
        # The borrowed continuation must not import its historical card pity.
        # These captures enter the first combat of a fresh run, before any
        # combat-card reward; retain this run's initialized rarity factor.
        initial['progress_state']['card_rarity_factor'] = fresh_card_rarity_factor
        initial['progress_state']['potion_chance'] = scene['initial']['potion_modifier']
        initial['screen_info'] = copy.deepcopy(fixture['screen_info'])
        initial['rng'] = copy.deepcopy(stock['before']['_rng'])
        initial['player_state']['current_hp'] = scene['initial']['hp']
        initial['player_state']['max_hp'] = scene['initial']['max_hp']
        battle = native.LightspeedBattle()
        battle.reset_encounter_probe(stock['seed'], 'TWO_THIEVES', initial['rng'],
                                     ascension=20, act=2, floor=20, scenario_id=scene['id'])
        battle.set_player_health(scene['initial']['hp'], scene['initial']['max_hp'])
        battle.set_card_piles(scene['initial']['hand'], scene['initial']['draw'], [], [])
        battle.set_potions([])
        snapshot = battle.snapshot()
        controlled = scene['initial']
        if 'relics' in controlled:
            prototype = native.LightspeedBattle()
            names = [normalize_content_id(name) for name in controlled['relics']]
            prototype.reset(stock['seed'], 'TWO_THIEVES', 20, relics=names, replace_relics=True)
            relic_state = prototype.snapshot()['game_state']
            snapshot['game_state']['relics'] = relic_state['relics']
            player_internal = snapshot['game_state']['combat_state']['player']['_internal']
            for key in ('relic_bits0', 'relic_bits1'):
                player_internal[key] = relic_state['combat_state']['player']['_internal'][key]
            body = (root / 'native/simulator/include/constants/Relics.h').read_text().split(
                'enum class RelicId : std::uint8_t {', 1)[1].split('};', 1)[0]
            ids, value = {}, 0
            for part in re.sub(r'//[^\n]*', '', body).split(','):
                if not part.strip():
                    continue
                match = re.fullmatch(r'([A-Z][A-Z0-9_]*)(?:\s*=\s*(0x[0-9A-Fa-f]+|\d+))?', part.strip())
                if match is None:
                    raise ValueError('unsupported native relic enum')
                if match[2] is not None:
                    value = int(match[2], 0)
                ids[match[1]], value = value, value + 1
            initial['player_state']['relics'] = [{'id': ids[name], 'data': 0} for name in names]
        for monster in snapshot['game_state']['combat_state']['monsters']:
            name = normalize_content_id(monster['id'])
            if name in controlled.get('monster_hp', {}):
                monster['current_hp'] = controlled['monster_hp'][name]
        initial['combat_checkpoint'] = {'game_state': snapshot['game_state'], 'rng': snapshot['_rng']}
        run.load_state(initial)
        trace = []
        scripts = stock['actions']
        if scripts != scene['actions'][:len(scripts)]:
            raise ValueError('stock script is not a declared semantic prefix')
        if len(scripts) != len(scene['actions']) and not scene.get('stop_at_reward_boundary'):
            raise ValueError('stock script incomplete without declared termination condition')
        for boundary, script in enumerate(scripts):
            state = run.snapshot()
            actions = run.legal_actions()
            trace.append({'boundary': boundary, 'state': state, 'actions': actions})
            # search::Action END_TURN=4 shifted29; PLAY_CARD=0 with hand/target indices.
            if script['kind'] == 'end_turn':
                bits = 2147483648
            elif script['kind'] == 'play':
                bits = script['card_index'] - 1 | script['target_index'] << 16
            else:
                raise ValueError('unsupported semantic action')
            if not any(a['bits'] == bits for a in actions):
                raise ValueError('script action unavailable in native')
            run.step(bits)
        final = run.snapshot()
        trace.append({'boundary': len(scripts), 'state': final, 'actions': run.legal_actions()})
        reward = stock['reward_boundary']['_stock_reward_state']
        rewards = reward['screen_rewards']
        screen = final['public_screen']
        comparisons = {
            'potion_modifier': (reward['potion_modifier'], final['progress_state']['potion_chance']),
            'potion_rng': (stock['reward_boundary']['_rng']['potion'], final['rng']['potion']),
            'potions': ([normalize_potion_id(r['potion']) for r in reward['screen_rewards'] if r['type'] == 'POTION'],
                        final['public_screen'].get('potions')),
            'all_rng': (stock['reward_boundary']['_rng'], final['rng']),
            'player_hp': (stock['reward_boundary']['game_state']['current_hp'], final['player_state']['current_hp']),
            'player_gold': (stock['reward_boundary']['game_state']['gold'], final['player_state']['gold']),
            'reward_gold': ([r['gold'] + r['bonus_gold'] for r in rewards if r['type'] == 'GOLD'], screen['gold']),
            'reward_relics': ([normalize_content_id(r['relic']) for r in rewards if r['type'] == 'RELIC'], screen['relics']),
            'reward_cards': ([[{'id': normalize_content_id(c['id']), 'upgrades': c['upgrades'], 'misc': c['misc']}
                               for c in r['cards']] for r in rewards if r['type'] == 'CARD'],
                             [[{'id': c['id'], 'upgrades': c['upgrades'], 'misc': c['special_data']}
                               for c in cards] for cards in screen['card_rewards']]),
        }
        results.append({'seed': stock['seed'], 'initial': initial, 'trace': trace,
                        'comparisons': {key: {'stock': pair[0], 'native': pair[1], 'equal': pair[0] == pair[1]}
                                        for key, pair in comparisons.items()}})
    result = {'schema': 'sls-thief-reward-flow-repro-v2',
              'native_source_sha256': native.NATIVE_SOURCE_SHA256,
              'stock_capture_sha256': hashlib.sha256(args.capture.read_bytes()).hexdigest(),
              'fixture_sha256': hashlib.sha256(fixture_path.read_bytes()).hexdigest(), 'runs': results,
              'oracle_sha256': build['output_sha256'], 'scene_manifest_sha256': manifest_digest,
              'scope': 'CONTROLLED_REWARD_CONTENT_HP_GOLD_AND_ALL_RNG',
              'whole_reward_or_normal_flow_qualified': False,
              'limits': ['controlled initial fixture; not a normal Neow trajectory',
                         'comparison enumerates reward content, HP, gold and RNG; '
                         'not a full combat/reward adapter projection certificate']}
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps([{ 'seed': r['seed'], 'equal': {k: c['equal'] for k, c in r['comparisons'].items()}}
                      for r in results]))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
