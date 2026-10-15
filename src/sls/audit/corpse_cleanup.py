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


def classify_controlled_corpse_cleanup(stock, native, raw_stock):
    """Witnessed corpse cases only, with independent death and action proof."""
    try:
        original = raw_stock['_stock_direct']['monsters']
        slots = None
        projected = copy.deepcopy(stock)
        if (original[0]['id'] == 'SpireShield'
                and original[0]['dying'] is True and original[0]['current_hp'] == 0
                and native['direct']['monsters'][0]['current_hp'] == 0
                and stock['direct']['monsters'][0]['powers'] == [
                    {'id': 'ARTIFACT', 'amount': 2}, {'id': 'BACK_ATTACK', 'amount': 1}]
                and native['direct']['monsters'][0]['powers'] == [{'id': 'ARTIFACT', 'amount': 2}]):
            projected['direct']['monsters'][0]['powers'] = [{'id': 'ARTIFACT', 'amount': 2}]
            slots = {0}
        elif ((len(original) == 5 and original[2]['id'] == 'Reptomancer')
              or (len(original) == 7 and original[3]['id'] == 'Reptomancer')):
            candidates = {0, 1, 3, 4} if len(original) == 5 else {0, 1, 2, 4, 5, 6}
            slots = set()
            for slot in candidates:
                if stock['direct']['monsters'][slot]['powers'] == native['direct']['monsters'][slot]['powers']:
                    continue
                if (original[slot]['id'] != 'Dagger' or original[slot]['dying'] is not True
                        or original[slot]['current_hp'] != 0
                        or native['direct']['monsters'][slot]['current_hp'] != 0):
                    return None
                if (stock['direct']['monsters'][slot]['powers'] != []
                        or native['direct']['monsters'][slot]['powers'] != [{'id': 'MINION', 'amount': 1}]):
                    return None
                projected['direct']['monsters'][slot]['powers'] = native['direct']['monsters'][slot]['powers']
                slots.add(slot)
        elif (len(original) in (3, 4) and original[-1]['id'] == 'Exploder'
              and original[-1]['dying'] is True and original[-1]['current_hp'] == 0
              and native['direct']['monsters'][-1]['current_hp'] == 0
              and stock['direct']['monsters'][-1]['powers'] == [{'id': 'EXPLOSIVE', 'amount': 1}]
              and native['direct']['monsters'][-1]['powers'] == []):
            projected['direct']['monsters'][-1]['powers'] = []
            slots = {len(original) - 1}
        if slots is None or any(json.loads(a).get('target_index') in slots for a in native['actual_actions']):
            return None
        if projected != native:
            return None
        return 'WITNESSED_DEAD_POWER_ANIMATION_DIFFERENCE_NOT_EXACT_STATE_EQUALITY'
    except (KeyError, IndexError, TypeError, ValueError):
        return None
