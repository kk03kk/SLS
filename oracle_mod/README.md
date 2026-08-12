# Spirecomm parity instrumentation

This ModTheSpire mod is part of the original-game oracle. It adds no cards,
relics, balance changes, or player decisions. It does two parity-only jobs:

1. replaces gameplay calls to `CardGroup.getRandomCard(..., useRng=false)`
   with an isolated, seedable instance of the original game's `Random` class;
2. appends all retained RNG states to CommunicationMod JSON as top-level
   `_rng`, and reports the unsigned `math_seed`.

Build from the repository root:

```powershell
python scripts\build_oracle_mod.py
```

The launcher copies the resulting `oracle_mod\build\SpirecommParity.jar` into
the game's local `mods` directory and selects `spirecomm-parity`. Pass an
explicit unsigned 64-bit seed when needed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start_original_sts.ps1 -MathSeed 123456789
```

For an exact mid-run reload, also pass the saved counter as the Java system
property `spirecomm.math_counter`. A trace without `_rng` and `math_seed` is
behavioral evidence only, not an exact RNG-parity oracle trace.

The 2026-08-12 live smoke test loaded this mod with the pinned game and mod
builds, started CommunicationMod's Python child, and emitted all 14 streams.
For `math_seed=123456789`, the original game and native simulator produced the
same initial `counter`, `seed0`, and `seed1`.
