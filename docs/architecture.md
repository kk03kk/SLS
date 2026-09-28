# Architecture

SLS separates the policy from game execution at one boundary:

```text
public Observation + legal semantic Actions
    → recurrent policy (action probabilities + state value)
    → Backend.step(Action)
    → Transition (next decision, reward, terminal state)
```

`src/sls/contracts/` defines the shared data types. `src/sls/backends/simulator/` adapts the native engine; `src/sls/backends/original/` adapts CommunicationMod for live play and comparison. The policy reads public observations and candidate actions. Backend RNG, hidden draw order, and simulator-only state are not policy inputs.

The model in `src/sls/model/` encodes structured observations with relational attention, maintains belief memory with a GRU, and scores the variable-size legal action set. Its current input schema is `sls-policy-input-v5` (`src/sls/model/encoding.py`). `src/sls/rl/` collects sharded rollouts, optimizes recurrent PPO, evaluates on fixed seeds, and validates checkpoint contracts. The native simulator is the training backend; original-game comparison is a separate audit path under `src/sls/audit/` and `src/sls/diagnostics/`.

## Training and reproducibility

The completed experiment used the single-stage `IRONCLAD_A20_ACT1` profile defined in `src/sls/curriculum.py`; the selected 56M champion is the current demonstration model. Act2, Act3, and Heart profiles exist for future curriculum stages, but defining a profile does not establish a trained result. The [completed configuration](../configs/train/ironclad_a20_act1_60m_stable.toml) transferred the 46M champion's weights into a new training run; optimizer, worker state, loop state, and random streams were rebuilt. This differs from an exact checkpoint resume, which validates and restores the complete contract. See the [result and environment identity](results/a20-act1-60m-stable/README.md).

Training, periodic selection, and final evaluation use separate seed ranges. `src/sls/rl/preparation.py` bounds training seeds below the configured periodic, final, and diagnostic namespaces. Selection uses a fixed seed set; the final result uses held-out seeds. The job retains the 46M source as a candidate, so a later checkpoint only becomes selected if it wins under the configured selection rule.

The checkpoint contract records model and encoding schemas, configuration, native source identity, worker/runtime details, and resume state. Native source provenance uses local source contents; changing rules requires qualification and can invalidate exact resume. A policy artifact is a smaller export for inference and is distinct from a training checkpoint. Live play uses the same model and policy input path, with fail-closed recovery rules for the controller journal.

## Validation boundary

Automated tests and targeted stock-game parity audits cover known behavior and regressions. They do not certify every game seed or event branch. The versioned native simulator is based on the MIT-licensed `sts_lightspeed` project; its [source record](../native/simulator/SLS_VENDOR.json) and [upstream license](../native/simulator/LICENSE.lightspeed.md) remain in the tree. See [historical audits](history/README.md) for dated evidence and unresolved questions.
