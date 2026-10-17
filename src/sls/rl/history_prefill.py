"""Batched current-weight GRU prefill from public, legally replayed histories."""
from __future__ import annotations

import torch

from sls.audit.short_battle import PolicyContext, canonical
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import PolicyBatch
from sls.model.batching import encode_decision
from sls.model.encoding import ACTION_TYPE_IDS


def encoded_history(checkpoint,actions,expected):
    backend=SimulatorBackend(IRONCLAD_A20_ACT2)
    decision=backend.load_checkpoint(checkpoint) if checkpoint is not None else None
    if decision is None:
        raise ValueError('history requires an explicit normal-start checkpoint')
    records=[]
    for serialized in actions:
        action=Action.from_dict(serialized)
        if decision.terminal or action not in decision.actions:
            raise ValueError('illegal public-history action')
        encoded=encode_decision(decision)
        item=backend.step(action)
        records.append((encoded,ACTION_TYPE_IDS[action.kind.value]+1,float(item.reward)))
        decision=item.decision
    if canonical(backend.checkpoint())!=canonical(expected):
        raise ValueError('history replay hidden state differs')
    return records,decision


@torch.no_grad()
def prefill(model,histories,initial=None):
    if model.training:
        raise ValueError('history prefill requires eval mode')
    device=next(model.parameters()).device
    if device.type=='cpu' and len(histories)>1:
        # The recorded CPU actor ran one trajectory at a time. Batched CPU
        # attention/GEMM can accumulate different rounding over long histories.
        # Keep the original calculation geometry and strict witness tolerance;
        # allocated CUDA execution retains batched prefill and its own replay gate.
        return [prefill(model,[history],None if initial is None else [initial[i]])[0]
                for i,history in enumerate(histories)]
    contexts=initial or [PolicyContext(model.initial_memory(1).cpu()) for _ in histories]
    contexts=[PolicyContext(c.memory.clone().to(device),c.previous_action,c.previous_reward,c.episode_start) for c in contexts]
    for step in range(max(map(len,histories),default=0)):
        indices=[i for i,h in enumerate(histories) if len(h)>step]
        batch=PolicyBatch.from_encoded([histories[i][step][0] for i in indices]).to(device)
        output=model(*batch.model_inputs(),memory=torch.cat([contexts[i].memory for i in indices]).to(device),
            previous_action_types=torch.tensor([contexts[i].previous_action for i in indices],device=device),
            previous_rewards=torch.tensor([contexts[i].previous_reward for i in indices],device=device),
            episode_start_mask=torch.tensor([contexts[i].episode_start for i in indices],device=device))
        memory=output.next_memory.detach()
        for position,i in enumerate(indices):
            contexts[i]=PolicyContext(memory[position:position+1],histories[i][step][1],histories[i][step][2],False)
    if contexts:
        memory=torch.cat([c.memory for c in contexts]).cpu()
        contexts=[PolicyContext(memory[i:i+1],c.previous_action,c.previous_reward,c.episode_start)
                  for i,c in enumerate(contexts)]
    return contexts


class RootPrefillCache:
    """Cache public encodings indefinitely, policy memories only per tensor version."""
    def __init__(self,rows,reserve):
        self.rows=rows
        self.reserve=reserve
        self.encodings={}
        self.memories={}
        self.signature=None
        self.native_replay_steps=0

    def root_contexts(self,model,ordinals):
        if model.training:
            raise ValueError('history prefill requires eval mode')
        signature=(tuple((id(p),p._version,str(p.device),str(p.dtype)) for p in model.parameters()),
                   torch.are_deterministic_algorithms_enabled(),torch.get_float32_matmul_precision())
        if signature!=self.signature:
            self.memories.clear()
            self.signature=signature
        missing=list(dict.fromkeys(i for i in ordinals if i not in self.memories))
        for i in missing:
            if i not in self.encodings:
                backend=SimulatorBackend(IRONCLAD_A20_ACT2)
                backend.reset(self.rows[i]['source_seed'])
                normal=canonical(backend.checkpoint())
                del backend
                records,_=encoded_history(normal,self.rows[i]['prefix'],self.rows[i]['checkpoint'])
                self.native_replay_steps+=len(records)
                self.encodings[i]=records
        if missing:
            histories=[self.encodings[i] for i in missing]
            self.reserve(sum(map(len,histories)))
            values=prefill(model,histories)
            self.memories.update(zip(missing,values,strict=True))
        return [self.memories[i] for i in ordinals]

    def contexts(self,model,active,checkpoints):
        roots=self.root_contexts(model,[r['root'] for r in active])
        histories=[]
        for record,expected in zip(active,checkpoints,strict=True):
            row=self.rows[record['root']]
            history,_=encoded_history(row['checkpoint'],record['suffix'],expected)
            self.native_replay_steps+=len(history)
            histories.append(history)
        self.reserve(sum(map(len,histories)))
        return prefill(model,histories,roots)
