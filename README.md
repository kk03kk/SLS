# SLS

SLS is a simulator-first reinforcement-learning project for Slay the Spire.
Its canonical target is an Ironclad A0 run from Neow through the Act 3 victory.
Training starts from random weights and uses no teacher trajectories or behavior
cloning. A self-generated horizon curriculum trains Act 1, then Act 2, then the
complete Act 3 run while retaining one optimizer/model chain.

The policy is a relational Transformer state encoder followed by a GRU belief
state conditioned on the previous action and reward, a variable-size semantic
action scorer, value head, and recurrent PPO. The C++
FullRun simulator is the training authority; CommunicationMod is used later to
test the exported policy in the real game.

## Local setup

Requirements are Python 3.12+, Windows or Linux, and a C++ compiler.

~~~bash
python tools/bootstrap.py --with-model
~~~

The bootstrap installs pinned dependencies, builds the native simulator, and
runs the complete test suite. The server preflight described below is Linux-only.

## Server training

There is one configuration and one exact-resume training chain:

~~~bash
python tools/submit_slurm.py preflight --python "$TRAIN_PY"
python tools/submit_slurm.py benchmark --python "$TRAIN_PY"
python tools/submit_slurm.py smoke --python "$TRAIN_PY"
python tools/submit_slurm.py pilot --python "$TRAIN_PY"
python tools/submit_slurm.py train --python "$TRAIN_PY"
~~~

Smoke learns the Act 1 horizon through 5 million environment decisions, pilot
changes to the Act 2 horizon through 25 million, and train changes to FullRun
through 100 million. Horizon changes preserve model, optimizer, counters and RNG
state while deliberately resetting environments, recurrent state and prior
experience. Pilot cannot start until the smoke promotion gate passes, and train
cannot start until the pilot gate passes. Repeating a submission after a Slurm
time limit resumes the same chain. Worker and shard counts are selected for the
exact native binary used by training.

Exact checkpoints include model and optimizer state, belief memory, previous
experience, simulator environments, episode limits, counters, and all RNG states.
Periodic evaluation uses 128 held-out seeds beginning at 10^12; final evaluation
uses 1,000 different seeds beginning at 2*10^12. An artifact is produced only
after its success/progress gate passes with zero backend errors or limit events.

See [the Chinese NUS runbook](docs/nus-training-zh.md) for the complete
deployment, monitoring, download, and recovery procedure.

## Live game

After the final FullRun promotion gate passes, start a fresh Ironclad A0 game and
attach at the Neow decision:

~~~bash
python tools/play_live.py \
  runs/ironclad-a0-fullrun-v2/ironclad-a0-fullrun-v2.pt \
  --device cpu --max-actions 5
~~~

The live journal binds recurrent memory to the exact model-weight digest and
persists previous-action/reward inputs with each acknowledgement. The controller
resumes only at an acknowledged matching boundary; uncertain delivery stops for
manual recovery because CommunicationMod cannot prove the intended successor.

See [architecture](docs/architecture.md), [repository map](docs/repository-map.md),
and [local runtime](docs/local-runtime.md).
