# Local live-game runtime

Live play requires the user's own Slay the Spire installation, ModTheSpire,
BaseMod, and CommunicationMod. These JARs, game saves, and Mod configuration
are local assets and must never be committed.

Start the game with CommunicationMod configured to launch
`tools/play_live.py`, or pipe its newline-delimited protocol to that process.
Begin with `--max-actions 5` and inspect `logs/live-agent.jsonl` before allowing
a complete run.

The controller can attach at an existing public decision boundary. It validates
the artifact's ascension range, records intent/ack pairs, and refuses to repeat
an action when delivery is uncertain.
