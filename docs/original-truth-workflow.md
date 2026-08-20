# Original truth and local replay workflow

The authoritative runtime uses `D:\Anaconda\envs\DL\python.exe` and four
components: ModTheSpire, BaseMod, CommunicationMod, and SpirecommParity.
SuperFastMode is excluded from authoritative captures.

## Owned live capture

Build the instrumentation, then let the coordinator own the complete launch
and restore lifecycle:

```powershell
D:\Anaconda\envs\DL\python.exe tools/build_oracle.py
D:\Anaconda\envs\DL\python.exe tools/run_original.py capture --seed 0 --max-steps 10
```

The coordinator creates a write-ahead journal, protects the user's Ironclad
saves and ModTheSpire configuration, installs the current Oracle, writes the
strict three-mod list, launches ModTheSpire through the game's `javaw.exe`,
waits for the validator result, stops only its owned Java process, and verifies
the restored file hashes. It always uses `--skip-launcher`; `--skip-intro` is
enabled by default and can be disabled with `--no-skip-intro` for the one-time
equivalence control. SuperFastMode is never loaded.

An unfinished journal is recovered before any new launch. Recovery refuses to
touch files while the recorded, identity-matching Java process is alive, and a
`RECOVERY_FAILED` journal blocks later runs. Manual recovery remains available:

```powershell
D:\Anaconda\envs\DL\python.exe tools/prepare_original_runtime.py --recover
```

CommunicationMod receives POSIX-style Windows paths because Java properties
would otherwise consume backslashes as escapes. Python stdout remains reserved
for protocol commands; diagnostics and results go to stderr or ignored files.

## Durable truth bundle v2

Full truth bundles are ignored local artifacts under
`validation-results/truth/<run-id>`. Recording first uses `<run-id>.partial`.
Each raw boundary is appended and fsynced immediately; anchors are hashed when
created. Finalization produces deterministic gzip artifacts and atomically
renames the directory. Recover a crashed recording with:

```powershell
D:\Anaconda\envs\DL\python.exe tools/recover_truth.py validation-results/truth
```

A recovered partial is explicitly incomplete and aborted, and can never count
for acceptance. V1 bundles remain immutable and readable. Current adapters and
canonicalizers always rerun from the stored raw payload instead of trusting old
canonical caches.

Every v2 manifest records capture/evidence class, Git and dirty-diff identity,
Oracle schema/hash, adapter/canonicalizer/policy hashes, native producer ABI and
SHA, launch arguments, JAR hashes, termination reason, anchors, and artifact
hashes. `MATCH`, `DIFFERENCE`, and `INCONCLUSIVE` are distinct states. A missing
map coordinate, intent, RNG stream, continuation field, or unknown screen is an
`EVIDENCE_GAP`; it is never replaced by a guessed default.

The current Oracle instrumentation contract is `spirecomm-parity-v10`. Besides
RNG, map, queue, adjusted monster-intent, and generated reward evidence, v5
records validation-only Discovery timing evidence. Stock `DiscoveryAction`
consumes card RNG once per retrieval render update, so paired and offline replay
use the captured update count without exposing it to policy observations or
semantic action metadata. Older schemas remain readable; missing required
evidence is explicitly inconclusive.

Version 7 also records bottled-card master-deck identity as continuation
evidence. Stock autosaves preserve only card ID, upgrades, and misc, so loading
a run with equivalent duplicate cards rebinds the bottle to the first matching
master-deck card. A `RESUMED_AUTOSAVE` segment records this lossy stock
normalization and aligns its native checkpoint to the observed resumed state;
live-run checkpoints retain the originally selected card identity.

Version 8 also records every serialized combat card's authoritative
`costForTurn`. Older combat payloads without it are evidence gaps: temporary
changes such as Liquid Memories cannot be reconstructed from the base `cost`.

Version 9 records active stock card `Soul` continuations with card UUID,
destination, dynamic cost, and completion state. These presentation objects
can mutate gameplay state later (notably `clearPowers()` after a rapid
discard-to-hand retrieval), so paired replay passes the observed consequence
as validation-only evidence rather than exposing animation state to policy.

