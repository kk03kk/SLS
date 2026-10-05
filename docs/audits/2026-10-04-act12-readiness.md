# 2026-10-04：Act 1–2 迁移准备与独立研究复核

后续资格复核找到并修复 Snecko 的真实 debuff 顺序差异，原版 JAR 已找到且重新执行了
字节码检查；见[前后证据与迁移契约](../results/act12-qualification-20261004/README.md)。
本文 32-seed 诊断保留旧 native 身份，不改写成新环境结果。

结论：90M 本轮结束后应把研究重心转向正常 A20 开局、连续打通 Act 1–2。无需先把
Act 1 胜率磨到任意的 90%/95% 门槛；也不能直接复制 90M 配置、改终止幕数并投一个长作业。
先做跨幕资格验证与小规模训练试跑，再依学习信号决定是否需要状态课程。当前 90M 作业不变。

审计源码基点为 `5435d4a2f423a1e3b445977a48fae41ac90b5837`；本报告没有修改训练、
observation/action/reward、模型或 native 语义。测试与模型诊断于 2026-10-03 本地完成，
文献检索覆盖 2025–2026 年、包括 2026-09 的预印本；文献结论不视为 SLS 已验证结果。

## 独立运行证据

原始本地记录：`local/reports/act12-audit-20261003/`。本报告的持久证据副本在
`docs/results/act12-readiness-20261004/`，包括真实模型评估、边界探针与测试输出。

- `tests/test_a20_curriculum.py`、`tests/test_curriculum.py`、
  `tests/simulator/test_fullrun_structure.py`、`tests/simulator/test_a20_late_act_parity.py`、
  `tests/rl/test_single_act_selection.py`：**57 passed，26.46s**。
- Act 1/Act 2 各用 seed 0、3、4 做跳过战斗的边界探针。Act 1 在击败第一幕 Boss 后的
  COMBAT_REWARD 终止，尚未选择 Boss 卡牌和 Boss 遗物。Act 2 允许这些决策并继续第二幕，
  在击败第二幕 Boss 后终止。这只证明流程边界，不估计胜率。
- 冻结真实 **70M final.pt**，SHA256
  `cb53fee1ae3f47cc906665c903068dabaf38a328be1e07697e6d17c5b21d7d5c`，
  不更新权重、正常开局、不跳战斗，在新开发种子区间
  `[8000005000000, 8000005000032)` 做贪心 Act 1–2 诊断。
- **26/32 进入 Act 2，0/32 打通 Act 2**；共 7,295 decisions。只有两局到达第二幕 Boss，
  均为 Automaton、均失败。失败楼层中位数 22；多数第二幕失败发生在 Boss 之前。
  backend error/truncation、timeout、step/cycle limit 均为 0。
- 0/32 的二项 Wilson 95% 区间约为 **[0, 10.72%]**。不能称真实胜率为零，不能代表正在
  训练的 90M 模型，也不能证明 Act 2 不可学习。此为 Windows CUDA 开发诊断，不与 NUS
  不同运行时的历史百分比直接作显著性比较。原始 JSON 保存 native/runtime 身份与逐 seed 结果。

这使“跨幕学习有必要”的证据比“继续针对第一幕漂亮 checkpoint 微调”的证据更直接。
但不能据此认定 critic、λ 或某个敌人实现就是失败根因。

## 当前源码确认与未确认事项

