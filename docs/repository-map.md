# Repository map

| Path | Purpose |
| --- | --- |
| `src/sls/contracts/` | Public observations, semantic actions, decisions, transitions |
| `src/sls/backends/` | Simulator and original-game adapters |
| `src/sls/content/` | Content registry and policy scope |
| `src/sls/model/` | Input encoding, relational network, recurrent memory |
| `src/sls/rl/` | Rollouts, PPO, evaluation, checkpoint and migration contracts |
| `src/sls/runtime/` | Exported policy artifact and live controller |
| `src/sls/audit/`, `src/sls/diagnostics/` | Parity checks and failure investigation |
| `native/simulator/` | Native simulator, binding, upstream attribution |
| `native/oracle/` | Observation Oracle patch source |
| `configs/train/` | [Current and historical configurations](../configs/train/README.md) |
| `tools/` | [Command entry points](../tools/README.md) |
| `tests/` | Automated tests |
| `docs/history/` | [Dated audits and old plans](history/README.md) |
| `docs/results/` | Compact measured results, model identity and stage closeouts |

The 46M → 60M stage is complete. The current demonstration model is the selected 56M A20 Act1 champion; its independent final result is 1,585/2,048 in the server training simulator. See [stage results](results/a20-act1-60m-stable/README.md). Its configuration and [operator instructions](a20-act1-60m-stable-launch.md) are retained for provenance. No new run is scheduled. Historical documents are evidence of earlier decisions, not instructions to launch a current job.

The [2026-09-26 end-to-end audit](audits/2026-09-26-project-audit.md) records current source checks, fixes, and verification limits.
The [2026-09-28 closeout audit](audits/2026-09-28-stage-closeout.md) records model import, full automated checks and the archive layout.
The [Act1 qualification record](audits/2026-09-28-act1-qualification.md) records the current regression corpus, qualification safeguards and remaining environment gates.
The [Act1 review and training direction](audits/2026-09-28-act1-review-and-training-direction.md) records the maintainer's decision to pause broader parity auditing, the latest real-game demonstration, and the proposed experiments for the performance plateau. Complete semantic qualification remains open.
The [plateau execution workflow](act1-plateau-workflow.md) provides the prepared one-job baseline/diagnosis path and two budget-limited reward experiment configurations. No new training has been launched.
The [2026-09-29 independent audit](audits/2026-09-29-independent-audit.md) re-derived the stage result from raw evidence, paired the retired training environment against the current one on the same 2,048 seeds, measured a seed-block effect of about 3.4 percentage points, retracted the "run-level policy is broken" hypothesis after a learnability probe rejected it, and records the fixes staged for the next job.

## Local and ignored files

`local/build/` holds build outputs and downloaded tools. `local/runs/` holds training checkpoints, metrics and Slurm logs. `local/external/` holds user-owned game and Mod files. `local/audits/`, `local/reports/`, and `local/logs/` hold generated evidence and live journals. The ignored stock-game decompilation projection is under `local/audits/stock-decompilation-tree/`; its tracked generation metadata is in [`docs/audits/stock-decompilation/`](audits/stock-decompilation/README.md). `runs/archives/` holds downloaded server archives; `model/` holds exported policies. These paths are ignored by Git, except `model/README.md`. They may contain unique evidence and should not be deleted as part of source cleanup.

A fresh clone therefore contains the source, configs, tests and docs, but no training checkpoint, pretrained policy, game binaries or Mod JARs. The default exported policy and its companion JSON belong in `model/`. Historical exports reside in `runs/archives/policies/`, and old extracted training directories in `runs/archives/extracted-history/`. Follow the [model release instructions](model-release.md) to share the current model.
