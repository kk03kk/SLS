# Base-game content registry

Step 3 establishes the denominator for the full-game parity project. The
committed machine-readable artifact is
`spirecomm/content/registry.json`; it is generated from the constant enums in
the pinned `sts_lightspeed` commit recorded by `reference_build.json`.

## Status semantics

- `declared` / `upstream`: the ID exists in the pinned source inventory. This
  makes no claim that this repository has audited its behavior.
- `partial` / `unit`: this repository has executable behavior and project unit
  tests for the named slice, but has not established original-game parity.
- `implemented`: reserved for complete semantics for that content item.
- `oracle_trace`: reserved for evidence captured from the frozen original game
  build. It is stronger than a unit test and is primarily added in Steps 5-9.

An item must not be promoted merely because `sts_lightspeed` has a switch case
for it. Promotion requires evidence owned by this repository.

## Rebuilding

The ignored `.native-build/sts_lightspeed` checkout must be present at the
pinned commit. Generate or verify the artifact with:

```powershell
python scripts/build_content_registry.py
python scripts/build_content_registry.py --check
```

The generator rejects a different checkout commit, records SHA-256 hashes for
every source header, sorts by native ordinal, and writes deterministic JSON.
The runtime loader validates unique IDs/ordinals and all status values.

## Scope

The registry contains the seven independently selectable base-game content
families: characters, cards, relics, potions, monsters, encounters and events.
Internal mechanics such as powers, stances, or monster move states are tracked
by the Step 4 mechanic matrix rather than being counted as content objects.