| 主题 | 已确认事实 | 含义与限制 |
| --- | --- | --- |
| 任务语义 | `curriculum.py` 的 A20_ACT2 从正常 Neow 开始，目标为连续前两幕；`environment.py` 使用 native completed_act 判断完成 | 不是单独第二幕开局；此前第一幕训练未经历通关后的卡牌/遗物选择 |
| 迁移 | `rl/act1_transfer.py` 已支持立即前一 A20 profile 的 `curriculum-stage` 转移；SHA/profile/steps 校验，复制所有模型权重 | 不必新写一种迁移；包括 value head，重置 Adam/RNG/workers/memory/update/best，累计 steps 继承。不能称 exact resume |
| Reward | failure_progress_scale=0、gamma=1 的 Win ±1 与该 horizon 的通关概率对齐；PBRS 为势能差且终局势能为零 | Act 2 把失败/成功目标和势能楼层分母改变为新任务；不能在 Act 1 结束额外给一次胜利奖励 |
| Critic | 共享 backbone、单一 value head；转移保留该 head，而终局目标由 Act 1 改为 Act 1–2 | 存在目标失配风险；尚未量化新任务 MC calibration，不能声称保留 critic 已造成性能损害 |
| GAE | terminal mask/bootstrap 与 returns 计算未发现本次新故障；λ=.98 的直接 TD residual 权重半衰期约 34 decisions | `.98^170≈.032` 不是信用传播完全截止；value bootstrap 仍传递未来。不能仅凭长度立即换 λ=1 |
| Normalization | 按 combat/run/choice 分域，而非按幕分域 | 第二幕改变数据分布与相对缩放；应先记录 act×domain 原始 advantage/scale，再决定修改 |
| 熵时钟 | PPO 用累计 environment_steps；90M 配置熵系数恒为 .002 | 新任务复制配置不会自然恢复探索；需显式决定新阶段时钟与系数，不随手重置全部训练变量 |
| 状态输入 | policy-input-v5 包含当前 act/floor，但不包含目标 horizon；跨幕非终局保留 recurrent memory | 在同一策略里混合 Act 1 与 Act 1–2 的不同终局奖励，会使共享 critic 面对未显式标识的目标；不能把简单混合当无代价方案 |
| Checkpoint | single-stage 非 Act 1 使用 HORIZON_CLEAR_COUNT，health-first、按 clear count 选择 | 已能按真正新目标选择；无需恢复 failure floor/median veto 或用训练 reward 挑模型 |
| Simulator | 现有晚幕回归覆盖若干 stock 源码推导的行为与 checkpoint 重放 | 测试通过不是完整原版 parity。本次无可执行原版 JAR 对照，不声称第二幕已获得 stock 资格认证 |
| Neow | 近期公开特征拟合未证明额外可泛化收益；完美预知 oracle 差距不等于可学收益 | 新目标会改变 Neow 的价值，应重新记录，但当前不支持优先添加人工 Neow 规则或改表征 |

第一幕成功策略未必是完整两幕的最优策略：小伤害、低成长卡牌可能提高第一幕生存，却降低
第二幕存活；Boss 遗物、能量与牌组成长决策也需要新训练。主要指标应是
`P(正常开局打通 Act 2)`。`P(到达 Act 2)` 和 `P(通关 | 到达 Act 2)` 是分解诊断，后者
的入幕状态分布随模型改变，不能当作不变人群上的因果比较。第一幕胜率略降也不自动否决
两幕联合胜率真实改善。

## 新研究怎样影响判断

