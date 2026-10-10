# 2026-10-09 训练发布状态

训练前Act1+Act2有限工作已完成，隔离发布提交5e26eea0ab9c38a6130d8bddd30749f75c5a9a40已推送main；发布来源6efbb772958c06d1b9133f374d046d8838eaa256da1a146eaf7e1ee530eefd3d，只包含b100加盗贼药水与cardRng边界修复。93项相关测试、35配置检查、父四文件/bundle SHA、迁移/来源绑定与seed冲突扫描通过。发布来源重放63有效战斗、36共享机制、9完整系统分支和6正常Neow轨迹1990边界；两条已知差异与旧来源完全相同，Automaton334边界条件匹配保留资格边界。

可以提交带强制启动门禁的20M作业，不表示NUS真实模型验收已经通过或训练已运行。预算含524288预热，不额外收费预算。唯一启动脚本：local/operator/start-critic20m-20261009.sh。正式说明在隔离发布目录docs/results/act12-critic20m-20261007/READINESS_20261008.md及GitHub同提交。

D:/SLS本地完整审计来源6805与未提交的晚幕改动全部保留，未整包发布；原版/字节码/原始日志未上传。Act3/钥匙取得/Act4/Heart及可达内容完整认证仍未完成，不作为本次Act1+Act2的无限前置。以下8日状态是历史快照，旧未发布陈述不代表当前发布状态。

---

# 2026-10-08 当前进度（恢复后）

本轮尚未达到训练发布完成条件。下面历史记录保留原身份；旧暂停状态、旧来源及旧完整测试结果不是当前结果。

六种有序A20双Boss组合已有受控代表性原版证据；最后两种Awakened作为第二Boss的生命周期、终点、恢复见double-boss-awakened-second-qualification-r1.json。尸体Artifact/Ritual差异保留原始equal=false，仅按窄条件分类，不一般忽略powers。

新增确认修复：transitionToAct对cardRng计数边界错误。原版0/250/500/750不推进。新共享环境来源6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0。14条原版回归修复前11失败/3通过，修复后14通过；相关局部157测试通过，另22项Act1→2/Act2→3合成边界探针通过（旧来源6失败/16通过），单线程native构建/import及相关Ruff检查通过。不是完整测试集。详见card-rng-boundary-correction-r1.json。

Oracle初态另有工具缺陷：setCounter不能回退，131200248请求0实际250，明确排除。1.3.41重建受控初态RNG并严格检查独立读数；131200258–260初态及终点匹配、边界恢复一致。两批游戏退出后各63项文件独立哈希恢复核验零差异，目前无游戏运行。

Act4入口仅验证初态已持三钥匙后的真实双Boss、对话、实际TheEnding MAP，不涵盖钥匙获取/机会成本、燃烧精英、Act4商店/休息全流程。可达内容、事件及正常晚幕轨迹仍有未验证义务。Discovery production计时资格继续保留；不能宣称全三幕/Heart完成认证。

隔离训练发布目录仅提取盗贼奖励与共享RNG两项修复，未混入晚幕整体修改或速度优化。发布来源单线程构建与82项相关轻量测试已通过；显式weights-only migration与配置/plan/operator SHA绑定、35项配置轻量检查以及29份实际评估记录的seed冲突扫描也已通过；仍需完成剩余模拟器义务与最终发布审阅，NUS计算门禁未运行；未commit/push。服务器真实模型门禁未运行。不能执行历史aa63作为更新后的训练。历史lambda结果不改写。

---

# Current full-run simulator audit status

Latest: six actual controlled Awakened-first double-boss runs (seeds131200231–236) witness first-phase death,320HP rebirth,final victory and subsequent Time Eater or Donu/Deca completion. Initial/second-entry and enumerated VictoryRoom endpoint fields match; all native boundary restores match. Dead second-Cultist Ritual5 is narrowly source-classified animation cleanup; raw inequality retained.84 bounded checks,Oracle1.3.37 production smoke and both63-file independent recovery checks pass. Native9bbf5664 unchanged. Four ordered pairs have representative controlled evidence; two Awakened-second pairs and broader keys/Act4/content/natural-trajectory scope remain open. See double-boss-awakened-first-qualification-r1.json. Older entries are historical.

