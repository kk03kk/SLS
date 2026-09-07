# Event observation repairs — 2026-09-07

This change closes the concrete gaps found in the 64-seed Act1 diagnostic and
the subsequent stock-bytecode event audit. It is not a claim of complete game
parity. Evidence came from the local original-game JAR, native implementation,
historical trajectories, and mechanism regression tests.

## Repaired behavior

| Mechanism | Change |
| --- | --- |
| Neow | Each displayed bonus and drawback reaches the policy; unrevealed reward outcomes remain hidden. |
| Falling | Each removal option references the exact eligible deck instance, including its public upgrade/card state. Stock's no-eligible-card Leave maps to native semantic option 3. |
| We Meet Again | Potion/card options reference their displayed owned object; the displayed gold payment is explicit. |
| N'loth | Each trade references the selected owned relic. |
| World of Goop | Displayed gold loss, damage and gold reward are explicit. |
| Dead Adventurer | Public enemy clue and current displayed encounter probability are explicit. After three safe searches the event ends; no fourth search is offered. |
| Scrap Ooze | Current displayed damage and relic probability are explicit. The stock integer RNG threshold is preserved. |
| Knowing Skull | Current branch costs are explicit; stock potion/gold/card button order maps to native gold/card/potion semantics without changing wire commands. |
| Note for Yourself | Offered card preview includes its public upgrades and mutable card features. |
| Designer | The two-random-upgrades branch now pays the stock gold cost. |

Both adapters share `sls.content.event_options`. An event action retains its
event-specific option identity and adds a subject reference to an owned object
or an offered-card preview. This uses the model's existing entity/action
reference mechanism. No strategy preferences or community heuristics enter the
reward function.

## Explicit independent-episode context

`CurriculumProfile.note_for_yourself_card` defaults to `IRON_WAVE`; a card spec
such as `BASH+1` is also supported. Each episode starts with this supplied account
context. Trading away a card does not mutate another worker's account or a later
episode. This intentionally models independent seeded runs rather than a shared
persistent player account.

`CurriculumProfile.secret_portal_eligible` defaults to `True`. Setting it to
`False` excludes the portal through the existing speedrun eligibility logic.
This is a controlled environment condition, not a simulated real-world clock:
the simulator does not infer 800 seconds of human play from CPU wall time.
These settings are included in the profile's checkpoint contract and in native
run state, restored through both direct state restoration and deterministic
replay. A backend rejects a checkpoint with a different starting context.
Neither setting changes the Act1 objective.

## Deployment and validation

The policy input is versioned as `sls-policy-input-v5` with the regenerated v5
vocabulary. Previous policy/checkpoint artifacts remain untouched. The intended
next experiment starts from random model parameters.

Rebuild native code before using this checkout. For original-game observation,
build/install `SpirecommParity-observation-v4.jar` using
`tools/build_observation_oracle.py`. Old native/oracle projections fail clearly
when required displayed event details are absent.

Local verification: native and Oracle builds succeeded; the full test suite
passed **587 tests**, with one historical-policy diagnostic skipped because the
new encoding correctly rejects that policy. Existing training smoke tests cover
PPO updates, checkpoint save/load and worker restoration. Two warnings are
expected runtime-rebind test cases. Vocabulary generation and diff whitespace
checks passed.

The original 64 trajectories were replayed through **10,241 decision boundaries**
using their recorded actions (no policy inference). After excluding only the
new public event properties/previews, historical observations had **zero
differences**. Historical event actions were matched by their stable option
identity to allow the newly added subject reference. The three We Meet Again
encounters now expose the card instance or gold amount before selection.

The Oracle JAR was compiled against the local original game but has **not** been
installed and exercised in a new live original-game parity session. That remains
a deployment verification, not a locally proven result. No long training or
fresh model evaluation was performed, and historical checkpoints/logs were not
modified.

Local diagnostic evidence is under `local/audits/event-audit/`, including
`tests-repair.txt`, `build-repair.txt`, `oracle-repair.txt` and the new historical
action replay results. These ignored artifacts are separate from the original
64-seed traces.
