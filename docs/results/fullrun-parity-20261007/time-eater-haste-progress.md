# Time Eater selected half-HP Haste branches

Seeds131200144–149, two scenarios, three runs each. Original source first:
`TimeEater.getMove` checks current HP strictly below maxHP/2 and unused Haste;
`takeTurn` queues debuff removal, Shackled removal, healing to half and A19+
block equal to Head Slam damage. Native supplied no expected values.

Original/runtime observations:

- Bash crosses241→233 of480 HP. The next move selection chooses Haste;
  subsequent Haste heals240 and grants32 block, removing Vulnerable.
- Disarm applies Strength−2 at235 HP. Haste removes negative Strength,
  heals240 and grants32 block. Later Anger consumes6 block without HP loss.

All six strict comparisons match every recorded stable boundary, including legal
actions and RNG. Six original-derived regression tests pass. No native rule fix
was needed for these branches. One initial manifest was rejected before game
launch because source keys lacked the complete package prefix; its contents and
failure log remain local. The executed immutable manifest is
`native/oracle/resources/spirecomm/parity/fullrun-time-eater-haste-r2.json`.

Native source97fecc84; executed Oracle r19/1.3.16 SHA
`9b00d34ebd4bef560bce181ed37720bd9f62cce02c87b3235e8ad63b8d5f0be0`.
Capture/replay/recovery evidence is under `local/audits/fullrun-parity-20261007`.
All63 protected entries independently rehashed unchanged after recovery.
This is controlled Haste evidence, not complete Boss flow or natural-win evidence.
