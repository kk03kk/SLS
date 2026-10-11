"""Narrow classification of a witnessed animation-only Deca power cleanup.

Retain the raw difference. This never changes simulator state or reports exact
state equality, and cannot authorize a different enemy/power or a live target.
"""

import copy
import json


def classify_deca_artifact_cleanup(stock, native):
    try:
        original = stock['direct']['monsters'][0]
        simulated = native['direct']['monsters'][0]
        if (original['id'] != 'DECA' or simulated['id'] != 'DECA'
                or original['current_hp'] > 0 or simulated['current_hp'] > 0
                or original['powers'] != []
                or simulated['powers'] != [{'id': 'ARTIFACT', 'amount': 3}]):
            return None
        if any(json.loads(action).get('target_index') == 0 for action in native['actual_actions']):
            return None
        projected = copy.deepcopy(native)
        projected['direct']['monsters'][0]['powers'] = []
        if projected != stock:
            return None
    except (KeyError, IndexError, TypeError, ValueError):
        return None
    return 'WITNESSED_DEAD_DECA_ARTIFACT_ANIMATION_CLEANUP_NOT_EXACT_STATE_EQUALITY'


def classify_cultist_ritual_cleanup(stock, native):
    """Only the witnessed dead second Cultist's Ritual5; retain raw inequality."""
    try:
        original = stock['direct']['monsters'][1]
        simulated = native['direct']['monsters'][1]
        if (original['id'] != 'CULTIST' or simulated['id'] != 'CULTIST'
                or original['current_hp'] > 0 or simulated['current_hp'] > 0
                or original['powers'] != []
                or simulated['powers'] != [{'id': 'RITUAL', 'amount': 5}]):
            return None
        if any(json.loads(action).get('target_index') == 1 for action in native['actual_actions']):
            return None
        projected = copy.deepcopy(native)
        projected['direct']['monsters'][1]['powers'] = []
        if projected != stock:
            return None
    except (KeyError, IndexError, TypeError, ValueError):
        return None
    return 'WITNESSED_DEAD_CULTIST_RITUAL_ANIMATION_CLEANUP_NOT_EXACT_STATE_EQUALITY'
