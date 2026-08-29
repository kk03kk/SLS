# SLS

SLS is a simulator-first reinforcement-learning project for Slay the Spire.
The repository contains a native C++ FullRun simulator, one canonical
observation/action contract, a relational Transformer policy, PPO training,
exact checkpoints, and a live-game controller for CommunicationMod.

The project deliberately trains from random weights. It does not contain or
consume teacher trajectories, behavior-cloning corpora, Original-game truth
bundles, or parity release gates. The simulator is the training environment;
real-game runs are downstream deployment tests.

## Setup

Requirements: Python 3.12+, Windows or Linux, and a C++ compiler. The Windows
builder installs its pinned Zig toolchain.

```bash
python -m pip install -e ".[test]"
python tools/build_native.py
pytest
```

Install the model dependencies and run the server checks:

```bash
python tools/bootstrap.py --with-model
python tools/preflight_training.py --allow-cpu  # omit --allow-cpu on the GPU server
python tools/benchmark_workers.py
```

## Training

The current checked-in configs are the stable Act 1 baseline used while the
A0 FullRun recurrent training path is built.

```bash
python tools/train_full_run.py --config configs/train/act1_smoke.toml
python tools/train_full_run.py --config configs/train/act1_pilot.toml
python tools/train_full_run.py --config configs/train/act1_train.toml --resume auto
```

Every run starts from natural simulator resets and random policy weights. Exact
resume restores the optimizer, RNGs, worker environments, and episode-limit
state. Generated builds, runs, checkpoints, logs, game JARs, and saves are
ignored by Git.

## Live game

Export a simulator-trained checkpoint, launch Slay the Spire with
CommunicationMod, then attach the controller:

```bash
python tools/export_policy.py runs/act1-train-v3/latest.pt \
  --output runs/ironclad.pt --ascension-min 0 --ascension-max 0 --goal ACT1
python tools/play_live.py runs/ironclad.pt --device cuda --max-actions 5
```

Artifacts are explicitly marked `simulator_only`. Live play journals every
intent and acknowledgement and refuses uncertain resends.

See [architecture](docs/architecture.md), [repository map](docs/repository-map.md),
and [local runtime](docs/local-runtime.md).
