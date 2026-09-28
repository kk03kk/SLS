# A20 Act1 simulator fidelity plan — 2026-09-26

Stage status, 2026-09-28: the user has closed the 46M → 60M training stage and
resumed simulator qualification as phase 1 of the next roadmap. The selected 56M model and measured
server results are recorded in [stage closeout](../results/a20-act1-60m-stable/README.md).
Unreviewed reachability and semantic obligations below remain open; this
document must not be read as full Act1 parity certification.
Current qualification work is recorded in
[phase 1 evidence and remaining gates](2026-09-28-act1-qualification.md).

## Decision and evidence baseline

The target is the stock `desktop-1.0.jar` Ironclad A20 Act1 decision process,
under the project's declared policy restrictions. Stock JAR SHA256:
`cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`.
The earlier local qualification baseline source SHA256 was
`c28b45cefa2e2d8529ae21c53bcf722b1ab33efc75693d759887da554b2480a8`;
the matching Windows artifact SHA256 is
`bd1623f455195ee655f417601b81d5e21917672ea4c915249fc59ac5a8350e62`.
These identify that historical local build. The latest transform correction
and native identity are in the [mechanics batch](2026-09-28-act1-mechanics.md).
Before the
event-RNG correction below, the local source/artifact SHA256 pair was
`95e5b651516bb11c0a87eec2ad08d35435be517fd7520163052c7785c5245bc3` /
`68416a0c2c15c62251f9fdd07cadbef3cfb5dac3993f79aa73e90a38f7873db5`.
After the event-RNG correction and before the Egg preview correction, the
source/artifact pair was
`1c0a4b97cacef40e5b10e509c560cbab87a5640659a0b81b77f95297317ce7e5` /
`d6a663e8a5a9fc0cd9acf7b8f1b4e699b1572255cfd09fc069a67eaf2746a57f`.

Two scope decisions need precise wording:

1. **Prismatic Shard** is a valid stock shop relic. It changes card rewards to
   include other colours, which requires other classes' card rules and some
   related mechanics. SLS keeps the relic in the native offer/RNG pool but
   removes its acquisition action from both policy adapters. This is a
   reasonable *declared Ironclad-only policy restriction*, not full stock
   action-set parity. Do not delete the relic from the pool: that would change
   subsequent draws and RNG. Label evaluation results as subject to this
   restriction. Full stock support is a separate expansion after off-colour
   content and interactions are implemented.
2. **A Note For Yourself** cannot occur at A20. The stock
   `AbstractDungeon.isNoteForYourselfAvailable()` returns false for ascension
   15 or greater. Native `oneTimeEventsAsc15` omits the event and the A15+
   constructor chooses that pool. The A20 `AUTO_LEAVE` branch is unreachable
   during normal generation; it is not evidence of A20 gameplay divergence.
   Keep it as defensive behavior for forced states or remove it only in a
   separately versioned profile cleanup. Add an eligibility regression that
   tests the pool and source rule directly, rather than sampling seeds and
   hoping not to see a rare event. For ascensions 1–14, stock additionally
   depends on account unlock history; that distinction is outside the current
   A20 profile but must be handled before generalising this pool.

The current 252-combat soak and focused tests establish simulator invariants,
not complete stock parity. Historical 128-seed evidence has a different native
source digest and must not be presented as current-source qualification.
Short original-game parity runs were later authorized and are recorded below.

## Work sequence

### 0. Freeze evidence for the running 60M experiment

- Record its submitted Git commit, config, native digest, checkpoint source,
  evaluation seed ranges and final selected-checkpoint hash when downloaded.
- Do not copy changed local native or Python environment code onto the running
  checkout or describe a newly changed simulator as an exact 60M resume.
- Keep all current checkpoints, manifests, Slurm logs and audit artifacts.
- Any rule change creates a new environment identity and requires a new native
  build, compute-node preflight and a separately named evaluation/training run.

### 1. State the exact A20 Act1 contract

- Generate an A20 Act1 reachable-content inventory from the stock JAR and
  native registry: Neow, map/rooms, encounters, events, shops, rewards, cards,
  relics, potions, powers, and Act1 terminal transition.
- Separate **stock-reachable**, **policy-reachable**, **forced-probe-only**, and
  **outside Act1** entries. Explicitly record the Shard purchase restriction.
