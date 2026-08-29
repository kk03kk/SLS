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

Policy input `sls-policy-input-v3` encodes observation entities with exact
content/category tokens, numeric presence masks, and typed map adjacency. Each
candidate action has a kind, fixed metadata slots, and five masked entity
references (`subject`, `target`, `option`, `node`, `reward`). Protocol-only
options are promoted to stable decision-scoped entities. Unknown base-game
content, metadata, and unresolved references fail instead of falling into a
hash bucket. A Transformer produces a state representation; a state query
scores every action key and a value head predicts the state value. Candidate
or entity list position is never semantic identity.

Native environments live in spawned worker processes. Model inference remains
centralized. Fixed-horizon rollout, terminal-safe GAE, clipped PPO, entropy,
value loss, gradient clipping, evaluation, and exact native environment
checkpoints all use the same canonical Decision contract.

Exact resume and curriculum transfer are separate contracts. Exact resume
restores the optimizer, RNGs, worker environments, episode limiters, and the
entire training/source/readiness contract. Weight-only warm start validates the
model architecture, encoding schema, vocabulary hash, and state-dict shape,
but deliberately does not restore those exact-run states. The two modes are
mutually exclusive.

Act curriculum completion is detected only when the next public observation
has actually entered a higher Act. Boss death, reward, chest, Boss relic, and
delayed selection continuations do not terminate early. Training rollouts may
apply the versioned potential-based Act 1 shaping reward; evaluation and model
selection continue to use only the unshaped terminal outcome.

Training additionally applies `sls-act1-episode-limit-v1`: a nonterminal run
that reaches 512 decisions or visits the same policy-visible boundary more than
four times becomes a training failure with reward -1 and a terminal GAE mask.
The backend's authoritative transition and reward remain unchanged. Per-worker
step/visit state is part of checkpoint v3, so exact resume cannot cross or lose
an episode-limit boundary.

The training gate is `policy-transfer-v1`: public observations, candidate
identity, terminal meaning, vocabulary, unsupported content, and deterministic
mechanism probes are strict. Random mechanisms use distribution comparisons;
same-seed whole-run traces remain useful diagnostics, not policy blockers.

The source-generated A0-A20 FullRun inventory and staged semantic ledger are
described in `ironclad-fullrun-expansion.md`. Native structural traversal is
not a release claim: Act 2, Act 3, Heart, and A1-A20 remain blocked until their
Original/native transition and route evidence closes.

The ignored Original artifacts are converted locally into a committed
readiness lock after current-code replay of every selected route segment. A
Linux training host verifies the lock against Git blob-based adapter,
canonicalizer, policy, vocabulary, checkpoint, and native-source contracts;
it never needs the game or truth bundles.

All default NUS stages share `configs/validation/policy_transfer_v1.json`.
Legacy readiness locks remain verifiable historical attestations but are not
consulted by default training configurations.

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
