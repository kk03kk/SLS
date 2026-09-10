# Ironclad A0 Act1 v4 5M：审计与下一轮决策

结论：保留 4,505,600-step best。建议以累计 10M 为下一轮预算上限，做明确标记的 half-LR continuation；不建议从 best 原封不动恢复全部随机状态并声称这是一个新尝试。100-seed natural-policy diagnostic 未发现明确 simulator bug。最值得关注的新行为证据是 Neow 几乎固定 Boss swap，以及前期资源/构筑与 Guardian 战处理共同造成失败。

## 1. 实验实际结果

证据来自 `sls-act1-v4-5m-complete.tar.gz`。原包及旧 checkpoint 未修改；安全解包至 `local/audits/act1-5m-complete/local/runs/`。本次直接检查 metrics、两个作业的 stdout/stderr、manifest、配置、best/final/导出模型与评估结果。分析以实现及重放为依据。

| 累计 steps | 固定集通关数 | 通关率 |
|---:|---:|---:|
| 0 | 0/512 | 0.00% |
| 507,904 | 8/512 | 1.56% |
| 1,015,808 | 137/512 | 26.76% |
| 1,507,328 | 221/512 | 43.16% |
| 2,015,232 | 358/512 | 69.92% |
| 2,506,752 | 319/512 | 62.30% |
| 3,014,656 | 352/512 | 68.75% |
| 3,506,176 | 351/512 | 68.55% |
| 4,014,080 | 369/512 | 72.07% |
| 4,505,600 | 391/512 | 76.37% |
| 5,013,504 | 357/512 | 69.73% |

0→2M 学会了基本战斗和整幕推进，固定集约达 70%；之后仍有提升，但伴随明显胜负交换。4.5M→5M 的净下降为 34/512（6.64 个百分点），92 个 seed 胜转负、58 个负转胜；成对二项检验 p≈0.00685，仅作为这次事后对比的描述，不是经过多次选模校正的发现。

最佳模型的独立 1024 seeds 为 769 胜，75.10%，Wilson 95% CI 约 72.36%–77.65%。与固定集 76.37% 接近，未见明显固定-seed 过拟合。5M 的 `final.pt` 本身只有固定集 69.73% 的证据；不能把 best 的 held-out 成绩归给它。

| Boss | 所有该 Boss seeds 的胜率 | 实际进入 Boss 后胜率 | Boss 战死亡 |
|---|---:|---:|---:|
| Guardian | 200/321 = 62.31% | 200/302 = 66.23% | 102 |
| Hexaghost | 289/369 = 78.32% | 289/340 = 85.00% | 51 |
| Slime | 280/334 = 83.83% | 280/317 = 88.33% | 37 |

255 次失败中 190 次在 floor 16，65 次在 Boss 前。Guardian Boss 战死亡占全部失败 40%；即使消除这 102 次死亡，总体也只有约 85.06%，所以只修 Guardian 局部战斗不足以达到 100%。Boss 前死亡包含三哨卫 10、乐加维林 7、地精大块头 4 次，也有多种普通战斗。

306 个有效更新编号无重复，数值指标全部有限。训练记录 12,467 胜、22,090 死亡；backend truncation / step limit / cycle limit 均为 0。正式评估的 backend error、truncation、timeout、cycle/step limit 也均为 0。评估实现遇到 backend 异常会抛错，而不是悄悄跳过 seed。

后期平均 final KL 约 0.015–0.016、clip fraction 约 0.13–0.14、归一化 entropy 约 0.42、value explained variance 约 0.54–0.56。共 22/306 次触发 KL 停止；最大 KL≈0.074 出现在第一次更新，后续峰值约 0.025。梯度日志为裁剪前范数，约 0.5 不意味着裁剪失效。没有数值崩溃或 value 完全失学的证据，也不能据此否认 policy oscillation。

旧 Note/potion alias 崩溃确实存在；最终轨迹来自修复后恢复的分支。回滚后的有效 metrics 连续，旧失败日志另存。恢复作业 stderr 的旧 native 探测失败随后完成构建，NumPy/cuBLAS 提示不是最终失败。训练程序完成了保存、held-out 和导出；压缩包没有独立 sacct 记录，因此未把用户提供的 Slurm 状态当作额外实测证据。`promotion_passed` 在这条流程代表允许导出，不代表接近 100% 达标。

采样加 PPO 更新累计约 16.6 小时，通常 80–87 steps/s。64/8 与 128/8 benchmark 相差仅约 1.8%；不建议加 workers。没有 GPU utilization 时间序列，不能用显存占用推断利用率。最终显存峰值约 38.8 GB，保持 1024-seed evaluation，未经分批实现与验证不直接扩大同时推理的 seed 数量。

