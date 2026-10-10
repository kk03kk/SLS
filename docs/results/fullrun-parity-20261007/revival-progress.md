# Darkling and Awakened One controlled revival evidence

Six Darkling runs (131200132–137) match original direct state, legal actions and
RNG at all recorded boundaries: kill one edge/center Darkling, wait for revival,
then damage the revived target. Simultaneous all-Darkling death remains unverified.

Six Awakened runs (131200138–143) initially diverged. Two independent roots were
isolated, with original captures and failing intermediate replays preserved:

1. The isolated `LightspeedBattle` end-turn cleanup treated half-dead monsters as
   ordinary corpses and erased retained Strength/Regen. Excluding half-dead
   monsters fixes the three basic rebirth traces. FullRun does not use this
   cleanup: this was a probe error, not proof of a production rebirth error.
2. Production `BattleContext.onUsePowerCard` omitted Curiosity's Strength callback.
   Original `CuriosityPower.onUseCard` queues Strength for a power card. Adding
   the owner's queued Strength fixes the remaining three power-card traces.

All six now strictly match on source
`97fecc84af6595efb62b454671bc8c853f478c80dd0f157a13c168ad216c9abe`.
Oracle r17/1.3.14 SHA:
`d012737d1a0b4b84cc75d2fd114b2993e891d3cd6c55f31c30b7e027badb8ab9`.
Every actual game batch restored63 protected files, independently rehashed with
zero mismatches. Full Python suite:1300 passed, one expected skip, four warnings.

Original-derived compact expectations are in `tests/fixtures/*revival-stock.json`
and `awakened-rebirth-stock.json`; corresponding simulator tests replay masks,
direct state and RNG. Raw captures, before/intermediate/after replay reports,
stock bytecode and source/binary archives remain under
`local/audits/fullrun-parity-20261007`.
`awakened-semantics-migration-r1.json` separates both source transitions and
records285 unchanged native control boundaries. No automatic compatibility waiver
was added. PPO/reward/model/schema are unchanged; future continuation must check
the new source identity explicitly.

These traces cover first-phase death/rebirth and selected power-card interactions.
They do not certify all debuff/relic/copy combinations, final Boss victory,
ordered double Bosses or a natural complete run.
