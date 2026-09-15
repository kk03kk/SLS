# A20 Act1 30M audit and proposed 40M continuation

Audit date: 2026-09-15. Source before changes: `c71a095155173b27d6fd4e3f8549f898219c8c11`.

## Evidence and results

Read the latest `runs/archives/sls-ironclad-a20-act1-30m-champion.tar.gz` into `local/audits/a20-30m/bundle/`. The archive includes champion, final, exported policy, 795 metrics records, all fixed and held-out seed results, manifest/config/bundle, and job 844392 stdout/stderr. It does not include every periodic checkpoint. Original archives and source checkpoints were not modified. File hashes, complete metrics aggregation, and log inventory are in `local/audits/a20-30m/training-analysis.json`.

| Cumulative steps | Fixed wins /512 |
|---|---:|
| 17,006,592 migration baseline | 194 |
| 18,006,016 | 220 |
| 19,005,440 | 228 |
| 20,004,864 | 236 |
| 21,004,288 | 250 |
| 22,003,712 | 237 |
| 23,003,136 | 262 |
| 24,002,560 | 286 |
| 25,001,984 | 286 |
| 26,001,408 | 272 |
| **27,000,832 champion** | **294 (57.42%)** |
| 28,000,256 | 275 |
| 29,016,064 | 294 |
| 30,015,488 final | 274 (53.52%) |

Champion held-out: **554/1024 = 54.10%, Wilson 95% CI 51.04–57.13%**. Hexaghost 166/331 = 50.15%; Slime 208/351 = 59.26%; Guardian 180/342 = 52.63%. Conditional on reaching the boss, wins are 62.17%, 72.73%, 63.83%. Of 470 failures, 281 occur on floor 16, and 189 before the boss. Backend errors, truncations, step/cycle limits, self-loops and timeouts are zero.

This is substantial learning: +19.53 percentage points on the same fixed seeds versus A20 migration baseline. Fixed selection optimism is plausible (57.42% selected vs 54.10% unseen); these results do not establish severe overfitting. Baseline has no paired evaluation on this final held-out set, so its held-out improvement cannot be asserted. After 24M progress slows and reversals remain large. At 29M→30M, 62 losses become wins while 82 wins become losses; these are actual paired-seed reversals, not just changing seed samples. See `paired-evaluations.json`.

794 PPO updates: first/last 100 average KL 0.00312/0.00392, final-epoch KL 0.00556/0.00713, clip fraction 5.87%/6.09%, entropy 0.313/0.223, value explained variance 0.539/0.592. Last-window final-epoch KL max 0.01146, below target 0.02; no KL early stop. Gradient norm averages 0.610/0.654 before clipping. No evidence of numerical optimizer collapse. Value prediction remains imperfect; a single loss statistic does not establish broken credit assignment.

The boss bottleneck includes combat and earlier HP/deck/resource preparation. Floor-16 deaths alone cannot distinguish them, and do not justify late-act reset curriculum. One diagnostic game cannot assign population-level causal responsibility to individual decisions.

Throughput averages roughly 82→85 environment decisions/sec (sampling plus update); final held-out evaluation took 906.6 seconds. 64 workers/8 shards, MIG A100 40GB slice, peak allocation approximately 27 GiB. These aggregate records cannot establish GPU occupancy or CPU saturation. Keep layout; profile sampling/update separately before proposing performance rewrites. The initial stale-native traceback is the preparation probe followed by rebuild and successful training, not a failed training update. NumPy and initial cuBLAS-context warnings did not abort this run.

## Champion and proposed next experiment

Source: `local/runs/ironclad-a20-act1-v1-30m/stages/train/selection/best_progress.pt`.

SHA256: `65564a7f0add77644677a0fae3ba289ff1416909a3990840e3bd8ba95fbbdc90`.

Loaded trainer step is 27,000,832, update 610; exported policy weights are tensor-identical to champion, not final. The archive's export is still marked simulator-only; this one-seed audit is not unrestricted deployment certification.

Proposed config: `configs/train/ironclad_a20_act1_40m.toml`.

- Continue from champion to **cumulative 40M** (about 13M additional), separate output directory. Preserve policy, Adam moments/counters, recurrent context, worker state and RNG.
- **Only optimizer parameter change: LR 0.0000625 → 0.00003125.** This is a conservative lower-update-size experiment motivated by late paired reversals, not a demonstrated cure or proof that the old LR was too high. It also prevents mechanically repeating the same optimization path through 27M→30M while preserving random state.
- Keep model, reward/potential shaping, gamma 1, lambda .98, rollout256, recurrent64, minibatch16, two epochs, clip .2, target KL .02, gradient clip .5, value coefficient .5, and value clip .2.
- Keep entropy schedule .02→.002 over cumulative40M. It resumes around .00785 at champion. Entropy is declining but not collapsed; changing exploration simultaneously would confound the LR experiment. Do not restart decay or force aggressive early exploration removal.
- No evidence warrants changing potential shaping or doubling recurrent unroll. Terminal success is +1, floor16 failure is -0.2, earlier failure is worse. Thus the objective includes failure progress and is not mathematically identical to pure win-rate maximization; evaluate by clear counts, and reserve reward ablation for actual evidence of a progress/win tradeoff. Gamma1 and terminal potential handling remain unchanged. GAE lambda .98 weights residuals over roughly 50 decisions; this motivates a future isolated credit-assignment experiment if scaling stalls, not an immediate claim that rewards stop propagating after 50 actions.
- Fixed512 evaluation every1M; checkpoint every.25M; preserve best on strict clear-count improvement, ties retain earlier. Final unseen1024 seeds `[2000000004096,2000000005120)`. Keep all prior held-out ranges excluded from training.
- Judge improvement by fixed paired transitions plus final unseen confidence interval/Boss results. If the next several evaluations remain within the same band, do not blindly buy more steps; investigate decision-level failure causes and consider an isolated sequence-length/critic experiment.

