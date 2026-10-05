# Bounded Act2 rule scenarios, 2026-10-04

This batch narrows the qualification gap before normal-start A20 Act1–2
training. Eleven new cases pass against expectations independently derived from
the installed stock JAR, then checked again with `javap -c -p`. No simulator,
observation, action, reward, model or training semantics changed in this batch.

| Rule | Stock evidence | Native runtime coverage |
| --- | --- | --- |
| Champ Face Slap | `takeTurn`: Frail new offset 701 precedes Vulnerable 731 | One Artifact leaves Vulnerable 2 |
| Champ Taunt | `takeTurn`: Weak 831 precedes Vulnerable 861 | One Artifact leaves Vulnerable 2 |
| Collector mega debuff | `takeTurn`: Weak 387, Vulnerable 423, Frail 459 | One Artifact leaves Vulnerable/Frail 5 at A20 |
| Automaton | `getMove`: beam resets turn count at 54–56; A19 boundary at 68–93 | Full opening and two beam cycles at A18/A19/A20; A19 skips stun but retains Boost |
| Champ phase | `getMove`: integer half-health comparison at 10–20 | HP 220 does not trigger; HP 219 triggers Anger; Strength 4 becomes 16 then Execute |
| Spheric Guardian | Prebattle Barricade/Artifact/block; `takeTurn` A17 boundary at 131–173 | A16/A17/A20 retain initial block, add 25/35, then Frail Attack/Slam/Harden, with checked HP and block |

`native-traces.json` contains real controlled native snapshots, RNG counters,
stock class hashes and disassembly hashes. The full proprietary disassembly is
kept locally, not committed. High player HP is intentionally used to observe
multiple cycles; Champ HP/moves are explicitly forced for boundary probes.
These are rule scenarios, not normal-start win-rate evaluations.

Reproduce locally in Conda DL (use fresh output paths; the recorder rejects
overwrites):

```powershell
python -m pytest tests/simulator/test_a20_late_act_parity.py -q
python tools/audit_act2_rule_scenarios.py --stock-jar D:/Steam/steamapps/common/SlayTheSpire/desktop-1.0.jar --javap D:/java/bin/javap.exe --disassembly-dir local/reports/act2-rule-replay/stock --output local/reports/act2-rule-replay/native-traces.json
```

This is stock-bytecode review plus native execution, not a newly executed
stock-game trajectory comparison. It does not prove complete combat/action/RNG
equivalence, card interactions, Bronze Orb Stasis, all Champ debuff cleansing,
Collector respawns, all elite mechanisms, event semantics or cross-act reward
parity. Those remain distinct qualification work. No policy improvement is
claimed. The 90M parent is still unbound pending actual completed artifacts.
