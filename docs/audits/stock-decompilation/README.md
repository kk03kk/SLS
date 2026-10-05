# Stock-game decompilation evidence

`manifest.json` is the generation record for a CFR projection of the pinned `desktop-1.0.jar`. It records the original JAR SHA256, decompiler version, file counts and paths **at generation time**. Do not edit the manifest to make old paths look current. The JAR bytecode is authoritative when a decompiled expression is ambiguous.

The generated `source/` tree is not committed. On the primary workstation it was moved from the former repository-root `decompiled/desktop-1.0/source/` to ignored `local/audits/stock-decompilation-tree/desktop-1.0/source/` during the public-project cleanup. A fresh clone contains this metadata but not the game JAR or the decompiled projection. Generate a local projection only from a game copy you are entitled to use.

The relocation changes storage only; it does not change the historical audit conclusions or the hashes recorded in the manifest. Simulator fixes belong in `native/simulator/`, with corresponding tests and cited stock-game evidence.