Latest: controlled reverse order Donu/Deca -> Time Eater -> VictoryRoom also matches enumerated initial/second-entry/endpoint fields and boundary restores for seeds131200228–230. Oracle1.3.36 production smoke and validation batch pass; both63-file recovery checks have0 mismatches.63 bounded checks pass. Native remains9bbf5664, no new production change. Two ordered pairs now have bounded representative evidence; four Awakened One pairs remain open, along with full keys/Act4/content/natural-trajectory scope. See double-boss-reverse-qualification-r1.json. Historical entries below retain their original stage identities.

Latest bounded qualification (Oracle1.3.35, native9bbf5664): seeds131200225–227 execute actual Time Eater -> Donu/Deca -> VictoryRoom. At the aligned floor52 endpoint, HP/maxHP/gold, ordered deck IDs/upgrades and all14 RNG streams match; every native decision-boundary restore matches. Dead Deca Artifact cleanup remains a raw direct-state difference, independently traced to stock death-animation cleanup and narrowly classified without changing exact equality.53 focused checks and Ruff pass; production smoke passes; two independent63-file recovery checks have0 mismatches. Other five ordered pairs and full late-act/content obligations remain open. See double-boss-terminal-qualification-r1.json. Earlier paragraphs are historical stage records, superseded only within these enumerated fields. No resume waiver or server change.

Latest: three stock-controlled complete TimeEater->Donu/Deca fights captured; terminal-victory restore bug confirmed and fixed,131 targeted checks pass. New native source 9bbf5664d5efa1eb65fdbfde8ebdf5f16b6a0d3507f2231aad2da001d35ae606. Corpse Artifact direct-state difference and final victory-room boundary alignment remain open. See double-boss-terminal-source-transition-r1.json; no complete-pair certification or resume waiver.

Flow-probe preparation: Oracle1.3.33 supports reviewed initial upgraded-card/permanent-deck fixtures without later battle interventions;39 lightweight checks and production smoke pass,63-file independent restore has0 mismatches. New flow fixture has not run. Full six-pair obligations remain open; native81e83de9 unchanged. See double-boss-progress.md.

Latest extended batch: three stockr3 seeds match first/second stable decision projections, direct ordered card/power/item fields and legal actions. Oldr2 pending DEBUG intent is rejected for full decision qualification.120 targeted checks pass;Oracle1.3.32 production smoke passes;two63-file restore checks have0 mismatches. Native81e83de9 unchanged. See double-boss-entry-extended-evidence-r1.json; complete second victory/other five pairs remain open.

Newest: first-boss victory initialization omission confirmed and repaired on native81e83de9. Three before regressions fail;64 focused/adjacent checks pass after repair. Selected second-entry fields match three stock seeds after MawBank fixture correction. See double-boss-source-transition-r1.json. Six complete pairs and full projection remain open; no source-preserving/resume waiver. Earlier paragraphs retain their original stage identities.

**Current human scope: full Act1–Act3 + A20 ordered double Bosses + keys + Heart.**

Latest source-review batch: four stock classes freshly exported and independently
reviewed for A20 double-boss entry/transition. All six ordered pairs remain
runtime-unverified. Existing isolated encounters do not establish consumed
bossList/bossKey; see double-boss-progress.md for the concrete tool gaps.

Tool follow-up: Oracle1.3.31/v13 adds independent boss-list diagnostics and
validated initial consumed-list fixtures; capture supports stock proceed to a
strict second-combat boundary. Both production/validation smoke pass after an
explicit reader-version correction (initial failure preserved);48 lightweight
checks pass and three recovery receipts independently verify63 files each.
No ordered-pair runtime scene has yet executed. Native stays32cbf557….

Follow-up stock witness: Time Eater -> Donu/Deca actual continuous transition
captured at3 seeds131200219–221, floor50->51. Oracle1.3.32/v13. This is stock-only
entry evidence; native comparison and second victory remain open. Initial tool
failure (incorrect reward/COMBAT overlay assumptions) preserved;49 lightweight
checks pass, both launches independently restore63 files with0 mismatches.
See double-boss-entry-stock-witness-r1.json; prior no-execution sentence is historical.
The prior Act2-only restriction is superseded. See
[FULL_SCOPE_RESUMED_20261008.md](FULL_SCOPE_RESUMED_20261008.md).
Current native is 9bbf5664…; the older identity and scope paragraphs below are
historical records, not current complete-qualification claims.

