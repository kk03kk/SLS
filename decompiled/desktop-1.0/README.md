# Slay the Spire `desktop-1.0.jar` audit projection

This directory contains a complete CFR decompilation of the pinned stock-game JAR used by SLS parity audits.

## Authority rule

The authoritative artifact is `D:\SLS\local\external\original-game\desktop-1.0.jar` with SHA256:

`cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`

The files under `source/` are a readable projection of that bytecode. They are not hand-maintained source code and must not be edited to redefine game behavior. If a decompiled expression is ambiguous, inspect the JAR bytecode and record the evidence in the audit.

## Reproducibility

Decompiler identity, hashes, arguments, coverage counts, and known third-party failures are recorded in `manifest.json`. CFR's own diagnostics are in `source/summary.txt`.

At generation time, all 2,008 top-level classes in the `com.megacrit.cardcrawl` namespace had a corresponding Java file. None of CFR's reported problem methods belonged to that namespace.

## Layout

- `source/com/megacrit/cardcrawl/`: stock game implementation used for semantic review.
- `source/summary.txt`: CFR diagnostics.
- `manifest.json`: immutable generation and authority metadata.

Do not place simulator fixes in this directory. Simulator changes belong in the normal SLS source tree and should cite the relevant stock class and method in parity evidence.
