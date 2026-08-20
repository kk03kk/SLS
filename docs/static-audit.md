# Static audit boundary

This document records what the repository can prove without launching Slay the
Spire. It deliberately does not claim FullRun parity: that acceptance requires
the locally owned game, ModTheSpire, BaseMod, CommunicationMod, and the Oracle
Mod running against the same seeds and semantic actions.

## Code-proven facts

- The canonical executable environment currently accepts Ironclad only.
- The generated content registry contains 370 cards, 180 relics, 43 potions,
  65 monsters, 63 encounters, 56 events, and four character IDs.
- Every registered C++ card/relic/non-empty-potion game ID is present in the
  committed decompiled Java reference tree.
- All 75 playable Ironclad cards and all 35 obtainable colorless reward cards
  have an explicit native attack/skill/power execution case where one is
  required. Status and curse lifecycle cards are not all played through those
  switches.
- Unsupported playable-card dispatch now throws in Release builds instead of
  silently doing nothing.
- Java-compatible seed text conversion and native RNG edge behavior have local
  regression coverage.
- The FullRun checkpoint captures run RNG streams, ordered pools, map state,
  player/inventory state, screen continuation, combat state, card identities,
  stasis slots, summon/split display history, and combat choice state. States
  containing non-serializable callbacks are reconstructed from seed plus the
  canonical action history.
- A deterministic checkpoint walk currently restores and continues identically
  across run screens and combat boundaries for the committed regression seed
  set. The broader local audit also completed 30 seed walks.
- Original and Simulator adapters emit the same canonical combat-card reward
  shape, including one action per card and a folded skip action.
- PPO checkpoint tests reproduce the next optimization update exactly, including
  model, optimizer, RNG, and worker environment state.

Run the machine-readable checks with:

```bash
python tools/audit_static.py
python -m pytest
```

## Issues fixed by this audit

- Internal Java IDs such as `Ghostly`, `Venomology`, `Yang`, and
  `WingedGreaves` are normalized from generated registry data rather than an
  incomplete hand-maintained list.
- Java `Random.nextInt` overflow/rejection semantics, invalid bounds, and seed
  string zero/invalid-character handling were corrected.
- Native combat reward generation now assigns an index to every gold reward;
  multiple gold rewards no longer collapse to duplicate semantic actions.
- Reward-card candidates and skip behavior are folded consistently across the
  Original and Simulator backends.
- Checkpoint continuation now preserves Discovery RNG fields, rest-room return,
  treasure relic tier, temporary-card UUID allocation, Bronze Automaton stasis
  cards, and large-slime split display state.
- Stale fields from reused combat/event structures are normalized out of the
  checkpoint contract.

## Static gaps that remain

These are facts, not proof of divergence:

- `GameContext::disablePrismaticShard` is `true`; Prismatic Shard's cross-color
  reward-pool behavior is therefore intentionally absent and is a known
  Simulator/original difference if that relic is obtained.
- The native source still contains 120 `assert(false)` sites and many inherited
  `TODO` comments. Most assertions are invalid-input/internal-invariant branches,
  but their mere presence cannot prove the branch unreachable in every FullRun.
- 244 registered cards have no explicit play switch. This includes non-Ironclad
  colored cards (outside the executable backend contract) plus status/curse
  cards whose behavior is implemented through lifecycle hooks. The number is
  not itself a missing-card count.
- Matching IDs and switch coverage prove wiring and reachability, not numerical
  or ordering parity for every card, relic, potion, event, monster move, or
  interaction.
- Decompiled Java is static evidence only. It cannot reveal differences caused
  by runtime patch order, CommunicationMod visibility timing, BaseMod behavior,
  or the actual installed game build.

## Inferences

- The checkpoint regression set exercises substantially more continuation state
  than a reset-only smoke test, so checkpoint determinism is credible for the
  covered paths. It is not an exhaustive state-space proof.
- The remaining highest parity risk is in compound ordering behavior: event
  phases, relic/power callbacks, death/summon cleanup, and rare card-selection
  continuations. Those paths combine mutable state with queued actions and are
  poorly established by identifier coverage alone.
- Non-Ironclad implementations are reusable simulator assets but are not part of
  the current Python environment promise.

## Required dynamic acceptance

After importing the local JARs, build the Oracle Mod and run the configured
corpus gate. Acceptance must compare canonical observations, candidate actions,
selected actions, RNG evidence, terminal outcome, and required screen coverage.
Any static finding that conflicts with the live original game is subordinate to
the live result.

The local-only files and expected locations are listed in `local-runtime.md`.
