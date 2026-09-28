# A20 Act 1 post-54M audit and evidence plan

Status: 2026-09-22. No new long training is authorized by the current evidence.
The canonical champion remains the 46,006,272-step checkpoint from the completed
50M run.

## Evidence and repository state

The local checkout and `origin/main` were both `99d1990` (`Batch recurrent PPO
and prepare A20 recovery`) before this audit, with no pre-existing working-tree
changes. `git fsck --full` found no missing or corrupt reachable objects; it
reported only dangling objects. The relevant development chain is
`c71a095 -> 617e90a -> de2c941 -> 20bab3b -> 99d1990`. The implementation,
checkpoint contracts, evaluation artifacts and tests were inspected rather than
treating the plans in this directory as proof.

The local machine contains complete A20 30M, 40M and 50M run directories, but
not the server-only 54M run. Local hashes were recomputed:

- 46,006,272-step `best_progress.pt`:
  `2db39fde737445b109d31cc522dde42dfe2c7372b34007e108ce702b03acc70d`.
- Its independent 1,024-seed evaluation:
  `27795d9506782e3ca23e6b4e0a951343442a98a48b82063f9cec3dd7913661fa`.
- The checkpoint is schema v5, A20 Act 1, native source digest
  `653181e0795c44734fb8f17de1c1a7981869737c1cc3c2868c8f420ff6afbcc6`,
  vocabulary digest
  `04df4dc504ccf10d389b7b0e798ebc7a1ea2036edfc273a5cf5b11071b3b8ed0`,
  and records Torch 2.6.0+cu124, deterministic algorithms and `high` matmul
  precision.

The operator's server inspection found a clean checkout at `99d1990`. Job
869386 was no longer in Slurm, and the manifest was `COMPLETE` at 54,001,664
steps. The selected 52,002,816-step checkpoint has SHA256
`c71b7c4763c8c0648b42c081fb6a0bed34543a7cf684e83baed1f2a7d6cd997c`;
`final.pt` has SHA256
`1137338befbd7addb411922049939b089ed96a7e8d7aad0953279ee82b6f593e`;
and `final-evaluation.json` has SHA256
`d47ebb7800d41fca43fd9c000aba2d5d16d16b6490298de62412087ac651d1cb`.
These are operator-supplied server facts and have not yet been downloaded for
independent local byte inspection.

## Model result and champion decision

The 46M champion cleared 766/1,024 fresh seeds (74.80%, recorded 95% Wilson CI
72.06%-77.37%) without runtime failures. The 52M recovery checkpoint cleared
748/1,024 different fresh seeds (73.05%, CI 70.25%-75.67%), also without
runtime failures. Its internal `promotion_passed` only compares checkpoints on
the recovery run's selection set. Because the two final evaluations used
different seeds, the observed -1.76 percentage points is not a paired estimate.
It is nevertheless no evidence of an improvement. The 46M checkpoint remains
the canonical champion.

The high-LR experiment is negative evidence, not a crash: 383/512 at its 46M
baseline declined to 371/512 and 361/512 near 47M and 48M while clipping was
frequent. The lower-LR entropy-restart recovery produced local selection peaks
but no independently demonstrated breakthrough. Together, these results reject
"more PPO steps at a nearby LR is sufficient" as the next working hypothesis.

## What the 46M champion loses to

`tools/analyze_act1_failures.py` independently summarized all 1,024 rows in the
canonical evaluation, not only the 16 sampled failure traces:

| Outcome | Runs | Share of 258 failures |
| --- | ---: | ---: |
| Boss death on floor 16 | 174 | 67.44% |
| Pre-boss death | 84 | 32.56% |

Of the 84 pre-boss deaths, the last encounters were Lagavulin 22 times,
Gremlin Nob 18 times and three Sentries 14 times. Thus 54/84 pre-boss deaths
(64.29%) were at elites. Boss death plus elite death accounts for 228/258
(88.37%) of failures. By scheduled boss:

| Boss | Assigned | Wins | Boss deaths | Pre-boss deaths | Boss entries |
| --- | ---: | ---: | ---: | ---: | ---: |
| Hexaghost | 326 | 237 | 63 | 26 | 300 |
| Slime Boss | 336 | 264 | 42 | 30 | 306 |
| The Guardian | 362 | 265 | 69 | 28 | 334 |

