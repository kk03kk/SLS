# Original truth and local replay workflow

The authoritative runtime uses `D:\Anaconda\envs\DL\python.exe` and four
components: ModTheSpire, BaseMod, CommunicationMod, and SpirecommParity.
SuperFastMode is excluded from authoritative captures.

## Short live capture

Build the instrumentation and prepare the recoverable runtime:

```powershell
D:\Anaconda\envs\DL\python.exe tools/build_oracle.py
D:\Anaconda\envs\DL\python.exe tools/prepare_original_runtime.py --max-steps 10
```

The prepare command creates a write-ahead journal under
`validation-results/runtime-journals`, protects the user's Ironclad saves and
ModTheSpire configuration, installs the current Oracle, and configures
CommunicationMod. A later prepare automatically recovers any journal left by a
crashed process. Recovery can also be requested explicitly:

```powershell
D:\Anaconda\envs\DL\python.exe tools/prepare_original_runtime.py --recover
```

Full truth bundles are ignored local artifacts under
`validation-results/truth/<run-id>`. A clean worktree can be required for a
release capture with `tools/validate_full_run.py --require-clean`.

## Offline daily loop

Replay immutable wire payloads and native checkpoints without launching the
game, then extract the first difference:

```powershell
D:\Anaconda\envs\DL\python.exe tools/replay_truth.py <bundle> --from-step 0
D:\Anaconda\envs\DL\python.exe tools/extract_regression.py <bundle> --step N --issue issue-id
```

Replay exits with 0 for a match, 1 for a reproduced difference, and 2 for an
invalid or tampered artifact. Deterministic gzip regression fixtures under
`tests/fixtures/regressions` are the only truth-derived artifacts intended for
Git.

## Official autosave segment replay

Every entered room captures an official autosave and paired native exact
checkpoint. Prepare a local Original replay from the nearest room:

```powershell
D:\Anaconda\envs\DL\python.exe tools/prepare_original_runtime.py `
  --segment-bundle <bundle> --anchor <anchor-id> --to-step <step>
```

`parity_continue` only triggers the stock Ironclad Resume path at the main
menu. It never constructs player, map, or RNG state. The resume hash permits
one explicit v1 normalization: after floor zero, the destroyed and never again
consumed Neow RNG stream is absent from an official-save load. Every other
public-state, live-RNG, and continuation difference is fatal.

Long Original FullRuns are reserved for initial corpus acquisition, milestone
coverage, and final acceptance. Routine fixes use truth replay, a minimal
fixture, and only the nearest autosave or registered Oracle scenario when live
evidence must be refreshed.
