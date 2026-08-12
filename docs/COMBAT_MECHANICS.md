# Generic combat mechanics matrix

Step 4 tracks shared rules independently from cards, enemies and relic counts.
The machine-readable source is `spirecomm/simulator/combat_mechanics.json`.

Statuses are deliberately conservative:

- `unimplemented`: the shared primitive is absent.
- `partial`: executable code exists, but some combinations, ordering rules or
  original-game evidence are missing.
- `implemented`: the scoped primitive has complete project-owned evidence.

Evidence levels are `none`, `upstream`, `unit`, and `oracle_trace`. Upstream
code alone never proves parity. A Step 4 exit requires every matrix row to be
`implemented`, focused project tests for every row, and original-game traces
for ordering-sensitive boundaries. Content-specific exhaustive coverage is
then performed in Steps 5 and 6.

The initial audit identified four absent primitives. Mantra threshold behavior
and core stance transitions are now implemented from the frozen original JAR;
orb slots, channel/evoke, and passive orb/Focus processing remain absent. It also identifies
ordering risks in action cleanup, just-applied powers, relic damage modifiers,
retain/ethereal cleanup, and stance hooks. These are now visible gates rather
than implicit assumptions.
