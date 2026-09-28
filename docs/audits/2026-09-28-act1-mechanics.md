# Act1 acquisition, gremlins and RNG — 2026-09-28

## Result and identity

One gameplay discrepancy was fixed: transform selection must sample the
ordered candidate list **after removing the prohibited card**. Complete
Act1 semantic qualification remains open. No training or model-strength
evaluation was performed, and the 56M weights were preserved.

Current native source SHA256:
`470acd317f0b3104c3c5de91e346934798414c8f328381555458ebd3ab0bfad9`.
Windows artifact SHA256:
`cdfa93d00b5ae083e241e2d186a680922fad364b05bf6764e1f325e2c8becc4e`.
Stock JAR SHA256:
`cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`.

The previous source/artifact pair in the first qualification batch is
historical; its ten trajectories were freshly replayed against this new build.

## Transform correction

Stock `AbstractDungeon.returnTrulyRandomCardFromAvailable`,
`returnTrulyRandomColorlessCardFromAvailable`, and
`CardLibrary.getCurse(prohibitedCard, rng)` build a filtered list, then make
one RNG call using the filtered size. The previous native code sampled
`size - 1` candidates but only skipped a slot when the selected slot itself
contained the prohibited card. Slots after the prohibited card therefore
mapped incorrectly, and the last candidate often became unreachable.

For seed 0, transforming Anger over the supplied native ordered pool:
stock selects Headbutt; the former native index mapping selects Havoc.
The corrected native selection returns Headbutt and matches the stock RNG
state and counter. This is an isolated selection comparison; it does not
certify that the supplied pool itself has the stock order.

`drawCardExcluding` now counts eligible entries, draws once, and walks them
in order. It handles an absent prohibited card by using the full pool, and
rejects an empty candidate set. Production curse, colorless and colored
transform paths use this helper. Observation encoding is unchanged.

The diagnostic reconstruction found 364 disagreements between the previous
index mapping and stock selection in the sampled cases where the prohibited
card was in the pool. This reconstruction uses the independently observed
stock-selected index; it is not execution of a preserved historical binary.
It is preserved under `local/audits/act1-mechanics-20260928-fixed/`.

## Independent short execution evidence

`tools/java/StockAct1MechanicsProbe.java` executes the pinned original JAR's
methods. Its scaffolding supplies inert cards/monster state, empty powers,
localization, fonts, display settings and silent audio. It does not implement
the selection, move or block rules itself. It never starts the game window or
changes installed game saves/configuration. Stock static initialization can
create a default display config; the runner sets its working directory under
the new ignored audit output directory. A smoke-run config initially created
at the repository root was moved to ignored audit storage.

`tools/audit_act1_mechanics.py` verifies the JAR and current native identities,
compiles the probe, saves inputs and both outputs, compares them, and refuses
an existing evidence directory. Its current result:

| Check | Cases | Observed result |
| --- | ---: | --- |
| Stock RNG: nine mixed operations plus Java shuffle | 11 seed sequences | Exact values, float32 bits and start/end state/counter match |
| Transform selection | 960 | Selected card and RNG state/counter match |
| A20 gremlin `getMove` | 900 | Selected move byte matches |
| Shield Gremlin block targeting | 55 | Per-monster block and AI RNG state/counter match |

All **1,926 cases passed**. Seeds include zero, normal run/evaluation namespaces,
signed-long boundaries and all-one unsigned bits. RNG-state integers are
compared without conversion to floating point; JSON float32 output is compared
by its original float32 bits.

The transform cases cover every prohibited member of the 72-card colored,
35-card colorless and 10-card curse candidate pools over eight seeds, plus an
absent prohibited card for each pool. Pool order is deliberately supplied to
stock and native alike to isolate filtering and drawing; actual stock pool
construction remains a separate obligation.

The move cases exercise rolls 0–99 for seven declared Nob histories and the
Shield/Sneaky initial selection. They compare move bytes, not intent damage,
constructor HP, `canVuln=false`, complete move-state mutation or full turns.
Some histories are forced method inputs, not claims of natural reachability.

