# Act1–2 analysis contract correction, 2026-10-04

The previous generic analyzer assumed that every Act1 boss success was a win
of the whole evaluation horizon. This was correct for Act1-only evaluations,
but contradicted `rl/evaluate.py` for multi-act runs: that evaluator records an
intermediate boss success on each act transition, even if the episode later dies.

The error was reproduced against committed source `15a78ba` using the actual
frozen70 normal-start 32-seed diagnostic. The old analyzer rejected it with
`boss counts disagree with raw outcomes`. The corrected analyzer explicitly
uses horizon 2 and accepts the same unmodified data: 26 Act1 clears/Act2 reaches,
0 two-act clears, two Act2 boss entries. This is recomputation of existing
development evidence, not new evaluation or server training. Original simulator
and runtime identities are retained in `recomputation.json`.

`sls-act12-pilot-analysis-v2` now separates:

- Terminal full-horizon wins, paired over every normal-start evaluation seed.
- Per-act clears and reach rates, checked against raw boss assignments/counts.
- Per-Boss act clears, entry-conditional act-clear rates and full-horizon wins.
- Failure floors grouped by the last reached act.

Absent entries/reaches produce undefined conditional rates. Act2 Boss groups
are policy-dependent reached populations; they do not justify a fixed-population
causal comparison. The primary metric remains the complete two-act outcome.
Optional environment/reward/decoding metadata must agree when present. Both
the success flag and `ACT_2_CLEARED` reason must agree.

The original Act1 default remains unchanged. Real70 endpoint and selected
confirmation files were recomputed with both old and new source: outputs are
exactly equal, respectively 1573/2048 and 1581/2048. Their hashes are recorded.
No observation/action/reward/model/native/training implementation changed.

Validation: 62 related tests passed; Ruff passed. Regression coverage includes
the real committed 32-seed artifact, unequal reach rates, zero entries, corrupt
per-act counts/reach metadata, terminal reason and evaluation identity mismatch.
`pytest.xml`, `validation.json` and `recomputation.json` preserve the evidence.

Reproduce the main regression locally:

```powershell
conda activate DL
python -m pytest tests/test_act12_analysis.py tests/test_win_continuation_analysis.py tests/test_compare_run_arms.py tests/test_act12_pilot.py -q
```

This batch does not bind a 90M model, change the running job or authorize a
new server submission. Actual completed 90M artifacts are still required.