A real-checkpoint initialization was executed in `local/audits/a20-30m/integration-child`, with local-only paths/layout fixture. Tensor comparisons verified preserved learning state and unchanged source hash. This checks initialization, not a server CUDA preflight or a new benchmark. The small A0/A20 integration regressions additionally exercise update/save/restore continuation through the direct-script entry.

## Fixes

1. Live runtime and trajectory capture now bind the artifact's explicit environment profile. Previously `ACT1` silently selected A0 or the live default rejected Act1. Explicit A20 binding does not relax mismatched-profile checks.
2. Simulator checkpoint import rejects cross-ascension state before mutation. All 64 actual champion states round-trip unchanged. Normal training's outer contract already protected this path; there is no evidence this run trained on A0 accidentally.
3. Best checkpoint promotion now stages weights, publishes a pending transaction, and recovers a matching weight/metadata pair after interruption. A read-only continuation refuses an unfinished parent promotion. It does not repair/delete the parent automatically. Fault-injection regression covers interruption after replacing weights. This assumes one writer per run directory.
4. Artifact validation rejects an ascension range excluding its own environment profile.
5. A20 continuation removes an A0-only hardcoded contract check while preserving exact profile, model, reward, PPO and source checks. Only explicit reviewed source transitions are allowed. Child best metadata now carries the child checkpoint hash.
6. Real-game experiment found campfire Recall IDs shifted when Smith is unavailable (Fusion Hammer). Original adapter now uses stable semantic IDs while translating to the actual compact `choose` index. Regression checks Recall ID2 executes `choose 1`.
7. Trajectory tools record chosen probability and optional diagnostic state/active RNG, outside all policy inputs. Launcher accepts an explicit oracle JAR.

No network/observation tensor schema or simulator gameplay rule changed. The source transition `c12c370...`→`653181e...` only adds the cross-ascension loader guard; it is recorded as state-preserving and still requires fresh preparation. LR change is an explicit continuation branch, **not exact resume of the old experiment**. No policy-only warm start is needed for these fixes.

## Same-seed original game experiment

Both independent runs: Ironclad/A20/Act1, seed0 selected in advance, same champion CPU deterministic argmax, zero initial recurrent memory, same project Neow setup and oracle. Original uses stock game with BaseMod/CommunicationMod/SpirecommParity, no gameplay fast mod. Initial conditions matched at Neow. Both original launches restored backed-up user/game configuration successfully (RECOVERED, no recovery failures).

Before fix: 155 actions, 156 boundaries on both sides; same actions throughout, Slime Boss death on floor16. First public/action-ID divergence **Decision26, floor6 rest**, simulator Recall `rest-option:2` vs Original `rest-option:1`; repeated at Decision107. Both selected Rest with the same probability. This was category D (semantic identifier mismatch), not RNG or damage mechanics.

After fix, both games were rerun to completion independently. **All 156 public observations, semantic legal-action sets, chosen actions, chosen probabilities, and memory outputs match.** All active RNG streams also match at every boundary (verified by restoring each saved simulator state and reading active combat RNG). No observed RNG-result or RNG-call divergence, no new simulator mechanics bug in this trajectory.

Important details rather than an overly broad “exact parity” claim:

- Raw model-input hashes differ at Decisions16,96,98 because candidate actions are permuted. Reconstructed batches show differences only in action-type/reference tensors; observation tensors are equal. Semantic action scores/argmax, chosen probabilities, and memory outputs are equal. `input-diff.log` records tensor positions and action order. Thus the first raw tensor difference is Decision16, while the first actionable public-ID defect before repair was Decision26. The comparator compares semantic candidate sets and does not claim all raw tensor bytes match.
- Outer GameContext RNG in serialized simulator state is stale during battle by design; active battle RNG is separate. Comparing it directly with Original active RNG gives a false first divergence at Decision2. `active-rng-comparison.json` verifies correct active streams and `timeline.json` uses that comparison.
- One seed covers only its encountered content and one Boss; it does not certify Hexaghost, Guardian, all events, rare relic trigger interactions, or all A20 states. Previously noted rare relic-order risks remain unproven hypotheses. The older coverage checker is not a complete A20 branch-coverage certificate.

Evidence under `local/audits/a20-30m/`: `simulator.jsonl`, `original.jsonl` (before fix), `simulator-verified.jsonl`, `original-verified.jsonl` (full rerun with raw state), action journals, `strict-diff.json`, `verified-diff.json`, `timeline.json`, `timeline.md`, `active-rng-comparison.json`, source/checkpoint comparisons and test logs. Original raw protocol/RNG logs and restored backup journals are referenced by `original*-launch.log`. Hidden diagnostic fields are never sent to the model.

## Validation and server use

Native rebuilt successfully. Full suite: 660 passed, 1 skipped (historical incompatible policy), 4 expected runtime-rebind warnings. Ruff: all checks passed. A0/A20 direct-script continuation regression, interrupted-best recovery, profile/range checks, actual champion 64-state round-trip, and full original rerun completed. Slurm **dry-run only** was executed locally; its Windows paths are expected to be replaced by Linux paths when generated on NUS.

Before long training, the normal compute-node preparation must pass dependency/native rebuild, current preflight, layout compatibility and real update/save/restore checks. Reuse the existing64/8 benchmark because only scalar LR changed; do not force new layout. An unknown source/config difference must still fail. No NUS job was submitted.

When the user chooses to launch:

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a20_act1_40m.toml --prepare
```

The original30M run and its preparation benchmark must be present at their configured server paths. The new run writes to `local/runs/ironclad-a20-act1-v1-40m`.
