# Ironclad A0 FullRun v3 10M migration runbook

This chain targets ordinary Act 3 victory. It does not enter Act 4. The source
milestone is exactly 7,004,160 environment steps (update 570) with 48 workers,
256 rollout steps, and 16 shards. `latest.pt` is never substituted for the
numbered source checkpoint.

The migration is intentionally operational rather than an audit gate. It
accepts the exact operator-selected 7M checkpoint directly, but still refuses
wrong steps/update, worker layout, model shape, vocabulary, encoding, PPO
contract, native artifact, or benchmark identity. It records the source hash
and preserves the original checkpoint inside the new v3 directory.

## Preserve 7M before updating the server checkout

On the old training checkout, wait for the exact numbered checkpoint and stop
the batch safely:

```bash
cd ~/SLS
JOB_ID=<current-job-id>
CKPT=local/runs/ironclad-a0-fullrun-v2/checkpoint-steps-000007004160.pt

while [ ! -f "$CKPT" ]; do
  squeue -j "$JOB_ID" -o '%.18i %.10T %.20R'
  sleep 60
done

scancel --batch --signal=TERM "$JOB_ID"
sacct -j "$JOB_ID" --format=JobID,State,ExitCode,Elapsed,NodeList
```

Copy the exact checkpoint and its old-run provenance before updating code:

```bash
mkdir -p local/exports/act2-7m-source
cp -p "$CKPT" local/exports/act2-7m-source/source-checkpoint.pt
cp -p local/runs/ironclad-a0-fullrun-v2/run-manifest.json \
  local/exports/act2-7m-source/source-run-manifest.json
sha256sum local/exports/act2-7m-source/source-checkpoint.pt \
  > local/exports/act2-7m-source/SHA256SUMS
```

## Update, rebuild, benchmark, and create v3

Update the server to the selected current code, rebuild native on a compute
node, recreate the fixed 48:16 benchmark, and run preflight. Then create the
new chain:

```bash
"$TRAIN_PY" tools/prepare_training_migration.py \
  --source local/exports/act2-7m-source/source-checkpoint.pt \
  --config configs/train/ironclad_a0_fullrun_10m.toml \
  --stage train \
  --output local/runs/ironclad-a0-fullrun-v3
```

The migration checks update 570, 7,004,160 steps, Act2 source profile, exact
model/PPO/vocabulary/encoding contracts, 48:16 layout, and current native
binary. It preserves learning state and RNG while resetting only
the in-flight environments, episode limits, recurrent memory, episode-start
mask, previous action, and previous reward.

Submit the long job directly after migration:

```bash
"$TRAIN_PY" tools/submit_slurm.py train \
  --python "$TRAIN_PY" \
  --constraint xgpg \
  --time 24:00:00 \
  --config configs/train/ironclad_a0_fullrun_10m.toml
```

It exact-resumes the v3 chain to 10,002,432 cumulative steps (update 814).
The first baseline evaluation and first PPO update provide the early health
check without creating a separate disposable chain or mandatory soak gate.

The train stage records rotating 256-seed diagnostics every 500k, fixed
1,000-seed selection evaluations every 1M, numbered checkpoints every 500k,
and a never-used 2,000-seed final evaluation. Artifact export additionally
requires FullRun success >=0.5%, Act2 reach >=65%, Act3 reach >=8%, every Act1
boss >=60%, and zero backend/truncation/timeout/step/cycle/self-loop failures.