This locates the failure burden but does **not** identify its cause. The saved
per-seed rows do not contain boss-entry HP/deck/relic/potion state, route room
types, card/shop/campfire choices, or complete legal-action histories. Aggregate
boss action counters cannot causally distinguish weak preparation from combat
tactics. Claims about potion use, pathing, deck building, representation or PPO
credit assignment therefore remain hypotheses.

The corpus capture and analyzer were generalized from their old hard-coded A0
profile to the profile bound into an Act 1 checkpoint. A stratified 100-run
capture (40 wins, 40 boss deaths and 20 early deaths across all three bosses)
will record complete public observations, legal alternatives, chosen actions
and replay snapshots without changing policy inputs or action selection.

## Performance finding

Batched recurrent PPO is a verified engineering success on the server: typical
late-54M updates were about 165 seconds (116 seconds collect, 49 seconds
optimize), or about 99 decisions/s. Compared with the earlier experiment's
roughly 235-second update (about 117 seconds in each half), optimization fell
about 59% and total update time about 30%. Collection is now about 70% of the
A100 update and is the next performance domain.

Collection now records six non-overlapping wall-clock segments. A one-layout,
one-measured-round local qualification on an RTX 5070 Laptop GPU with the real
46M warm start and 64 workers/16 shards measured 145.73 seconds total,
27.57 seconds collect, 118.16 seconds optimize and 28.11 decisions/s. Collection
was encode 9.77 s (35.4%), transition bookkeeping 8.86 s (32.1%), native worker
step 4.91 s (17.8%), policy/inference 3.87 s (14.0%), with reset/finalize below
0.2 s combined. This local result shows that raw simulator stepping is not the
only likely collection bottleneck, but it is not a substitute for an A100
profile. CUDA, CPU, NUMA and IPC behavior differ. No collection rewrite is
justified until the identical profile is measured on xgpg.

## Minimal falsifiable experiments

These are short diagnostics, not a continuation run:

1. Evaluate 46M and 52M on the same 2,048 new seeds, same simulator, runtime,
   profile and deterministic settings. Record both-win, 46-only, 52-only and
   both-loss seeds overall and per boss, plus exact two-sided McNemar tests.
2. Capture the stratified 100-run full-decision corpus for the 46M champion and
   analyze preparation and tactical patterns. Correlations generate a proposed
   intervention; a replay or isolated controlled experiment must then test it.
3. Run one 64-worker/16-shard A100 benchmark with the new collection segments.
   Do not repeat the historical layout search: this experiment asks where time
   goes, not which already-tested layout wins.

Acceptance and rollback rules:

- The 52M checkpoint cannot replace 46M unless the paired overall delta is
  positive with exact McNemar `p < 0.05`, runtime failures remain zero, and no
  boss group shows a practically large unexplained regression. Otherwise 46M
  remains champion. This comparison is diagnostic and must not be reused as a
  selection set for a later trained candidate's final evaluation.
- A learning intervention requires a repeated, quantified failure pattern and
  a mechanism that existing replay or a small isolated experiment can falsify.
  Anecdotes and aggregate correlations are insufficient. Any simulator or
  observation correctness defect takes priority over learning changes.
- A performance patch must preserve deterministic outputs, RNG order and PPO
  semantics, pass existing equivalence/resume tests, and improve median
  collection time by at least 10% in an otherwise identical three-round A100
  A/B. Revert it on semantic divergence, instability, or a smaller gain.
- Stop a diagnostic on runtime errors, native-source/contract mismatch,
  incomplete paired seed sets, non-finite values or unexplained replay drift.
  Never promote a partial output.

## Next formal-training decision

There is not yet enough evidence to specify a responsible long-training
hypothesis. The most likely high-level bottleneck is policy quality around
high-intensity elite/boss encounters, potentially combining upstream resource
preparation with tactical execution; the current artifact cannot separate
those alternatives. The paired comparison and decision corpus are the minimum
information needed to decide between a targeted distribution/reward/model
experiment and no learning change. Until then, do not submit 60M, 70M or any
other formal continuation.
