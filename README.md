# SLS

SLS is a reinforcement-learning interface around an audited fork of the
`sts_lightspeed` simulator. The native FullRun simulator is the high-speed
runtime; the original game, CommunicationMod, and the Oracle Mod are used to
validate behavior at canonical decision boundaries.

The current baseline intentionally does not preserve legacy training APIs or
their checkpoints. There is one public contract:

```text
Observation + candidate Actions -> Decision -> Backend.step(Action) -> Transition
```

Parity-only RNG and continuation state live in `ValidationSnapshot` and never
enter policy observations.

## Requirements

- Python 3.12+
- Windows or Linux
- A C++ compiler on Linux; the Windows build tool installs its pinned Zig toolchain

## Simulator setup

```bash
python -m pip install -e ".[test]"
python tools/build_native.py
pytest
```

## Original-game validation

Original game and Mod JARs are external assets and are never committed. Import
them explicitly, build the validation mod, then configure CommunicationMod to
launch `tools/validate_full_run.py`:

```bash
python tools/import_original_game.py \
  --game-jar /path/to/desktop-1.0.jar \
  --modthespire /path/to/ModTheSpire.jar \
  --basemod /path/to/BaseMod.jar \
  --communicationmod /path/to/CommunicationMod.jar
python tools/build_oracle.py
python tools/validate_corpus.py --config configs/validation/full_run.toml
```

Generated builds, imported JARs, validation results, logs, and checkpoints are
ignored. The committed decompiled Java tree under `reference/original-game` is
reference evidence, not runtime code.

## Documentation

The Original truth-bundle, offline replay, minimal-regression, and official
autosave workflow is documented in
[`docs/original-truth-workflow.md`](docs/original-truth-workflow.md).

- `docs/architecture.md`: contracts, backend relationship, parity, and RL flow.
- `docs/repository-map.md`: responsibility of every first-party directory/file.
- `docs/static-audit.md`: local static evidence, fixes, known gaps, and proof boundary.
- `docs/ironclad-fullrun-expansion.md`: source-generated A0-A20 closure, current FullRun evidence, and blocked readiness stages.
- `docs/local-runtime.md`: files that must come from the local game/Mod installation.

## Training and live play

PPO and production inference use the same stateless Decision contract. Policy
v3 is a relational, screen-aware model with exact content tokens, explicit map
edges, card zones, entity types, and semantic action references. Training has
two explicit safety levels. `EXPERIMENTAL` smoke/pilot runs retain native,
Decision, regression, CUDA, vocabulary, source-identity, and exact-resume
checks, but do not claim policy transfer. `PRODUCTION` additionally requires a
complete `policy-transfer-v1` gate, including the Original policy canary.

```bash
python tools/bootstrap.py --with-model
python tools/generate_policy_vocabulary.py --check
python tools/preflight_training.py --mode experimental
python tools/generate_teacher_corpus.py --seed-count 1000 --output runs/teacher-act1.json.gz
python tools/generate_teacher_corpus.py --seed-start 20000 --seed-count 100 --output runs/teacher-act1-validation.json.gz
python tools/pretrain_behavior.py runs/teacher-act1.json.gz --validation-corpus runs/teacher-act1-validation.json.gz --output runs/act1-bc.pt --artifact-output runs/act1-bc-artifact.pt --device cuda
python tools/train_full_run.py --config configs/train/act1_smoke.toml --warm-start runs/act1-bc.pt
```

Resume an exact run, or export and attach the current real Ironclad game:

```bash
python tools/train_full_run.py --config configs/train/act1_train.toml --resume auto
python tools/export_policy.py runs/heart-a0-a20/latest.pt --output runs/ironclad-heart.pt --ascension-min 0 --ascension-max 20 --goal HEART
python tools/play_live.py runs/ironclad-heart.pt --device cuda
```

Experimental checkpoints and artifacts are permanently marked and are rejected
by production export, production resume/warm-start, and live play. The live
controller uses deterministic argmax, rejects Prismatic Shard runs,
journals intent/ack records, refuses uncertain resends, supports Ctrl-C, and
can attach at any public decision boundary. `act1_train.toml` is deliberately
named for its real horizon; Heart naming is reserved for a Heart curriculum.
Launch the Mod with JVM property `-Dsls.oracle.mode=production` to suppress
validation-only RNG, continuation, timing, and scenario fields on the wire.

NUS Linux/Slurm 的拉取、分层 preflight、teacher/BC、smoke 和 pilot 命令见
[`docs/nus-training-zh.md`](docs/nus-training-zh.md)。仓库包含构建 production gate
所需的最小不可变 Original 路线；最终 production gate 仍需已接受的 Original canary。
