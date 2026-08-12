# Slay the Spire simulator parity contract

## Canonical target

The simulator targets the unmodified gameplay content of the Windows Steam
build identified in `reference_build.json`:

- Slay the Spire desktop build dated `12-18-2022`
- Steam app `646570`, build ID `10180494`
- four base-game characters
- Ascension 0 through 20
- Acts 1 through 4, including the Corrupt Heart

The original-game oracle is launched with BaseMod, CommunicationMod, and the
project's content-free parity instrumentation mod. The latter may expose RNG
state and isolate the explicitly controlled `math_seed`, but may not alter
content, balance, decisions, or other gameplay behavior. Until that
instrumentation is installed and reports its configuration, a trace is useful
for behavioral comparison but is not eligible for exact RNG parity.
SuperFastMode may be used only in separately labelled performance experiments
after its traces pass the same parity checks.

The game language is not part of gameplay semantics. Stable internal IDs are
canonical; localized display strings are diagnostic metadata.

## Deterministic equivalence

Given the same canonical build, character, ascension, game seed, controlled
`math_seed`, starting state and sequence of legal player decisions, the
original game and simulator must agree
at every player decision boundary on:

1. the complete ordered semantic legal-action list;
2. all gameplay-relevant player, card-pile, monster, potion, relic and power
   state;
3. map, room, reward, shop, event and run-progression state;
4. RNG-dependent choices and their ordering;
5. rewards, termination flags and final outcome.

Equivalence includes the original RNG algorithms, derived stream seeds, stream
ownership, call order, bounds and any gameplay-relevant RNG consumption. A
matching distribution is not sufficient.

The additional `math_seed` is required by an original-game determinism defect:
LibGDX's global `MathUtils.random` is time-seeded and shared by presentation
code, but the Courier's colored-card restock also consumes it through
`CardGroup.getRandomCard(..., useRng=false)`. Therefore the base game does not
produce a unique gameplay trajectory from its displayed game seed alone. The
canonical oracle instrumentation must seed and isolate this stream; a run that
does not report its `math_seed` is not eligible for exact differential parity.

## Decision boundary

A decision boundary is a state where the game has finished resolving all
automatic actions and is ready to accept a semantic player action. Intermediate
animation frames and partially resolved action queues are not agent-facing
states and are not compared.

Every accepted simulator action must correspond to an advertised original-game
action. Unsupported mechanics must fail explicitly; they must never silently
approximate a result or emit an illegal command.

## Intentionally excluded presentation state

The following may differ when they cannot affect later gameplay:

- animation, VFX, audio, render position and wall-clock timing;
- logs, timestamps and platform window state;
- localized presentation text when the stable internal ID matches;
- opaque process-local object identities such as Java references or card UUIDs,
  provided identity relationships relevant to gameplay are preserved;
- achievements, telemetry and Steam UI state.

If an apparently presentational value is later shown to influence gameplay or
RNG consumption, it immediately becomes parity-critical.

## Verification policy

Parity is established incrementally, never deferred until all content exists.
Each implemented mechanic requires:

- deterministic unit or invariant tests;
- at least one evidence source recorded in the content registry;
- original-game differential traces when CommunicationMod exposes the fields;
- regression coverage for every fixed divergence.

No finite test set proves equivalence for every seed and policy. Release gates
therefore combine fixed-seed golden traces, randomized legal policies,
property-based tests, checkpoint branching, RNG call auditing and first-
divergence reporting.

## Version changes

`reference_build.json` is immutable for this target. If Steam changes any
canonical binary hash, verification must fail until the change is audited. A
new supported game build receives a new manifest and its own parity results;
the existing baseline is not overwritten silently.
