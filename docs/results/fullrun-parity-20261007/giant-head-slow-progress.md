# Giant Head selected Slow interactions

Six actual A20 TheBeyond elite traces131200156–161 strictly match original
state, legal actions and RNG. Script: Defend_R, sequential Angers, optional Fire
Potion between attacks, end turn, another Anger. Independent source is
`SlowPower.onAfterUseCard`, `atDamageReceive` and `atEndOfRound`: card completion
increments Slow; NORMAL damage receives the multiplier; end round resets it.
Six compact original-derived regression cases pass without native rule changes.

An initial batch failed initialization because the scene used `Defend` rather
than stock ID `Defend_R`; it is an execution failure, never a pass. Its manifest,
crash logs and recovery evidence remain local. A separate corrected immutable
manifest `fullrun-giant-head-slow-r2.json` produced the six completed runs.
Both launchers recovered63 protected entries; independently rehashed before the
next launch and after the completed corrected batch with zero differences.

Executed Oracle r22/1.3.19 SHA
`b6e921cb4ac115a66ecbe1ebbc35e9c55b8b7101cc07fbfcf1106732d5c60ce7`.
Native source090f8a4e. Original bytecode, captures and replays remain local;
the initial source extraction also preserves a class-name lookup failure
(`Defend_R` versus actual class `Defend_Red`), corrected in a separate extraction.

This covers selected Slow stacking, rounding, potion exclusion and reset. Late
escalating attacks, other relic/card interactions and encounter victory remain
separate obligations. It provides no win-rate estimate.
