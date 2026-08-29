# SLS

SLS is a simulator-first reinforcement-learning project for Slay the Spire.
Its canonical target is an Ironclad A0 run from Neow through the Act 3 victory.
Training starts from random weights and uses no teacher trajectories, behavior
cloning, samples, or automatic curriculum.

The policy is a relational Transformer state encoder followed by a GRU memory,
variable-size semantic action scorer, value head, and recurrent PPO. The C++
FullRun simulator is the training authority; CommunicationMod is used later to
test the exported policy in the real game.

## Local setup

Requirements are Python 3.12+, Windows or Linux, and a C++ compiler.

~~~bash
python tools/bootstrap.py --with-model
python tools/preflight_training.py --allow-cpu
~~~

The bootstrap installs pinned dependencies, builds the native simulator, and
runs the complete test suite.

## Server training

There is one configuration and one exact-resume training chain:

~~~bash
python tools/submit_slurm.py preflight --python "$TRAIN_PY"
python tools/submit_slurm.py benchmark --python "$TRAIN_PY"
python tools/submit_slurm.py smoke --python "$TRAIN_PY" \
  --initialize-from runs/ironclad-a0-fullrun-v1/best_success.pt
python tools/submit_slurm.py pilot --python "$TRAIN_PY"
python tools/submit_slurm.py train --python "$TRAIN_PY"
~~~

Smoke stops at 100,000 environment decisions, pilot continues the same
checkpoint to 2,000,000, and train continues to 50,000,000. Repeating the
training submission after a Slurm time limit resumes the same chain. Worker and
shard counts are selected automatically by the benchmark for the current Git
commit and simulator source.

Exact checkpoints include model and optimizer state, GRU memory, simulator
environments, episode limits, counters, and all RNG states. Periodic evaluation
uses 128 held-out seeds beginning at 10^12; final evaluation uses 1,000
different seeds beginning at 2*10^12.

See [the Chinese NUS runbook](docs/nus-training-zh.md) for the complete
deployment, monitoring, download, and recovery procedure.

## Live game

Each completed stage exports a simulator-only FullRun artifact. Start a fresh
Ironclad A0 game and attach at the Neow decision:

~~~bash
python tools/play_live.py \
  runs/ironclad-a0-fullrun-v2/ironclad-a0-fullrun-v2-smoke.pt \
  --device cpu --max-actions 5
~~~

The live journal persists recurrent memory with each intent/acknowledgement.
The controller can resume only when the current boundary matches the journal;
it rejects unprovable mid-run attachment and uncertain action resends.

See [architecture](docs/architecture.md), [repository map](docs/repository-map.md),
and [local runtime](docs/local-runtime.md).
