# Original combat logic oracle audit

The frozen Steam `desktop-1.0.jar` from `reference_build.json` is the authority
for ordering-sensitive generic mechanics. It is inspected locally and is not
redistributed. CFR 0.152 (SHA-256
`f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2`) is used
only to make the Java bytecode readable.

## Stances and Mantra

Audited original classes:

- `actions/watcher/ChangeStanceAction`
- `stances/CalmStance`
- `stances/WrathStance`
- `stances/DivinityStance`
- `powers/watcher/MantraPower`

The observed core rules are: changing to the current stance is a no-op; exit
hooks run before replacing the stance; leaving Calm queues two energy; entering
Divinity queues three; Wrath multiplies normal damage given and received by
two; Divinity multiplies normal damage given by three and returns to Neutral at
the next turn start. Mantra stacking at ten queues Divinity, subtracts ten, and
removes the power only when no remainder remains.

The reproducible native implementation is
`simulator/native/patches/0003-generic-stance-mechanics.patch`. It currently
covers the shared stance value and core Calm/Divinity energy hooks. The original
power/relic/card callbacks surrounding a stance change remain in the Step 4
matrix as partial work.

## Orbs

Audited original classes include `AbstractPlayer`, all four orb classes,
`ChannelAction`, `EvokeOrbAction`, `IncreaseMaxOrbAction`, and
`DarkOrbEvokeAction`. This established the slot cap and FIFO channel/evoke
behavior, Focus exclusions for Plasma, the four base passive/evoke values, Dark
growth, and the weakest-current-HP target rule. Native implementation is the
next bounded Step 4 task; the three orb rows remain explicitly unimplemented.
