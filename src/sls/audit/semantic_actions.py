"""Controlled audit targets by live content occurrence, never policy choice."""
from __future__ import annotations

from typing import Any, Mapping

from sls.content.normalize import normalize_monster_id


def resolve_target(action: Mapping[str, Any], monsters: list[Mapping[str, Any]]) -> dict[str, Any]:
    result = dict(action)
    selector = result.pop('target', None)
    if selector is None:
        return result
    if 'target_index' in result or set(selector) != {'monster_id', 'alive_ordinal'}:
        raise ValueError('ambiguous or incomplete semantic target')
    ordinal = selector['alive_ordinal']
    if type(ordinal) is not int or ordinal < 0:
        raise ValueError('invalid live target ordinal')
    identifier = normalize_monster_id(selector['monster_id'])
    matches = [index for index, monster in enumerate(monsters)
               if normalize_monster_id(monster.get('id', monster.get('content_id'))) == identifier
               and monster['current_hp'] > 0 and not monster.get('is_gone', False)
               and not monster.get('half_dead', False)]
    if ordinal >= len(matches):
        raise ValueError('semantic target does not exist')
    result['target_index'] = matches[ordinal]
    return result