Latest update: Implant permanent obtain and selected relic callbacks are repaired
on native 32cbf557…; 12 stock-controlled deck/resource/RNG comparisons match.
Nine additional stock Implant -> actual lethal -> stable reward runs now match
the enumerated deck/resource/reward/RNG fields (seeds131200210–218).
Oracle1.3.30 production smoke and40 current targeted tests pass; independent
restore checks cover63 protected files per launch with0 mismatches.
Reward collection/next-room continuation and other relic combinations remain open.
See implant-victory-evidence-r1.json and writhing-implant-progress.md;
older UNFIXED/c071 paragraphs below record the prior stage, not the current status.

Earlier full-scope progress: Giant Head late escalation/cap matches three stock
12-turn traces (39 boundaries), with original-derived restore regressions.
Writhing Implant permanent-deck timing is now a confirmed three-seed native
difference and remains UNFIXED. See giant-head-late-progress.md and
writhing-implant-progress.md. Oracle 1.3.28 adds validation-only direct master-deck
evidence; source identity stays separate from historical 1.3.26/27 runs.

> 2026-10-08 最新补充：[盗贼奖励修复与真实 Act2 收尾](ACT12_REWARD_CLOSEOUT_20261008.md)。新 native 身份 c07116cc…，15 次 TheCity 奖励对照完成；历史收尾及训练路径保留原身份，不再代表当前发布状态。

**2026-10-08 update:** all-thieves-escaped potion rule repaired with stock runtime evidence; 15 actual-TheCity controlled reward runs match. See ACT12_REWARD_CLOSEOUT_20261008.md for current identities, recovery and migration restrictions. The entries below preserve historical stages.

**Active scope changed by the human:** Act1+Act2 training preparation only.
See [ACT12_TRAINING_SCOPE.md](ACT12_TRAINING_SCOPE.md). Act3/Heart expansion,
ordered double Bosses and Act4 flow are deferred; their existing evidence stays.
The tables below retain completed full-run work and are not a new training gate.

Current bounded Act1/Act2 review is closed for the existing matched training
environment:63 combat obligations +9 unique system seeds replayed,2262 checkpoint
continuations,36 shared-mechanic runs, and six strict normal-start trajectories.
Historical invalid Entangle probes and production timing differences are retained.
See act12-training-audit-r1.json. Server6e54adbc and clean HEAD e1902d6a source
digests match; use that training environment for later endpoint binding. Local
late-act corrections are not automatically migrated. Real pilot results and
parent binding remain prerequisites; no new launch plan or GitHub push made.

Final scope-specific completion check:44 targeted tests pass, including injected
damage/mask/duration/RNG/cost differences and direct-state tampering;63 protected
files independently rehashed with no game running,0 mismatches. Read
[ACT12_CLOSEOUT.md](ACT12_CLOSEOUT.md) for the requirement-by-requirement record.

The audit is ongoing. Read this file first; older batch reports preserve their
original identities and findings. No local audit work has been committed/pushed,
and server job916291 remains on6e54adbc. Final holdout seeds are untouched.

## Current identity

Native canonical source:
`0fd7d4c6d4a78d6078cb9169cb679212b1734ce5a47f575e523787551bc42812`.
Latest completed controlled and production-smoke Oracle:r25/1.3.22,
`6c6ffe24ef6ed82a8cae68348dd4f14689f2bfd01d4e685de1b9f82fe8740fcd`.
Stock JAR:
`cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`.

## What is supported by evidence