## 2. 唯一指定的 continuation 来源

- 文件：`local/runs/ironclad-a0-act1-v4-5m/stages/train/selection/best_progress.pt`
- steps：4,505,600；update：275；episodes：31,363；next_seed：10,031,427。
- SHA256：`e0a86734786b520ad50d23ff05bed9241e4a4d7cc8442d6372c8fa5a47645b73`
- 这是完整训练 checkpoint：模型、82 组参数对应的 Adam state、RNG、循环记忆、episode limits、64 个 worker 状态都在。
- 与 `checkpoint-steps-000004505600.pt` 的模型权重一致；最终导出的推理模型权重也与它一致。导出模型没有完整优化器/worker 恢复能力，不作为续训来源。

本地实际创建了隔离的续训验证副本，逐项比较模型、Adam moments/step、RNG、worker、记忆；仅显式 LR 与运行计划身份变化，64 个真实 worker 状态全部恢复并 round-trip。旧训练 seed 上限保持 2,000,000,000,000，旧 held-out 不会被新训练纳入。

## 3. 完整轨迹与 simulator / observation 审计

现有 `seed_results` 保存终局、牌组、路线、上下文摘要；`failure_traces` 最多 16 类、每类末尾 32 步。它们不包含 Neow 至结束的所有状态/动作；checkpoint 中的 64 个环境是训练现场，也不是 1024 个评估 seed 的轨迹。

因此完成了 100-seed corpus：40 个胜利、40 个 Boss 战失败、20 个 Boss 前失败，覆盖三个 Boss。各层内部按 `SHA256(act1-v4-diagnostic-v1:seed)` 排序，配额和完整 seed 清单保存在 `corpus100/selection.json`。这是刻意取样，40/100 不是模型总体胜率。

使用正式 `evaluate()` 的 eval/no_grad/argmax、循环记忆、previous action type 和 raw reward 推理流程。仪表只包装 simulator 的记录，不改变输入或采样。保存初始公开 Observation、每一步动作及全部合法动作、native action 映射、后续公开 Observation、reward/终止及自动动作；前一行即下一动作的前置状态。私有 native/RNG floor-entry 快照单独保存用于 replay，不进入模型输入。整个 corpus 约 10.2 MiB。

- 100 局的胜负、楼层、steps、路线及最终牌组全部与服务器结果一致。
- 不重新推理，逐动作重放全部 15,379 步；公开 Observation、合法动作顺序、native 映射、reward、终止和自动动作均一致。
- 比对 1,618 个原生快照，包含 RNG 与 continuation 状态；均一致。
- 检查 HP 范围、非负金币、楼层推进、动作唯一性、公开 draw pile 不带隐藏排序、无已持有 Prismatic Shard。
- 12 次纸条自动离开正常。记录中无战斗动作意外修改永久牌组。
- 资源变化的可疑类别已核对：开箱添诅咒对应 Cursed Key；选卡多 9 金对应 Ceramic Fish；领遗物改变升级数对应 War Paint/Whetstone。使用本地原版 JAR 的 javap 输出核对这些效果，以及 Sharp Hide 的每张攻击触发反伤机制。

分类结论：

A. 未发现新的明确 simulator/backend bug。
B. 未发现明确 observation/action encoding 错误；Neow 随机奖励/代价、Guardian Mode Shift/Sharp Hide 及其数值与 owner 都在公开输入中。模型并非只能看见选项编号。编码只消费 Decision 的公开字段，私有 seed/RNG 快照没有进入网络。
C. 存在策略层面的明显不足，见下一节。
D. 复杂牌/遗物的全部队列顺序、全事件分支及全 seed parity 仍需更广原版对照；同 simulator replay 一致不等于原版 parity certification。

准确结论是：**100-seed natural-policy diagnostic 未发现明确 simulator bug**，没有新的证据要求迁移 observation、重置模型或宣布本轮结果被 backend bug 污染。

## 4. 75%→100% 的障碍：证据与边界

### 前期决策和探索

100/100 的 greedy Neow 都选择 Boss swap；这些初始公开选项上的 swap 概率最小 98.01%、平均 99.51%、中位 99.71%。整体 entropy 尚可掩盖了这个局部选择几乎固定的事实。不能说 Boss swap 总是错，但当前模型几乎不根据不同 Neow 选项改变选择。

