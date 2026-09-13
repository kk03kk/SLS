# Ironclad A20 Act1: local environment and 17M -> 30M proposal

Status: release candidate validated locally; publish after final checks. No
real-game launch is part of this release. This is an environment transfer, not
an exact resume of the A0 experiment. Server execution requires compute-node
preparation; local checks do not certify NUS execution.

## Objective and environment boundary

Match stock A20 Act1 mechanics, legal choices, visible information and random
outcome distributions. Exact seed-to-seed RNG alignment is not a release gate;
this does not permit biased random distributions or incorrect random mechanics.
Defeating the Act1 boss ends the episode successfully before rewards/Act2.

Prismatic Shard remains in the shop pool, visible with its normal price, but
its acquisition action is removed. Other offers keep their identity. The model
does not learn the prohibition. Already-owned imported states are rejected.
Normal automatic Act1 relic pools do not contain this shop-only relic.
Keys, burning elites and their real costs remain available without key bonuses.
Note for Yourself is absent at A15+, so the A0 auto-leave exception is inactive.
Runs use unlocked content and the full seeded Neow opening, independently of
previous training episodes; no cross-run player progression is simulated.

## Ascension source review

Evidence: native implementation, offline stock class decompilation and local
regressions. No new real-game validation will be launched. This is not a claim
of exhaustive parity certification.

| Threshold | Relevant effect / implementation finding |
| --- | --- |
| A1 | More elite map rooms; existing 1.6 multiplier retained. |
| A2-A4 | Normal/elite/boss attack increases already implemented. |
| A5 | Reduced between-act healing; outside this episode's post-boss boundary. |
| A6 | Start at 90% HP, rounded; retained. |
| A7-A9 | Normal/elite/boss HP increases; retained. A20 bosses: Slime 150, Hexaghost 264, Guardian 250. |
| A10 | Ascender's Bane: starting curse, ethereal, cannot be removed/transformed; existing mechanics retained, scope metadata corrected. |
| A11 | Two base potion slots; retained, normal relic modifiers still apply. |
| A12 | Reduced upgraded-card chance applies to later acts; Act1 reward upgrade base chance remains zero. |
| A13 | Reduced boss gold is after this experiment's terminal boundary. |
| A14 | Ironclad maximum HP 75, starting HP 68; retained. |
| A15 | Worse event costs/rewards and event eligibility already branch on ascension. Tests cover shrine/serpent/Blue Woman/Scrap Ooze and Bane selection exclusions. |
| A16 | Fixed shop inventory multiplier from erroneous 0.80 to stock 1.10. Purge price is not increased; stock Courier restock pricing is retained. |
| A17 | Stronger normal-enemy powers/AI; existing Cultist, Jaw Worm, slimes, louses, gremlins and slaver branches reviewed against stock. |
| A18 | Stronger elite abilities, including Nob Enrage 3, Lagavulin -2 siphon and Sentry 3 Dazed; existing implementations retained. |
| A19 | Guardian Mode Shift 40/Sharp Hide 4, Hexaghost stronger Inflame/Sear, Slime Boss 5 Slimed; existing implementations retained. |
| A20 | Extra boss occurs in Act3 only. Act1 still has one boss. |

Additional fixes: stock purge pricing for Smiling Mask and combined shop
discount relics; expose the public purge price in both adapters using the
existing SHOP token and price numeric field. The oracle now attaches full
visible card previews to combat card rewards. The latter was compiled offline;
live validation was stopped at the user's request and its backup journal reports
RECOVERED with no recovery failures.

Observation v5 tensor dimensions and vocabulary remain unchanged. Adding public
purge-price information changes input semantics: old source contracts are not
silently relabeled exact-compatible. The A20 profile has its own scope identity.

## What the completed A0 experiment demonstrates

Read-only archive: `runs/archives/sls-act1-v4-20m-champion.tar.gz`.

- Best: 17,006,592 steps, 473/512 fixed (92.38%); final 20,004,864 steps,
  461/512 (90.04%). Keep the champion, not final.
- Independent held-out: 918/1024 (89.65%), versus 850/1024 (83.01%) in the
  preceding experiment. These are different held-out cohorts, not paired seeds.
- Held-out bosses: Hexaghost 89.46%, Slime 92.99%, Guardian 86.76%.
  82 of 106 deaths were on floor 16. This locates failure, not its cause:
  earlier drafting, routes and resource decisions may create delayed failure.
- After 15M, mean KL ~0.00285, clip fraction ~0.0596, entropy ~0.397,
  value loss ~0.0131, explained variance ~0.505; no KL early stops.
- No reported backend errors/truncations/limits/timeouts in final evaluation.
  These health counts do not prove absence of all simulator bugs. The newly
  found purge/observation issues can affect A0; the A16 markup bug cannot.
- Training is still productive but oscillatory. The fixed-versus-held-out gap
  is compatible with selection optimism and sampling variation. There is no
  evidence here that another LR halving is necessary. The experiment changed
  both training budget and LR, so it does not isolate an LR causal effect.
