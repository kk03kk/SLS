# spirecomm
A package for using Communication Mod with Slay the Spire, plus a simple AI

The full-game simulator is governed by the pinned
[`parity contract`](docs/PARITY_CONTRACT.md), the
[`10-step roadmap`](docs/ROADMAP.md), the
[`content registry`](docs/CONTENT_REGISTRY.md), and
[`combat mechanics matrix`](docs/COMBAT_MECHANICS.md), and
[`reference_build.json`](reference_build.json).
Verify that the installed original-game oracle still matches the pinned build:

```powershell
.\scripts\verify_reference_build.ps1
```

## Communication Mod

Communication Mod is a mod that allows communication between Slay the Spire and an external process. It can be found here:

https://github.com/ForgottenArbiter/CommunicationMod

The spirecomm package facilitates communicating with Slay the Spire through Communication Mod and accessing the state of the game.

## Requirements:

- Python 3.5+
- kivy, only for the example GUI for Communication Mod, found in utilities

## Running the AI:

To run a simple Slay the Spire AI, configure Communication Mod to run main.py

For the verified local Windows setup, launch ModTheSpire directly with its
bundled Java 8 runtime instead of automating the Steam UI:

```powershell
cd D:\SLS\spirecomm
.\scripts\start_original_sts.ps1
```

The launcher fixes the working directory to the Slay the Spire installation,
selects `basemod,CommunicationMod` by mod ID, captures Java logs under
`logs\launcher`, and reports `READY` only after Python receives a combat JSON
state. It refuses to create a duplicate game process by default.

## Installing spirecomm:

Run `python setup.py install` from the distribution root directory

## Minimal OriginalSTSEnv + RandomBattleAgent

`spirecomm.envs.OriginalSTSEnv` reads CommunicationMod JSON and exposes a
Gymnasium-style `reset()` / `step()` interface for one battle. Rich parsed
state and the current legal commands are returned in `info["battle"]` and
`info["legal_actions"]`.

CommunicationMod is currently configured to launch `act1_corpus_main.py`.
The runner starts an Ironclad run, chooses safe pre-combat options, plays one
battle using uniformly random legal actions, saves a differential trace, and
then exits. Diagnostics are written only under `logs/`; stdout remains reserved
for CommunicationMod commands.

Run the offline checks with:

```powershell
conda run -n DL python -m unittest discover -s tests -v
```

The versioned content denominator contains all 781 registered base-game IDs
across characters, cards, relics, potions, monsters, encounters and events.
Verify that the committed artifact still matches the pinned upstream headers:

```powershell
conda run -n DL python scripts\build_content_registry.py --check
```

## Headless SimulatorSTSEnv

`SimulatorSTSEnv` uses the audited MIT-licensed `sts_lightspeed` engine at
commit `7476a81954020087da31d41d16fddf475746ec2d`. It shares `BaseSTSEnv`, the
observation space, the 128-entry discrete action space, semantic legal actions,
and action masks with `OriginalSTSEnv`.

Build the Python 3.12 native module on Windows from the `DL` environment:

```powershell
conda run -n DL python scripts\build_lightspeed.py
```

The script installs pinned CMake, Ninja, Zig/Clang and pybind11 into the ignored
`.native-build` cache, checks out the audited engine commit, and writes the
compiled `_lightspeed` module into `spirecomm\simulator`. A Visual Studio C++
workload is not required.

Minimal use:

```python
from spirecomm.envs import SimulatorSTSEnv

env = SimulatorSTSEnv(encounter="JAW_WORM", ascension=0)
observation, info = env.reset(seed=123)
while True:
    action = env.action_space.sample(mask=info["action_mask"])
    observation, reward, terminated, truncated, info = env.step(action)
    if terminated or truncated:
        break
```

