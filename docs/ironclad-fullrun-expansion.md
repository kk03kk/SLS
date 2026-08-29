# Ironclad FullRun expansion status

This document reports evidence generated from source. It is not a readiness
authority. The machine-readable authorities for this development line are:

- `configs/validation/ironclad_fullrun_inventory.json`
- `configs/validation/ironclad_fullrun_semantic_audit.json`

Regenerate and verify them with:

```bash
python tools/generate_fullrun_inventory.py --check
python tools/audit_fullrun_semantics.py --check
```

## What is executable now

The single native state machine already traverses Acts 1, 2, 3, and 4; exposes
the Ruby, Emerald, and Sapphire keys through canonical actions; fights the Act
4 elite and Heart; and schedules the second distinct Act 3 boss at A20. The
canonical structural integration test drives both A0 and A20 from Neow to a
Heart terminal using only public observations and semantic actions. Combat is
skipped in that test, so this is `NATIVE_EXECUTED` route evidence, not Original
parity.

The curriculum registry contains A0-A20 `FULLRUN` and `HEART` profiles. A
`FULLRUN` profile accepts the natural Act 3 ending or a Heart victory; a `HEART`
profile fails if the run ends before Act 4. Existing A0 Act 1/2/3 profiles are
unchanged.

## Reachable closure and the known difference

The Original-theoretical A0 Heart closure currently inventories 344 cards, 151
relics, 33 potions, 52 events, 63 encounters, and 65 monsters. At A20 it has
345 cards, 151 relics, 33 potions, 51 events, 63 encounters, 65 monsters, and 20
source-indexed ascension modifiers.

The current native closure is smaller: 130 cards at A0 (131 at A20) and 150
relics. Original `PrismaticShard` makes ordinary rewards call
`CardLibrary.getAnyColorCard`; native explicitly sets
`GameContext::disablePrismaticShard = true`. Consequently 214 theoretically
reachable reward cards and Prismatic Shard itself cannot occur in native runs.
This is recorded as `DIFFERENCE`, not hidden as policy scope.

## Policy-transfer status

The old stage ledger is retained as historical exact-trajectory evidence. It
no longer blocks simulator curriculum work merely because later whole-run RNG
trajectories are incomplete. Promotion still requires exact public contracts,
deterministic probes, distribution checks for random mechanisms, and real-game
checkpoint evaluation; unsupported mechanisms remain explicit gaps.

The native structural path is useful for development but cannot promote a
stage. Promotion requires `SOURCE_MATCHED`, `NATIVE_VERIFIED`,
`ORIGINAL_VERIFIED`, and `ROUTE_VERIFIED` evidence with zero differences and
zero blocked entries for that stage.

## Exact resume versus curriculum transfer

`--resume` remains exact resume. It requires an identical profile, worker
count, PPO contract, policy-transfer gate, state-corpus digest, native digest, RNG state, and environment
state. This branch does not change any path included in the current native
source digest, so an Act 1 checkpoint retains its exact-resume contract.

`--warm-start CHECKPOINT` is a separate transfer operation. It strictly checks
the model architecture, encoding schema, vocabulary hash, and state-dict shape,
then loads model weights only. It intentionally does not load optimizer, RNG,
worker, environment, readiness, or source contracts. `--resume` and
`--warm-start` are mutually exclusive.

Example, only after a target curriculum has its own readiness-gated config:

```bash
python tools/submit_slurm.py pilot \
  --python /home/h/hengzhi/venvs/sls/bin/python \
  --config configs/train/<ready-target>.toml \
  --workers 32 \
  --warm-start runs/act1-pilot-v2/latest.pt
```

There is intentionally no production FullRun TOML yet: adding one before its
stage lock exists would provide an unsafe training path around readiness.

## Minimum next validation work

1. Capture deterministic Original and native Act 1-to-Act 2 boundaries for all
   three Act 1 bosses and promote only matching canonical transitions.
2. Close Act 2 encounter/monster/event semantics, then its boss transition and
   route ledger; issue an Act 2 lock only at zero difference/blocked.
3. Repeat for Act 3, the three-key/Act 4 route, and Heart.
4. Validate each A1-A20 modifier against controlled Original/native scenarios,
   including A20's second Act 3 boss.
5. Implement and validate Prismatic Shard plus its any-color card closure, or
   keep FullRun release blocked.

Original/CommunicationMod-dependent captures cannot be completed by the local
native-only test environment. They must be run in the existing Windows Original
validation setup; native structural success must not be substituted for them.
