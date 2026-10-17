"""A task adapter: natural root sampling, current-policy history, battle terminal.

Only the existing PPO collector interface is extended. PPO loss/GAE/optimizer
and production reward/configuration remain shared and unchanged.
"""
from __future__ import annotations

import copy
from dataclasses import replace

from sls.audit.short_battle import canonical
from sls.rl.battle_bank import load_bank
from sls.rl.battle_outcome import player_escaped
from sls.rl.history_prefill import RootPrefillCache
from sls.rl.training_contract import sha256_file


class BattleCurriculum:
    def __init__(self,directory,*,prefix_budget=8_000_000):
        from pathlib import Path
        self.directory=Path(directory)
        self.rows,manifest=load_bank(self.directory,'fit')
        self.contract={'schema':'sls-history-conditioned-battle-task-v1',
            'bank_sha256':sha256_file(self.directory/'manifest.json'),
            'fit_sha256':manifest['files']['fit.json.gz'],'terminal':'CURRENT_BATTLE_WIN_LOSS',
            'reward':'WIN_1_LOSS_MINUS_1_INTERMEDIATE_0','policy_history':'CURRENT_WEIGHTS_PUBLIC_PREFIX',
            'player_escape':'LOSS_NOT_VICTORY',
            'prefix_budget':prefix_budget}
        self.started=0
        self.active={}
        self.outcomes={}
        self.prefix_decisions=0
        self.prefill_cache=RootPrefillCache(self.rows,self._reserve_prefix)

    def _reserve_prefix(self,count):
        if self.prefix_decisions+count>self.contract['prefix_budget']:
            raise RuntimeError('current-policy prefix inference budget exhausted')
        self.prefix_decisions+=count

    def apply(self,workers,indices,seeds,decisions):
        result=[]
        for index in indices:
            # Deterministic round robin covers every root; no outcome priority.
            ordinal=self.started%len(self.rows)
            self.started+=1
            self.active[str(index)]={'root':ordinal,'suffix':[]}
            result.append(workers.load_one(index,canonical(self.rows[ordinal]['checkpoint'])))
        return result

    def interaction_contexts(self,model,workers,indices,decisions):
        indices=list(indices)
        if not indices:
            return []
        model.eval()
        checkpoints=[workers.checkpoint_one(i) for i in indices]
        return self.prefill_cache.contexts(model,[self.active[str(i)] for i in indices],
                                          checkpoints)

    def observe_transitions(self,decisions,candidates,transitions):
        result=[]
        for index,(decision,candidate,item) in enumerate(zip(decisions,candidates,transitions,strict=True)):
            if item.truncated:
                raise RuntimeError('battle task backend truncated')
            action=next(a for a in decision.actions if a.candidate_id==candidate)
            self.active[str(index)]['suffix'].append(action.to_dict())
            done=item.terminated or item.decision.observation.screen.value!='COMBAT'
            escaped=player_escaped(decision,action,item)
            win=done and not escaped and item.decision.observation.player.current_hp>0 and not (
                item.terminated and not item.info.get('success'))
            if done:
                item=replace(item,decision=replace(item.decision,terminal=True,actions=()),
                    terminated=True,reward=1. if win else -1.,info={**item.info,'success':bool(win),
                        'research_task':'CURRENT_BATTLE','reason':'ESCAPE' if escaped else 'BATTLE_WIN' if win else 'DEATH'})
                if escaped:
                    self.outcomes['escape']=self.outcomes.get('escape',0)+1
            result.append(item)
        return result

    def note_terminal(self,index,reason):
        self.outcomes[reason]=self.outcomes.get(reason,0)+1

    def state_dict(self):
        return copy.deepcopy({'started':self.started,'active':self.active,'outcomes':self.outcomes,
                              'prefix_decisions':self.prefix_decisions})

    def load_state_dict(self,state):
        if set(state)!={'started','active','outcomes','prefix_decisions'}:
            raise ValueError('battle curriculum state contract mismatch')
        if any(type(state[k]) is not int or state[k]<0 for k in ('started','prefix_decisions')):
            raise ValueError('invalid battle continuation counters')
        for value in state['active'].values():
            if not 0<=value['root']<len(self.rows) or not isinstance(value['suffix'],list):
                raise ValueError('invalid battle root/history')
        self.started=state['started']
        self.active=copy.deepcopy(state['active'])
        self.outcomes=dict(state['outcomes'])
        self.prefix_decisions=state['prefix_decisions']
        self.prefill_cache=RootPrefillCache(self.rows,self._reserve_prefix)