Current tested scope loads all 75 Ironclad cards (base and upgraded forms) and
all 20 Act 1 encounter definitions. Card semantics, encounter semantics and
cross-engine parity are still being expanded; this is not yet a claim of full
Act 1 equivalence. Custom decks can be supplied through
`reset(options={"deck": [...]})`. Unsupported simulator action kinds fail
explicitly instead of silently issuing an invalid command.

The 20 A0 Act 1 enemy state machines now have executable coverage: all 14
normal encounter compositions, normal-enemy move-history constraints, all
three elite mechanics, and all three boss cycles/phase transitions. The audit
also carries reproducible fixes for Lagavulin's natural wake and Red Slaver's
one-use Entangle flag. These tests establish simulator-internal rules and
invariants; arbitrary-seed equivalence with the original Java game still
requires more CommunicationMod differential traces.

Inspect deterministic enemy intents without launching the game:

```powershell
conda run -n DL python scripts\probe_act1_state_machines.py --encounter ALL --seeds 4 --turns 10
```

Combat fixtures may also provide potion slots and replace the current relic set:

```python
observation, info = env.reset(seed=123, options={
    "potions": ["Fire Potion", "Block Potion"],
    "relics": ["Anchor", {"id": "Happy Flower", "counter": 2}],
})
```

Potion use/discard actions are part of the shared `LegalAction` API and potion
identities/usability are present in the numeric observation. Multi-card potion
screens use sequential `choose` actions followed by `proceed`, matching the
CommunicationMod interaction model. All 33 potions in the Ironclad pool have a
safe tested action path; individual potion semantics still need deeper parity
fixtures. Relic identities and live counters are included in rich agent state,
while full numeric relic vocabulary and exhaustive relic semantics remain open.

A live simulator battle can be cloned at a normal player decision point:

```python
from spirecomm.checkpoints import export_combat_checkpoint

checkpoint = export_combat_checkpoint(env.payload)
clone = SimulatorSTSEnv()
clone_observation, clone_info = clone.reset(options={"checkpoint": checkpoint})
```

Checkpoint schema v1 preserves ordered card piles and dynamic card fields,
player and monster hidden combat fields, relic counters, potion slots, monster
group flags, and all six battle RNG streams. Restoring while a card-selection
prompt is open is deliberately rejected for now because the upstream engine's
pending continuation queue is not serializable.

Run a throughput benchmark:

```powershell
conda run -n DL python scripts\benchmark_simulator.py --episodes 1000
```

### Original-game differential traces

To record a new original-game trace, configure CommunicationMod to launch
`golden_trace_main.py`. It writes `logs\golden_original.json` while keeping
stdout protocol-only. Existing protocol logs can also be imported and replayed:

```powershell
conda run -n DL python scripts\import_protocol_trace.py `
  logs\random_battle_protocol.jsonl logs\golden_imported.json
conda run -n DL python scripts\compare_trace.py logs\golden_imported.json
```

The comparator stops at the first divergent turn and field. The checked-in
implementation has been validated against the existing 24-action real-game
Jaw Worm protocol trace, including Panacea, Artifact, enemy intents and the
post-combat Burning Blood heal.

Newly recorded version-2 traces additionally compare the complete ordered
semantic legal-action list, potion slots, relic counters, enemy `move_id`,
dynamic card fields, rewards, termination flags and final outcome. Version-1
traces remain replayable, but fields that were not recorded in them cannot be
retroactively verified. `logs\golden_strict_jaw_worm.json` is the strict
version-2 import of the existing 24-action CommunicationMod session.

For corpus collection, configure CommunicationMod to launch
`act1_corpus_main.py`. `STS_CORPUS_BATTLES` controls how many consecutive
battles the process records (default `1`); traces are grouped by inferred Act 1
encounter under `logs\act1_corpus`. Replay the corpus and print the 20-encounter
coverage matrix with:

```powershell
conda run -n DL python scripts\compare_act1_corpus.py `
  --include logs\golden_strict_jaw_worm.json
```
