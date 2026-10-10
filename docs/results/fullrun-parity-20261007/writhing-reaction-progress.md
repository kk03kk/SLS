# Writhing Mass selected Reactive/Malleable boundaries

Nine controlled original runs match state, actions and RNG: initial two scenarios
131200162–167 cover sequential positive hits, a Fire Potion and end-turn reset;
supplement131200168–170 guarantees a fully blocked fifth Anger. All nine compact
original-derived regression cases pass. No additional native rule change needed.

Original `ReactivePower.onAttacked` requires a non-null owner, non-HP_LOSS/non-
THORNS damage, positive damage and a nonlethal hit. `MalleablePower.onAttacked`
and reset callbacks were audited independently. The original Fire Potion loses
20 damage to3 block→17 HP damage without changing Reactive AI RNG/Malleable.
Four ordinary Angers create6 block; fifth fully blocked Anger neither increments
Malleable nor rerolls AI. These are observed original values, not native answers.

The first scenario's four attacks alone do not guarantee a blocked hit. Its
name is not proof of branch coverage. A rolled Flail covered one blocked branch
in the potion scenario; the supplemental fixed fifth-hit scenario removes that
random dependence. Original manifests and reports remain immutable.

Executed Oracle r23/1.3.20 for six runs and r24/1.3.21 for three runs. Native
source090f8a4e. Each recovery restored63 entries, independently rehashed unchanged.
Raw source/captures/replays remain local; no natural win rate or Implant
equivalence is claimed. Implant/master-deck timing and other move/relic branches
remain separate obligations.
