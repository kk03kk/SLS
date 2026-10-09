# Critic20M GRID startup repair — 2026-10-09

Job 922895 at 5e26eea failed in the GRID acceptance probe, before model warmup or production training. The operator supplied the traceback and a successful 64:16 A100 benchmark (~115.97 decisions/s); raw server logs have not been imported. Benchmark completion does not prove the remaining compute gate passed.

Action.from_dict requires explicit schema version 1. The hand-written REMOVE_CARD dictionary omitted it and was rejected as version -1. Backend legality uses complete candidate identity, including metadata. Every GRID step now reuses the unique current Decision Action object by kind and card instance, refreshing after each transition and restoration. Missing or ambiguous candidates still fail closed. The only remaining from_dict call reads the complete hash-bound fixture action to identify kind and instance.

The extracted GRID probe retains first selection, selected encoding, candidate count, unchanged deck, legal cancellation, exact restored checkpoint, two different instances of the same card, removal of two cards and automatic completion. Tests reproduce the original schema failure, enforce current object identity and metadata, and inject projection, cancellation, restoration, commit, final-action and encoding failures. Locally encoding uses a mock callback; the original real encoder assertion is unchanged in the NUS entry point. All real warmup/return/PPO recovery checks remain unchanged.

Local results: 104 targeted lightweight/bounded CPU tests; 35/35 lightweight configs; Ruff; diff check; submission dry-run; source identity checks. No model, GPU, rollout, benchmark or native rebuild ran locally. Actual GPU acceptance is NOT_YET_RUN. See startup-fix-validation.json.

Only operator code and its plan hash binding changed. Training recipe, native source, observation/action schemas, v6 checkpoints and migration evidence remain unchanged. No compatibility whitelist is added. Original failure and historical readiness evidence are retained.

Restart in a new isolated directory pinned to the repaired commit, importing the original frozen 90M parent. Preserve the failed checkout and reports. Cancel only pending children 922896 and 922897; inspect unexpected active states. Repeat all acceptance gates; do not delete failed ledger entries, reuse failed submission receipts, spoof gate success or resume probe learning.
