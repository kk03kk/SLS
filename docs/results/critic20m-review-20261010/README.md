# Critic20M 中止实验核验与项目方向：2026-10-10

**判断：支持停止原样续训，保留全部模型；不支持宣告 critic 预热无效，也不支持继续靠降低 value loss 解决通关。** 同环境开发集已经显示明显的 Act1 能力流失，却没有确认两幕联合成功的改善。下一阶段应围绕“后段经验供给＋正常开局能力保留”组织实验，而不是直接扩预算、扩网络或进入未经资格验证的完整 A20H 长训。

用户确认本次是主动停止作业以便分析。取消日志不是训练崩溃证据。下面分别列出事实、因果限制、工程风险和可执行的下一步。

## 归档完整性、身份和实际预算

输入：`D:/SLS/local/imports/critic20m/critic20m-stopped-20261010.tar.gz`，127,059,225 bytes，SHA256 `4f8f1c7042946198619b9105c872917981925a9dc299bbebc786b2e1f67b726e`。完整读取 gzip 至 EOF 校验 CRC/trailer；逐个核对 19 个 TAR 成员（15 个普通文件、4 个目录），拒绝路径越界、链接和重复成员。详见 [archive-audit.json](archive-audit.json)。本地散列绑定收到的文件；没有服务器独立散列，不能额外声称下载真实性已获得双端证明。

训练身份为干净 `80bfb37a806c18154ab38f6a845160fbf59634ce`，native 源摘要 `6efbb772958c06d1b9133f374d046d8838eaa256da1a146eaf7e1ee530eefd3d`，训练实现摘要 `1459b6ff6737bd72852965f230d2c1a86957eb9afce0daea172b56f43785189e`。NUS 为 A100 40GB、16 CPU、64 workers/16 shards、Torch 2.6.0+cu124。目标是自然开局战士 A20 Act1–2，**不是完整 A20H**。

初始化保留冻结 90M 模型全部权重，重置 Adam、RNG、worker、recurrent memory 和选择记录。前 32×16,384＝524,288 decisions 只预热 value head；之后解除冻结，进行整个网络的 PPO。名字里的 critic20M 不表示 20M 全程只训练 critic。预热代码和模型前向路径均已查验；新增测试证明单独改动 value head 不改变 actor logits 或 recurrent memory。日志声称 compute gate PASS，但其原始 probe/compute-gate 文件不在归档中，不能仅由一句 PASS 重做完整独立验收。

| 证据位置 | 累计 steps | 新增 decisions | update |
|---|---:|---:|---:|
| 冻结父模型 | 90,013,696 | 0 | — |
| best_progress | 92,012,544 | 1,998,848 | 122 |
| latest.pt | 97,009,664 | 6,995,968 | 427 |
| 最后完整训练日志 | 97,730,560 | 7,716,864 | 471 |
| 原计划终点 | 110,013,696 | 20,000,000 | 未达到 |

因此只完成了计划的 **38.58%（日志口径）**；可恢复模型只有新增约 7.00M。`latest.pt` 与第 97M 周期文件模型一致，未保存的日志尾段为 **44 updates / 720,896 decisions**。日志中的最后模型不能由现有权重重建。`best_progress.pt` 在 92M，不能改称最后权重。

471 个训练 update 的 JSON 指标与 stdout 完整逐项匹配；stdout 按原实现省略 evaluation 的 seed_results/failure_traces，这两项保留在 metrics 中。9 个 checkpoint 的 schema、模型、PPO、输入词表、native、训练身份、步数、累计 episode/termination、optimizer 和 recurrent memory 均核对；576 个保存的 native worker 状态在相同 native 源下逐个恢复并完成公开编码。检查未恢复训练进程、optimizer 或旧 RNG 到生产 run，也没有运行这些 worker 的后续训练。

`run-manifest.json` 仍为 RUNNING，日志终止于用户取消；归档没有 `final.pt`、training-bundle、固定终点/reference/selected confirmation evaluation。现有完整实验分析器正确拒绝把它当 COMPLETE；本次新增独立的 stopped-run 审计入口，没有放宽旧分析器或恢复契约。

该保存缺口说明这次停止未完成干净收尾；不能由归档单独区分进程组信号、Slurm KillWait、包装进程转发或最终 SIGKILL。已有训练代码有 SIGTERM 保存逻辑，所以“代码根本没有信号处理”也不符合事实。下一轮应先读取 sacct 和 allocation ledger，验证软停与保存完成，再考虑缩短新实验 checkpoint 间隔。历史丢失尾段无法补造。

