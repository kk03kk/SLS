import copy
from types import SimpleNamespace

import pytest

from sls.rl.evaluate import _act2_elite_entry
from tools.select_act2_canaries import START, select


def scout():
    rows = [{"seed": START + i, "act_entries": {"1": {}, "2": {}},
             "last_context": {"enemy_ids": ["SNAKE_PLANT"]},
             "reason": "DEATH", "entered_bosses": [], "act2_elite_entries": {},
             "act2_entry_diagnostics_contract": "sls-act2-map-room-coverage-v1"} for i in range(128)]
    rows[1]["last_context"]["enemy_ids"] = ["BOOK_OF_STABBING"]
    rows[1]['act2_elite_entries'] = {'23': ['BOOK_OF_STABBING']}
    rows[3]["last_context"]["enemy_ids"] = ["GREMLIN_LEADER"]
    rows[3]['act2_elite_entries'] = {'24': ['GREMLIN_LEADER']}
    for index, boss in ((5, "CHAMP"), (9, "AUTOMATON"), (69, "COLLECTOR")):
        rows[index]["entered_bosses"] = [f"ACT_2:{boss}"]
        rows[index]["last_context"]["enemy_ids"] = [{
            "CHAMP": "THE_CHAMP", "AUTOMATON": "BRONZE_AUTOMATON", "COLLECTOR": "THE_COLLECTOR",
        }[boss]]
    return {"profile": "IRONCLAD_A20_ACT2", "result": {"seed_results": rows}}


def test_selects_first_disjoint_seeds_for_each_coverage_role():
    assert [r["seed"] for r in select(scout())] == [START + i for i in (0, 1, 2, 3, 4, 5, 9, 69)]


def test_missing_boss_entry_is_not_replaced_by_preselected_boss_identity():
    data = copy.deepcopy(scout())
    data["result"]["seed_results"][69]["entered_bosses"] = []
    with pytest.raises(ValueError, match="missing actual entry"):
        select(data)


def test_rejects_non_diagnostic_seed_range():
    data = scout()
    data["result"]["seed_results"][0]["seed"] = 9000000000000
    with pytest.raises(ValueError, match="fixed diagnostic range"):
        select(data)


def test_ordinary_slaver_does_not_count_as_elite_and_entry_can_survive():
    data = scout()
    rows = data['result']['seed_results']
    rows[0]['last_context']['enemy_ids'] = ['RED_SLAVER', 'ACID_SLIME_M']
    rows[1]['last_context']['enemy_ids'] = ['CHAMP']
    selected = select(data)
    assert next(r for r in selected if r['seed'] == START)['role'] == 'ACT2_ORDINARY_FAILURE'
    assert next(r for r in selected if r['seed'] == START + 1)['role'] == 'ACT2_ELITE_ENTRY'


def test_missing_entry_diagnostics_requires_new_scan():
    data = scout()
    del data['result']['seed_results'][0]['act2_elite_entries']
    with pytest.raises(ValueError, match='rescan'):
        select(data)


def test_colosseum_taskmaster_is_not_a_map_elite_entry():
    observation = SimpleNamespace(run=SimpleNamespace(act=2),
                                  screen=SimpleNamespace(value="COMBAT"),
                                  enemies=[SimpleNamespace(monster_id="TASKMASTER")])
    assert _act2_elite_entry(observation, (2, "EVENT")) == []
    assert _act2_elite_entry(observation, (1, "ELITE")) == []
    assert _act2_elite_entry(observation, None) == []
    assert _act2_elite_entry(observation, (2, "ELITE")) == ["TASKMASTER"]
    assert _act2_elite_entry(observation, (2, "BURNING_ELITE")) == ["TASKMASTER"]


@pytest.mark.parametrize('monster', ['THE_CHAMP', 'THE_COLLECTOR', 'BRONZE_AUTOMATON'])
def test_actual_boss_monster_ids_cannot_be_ordinary_failures(monster):
    data = scout()
    data['result']['seed_results'][0]['last_context']['enemy_ids'] = [monster]
    selected = select(data)
    assert all(r['seed'] != START for r in selected)
    assert sum(r['role'] == 'ACT2_ORDINARY_FAILURE' for r in selected) == 3
