# Local live-game runtime

Live play requires the user's own Slay the Spire installation, ModTheSpire,
BaseMod, and CommunicationMod. These JARs, game saves, and Mod configuration
are local assets and must never be committed.

Start the game with CommunicationMod configured to launch
`tools/play_live.py`, or pipe its newline-delimited protocol to that process.
Begin a fresh Ironclad A0 game, attach while the Neow choice is visible, run
with `--max-actions 5`, and inspect `local/logs/live-agent.jsonl` before allowing a
complete run.

The controller validates the artifact's ascension range, FullRun goal and exact
model-weight digest. Each intent durably stores the GRU state produced from the
current public observation. Each acknowledgement binds it to the next observed
boundary and records the previous action/reward inputs needed by the belief
state. A later process may resume only when that acknowledged boundary matches.
A fresh journal at a mid-run state, an artifact mismatch, malformed memory, or
any unacknowledged action fails closed even if the game boundary has changed.

`--wait-for-neow` may be used when CommunicationMod starts the controller at the
main menu. It polls only the advertised public `state` command and times out at
`--wait-timeout`; it never starts or resets a run itself.

Ordinary `FULLRUN` artifacts stop successfully after Act 3. `HEART` artifacts
continue to Act 4. This release intentionally does not reconstruct recurrent
history for an arbitrary mid-run save.

## Browser inspector

`tools/play_live_inspector.py` is a separate, debug-only entry point. It first
opens a model-selection setup page and serves its controls only on
`127.0.0.1:8765`. Opening the game does not load a model or send a game action.
The dashboard shows
every legal action's logit and legal-action softmax probability, plus the value
head (which is a state-value estimate, not an action Q-value). It supports a
0–10 second live delay, model single-step, safe boundary pause, and explicit
manual action selection. Card rewards have a separate 0–10 second preview
delay (3 seconds by default): the model may score the already-public choices,
but the backend opens the three-card screen and leaves it visible before it
sends the selection. One model step is one semantic decision, so opening and
choosing that reward is still intentionally one inspector step.

The dashboard treats `model/` as the canonical library of testable exported
policies. Training checkpoints under `local/runs`, including `latest.pt`, are
resume artifacts and are not listed directly. Their weights must first be
exported as a standalone policy artifact.

List exported models without opening or connecting to the game with:

```powershell
D:\Anaconda\python.exe tools\play_live_inspector.py --list-models
```

Configure CommunicationMod for the model-selection dashboard with:

```powershell
D:\Anaconda\python.exe tools\configure_live_inspector.py
```

The helper verifies all referenced files, preserves unrelated properties,
creates a timestamped sibling backup, and prints the exact new command. Restore
a printed backup with:

```powershell
D:\Anaconda\python.exe tools\configure_live_inspector.py --restore <backup-path>
```

Then launch ModTheSpire/CommunicationMod. The browser opens automatically on a
dedicated setup page. Select an exported policy and click **Load and connect to
game** before creating a matching fresh run. When the dashboard reaches
`CONNECTING`, create the requested Ironclad/Ascension run. It sends no game
action after attachment until **Run**, **Single step**, or **Execute selected
action** is pressed. A pause requested while a game command is settling takes
effect at the next stable decision boundary.

The inspector alone permits A0 `ACT1`, `ACT2`, and `ACT3` artifacts. The normal
`play_live.py` path remains restricted to `FULLRUN` and `HEART`. A curriculum
session stops at its target boss-clear boundary before exposing a boss-relic or
next-act decision to a policy that was not trained for it.

## Reproducible seed audit

Live journals use `sls-live-action-v4`. Every new intent includes a process
`session_id`, the complete public Observation, all ranked candidate actions,
the model recommendation, the action actually selected, and the public run seed
when CommunicationMod supplies it. The ACK carries the same session ID. Older
v3 logs remain readable, but cannot reconstruct observations that they never
recorded; start a new inspector session to obtain the v4 evidence.

Replay a signed Java `long` seed with the exact recurrent runtime and generate a
baseline plus a clearly-labelled diagnostic block-deficit counterfactual:

```powershell
D:\Anaconda\python.exe tools\audit_policy_seed.py `
  model\ironclad-a0-act1-5m.pt `
  --seed -1466613676819842358 `
  --output local\reports\live-audit\seed-audit.json
```

Capture a boundary-by-boundary canary trajectory with the same previous-action
and previous-reward recurrent inputs used by live play:

```powershell
D:\Anaconda\python.exe tools\capture_policy_trajectory.py simulator `
  model\ironclad-a0-act1-5m.pt `
  --seed -1466613676819842358 `
  --output local\reports\live-audit\seed-trajectory-v2.jsonl
```

Trajectory v2 hashes and records recurrent context. It therefore replaces v1
captures for exact live/simulator comparisons.