1. [h1，ICML 2026](https://proceedings.mlr.press/v306/ivanova26a.html)
   展示 outcome-only reward 配合递增 horizon 的课程可以改善长程任务学习。
   [全文](https://arxiv.org/html/2510.07312v2)的样本复杂度结果依赖简化任务与参数分解假设。
   对 SLS 的启发是扩展 horizon，不是等待第一幕无限收敛；论文没有给 SLS 晋级胜率阈值，
   也不保证游戏跨幕无遗忘。
2. [PATH，ICML 2026](https://proceedings.mlr.press/v306/liu26j.html)
   先扩展课程路径覆盖，再向尚未掌握区域分配训练。
   [全文](https://arxiv.org/html/2608.26469v1)包含 MiniGrid/BipedalWalker 等实验。
   启发是依据失败状态分配数据，而非机械提高幕数或只训练 Boss；尚未在 SLS 复现。
3. [DART，RLC/RLJ 2026 预发布论文页](https://rlj.cs.umass.edu/2026/papers/Paper74.html)
   提出 bootstrapped base critic 与 Monte Carlo residual critic 的组合，针对 value error
   传播造成的 advantage 偏差。本次只成功查阅官方摘要，未完成全文核验。
   支持优先测 critic 校准，不支持直接声称 SLS 应改双 critic；理论限定在线性函数近似等条件。
4. [GACA，2026-09-11 预印本](https://arxiv.org/abs/2609.12424)
   在 ALFWorld/WebShop 的 LLM agent 上用状态分组与不确定性调整信用分配。
   [全文](https://arxiv.org/html/2609.12424v1)不提供 SLS recurrent PPO 的结论；相同公开观察
   可能有不同隐藏状态与记忆，动作 NLL 也不直接等于游戏中的决策重要性。暂不照搬。
5. [On-policy parallelized collection，ICML 2025](https://proceedings.mlr.press/v267/mayor25a.html)
   将环境并行数、rollout 长度、更新遍数联系到偏差/方差与训练稳定性。
   对 SLS，吞吐优化应先测 simulator/encoding/worker/optimizer 的耗时，保持更新强度清楚；
   增加并行数不是零代价、也不保证同预算胜率更高。

这些研究支持诊断与课程的方向，不构成已经验证的最佳配方。当前没有证据要求换 GRPO、
删除 critic、重置 actor 或增大网络。

## 建议的下一步及停止条件

1. **完成 90M，并按已登记规则结案。** 保留 endpoint、开发集选中模型、完整状态与原始证据。
   据该轮开发结果冻结一个 Act 1 迁移父模型，不为了新任务反复挖掘旧评估集。90M 结果尚未知。
2. **Act 2 资格与基线。** 固定父模型后，用新开发集作端到端零训练基线，记录幕间
   HP/牌组/遗物/药水、Boss 卡牌与遗物选择，以及 Act 2 各类遭遇。
   用 stock trace 重点验证跨幕流程和第二幕高影响规则：Spheric Guardian、Snecko、Chosen、
   Book of Stabbing、Gremlin Leader、Slavers、Collector、Champ、Automaton。
   预领取奖励信息与逃跑药水等既有未量化差异单独查证，不在当前长作业中途改 native。
   补充复核已确认当前核心 Smoke Bomb Boss/BackAttack 限制已修复；不再将此限制列为未修复，
   见[项目优化复核](2026-10-04-project-priorities.md)。剩余资格指完整交互与 RNG 对照。
3. **先做新增约 2M decisions 的正常开局 Act 1–2 训练试跑，而非直接长训 20M。**
   主假设：正常开局数据、现有 recurrent PPO 与 Win objective 足以开始形成第二幕成功信号。
   control 为冻结父模型在新 horizon 的表现；变量为跨幕目标与对应新阶段权重迁移。
   使用一个明确记录的新 training seed、单独 output、`curriculum-stage` 身份；
   默认保留 actor/critic 权重，使用 fresh Adam 等既定迁移行为，保持 λ、normalization、
   架构不变。探索系数先从现有 .002 基线开始，并记录新决策 action entropy，
   不同时加一套高熵配方。实际累计 target=父模型真实 steps+新增预算。
4. **评估作为短试跑中的观测，不单独排一个大型评估作业。** 同一作业每约 0.5M 用固定
   256 新开发 seeds，试跑结束冻结 checkpoint 后用独立 1,024 新开发确认 seeds 与父模型配对。
   两个开发块、training seeds 必须登记互不重叠；本次 32 个 seeds 归为已暴露开发数据。
   最终保留集继续封存，待正式配方与 checkpoint 冻结后一次性打开。小试跑的单 training
   seed 仅判断可行性，不作普适配方因果结论，也不对每个 checkpoint 连续显著性检验。
5. **判据。** 试跑值得扩预算，需要训练中真实第二幕通关开始稳定出现、开发曲线提供重复
   改善信号，健康错误为零；确认集报告 paired gain/loss、CI 与胜率。若仍几乎没有成功或
   多次开发点无改善，就不盲目加到更长累计步数。先区分低探索、critic 目标错配与入幕状态
   不可生存，选择一个补救变量。一次短试跑不宣称最终泛化提升已成立。
6. **必要时才引入自然状态后缀课程。** 状态必须来自真实正常开局，保留可重放 native/RNG
   与必要的 recurrent 历史；从恢复状态重新采样新策略轨迹，不能把旧策略轨迹直接当 PPO
   on-policy 数据。不得增加血量、伪造好牌组或用跳战斗状态宣称正常局提升。
   状态库随第一幕策略更新，辅助分布比例单独实验；最终判断始终回到正常开局两幕通关。

critic 只在 MC calibration 显示确实失配时考虑 head-only 重拟合；不得默认把 actor/shared
features 一起重置。λ 或 normalization 消融排在对应诊断之后；吞吐工程可先做不改变学习
契约的 profiling，但其有效性需要实际 benchmark。现有小样本不支持立即投入复杂算法移植。

当前最强结论是任务覆盖不足与新终局目标迁移风险，而不是某个 PPO 理论缺陷已经被定位。
等待 90M 原始结果是为了选定真实父模型；本报告不产生伪造训练结果或新的服务器长训命令。