- Late training throughput ~87 environment steps/s. It is not a GPU occupancy
  measurement; remeasure complete sampling + PPO throughput under A20 before
  choosing workers. Do not infer high/low hardware utilization from win rate.

## Local training proposal

Config: `configs/train/ironclad_a20_act1_30m.toml`.

Parent: `local/runs/ironclad-a0-act1-v4-20m/stages/train/selection/best_progress.pt`.
SHA256: `723e271a751e7a1e986311a746795a566080d5e71fc1b40baa29c93ed9cd5e75`.

Transfer all network weights, including value head. Reset Adam, RNG, workers,
recurrent state, update counter and best selection. A new training seed range
starts at 40,000,000. Preserve parent provenance and cumulative step accounting:
17,006,592 -> 30,000,000 means 12,993,408 new A20 steps, rounded up to a full
rollout. This counter does not claim those first 17M were A20 experience.
Once initialized, the child uses ordinary strict checkpoint resume.

Preserving the critic is a minimal-change starting point, not an assertion that
its A0 value estimates are calibrated on A20. Review value explained variance,
KL, clip and performance during initial A20 adaptation before deciding on changes.

| Setting | Proposal |
| --- | --- |
| Network | Existing v5, embedding 128, 4 layers/4 heads, FFN 256, recurrent 256 |
| LR | Keep 0.0000625; no unsupported additional halving |
| PPO | Rollout 256, sequence 64, minibatch 16 sequences, 2 epochs, clip 0.2 |
| Credit assignment | Gamma 1.0, GAE 0.98; existing reward/potential shaping unchanged |
| Stability | Target KL 0.02, gradient clip 0.5, value coefficient 0.5/clip 0.2 |
| Entropy | Keep 40M cumulative clock: coefficient ~0.012347 at transfer, 0.0065 at 30M |
| Evaluation | Initial A20 baseline, then ~1M intervals, same 512 fixed seeds |
| Checkpoints | Every 0.25M, latest/best/final; ties retain earlier best |
| Final | Best on fresh 1024 held-out seeds [2000000003072, 2000000004096) |

No artificial elite/draft/Boss strategy rewards. No network redesign, critic
reset or reward retuning without evidence from A20. The A0 win rate is not an
A20 baseline. Next useful measurement is the transferred policy's A20 baseline,
followed by whether 1-3M of adaptation produces durable improvements.

## Validation and remaining release work

Native rebuilt locally. Targeted tests include A20 start/boss terminal boundaries,
events, shop markup/removal costs, scope/weight compatibility, and actual
training-entry initialization, interruption, resume, finalization and best saving.
Historical compatibility tests now exercise their historical approved source
pair explicitly; the new environment is not added to that resume exception.

The real champion passed local CUDA preflight: A20 observations, policy forward,
short PPO updates, save/load and identical next update after restore. This used
one worker and shortened rollout for a bounded check, not a server benchmark.
Reports are under `local/audits/a20-act1/`. Slurm command generation was checked
with `--dry-run`; no job was submitted.

Local checks before the final source review: **644 passed, 1 skipped**, Ruff and `git diff --check` pass.
The skip rejects a historical policy with incompatible encoding; three warnings
are expected runtime-rebind cases exercised by the tests.

Before an eventual server launch: finish local review, publish the agreed commit,
ensure the pinned source checkpoint exists, then run compute-node native build,
preflight, full-update worker benchmark and worker restore verification through
`--prepare`. Source/config changes invalidate stale preparation. Original A0
artifacts are read-only. A20 performance and NUS throughput remain unmeasured.

## Final bounded source review (2026-09-13)

Re-read stock ShopScreen initialization/discount/purge code; Cleric, GoopPuddle,
GoldenIdolEvent, ShiningLight, ScrapOoze, DeadAdventurer, shrine potion/card events;
and Nob, Sentry, Lagavulin, Guardian and Hexaghost ascension branches alongside
native GameContext.cpp, Shop.cpp and MonsterSpecific.cpp. Local stock jar SHA256:
`cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`.

No additional clear mechanism defect was identified in this pass. Retain the
previously corrected A16 inventory markup and purge discount ordering; do not
apply an extra A16 markup to Courier restocks, which stock does not do. Added
executable threshold regressions at A14/15, A17/18 and A18/19 for Shining Light,
Nob Enrage/first attack, Sentry Dazed count and Hexaghost Sear Burns. These tests
run native mechanics, not merely searches for constants in source text.

An apparent stage-export goal mismatch was ruled out by entry-point testing:
that branch is restricted to smoke/pilot. Act1 final export already uses ACT1.
Added checks of the actual exported ascension range for both A0 and A20; no
unnecessary change to export control flow was retained.

This targeted review is sufficient to proceed with the experiment, not a proof
of all-event/all-relic parity. Keep crash/limit telemetry and minimize suspicious
mechanical failures before changing training parameters. No additional strategy
rules, network changes or reward changes were introduced in this review.

Final release checks: **653 passed, 1 skipped, 3 expected runtime warnings**;
Ruff, vocabulary consistency, native build provenance and Slurm dry-run pass.
