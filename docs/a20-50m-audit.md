# A20 Act1 50M result and repository audit

Audit date: 2026-09-20. Audited Git revision: `de2c941` plus the local 50M
result archive. This audit treats implementation, tests, build output and
machine-readable run artifacts as evidence; older plans and reports are not
accepted as proof by themselves.

## 50M result

The archive `runs/archives/sls-ironclad-a20-act1-50m-final.tar.gz` has SHA256
`02b54727220d61b46c8c7dcb8c22f1af3667d63d0b8155ecbb93b4a8ea77544f`.
Its member paths are relative and traversal-free. The archived training config
is byte-identical to the LF content in Git, and the manifest records the current
native source digest
`653181e0795c44734fb8f17de1c1a7981869737c1cc3c2868c8f420ff6afbcc6`.

- Training completed at 50,003,968 environment steps and update 2,014.
- The selected checkpoint is the 46,006,272-step periodic best, with 388/512
  fixed-seed clears (75.78%). Its actual SHA256 matches both best metadata and
  final-evaluation metadata:
  `2db39fde737445b109d31cc522dde42dfe2c7372b34007e108ce702b03acc70d`.
- The selected checkpoint cleared 766/1,024 new final-evaluation seeds:
  74.80%, with a 95% Wilson interval of 72.06%–77.37%.
- Final boss success rates were 72.70% for Hexaghost, 78.57% for Slime Boss and
  73.20% for The Guardian.
- Backend errors, backend truncations, timeouts, step limits, cycle limits and
  self loops were all zero. The promotion gate passed.

The result demonstrates the recorded simulator workload, not universal parity
with every stock-game branch. `final.pt` is the terminal training state; the
reported final evaluation intentionally uses the earlier selected best.

## Repository verification

- `python -m pytest -q`: 664 passed, 1 skipped. The skip verifies that a
  historical policy is rejected by the current encoding. Four warnings are
  expected runtime/provenance-rebind warnings raised by tests that exercise
  that contract.
- `python -m ruff check .`: passed.
- `python tools/generate_policy_vocabulary.py --check`: passed with vocabulary
  hash `04df4dc504ccf10d389b7b0e798ebc7a1ea2036edfc273a5cf5b11071b3b8ed0`,
  matching the run manifest.
- `python tools/build_native.py --jobs 4`: a clean source rebuild completed and
  the resulting module imported successfully.
- After restoration and the clean native rebuild, the simulator, structure and
  native-build regression subset passed again: 106 passed.
- `python -m pip check`, Python bytecode compilation, `git diff --check`, tracked
  file presence/case-collision checks and repository object validation found no
  dependency breakage, syntax failure, whitespace error, missing tracked file,
  path collision or corrupt Git object.

The native build still emits numerous compiler warnings inherited from the
vendored simulator: unused declarations/parameters, signedness comparisons,
two class/struct forward-declaration mismatches, one variable-length array
extension and warnings in the vendored nlohmann header. They do not prevent the
supported Windows build or the test suite, but warning cleanup remains technical
debt. A `TODO` also remains in the legacy search-agent action code; the current
RL environment is covered by the canonical semantic-action tests rather than
that heuristic search path.

## Cleanup performed

- Moved the 50M archive from the repository root into `runs/archives/`, the
  documented location for downloaded server archives.
- Removed regenerable root `build/`, pytest/Ruff caches, Python `__pycache__`
  trees and `src/sls.egg-info`.
- Removed the superseded pre-30M `a20-act1-local-plan.md`; dated result audits,
  recovery records, models, server archives and decompiled stock sources were
  retained because they remain provenance or reproducibility inputs rather than
  disposable cache.
- Updated README and repository-map claims that still described 30M/40M as the
  current stage.

During cache removal, an explicitly named nested `local/audits` target was
unexpectedly collapsed by `git clean -X` to the ignored `local/` root. All nine
server archives were immediately re-extracted, restoring archived A0/A20
checkpoints, manifests, metrics, preparation records and Slurm logs (including
the 40M and 50M runs), and the native build environment was rebuilt. Exported
models and `runs/` were never affected. Local-only, unarchived material could
not be recovered: the prior local original-game/Mod dependencies, development
logs/reports and the small canary/diagnostic/evaluation/stock-audit directories.
Those files were ignored and not part of the reproducible source tree, but any
unique captured evidence in them is no longer available locally. `local/`
measured about 2.94 GiB before cleanup and about 1.73 GiB after archive
restoration plus the native rebuild; the difference includes both deliberately
regenerated build/cache material and unrecoverable local-only material.
