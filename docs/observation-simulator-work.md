# Observation / Simulator systematic repair

## Closeout under the user's revised acceptance criterion

On 2026-09-05 the user explicitly changed the stopping criterion to an overall
engineering review: fix apparent significant problems and stop when the project
looks sufficiently sound. This bounded review is complete. The exhaustive
acceptance table and open items below remain a historical work ledger and future
backlog; they are not a claim of complete stock parity or instructions to keep
this task running. See [closeout](audits/2026-09-05/observation-simulator-closeout.md).

Objective: close the supported simulator and public-observation correctness gaps
systematically, without launching the original game. This work includes native
rules, both adapters, model encoding, identity/version changes, checkpoint
compatibility and verification. Existing training artifacts remain read-only.

Primary evidence: current implementation, local stock JAR bytecode, local
decompiled code checked against bytecode where necessary, native runtime probes,
and independent expected-result tests. Old audit manifests are discovery aids,
not proof of correctness.

## Work and acceptance evidence

| Area | Required evidence | State |
| --- | --- | --- |
| Combat card upgrades and mutable costs | Stock bytecode comparison; multi-hit Blood for Blood, zero-cost upgrades, repeated upgrade regressions | Blood for Blood and upgradeBaseCost repaired; pile cost lifecycle remains open |
| Public field inventory | Field-by-field stock/native/adapter/model matrix for cards, powers, enemies, relics, choices and run state | Initial implementation matrix completed; explicit gaps remain |
| Ownership and relationships | Power-owner links preserved through both adapters and encoded model inputs; actor/enemy swap changes representation | Power ownership repaired, 7 dedicated tests |
| Dynamic card state | Visible mutable damage/cost/flags survive snapshots, adapters, choices and deck projection; hidden state stays excluded | Rampage/Ritual Dagger and flags projected across card entry points; 8 adapter tests |
| Version and compatibility | New semantic identity; old checkpoints/artifacts rejected or explicitly migrated, never silently rebound | Observation 2 / encoding v4; strict artifact rejection tested; checkpoint identity review remains |
| Simulator rules | Scoped content and shared-mechanism inventory; independent expectations for high-risk combinations and remaining implementations | Pending |
| Serialization and determinism | Roundtrip at decision boundaries and pending choices; independent RNG-stream checks; native source provenance | Pending |
| End-to-end verification | Built native, complete tests, generated-asset checks, bounded non-policy scenario/seed sweeps; no original-game launch | Pending |

Completion requires inspecting coverage against each row, not only a green test
suite. Linux/A100 preflight and the required worker benchmark remain necessary
before subsequent long training on a changed native environment.

## Verified changes in the working tree

- Native combat upgrades preserve Blood for Blood's accumulated discount and
  stock `upgradeBaseCost`'s already-zero cost-for-turn behavior. Seven real
  card-action tests pass; six failed before the fix.
- Power entities carry a validated owner reference, encoded as a directed
  power-to-player/enemy edge. Owner swaps affect representation and target
  scores; arbitrary entity permutation does not alter the semantic result.
- Native Rampage `specialData` is damage growth, whereas stock `misc` is not.
  The adapters now consume an explicit public `base_damage`, failing closed for
  legacy Rampage payloads. Ritual Dagger's persistent damage and public
  free/retain flags survive hand, pile, deck, choice, reward and shop projection.
- `native/oracle/src/spirecomm/parity/CardStatePatch.java` maintains the changed
  Oracle patch. `tools/build_observation_oracle.py --javac D:/java/bin/javac.exe`
  compiled a separate `local/build/oracle/SpirecommParity-observation-v2.jar`.
  Neither original game assets nor the original Oracle archive were overwritten.
  This is compile/bytecode evidence, not a live-game integration pass.
- FullRun previously exposed only an empty CONFIRM for Forethought+, despite
  the standalone battle API supporting multi-selection. The policy adapter now
  supports incremental arbitrary subsets. Sacred Bark + Liquid Memories now
  executes exactly two selected discard cards in FullRun; its action encoding
  uses two indices so cards beyond index 9 remain selectable. Five FullRun
  transition regressions cover these paths.
- Full suite after the native rebuild: **363 passed, 1 skipped** (13.44 s).
  The skipped local 5M artifact diagnostic first verifies rejection of its old
  encoding; historical model action-count expectations cannot apply to v4.
  Two independent generated-metadata tests also reject pre-ownership schemas.
  Ruff passes. Logs: `local/observation-v4-tests.txt` and
  `local/build-native-observation-v4.log`.

## Next checks; do not infer closure from passing tests

### Selected-card observation follow-up

