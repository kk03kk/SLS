# Donu/Deca one-survivor support targets

Six original traces131200150–155 were captured in actual A20 TheBeyond Boss
context. Kill Deca or Donu with Anger, then let the survivor execute three turns.
Original `ApplyPowerAction.update` rejects dead/escaped targets;
`GainBlockAction.update` rejects dying/dead targets. These stock action guards
provide the independent basis for the fix.

Before: native Donu still added Strength to dead Deca. Native Deca still added
block and Plated Armor to dead Donu. All six original-derived regressions failed.
Measured downstream differences stayed on the corpse fields; survivor state,
actions and RNG matched. No training/win-rate effect is asserted.

After: only the existing two support move branches skip the dead partner.
All six strict replays match;24 local/adjacent tests pass. Source transition:
`97fecc84af6595efb62b454671bc8c853f478c80dd0f157a13c168ad216c9abe`
→`090f8a4ec4c49ee6a07165e13eea5c14ec16b39375b695b1ea5ab10bb42b7e02`.
Model/encoding/action layouts remain unchanged; corpse observation values change.
The new source identity is mandatory. No automatic compatibility waiver exists.

Executed Oracle r20/1.3.17:
`9fd749f02f9dee5b2806148682649d867575a3f2b5a609ca23a89916d316caa4`.
Recovery63 entries independently rehashed with zero mismatches. Raw stock/source,
old binary/source archive, old failures, fixed replay and separate migration
record remain under `local/audits/fullrun-parity-20261007`.
Normal native control285 boundaries are unchanged; this is not stock-flow proof.
Complete Boss victory/double Boss continuation and other death relics remain open.
