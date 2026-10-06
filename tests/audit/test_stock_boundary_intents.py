from sls.audit.act2_differential import pending_stock_intents


def test_actual_intent_cannot_be_hidden_by_an_attack_move_projection():
    raw = {'_monster_intents': [{'intent': 'ATTACK', 'damage': 8}],
           'game_state': {'combat_state': {'monsters': [
               {'intent': 'DEBUG', 'current_hp': 30},
               {'intent': 'DEBUG', 'current_hp': 0},
               {'intent': 'DEBUG', 'current_hp': 30, 'is_gone': True},
               {'intent': 'DEBUG', 'current_hp': 30, 'half_dead': True},
           ]}}}
    assert pending_stock_intents(raw) == [0]
    raw['game_state']['combat_state']['monsters'][0]['intent'] = 'ATTACK'
    assert pending_stock_intents(raw) == []


def test_terminal_screen_is_not_an_unmaterialized_player_decision():
    raw = {'game_state': {'screen_type': 'GAME_OVER', 'combat_state': {
        'monsters': [{'intent': 'DEBUG', 'current_hp': 30}],
    }}}
    assert pending_stock_intents(raw) == []