- Trace stock eligibility conditions, not just pool membership: ascension,
  act/floor, HP/gold/deck/relic requirements, account-history dependencies and
  game modifiers. Assert Note For Yourself is absent at A20.
- Publish the inventory with stock JAR hash, native source hash and a count of
  unresolved entries. A registry entry or captured scenario alone is not a
  semantic match.

### 2. Audit transitions from the outside inward

For each stock-reachable obligation, compare the stock Java/bytecode branch
with native code and create a minimal reproducible simulator probe. Prioritize
the distribution that generates training experience:

1. Seed and RNG stream setup; map generation; room, monster and boss pools.
2. Neow options; event eligibility and outcomes; shop prices/stock; card,
   potion and relic reward generation.
3. Act1 combat: enemy move selection, damage/block/debuff ordering, card and
   relic callbacks, death/victory boundaries, reward transition.
4. Public observation and legal action projection on both backends, including
   UI folds, card selection, potion use, keys and dynamic card values.
5. Checkpoint save/load and one-step replay at each boundary.

Store for each finding: stock class/method/branch, native source location,
input state and seed, expected/actual public state, legal actions, reward,
terminal state, RNG counters, and artifact/source hashes. A difference gets a
small regression before the fix. Keep a separate category for cosmetic or
protocol-only differences, with the stable-state equivalence explained.

### 3. Evidence gates

- Make a machine-readable coverage manifest for **A20 Act1 policy-reachable**
  obligations. `SEMANTIC_MATCH` requires independent stock and simulator
  evidence plus before/actions/after/RNG comparisons. `UNREVIEWED`,
  `BRANCH_PARTIAL`, and `SEMANTIC_DIFFERENCE` remain open items. The current
  `require_semantic_training_gate(..., require_scope_complete=True)` is a
  validator, not a gate currently wired into training; integrate it only once
  the Act1-specific inventory and evidence are real.
- Run deterministic rule probes and one-step checkpoint replay locally in DL.
  Use bounded CPU checks; do not run a long local model evaluation.
- The user subsequently authorised short original-game comparisons. Capture
  targeted stock runtime traces for branches still marked `BRANCH_PARTIAL`.
  Do not report a complete runtime parity gate until all reachable obligations
  have matched stock and simulator evidence on the pinned JAR.
- After each native/observation change, qualify the new build through the
  existing compute-node preparation flow before new long training. Compare
  old and new model results on identical held-out seeds; a win-rate movement
  alone does not prove improved fidelity.

### 4. Full Shard support, if expanding scope

Implement other colours' card pools, card effects, powers, orb mechanics,
cross-class interactions and their observation/action encoding first. Then
remove the acquisition filter in both backends together and version the
environment/model contract. Run targeted stock comparisons for Shard reward
generation and acquired off-colour cards before permitting the purchase in
training. This is a later scope expansion, not a prerequisite for an honestly
labelled Ironclad-only A20 Act1 result.

## Completion criteria

The A20 Act1 simulator may be described as **verified within declared policy
scope** only when every reachable obligation is matched or has a documented
equivalent UI fold, the coverage gate passes, no unresolved semantic
differences remain, and the native/source hashes match the tested artifact.
Even then the claim is limited to that game version, profile and policy scope;
finite probes do not prove equivalence for every seed or possible interaction.

The immediate next implementation batch is the Act1 reachable-content and
eligibility inventory, including direct A20 Note For Yourself and Shard
assertions. It is useful without touching the running 60M job.

## First implementation batch

`tools/audit_a20_act1_targets.py` now writes a hash-bound, fail-closed audit
target inventory to an ignored `local/audits/` path. On the current native
build it reports 131 card, 33 potion, 151 relic, 24 event, 22 encounter and
25 monster candidates. Cards, potions and relics are deliberately conservative
global lists, **not confirmed Act1 reachability counts**. The event list is
read from the actual A20 native pools and excludes Note For Yourself and six
one-time candidates rejected in Act1 by both stock `getShrine` and native
`isEventEligible`. The Shard stays visible in the relic audit inventory with
its policy acquisition marked `EXCLUDED`.

