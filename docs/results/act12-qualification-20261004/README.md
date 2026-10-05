# Act 1–2 qualification: Snecko correction, 2026-10-04

Controlled verification found one confirmed simulator discrepancy. Stock A17+
Snecko Tail Whip queues Damage, Weak, then Vulnerable. Native previously queued
Damage, Vulnerable, then Weak. With one Artifact, the original order leaves
Vulnerable; the previous native order left Weak. At A16 there is only Vulnerable,
so one Artifact removes it entirely.

The installed stock JAR was located at the path recorded in earlier audit
documentation. `javap -c -p` was executed again against that JAR. Its identity is
`cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`.
`snecko-after.json` records the class digest and takeTurn bytecode offsets;
full disassembly remains local. No original game was launched in this batch.

- `snecko-before.json`: actual old-native controlled scenario; Weak remained.
- `snecko-after.json`: actual corrected-native scenario; Vulnerable remains.
- `simulator-transition.json`: immutable rule revision
  `sls-snecko-tail-whip-order-v1`, with supporting file hashes and exact source/
  target native identities. It permits fresh adjacent-stage weight transfer only.
- `transfer_micro_probe.py`: a real 70M parent verifies all-weight transfer,
  fresh optimizer/state, reviewed cross-horizon reference and checkpoint replay.
  This is a small local implementation probe, not a 90M/NUS training result or
  evidence of improved win rate. Recorded output is stored separately.

Old native source:
`1e30bb6cfa32f600cd63c59983c14ab8928fac4452c1586d630295d2c6ff9453`.
Corrected native source:
`fe354a23c7584d68d0a2b6681b8dfd4e97d98e107b7ddf57c479060d91468f39`.

Observation/action/model encoding remains unchanged. Environment semantics and
training implementation identities change; old environment checkpoints cannot
be resumed under this correction. Source model weights remain compatible through
the explicit reviewed curriculum migration. Both the frozen parent and trained
candidate must be evaluated under the same corrected Act2 rules. Historical
70M/90M results retain their original identities; server pulls wait for completion.

This proves a specific ordering correction using stock bytecode and native runtime
regression. It does not establish complete Act2 parity, compare original full-game
trajectories, or measure a policy's win-rate gain.