Selected hand cards previously disappeared from Observation while their selector
remained open. Stock CommunicationMod already exports ordered `selected` and
`selected_cards` arrays for hand and grid screens respectively. Observation now
has a separate `selected_cards` entity tuple; both adapters project mutable card
properties, public source and selected order. Model CHOICE rows encode the
selected flag and order, allowing current selected state to be reconstructed
without recurrent history. No hidden card identifier or draw order is exposed.

Original grid source inference is shared by remaining and selected cards, so
empty remaining-choice lists do not change a discard/exhaust source to GENERATED.
Tests cover ordered Forethought selections, disappearance after confirmation,
Original hand/discard/exhaust/master-deck sources, mutable Ritual Dagger state,
and model distinction when selected order or card identity changes. Existing
real pending-choice JSON restore tests now include the selected-card tuple in
their complete decision comparison.

Verification: **403 passed, 1 skipped** (14.31 s), Ruff, vocabulary and whitespace
checks pass. Log: `local/selected-card-tests.txt`. Stock CommunicationMod bytecode:
`local/audits/repository-20260905/communication-state-bytecode.txt`.
Selection operation/count, event option amounts, card cost lifecycle and scoped
simulator-rule coverage remain open.

### Potion actions during incremental selection

FullRun's incremental-selection adapter replaced the native action list and
accidentally discarded legal potion actions. Its step handler also cleared the
pending selected indices after every native action, including potion use and
discard while the same selector remained open. The adapter now retains native
potion actions and preserves ordered selections across those two operations.
Confirming a choice or leaving it still clears the old selected list.

Independent expectations come from stock `AbstractPotion.canUse` (combat/turn
conditions without a blanket card-screen prohibition), `BlockPotion.use`
(queues GainBlock), and the suspended native action path. FullRun regressions
select Strike then Bash around potion use/discard and verify exact draw order,
delayed 12 block versus no block, and consumed potion inventory. A third test
queues Elixir behind Forethought, confirms the first selection, and verifies
that the newly opened exhaust selector starts with no inherited selection.

Full verification after the implementation change: **397 passed, 1 skipped**;
the subsequently added Elixir scenario passes with all **12** targeted FullRun
choice tests. Ruff and vocabulary checks pass. Evidence:
`local/choice-potion-tests.txt` and
`local/audits/repository-20260905/choice-potion-bytecode.txt`.
Full selection context and broader potion immediate/queued-effect parity are
still under review; this does not close all choice semantics.

### Match and Keep attempt-counter follow-up

Stock `GremlinMatchGame.render` displays `attemptCount`; its pair-resolution
logic decrements it once per attempt. Native `public_screen` already contained
the corresponding counter, but both adapters/model omitted it. The counter now
flows through `public_context` into numeric encoding. The maintained Oracle
`EventStatePatch` adds the displayed counter to CommunicationMod's event state;
the separate Oracle archive compiles successfully. Historical payloads without
the field remain missing rather than being interpreted as zero remaining tries.

A real seed-7 run reaches the event and tests all five attempts. At each decision
boundary, the counter is encoded, JSON checkpoint restore reproduces the decision,
and the next pair produces the same result in the restored instance. The counter
disappears after leaving the event. Probe-only event resets deliberately do not
provide a production replay history and were not used for the restore assertion.

Verification: **395 passed, 1 skipped**, Ruff, vocabulary and whitespace checks
pass. Evidence: `local/match-attempts-tests.txt`,
`local/build-oracle-match-attempts.log`,
`local/audits/repository-20260905/match-attempts-bytecode.txt`.
Selection context, event option data, cost lifecycle, choice-time potions and
the broader scoped rule inventory remain open.

### Bottled-card projection follow-up

The three bottle associations previously stopped at native deck indices. They
now project as card flags through the persistent deck, combat initialization,
card movement, deck selections, both adapters and model numeric fields. Combat
snapshots restore the flags. Stock `AbstractCard.makeStatEquivalentCopy` copies
the flags, whereas the Duplicator event explicitly clears them for its new
persistent card; native combat flags and persistent deck indices preserve this
distinction. A dedicated combat-copy scenario is still pending.

Four regressions cover all three bottle types, opening-hand placement, post-play
movement, JSON restore, adapter agreement and model distinction. Full suite:
**394 passed, 1 skipped**, Ruff and generated vocabulary checks pass. Logs:
`local/bottled-card-tests.txt`, `local/build-native-bottles.log`,
`local/build-oracle-bottles.log`. The rebuilt Oracle is a separate archive;
the original game was not launched.

### Ancient Tea Set and pair-action follow-up

Ancient Tea Set now records a persistent charge on actual campfire entry,
retains it through noncombat rooms, grants two energy at the next combat start,
and clears the persistent charge on battle exit (including Smoke Bomb). Public
counter is -2 while charged and neutral after combat starts. Tests use a real
seed-0 campfire -> Golden Idol event -> combat -> escape -> second combat path,
with JSON checkpoint restoration at the campfire. First combat starts with five
energy; the second starts with three. Three additional cases prove an uncharged
relic does not infer charge merely from the previous room.