`tools/build_semantic_coverage.py --targets <inventory>` can now restrict
method obligations to this candidate set. It rejects stock-JAR, native-source
or scope-hash mismatches and missing bytecode categories/target IDs. All new
obligations remain `UNREVIEWED`; the inventory cannot promote them to parity.
Before writing the inventory, the tool compares the ordered native Act1
event/shrine/one-time pools to the corresponding stock Java projection:
11/6/13 entries match on the current artifact. This verifies initial pool
membership and order only; per-state eligibility, RNG draws and event results
are the next audit layer.
It also compares the stock Exordium weak/strong/elite encounter tables with
the native Act1 tables, including stock's stable sort by weight and normalized
weights: 4/10/3 entries match. This is table-level evidence, not a matched
seed trajectory or combat outcome.
The target inventory changes only audit tooling and tests. The separate
correction below changes native semantics locally; neither was synced to the
running training experiment.

### Confirmed event-RNG correction

Stock `EventRoom.onPlayerEntry` constructs a duplicate RNG at the current
event counter, then passes that duplicate through `generateEvent` and its
fallback to `getShrine`. Native already used a copy for ordinary selection,
but `GameContext::getEvent` accidentally passed the shared `eventRng` to
`getShrine` when its ordinary event list contained no eligible entry. A forced
A20 floor-2 case with only an ineligible Dead Adventurer and Golden Shrine
reproduced the difference: shared event counter advanced from 0 to 2 instead
of 0 to 1. The regression failed before the one-line correction and passed
after a fresh native build. The relevant simulator/original/audit/content/
contract suite passes 243 tests. This is a rare fallback path; no claim is
made that it changed the running 60M job's outcome. Any future training or
evaluation on this new native identity needs its own qualification and must
not be represented as an exact continuation of the older environment.

### A20 Act1 event eligibility review

The source-level conditions in stock `AbstractDungeon.getEvent/getShrine` and
native `GameContext::canAddEvent/canAddOneTimeEvent` agree for the Act1 branches
reviewed here:

| Candidate | Stock/native condition |
| --- | --- |
| Dead Adventurer, Mushrooms | Floor greater than 6 |
| The Cleric | Gold at least 35 |
| Fountain of Cleansing | Own a removable curse; Ascender's Bane, Curse of the Bell and Necronomicurse do not qualify |
| The Woman in Blue | Gold at least 50 |
| Face Trader | Act1 or Act2 |

The other Act1 pool entries fall through the eligibility switch. This is a
static branch review. Short forced-state simulator probes now exercise the
early-floor exclusion of Dead Adventurer/Mushrooms, the Cleric's 35-gold
threshold in both directions, the Fountain with only A20's unremovable Bane,
and the Woman in Blue without 50 gold. The probes establish these specific
native branches, not original-game runtime parity or event outcomes.
Continue with map room routing and reward effects. The current A20 candidate
manifest remains `UNREVIEWED` for semantic parity.

### Combat reward path review

Compared stock `AbstractRoom`, `MonsterRoomElite`, `MonsterRoomBoss`,
`CombatRewardScreen`, `RewardItem`, `AbstractDungeon.getRewardCards`, and
`PotionHelper` with native `GameContext::create*CombatReward`,
`createCardReward`, `addPotionRewards`, and `Game::returnRandomPotion`.
For ordinary, elite and Act1 boss combat, the source-level order is gold,
elite relics where applicable, potion roll, then card choices. Prayer Wheel
adds a second card choice only after an ordinary monster room. The elite
relic tier cutoffs, base potion chance and adjustment, boss rare-card rule,
and the common/rare card pity update agree in the reviewed branches. These
are source comparisons, not original-game runtime traces.

The ordered 33-entry Ironclad potion draw pool now has a fail-closed stock
projection check in `compare_stock_ironclad_potion_pool`, integrated into the
target inventory tool. A test swaps its first two entries and verifies the
check fails. The current local stock source projection and native pool match
all 33 positions. Potion rarity, behavior, effects, and RNG implementation
remain separate semantic obligations. The stock JAR is absent from the
manifest's original `local/external` path, but the installed JAR at
`D:\Steam\steamapps\common\SlayTheSpire\desktop-1.0.jar` has the same pinned
SHA256. Using that file, the complete hash-verified inventory regenerated
successfully at `local/audits/a20-act1-targets.json` without opening the game.

### Act1 map and chest review

