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
    -> relational Transformer state encoder
    -> GRU belief memory (previous action + reward + current observation)
    -> variable-candidate policy + value heads
    -> recurrent PPO
    -> exact checkpoint / simulator-only artifact
    -> CommunicationMod live controller
```

The native simulator is the training authority. Original-game comparison and
Oracle instrumentation are maintained as a separate validation path under
`sls.audit`, `sls.diagnostics`, and `tools`; they never provide training input.
Teacher policies and behavior cloning are outside this project.

Native source provenance is a canonical digest of local source paths and file
contents. Git metadata is recorded when available, but Git is not required to
build, test, or run the local project.

Training workers own native environments while model inference is centralized.
Rollouts remain time-major and are optimized as contiguous recurrent sequences.
Episode-start masks reset only the corresponding GRU row; GAE, memory and
gradients never cross a terminal boundary. Entropy decay is driven by total
environment decisions so changing the benchmark-selected worker count does not
change the schedule.

Smoke, pilot and train are cumulative Act 1, Act 2 and FullRun horizons in one
learning chain rather than three initializations. Horizon migrations preserve
learning/RNG state and reset environments, belief memory, previous experience
and episode limits together. Ordinary checkpoints restore all fields exactly.
Periodic and final evaluation use separate high seed namespaces that training
cannot enter.

PPO normalizes advantages independently for combat, run and choice decisions,
and normalizes entropy by legal-candidate count. Evaluation aborts on backend
errors. A checkpoint becomes an artifact only after a configured milestone gate;
the final live artifact also requires a passing independent 1,000-seed run.

Live play uses the same observation, previous-experience input, action encoder,
recurrent model and candidate scorer. Restart is allowed only at an acknowledged
matching boundary. Policy artifacts bind model weights, vocabulary, source and
configuration digests and remain marked simulator-trained rather than
Original-validated.
