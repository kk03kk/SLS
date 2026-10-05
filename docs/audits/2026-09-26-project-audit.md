# SLS end-to-end engineering audit — 2026-09-26

This audit used the local `D:\SLS` source and native build in the `DL` environment. It followed the A20 Act1 data path from native game state to public observations and legal actions, worker transitions, recurrent rollouts, PPO, checkpoints, selection, final evaluation, and export. Existing prose was used only to locate prior experiments; conclusions below come from source inspection and the checks listed here. The NUS 60M job was not contacted or modified.

## Verified path and limits

| Layer | Source/evidence checked | Result and boundary |
| --- | --- | --- |
| Environment | `native/simulator/`, `src/sls/backends/simulator/environment.py`, contract tests and 16 local A20 seeds | Legal candidate IDs were unique, all 16 sampled episodes reached a terminal boundary, and 38 native checkpoints round-tripped. The 542 actions were chosen randomly, so this measures invariants, not policy quality or stock-game parity. |
| Policy input | `src/sls/contracts/`, `src/sls/model/encoding.py`, batching/model tests | Current input schema is v5; policy sees public observations and semantic legal candidates. Existing tests cover owner references, dynamic card fields, padding and GRU reset. No exhaustive stock-game comparison was possible because the original-game JAR is not in the verified local path. |
| Training | `src/sls/rl/{workers,rollout,reward,episode_limit,ppo}.py`, training configs and tests | GAE masks terminal boundaries, recurrent sequences carry episode-start masks, and the A20 configuration parses with valid PPO values. The running server job remains on its submitted code. |
| Recovery | `src/sls/rl/checkpoint.py`, `training_contract.py`, `tools/train_full_run.py` | Complete checkpoint state is saved atomically. Runtime rebind checks native/runtime/config fields; this audit added a separate guard for Python training implementation changes at the canonical CLI entry point. |
| Evaluation | `src/sls/rl/evaluate.py`, `best_checkpoint.py`, finalization code | Selection and final seeds are distinct. The selected checkpoint is evaluated separately from the terminal training state. `boss_success_rate` is grouped whole-Act success by scheduled Boss, not conditional Boss-combat win rate. |

## Changes made

1. **Automatic resume source guard.** New manifests record a digest of PPO, model, contract, curriculum and training-entry implementation. Automatic resume rejects a changed digest. For older clean Git manifests without that field, the CLI compares the current implementation paths with the recorded commit, including untracked implementation files. Reviewed native changes remain a separate contract. Legacy manifests without a trustworthy clean Git revision cannot be fully proven by this fallback and retain their former behavior.
2. **Seed-range validation.** The trainer now checks every planned rotating diagnostic seed range against fixed selection and final ranges before constructing workers. It also rejects evaluation seeds beyond the native unsigned 64-bit domain and diagnostic cadence without a seed set.
3. **Config fail-fast checks.** PPO rejects NaN and infinite scalar hyperparameters. Required positive integer settings reject booleans, fractional values and strings; evaluation action limits are checked before training.
4. **Documentation.** The model release checklist now distinguishes scheduled-Boss whole-Act success from Boss-combat entry performance.

These changes do not alter the simulator rules, model tensors, reward formula, action choice, or PPO update math for valid existing configurations. They do change source/provenance evidence. **Do not synchronize this uncommitted local tree into the ongoing 60M server checkout.** Any future server code update needs its own preparation/source checks; an automatic resume may intentionally reject implementation changes instead of silently treating them as equivalent.

## Verification

- Before edits: 767 tests passed, 1 historical-model skip; Ruff and policy vocabulary checks passed.
- Final full suite before the last entry-point edge-case test: 812 tests passed, 1 expected skip, 4 warnings from explicit runtime-rebind tests. The 111 affected tests passed after the positive-integer and legacy-Git guard edits; the additional entry-point test passed in its 14-test module. Ruff passed.
- All 13 checked-in training TOML files parsed and passed PPO and seed-namespace validation.
- A20 Act1 short invariant run: 16 seeds, 16 completed episodes, 542 random actions, 38 checkpoint round-trips, no errors. Zero wins under this random chooser is not a model result.
- No local long model evaluation, GPU training, server command, or real-game run was performed.

## Remaining evidence needed

The full native implementation and every game branch have not been proven equivalent to the original game. The original-game JAR was absent at the recorded local path, so this audit could not reproduce bytecode or live-game parity. The 46M historical 766/1,024 result was not rerun on the changed simulator. The 60M job has no result available locally; its independent evaluation and selected checkpoint hash must be inspected after completion before claiming improvement. Direct use of PyTorch checkpoint loaders requires trusted `.pt` files because they deserialize pickle-backed training state.
