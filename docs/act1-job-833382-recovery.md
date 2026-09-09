# Act1 job 833382 crash and recovery

Evidence: `runs/sls-act1-v4-first-training-crash.tar.gz`, inspected without changing the archive or downloaded checkpoints. Local extracted evidence and test outputs are under `local/audits/act1-first-training-crash/` (ignored).

## Cause

Worker 43, episode 75, seed 10004912 entered Note for Yourself on floor 2 with Swift and Block potions. The auto-leave adapter matched every native action with `idx1 == 1`. Both event Leave and potion-slot-1 Discard satisfy that condition. The assertion therefore found two actions and aborted shard 5 during collection. No potion was actually discarded by this failed assertion.

The original crash dump contains the last successfully adapted MAP boundary, not the newly entered event. Reproducing the failure requires restoring `raw_backend_state` **and executing `last_semantic_action`**. Merely loading the dump does not reproduce this step failure. The regression retains its ten native replay actions and retries that map action.

The fix selects an event action only after excluding potion/discard actions. The same filter handles normal entry and checkpoint restoration. Tests compare it with native manual Leave, including unchanged inventory, RNG, resulting decision and checkpoint round-trip.

## Training state

- 22 completed PPO updates, 360,448 logged steps, 4,791 completed episodes.
- The downloaded latest checkpoint is update 16, 262,144 steps, 3,847 episodes; all 64 saved worker states restore and round-trip locally.
- Six completed updates (98,304 steps), plus the incomplete rollout, must be recomputed. There is no need to reinitialize the policy or optimizer.
- Median full-update throughput: 87.43 steps/s. Logged numeric metrics are finite; no backend truncations, cycle-limit or step-limit terminations occurred in the completed updates. Training recorded two Act1 successes. The random initialization baseline was 0/512. This early sample is not a convergence assessment.
- Benchmark: 32/4 = 81.47, 64/8 = 87.74, 128/8 = 89.35 steps/s. The existing 64/8 layout is within 1.8% of the fastest, satisfying the 5% selection rule. These timings do not measure GPU utilization directly.

The initial stale-native traceback was a preparation probe followed by a successful rebuild. Both preflight and full-layout worker-resume reports passed. NumPy and initial cuBLAS-context warnings were not the fatal error. Short checks had not exercised this event/potion combination.

## Recovery contract

`configs/compatibility/state-preserving-source-transitions.json` permits only the exact directional old/new simulator-source pair for this fix. Previously successful transitions, model/observation schema, reward/PPO, optimizer, RNG and saved worker state are unchanged. The formerly crashing boundary now performs the already specified Leave action. Unknown source changes still fail; other checkpoint contract fields remain protected.

This is a recorded state-preserving bug-fix continuation, not an assertion of exact replay across changed code or GPU hardware. The checkpoint loader records the source difference in the existing rebind manifest history. It retains all learning and in-flight episode state, rather than performing environment migration or warm-start.

Preparation rebuilds native provenance if needed and reruns the short configuration preflight and full-layout save/restore/update verification. It reuses the existing benchmark layout and does not rewrite its historical throughput or provenance. Only then can training resume. No model evaluation or build runs on xlogin.

Before resumed updates append to metrics, the complete original log is preserved as a content-addressed `metrics.before-resume-*.jsonl` beside the active log. Active metrics retain only steps at or before the restored checkpoint, preventing duplicate rollback branches. The source archive is untouched.

## Server command

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a0_act1_5m.toml \
  --prepare
```

Keep the existing run, preparation directory and latest checkpoint. Do not delete them, manually edit hashes, or request an environment migration. If the preparation contract rejects another mismatch, inspect that traceback before submitting again.

## Local validation

- Native build completed with source digest `a0da97e6fd694b9cf9074726d48a6d6d649d2554de2229ce78af8b8628122045`.
- Full suite: 609 passed, 1 historical incompatible-policy test skipped; Ruff passed.
- Actual configured network short GPU preflight: passed, including exact save/restore and next-update comparison. This used the local GPU/software stack; server preparation will independently verify its own runtime.
- Original failure reproduced before the fix; retry after the fix returns MAP with both potions retained. All 64 downloaded saved worker states restore and round-trip.
- Source-transition regression compares the next PPO update and weights after restoring optimizer/RNG/worker state, and rejects a simultaneous PPO semantic change.
- Slurm submission dry-run passed. No server job was submitted from the local machine.
