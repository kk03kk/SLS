# A20 Act1 40M result and 50M continuation

Audit date: 2026-09-17. The 40M job was externally killed near 39.01M, so the
run did not reach finalization. Its atomically promoted periodic best is valid:

- checkpoint: `local/runs/ironclad-a20-act1-v1-40m/stages/train/selection/best_progress.pt`
- SHA256: `802cc83e6e2fa16a00ebea44a72a46bb2f48ee6295a2eefc5d0375faffd4b3a5`
- trainer state: 38,010,880 environment steps, update 1,282
- fixed evaluation: 374/512 Act1 clears (73.05%)
- selection objective: `ACT1_CLEAR_COUNT`
- evaluation runtime failures, limits, truncations and backend errors: zero

The independent local 100-seed deterministic diagnostic cleared 77/100. Its
95% Wilson interval overlaps the 512-seed result, so this supports generalization
but is not evidence that the policy's true rate increased to 77%. Hexaghost
remains the largest Boss gap. Macro behavior is coherent but sharp: the policy
usually takes card rewards, rarely removes cards, and strongly prefers safe
event choices. No simulator invariant failure or obvious stock-mechanics defect
was observed. The diagnostic summary code was corrected to deduplicate card
offers on `COMBAT_REWARD`, classify skip/Singing Bowl outcomes, and keep event
follow-up choices separate from event options.

## 50M experiment

Config: `configs/train/ironclad_a20_act1_50m.toml`.

Continue from the pinned 38,010,880-step best to cumulative 50M in a new sibling
run directory. The parent lacks `final-evaluation.json` because the job was
killed before finalization. The continuation therefore explicitly declares
`continuation_selection_evidence = "periodic-best"`. Initialization still
requires the pinned checkpoint hash, matching checkpoint step/update, a complete
512-episode best record, the clear-count objective, and zero evaluation runtime
failures. This is not an exact resume of the interrupted 40M trajectory; it is a
state-preserving branch from its selected best.

Only the cumulative target and new final held-out seed range change:

- LR remains `0.00003125`.
- PPO, model, rewards, worker layout and training seed namespace remain unchanged.
- Entropy remains `.02 -> .002` on the original cumulative 40M clock and stays
  at `.002` afterward. Extending the clock would jump the coefficient upward at
  continuation, confounding this scaling run.
- Fixed 512-seed evaluation remains every 1M steps.
- Checkpoints remain every 250k steps.
- Final evaluation uses 1,024 fresh seeds
  `[2000000005120, 2000000006144)`.

The existing 64-worker/8-shard benchmark may be reused because tensor workload
and PPO structure are unchanged. `--prepare` must still rebuild or validate the
current native source, validate the pinned parent and exercise worker checkpoint
resume on the allocated compute node.

## NUS launch

The server must retain the complete 40M parent directory, especially its
training config, best checkpoint and best metadata. From the login node:

```bash
cd ~/SLS
git switch main
git pull --ff-only origin main
git status --short
TRAIN_PY=/home/h/hengzhi/venvs/sls/bin/python
"$TRAIN_PY" -m pip install -e . --no-deps
"$TRAIN_PY" tools/submit_slurm.py train \
  --config configs/train/ironclad_a20_act1_50m.toml --prepare
```

`git status --short` should be empty before submission. Preparation and training
run on the Slurm compute node; do not run them directly on `xlogin`.
