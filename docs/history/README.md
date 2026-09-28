# Historical record

These files preserve dated audits, failed experiments, investigations and earlier operating plans. They describe the source and runtime state at the time they were written; use the root [README](../../README.md) and [repository map](../repository-map.md) for current entry points. Checkpoint names, hashes and old commands remain here for traceability. Do not run an old training command without checking its source and contract.

## A20 Act1 progression

- [Completed 46M → 60M stage and selected 56M champion](../results/a20-act1-60m-stable/README.md) — current result, 1,585/2,048

- [30M audit](a20-30m-audit.md)
- [40M audit and 50M plan](a20-40m-audit-and-50m-plan.md)
- [50M audit and 46M champion](a20-50m-audit.md)
- [First 60M optimization plan](a20-60m-optimization-plan.md) — experiment stopped after regression
- [54M recovery plan](a20-54m-recovery-plan.md) — completed, not promoted
- [Post-54M audit](a20-next-stage-audit.md)
- [Simulator audit, 2026-09-23](a20-simulator-audit-2026-09-23.md)

## Earlier training and environment work

- [Act1 environment closeout](act1-environment-closeout.md), [event observation repairs](event-observation-repairs.md), [observation simulator ledger](observation-simulator-work.md)
- [A0 Act1 5M instructions](training-act1-5m.md), [5M audit](act1-v4-5m-audit.md), [20M continuation](act1-v4-20m-continuation.md)
- [A0/FullRun 10M–15M](training-10m-15m.md), [FullRun v3 migration](fullrun-v3-migration.md), [old NUS guide](nus-training-zh.md)
- [Act1 job recovery](act1-job-833382-recovery.md), [import repair](job-837265-import-fix.md)
- [Code audit, 2026-08-31](code-audit-2026-08-31.md), [project audit, 2026-09-22](project-audit-2026-09-22.md)

The [2026-09-05 independent audit](../audits/2026-09-05/report.md) remains in its original evidence directory.

The earlier stock-game audit paths under `decompiled/` refer to the original layout. The ignored projection now lives under `local/audits/stock-decompilation-tree/`; see its [generation metadata](../audits/stock-decompilation/README.md).