Stock `Exordium.initializeLevelSpecificChances`,
`AbstractDungeon.generateMap/generateRoomTypes`, and `RoomTypeAssigner`
were compared with native `Map::fromSeed`, `fillRoomArray`, and
`assignRoomToNode`. The five Act1 room chances (shop 0.05, rest 0.12,
treasure 0, event 0.22, elite 0.08) and the A1+ 1.6 elite multiplier match
the stock source projection. The native map uses the same 15-by-7 dimensions,
fixed first monster row, ninth treasure row, final rest row, and row
restrictions for early elites/rests and the penultimate rest row. A bounded
native probe checked these structural conditions, next-row edges and the
single burning elite on seeds 0–31. It passed; this does not establish
seed-for-seed stock map identity. The audit target tool now records the
constant comparison and this native invariant probe.

A separate headless bytecode probe, `tools/java/StockAct1MapProbe.java`, calls
stock `MapGenerator` and `RoomTypeAssigner` directly without starting the game
window. `tools/audit_act1_map_paths.py` verifies the pinned stock JAR and
current native artifact, then compares ordered nodes, room types and edges on
seeds 0–31. All 32 maps matched. The harness constructs the A20 Exordium room
list from the reviewed stock formula, then calls the stock assignment method;
it normalizes only the virtual boss-edge label (stock row 16 to public protocol
row 15). Burning elite placement is excluded from this bytecode comparison
because its stock helper requires full dungeon initialization; the native
single-burning-elite invariant is separately checked. The result is strong
map-generation evidence, not whole-run parity.

### Confirmed Egg preview correction

Stock `MoltenEgg2.onObtainCard` requires `!card.upgraded` before upgrading an
attack. The native `previewObtainCard` lacked this guard. A deterministic
native probe with Searing Blow and Molten Egg returned upgrade counts 1, then
2 on a second preview; the stock rule requires 1, then 1. The regression was
observed failing before the fix and passing after it. Native now makes Egg
preview idempotent for already upgraded cards. `Shop::assignRandomCardExcluding`
also no longer previews the second attack/skill card before `setupCards`
previews the full inventory, matching stock `ShopScreen.initCards`' single
preview pass. This is a real local environment-semantic change. The rebuilt
artifact, target inventory and 32-seed headless map comparison use the new
source identity; 254 focused tests pass. The ongoing 60M server experiment
uses its earlier environment and was not modified.

Stock `AbstractChest.randomizeReward/open` and the small/medium/large chest
constructors were also compared with native `setupTreasureRoom` and
`openTreasureRoomChest`: size odds, the shared gold/relic roll, per-size
thresholds, gold range, Matryoshka, Cursed Key, N'loth's Hungry Face, and
Sapphire Key paths match in the reviewed source branches. Relic callback
ordering and stock runtime state transitions still need direct evidence.

### Full original-game trajectory comparisons

After the user authorized short original-game runs, the policy trajectory
canary was run with the existing 38M exported policy and zero recurrent memory
on the pinned stock game plus BaseMod, CommunicationMod and the pinned Oracle.
The first complete seed, `20260926`, exposed successive first differences.
Each correction was made locally and the simulator replayed in seconds against
the saved original trace:

| Boundary | First difference | Correction |
| --- | --- | --- |
| 66 | Pocketwatch's visible counter was zero in native, one in stock | Project the number of cards played this turn and reset the native relic's combat-exit counter to -1 |
| 89 | Equal shop action sets in a different order | Put purge before cards, relics and potions in the simulator policy adapter |
| 114 | Potion discard preceded reward-screen proceed | Put inventory potion actions after the parent reward actions |
| 124 | Red Slaver repeated Scrape at A20; stock used Stab | Follow stock A17+ last-move branch, rather than the A0 last-two-moves branch |
| 130 | A potion reward was visible with full slots in both backends, but only native exposed collection | Suppress collection in the simulator policy adapter while leaving the reward visible |

The stock `SlaverRed.getMove` bytecode directly establishes the A17+ branch:
after Scrape, it chooses Stab unless an earlier Entangle branch applies. The
new forced A20 move probe checks this independently of the trajectory.

