# Reptomancer: production projection and controlled target mapping

This local batch corrects the classification of the previously open slot/list
difference. It does not complete the full three-act/Heart audit or certify the
entire encounter. No Git commit/push or server checkout change is made.

## Independent findings

Stock `SpawnMonsterAction` retains dead Dagger entries in its public MonsterGroup.
Native's existing production `public_combat_state` already records the same
retained summon ghosts. The lossless `LightspeedBattle.snapshot()` instead exposes
the five internal slots plus separate checkpoint ghost records. Comparing that
serializer directly to the stock public list caused the reported seven-vs-five
difference. It was not evidence that the production simulator lost the old objects.

A new read-only `public_combat_probe_snapshot()` exposes the exact existing
production projection without replacing the checkpoint serializer. In all three
original four-turn stock cases, it has seven entries with matching ordered enemy
identities, HP, max HP and block; reading it leaves the lossless snapshot unchanged.

## Actual target-script evidence

An immutable new scenario uses seeds131200099–101, allocated after checking all
prior records. It constructs actual stock TheBeyond/elite context and executes:

1. Four end-turns, allowing original dagger self-deaths and replacement summons.
2. Anger on the first live Dagger.
3. Anger on the second live Dagger.
4. Fire Potion on the second live Dagger.

The same semantic script is replayed in native. Selection is by normalized content
and live ordinal, not an agent policy. Stock's resolved physical target indices
are recorded and independently checked; native's public target position maps to
its internal slot only when executing the corresponding action.

All three runs match from boundary4 through both targeted cards and the potion
kill: creature state, legal actions, ordered card zones, item state, damage and RNG.
Thus the retained-corpse/target-slot mapping is supported for these fixed histories.

Strict reports still contain differences at earlier boundaries2/3: dead Dagger
Minion1 remains briefly in native while stock has cleared its powers after the
death animation timer. Each residue was checked to belong to a gone, HP0,
non-half-dead Dagger. No other differences occur in these full scripted traces.
No global dead-power normalizer was added and no strict failure was rewritten.
Classification is **target mapping matched with declared, trace-scoped animation
residues**, not an unconditional strict match.

This is validation-mode controlled evidence using the production native view.
It is neither an actual natural-start production trajectory nor a win-rate estimate.

## Code and identities

- New read-only native production-view probe; original lossless snapshot and load
  format retained.
- Audit semantic target resolver rejects missing targets, negative/non-integer
  ordinals, conflicting explicit indices and half-dead/gone targets.
- Capture records resolved stock actions. Replay verifies them, supports explicit
  `--native-view production-public`, and can retain the first failure while
  inspecting later boundaries with `--continue-after-divergence`.
- Reports identify the selected view and hash the audit helper sources.

Current native canonical source:
`1d21b21a02c6c4fa44ff8dba4ae092b1c864b9987c7dd1935b4625caede87bb3`.
Previous source was `5fb389065dd9b6f52c470a962308f57ae0f5e039f07dcebc7fc411927625f5c5`.
The only native addition is the read-only method/binding.285 fixed normal native
snapshots/actions match before/after; no production rule, PPO, reward, network,
distribution or model/checkpoint layout changed. No global source waiver registered.

Oracle1.3.7/r10 SHA:
`5fe5d218015510002ce7009a8274c7ba50ee4ed8ae8e6c1e390aa5a2c5cd0f8a`.
Its new immutable resource is `fullrun-repto-targets-r1.json`.

## Verification and evidence

- Full Python:1257 passed,1 intended skip,4 expected warnings.
- Relevant focused suite:42 passed. Native build/import, Ruff and diff whitespace
  checks passed.
- The owned game batch completed, recovery is RECOVERED, and all63 protected
  files were independently rehashed unchanged after shutdown.
- Raw traces, stock bytecode, old failures and JARs remain local. Compact stock
  fixture: `tests/fixtures/reptomancer-stock-public.json`; independent checks:
  `tests/simulator/test_reptomancer_public_probe.py`.
- Machine-readable summary: `local/audits/fullrun-parity-20261007/local-progress-r6.json`.

Offline reproduction (new output path required):

```powershell
$env:PYTHONPATH='D:/SLS/src;D:/SLS'
& D:/Anaconda/envs/DL/python.exe tools/replay_act2_encounter_batch.py `
  local/audits/fullrun-parity-20261007/repto-target-capture-r1.json `
  --oracle-build local/build/oracle/fullrun-parity-r10.build.json `
  --manifest native/oracle/resources/spirecomm/parity/fullrun-repto-targets-r1.json `
  --native-view production-public --continue-after-divergence `
  --output local/audits/fullrun-parity-20261007/repto-target-production-replay-new.json
```

## Still required

Reptomancer death/minion cancellation, remaining AI branches and longer ghost
accumulation histories remain unverified. The next high-impact batch is Heart
Beat of Death/order/cap/reset and shield/spear turning/death. Other Act3 special
branches, double bosses, keys, late natural trajectories and reachable content
coverage remain in the original full-scope plan.
