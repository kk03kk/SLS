# Transient, Reptomancer and all-Darkling encounter endings

Nine completed stock A20 TheBeyond runs131200171–179, three per mechanism.
All stable combat boundaries before the ending match strict state/actions/RNG.
The final comparison is explicitly a subset: player and monster HP/maxHP,
encounter-won status and full RNG. It does not certify reward content, terminal
card piles, corpse powers, room continuation or a natural complete run.

- Transient: initial Anger reduces its first attack through Shifting; six A20
  turns and Fading terminate with stock player HP4622 after Burning Blood.
  Native prefix and ending subset match without a production rule repair.
- All three Darklings: three lethal Angers make the encounter end without
  revival. Prefix and ending subset match without a new production rule repair.
- Reptomancer: stock queues living Dagger suicides on leader death. The old
  native ends with surviving positive-HP Dagger objects. Controlled restoration
  also used a hallway room and therefore omitted the elite relic RNG draw.

The isolated elite-room correction fixes RNG only. The production correction
queues minion deaths in stock order and retains those DAMAGE actions through
victory cleanup. An intermediate fix still failed because its death actions were
filtered out; its source/binary and failed tests remain preserved. Final source:
`0fd7d4c6d4a78d6078cb9169cb679212b1734ce5a47f575e523787551bc42812`.
All nine ending subsets now match;33 focused/adjacent tests pass. Full Python:
1338 passed, one expected skip, four warnings. The native build/import passes.
285 fixed normal native boundaries remain unchanged, a regression control only.

Original Reptomancer.die, SuicideAction.update and GameActionManager's preservation
of DAMAGE actions provide independent bytecode evidence. This does not establish
all death-relic interactions. No PPO/model/reward/schema changes or automatic
source compatibility waiver were made. See local separate migration record.

Executed Oracle r25/1.3.22 SHA
`6c6ffe24ef6ed82a8cae68348dd4f14689f2bfd01d4e685de1b9f82fe8740fcd`.
Both failed-r1 and completed-r2 batches recovered63 protected files, independently
rehashed unchanged. The initial batch had a wrong encounter ID and remains an
execution failure despite its six partial traces. A new pre-launch manifest
encounter check prevents this failure before the game launches.

Current production Oracle smoke passes; it checks production/validation isolation
and normal startup, not a natural-win rate. Raw captures, original bytecode,
source archives, failures, intermediate/final diagnostics and compact fixture
extraction remain local under `local/audits/fullrun-parity-20261007`.