## 同环境开发曲线：能力保留已经成为问题

全部评估使用同一组 `[8000012000000,8000012000512)` 512 开局，greedy 策略、相同发布环境。下表由逐 seed 原始结果重算，并非引用 best JSON 或旧结论。

| 模型时点 | Act2 联合完成 | 到达 Act2 | 真正进入 Act2 Boss 战 | cycle limit | 失败楼层中位数 |
|---|---:|---:|---:|---:|---:|
| 冻结 90M | 2/512 | 352/512（68.75%） | 39 | 47 | 21 |
| 92M / selected | 3/512 | 301/512（58.79%） | 44 | 36 | 20 |
| 94M | 3/512 | 260/512（50.78%） | 32 | 18 | 18 |
| 96M | 0/512 | 236/512（46.09%） | 25 | 5 | 16 |

92M 相对父模型，Act2 到达在 103 个 seed 上丢失、52 个获得，净 −51，exact McNemar p≈5.12e−5；96M 丢失 166、获得 50，净 −116，p≈9.87e−16。它们是重复使用的开发 seed，p 值属于探索性证据，不是封存保留集确认。但退化幅度大，不能被“成功数从 2 到 3”抵消。

92M 的联合成功配对是失去 1 个、获得 2 个，p=1；94M 也是如此。96M 的 0 成功不代表真实能力严格为零。这组窗口没有确认新的两幕能力，也没有证明 92M 是最佳泛化模型。best 选择在同一窗口重复比较并取最早最高计数，是开发选择，存在选择偏差。

代码中的 `boss_attempts` 是分配到该 Boss 的开局数，包含尚未进入该战斗就死亡/循环的 run。例如父模型 Act2 assignment 共 352，但真实 combat entries 共 39。`boss_success_rate` 因此混入了整幕生存和路线能力；不能称作“进入 Boss 后仅约 1% 能获胜”。条件在真正进入 Boss 后，本窗口联合成功为父模型 2/39、92M 3/44、94M 3/32、96M 0/25；分母很小且构筑与资源不一致，仍不能据此排名纯战斗能力。

循环减少也有竞争性失败偏差：没有通过 Act1 就不会遇到 floor 17 选择页。floor 17 CARD_REWARD 循环分别为 44、35、11、5；这是有价值的界面行为证据，但其下降不等于游戏能力改善。到达 Act2 时平均牌组大小约 19.67→19.98→19.92→20.11，平均 HP 69.71→68.78→68.71→67.79；没有支持“牌组体积膨胀是主要根因”的证据，也不能由大小排除卡牌结构质量问题。

## 独立本地行为诊断

研发基于此前 CPU 分支，native 仍绑定 80b，没有混入原工作区晚幕修复。新工具复用 backend、公开编码和严格权重加载，只读模型。

当前战斗诊断从前一轮自然状态库固定选择 12 个 Act2 COMBAT 状态：先包括全部可用 Boss 状态，再按状态 SHA256 排序补齐。每个模型使用相同完整公开历史重建自己的记忆，恢复相同 private native 状态，最多 256 决策，**在当前战斗边界停止**。完整 Observation、候选、实际动作和 value 均保存；逃跑、游戏终止、循环与诊断上限分开，战斗结束不伪造完整 shaped MC。

| 模型 | 存活到当前战斗边界 | 死亡 | 未完成 |
|---|---:|---:|---:|
| parent90 | 8 | 4 | 0 |
| criticbest | 8 | 4 | 0 |
| criticlatest | 7 | 5 | 0 |

三个模型在可用的三个 Act2 Boss 状态上均死亡；Champ 缺失，这些还是战斗中的自然中间状态，并非健康资源的新战斗。它没有给出 critic 系列更强的纯战斗证据，也不足以判定网络容量。12 个相关状态不是独立开局，不作为正式胜率。战斗结束 HP 可能含 Burning Blood 等结算治疗，不能直接解释为战斗承伤。原始全轨迹在本地交付包中，统计见 [summary.json](summary.json)。