为检验“延迟失败可能来自前期选择”，额外做了同一 30-seed 小对照：每个 Boss 取 5 个原胜利、5 个原失败，按 seed 升序；同批次 greedy control 重现原 15/30。仅在 Neow 强制第一选项，随后仍由同一冻结策略自主行动，得到 22/30，11 局救回、4 局丢失；Guardian 子集 5/10→9/10。成对精确检验 p≈0.118，样本不足以做总体提升声明；不同 Neow 改变了随后牌组与随机过程，不能拆成某一张卡或单纯治疗的效应。冻结策略也可能不适应其他开局。

这个干预至少证实，一部分失败可以在不改变后续战斗网络的情况下，通过前期选择改变。因此不应把 floor-16 失败一概解释为“Boss 样本不足”，也不应把第一选项或禁 Boss swap 写入训练规则。

### 局部战斗和资源

诊断集有 4/14 个 Guardian 战失败是在满 HP 进入 Boss 后发生，排除了“所有失败都只是进场残血”。失败组进场平均 HP≈64.6，胜利组≈78.1；这是病例分层样本的相关性，不是独立因果效应。构筑质量、遗物与路线同样不同，牌组大小接近（约 17–18 张）本身没有指向容量或 deck-size bug。

- seeds 2000000000173 / 2000000000739 在 3 HP、有合法 Defend 时先打攻击，被 Sharp Hide 立即反伤死亡；原生反事实先 Defend 再攻击可保留 3 HP，但未证明必能通关。
- seed 2000000000714 的一次 Strike→Defend 交换为 Defend→Strike，在重放同回合后续动作至结束后多保留 3 HP。
- 另外两次看似类似的顺序交换，整回合 HP 完全相同。因此没有把所有“先攻后防”归为错误。
- 100 局共 253 次 Rest，其中 24 次满血；149 次买卡，仅 1 次买遗物、0 次买药水；有 100 次留能量且仍有合法出牌的 End Turn。这些是审查线索，不能不看上下文就计算成错误率或据此加惩罚。

### 训练机制判断

训练预算仍可能不足，但不能承诺翻倍预算就接近 100%。已有较大胜负交换，且存在 Neow 局部探索停滞。value 有学习信号但不完美；缺乏分事件/分 Boss 的 value calibration，无法把它定为单一根因。没有证据要求扩网络或改变 observation 张量。

reward 的 terminal success=+1、floor-16 failure=-0.2，明确区分真正胜利与只到 Boss；gamma=1、terminal potential=0 的 shaping 会望远镜抵消，不等于永久奖励休息或囤 HP。失败进度项会偏好更深失败，是否妨碍最后胜率需独立 ablation，目前未证明。GAE lambda=.98 的直接 TD 残差传播按 .98^k 衰减（100 步约 .133），早期构筑信用主要依赖 value 的间接传播；这是合理的长程风险解释，不是已证实的算法错误。

## 5. 下一轮：best → 累计 10M 的单变量分支

不建议“从 best 原样 exact resume”当新尝试：保留模型/Adam/RNG/workers 且参数不变，在相同确定性运行条件下会重现已有 4.5M→5M 的 31 次更新。改变评估频率不改变训练梯度。可以从 final 原样往后跑并保留 best，但那是另一种预算续跑方案，不是从 best 改善更新轨迹。

建议本轮从 best 创建明确的新分支，仅将 LR 2.5e-4→1.25e-4。依据是多次 policy regression 和希望降低更新幅度；一半 LR 是受控试验值，不声称已证明最优，也不声称它能直接解除 Neow 的 99.5% 集中。

保持：v5 网络 128/4层/4heads/FFN256/循环256、64 workers/8 shards、rollout256、sequence64、minibatch16 sequences、epochs2、clip.2、gamma1、lambda.98、targetKL.02、grad clip.5、value系数.5/value clip.2、全部 reward 和 episode limits。

entropy 保持原 40M 线性 .02→.002，累计步数不重置：best 约 .01797，到 10M 约 .01550。不同时重置 entropy、换网络、改奖励，以免无法归因。新增 Neow 的实际选项计数、mean swap probability 和 normalized entropy 日志，仅观测、不改变采样或 loss。

目标 `10,000,000` 是累计环境步数。按 16,384-step 完整更新，从 4,505,600 继续约新增 5,505,024 steps，预计停在 10,010,624，而不是再训练一个独立 10M。废弃 4.5M→5M 分支的已花算力不会被冒充未花；新分支逻辑累计与历史实际总算力是不同口径。

评估每 0.25M，仍用原固定 512 seeds；保存每 0.25M，保留 latest/best/final。best 仍优先固定集通关数，同分保留较早模型；不增加未经验证的 reward 或统计显著性选模门槛。初始化子目录就保留原 best，因此后续回退不会丢掉它。更频繁选模会有选择偏差，正式改进结论依赖独立终评。

