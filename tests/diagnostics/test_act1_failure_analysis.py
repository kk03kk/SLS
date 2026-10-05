from tools.analyze_act1_failures import analyze


def _row(seed, *, success, floor, boss, enemies=(), entered=False):
    return {
        "seed": seed,
        "success": success,
        "reason": "SUCCESS" if success else "DEATH",
        "floor": floor,
        "bosses": {"1": boss},
        "entered_bosses": [boss] if entered else [],
        "last_context": {"enemy_ids": list(enemies)},
    }


def test_failure_analysis_separates_boss_early_and_elite_deaths() -> None:
    rows = [
        _row(1, success=True, floor=16, boss="HEXAGHOST", entered=True),
        _row(2, success=False, floor=16, boss="HEXAGHOST", entered=True),
        _row(3, success=False, floor=10, boss="SLIME_BOSS", enemies=("GREMLIN_NOB",)),
        _row(4, success=False, floor=6, boss="SLIME_BOSS", enemies=("CULTIST",)),
    ]
    report = analyze({
        "result": {
            "episodes": 4,
            "successes": 1,
            "seed_results": rows,
            "death_floor_distribution": {"6": 1, "10": 1, "16": 1},
            "boss_action_metrics": {},
        },
    })
    assert report["outcomes"] == {
        "boss_death": 1, "preboss_death": 2, "win": 1,
    }
    assert report["bosses"]["HEXAGHOST"]["boss_entries"] == 2
    assert report["bosses"]["SLIME_BOSS"]["preboss_deaths"] == 2
    assert report["preboss_elite_deaths"] == 1
    assert report["preboss_elite_death_share"] == 0.5


def test_failure_analysis_does_not_mislabel_runtime_failure_as_death() -> None:
    row = _row(1, success=False, floor=16, boss="HEXAGHOST", entered=True)
    row["reason"] = "step_limit"
    report = analyze({"result": {
        "episodes": 1, "successes": 0, "seed_results": [row],
        "step_limits": 1,
    }})
    assert report["outcomes"] == {"operational_failure": 1}
    assert report["bosses"]["HEXAGHOST"]["boss_deaths"] == 0
    assert report["runtime_quality"]["step_limits"] == 1
