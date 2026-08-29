# Repository map

- `cpp/simulator`: native FullRun engine and Python binding.
- `src/sls/contracts`: canonical public observations, semantic actions,
  decisions, and transitions.
- `src/sls/backends/simulator`: native simulator adapter.
- `src/sls/backends/original`: minimal CommunicationMod adapter and live
  session transport.
- `src/sls/content`: generated content registry, normalization, and the
  policy-visible Ironclad scope.
- `src/sls/model`: policy vocabulary, batching, relational Transformer, and GRU.
- `src/sls/rl`: workers, recurrent rollout math, PPO, evaluation, rewards, episode
  limits, and exact checkpoints.
- `src/sls/runtime`: simulator-only policy artifacts and safe live controller.
- `configs/train`: one canonical self-generated Act 1 -> Act 2 -> FullRun chain.
- `tools`: native build, training, resume verification, worker benchmark,
  Slurm submission, policy export, and live play.
- `tests`: contracts, simulator, model, RL, checkpoint, and live-runtime tests.

Generated or locally owned material belongs under ignored directories such as
`.build`, `external`, `runs`, `logs`, and `validation-results`.
