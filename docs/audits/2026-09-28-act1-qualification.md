# Phase 1: Ironclad A20 Act1 qualification

## Decision

Status: **in progress; complete semantic qualification has not passed**.
At the maintainer's request, further broad qualification work is paused after
the [stage review](2026-09-28-act1-review-and-training-direction.md); the remaining
obligations below are retained. This pause does not certify complete parity.
The current build reproduces the ten saved stock policy trajectories, but
those trajectories do not establish all reachable branches or internal RNG
parity. Continue environment qualification before evaluating 56M in the new
environment or scheduling another training run.

The first batch did not change native gameplay, observation encoding or policy
weights. Its native source/artifact identities were:

- Source: `c28b45cefa2e2d8529ae21c53bcf722b1ab33efc75693d759887da554b2480a8`.
- Windows artifact: `bd1623f455195ee655f417601b81d5e21917672ea4c915249fc59ac5a8350e62`.
- Stock JAR: `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`.

The subsequent [acquisition, gremlin and RNG batch](2026-09-28-act1-mechanics.md)
fixed transform selection and rebuilt native. Use that record for the latest
source identity; the first-batch results below remain historical evidence.

## Completed work

### Make qualification failures visible

`validate_semantic_coverage` now requires independent stock and native
identities and before/actions/after/RNG comparisons for `SEMANTIC_UI_FOLD`
and `PRESENTATION_ONLY`, as well as `SEMANTIC_MATCH`. The two exemption
classifications require a written rationale. Malformed hashes and evidence
that disagrees with the manifest's declared environment are rejected.

An A20 Act1 complete-scope check now requires the corresponding target
inventory and method-obligation baseline. It checks their stock/native/scope
identities and rejects missing method obligations or edited method identities.
It does not silently use the A0 full-run catalog. Mixed semantic and rendering
methods retain a semantic content classification.

This validator is an audit tool, not a newly installed training startup gate.
Passing schema checks does not authenticate a reviewer's claims or prove that
all bytecode branches have been enumerated. Review evidence is still necessary.

### Preserve and replay the original corpus

[The pinned regression corpus](act1-regression-corpus.json) names ten original
captures and their SHA256 hashes. `tools/replay_act1_corpus.py` loads the
historical policy once, verifies the stock JAR, policy identity and input
evidence, and captures the current simulator against every saved original
route. It refuses an existing output directory so previous evidence survives.

The current replay passed **10/10 complete trajectories, 1,748/1,748 decision
boundaries**. It compares observations, legal action sets and order, policy
inputs, selected actions, recurrent state, and terminal/success flags.

Artifacts are under ignored
`local/audits/act1-qualification-20260928-threads16/`, including per-seed
captures/comparisons, `summary.json`, and `semantic-coverage.json`.
The tracked [compact result](act1-qualification-20260928-summary.json) records
the result and remaining content gaps. Full original evidence remains under
`local/reports/`; it is not bundled with a fresh Git clone.

The original captures predate embedded game/runtime identities. Their JAR
provenance is declared by the pinned historical corpus and earlier audit,
not independently recovered from those old JSONL headers. New captures now
record native source/artifact or stock JAR identity, plus Torch version,
CPU thread settings and MKLDNN setting. When both sides record inference
settings, incompatible settings fail the comparison.

### Resolve a replay reproducibility problem

The first replay script used one CPU thread. That produced memory/action
hash differences despite equal policy inputs at the first divergent boundary.
A seed-0 control using the workstation's 16-thread setting reproduced all
160 boundaries; the 16-thread corpus replay then passed all ten seeds.

The corpus now pins 16 threads. The failed one-thread results are preserved
under `local/audits/act1-qualification-20260928/`; the seed-0 control is
`local/audits/act1-qualification-thread-control-seed0.jsonl`.
This is evidence of inference-setting sensitivity, not evidence of a stock
gameplay rule mismatch. Do not weaken exact comparisons to make it pass.

### Audit event eligibility boundaries

