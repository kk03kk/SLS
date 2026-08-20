# Canonical SLS architecture

## Authority and responsibilities

The original game is the behavioral authority. It is slow and externally
owned, so it is used only for validation. The committed C++ simulator is the
runtime used by tests, evaluation, and training. Equality is defined at
decision boundaries by the canonical Python contracts, not by backend-private
objects or UI timing.

```text
Original Game -> CommunicationMod/Oracle -> OriginalBackend --+
                                                             +-> Decision/Transition
C++ FullRun Simulator -> native module -> SimulatorBackend ---+

Original raw state  ----+
                       +-> canonical parity comparator -> trace/corpus/coverage
Simulator raw state ----+
```

## Policy boundary

`Observation` contains only information visible in the original game. `Action`
contains a stable decision-scoped semantic identity. `Decision` combines one
observation with the current candidate set. `Transition` contains the next
decision, reward, and episode termination.

Wire commands, native action bits, RNG state, and hidden draw order cannot enter
these objects. Validation-only state is carried by `ValidationSnapshot`.

## Parity

`run_paired` resets both backends with the same seed, compares their initial
decision and raw canonical state, selects one action present in both candidate
sets, executes that same semantic action, and repeats. A trace records:

- recursive observation differences;
- candidate-action differences;
- public run/combat/inventory/map differences;
- Oracle/native RNG differences;
- selected canonical action.

The first unexplained difference is a failure. Corpus coverage reports the
seeds, screens, action kinds, semantic steps, complete runs, and matching runs.

## FullRun RL

The model encodes observation entities and the current candidate actions. A
Transformer produces a state representation; a state query scores every action
key and a value head predicts the state value. Candidate list position is never
the identity of an action.

Native environments live in spawned worker processes. Model inference remains
centralized. Fixed-horizon rollout, terminal-safe GAE, clipped PPO, entropy,
value loss, gradient clipping, evaluation, and exact native environment
checkpoints all use the same canonical Decision contract.

Training is technically available, but parity acceptance is the project gate:
large training runs should not start until the selected FullRun corpus has zero
unexplained differences and the required screen/action coverage.

## Reproducibility profiles

Simulator profile:

```text
Python 3.12 checkout
-> tools/bootstrap.py
-> pinned build tools
-> cpp/simulator
-> .build/native/<python-tag>/_lightspeed
-> tests
```

Original-validation profile adds locally owned JARs. They are imported into
`external/original-game`, hashed, checked, and used to compile the committed
Oracle source. See `docs/local-runtime.md`.
