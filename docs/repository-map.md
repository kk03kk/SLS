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

`local` is not source code. Delete selected regenerable parts only when their
checkpoints, game assets, or evidence are no longer needed.
