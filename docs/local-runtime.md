# Local live-game runtime

Live play requires the user's own Slay the Spire installation, ModTheSpire,
BaseMod, and CommunicationMod. These JARs, game saves, and Mod configuration
are local assets and must never be committed.

Start the game with CommunicationMod configured to launch
`tools/play_live.py`, or pipe its newline-delimited protocol to that process.
Begin a fresh Ironclad A0 game, attach while the Neow choice is visible, run
with `--max-actions 5`, and inspect `logs/live-agent.jsonl` before allowing a
complete run.

The controller validates the artifact's ascension range and FullRun goal. Each
intent stores the GRU state produced from the current public observation, and
each acknowledgement binds it to the next observed boundary. A later process
may resume only when that boundary matches. A fresh journal at a mid-run state,
an artifact mismatch, malformed memory, or uncertain resend fails closed.

Ordinary `FULLRUN` artifacts stop successfully after Act 3. `HEART` artifacts
continue to Act 4. This release intentionally does not reconstruct recurrent
history for an arbitrary mid-run save.
