# SLS Oracle Mod

This ModTheSpire component exposes deterministic reset, state, and RNG evidence
needed for Original-game differential validation. It must not implement game
rules or provide policy input.

Import the external Original-game JARs with `tools/import_original_game.py`, then
run `python tools/build_oracle.py`. The deterministic output is written to
`.build/oracle/SpirecommParity.jar`.
