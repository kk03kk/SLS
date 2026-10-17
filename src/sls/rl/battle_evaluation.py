"""Paired public-policy battle/suffix evaluation, with no policy access to RNG."""
from __future__ import annotations

import math
from collections import Counter

import numpy as np
import torch

from sls.audit.short_battle import canonical
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.rl.battle_outcome import player_escaped
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.history_prefill import RootPrefillCache


@torch.no_grad()
def evaluate_roots(model,rows,*,greedy=True,replicates=1,batch_size=1,suffix=False,
                   zero_memory=False,no_potions=False,max_steps=512,stop_requested=lambda:False):
    model.eval()
    device=next(model.parameters()).device
    prefill_steps=0
    def reserve(n):
        nonlocal prefill_steps
        prefill_steps+=n
    cache=RootPrefillCache(rows,reserve)
    results=[]
    for start in range(0,len(rows),batch_size):
        if stop_requested():
            raise InterruptedError('evaluation stopped; fixed endpoint is preserved')
        ordinals=list(range(start,min(start+batch_size,len(rows))))
        # Build roots before allocating the battle instances: CPU smoke needs
        # at most one live worker plus one sequential restore/prefill instance.
        # Batch public-history inference even when CPU battles must be serial.
        # Warming a context does not create a battle or execute a target action.
        if any(i not in cache.memories for i in ordinals):
            cache.root_contexts(model,range(start,min(start+max(16,batch_size),len(rows))))
        initial=cache.root_contexts(model,ordinals)
        for replicate in range(replicates):
            backends=[SimulatorBackend(IRONCLAD_A20_ACT2) for _ in ordinals]
            decisions=[b.load_checkpoint(canonical(rows[i]['checkpoint'])) for i,b in zip(ordinals,backends,strict=True)]
            contexts=[type(c)(model.initial_memory(1).cpu() if zero_memory else c.memory.clone(),
                      c.previous_action,c.previous_reward,c.episode_start) for c in initial]
            limits=[EpisodeLimitState.initial(d) for d in decisions]
            generators=[torch.Generator().manual_seed(rows[i]['source_seed']+9187*replicate) for i in ordinals]
            active=list(range(len(ordinals)))
            entropy=[[] for _ in ordinals]
            root_values=[None for _ in ordinals]
            action_counts=[Counter() for _ in ordinals]
            end_turn_hp_loss=[0 for _ in ordinals]
            for step in range(max_steps):
                if stop_requested():
                    raise InterruptedError('evaluation stopped; fixed endpoint is preserved')
                batch=PolicyBatch.from_decisions([decisions[i] for i in active]).to(device)
                output=model(*batch.model_inputs(),memory=torch.cat([contexts[i].memory for i in active]).to(device),
                    previous_action_types=torch.tensor([contexts[i].previous_action for i in active],device=device),
                    previous_rewards=torch.tensor([contexts[i].previous_reward for i in active],device=device),
                    episode_start_mask=torch.tensor([contexts[i].episode_start for i in active],device=device))
                logits=output.logits.detach().cpu()
                memory=output.next_memory.detach()
                following=[]
                for position,index in enumerate(active):
                    decision=decisions[index]
                    if step==0:
                        root_values[index]=float(output.value[position])
                    scores=logits[position,:len(decision.actions)].clone()
                    if no_potions:
                        for k,action in enumerate(decision.actions):
                            if action.kind.value=='USE_POTION':
                                scores[k]=-torch.inf
                    probability=scores.softmax(-1)
                    entropy[index].append(float(-(probability*probability.clamp_min(1e-30).log()).sum()))
                    chosen=int(scores.argmax()) if greedy else int(torch.multinomial(probability,1,generator=generators[index]))
                    action=decision.actions[chosen]
                    action_counts[index][action.kind.value]+=1
                    item=backends[index].step(action)
                    if action.kind.value=='END_TURN':
                        end_turn_hp_loss[index]+=max(0,decision.observation.player.current_hp-item.decision.observation.player.current_hp)
                    if item.truncated:
                        raise RuntimeError('evaluation backend truncated')
                    decisions[index]=item.decision
                    context=contexts[index]
                    context.memory=memory[position:position+1]
                    context.previous_action=ACTION_TYPE_IDS[action.kind.value]+1
                    context.previous_reward=float(item.reward)
                    context.episode_start=False
                    done=item.terminated or (not suffix and item.decision.observation.screen.value!='COMBAT')
                    escaped=not suffix and player_escaped(decision,action,item)
                    reason=None if done else limits[index].observe(item.decision,max_steps=max_steps,max_boundary_visits=4)
                    if done or reason:
                        win=not reason and not escaped and item.decision.observation.player.current_hp>0 and not (
                            item.terminated and not item.info.get('success'))
                        results.append({'sample_id':rows[ordinals[index]]['sample_id'],
                            'source_seed':rows[ordinals[index]]['source_seed'],'replicate':replicate,
                            'success':bool(win),'steps':step+1,'reason':reason or ('escape' if escaped else 'success' if win else 'death'),
                            'hp':item.decision.observation.player.current_hp,
                            'potions':len(item.decision.observation.potions),
                            'mean_entropy':float(np.mean(entropy[index])),
                            'action_counts':dict(action_counts[index]),
                            'net_hp_drop_on_end_turn':end_turn_hp_loss[index],
                            'root_value':root_values[index]})
                    else:
                        following.append(index)
                active=following
                if not active:
                    break
            if active:
                raise RuntimeError('evaluation limit did not terminate')
            del backends
    return {'episodes':len(results),'wins':sum(r['success'] for r in results),
            'success_rate':sum(r['success'] for r in results)/len(results),
            'decisions':sum(r['steps'] for r in results),'prefill_decisions':prefill_steps,'results':results}


def paired_result(reference,candidate,*,bootstrap_seed=734017):
    left={r['sample_id']:r for r in reference['results']}
    right={r['sample_id']:r for r in candidate['results']}
    if (not left or set(left)!=set(right) or len(left)!=len(reference['results'])
            or len(right)!=len(candidate['results'])):
        raise ValueError('primary evaluation needs exactly one paired result per source')
    if (len({r['source_seed'] for r in left.values()})!=len(left)
            or any(left[k]['source_seed']!=right[k]['source_seed'] for k in left)):
        raise ValueError('primary evaluation origin pairing mismatch')
    differences=np.array([int(right[k]['success'])-int(left[k]['success']) for k in left])
    n=len(differences)
    b=int((differences==1).sum())
    c=int((differences==-1).sum())
    d=b+c
    p=min(1.,2*sum(math.comb(d,k) for k in range(min(b,c)+1))/2**d) if d else 1.
    probabilities=np.array([(differences==-1).mean(),(differences==0).mean(),(differences==1).mean()])
    draws=np.random.default_rng(bootstrap_seed).multinomial(n,probabilities,size=20000)
    interval=np.quantile((draws[:,2]-draws[:,0])/n,[.025,.975]).tolist()
    gain=float(differences.mean())
    return {'independent_origins':n,'gain':gain,'paired_bootstrap_ci95':interval,
            'mcnemar_exact_two_sided_p':p,'candidate_only_wins':b,'parent_only_wins':c,
            'pass_learnability':n>=384 and gain>=.10 and interval[0]>0 and p<.05,
            'note':'Diagnostic battle benefit only; no whole-Act2 improvement inferred.'}
