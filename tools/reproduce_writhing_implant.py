"""Short CPU-only comparison of permanent deck timing, without policy inference."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import zipfile
from collections import Counter
from pathlib import Path

from sls.audit.stock_clock import verify_sealed_oracle
from sls.content.normalize import (
    normalize_card_id,
    normalize_content_id,
    normalize_potion_id,
    normalize_relic_counter,
)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', type=Path, required=True)
    parser.add_argument('--oracle-build', type=Path, required=True)
    parser.add_argument('--manifest', type=Path,
                        default=Path('native/oracle/resources/spirecomm/parity/fullrun-writhing-implant-r1.json'))
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    data = json.loads(args.capture.read_text())
    launch = json.loads(args.capture.with_suffix('.launch.json').read_text())
    build = json.loads(args.oracle_build.read_text())
    jar = args.oracle_build.with_name(args.oracle_build.name.removesuffix('.build.json') + '.jar')
    verify_sealed_oracle(jar, build)
    if (not data.get('execution_complete') or data.get('execution_error')
            or launch.get('mode') != 'validation' or launch.get('recovery_status') != 'RECOVERED'
            or launch['completion']['exit_code'] != 0 or launch['oracle_sha256'] != build['output_sha256']
            or data['stock_jar_sha256'] != build['dependencies']['game']):
        raise ValueError('stock execution or source identity failure')
    from sls.backends.simulator import native
    from sls.rl.training_contract import native_source_digest

    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError('stale native binary')
    root = Path(__file__).resolve().parents[1]
    fixture_path = root / 'tests/fixtures/regressions/act2-smoke-victory-heal-131100063.json'
    template = json.loads(fixture_path.read_text())['before']
    enum = (root / 'native/simulator/include/constants/Cards.h').read_text().split(
        'enum class CardId : std::uint16_t {', 1)[1].split('};', 1)[0]
    ids, value = {}, 0
    for part in re.sub(r'//[^\n]*', '', enum).split(','):
        if not part.strip():
            continue
        match = re.fullmatch(r'([A-Z][A-Z0-9_]*)(?:\s*=\s*(0x[0-9A-Fa-f]+|\d+))?', part.strip())
        if match is None:
            raise ValueError('unsupported native card enum')
        if match[2] is not None:
            value = int(match[2], 0)
        ids[match[1]], value = value, value + 1
    output = []
    for row in data['runs']:
        scene = row['scene']
        resource = 'spirecomm/parity/' + args.manifest.name
        with zipfile.ZipFile(jar) as archive:
            manifest = json.loads(archive.read(resource))
        if scene not in manifest['scenes'] or row['seed'] not in scene['seeds']:
            raise ValueError('undeclared scene or seed')
        if data['scene_manifest_sha256'] != build['members'][resource]:
            raise ValueError('scene source identity mismatch')
        if hashlib.sha256(args.manifest.read_bytes()).hexdigest() != build['members'][resource]:
            raise ValueError('manifest differs from sealed original scene')
        if (scene['encounter'], scene['ascension'], scene['act'], scene['floor']) != ('WRITHING_MASS', 20, 3, 40):
            raise ValueError('unexpected scene context')
        if row['actions'] != scene['actions'] or any(a['kind'] not in {'end_turn', 'play'} for a in row['actions']):
            raise ValueError('unexpected or missing semantic actions')
        run = native.LightspeedRunState()
        run.reset(row['seed'], 20)
        initial = run.snapshot()
        rarity = initial['progress_state']['card_rarity_factor']
        initial['run_state'].update(act=3, floor=40)
        initial['derived_rng']['map']['act'] = 3
        initial['progress_state'] = copy.deepcopy(template['progress_state'])
        initial['progress_state']['card_rarity_factor'] = rarity
        stock_initial_rewards = row['boundaries'][0].get('_stock_reward_state', {})
        if 'card_rarity_factor' in stock_initial_rewards:
            initial['progress_state']['card_rarity_factor'] = stock_initial_rewards['card_rarity_factor']
            initial['progress_state']['potion_chance'] = stock_initial_rewards['potion_modifier']
        initial['screen_info'] = copy.deepcopy(template['screen_info'])
        initial['rng'] = copy.deepcopy(row['before']['_rng'])
        setup = scene['initial']
        initial['player_state'].update(current_hp=setup['hp'], max_hp=setup['max_hp'])
        # Neow can upgrade, add or remove a card before the controlled encounter.
        # Pair only the independently-read initial deck, never the post-Implant deck.
        stock_initial = row['boundaries'][0]['_stock_direct']['master_deck']
        public_initial = row['boundaries'][0]['game_state']['deck']
        if (len(stock_initial) != len(public_initial) or any(
                (c['id'], c['upgrades']) != (p['id'], p['upgrades'])
                for c, p in zip(stock_initial, public_initial))):
            raise ValueError('direct and public initial deck identity mismatch')
        if any(c['upgrades'] > 1 or p['special_data'] != 0 for c, p in zip(stock_initial, public_initial)):
            raise ValueError('initial card needs additional growth/bottle evidence')
        initial['player_state']['deck'] = [
            {'id': ids[normalize_card_id(c['id'])], 'upgraded': c['upgrades'] > 0, 'misc': 0}
            for c in stock_initial]
        battle = native.LightspeedBattle()
        battle.reset_encounter_probe(row['seed'], 'WRITHING_MASS', initial['rng'], 20, 3, 40, scene['id'])
        battle.set_player_health(setup['hp'], setup['max_hp'])
        battle.set_card_piles(setup['hand'], setup['draw'], [], [])
        battle.set_potions([])
        state = battle.snapshot()
        if 'relics' in setup:
            prototype = native.LightspeedBattle()
            names = [normalize_content_id(name) for name in setup['relics']]
            prototype.reset(row['seed'], 'WRITHING_MASS', 20, relics=names, replace_relics=True)
            own = state['game_state']['combat_state']['player']['_internal']
            source = prototype.snapshot()['game_state']['combat_state']['player']['_internal']
            for key in ('relic_bits0', 'relic_bits1'):
                own[key] = source[key]
            body = (root / 'native/simulator/include/constants/Relics.h').read_text().split(
                'enum class RelicId : std::uint8_t {', 1)[1].split('};', 1)[0]
            relic_ids, index = {}, 0
            for part in re.sub(r'//[^\n]*', '', body).split(','):
                if not part.strip():
                    continue
                match = re.fullmatch(r'([A-Z][A-Z0-9_]*)(?:\s*=\s*(0x[0-9A-Fa-f]+|\d+))?', part.strip())
                if match is None:
                    raise ValueError('unsupported relic enum')
                if match[2] is not None:
                    index = int(match[2], 0)
                relic_ids[match[1]], index = index, index + 1
            relics = row['boundaries'][0]['_stock_direct']['relics']
            initial['player_state']['relics'] = [
                {'id': relic_ids[normalize_content_id(r['id'])], 'data': max(0, r['counter'])} for r in relics]
            for r in relics:
                if normalize_content_id(r['id']) == 'OMAMORI' and r['counter'] == 0:
                    bit = relic_ids['OMAMORI']
                    own['relic_bits0' if bit < 64 else 'relic_bits1'] &= ~(1 << (bit if bit < 64 else bit - 64))
        state['game_state']['combat_state']['monsters'][0]['move_id'] = 'WRITHING_MASS_IMPLANT'
        if 'WRITHING_MASS' in setup.get('monster_hp', {}):
            state['game_state']['combat_state']['monsters'][0]['current_hp'] = setup['monster_hp']['WRITHING_MASS']
        initial['combat_checkpoint'] = {'game_state': state['game_state'], 'rng': state['_rng']}
        run.load_state(initial)
        boundaries = []
        for index, stock in enumerate(row['boundaries']):
            direct = stock['_stock_direct']
            if (direct.get('master_deck_evidence_schema') != 'sls-stock-master-deck-v1'
                    or direct.get('dungeon_id') != 'TheBeyond' or direct['ascension'] != 20):
                raise ValueError('missing independent master-deck/context witness')
            if index:
                action = row['actions'][index - 1]
                bits = (2147483648 if action['kind'] == 'end_turn'
                        else (action['card_index'] - 1) | (action.get('target_index', 0) << 16))
                if not any(a['bits'] == bits for a in run.legal_actions()):
                    raise ValueError('semantic action unavailable')
                run.step(bits)
                if index == len(row['boundaries']) - 1 and 'reward_boundary' in row:
                    stock = row['reward_boundary']
                    direct = stock['_stock_direct']
            current = run.snapshot()
            # The checkpoint's outer player_state is the between-room state;
            # active combat owns the current HP and other live resources.
            live_player = (current['combat_checkpoint']['game_state']['combat_state']['player']
                           if 'combat_checkpoint' in current else current['player_state'])
            stock_deck = Counter(normalize_card_id(c['id']) for c in direct['master_deck'])
            native_deck = Counter(c['id'] for c in current['public_inventory']['deck'])
            if index == 0 and stock_deck != native_deck:
                raise ValueError('initial permanent decks differ')
            comparisons = {
                'hp': (direct['player']['current_hp'], live_player['current_hp']),
                'max_hp': (direct['player']['max_hp'], live_player['max_hp']),
                'gold': (stock['game_state']['gold'], live_player.get('_internal', {}).get(
                    'gold', current['player_state']['gold'])),
                'rng': (stock['_rng'], {**current['rng'], **current.get('combat_checkpoint', {}).get('rng', {})}),
            }
            for relic in direct['relics']:
                name = normalize_content_id(relic['id'])
                if name in {'OMAMORI', 'DU_VU_DOLL'}:
                    value = next(r for r in current['public_inventory']['relics'] if r['content_id'] == name)
                    comparisons[name] = (normalize_relic_counter(relic['counter']), value['counter'])
            if index == len(row['boundaries']) - 1 and 'reward_boundary' in row:
                rewards = stock['_stock_reward_state']['screen_rewards']
                screen = current['public_screen']
                comparisons.update({
                    'reward_gold': ([r['gold'] + r['bonus_gold'] for r in rewards if r['type'] == 'GOLD'],
                                    screen.get('gold')),
                    'reward_potions': ([normalize_potion_id(r['potion']) for r in rewards if r['type'] == 'POTION'],
                                       screen.get('potions')),
                    'reward_relics': ([normalize_content_id(r['relic']) for r in rewards if r['type'] == 'RELIC'],
                                      screen.get('relics')),
                    'reward_cards': ([[{'id': normalize_card_id(c['id']), 'upgrades': c['upgrades'], 'misc': c['misc']}
                                      for c in r['cards']] for r in rewards if r['type'] == 'CARD'],
                                     [[{'id': c['id'], 'upgrades': c['upgrades'], 'misc': c['special_data']}
                                       for c in cards] for cards in screen.get('card_rewards', [])]),
                    'potion_modifier': (stock['_stock_reward_state']['potion_modifier'],
                                        current['progress_state']['potion_chance']),
                    'card_rarity_factor': (stock['_stock_reward_state']['card_rarity_factor'],
                                           current['progress_state']['card_rarity_factor']),
                })
            boundaries.append({'boundary': index, 'stock_deck_counts': dict(stock_deck),
                               'native_deck_counts': dict(native_deck), 'equal': stock_deck == native_deck,
                               'resource_comparisons': {k: {'stock': p[0], 'native': p[1], 'equal': p[0] == p[1]}
                                                        for k, p in comparisons.items()},
                               'native_state': current})
        output.append({'seed': row['seed'], 'initial': initial, 'boundaries': boundaries,
                       'first_difference': next((b['boundary'] for b in boundaries if not b['equal']
                                                 or any(not c['equal'] for c in b['resource_comparisons'].values())),
                                                None)})
    result = {'schema': 'sls-writhing-implant-deck-timing-v2', 'runs': output,
              'native_source_sha256': native.NATIVE_SOURCE_SHA256,
              'capture_sha256': hashlib.sha256(args.capture.read_bytes()).hexdigest(),
              'oracle_sha256': build['output_sha256'],
              'fixture_sha256': hashlib.sha256(fixture_path.read_bytes()).hexdigest(),
              'scope': 'CONTROLLED_DECK_AND_ENUMERATED_RESOURCE_FIELDS_NOT_FULL_COMBAT_CERTIFICATE'}
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps([{'seed': r['seed'], 'first_difference': r['first_difference']} for r in output]))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
