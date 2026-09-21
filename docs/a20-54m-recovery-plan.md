# A20 Act1 54M recovery experiment

Prepared 2026-09-21 after stopping the first 60M optimization experiment. This
run is a fresh weight transfer from the audited 46,006,272-step champion, not a
resume of either the 50M optimizer state or job 868205.

## Why the previous experiment stopped

The higher `0.0000625` learning rate produced finite updates and stayed below
the KL guard, but it did not improve the fixed-seed curve. The new 512-seed
baseline was 383 clears; evaluations around 47M and 48M fell to 371 and 361.
Gradient clipping was active on most minibatches and final KL commonly reached
roughly 0.005-0.010, compared with about 0.003 late in the older 50M run. The
evidence does not justify spending the remaining 60M budget on the same
trajectory.

The run did show that 64 workers / 16 shards is the best measured xgpg layout,
but total throughput rose only to about 70 decisions/s. Collection and PPO
optimization each consumed roughly half of a 232-second update. Low average
GPU utilization came from thousands of small sequential model launches, not
from insufficient A100 compute capacity.

## Structural performance changes

For each recurrent PPO minibatch, all 16 sequences × 64 time steps are now
encoded by the Transformer and candidate-action encoder in one batched call.
Only the GRU recurrence and action/value heads remain time ordered. The public
single-step forward path is unchanged for rollout collection. Tests compare
the split and ordinary paths, including outputs, recurrent state and parameter
gradients.

This reduces expensive encoder launches per minibatch from 64 to one and
coalesces sequence metadata transfers. A local real-model qualification drove
the GPU to 100% and used about 12.8 GB peak memory, which fits the 40 GB A100;
the A100 qualification remains authoritative for throughput.

Intermediate epochs retain a full-rollout KL check because it controls early
stopping. After the final epoch no further update can be prevented, so its
reported KL uses a deterministic 4,096-sample subset instead of rescanning all
16,384 decisions. The metric records its sample fraction. This changes no
gradient update.

The config pins the already measured 64-worker / 16-shard layout. Because the
PPO execution structure changed, preparation runs one qualification benchmark
for that layout. It no longer searches all five historical layouts.

## Learning experiment

Config: `configs/train/ironclad_a20_act1_54m_recovery.toml`.

- Parent: 46,006,272-step champion, SHA256
  `2db39fde737445b109d31cc522dde42dfe2c7372b34007e108ce702b03acc70d`.
- Target: 54M cumulative steps, about 8M new decisions.
- Learning rate: `0.00003125`, restoring the stable late-50M value rather than
  repeating the unsuccessful doubled-rate experiment.
- Entropy: controlled restart at `0.006`, linearly returning to `0.002` over
  4M steps from the parent boundary. This is intended to explore alternatives
  without sustaining a permanently high-entropy policy.
- Adam, RNG, workers, recurrent state, update counter and selection state are
  reset. All model parameters, including the value head, are preserved.
- Periodic and final evaluation seeds are new and disjoint from both the 50M
  run and the stopped experiment.
- The source checkpoint remains protected by the selection progress guard.

Inspect the source baseline and every 1M evaluation. Stop after the 48M
evaluation if both 47M and 48M remain materially below baseline and the entropy
restart shows no recovery. Continue toward 54M only if the curve is stable or
improving. A checkpoint may replace the source only by improving fixed-seed
successes without runtime failures.

## Server submission

After pulling the reviewed commit into a clean server checkout:

```bash
cd /home/h/hengzhi/SLS
export SLS_PY=/home/h/hengzhi/venvs/sls/bin/python
export CUBLAS_WORKSPACE_CONFIG=:4096:8

$SLS_PY tools/submit_slurm.py train \
  --config configs/train/ironclad_a20_act1_54m_recovery.toml \
  --constraint xgpg --prepare
```

Before allowing the long run to continue, inspect the single-layout benchmark,
peak CUDA memory, source checkpoint hash, baseline evaluation and first update.
The preparation should report exactly 64 workers and 16 shards.
