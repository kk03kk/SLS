# A20 Ironclad full-run parity: first implementation batch

This is an **in-progress audit**, not a three-act/Heart qualification. Server job
916291 remains on `6e54adbc0aa6fca81b994490837db382b245e42b`. No PPO, reward,
network, training distribution or native production rules changed in this batch.

## Inventory and identity

`inventory.json` is a source-bound, conservative initial ledger: 803 registry,
shared-engine and flow entries. Registry membership is not reachability. Sentinel
and off-character entries remain candidates requiring review; Prismatic Shard's
policy exclusion is explicit. Existing reports do not automatically grant passes.
The Act4 encounters are explicitly included despite their absence from the older
Act1–3 content-scope encounter sections.

- Native source: `b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f`.
- Training implementation: `9dca86829696613903d07b7fcbc6d4928f1c957c98fd6eaad534d6e76519ba94`.
- Stock JAR: `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`.
- Frozen reference model: `ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c`.

The reference retains its Act1 training provenance and uses the existing explicit
Act2 evaluation contract. No final holdout seeds are used.

## Actual stock results

Two candidates were rechecked with the current native implementation and then
captured from normal stock Neow starts, using production Oracle r7. Both actually
entered their intended Act2 Boss. The batch completed with verified restoration.

| Seed | Witnessed Boss | Strict replay |
| --- | --- | --- |
| 8000011000069 | Collector | All 349 boundaries and terminal result match |
| 8000011000009 | Automaton | First difference at boundary 122, Act1 floor16 |

The Automaton first difference is the position of Dazed in the next hand after
end-turn. Card costs/playability/action positions consequently differ. This is
not dismissed as a display difference. Reckless Charge was played at boundary116,
but its responsibility was initially a hypothesis. Independent bytecode shows the
same basic random insertion formula as native; that alone does not establish
queue timing or the earlier RNG history. No native fix has been guessed.

Raw ordered-pile forensics further localizes the hidden difference to boundary117,
immediately after Reckless Charge: the preceding 13-card draw pile matches, but
the generated Dazed occupies stock index7 versus native index4. The first
policy-visible difference remains boundary122. No stock production RNG state was
observed in the policy payload. The separate
forensic data must not become policy inputs; see `draw-order-localization-r1.json`.

The new shared corpus has 12 scenes, three seeds each (131200000–131200035).
**All 36 stock executions and native multi-boundary replays match.** It covers
specified multihit/Malleable, thorns, Artifact, duration, draw/shuffle, generated
copy, exhaust, Double Tap, potion/Flight, Reaper/Fungi death/healing, Corruption
costs, and HP-loss/energy interactions. These are isolated mechanic results,
not dungeon-flow certification. The thorns lethal branch is not guaranteed;
post-victory healing/reward branches still need additional obligations.

The shared batch used sealed Oracle build r3
(`a722041a55033c01360e7834caa2e8dd200b7f8cf8980b70d926d5a3d76aa9e4`).
Later r4 adds a corpus selector and the three-seed first-divergence probe; it does
not retroactively change the r3 evidence identity. Unexecuted scene drafts and
their original build artifacts were retained locally before source review edits.

The separate Reckless Charge minimal probe (131200036–131200038) also matches
stock across all three runs, including controlled RNG and subsequent draw order.
This rules out declaring its isolated insertion formula wrong on this evidence;
it does not by itself explain the production history. See `reckless-probe-r1.json`.
Oracle r4 production isolation smoke passes and all 63 protected backup entries
match their pre-run hashes after shutdown; see `restoration-r4.json`.

The original owned production process already recorded an independent passive
Discovery clock witness: seed8000011000009, floor16, serial1, **15 retrieval
updates**. This was found in its original stdout, not inferred by searching for
a matching count. Native used its existing default14. Supplying only that
observed count at boundary112 explains all334 public boundaries, actions and
the terminal outcome through Automaton. See `automaton-clock-conditioned-r1.json`.
The strict failure remains intact. This is a clock-conditional match, not an
unconditional production pass, and does not change the training default.
No new native rule error is established by this trajectory.

`progress-r1.json` contains compact hashes, statuses and first public divergence;
it predates the separate clock diagnosis and retains that earlier review state.
Raw stock/native trajectories, bytecode, class inventories and recovery journals
remain under `local/audits/fullrun-parity-20261007` and the existing recovery root.

## Tools and boundaries

- `tools/prepare_fullrun_audit.py` builds a fresh ledger without overwriting one.
- `tools/prepare_shared_scenes.py` binds stock class identities and allocates three
  seeds per scene after scanning recorded audit allocations; collisions fail.
- Existing encounter capture/launch/replay tools accept a new immutable corpus
  through `--manifest`, while retaining the old Act2 default and command.
- Oracle 1.3.1 provides validation-only `parity_scene SCENE_ID [CORPUS_ID]`.
  Corpus identity is bound to actual embedded build members; the launcher also
  checks actual stock class bytes against recorded class hashes.
- Missing source evidence, unsupported late-act context or stale native/build
  identity are rejected. Damage, mask, duration, RNG and cost perturbations are
  detected by the differencer.

**Act3/Act4 controlled setup is not implemented yet.** The new scene entry rejects
these contexts. Reading TheEnding's constructor confirms dungeon/scene/map
initialization beyond an act-number change. The old generic encounter probe must
not be used to claim genuine late-act dungeon context.

Current independent stock projection reads creature HP/block/powers and energy.
Card/potion/relic projection expansion remains required to close the shared
adapter blind spot. Existing adapted comparisons do not satisfy that extra
independence requirement by themselves.

TheBeyond's reviewed boss selection also depends on stock boss-seen unlock flags.
Native's three-boss shuffle corresponds to the fully-seen branch. Before natural
Act3 comparisons, record the actual unlock precondition instead of assuming it
from ascension. This is a context/precondition risk, not a confirmed wrong A20
boss rule.

## Resume order and training gate

1. Preserve the strict draw-order failure and its independently witnessed
   Discovery-clock explanation; unobserved timing combinations remain unverified.
2. Expand direct stock card fields and establish actual TheBeyond/TheEnding room
   setup before running the late-act corpora.
3. Execute all 16 Act3 encounters and their designated elite/Boss branches.
4. Execute six ordered double-Boss flows, keys, Act4 route, shield/spear and Heart.
5. Fill reachable content branches and late-act natural-start coverage.

The Collector witness is one trajectory, not an enemy-wide certificate. These
captures are not win-rate estimates or checkpoint selection evidence. The first
Act1 divergence was reviewed and explained under the independently observed
clock condition; unconditional frame-timing equivalence remains unqualified. Current
server results keep their frozen identity. Any future rule fix requires a new
native identity and explicit compatibility/comparability decision.

Final local suite: **1223 passed, one historical-artifact skip, four expected
warnings** (178.86s). Ruff and all33 training configurations pass. AGENTS.md hash
is unchanged. No native production code changed, so no native rules migration is
registered. These checks validate this tool batch, not the unfinished full-run
parity obligations. The subsequent historical-clock verifier change also passed
its targeted tests. Offline replay verifies the sealed historical JAR and each
member against its original build record; launch/install still require current
source identity. Original evidence is not relabelled with today's Oracle source.
