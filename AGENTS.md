# AGENTS.md

## Project context

SLS is a Slay the Spire 1 simulator and recurrent PPO agent project for training and evaluating agents that play Slay the Spire.

This `AGENTS.md` file defines persistent top-level instructions for agents working on the project. **Do not modify, rewrite, update, or delete this file unless the human user explicitly asks you to do so.** Project progress, experiment status, results, and other changing information belong elsewhere, not in this file.

## Local development

The primary workstation is Windows at `D:\SLS`. Use the Python 3.12 Conda environment `DL` for development, native builds, tests, parity auditing, and other local tasks. Activate it with `conda activate DL`.

Code changes should be completed and appropriately validated locally first.

In PowerShell sessions, keep Python bytecode out of source directories:

```powershell
conda activate DL
$env:PYTHONPYCACHEPREFIX = 'D:\SLS\local\build\python-cache'
```

If Conda is not initialized in the current PowerShell session, run `& D:\Anaconda\shell\condabin\conda-hook.ps1` first. Do not change the user's global environment just to configure a task session.

## Repository organization

Maintain one project that progresses on the existing `main` branch. **Do not create new branches or Git worktrees for development, experiments, audits, or cleanup.** Preserve existing uncommitted work. Cleanup does not authorize force-pushing, resetting the repository, deleting branches, or rewriting Git history.

Keep the root limited to project metadata, `README.md`, `AGENTS.md`, the license, and the established project directories. Do not leave scratch scripts, downloads, logs, archives, models, or build outputs in the root. Keep reusable commands in `tools/`, implementation in `src/` and `native/`, configurations in `configs/`, and regressions in `tests/`. Do not move working code merely because it was introduced during an old experiment.

## Documentation

Maintain exactly these five everyday documents in `docs/`:

- `README.md`: navigation and documentation maintenance rules.
- `architecture.md`: module responsibilities, interfaces, and compatibility boundaries.
- `development.md`: setup, builds, tests, live operation, and collaboration workflow.
- `training.md`: experiment contracts, evaluation, recovery, models, and artifact management.
- `status.md`: current verified results, open issues, and next steps.

Update the appropriate existing document in place. **Do not create a new dated analysis, audit, progress, launch, cleanup, closeout, or index document for each task.** Keep the root README short and link to these documents. Do not add separate directory READMEs that duplicate them. Licenses and immutable review text needed by a verified contract are exceptions, not everyday documentation.

Task-specific exports and scratch analyses belong in `local/reports/<task>/`, not `docs/` or the root. On task completion, integrate the useful conclusion into the maintained documents and retire the temporary material. Do not accumulate both a long analysis report and multiple summaries of the same finding.

## Local artifacts and experiment lifecycle

- `model/`: inference weights and their identity JSON only; no training checkpoints or optimizer state.
- `local/runs/`: the current parent lineage and experiments still needed for the next development stage, with checkpoints, config, manifests, metrics, and evaluations.
- `local/archives/`: source packages and receipts needed by those current experiments.
- `local/audits/`: current rule evidence, private stock-game sources, and trajectories needed for verification.
- `local/build/`: compiler/dependency tooling, native/Oracle builds, and caches. Use existing pytest/ruff cache locations. Build helpers may temporarily generate `build/` or `src/*.egg-info`; inspect and retire stale outputs at task end without breaking installed package metadata.
- `local/operator/`, `local/logs/`, `local/reports/`: current machine settings/operator commands, runtime logs, and active diagnostics. Retire superseded scripts and completed diagnostics.

Keep these artifacts ignored by Git. Do not recreate parallel artifact roots such as `runs/`, `checkpoints/`, `artifacts/`, or `validation-results/`. If an external application creates its own files, handle them as generated output rather than project source.

Archive completed older experiments outside the repository under `D:\SLS-archive\<date>-<purpose>\`. Keep the currently adopted parent checkpoint, its recovery state when needed, selected candidates, source package, config, identity, and evaluation evidence accessible locally. Intermediate snapshots and superseded workcopies need not remain in the active project merely because they once existed.

Before moving or removing artifacts, inspect actual code/config/test references and distinguish current dependencies from historical records. Verify resolved source/destination paths, preserve original bytes, record original/new paths and SHA256, and verify the moved files. Do not permanently delete unique models, raw packages, or evidence as routine cleanup. A move on the same disk is not an independent backup or freed disk space.

Do not move active game recovery journals or backups. Confirm journal status is `RECOVERED` with no recovery failures before retiring completed game batches; do not infer recovery from a directory name or completion marker alone. Preserve original journal bytes and document that historical absolute paths require restoring the original layout before reuse.

Portable immutable evidence belongs in `tests/fixtures/evidence/` only when needed for project verification. Preserve hashes and update executable references when paths change. Do not rewrite checkpoint metadata, raw evidence, or historical experiment identity to make a source gate pass. A completed plan may be registered as historical only with its unchanged original plan and matching completion manifest; it must remain unavailable for resubmission.

## Finishing a task

Check for loose files, duplicated explanations, stale preparation/operator scripts, and generated caches or packaging remnants. Update the fixed documentation and archive retired outputs instead of adding another cleanup report. Run validation appropriate to the changes and report missing private evidence as a limitation rather than a pass. Recheck path references and hashes when moving functional artifacts. Keep evolving experiment results out of this file.

## GitHub and server workflow

The normal workflow is:

**local development → local validation → push to GitHub → server pulls from GitHub → server-side execution/training**

The agent works on the local repository.

When work needs to be performed on the NUS server:

1. Complete the necessary code/configuration changes and appropriate validation locally first.
2. Commit and push the required changes to GitHub.
3. Provide the human user with exact commands to run on the server, including pulling the corresponding changes from GitHub and performing the required server-side work.
4. The human user executes those commands on the NUS server and pastes the output back.
5. Inspect the pasted output and determine the next step.

Do not assume local changes are already present on the server. Server-side work should follow the local change → GitHub push → server pull workflow.

## Independent audits

When asked for an independent audit, verify claims against source, native implementation, tests, artifacts, and reproducible runtime evidence. Documentation and earlier audit prose may explain intent but are not proof.
