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
- `docs/local-runtime.md`: files that must come from the local game/Mod installation.

## Training

FullRun PPO uses the same canonical Decision contract as validation. Build the
native simulator and install the model extra, then run:

```bash
python tools/bootstrap.py --with-model
python tools/train_full_run.py --config configs/train/full_run.toml
```

Large training runs are gated on the selected Original/Simulator parity corpus;
the presence of the trainer does not imply that parity acceptance is complete.