| Area | Evidence and conclusion | Remaining scope |
| --- | --- | --- |
| Inventory |803 conservative candidates, not803 certified/reachable items | Reachability and item/event branches |
| Shared mechanics |36 controlled runs for12 selected interactions; separate direct projection checks | Lethal callbacks and other combinations |
| Natural Act2 flow |Collector349 boundaries match; Automaton334 only under independently witnessed Discovery timing | Unconditional timing and later natural trajectories |
| Act3 basic encounters |All16 have actual stock multi-turn basic captures | Special AI/phase/death branches |
| Reptomancer |Three targeted replacement-Dagger traces; production target mapping matches | Declared dead Minion residues, leader death and longer histories |
| Ending interactions |12 strict cap/reset/Beat/Defend/facing matches | Death/terminal combinations beyond measured scripts |
| Shield/Spear one-survivor death |Three strict matches; three dead-Shield-only BackAttack residues, subsequent turns match | Complete encounter win/other modifiers |
| Heart fatal Beat |Six strict loss matches, raw piles/masks/RNG retained | Other relic/death interactions |
| Heart lethal card/potion |Six terminal-resource matches after isolated probe and terminal RNG repairs | Full victory cleanup/route; no natural-win claim |
| Darkling revival |Six edge/center half-death/revival traces strictly match including masks/RNG | All Darklings down together, other death effects |
| Awakened One rebirth |Six original traces match after two separate fixes, powers before/after phase included | Full Boss win/double-Boss continuation; other debuff/relic combinations |
| Time Eater Haste |Six original traces strictly match crossing half HP and negative Strength, healing/block/cleanup/actions/RNG | Other modifiers and final Boss flow |
| Donu/Deca one-survivor |Six strict matches after dead-partner support guards; measured old difference confined to corpse fields | Complete Boss victory, relic death modifiers |
| Giant Head Slow |Six strict card/potion/reset traces and regressions match without rule changes | Late escalating attacks, other modifiers |
| Writhing Reactive/Malleable |Nine strict positive/blocked/potion/reset traces match, guaranteed zero-damage fifth hit added | Implant and master-deck timing, other modifiers |
| Transient/Reptomancer/all-Darkling endings |Nine strict pre-terminal prefixes and final HP/outcome/RNG subsets match after separate elite-probe and Reptomancer death repairs | Terminal powers/piles/reward content and continuation, death relics |

## Actual errors versus tool corrections

Production rules repaired: Darkling doubled Nip ascension bonus; Writhing Flail
block and repeated-move RNG; Heart post-victory misc draw; missing Curiosity
Strength callback on power-card use; Donu/Deca support incorrectly applied to a
dead partner; Reptomancer omitted living-Dagger queued deaths. These change
source/environment identity.

Tool corrections: actual Act3/Act4 context setup, production-view target mapping,
bounded Oracle action drain, isolated Act4 restore room/callback, and excluding
half-dead monsters from the isolated battle's corpse-power cleanup, and controlled
Act3 elite-room restoration. These are
separately recorded; they are not all production bugs.

The latest Curiosity repair queues Strength for the actual power owner. The
isolated half-dead cleanup repair alone makes the basic rebirth traces match;
the power-card traces still fail until Curiosity is fixed. Before/intermediate
failures and old source/binaries remain local. No global resume waiver added.

PPO, reward, network, distribution, observation/action schema and model/checkpoint
layouts are unchanged. New environment source identity is mandatory; Act1+Act2
compatibility must be reviewed explicitly before a future server continuation.

## Checks and next work

Current full Python suite:1338pass/1 intended skip/4 warnings on0fd7d4c6.
Latest focused33 regressions pass. Earlier full
checks failed only the Oracle resource inventory's stale version/count; those
logs remain preserved. The frozen-input complete rerun passes. Native build/import
and targeted7 build checks pass.
Latest focused18 original-derived revival/terminal tests pass. Native build/import
passes. Separate source-transition records retain both Awakened roots;285 fixed
normal native boundaries are unchanged. This is a native regression control,
not original-game or win-rate evidence.
Each completed game batch restored and independently rehashed63 protected entries.

No game batch currently running. Corrected late-terminal batch completed;
failed batches remain execution failures. Manifest encounter IDs now receive
pre-launch allowlist checks, with two rejection/selection tests passing.
Production smoke and63-entry recovery rehash pass. The previous proposed next
work on ordered double Bosses/keys/Act4 flow is now deferred at human request.
Next work is current-source Act1/Act2 evidence replay, source-transition review,
and binding a verified training endpoint when real pilot results are available.
Remaining mechanisms include
late Giant Head attacks and Writhing Implant/deck timing. Ordered
double Bosses, keys/Act4 route, reachable content/events and natural late-act flow
remain. No whole-game qualification or training-effect claim is made.

Oracle1.3.41 production隔离smoke通过，退出后独立63项恢复哈希零差异。隔离发布轻量/局部测试现在86通过（加入4项父身份/migration拒绝测试），相关Ruff和差异检查通过。尚未commit/push；晚幕/钥匙未完成项仍未升级为通过。
