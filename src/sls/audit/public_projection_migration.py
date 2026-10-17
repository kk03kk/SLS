"""Reproject an immutable stock message; never drop a material comparison."""
from __future__ import annotations

import hashlib
import json
from dataclasses import replace

from sls.audit.decision_identity import canonical_projection
from sls.backends.original.adapter import adapt_original
from sls.content.selection import PROJECTION_REVISION


def stock_observation(observation,actions,terminal,raw):
    game=raw.get('game_state',{})
    state=game.get('screen_state',{})
    if (game.get('screen_type') not in ('GRID','HAND_SELECT')
            and not (game.get('combat_state') and observation.get('choice_options'))):
        return observation,None
    current_object=adapt_original(raw,allow_key_acquisition=False).decision.observation
    current=current_object.to_dict()
    requirement=[e for e in current_object.choice_options if e.instance_id=='selection-requirement']
    if not requirement:
        return observation,None
    if any(e['instance_id']=='selection-requirement' for e in observation.get('choice_options') or ()):
        if canonical_projection(current,actions,terminal)!=canonical_projection(observation,actions,terminal):
            raise ValueError('current stock public projection differs')
        return current,None
    if len(requirement)!=1:
        raise ValueError('stock quantity not independently reprojected')
    legacy=replace(current_object,choice_options=tuple(e for e in current_object.choice_options
                                                     if e.instance_id!='selection-requirement')).to_dict()
    if canonical_projection(legacy,actions,terminal)!=canonical_projection(observation,actions,terminal):
        raise ValueError('unapproved historical public projection change')
    return current,{'contract':PROJECTION_REVISION,'source':'IMMUTABLE_ORIGINAL_PUBLIC_SELECTION',
        'required_quantity':dict(requirement[0].properties)['amount'],
        'wire_num_cards':state.get('num_cards'),'legacy_all_other_fields_equal':True,
        'wire_max_cards':state.get('max_cards'),
        'old_observation_preserved_in_raw_evidence':True,
        'stock_message_sha256':hashlib.sha256(json.dumps(raw,sort_keys=True).encode()).hexdigest()}
