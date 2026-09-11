# Act1 v4: champion continuation to cumulative 20M

## Evidence and decision

Read from `sls-act1-v4-10m-complete.tar.gz`, including all 337 metrics records,
selection metadata, final evaluation, checkpoint payload and the completed job's
stderr. The selected checkpoint is 7,766,016 steps (update 474), fixed 422/512,
held-out 850/1024. Held-out boss rates are Hexaghost 81.54%, Slime Boss 87.54%,
Guardian 80.12%. Final at 10,010,624 steps scores 414/512; do not initialize from
final. The later 8,765,440 checkpoint also scores 422; the existing tie rule keeps
the earlier champion. Final evaluation reports zero backend errors/truncations,
step limits and timeouts.

After 7.5M, mean approximate KL is 0.00584 (range 0.00370–0.00797), clip fraction
9.80%, entropy 0.416, value loss 0.0197 and explained variance 0.455. No KL early
stops occur. Fixed evaluations repeatedly exchange roughly 50–70 wins/losses
between adjacent checkpoints while aggregate performance plateaus. This supports
a smaller update size as a conservative experiment; it does not prove excessive
LR is the sole cause or that further training will reach 100%.

Use **LR 0.0000625**, half the champion's 0.000125. Keep network, reward, PPO
epochs, clipping, GAE, gamma, rollout, minibatches, worker layout and entropy
schedule unchanged. Entropy coefficient continues from about 0.01651 at the
champion to 0.011 at 20M on the original 40M schedule. No artificial game rules
or strategy rewards are added.

RNG and in-flight episodes are preserved. The first rollout repeats the parent's
next rollout, but its update uses the new LR and diverges from the old parameter
trajectory. This is an explicit continuation branch, not exact resume of the
previous experiment. It deliberately avoids also reseeding workers or resetting
Adam. Local regression verifies different updates with the half-LR branch.

## Configuration and retained state

`configs/train/ironclad_a0_act1_20m.toml` binds:

- Parent: `local/runs/ironclad-a0-act1-v4-10m/stages/train/selection/best_progress.pt`.
- SHA256: `c8e113c4e486c9615baf5f763398383b7e740f88abbd7a39d5a97b38dbae691d`.
- New output: `local/runs/ironclad-a0-act1-v4-20m`; original 5M/10M are read-only.
- Target: cumulative 20,000,000 steps, approximately 12.234M additional steps;
  complete rollouts may slightly exceed it.
- Same fixed 512 seeds, baseline plus evaluations near 8M, 9M, …, 20M.
- Recovery saves every 250k, latest/best/final retained. Seed the new best with
  the champion; only a strictly better fixed win count replaces it.
- Final best evaluation: 1024 new seeds `[2000000002048, 2000000003072)`.
  Training remains capped below `2000000000000`, excluding older held-out sets.

At the observed mean throughput of 67.3 steps/s, updates alone take about 50.5
hours. Evaluation/preparation add time; the existing 72-hour Slurm request and
interruption-safe resume remain in place. Less frequent selection can miss a
transient peak between evaluations; the reduced evaluation cost is intentional.

## Implementation and validation

The initializer already supports sequential half-LR continuations without
resetting model/Adam/RNG/workers. Fix benchmark reuse to follow the parent chain:
the retained report was measured at the original 5M LR, not at the 10M LR. Every
ancestor must match the current workload apart from LR; cycles and non-LR
changes are rejected. The report is never rewritten or relabeled. Original 5M
and 10M config files and the original benchmark must remain available on NUS.

Local direct-script initialization of the actual champion succeeded. Recursive
comparison of all payload fields found only the expected LR and training-identity
changes; repeat initialization left latest unchanged and the source SHA stayed
unchanged. Temporary validation outputs are under
`local/audits/act1-10m-complete/`, not a formal run.

Small-model integration exercises two successive continuations, actual subprocess
entry, worker restore, PPO update, best preservation and interrupted publication.
Regressions cover ancestor benchmark reuse/rejection and the exact 20M config.
Local tests do not replace the compute-node CUDA checkpoint preflight, which
`--prepare` executes before training. No new native build or benchmark is needed
when existing artifacts and workload evidence match.

## NUS operation

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a0_act1_20m.toml --prepare
```

The same command resumes a valid existing 20M run. Preparation failures stop
before long training. No Slurm job was submitted during local preparation.
