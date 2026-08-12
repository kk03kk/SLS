# Deterministic kernel and RNG audit

Status: Step 2 complete. This inventory records the deterministic-kernel gate,
its evidence, and work deliberately routed to later content/conformance steps.

## Algorithms already present

The upstream engine implements the base game's xor-shift `Random` state as
`counter`, `seed0` and `seed1`, including MurmurHash3 seed expansion and the
integer, long, float, double and boolean range APIs. It separately implements
Java's 48-bit linear-congruential `java.util.Random` behavior for collection
shuffles seeded from `shuffleRng.randomLong()`.

These algorithm implementations are necessary but not sufficient: ownership,
initialization, call ordering and deliberate throwaway calls must also match.

## Full-run streams found in GameContext

| Stream | Intended ownership | Combat checkpoint today |
|---|---|---:|
| `aiRng` | enemy move selection | yes |
| `cardRandomRng` | in-combat random card/target effects | yes |
| `cardRng` | run card rewards and transformations | no |
| `eventRng` | event selection | no |
| `mathUtilRng` | miscellaneous game math; upstream notes time-based behavior | no |
| `merchantRng` | shop generation | no |
| `miscRng` | miscellaneous combat/run choices | yes |
| `monsterHpRng` | monster HP rolls | yes |
| `monsterRng` | encounter selection | no |
| `neowRng` | Neow choices | no |
| `potionRng` | potion drops and random potion selection | yes |
| `relicRng` | relic tier and pool selection | no |
| `shuffleRng` | draw-pile shuffles | yes |
| `treasureRng` | chest selection | no |

Map generation creates a derived local stream from `seed + actOffset`; it must
be reproducible even though it is not retained after map construction.

## Current proven capability

The native Python bridge exports and restores all six battle streams:
`ai`, `monster_hp`, `shuffle`, `card_random`, `misc` and `potion`. Existing
checkpoint tests prove JSON round-trip, exact stream-state restoration and
identical next-step behavior for a representative multi-enemy battle.

The base game's compiled `com.megacrit.cardcrawl.random.Random` and LibGDX
`RandomXS128` classes were selectively audited from the pinned desktop JAR.
No decompiled game source is stored in this repository. A read-only Nashorn
oracle (`scripts/rng_oracle_probe.js`) executes the original classes and emits
version-pinned test vectors. The native `rng_probe` matches five representative
64-bit seeds across nine API calls, including inclusive integer ranges, bounded
longs, booleans and floats. It also matches the initial and final `counter`,
`seed0` and `seed1` states exactly. The frozen outputs live in
`tests/fixtures/rng_vectors.jsonl` and are enforced by
`tests/test_rng_parity.py`.

`spirecomm/checkpoints.py` now defines the versioned full-run checkpoint
envelope. It rejects checkpoints unless all 14 retained streams contain a
valid `counter`, `seed0` and `seed1`, unless all currently identified ordered
event, relic, card and encounter pools are present, and unless the derived map
RNG inputs are explicit.

The native `LightspeedRunState` bridge now exports and restores those 14
streams directly from `GameContext`, together with the ordered pools. A native
round-trip test advances every stream, restores the saved state, and proves
that all next outputs agree. Map generation records the pinned algorithm,
base/derived seeds, act, ascension, burning-elite flag and generated map
fingerprint; unsigned 64-bit seed wraparound is covered by a boundary test.

Selective audit of the pinned original `ShopScreen`, `AbstractDungeon`,
`CardGroup` and LibGDX `MathUtils` bytecode resolved the `mathUtilRng` question.
Courier colored-card restocking deliberately requests `useRng=false`, causing
`CardGroup` to consume the global `MathUtils.random`. That generator is created
with `new RandomXS128()` (time-seeded) and is also consumed by presentation
logic. Consequently a displayed game seed alone does not determine every
gameplay outcome. The parity episode identity now includes a controlled
`math_seed`; `LightspeedRunState.reset` accepts it explicitly and tests prove it
changes only the `math_util` stream.

The content-free `oracle_mod` now performs the original-side isolation. It
patches only the three `CardGroup.getRandomCard(..., useRng=false)` overloads,
uses the original game's `Random` implementation, resets from the explicit
`spirecomm.math_seed`, and appends all 14 retained stream states plus
`math_seed` to CommunicationMod JSON. A live pinned-build smoke test verified
the mod loaded, Python received the fields during combat, and the original and
native `math_util` initial states matched exactly for seed `123456789`.

The same audit found an upstream semantic mismatch: Courier restocking selected
from all cards of the rolled rarity, while the original preserves the purchased
card's attack/skill/power type. The pinned patch now calls the type-and-rarity
pool and performs the original common-power fallback. A native probe verifies
all three colored card types and the exact affected streams (`card`,
`math_util`, and `merchant`).

Trace schema v3 records available RNG state at every semantic action boundary.
Replay stops at the first mismatching stream and field (`counter`, `seed0`, or
`seed1`) before reporting downstream gameplay drift. The associated trace step
and semantic action provide the call-site interval for focused audit.

Full-run checkpoint schema v2 additionally requires core player/progression
state, complete screen state and the ordered legal-action list. The native
bridge delegates enumeration, validation and execution to upstream
`GameAction`, rejects unadvertised actions, and explicitly rejects a checkpoint
whose continuation is not serializable. Exact restore-and-take-the-same-action
tests now cover combat plus Neow, reward and map decisions.

## Routed to later roadmap steps

1. Exhaustive event-specific fields and open card-choice callback semantics are
   content/run-layer work for Steps 3, 4 and 7. Until implemented they are
   marked `complete=false` and strict export/load rejects them.
2. Per-RNG-call source labels and broad original-game traces across run screens,
   save/load and many seeds are conformance work for Step 9. Step 2 already
   reports the first stream/field divergence at semantic action boundaries.
3. A unified full-run Gymnasium environment is Step 7. Step 2 exposes the run
   kernel and legal actions without prematurely freezing the final observation
   vocabulary before the content registry exists.

## Step 2 exit work

- define a versioned full-run checkpoint schema containing all retained RNG
  streams, the map derivation and ordered pools (done);
- expose the deterministic full-run kernel through a bridge without changing
  the shared Agent-facing action contract (done; upstream run actions exposed,
  unsupported continuations rejected explicitly);
- add deterministic algorithm vectors independent of combat content (done for
  the core `Random`/`RandomXS128` API and Java collection shuffling);
- add RNG call-site labels/counters suitable for first-divergence diagnostics
  (done at semantic action boundaries; deeper per-call labeling is Step 9);
- prove save/restore and same-action branching across combat and run screens
  (done for combat, Neow, rewards and map decisions);
- resolve or explicitly isolate `mathUtilRng` before declaring the kernel
  deterministic (done in both simulator and the original oracle; broad
  Courier/save-load differential coverage remains).
