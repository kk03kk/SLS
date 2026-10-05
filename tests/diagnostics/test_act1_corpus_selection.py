from collections import Counter

from tools.diagnose_act1_corpus import select_seeds


def test_corpus_selection_is_order_independent_and_has_declared_strata():
    rows = []
    for boss in ("THE_GUARDIAN", "HEXAGHOST", "SLIME_BOSS"):
        for success, floor in ((True, 16), (False, 16), (False, 8)):
            for _ in range(20):
                rows.append({"seed": len(rows), "success": success, "floor": floor,
                             "bosses": {"1": boss}})
    selected = select_seeds(rows)
    assert selected == select_seeds(list(reversed(rows)))
    assert len({r["seed"] for r in selected}) == 100
    assert Counter(r["stratum"] for r in selected) == {"win": 40, "boss_death": 40, "early_death": 20}


def test_sparse_current_cohort_keeps_available_strata_without_duplicate_padding():
    rows = [{"seed": 123, "success": True, "floor": 16, "bosses": {"1": "HEXAGHOST"}}]
    assert select_seeds(rows, allow_shortfall=True) == [{**rows[0], "stratum": "win"}]
