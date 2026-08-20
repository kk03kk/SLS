# Repository map

## Root

- `README.md`: clean-checkout setup and project scope.
- `pyproject.toml`: Python 3.12 package, pinned test/model extras, package data.
- `.gitignore`: excludes builds, imported game files, runs, checkpoints, logs,
  native binaries, Java classes/JARs, and Python caches.

## `src/sls`

- `__init__.py`: package identity and baseline version.
- `curriculum.py`: FullRun profile, Act/Heart horizon, and termination rules.

### `contracts`

- `action.py`: canonical ActionKind, semantic Action, candidate identity, private-field guards.
- `observation.py`: public entities, cards, enemies, map/shop state, Observation, hidden-state guards.
- `decision.py`: Decision and Transition invariants.
- `validation.py`: parity-only ValidationSnapshot.
- `__init__.py`: public contract exports.

### `backends`

- `protocol.py`: Backend and CheckpointableBackend protocols.
- `__init__.py`: backend protocol exports.

`backends/simulator`:

- `native.py`: ABI-specific `_lightspeed` loader from `.build/native`.
- `environment.py`: native snapshot adapter, candidate-to-bit map, reset/step/checkpoint.
- `__init__.py`: SimulatorBackend and curriculum profile exports.

`backends/original`:

- `transport.py`: newline-delimited CommunicationMod stdin/stdout transport.
- `session.py`: ready-state loop and advertised-command validation.
- `adapter.py`: CommunicationMod payload to canonical Observation/Action and private command map.
- `environment.py`: OriginalBackend reset/step and protocol-only UI folding.
- `__init__.py`: Original validation exports.

### `content`

- `registry.json`: generated canonical C++ enum IDs and source-header hashes.
- `registry.py`: registry loading and structural validation.
- `normalize.py`: cross-backend card, potion, power, monster, relic, and event ID normalization.
- `seed.py`: Slay the Spire seed-string/integer conversion.
- `__init__.py`: registry exports.

### `model`

- `batching.py`: deterministic entity/action feature batching for variable candidates.
- `transformer.py`: ModelConfig, entity Transformer, candidate scorer, value head.
- `__init__.py`: model exports.

### `rl`

- `workers.py`: spawned native environments and checkpoint commands.
- `rollout.py`: rollout tensors and terminal-safe generalized advantage estimation.
- `ppo.py`: centralized inference, rollout collection, clipped PPO optimization.
- `checkpoint.py`: atomic model/optimizer/RNG/native-state save and exact contract restore.
- `evaluate.py`: deterministic fixed-seed FullRun evaluation.
- `__init__.py`: RL exports.

### `validation`

- `compare.py`: Original/native public-state and RNG canonicalization plus command translation helpers.
- `diff.py`: stable recursive field-path comparison.
- `policies.py`: deterministic action selection from the intersection of candidate sets.
- `runner.py`: paired same-seed/same-action differential loop.
- `trace.py`: versioned JSON trace records.
- `coverage.py`: seed/screen/action/step coverage summary.
- `__init__.py`: validation exports.

## `cpp/simulator`

- `CMakeLists.txt`: the only native build target; compiles the engine and Python module.
- `SLS_VENDOR.json`: audited-fork upstream identity and no-download policy.
- `LICENSE.lightspeed.md`: upstream simulator license.
- `cmake/zig-windows-toolchain.cmake`: reproducible Windows Zig target.
- `python/module.cpp`: pybind11 binding for battle probes, FullRun state, actions, RNG, and checkpoints.
- `third_party/nlohmann/nlohmann/json.hpp`: only vendored JSON header used by SaveFile code.

`include/constants`:

- `Cards.h`, `CardPools.h`: card enums, metadata, and pools.
- `Relics.h`, `RelicPools.h`: relic enums, tiers, and pools.
- `Potions.h`: potion enums and rarity data.
- `CharacterClasses.h`: character enum.
- `MonsterIds.h`, `MonsterEncounters.h`, `MonsterMoves.h`: enemy and encounter identities/AI moves.
- `MonsterStatusEffects.h`, `PlayerStatusEffects.h`: combat status enums.
- `Events.h`, `Rooms.h`: event and room identities.
- `SaveFileMappings.h`: save-file string mappings.
- `Misc.h`: shared constants.

`include/combat`:

- `BattleContext.h`: complete mutable combat state machine.
- `Actions.h`, `ActionQueue.h`: combat effects and queued execution.
- `CardInstance.h`, `CardManager.h`, `CardQueue.h`: runtime cards and card queues.
- `CardSelectInfo.h`, `InputState.h`: decision-boundary input state.
- `Monster.h`, `MonsterGroup.h`: enemy state and group behavior.
- `Player.h`: player combat state.

`include/game`:

- `GameContext.h`, `Game.h`: FullRun state machine and run operations.
- `Card.h`, `Deck.h`: persistent card/deck state.
- `Map.h`: Act map generation and traversal.
- `Neow.h`: Neow choices.
- `Random.h`: original-compatible RNG streams.
- `RelicContainer.h`, `Rewards.h`, `Shop.h`: inventory, rewards, and merchant state.
- `SaveFile.h`: serialization/checkpoint support.