The pinned stock `AbstractDungeon.getEvent/getShrine` and native
`GameContext::canAddEvent/canAddOneTimeEvent` agree on the reviewed gold
thresholds: Cleric requires at least 35 gold; Woman in Blue at least 50.
Native regressions now check both sides of each boundary (34/35 and 49/50).

The forced one-time-event test uses an always-eligible sentinel after the
tested event, so the result tests eligibility rather than a random selection
between two valid events. These are source-backed native boundary tests;
they are not new original-runtime trajectory evidence.

## Remaining work, in execution order

### 1. Complete generation and acquisition reachability

The candidate inventory still conservatively includes 131 cards, 151 relics,
33 potions, 24 events, 22 encounters and 25 monsters. Method inventory contains
1,810 obligations, currently unreviewed. These are method-review tasks, not
1,810 confirmed bugs; rendering and helper methods will need justified
classification, and gameplay methods need branch enumeration.

Trace card/relic/potion acquisition through Neow (including boss swap),
rewards, shops, transforms, random generation and Act1 events. For each
candidate, record stock-reachable, policy-reachable, forced-only, outside
Act1, or unresolved, with source/bytecode evidence. Do not discard a special
card merely because its usual event occurs in Act2: other creation routes
must first be excluded. Keep Prismatic Shard in stock offer/RNG pools while
documenting its policy acquisition restriction. Note For Yourself remains
outside natural A20 generation.

### 2. Fill targeted combat evidence

The ten routes observe 45 cards, 22 relics, 20 potions and 22 monsters. Their
unobserved monster candidates are Gremlin Nob, Shield Gremlin and Sneaky
Gremlin. Nob has separate move probes; those do not establish its complete
combat semantics. Prioritize controlled original/native comparisons for:

- Nob: initial buff, A18+ attack cycle, skill-triggered strength and death.
- Shield Gremlin: random ally block, self-block when alone, ally death and
  transition to bash. Stock `GainBlockRandomMonsterAction` excludes dying and
  escape-intent allies; escape interactions belong to the later-act audit.
- Sneaky Gremlin: A20 HP range, attack damage, repeated attacks and no extra
  AI draws between turns. Gremlin Leader death/escape is outside Act1.
- All three Act1 bosses: late phases, multi-target damage/death ordering,
  split/summon and reward/terminal transitions beyond observed route branches.

Use bounded forced scenarios with fixed before state and independent stock
before/action/after/RNG evidence. Save each discrepancy and its smallest
reproducer before fixing it. Replay the existing corpus after native or
adapter changes; launch the original game only to capture missing evidence.

### 3. Qualify system and interaction branches

Review RNG streams, shop/offer order and prices, rewards, death and healing,
card upgrade/cost changes, potion capacity/Sozu, relic hooks, mandatory UI
folds and checkpoint round trips. Internal RNG is not compared by the current
policy-only corpus and needs explicit diagnostic or bytecode evidence.

A previously matched full route is regression evidence, not evidence that a
card's unplayed upgraded branch or an unowned relic's hook is correct.
Attach reviewed branches and evidence to the generated method baseline;
retain unresolved rows as blockers. Do not remove a method to turn a report
green.

## Checks in this batch

- Relevant audit, diagnostics, simulator, content and original tests:
  **325 passed, 1 skipped**. The skip is an optional historical-policy test
  whose old local artifact path is unavailable; the pinned ten-route corpus
  separately uses the preserved archive policy.
- Ruff: passed.
- Ten-route CPU replay: passed at the pinned inference setting.
- No training or model-strength evaluation was run. No original game launch
  was needed for this batch. Checkpoints and original evidence were preserved.

## Replay command

Run from `D:\SLS` in Conda `DL`; choose a new output directory each time:

```powershell
python tools/replay_act1_corpus.py --corpus docs/audits/act1-regression-corpus.json --artifact runs/archives/policies/ironclad-a20-act1-38m-best.pt --stock-jar D:\Steam\steamapps\common\SlayTheSpire\desktop-1.0.jar --targets local/audits/a20-act1-targets-final.json --output-dir local/audits/act1-regression-next
```

After native source changes, rebuild and regenerate the target inventory
first. Qualification must bind to the new source, not reuse a stale digest.
