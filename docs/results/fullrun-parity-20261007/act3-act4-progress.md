# A20 full-run audit: Act3 basic coverage and first Act4 stock probes

**Later evidence:** [Reptomancer follow-up](reptomancer-progress.md) resolves the
previous seven-vs-five projection classification and adds actual targeted scripts.
The open-slot statements below describe the earlier batch and remain as history.

This is bounded local progress, **not full Act3/Heart qualification**. No commit
or GitHub push is made. Server job916291 retains fixed commit
`6e54adbc0aa6fca81b994490837db382b245e42b`. PPO, reward, network and training
distribution are unchanged. Controlled high HP and fixed decks are initialization
fixtures; these runs are not normal starts, checkpoint-selection evidence or win-rate estimates.

## What now has independent runtime evidence

- All16 registered Act3 encounters have actual stock `TheBeyond` room captures,
  each with three fixed seeds and four enemy turns. Basic coverage does not close
  reaction, revival, half-HP, curse, death or terminal branches.
- Additional Nemesis Intangible card/potion scenarios match on three seeds,
  including the following non-Intangible turn.
- Time Eater's twelfth-card forced end and queued Strength match on three seeds.
- Shield/spear basic four-turn and Heart six-turn attack/buff cycles match on
  three seeds each. Direct stock reads confirm actual `TheEnding`, act4 and
  `MonsterRoomElite`/`MonsterRoomBoss`; merely assigning an act number is rejected.
- The new12 key runs match every compared boundary, including independent stock
  cards, items, creature values, legal actions and controlled RNG.

New immutable manifests are `fullrun-act3-basic.json` (14 scenes/42 seeds,
131200045–086) and `fullrun-late-key-r1.json` (4 scenes/12 seeds,131200087–098).
They supplement the previous Darkling/Nemesis six captures. All allocations were
checked for collisions before freezing. No final holdout is used.

## Confirmed errors and separate repairs

| Root cause | Independent basis and before/after evidence | Remaining scope |
| --- | --- | --- |
| Darkling Nip double ascension bonus | Earlier stock constructor/takeTurn captures; three old failures and three corrected matches | Revival/all-dead still missing |
| Writhing Mass Flail block18 instead of16 at A20 | Stock `takeTurn` gains `damage[2].base`, constructor supplies16 at A2+/15 below; three old regression failures and three passes | Not all ascensions or damage modifiers runtime-tested |
| Writhing Mass rejected consecutive Flail forced Wither | Stock `getMove` offsets447–456 rerolls `aiRng.random(69)`; after isolated block repair two traces pass and seed131200086 still fails; second repair makes all three four-turn traces match, including RNG | Reactive attacks and Implant branches remain open |

Repairs were separated; old native binaries, source archives and original
strict failures remain local. Fixtures under `tests/fixtures` contain compact
identified stock-derived expectations; tests do not ask native to generate the
expected values.

## Differences retained, not hidden

- Four fixed FourShapes/ThreeShapes traces first differ only in dead Exploder's
  residual Explosive1 power. Stock clears powers when its death animation timer
  expires. A separately labelled, trace-specific analysis continues to the next
  boundary and finds no other state/action/damage/RNG differences. Strict failures
  are retained. No general dead-power normalization was added.
- All three Reptomancer traces first differ at dead Dagger Minion cleanup.
  Continuing with only that precise animation exception reveals a later corpse
  list/slot difference: stock retains dead entries and can expose seven public
  entries, while native reuses its five internal slots. Target identity and legal
  action equivalence are **not proven**. This remains an open Act3 issue, rather
  than a silently accepted display mapping.
- Collector's existing normal-start production witness remains349 matching
  boundaries. Automaton remains334 boundaries only with independently observed
  Discovery retrieval-clock conditioning; its original strict failure is retained.

The48 basic Act3 captures currently yield41 strict matches, four Exploder
presentation differences and three open Reptomancer differences. The12 additional
key runs all match. These counts are branch evidence, not whole-content certificates.

## Identity and migration

Current native source:
`5fb389065dd9b6f52c470a962308f57ae0f5e039f07dcebc7fc411927625f5c5`.

The isolated Act4 probe API transition
`5b6db62d… -> 2b5aa6c4…` has285 matching fixed normal-process native snapshots
and actions. Its production-preserving scope is limited to the API extension.
The two Writhing rule transitions are separately recorded:
`2b5aa6c4… -> 4fb8b4c8… -> 5fb38906…`.

No global state-preserving waiver or checkpoint migration bypass is registered.
Reward/action/observation layouts and checkpoint/model layouts are unchanged,
but damage/block/RNG semantics changed. Future continuation must check the actual
training profile and source contract. Existing server results retain their old
environment identity; simulator repair benefits cannot be attributed to lambda.

Oracle1.3.6 is built from source (38 classes), SHA
`27c6fd063b0851c95612e531a0767e7d03c5c19a27b8a518b36a54ff61164f3e`.
Stock JAR SHA remains
`cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`.
Historical Oracle JARs and sealed manifests are preserved for offline replay.

## Validation and recovery

- Final complete Python suite:1248 passed,1 intended skip,4 expected warnings.
- Native rebuild and import smoke passed; Ruff passed;33/33 training configs valid.
- Darkling plus both Writhing regressions:9 passed. Old failures and intermediate
  failures are retained as evidence of test sensitivity.
- Existing36 shared,9 direct-v2 and6 Act3 context captures replay under the corrected
  current source without differences.
- Each new stock batch finished inside30 minutes, one owned game process at a
  time. All three recovery journals report RECOVERED; all63 protected files in each
  journal were independently rehashed against originals after shutdown.
- AGENTS.md is unchanged. Stock bytecode, JARs and raw traces stay under local;
  no local audit work is uploaded to GitHub.
- Current Oracle r9 production runtime isolation smoke passed; its additional
  recovery journal is RECOVERED and all63 protected files were rehashed unchanged.

## Next bounded work

1. Resolve Reptomancer target/corpse identity using the first live-target divergence
   and explicit semantic actions; prove any display mapping rather than altering masks blindly.
2. Add shield/spear turning/death and Heart Beat of Death/cap/reset/death scenarios.
3. Complete remaining Act3 critical branches, then ordered double bosses, keys and
   Act4 route. Continue the reachable content/event ledger and natural production
   late-act trajectories as real model reach permits.

Late-only open issues do not automatically block current Act1+Act2 training.
New shared or Act1/Act2 substantive errors do enter the next training gate.

Reproduction entry points are `tools/run_act2_encounter_batch.py` and
`tools/replay_act2_encounter_batch.py` with the corresponding immutable manifest
and sealed Oracle build. Use new output names; never overwrite prior evidence.
Local machine-readable summaries are `local-progress-r5.json`,
`act4-api-source-transition-r1.json`, and `writhing-semantics-migration-r1.json`
under `local/audits/fullrun-parity-20261007`.