Block cases cover source alone, dead allies, one valid ally in either slot and
multiple living allies. Empty-power monsters are supplied. A lone source
blocks itself with zero AI draws; even a single valid ally requires one draw.
Escape-intent allies, power interactions and cosmetic MathUtils RNG are not
qualified by these cases.

Artifacts: `local/audits/act1-mechanics-20260928-qualified-branches/` contains
`input.json`, Java logs/classes, `comparisons.json`, `summary.json`, and
`reviewed-coverage.json`. Failed probe setup attempts are preserved in earlier
ignored directories; they are not included as successful evidence.

The coverage overlay marks three monster `getMove` methods and the two system
obligations `RNG_STREAMS` / `CARD_POOLS` as `BRANCH_PARTIAL`. The remaining
1,805 method obligations remain `UNREVIEWED`. The audit gate remains closed;
partial evidence cannot silently promote an entire mechanic to a match.

## Acquisition review

`tools/audit_act1_acquisition.py` writes a source-identified route inventory.
Its latest output is `local/audits/act1-acquisition-20260928-with-route-references.json`.
It keeps candidate membership separate from proven semantics:

- Cards: 117 pool candidates; nine reviewed direct-route entries; five entries
  with no Act1 route found, still pending exhaustive exclusion.
- Relics: 130 pool candidates; 11 reviewed direct-route entries; ten special or
  fallback entries unresolved. Prismatic Shard stays in the pool with its
  declared policy acquisition restriction.
- Potions: 33 ordered stock-pool entries, with acquisition consumers and effects
  still requiring branch evidence.

Important paths retained in the inventory:

- **Curse of the Bell is an Act1 candidate** through Neow boss swap → Calling
  Bell. Boss-tier content cannot all be deferred to later acts.
- Act1 statuses include Burn, Dazed, Slimed and Wound through enemies or cards.
- Apparition, Bite, JAX and Ritual Dagger have reviewed Act2 direct event
  sources. Necronomicurse comes through the Act2 book event/Necronomicon.
  Their exclusion from all other creation and acquisition paths is still open.
- Five face relics come through Face Trader; Golden Idol, Odd Mushroom,
  Spirit Poop and Warped Tongs have Act1 event routes. Circlet's fallback
  generation and actual reachability must be considered separately.

The report assumes a standard run, unlocked Ironclad content and the declared
Shard restriction. Decompiled projections are hashed, but not regenerated from
the JAR in this tool. No conservative target is removed on the basis of pool
membership or a source search alone.

## Regression and next batch

The [compact result](act1-mechanics-20260928-summary.json) records the identities,
comparison counts, acquisition/coverage evidence hashes and regression result.

Fresh current-source replay: **10 complete original trajectories matched at
all 1,748 decision boundaries**. Results are under
`local/audits/act1-mechanics-regression-20260928/`; its target inventory is
`local/audits/a20-act1-targets-mechanics-20260928.json`.
Relevant audit/diagnostics/simulator/content/original checks:
**340 passed, one optional historical-policy-path check skipped**. Ruff passed.

Next evidence priorities:

1. Actual pool construction/order and conditional relic `canSpawn` / ownership
   rules; Neow, event, shop and reward consumer RNG boundaries.
2. Nob Bellow/enrage on skills, damage/vulnerability/death ordering and the
   Dead Adventurer variant; Shield's ally-death transition and bash; Sneaky's
   complete repeated attack turns and AI draw count.
3. Branch-specific card/relic/potion interactions and reward/terminal handling.

The current probes reduce these obligations; they do not establish full Act1
parity or update the historical 77.39% server result.

## Reproduce

From `D:\SLS` in Conda `DL`, use a new output directory:

```powershell
python tools/audit_act1_mechanics.py --stock-jar D:\Steam\steamapps\common\SlayTheSpire\desktop-1.0.jar --java D:\java\bin\java.exe --javac D:\java\bin\javac.exe --output-dir local/audits/act1-mechanics-next
```

`--coverage PATH` optionally annotates a baseline with partial evidence. It
rejects failed results or different stock/native identities and does not
overwrite a known semantic difference.
