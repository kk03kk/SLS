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
growth, and the weakest-current-HP target rule. The reproducible native
implementation is `simulator/native/patches/0004-generic-orb-mechanics.patch`.
Native boundary and checkpoint tests cover the core rules; Loop, slot decrease
ordering and live original traces remain open.

## Player damage pipeline

Audited original classes and methods:

- `AbstractPlayer.damage` and `AbstractCreature.decrementBlock`
- `IntangiblePlayerPower.atDamageFinalReceive`
- `BufferPower.onAttackedToChangeDamage`
- `Torii.onAttacked` and `TungstenRod.onLoseHpLast`
- `LoseHPAction.update`

For player damage the observed order is Intangible, block, Buffer, player and
relic `onAttacked` hooks (including Torii), then `onLoseHpLast` (including
Tungsten Rod), followed by HP-loss callbacks and HP subtraction. Intangible
caps values above one. Torii changes normal unblocked damage from two through
five to one and excludes `HP_LOSS` and `THORNS`; Tungsten removes one after
Torii. `HP_LOSS` bypasses block but still enters Buffer's damage-change hook.

The reproducible correction is
`simulator/native/patches/0005-damage-pipeline-parity.patch`. The native probe
locks the important combinations, including a retained Buffer after Intangible
is fully blocked, Buffer across multi-hit attacks, and Torii plus Tungsten at
both sides of Torii's threshold. Live original traces and the remaining
damage-changing relics are still required before these matrix rows can be
marked implemented.

## Just-applied turn durations

Audited original classes include `WeakPower`, `VulnerablePower`, `FrailPower`,
`DrawReductionPower`, `IntangiblePlayerPower`, `DoubleDamagePower`, and
`PhantasmalPower`. Weak, Vulnerable and Frail skip their first end-of-round
decrement only when the new power was sourced by a monster; stacking an
existing power does not reset its private `justApplied` flag. Draw Reduction
always starts with that flag and changes hand size only on initial application.
Intangible has no such flag and always decrements at end of round. Phantasmal
Killer creates Double Damage at the next turn start with `justApplied=false`,
so that buff also decrements at the coming end of round.

The reproducible correction is
`simulator/native/patches/0006-just-applied-duration-parity.patch`. It also
moves Draw Reduction expiration from the following start-turn draw boundary
to the original end-of-round stage, preventing an extra reduced draw.

## Retain, Ethereal and end-turn hand cleanup

Audited original classes include `DiscardAtEndOfTurnAction`,
`RestoreRetainedCardsAction`, `RetainCardsAction`, `AbstractCard`,
`EquilibriumPower`, `RetainCardPower`, `EstablishmentPower`, and
`EstablishmentPowerAction`.

The original first removes cards whose `retain` or `selfRetain` flag is set
from hand into limbo. It queues restoration, optionally queues ordinary hand
discard when neither Runic Pyramid nor Equilibrium is active, and only then
calls `triggerOnEndOfPlayerTurn` for cards still in hand. Consequently explicit
or self Retain wins over Ethereal, while Pyramid and Equilibrium do not protect
Ethereal cards. Equilibrium marks only non-Ethereal cards as retained. Manual
Retain selection permits selecting an Ethereal card but deliberately does not
set its retain flag. Restoration clears the one-turn dynamic flag.

Establishment runs before Retain Cards and Equilibrium at the end-turn power
boundary, so it discounts cards already carrying Retain/selfRetain, not cards
newly selected or marked later in that same boundary. The reproducible native
implementation is
`simulator/native/patches/0007-retain-ethereal-cleanup-parity.patch`.