Match and Keep pair actions previously referenced only generic choice entities;
the two actual card slots did not reach action scoring. Both adapters now bind
subject and target to the two visible slot entities. Model tests distinguish a
known matching pair from a known nonmatching pair and verify that all initially
hidden slots still encode as hidden cards, without accessing the underlying deck.

Full verification: **390 passed, 1 skipped** (historical model incompatibility).
Logs: `local/tea-set-match-tests.txt`, `local/build-native-tea-set.log`.
At this verification point, selection context, Match attempts, bottle relations, event data and broader
rule inventory remain open; the overall objective is not yet complete.

### Public sources and relic availability follow-up

The [field matrix](audits/2026-09-05/observation-field-matrix.md) now inventories
contract/model paths and marks missing public information explicitly. Original
DRAW/EXHAUST card selectors no longer fall through to GENERATED; stock pile
aliases and visible card/action operations have 11 source-specific regressions.

Relic counters now preserve meaningful negative sentinels: only neutral -1 is
canonicalized to zero. Stock Ancient Tea Set's -2 means charged, while several
other relics use it for spent state, so no generic "used up" interpretation is
applied. Native public counters translate persistent data and live Lizard Tail
availability; checkpoint relic entries retain separate `_internal_data` rather
than reusing a projected UI counter as engine state.

Independent execution found that `GameContext::obtainRelic` initialized Lizard
Tail data to zero while combat/noncombat resurrection checks expect availability
to be nonzero. A freshly obtained tail did not prevent lethal Bloodletting.
Acquisition now sets availability to one. A real native regression confirms
resurrection to 40/80 HP, a -2 public counter after use, and death on a second
lethal hit after checkpoint restore. No training checkpoint was rewritten.

Rebuilt verification: **384 passed, 1 skipped** (13.41 s), with logs in
`local/relic-availability-tests.txt` and `local/build-native-relic-availability.log`.
At this verification point, the matrix identified an **Ancient Tea Set defect**: native
checks `gc.lastRoom == REST` instead of preserving the charge across intervening
non-combat rooms. Public charge state was also absent natively. The subsequent
Ancient Tea Set follow-up above repairs this; selection context and event data
remain under review.

### Ordered multi-selection follow-up

Stock `HandCardSelectScreen.selectHoveredCard` appends to `selectedCards`
(`CardGroup.addToTop` calls `ArrayList.add`); `GridCardSelectScreen` also appends
in click order. The prior Python set, native hand bitset and helper-side sorting
discarded this order. New ordered native actions encode the partial permutation
of up to ten hand indices, while the legacy bitset decoder remains available for
historical replay. Liquid Memories stores its two discard indices in click order.
Both FullRun and standalone battle APIs preserve the order. Card removal still
uses descending indices to avoid invalidating later indices.

The discard-to-hand helper now leaves unreturned cards in their original discard
positions when the hand fills. Evidence includes reversed Forethought choices,
two ten-card Elixir permutations, and Liquid Memories selecting Strike then Bash
with only one hand slot left. A real existing Purity fixture selects `[1, 0]`,
serializes through JSON, restores its pending selection and produces the same
post-confirm decision. The checkpoint field stores the ordered list and rejects
duplicate entries or counts exceeding the selection limit.

Verification after rebuilding: **368 passed, 1 skipped** (13.23 s), Ruff and
vocabulary verification pass. Logs: `local/ordered-choice-tests.txt`,
`local/build-native-ordered-choice.log`; stock bytecode captured in
`local/audits/repository-20260905/selection-order-bytecode.txt`.
Full public-field inventory and complete simulator-rule acceptance remain open.

1. Complete a public-field matrix. Investigate selection task/count/selected
   state, Match and Keep attempts, event option amounts, relic counters and
   non-hand cost lifecycle. Current Original normalization resets non-hand
   current cost; native can still expose temporary exhaust-pile costs. Stock
   Soul and ExhaustCardEffect reset timing requires explicit boundary treatment.
2. Check every scoped choice through **FullRun**, including multi-selection
   checkpoint replay and choice-time potion use. Standalone battle coverage
   does not prove the training backend executes the same behavior.
3. Independently inspect scoped card/relic/potion/enemy/shared mechanisms against
   stock implementation. Existing content-entry smoke tests prove dispatch,
   not the numerical rules or callback order.
4. Audit replay/native semantic identity after action changes, then perform
   bounded non-policy decision sweeps, pending-choice restore and RNG checks.

The original 2026-09-05 repository audit remains a historical baseline report;
its "unfixed" labels are not the status of this working tree. Training artifacts
and logs remain read-only. No game launch or large model evaluation occurred.