The canary now records both ordered actions and the sorted action set. It
classifies order differences explicitly when both sides have the new field.
The original game was also run with `SuperFastMode` v1.0.9, SHA256
`cecdc2a54d0b2db8aa66857098d4740e5d74ec3361cd12416629019438d25c60`.
For seed `20260926`, all 169 boundaries' observation, action-set, policy-input,
chosen-action and recurrent-memory hashes matched the prior run without the
speed mod. The canary's backup journal reports `RECOVERED` after each run.

With the corrections above, seed `20260926` (Hexaghost) matched all 169
decision boundaries through the Act1 boss reward; seed `0` (Slime Boss)
matched all 160, and seed `3` (The Guardian) matched all 158. Comparison
reports are under `local/audits/act1-final-seed-*-comparison.json` and bind
both trace file hashes. These are complete trajectory checks for three seeds,
not exhaustive reachability or semantic
coverage of every Act1 card, relic, event, potion and combat branch.

The later source/artifact pair at the top of this document includes further
route corrections. The pinned stock target inventory and 32-seed headless map
comparison were regenerated against that identity, and the focused audit,
diagnostics, simulator, content and original-adapter suite passed 308 tests
with one expected historical-encoding skip. The running NUS 60M job still
uses its earlier submitted source identity.

### Additional Act1 route audit

A fourth complete route, seed `9`, matched all 219 boundaries and covered Blue
Slaver and Golden Idol. Route screening selected seed `5` because it reaches a
Gremlin Gang and two one-choice event introductions absent from the first
three routes. It revealed two additional UI folds and one native rule bug:

- Stock Bonfire Spirits presents a mandatory single event option before its
  card grid; native opens the grid directly. The Original adapter now executes
  that fixed introduction inside the map transition.
- Stock `GremlinWizard` constructor initializes `currentCharge` to one.
  Native initialized the corresponding `miscInfo` to zero, exposing Ultimate
  Blast one turn late. Native construction now starts at one. A forced native
  probe checks the constructor value and charge/attack sequence against the
  stock constructor and `takeTurn` bytecode.
- Stock Lab presents a mandatory single event option before its potion reward
  screen; native opens rewards directly. The Original adapter now folds that
  fixed introduction in the same way.

The later full-route comparisons also exposed three protocol and RNG details:

- Dream Catcher at a rest site opens a standalone card reward in stock. Native
  stores it in a generic reward screen. The simulator adapter now exposes the
  same card choices and folds the parent reward after a choice or skip.
- Stock shops refuse a potion purchase when all potion slots are occupied.
  Both policy adapters now omit that action while keeping the item visible.
- Smoke Bomb opens a delayed empty reward screen in stock. The Original
  adapter waits for the escape animation and folds the empty screen. More
  importantly, stock `AbstractRoom.update` draws combat gold and rolls a
  potion before its smoke-specific reward screen hides them. Native escape
  now consumes the corresponding streams and updates potion pity without
  granting rewards. On seed 8, RNG counters first diverged exactly after the
  Smoke Bomb: treasure 3 versus 4 and potion 28 versus 29; subsequent elite
  rewards differed. After the correction all 181 boundaries matched.

The current local source passed 308 focused tests (one expected historical
encoding skip). The A20 target inventory contains 131 card, 151 relic, 33
potion, 24 event, 22 encounter and 25 monster candidates. A new 32-seed
headless stock-JAR map comparison passed. These results qualify only the
reviewed branches and sampled trajectories; the candidate inventory still
contains unreviewed stock-reachability and semantic obligations.

The seed-6 route exposed a Windows protocol encoding fault. CommunicationMod
emits UTF-8 JSON, while the Python 3.12 process on this workstation reported
GBK for redirected standard input. This corrupted Chinese terminal labels,
including Woman in Blue's mandatory one-choice departure, so the generic UI
fold did not recognize it. `StdioTransport` now configures its input stream as
UTF-8. A byte-stream regression covers a Chinese label; the fresh original
seed-6 trajectory matched all 154 simulator boundaries, including Woman in
Blue, Dead Adventurer and Shining Light. This is an adapter correction, not a
native simulator rule change.

Seed 10 exposed two more state-projection differences after 102 matching
boundaries. A free Pummel generated by Infernal Blade retained a zero
`costForTurn` in native after being played and exhausted; stock showed its base
cost of one in the exhaust pile. Native now settles the cost of a played card
before its move to exhaust, as it already did for discard. At the final Act1
rest row, native offered all legal map destinations but marked only its graph
successor as reachable in the observation. The simulator adapter now derives
map-screen reachability from its actual map actions. Replaying the saved
original seed-10 trace then matched all 184 boundaries.