`include/sim`:

- `BattleSimulator.h`, `ConsoleSimulator.h`: simulation drivers.
- `SimHelpers.h`, `PrintHelpers.h`: simulation utilities.
- `RandomAgent.h`: native random test agent.
- `search/Action.h`, `search/GameAction.h`: encoded combat/run actions.
- `search/SimpleAgent.h`, `ExpertKnowledge.h`: deterministic native scripted policy.
- `search/BattleScumSearcher2.h`, `ScumSearchAgent2.h`: search utilities retained as simulator assets.

`src/combat` implements the matching combat headers:

- `Actions.cpp`, `BattleContext.cpp`, `CardInstance.cpp`, `CardManager.cpp`,
  `CardQueue.cpp`, `Monster.cpp`, `MonsterGroup.cpp`, `MonsterMoveDamage.cpp`,
  `MonsterSpecific.cpp`, `Player.cpp`.

`src/game` implements FullRun behavior:

- `Card.cpp`, `CombatReward.cpp`, `Deck.cpp`, `Game.cpp`, `GameContext.cpp`,
  `Map.cpp`, `Neow.cpp`, `SaveFile.cpp`, `Shop.cpp`.

`src/sim` implements simulator/search utilities:

- `BattleSimulator.cpp`, `ConsoleSimulator.cpp`, `PrintHelpers.cpp`, `SimHelpers.cpp`.
- `search/Action.cpp`, `BattleScumSearcher2.cpp`, `ExpertKnowledge.cpp`,
  `GameAction.cpp`, `ScumSearchAgent2.cpp`, `SimpleAgent.cpp`.

`include/data_structure/fixed_list.h` is the fixed-capacity container used by
the engine. `include/sts_common.h` contains shared low-level declarations.

## `java/oracle-mod`

- `ModTheSpire.json`: ModTheSpire manifest.
- `README.md`: Oracle build boundary.
- `BatchResetPatch.java`: in-process return-to-menu command.
- `CardGroupRngPatch.java`: original card-group RNG interception.
- `CardStatePatch.java`: additional public card state for CommunicationMod.
- `CommunicationStatePatch.java`: RNG/key/setup evidence injection.
- `DungeonSeedPatch.java`: parity RNG initialization on new/load run.
- `OracleScenarioPatch.java`: focused original-rule test scenarios.
- `ParityRng.java`: RNG state extraction.

The focused Oracle scenarios provide Original evidence for isolated simulator
mechanisms; they are not a second environment or training contract.

## `reference/original-game`

- `manifest.json`: ownership boundary and expected external runtime files.
- `decompiled/`: CFR output mirroring original Java packages. Its 4,874 files
  are static rule-reference evidence only; none is imported, compiled, or packaged.

## `tools`

- `bootstrap.py`: editable install, native build, and test workflow.
- `build_native.py`: pinned CMake/Ninja/pybind11/Zig native build.
- `import_original_game.py`: copy locally owned JARs and record hashes.
- `check_original.py`: verify imported hashes and Java compiler.
- `build_oracle.py`: deterministic Oracle JAR compilation.
- `generate_content_registry.py`: regenerate registry from C++ headers.
- `validate_full_run.py`: one-seed CommunicationMod parity entry.
- `validate_corpus.py`: TOML-driven multi-seed parity corpus, acceptance gate, traces, and coverage summary.
- `train_full_run.py`: canonical FullRun PPO entry and resume/save/evaluate loop.

## `configs`

- `validation/full_run.toml`: profile, parity seeds, output, step limit, RNG, and acceptance gate.
- `train/full_run.toml`: profile, worker, model, PPO, checkpoint, and evaluation settings.
- `train/smoke.toml`: one-update CPU integration configuration.

## `requirements`

- `test.lock`: exact pytest and test-runner dependency versions.
- `model.lock`: exact Torch 2.6 and transitive model dependency versions.

## `tests`

- `conftest.py`: source-layout import setup.
- `contracts/test_action.py`: action identity/private metadata.
- `contracts/test_observation.py`: policy visibility and hidden draw order.
- `contracts/test_decision.py`: candidate and termination invariants.
- `content/test_registry.py`: generated registry validation.
- `original/test_adapter.py`: map/combat CommunicationMod adaptation.
- `original/test_original_backend.py`: start/reset and Neow UI folding.
- `simulator/test_simulator_backend.py`: native FullRun canonical reset.
- `simulator/test_native_mechanisms.py`: native RNG/rules/lifecycle probes and exact checkpoint replay.
- `model/test_policy.py`: variable-candidate policy shapes.
- `rl/test_rollout.py`: terminal-safe GAE.
- `rl/test_training_smoke.py`: spawned native rollout, PPO update, checkpoint restore.
- `validation/test_compare.py`: public state canonical equality.
- `validation/test_runner.py`: trace and coverage behavior.
- `test_structure.py`: absence of legacy package roots and presence of canonical assets.
