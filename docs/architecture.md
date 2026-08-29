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
    -> GRU episode memory
    -> variable-candidate policy + value heads
    -> recurrent PPO
    -> exact checkpoint / simulator-only artifact
    -> CommunicationMod live controller
```

The native simulator is the training authority. Original-game comparison,
Oracle instrumentation, teacher policies, and behavior cloning are outside the
main repository. The pre-cleanup history remains recoverable from the Git tag
`pre-training-cleanup-20260829`.

Training workers own native environments while model inference is centralized.
Rollouts remain time-major and are optimized as contiguous recurrent sequences.
Episode-start masks reset only the corresponding GRU row; GAE, memory and
gradients never cross a terminal boundary. Entropy decay is driven by total
environment decisions so changing the benchmark-selected worker count does not
change the schedule.

Smoke, pilot and train are cumulative boundaries in one run rather than three
initializations. Checkpoints restore GRU memory, worker environments, optimizer,
episode limits, seed allocator, environment-step count and RNG state exactly.
Periodic and final evaluation use separate high seed namespaces that training
cannot enter.

Live play uses the same observation, action encoder, recurrent model, and
candidate scorer. The intent journal stores the post-observation GRU memory;
restart is allowed only at the matching acknowledged boundary. Policy artifacts
record model/vocabulary/source/config digests and are always marked as
simulator-trained rather than Original-validated.
