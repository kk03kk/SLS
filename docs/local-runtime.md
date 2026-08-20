# Local Original-game requirements

These files are intentionally not part of the repository. They must come from
the user's local Slay the Spire and Steam Workshop installation:

| Imported filename | Source |
|---|---|
| `desktop-1.0.jar` | Slay the Spire installation |
| `ModTheSpire.jar` | Workshop item `1605060445` |
| `BaseMod.jar` | Workshop item `1605833019` |
| `CommunicationMod.jar` | Workshop item `2131373661` |

Java 8 is also required because the target game runtime is Java 8. These inputs
are copied by `tools/import_original_game.py` into the ignored directory
`external/original-game/`, where a SHA-256 manifest is generated.

`SpirecommParity.jar` is not a local prerequisite. It is built from committed
source by `tools/build_oracle.py` and written to `.build/oracle/`.

CommunicationMod must be configured to launch either:

- `tools/validate_full_run.py` for one seed;
- `tools/validate_corpus.py` for a seed corpus.

The native `_lightspeed` extension is also generated locally by
`tools/build_native.py`; it is not committed.
