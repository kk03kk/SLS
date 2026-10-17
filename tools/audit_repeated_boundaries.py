"""Census historical policy-limit terminations and reproduce a legal grid cycle."""
from __future__ import annotations

import argparse
import copy
import json
from collections import Counter
from pathlib import Path

from sls.audit.short_battle import canonical
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model.batching import encode_decision
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.training_contract import native_source_digest, sha256_file


def grid_cycle():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-empty-cage-grid-131100069.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    initial = backend.load_checkpoint(fixture['before'])
    original = canonical(backend.checkpoint())
    encoded = encode_decision(initial)
    limiter = EpisodeLimitState.initial(initial)
    records = []
    action = Action.from_dict(fixture['action'])
    action_bits = backend._candidate_bits[action.candidate_id]
    for i in range(8):
        assert action in initial.actions
        transition = backend.step(action)
        following = encode_decision(transition.decision)
        reason = limiter.observe(transition.decision, max_steps=4096, max_boundary_visits=4)
        same_encoding = encoded.entity_numeric.shape == following.entity_numeric.shape and (
            encoded.entity_numeric == following.entity_numeric).all().item()
        records.append({'click': i+1, 'selected_count':len(transition.decision.observation.selected_cards),
                        'encoding_equals_initial':same_encoding, 'reason':reason,
                        'rng_unchanged':canonical(backend.checkpoint()['rng'])==original['rng'],
                        'deck_unchanged':transition.decision.observation.deck==initial.observation.deck})
    assert records[-1]['reason']=='cycle_limit'
    final = canonical(backend.checkpoint())
    assert final.pop('replay_actions') == original['replay_actions'] + [action_bits] * 8
    expected = dict(original)
    expected.pop('replay_actions')
    assert final == expected  # Only the real executed-action history advanced.
    assert not records[0]['encoding_equals_initial'] and records[1]['encoding_equals_initial']
    other_raw = copy.deepcopy(backend.raw_state)
    assert other_raw['public_screen']['select_count'] == 2
    other_raw['public_screen']['select_count'] = 1
    projected = SimulatorBackend(IRONCLAD_A20_ACT2)._adapt(other_raw)
    return {'classification':'LEGAL_REVERSIBLE_SELECTION_CYCLE_NOT_AN_ILLEGAL_OR_INVISIBLE_ACTION', 'clicks':records,
            'projection_omits_required_selection_count': projected == initial,
            'projection_limit':'Controlled public-projection mutation, not proof that both full native states are naturally reachable with every other field equal.',
            'not_a_proof_of_infinite_loop':'RNN memory can evolve; this validates the explicit finite repeated-boundary rule only.'}


def census(root):
    rows=[]
    for directory in sorted((root/'local/runs').iterdir()):
        for name in ('reference-evaluation.json','endpoint-evaluation.json'):
            p=directory/name
            if not p.is_file():
                continue
            data=json.loads(p.read_text())
            result=data['result']
            cycles=[s for s in result.get('seed_results',[]) if s['reason']=='cycle_limit']
            tails={r['seed']:r['tail'] for r in result.get('failure_traces',[])}
            observed=[]
            for c in cycles:
                tail=tails.get(c['seed'],[])
                if tail:
                    observed.append({'seed':c['seed'], 'last_screen':tail[-1]['screen'],
                        'last_kind':tail[-1]['action']['kind'], 'same_action_last_four':len(tail)>=4 and all(
                            r['action']==tail[-1]['action'] for r in tail[-4:])})
            rows.append({'path':str(p),'sha256':sha256_file(p),'episodes':result['episodes'],
                         'cycles':len(cycles),'floors':dict(Counter(s['floor'] for s in cycles)),
                         'traced_cycles':observed,'unclassified_without_trace':len(cycles)-len(observed),
                         'native':data.get('simulator',{}).get('native_source_sha256')})
    return rows


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    if args.output.exists():
        raise FileExistsError('retain original census')
    root=Path(__file__).resolve().parents[1]
    assignments=[]
    for p in [*sorted((root/'native/simulator/src/game').glob('*.cpp')), root/'native/simulator/python/module.cpp']:
        for line,text in enumerate(p.read_text().splitlines(),1):
            if 'regainControlAction =' in text and not text.strip().startswith('//'):
                assignments.append({'path':p.relative_to(root).as_posix(),'line':line,'text':text.strip()})
    result={'native_source_sha256':native_source_digest(),'historical':census(root),'controlled_grid_cycle':grid_cycle(),
            'callback_assignment_inventory':assignments,
            'limitations':['Historical reports save at most 16 diverse failure tails; untraced causes remain unknown.',
                           'Repeated policies/seeds across files are not independent observations.',
                           'Callback census is an inspection list, not exhaustive behavioral coverage.']}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open('x',encoding='utf-8') as f:
        json.dump(result,f,ensure_ascii=False,indent=2)
    print(json.dumps({'callback_assignments':len(assignments),'grid':result['controlled_grid_cycle']['classification']}))


if __name__=='__main__':
    raise SystemExit(main())
