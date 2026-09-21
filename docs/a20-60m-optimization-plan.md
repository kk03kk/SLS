# A20 Act1 60M optimization experiment

Status update, 2026-09-21: superseded and stopped as an experiment. Job 868205
reached at least 48,365,568 cumulative steps. On the new fixed 512-seed set,
the 46,006,272 source scored 383/512, while the 47,005,696 and 48,005,120
evaluations scored 371/512 and 361/512. Throughput improved to about 70.6
decisions/s, but collect and optimize still each took about 115 seconds per
update. Its weights are not an input to the next experiment; the audited 46M
champion remains the parent. See `a20-54m-recovery-plan.md`.

Prepared 2026-09-20 from the completed 50M run. This is a new experiment,
not an exact continuation of the 50M optimizer trajectory.

## Evidence from 50M

- The fixed 512-seed score improved to 388/512 (75.78%) at 46,006,272 steps,
  then stayed between 374 and 388 clears through 50,003,968 steps. The final
  held-out result was 766/1,024 (74.80%).
- Late-run sampled KL was approximately 0.0014-0.0026 against a target of 0.02,
  while clip fraction was approximately 0.034-0.042. Training never used the
  KL early stop. The policy was still updating, but conservatively.
- Entropy reached its 0.002 floor at 40M. Late policy entropy remained about
  0.18-0.19, so the run had not become fully deterministic.
- Late updates took roughly 259-283 seconds, or 58-64 decisions/second. Peak
  allocated GPU memory was only about 4.48 GB on an A100 40GB. The old log did
  not separate simulator collection from PPO optimization.

## Implemented performance work

`PPOTrainer` now keeps minibatch metric reductions on the GPU and transfers the
aggregate once per update. The former code converted six tensors to Python
floats in every minibatch, serializing CUDA repeatedly. Sequence tensor lookup
is vectorized, sampled actions are transferred only once, and rollout outputs
are packed into one device-to-host transfer per dtype before entering the
serial native-worker step. Environment resets
that occur on the same rollout step are now dispatched together across shards
instead of making one IPC round trip per completed episode.

On the local RTX GPU, an old/new controlled PPO comparison using the same
32-worker, 64-step rollout and minibatch size 16 took 13.89/19.47 seconds with
the old implementation and 12.81/15.16 seconds with the new implementation
(about 16% lower mean time). Final KL differed by less than `5e-10`. This is
local directional evidence, not a substitute for the A100 benchmark.
With an identical 32-worker, 128-step sampled trajectory, batched reset plus
coalesced device-to-host rollout copies reduced collection time from 24.86 to
23.38 seconds (6.0%) with identical episode and action results. A separate
local shard check took 26.44, 24.52, and 24.26
seconds for 4, 8, and 16 shards respectively; it supports measuring 16 shards
on NUS but does not justify forcing that layout without the A100 result.

Training metrics now include `collect_seconds` and `optimize_seconds` as well as
total `update_seconds`. They also record the effective learning rate and the
fraction of optimizer minibatches whose gradient norm exceeded the 0.5 clipping
threshold. This makes the next A100 run diagnose both its actual speed
bottleneck and whether clipping is limiting the higher learning rate.
`analyze_training_history.py` summarizes these fields when present.

A larger minibatch was considered but rejected: on the local variable-candidate
workload, 32 sequences was slower than 16 because the larger mixed batch paid
more candidate/entity padding. The production setting remains 16 until an A100
measurement supports changing it.

## Learning experiment

Config: `configs/train/ironclad_a20_act1_60m_optimization.toml`.

- Source: the audited 46,006,272-step champion, SHA256
  `2db39fde737445b109d31cc522dde42dfe2c7372b34007e108ce702b03acc70d`.
- Target: 60M cumulative steps (about 14M new decisions after the source).
- Preserve: every model parameter, including the value head.
- Reset explicitly: Adam state, workers, recurrent state, RNG stream, update
  count and best-selection state. The initialization manifest labels this as
  non-exact and records the parent hash and step count.
- Learning rate: restore `0.0000625`, the 30M rate, because the 50M late KL was
  about one tenth of its guard. The KL guard remains 0.02.
- Entropy floor: keep 0.002. Late entropy remained about 0.18-0.19 and the
  46M/50M policies exchanged 56 wins and 56 losses on the fixed seeds despite
  having the same total score, so the evidence shows policy churn rather than
  collapsed exploration.
- Minibatch size and epochs remain 16 and 2.
- Baseline retention is enabled: the source policy is evaluated first and later
  checkpoints cannot replace it unless they improve fixed-seed successes
  without runtime failures.
- Selection and final seed ranges are new and disjoint from the 50M ranges.

## Server launch and acceptance

After the reviewed source changes are present in the clean server checkout:

```bash
cd /home/h/hengzhi/SLS
export SLS_PY=/home/h/hengzhi/venvs/sls/bin/python
export CUBLAS_WORKSPACE_CONFIG=:4096:8

$SLS_PY tools/submit_slurm.py train \
  --config configs/train/ironclad_a20_act1_60m_optimization.toml \
  --constraint xgpg --prepare
```

Preparation validates the bound champion during preflight and initializes each
benchmark candidate from its weights. The full collect-plus-PPO benchmark tests
`32:4`, `64:8`, `64:16`, `128:8`, and `128:16`; the 16-shard candidates are
included because the allocation has 16 CPUs and the previous preparation never
measured them. First inspect the generated `.err`, benchmark
rows, initialization record, and the first updates. Stop and investigate if
the source hash/step validation fails, metrics are non-finite, KL exceeds 0.02,
clip fraction becomes extreme, or throughput regresses materially. Continued
learning is judged by the fixed 512-seed curve; final quality is judged only by
the new 1,024-seed final evaluation.