Version 10 records the allowlisted stock event phase (`screenNum`, `screen`,
or `curScreen`). Phase-aware adapters use it to map visible UI positions to
stable semantic option IDs; an event without phase evidence is inconclusive.

## Offline daily loop

Replay immutable wire payloads and native checkpoints without launching the
game. Audit the whole local corpus before selecting one difference cluster:

```powershell
D:\Anaconda\envs\DL\python.exe tools/audit_truth_corpus.py validation-results/truth
D:\Anaconda\envs\DL\python.exe tools/replay_truth.py <bundle> --from-step 0
D:\Anaconda\envs\DL\python.exe tools/extract_regression.py <bundle> --step N --issue issue-id
```

Replay exits with 0 for a match, 1 for a reproduced difference, and 2 for an
invalid or tampered artifact. Deterministic gzip regression fixtures under
`tests/fixtures/regressions` are the only truth-derived artifacts intended for
Git.

The extractor emits an instrumentation request for an evidence gap, not a fake
simulator test. Adapter, simulator-adapter, one-step native transition, RNG,
and continuation fixtures preserve source provenance and deterministic bytes.

## Official autosave segment replay

Every entered room captures an official autosave and paired native exact
checkpoint. Prepare a local Original replay from the nearest room:

```powershell
D:\Anaconda\envs\DL\python.exe tools/run_original.py resume `
  --bundle <bundle> --anchor <anchor-id> --to-step <step> --continue-steps 8
```

For a searched or hand-reviewed local suffix, pass a versioned semantic plan
instead of a policy step count:

```powershell
D:\Anaconda\envs\DL\python.exe tools/run_original.py resume `
  --bundle <bundle> --anchor <anchor-id> --to-step <step> `
  --action-plan configs/validation/action-plans/<plan>.json `
  --action-plan-offset <already-executed-actions>
```

Every planned action must be an exact legal canonical candidate in both
backends. Candidate drift aborts the segment instead of falling back to an
index or another action. The derived bundle records the plan path, content
hash, source provenance, and action count.

At an idle combat command boundary, capture folds all active card `Soul`
continuations and then requires two identical non-advancing `state` snapshots.
This prevents delayed `clearPowers()` calls and half-updated action payloads
from becoming policy decisions, while avoiding frame advances on Discovery and
other selection screens whose rendering can consume RNG.

`parity_continue` only triggers the stock Ironclad Resume path at the main
menu. It never constructs player, map, or RNG state. The resume hash permits
two source-proven lifecycle normalizations: after floor zero, the destroyed
Neow stream is absent; after combat, the floor-local `monster_hp`, `ai`,
`shuffle`, `card_random`, and `misc` streams are reset on stock save load and
reset again when the next room is entered. Persistent RNG and every other
public-state or causally relevant continuation difference remain fatal.

An anchor is `NATIVE_ONLY`, `OFFICIAL_SAVE_AND_NATIVE`, or
`RESUME_VERIFIED`. A save is accepted only after its hash changes, stabilizes,
decodes, and matches seed/floor/character/room. A derived resume creates a new
`RESUMED_AUTOSAVE` bundle with source bundle and anchor provenance; old raw
payloads are never edited. Checkpoint producer SHA is provenance, while
checkpoint schema/ABI determine compatibility. Every restored or reconstructed
boundary is compared before its action suffix can continue.

## Survey and acceptance

Use a survey only to discover unseen Original protocols beyond a known parity
difference:

```powershell
D:\Anaconda\envs\DL\python.exe tools/run_original.py survey --seed 0 --max-act 1 --max-steps 30
```

`ORIGINAL_SURVEY` is never parity or acceptance coverage. Final acceptance is
checked separately:

```powershell
D:\Anaconda\envs\DL\python.exe tools/validate_acceptance.py validation-results/truth
```

Only complete, clean-commit, eligible `LIVE_FULLRUN` paired captures can pass.
Resumes, surveys, historical traces, and Oracle scenarios cannot raise that
coverage.

Long Original FullRuns are reserved for initial corpus acquisition, milestone
coverage, and final acceptance. Routine fixes use truth replay, a minimal
fixture, and only the nearest autosave or registered Oracle scenario when live
evidence must be refreshed.
