# Full-game simulator roadmap

This is the core project plan. A step is complete only when its exit gate is
met; pre-existing partial code does not make a step complete by itself.

| Step | Scope | Status | Exit gate |
|---:|---|---|---|
| 1 | Freeze build, scope and parity contract | COMPLETE | Pinned hashes verify; contract and exclusions are explicit |
| 2 | Deterministic kernel and multi-stream RNG | COMPLETE | Serializable state and audited RNG streams reproduce representative branches |
| 3 | Complete content registry | COMPLETE | Every base-game content ID has implementation and evidence status |
| 4 | Generic combat mechanics | PENDING | Shared action/power/card/choice hooks cover the mechanic matrix |
| 5 | Four characters plus colorless/status/curse cards | PENDING | Every base/upgraded card has semantic and original-trace coverage |
| 6 | All enemies, bosses, potions, combat relics and encounters | PENDING | Every combat composition reaches valid terminal states and passes parity corpus |
| 7 | Complete run layer | PENDING | Maps, rooms, events, rewards, shops, chests, rest sites and pools replay |
| 8 | Ascension 1-20 and Act 4 | PENDING | Every ascension delta, keys, burning elite, spear/shield and Heart are covered |
| 9 | Full differential conformance | PENDING | Cross-seed full-run corpus has no unexplained first divergence |
| 10 | Performance, vectorization and formal RL | PENDING | Stable parallel API and benchmark gates pass before policy training |

## Progress reporting

Every completed task report must include:

1. current roadmap step and status;
2. concrete files or behavior changed;
3. verification evidence and failures, if any;
4. roadmap progress (`completed steps / 10`);
5. the next bounded task.

Feature coverage percentages are reported only after Step 3 creates the full
denominator. Until then, milestone progress and inherited partial work are
reported separately to avoid false precision.

## Current baseline

- Roadmap: **3 / 10 steps complete**.
- Current step: **Step 3 is complete**. The deterministic registry records all
  **781** selectable base-game IDs: 4 characters, 370 cards, 180 relics, 43
  potions, 65 monsters, 63 encounters and 56 events. It is generated from and
  hash-bound to the pinned `sts_lightspeed` headers. Every entry has separate
  implementation and evidence status; 159 inherited entries currently have
  project unit evidence and remain explicitly `partial`, while 622 are only
  `declared`. No entry is claimed `implemented` or original-parity complete.
  The schema, regeneration check and status rules are documented in
  [`CONTENT_REGISTRY.md`](CONTENT_REGISTRY.md).
- Current work: **Step 4 is in progress**. Its versioned matrix contains 50
  shared mechanics across action queues, damage, resources, powers, card zones,
  choices, lifecycle, stances/orbs and randomness. The initial audit classifies
  46 as partial, three orb primitives as absent, and only the Java shuffle
  primitive as implemented. Generic boundary tests now cover
  attack/block, per-hit block consumption, direct HP-loss callbacks, the
  Intangible/block/Buffer/Torii/Tungsten damage order, hand
  capacity, Ethereal cleanup, action-queue filtering, block/energy turn
  transitions, just-applied power durations, Draw Reduction expiry, complete
  shared Retain/Ethereal/Pyramid/Equilibrium cleanup ordering, Calm
  exit, Mantra-to-Divinity entry, orb slot/FIFO behavior,
  all four base orb effects, Focus, and ordered orb checkpoint round-trips.
- Next bounded task: audit the complete player-turn and end-turn callback
  sequence, especially card, power and relic priority around hand cleanup.
- Inherited implementation: `OriginalSTSEnv`, shared Agent-facing API,
  `SimulatorSTSEnv`, all Ironclad card definitions, Act 1 encounter definitions,
  partial potion/relic semantics, checkpoints and differential trace tooling.
- Important qualification: inherited coverage has not yet passed the new
  full-game gates and remains partial until audited under Steps 2-9.