### Current full-route evidence

All comparisons below use the source/artifact pair at the top of this file.
The simulator was recaptured from that exact build; the Original trajectories
are saved locally and the comparison JSON binds each pair of trace hashes.
The baseline policy is the exported 38M policy, not the running 60M model.

| Seed | Act1 end | Matched boundaries | Notable coverage |
| --- | --- | ---: | --- |
| 20260926 | Hexaghost | 169/169 | Red Slaver, shop and reward ordering |
| 0 | Slime Boss | 160/160 | Full potion slots in shop |
| 3 | The Guardian | 158/158 | Guardian route |
| 4 | Act1 boss | 182/182 | Scrap Ooze, We Meet Again |
| 5 | Boss defeat | 200/200 | Gremlin Wizard, Bonfire Spirits, Lab |
| 6 | Act1 boss | 154/154 | Woman in Blue, Dead Adventurer, Shining Light |
| 7 | Slime Boss | 141/141 | Dream Catcher rest reward |
| 8 | Boss defeat | 181/181 | Smoke Bomb, Mushrooms, Toxic Egg |
| 9 | Act1 boss | 219/219 | Blue Slaver, Golden Idol |
| 10 | Boss defeat | 184/184 | Match and Keep, Infernal Blade, final rest row |

These 1,748 matched decision boundaries cover ten complete Act1 outcomes.
They do not prove every candidate card, relic, potion, event or interaction.
The target inventory is still labelled
`AUDIT_CANDIDATES_NOT_PROVEN_STOCK_REACHABLE`; this is not a claim of exhaustive
stock equivalence. The NUS 60M training environment was not changed.

### Entropic Brew and Sozu audit

Stock `EntropicBrew.use` has distinct combat and outside-combat branches. The
outside-combat branch checks Sozu before making any calls to
`AbstractDungeon.returnRandomPotion()`. Native `GameContext::drinkPotion`
previously bypassed this check, filling all three potion slots and advancing
`potionRng` even with Sozu. A forced native probe reproduced the fault before
the correction (`potion_count=3`, 14 potion-RNG draws). After the correction,
the Sozu case has no potions and zero draws; the no-Sozu case still fills the
slots. The combat branch deliberately retains its capacity-wide RNG draws:
stock queues those rolls before later potion-obtain actions apply Sozu.

All ten saved Original trajectories were replayed against the rebuilt source
and still match at all 1,748 boundaries. The 32-seed headless map comparison
and focused tests also passed. This new rule change is local only and is not
part of the running 60M server environment.

### Wheel of Change and World of Goop audit

Stock `GremlinWheelGame` rolls `miscRng.random(0, 5)` after the spin is
selected. Its Act1 gold prize is 100; A15+ HP loss is a truncating cast of
`maxHealth * 0.15f`. The six result effects and the A20 `GoopPuddle` gold-loss
roll (`miscRng.random(35, 75)`, capped by current gold) match the native
branches on static review. This is branch-level source evidence, not a claim
that an original-game route exercised those branches.

One Wheel boundary differed: stock checks whether any purgeable, unbottled
cards exist before opening its removal screen. Native always opened a card
selection, which could strand a run with an empty eligible set. Native now
returns to the map when that set is empty. A forced seed-0 A20 probe checks
both sides: a normal deck opens card selection, while an empty deck returns
to the map with legal actions. The ten saved Original routes still match all
1,748 decision boundaries against the new native build. The 32-seed map
check also still passes. This fix is local only and is not part of the running
60M server environment.

A stock-JAR bytecode index was also regenerated for this source identity.
`tools/build_semantic_coverage.py` expands the candidate set into 1,810
fail-closed method/system obligations: 561 card, 771 relic, 169 potion, 165
monster, 108 event, 22 encounter and 14 system entries. They currently remain
`UNREVIEWED`; many may be unreachable in this specific policy profile, and
neither a registry mapping nor a matching route automatically promotes them
to `SEMANTIC_MATCH`. The ignored local manifest is
`local/audits/a20-act1-semantic-coverage-final.json`. This is the remaining
scope for a claim of exhaustive first-act parity.