另使用新的诊断区间 `[132300000,132300004)`，三个模型分别正常开局采集 4 条完整 greedy 轨迹，共 12 条、2,578 decisions、24 个分层自然状态；预先检查命名空间。12 条均真实死亡，没有诊断截断。实际 float32 shaped reward 全程相加、不 bootstrap，得到：

| 模型 | 4 条开局到达 Act2 | 初始 value 均值 | 初始 shaped MC 均值 | 全决策加权 greedy MC MSE |
|---|---:|---:|---:|---:|
| parent90 | 3 | 0.58718 | −1.03627 | 2.49453 |
| criticbest | 2 | −1.01737 | −1.03627 | 0.003239 |
| criticlatest | 2 | −1.02393 | −1.03627 | 0.001155 |

这是对“拟合失败回报不等于获得通关能力”的第二组直接证据；小样本不用于模型泛化排名。父模型 critic 的原目标是 Act1，不能用 Act2 误差宣告其原 critic 错误。greedy 完整 MC 与 stochastic PPO 的 `V+GAE` 目标也不同。详见 [natural-returns.json](natural-returns.json)。全部新旧诊断 seed 与状态只用于研究，不作为训练课程或正式评估样本。

## 算法与计算：证据支持什么、不支持什么

32 次预热累计收集 2,214 个结束 episode，其中 6 个成功；最后丢弃 7,363 个未完成状态的 MC 标签。`warmup_loss` 从 0.55292 到 0.006923，是训练 minibatch 上经过 `value_coefficient * 0.5` 缩放的误差，不能冒充独立 MC 校准。整个 7.72M 新决策只有 124 个训练成功 / 35,207 episodes，约 0.35%；success replay/自然后段经验的供给确实稀少。不存在“完全没有任何正回报”的证据。

前 50 次 PPO 的 combat advantage 缩放均值约 11.57，最后 50 次约 34.46；归一化保持各 domain std 接近 1，使逐渐变小的原始 TD/GAE 信号仍持续推动 actor。shared backbone 同时接受 policy 和 value gradient，并对全模型合并梯度裁剪；早期约 83.1% minibatch 被裁剪，后期约 64.4%。这是应当检查 critic/policy 梯度范数、夹角与 recurrent memory 漂移的依据，**不是已经证明的致因**。没有零方差爆炸或 nonfinite 证据，也不应立即把归一化关掉后宣称修好了算法。

value head 在 actor 输入之外，故纯 head 预热本身不能直接改变 greedy actor；后续全网络 PPO 可以改变共享表示。新 horizon、构筑选择变化、稀疏成功、局部负 TD 学习、探索不足、shared gradient 和旧 recurrent memory 的片段重建均可能参与退化。当前没有 no-warmup 同预算对照，不能把曲线归因于“预热伤害”或证明不预热会更好。

后 50 PPO updates 平均总时长约 142.10 s，其中 collection 101.56 s（71.5%）、optimization 40.32 s（28.4%）；collection 内编码 29.33、transition 38.39、worker step 22.68、policy inference 10.52 s。全部更新记录累计约 18.49 h，不含构建、准备门禁、评估等额外墙钟开销。已确认大量时间在非前向路径，但没有 GPU utilization/CPU/IPC profiler，不能说 A100 利用率就是某个百分比。

此前 CPU 静态字段和指纹优化已通过语义门槛。实际 NUS 日志定位出的 collection 开销使它们值得服务器复测，却不能把本地指纹 18.05% 加速变成 NUS 全训练 18.05%。优化 source 摘要仍会触发旧 critic20M 契约；本次没有恢复白名单，旧 run 不能在优化分支原地续训。

## 下一阶段的独立选择与验收门槛