新终评用 1024 个未参与本次分析的 seeds `[2000000001024, 2000000002048)`。旧 held-out 已被本次诊断使用，应视为开发证据。10M 结束后，另对旧 best 在**同一新集合**做一次服务器 evaluation，再按 seed 比较 win↔loss、置信区间、各 Boss 和死亡楼层；这一步不在本地重跑。

6M/7M 应作为人工审查点：如果固定集持续低于原 best、Guardian 没有改善、Neow 仍接近完全固定，不应只因为预算还剩就继续加码。下一项值得测试的是针对稀少决策的通用探索/信用分配实验，使用单独分支和相同 seeds；不要直接加入高手选项规则。当前入口不会自动停在这些人工审查点。

10M 的“真实提升”需在新 held-out 对旧 best 做成对比较（例如 paired CI 的下界>0），且 Guardian 等关键失败类别改善、环境健康保持。固定集只多赢几局而独立集无改善，不算已证明进步；即使达到 85% 或 90%，也还没有完成接近 100% 的目标。

## 6. 实现与服务器步骤

新增 `configs/train/ironclad_a0_act1_10m.toml`、`tools/initialize_act1_continuation.py`。`--prepare` 会核验 parent SHA/config/model/native/PPO，仅允许明确的 half-LR 变化，原子创建独立子目录，保留模型、Adam moments/step、RNG、记忆和 worker 状态；只在新副本修改 LR 与运行计划身份。未知环境/模型/奖励变化仍拒绝，旧目录和 checkpoint 不改。

准备阶段会使用实际 child/latest checkpoint 做完整 worker-layout preflight：恢复、一次真实 PPO 更新、临时保存恢复的继续更新一致性检查。准备测试不计入正式训练 steps。LR 的标量变化不改变测速工作量，复用原 benchmark 的 64/8 布局与原始报告，不伪造新吞吐结果。新增配置与代码会使旧 ready 失效并触发本次验证。

在本地代码发布后，登录节点只执行：

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a0_act1_10m.toml \
  --prepare
```

服务器必须保留原 `local/runs/ironclad-a0-act1-v4-5m` 及 preparation/benchmark。计算节点的 parent 核验、实际 checkpoint preflight、worker/optimizer 保存恢复检查和新 baseline 全部通过后才开始正式更新。没有绕过 software/runtime contract：本地 Torch 2.10 与服务器 2.6 不同，本地测试不能冒充服务器真实 checkpoint 的跨软件 exact-resume 认证。

10M 结束后，用原 best 做新 held-out 对照：

```bash
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py evaluate \
  --checkpoint local/runs/ironclad-a0-act1-v4-5m/stages/train/selection/best_progress.pt \
  --evaluation-profile IRONCLAD_A0_ACT1 \
  --evaluation-seed-start 2000000001024 \
  --evaluation-episodes 1024 \
  --evaluation-output local/runs/evaluations/act1-parent-best-new-heldout-1024.json
```

已完成本地原始 best 状态核验、100-seed 完整推理/重放、30+30 小干预对照、单变量 LR 的状态保持及拒绝不兼容回归测试、实际配置短 GPU preflight。全套 613 项测试通过、1 项历史不兼容模型测试跳过；Ruff 与 Slurm dry-run 通过。没有从本地提交服务器作业。

## 7. 本地证据索引

根：`local/audits/act1-5m-complete/`

- `metrics-analysis.json`、`learning-curve.png`：曲线、PPO 分段统计与成对回退。
- `corpus100/selection.json`、`result.json`、`seed-*.jsonl.gz`：选择规则、正式语义结果、完整公开轨迹。
- `corpus100/replay-*.json.gz`：独立私有恢复快照；`analysis.json`：15,379 步重放与资源/行为摘要。
- `resource-analysis.json`、`counterfactuals.json`、`neow-probabilities.json`、`neow-intervention.json`、`neow-control.json` 位于 corpus100。
- `*.bytecode.txt`：本次 targeted 原版规则检查。
- `continuation-validation-result.json`：真实 parent→独立副本状态保持结果；`continuation-validation-v3/` 是本地验证副本，不需上传。
- `local-10m-preflight.json`、`full-tests.log`：本地验证。
- 重建 corpus：`python tools/diagnose_act1_corpus.py --run <解包后的5M目录> --output <全新目录>`；重放：`python tools/analyze_act1_corpus.py <corpus目录>`。已存在的 corpus 不会覆盖。
