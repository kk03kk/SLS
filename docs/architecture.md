# Architecture

SLS has one policy boundary:

```text
Observation + legal semantic Actions
    -> Policy Decision
    -> Backend.step(Action)
    -> Transition
```

`Observation` contains only information available to a real-game agent.
Backend action bits, RNG state, hidden draw order, and simulator internals do
not enter policy input. Candidate actions have stable semantic identities and
are scored as a variable-size set.

```text
C++ FullRun simulator
    -> SimulatorBackend
    -> canonical contracts
    -> vector workers
    -> relational policy + value head
    -> PPO
    -> exact checkpoint / simulator-only artifact
    -> CommunicationMod live controller
```

The native simulator is the training authority. Original-game comparison,
Oracle instrumentation, teacher policies, and behavior cloning are outside the
main repository. The pre-cleanup history remains recoverable from the Git tag
`pre-training-cleanup-20260829`.

Training workers own native environments while model inference is centralized.
Rollouts use terminal-safe GAE, clipped PPO, entropy regularization, value
clipping, gradient clipping, and deterministic held-out evaluation. Synthetic
step/cycle limits become training failures and are part of exact-resume state.

Live play uses the same observation, action encoder, model, and candidate
scorer. Policy artifacts record model/vocabulary/source/config digests and are
always marked as simulator-trained rather than Original-validated.
