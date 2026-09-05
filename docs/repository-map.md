# Repository map

## Stable project roots

- `src/sls`: Python application and library code.
- `native/simulator`: native FullRun engine and Python binding.
- `configs`: declarative training and runtime configuration.
- `tools`: build, training, evaluation, audit, export, and live-play commands.
- `tests`: unit, contract, integration, and simulator tests.
- `docs`: maintained architecture and operating documentation.
- `local`: machine-owned state and generated evidence.

## Python package

- `src/sls/contracts`: canonical public observations, semantic actions,
  decisions, and transitions.
- `src/sls/backends/simulator`: native simulator adapter.
- `src/sls/backends/original`: CommunicationMod adapter and live session transport.
- `src/sls/content`: generated content registry, normalization, and policy scope.
- `src/sls/model`: vocabulary, batching, relational Transformer, and GRU.
- `src/sls/rl`: workers, rollout math, PPO, evaluation, rewards, episode limits,
  and exact checkpoints.
- `src/sls/runtime`: policy artifacts and fail-closed live controller.
- `src/sls/audit` and `src/sls/diagnostics`: simulator/original-game parity evidence.

## Local state

- `local/build`: native build tree and downloaded build tools.
- `local/runs`: checkpoints, metrics, trajectories, Slurm logs, and crash evidence.
- `local/external`: user-owned game and mod files.
- `local/logs`: live policy journals.
- `local/reports`: generated audit and validation reports.
- `runs/9m`: existing imported checkpoint and server stdout/stderr evidence.
- `local/imports`: existing migration input checkpoints.
- `local/audit`, `local/audits`: existing audit evidence; the 2026-09-05 audit
  uses `local/audits/repository-20260905`.

These are existing storage locations, not interchangeable copies. Keep imported
evidence and checkpoint paths stable. New training jobs use `local/runs`; new
audit summaries live in `docs/audits/<date>` with bulky evidence under
`local/audits/<audit-id>`. Build and test caches are regenerable; checkpoints,
game assets, journals and captured evidence require a separate retention decision.

## Command groups

| Purpose | Entry points |
| --- | --- |
| Local setup and native build | `bootstrap.py`, `build_native.py` |
| Server launch and qualification | `submit_slurm.py`, `preflight_training.py`, `benchmark_workers.py` |
| Training and state transfer | `train_full_run.py`, `prepare_training_migration.py`, `prepare_model_warm_start.py`, `verify_training_resume.py`, `diagnose_checkpoint_contract.py` |
| Archived training analysis | `analyze_training_history.py` |
| Evaluation and export | `evaluate_checkpoint.py`, `export_policy.py`, `seal_training_milestone.py` |
| Game interaction | `play_live.py`, `play_live_inspector.py`, `configure_live_inspector.py` |
| Content and stock comparison | `generate_content_registry.py`, `generate_policy_vocabulary.py`, `audit_stock_bytecode.py`, `audit_stock_parity.py`, `run_original_card_audit.py` |
| Reproduction and trajectories | `replay_failed_state.py`, `audit_simulator_seeds.py`, `capture_policy_trajectory.py`, `compare_policy_trajectories.py`, `run_original_canary.py` |

Names above are under `tools/`. Historical dated audits record findings at a
particular revision; they are not current acceptance gates. The
[2026-09-05 independent audit](audits/2026-09-05/report.md) includes unresolved
findings, actual test coverage, and a read-only evidence collector.