1. **立即做有限 GPU 开发确认，而非原样续跑。** 固定冻结 parent90、selected 92M、94M、saved latest 97M 四模型，在相同 80b native 上跑原注册的 4,096 development-confirmation seeds。模型哈希及步骤见 [qualification-plan.json](qualification-plan.json)。batch 固定 128、16 shards；与历史 512-seed batching 的数值身份不同，单独记录。它是“中止 checkpoint 的开发确认”，不是补造 20M fixed endpoint，也不是封存最终保留集。已实现独立 Slurm 入口；先收集缺失 compute gate/preflight/ledger，再独立构建和只读评估。
2. **优先研发自然后段课程，但加上正常开局能力保留门槛。** 收集独立训练 seed 上完整公开前缀及 native 恢复状态，覆盖 Act2 入口多选/确认、可生存的普通/精英战和 Boss 前后。候选初始研究比例为 75% 正常开局、25% 自然后段 reset，保留失败与成功、不同资源/构筑的分层分布；不是只挑能赢的牌组。reset 时用当前参数重算前缀记忆，避免复制 teacher memory、泄漏 RNG 或把跨版本 private checkpoint 当真。状态 reset 不强制跳过卡牌确认，不改变合法动作语义。
3. **小预算对照先回答分布干预是否有效。** 新身份、weights-only 初始化，冻结90M为候选起点；两臂同一初始化、奖励、网络、PPO、critic 预热及 student 决策预算，首轮各 2M，一臂正常开局，一臂只改自然 reset 分布。前缀重算/teacher 采集计算与决策成本另列并共享或明确计入，不能给课程臂隐形额外预算。当前训练 sampler 尚未接入，不能拿本轮诊断状态直接启动这个实验。GPU 确认结果回来后再绑定来源与新实验配置。
4. **同时监控 Act1 保留、联合通关和真实后段暴露。** 每 0.25M 检查正常开局 Act2 到达、独立同状态战斗、选牌循环、Act2 精英/Boss 实际进入资源；明显退化时停止扩预算而保留 checkpoint。最终比较相同独立开发 seeds 的配对成功与到达、置信区间、多训练 seed 复现。开发早停阈值先注册；建议 Act1 到达率相对参考损失超过 3 个百分点作为警戒，不能由小 batch 单次抖动直接判死刑。课程局部完成更好而正常开局下降，不算任务提升。
5. **成功经验利用作为独立后续干预。** 先做有去重、完整记忆前缀和资源分层的 recurrent BC/辅助模仿资格检查，再单独对照；不能把历史 teacher 动作当 on-policy PPO 样本。若课程没有收益，再依据梯度/记忆诊断单独比较 actor 保留 KL、critic 与 actor 梯度隔离、burn-in 或优势归一化方案，避免一次改多个变量失去解释。
6. **完整 A20H 另设环境资格门槛。** 80b 仅有有限 Act1–2 资格，本次没有验证 Act3/Heart。原工作区晚幕修复保持原状，尚不能被本轮 Act1–2 测试“认证”。双 Boss、Awakened/Rebirth、Donu/Deca、Heart、key/Act4 及奖励/跨幕随机时钟应逐项做 stock 原版差分和自然轨迹回放，在独立发布后重新采集、迁移权重、重新评估。即使两幕课程有效，也不能直接宣称 A20H 路线已完成。

这一路线借鉴 [Go-Explore 的返回已知状态后探索](https://www.nature.com/articles/s41586-020-03157-9) 和 [反向课程](https://arxiv.org/abs/1707.05300) 的经验供给思想；这是研究假设，不是这些论文已经证明 SLS 会奏效。这里坚持自然可达状态、公开历史、版本隔离和正常开局保留，不能把任意强牌组注入当最终任务。

## 本地验收与交付

隔离研发分支：`codex/critic20m-evidence-followup`，基于已验收 CPU 分支。只增加诊断/研究工具和文档；native、生产网络、奖励、PPO、训练 CLI、配置、旧 registered operators、恢复契约及 AGENTS.md 未改变。原 `D:/SLS` 的未提交工作保留。没有本地长训、GPU 或 NUS 执行。

CPU 全量测试 **1376 passed、2 skipped**；跳过为旧编码历史模型正确拒绝、CUDA 禁用。gzip 审计补强后另有 15 项针对性测试通过，Ruff、词表通过。复用此前隔离构建且源摘要不变，576 次实际 native 恢复通过。配置检查仍为 34/35，唯一拒绝是之前 CPU 优化触发的 critic20M 源码绑定；没有绕过。四模型 CPU smoke 每个 2 个研究 seed 已通过，不用它报告胜率。身份和日志 SHA256 见 [validation.json](validation.json)。

原始比较轨迹、全量测试日志、新自然状态库、回报和版本证据放在本地交付包，包身份见 delivery.json；不把原训练 checkpoint/权重复制进 GitHub。新采集 24 个状态的覆盖缺口在 [natural-corpus-manifest.json](natural-corpus-manifest.json) 如实记录。GPU 任务尚未执行；下一步所需人工操作见 [NUS 操作说明](NUS.md)。
