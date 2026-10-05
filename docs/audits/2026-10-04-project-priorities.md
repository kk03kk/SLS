# 2026-10-04 项目问题与优化优先级

后续复核已定位原版 JAR 并执行 `javap`。确认并修复 A17+ Snecko Tail Whip
的 Weak/Vulnerable 顺序差异；详见[资格证据](../results/act12-qualification-20261004/README.md)。
下文最初的“未执行原版对照”指没有启动原版游戏进行完整轨迹对照；目前已有该条规则的
原版字节码与 native 前后运行证据，仍不等于完整 Act 2 parity。

最终优先级维持：先确保跨幕规则可信、开展正常开局两幕学习，同时降低迭代成本。
不优先扩大网络或重新比较 Progress/Win。90M 结果尚未取得，不能判断其当前泛化胜率。
新增的 32-decision 权重迁移探针存在大 KL/高裁剪，但其 16-step 小批量刻意用于实现检查，
既没有终局样本，也不能代替服务器正式 batch 的优化诊断。

下一阶段应区分三个失败来源：入幕牌组成长不足、第二幕战斗执行不足、以及新的终局目标
没有形成可靠 advantage。分别记录入幕状态、遭遇类型/死亡楼层和 critic 的 MC 校准；
不能用同一条 aggregate reward 曲线决定修哪个。若新增约 2M 的正常开局试跑缺少成功信号，
根据诊断仅选择一种补救：真实状态后缀课程、critic head 适应或探索调整。
单训练 seed 的短试跑只回答学习能否起步，不证明跨训练 seeds 的稳健提升。

对当前 HEAD `5435d4a` 的补充复核，承接 [Act 1–2 证据](2026-10-04-act12-readiness.md)。
本次只增加审计记录，未修改运行中的 90M 训练。工程缺口、语义风险、统计限制与算法假设
分别描述，不将没有验证的优化建议当作故障。

## 最终判断

项目具备继续做跨幕训练的基础：版本化输入、结构化 action、循环记忆、原始产物、完整
checkpoint 身份、Win objective 与健康门禁已经存在。没有发现必须先推倒网络/PPO 的证据。
最值得投入的是正常开局的两幕训练、第二幕保真度资格，以及降低每次研究迭代的成本。
算法方面，首先检查 critic 对新目标的适应和关键决策探索，再考虑 λ/归一化/网络改变。

| 优先级 | 工作 | 证据状态 | 决策 |
| --- | --- | --- | --- |
| P0 | Act 1–2 数据与目标迁移 | 70M 冻结模型 26/32 入幕、0/32 通关；Act 1 未训练通关后奖励选择 | 90M 结案后开展短训练试跑，不等待任意 Act 1 胜率阈值 |
| P0 | Act 2 stock qualification | 存在晚幕回归，但本次没有新的原版可执行对照 | 扩长训前验证高影响敌人/事件/奖励与跨幕流程 |
| P1 | 编码/transition/IPC/优化吞吐 | NUS 原始计时显示非模型前向开销明显 | 先 profiling，再做可证明语义一致的优化；当前作业不换实现 |
| P1 | critic 校准、梯度来源、关键决策探索 | 新 horizon 改变 target；已有总梯度裁剪高频，但没有分来源证据 | 增加诊断，病因明确后只改一个变量 |
| P2 | recurrent prefix 一致性、BPTT 长度 | 片段从旧 rollout memory 开始、长度 64，无 burn-in | 测量新旧参数的 memory/logprob 偏差，不直接当 bug |
| P2 | λ、advantage normalization | 未发现 terminal/bootstrap 错误和近零方差爆炸 | 放在目标与 critic 诊断之后 |
| P3 | 大网络、actor 重置、复杂新算法、搜索 | 尚无容量不足/可塑性损失/搜索净收益证据 | 当前不优先投入 |

## 模拟器：资格缺口与已修复问题必须分开

核心 Smoke Bomb 合法性限制已经修复。`native/simulator/src/sim/search/Action.cpp` 会调用
`canUseSmokeBomb()`；后者拒绝 Boss 与 Surrounded/BackAttack。实际 native 探针验证普通战
可以逃跑、十种 Boss 均禁止、Back Attack 禁止、逃跑不调用战胜奖励回调。
不能继续引用旧文档把此项说成未修复。原始结果见
`docs/results/act12-readiness-20261004/native-core-recheck.json`。
新增运行 `python -m pytest tests/simulator/test_native_mechanisms.py -q`：34 passed，2.54s。
这并不证明逃跑所有 RNG/多阶段交互已经与 stock 全面等价。

另外，`environment.py` 的 COMBAT_REWARD observation 会枚举卡牌奖励内容；原版玩家此时
能看到什么、能否先查看再返回并重排领取顺序，需要按实际 UI 可达操作逐项比对。
源码确认了观测时点，尚未量化该差异对策略/胜率的影响，不能直接称未来 RNG 泄漏或作弊。

进入第二幕前，应检查 Snecko 混乱成本、Chosen Hex/状态牌、Spheric Guardian、Slavers /
Gremlin Leader / Book of Stabbing、三个 Boss 多阶段机制，以及血量恢复、能量/Boss 遗物、
卡牌预览/领取回调和事件。这是基于任务风险的资格清单，不是宣称这些项目都有 bug。
资格测试需同时比较公开状态、合法动作、回调/RNG 消耗和终局；只比较最终血量不足以证明
全流程保真。将修复单独版本化，固定模型做前后配对，避免把模拟器变更收益算成训练收益。

## 网络：现有结构合理，值得检查的是用途与训练信号

`model/transformer.py` 当前使用实体/内容/数值编码、地图与 power-owner 关系、4 层
Transformer、256 维 GRU、三组 action scoring heads、一个共享 value head。
`batching.py` 动态保留 entities/actions 并 padding，没有发现固定最大长度导致的静默截断。
这已比固定 action 编号、忽略构筑上下文的简单 MLP 更符合任务。现有表现不能证明需要扩容。

风险与证据界限：

- Act 2 专属内容虽在词表/模拟器中，embedding 有定义不等于训练过；第一幕策略不能保证
  已学会第二幕怪物、成长性牌组和 Boss 遗物价值。
- actor/critic 共享 backbone，联合 loss 对全部参数统一 gradient clipping。70M 总 norm
  clipping 频率 91.45%、裁剪前均值约 .804，阈值 .5；不知道 actor/value 各自产生多少梯度、
  是否冲突。value coefficient 或 max_norm 不能仅凭裁剪频率决定。
- explained variance≈.57 是对 GAE target 的解释率，不是原始通关概率校准。需使用与当前
  policy 对应的 MC outcome/return，按幕/楼层/决策域分层，且反演或计入 PBRS 后再比较。
- BPTT 为 64 decisions。循环状态可跨片段携带，但梯度不跨全部历史；“GRU 永远只能记
  64 步”不成立。优化从 rollout 收集时的 input_memories 起步，更新参数后 prefix memory
  可能变旧；这是近似风险而非已证实错误。先测重算 prefix 对 logprob/value 的改变，再
  决定 burn-in 或长度实验。
- 不同 horizon 的 Win target 不能无条件混合；当前 observation 没有目标 horizon 字段。
  如果未来要多任务训练，需显式 goal conditioning/目标契约或适当 value 分离，版本化迁移。

可塑性损失值得观测，但不能以累计训练步数认定模型失去学习能力。
[C-CHAIN，ICML 2025](https://proceedings.mlr.press/v267/tang25g.html)研究输出 churn 与
可塑性；[Plastic PPO，ICML 2025](https://proceedings.mlr.press/v267/zhou25am.html)将重置
与知识恢复结合。它们不证明 SLS 应重置 actor；适应新任务的学习曲线与特征诊断需先出现。

## PPO 与策略：正确目标不等于自动得到好学习信号

Win ±1、gamma=1 是当前目标的合理选择，没必要重开 Progress 对照。PBRS 整局总和的
不变性不意味着有限 rollout、近似 critic 与标准化之后的训练轨迹相同。
λ=.98 可降低方差但增加依赖 value 的程度，不能用 residual 衰减公式直接推出失败。
三域 normalization 实际改变域间缩放；70M 的平均 scale 约 2.99/3.51/3.51，未见爆炸。
下一阶段数据分布变化后再测，不能把“稀少选择”简单替换成更高 loss 权重。

70M 的 final KL≈.00358、ratio clip≈3.51%、没有 early-stop，与“PPO 更新很猛烈”不符；
但也不足以证明 LR 太小，因为有效步长受 clipping、Adam、目标噪声与探索共同影响。
不要一次性增加 LR、epoch、λ、entropy，以免无法归因。

策略应围绕正常开局联合胜率学习：选牌/跳牌、路线、升级/休息、药水、Boss 遗物和战斗
共同塑造入幕分布。26 局入幕只有 2 局到第二幕 Boss，不能据此优先做 Boss-only 课程。
Neow slot1 集中不是表征故障的证明；此前的完美预知 oracle 不是可实现条件策略。
关键选择需记录可用候选、概率/熵与后续结果，不用人工高手规则替代学习。

若正常开局小试跑没有足够成功轨迹，再考虑真实状态后缀课程；重新采样当前策略、保持
状态/记忆/随机数身份，最后用正常开局评价。辅助训练分布的成功不能用于宣称最终目标改善。

## 吞吐：最有直接工程证据的改进机会

70M 的 732 次更新、约 12M decisions，训练更新吞吐约 102.77 decisions/s。
服务器原始计时：编码约 27.94%、Python transition 23.46%、policy forward 阶段约 6.99%、
optimizer 27.03%；计时项有不同层级，不可简单相加，也不能推出 GPU utilization=7%。

当前源码每个采样步逐 Decision 编码，重新分配与 padding 多个张量，传递 Python 对象，
等待同步 shard response。更新时一次编码片段后又逐时步 GRU/action head 计算；
`torch.any` 的 Python 分支在 CUDA 下是潜在同步点。源码提示优化机会，不等于已测得热点。
更长牌组与大实体 observation 还会提高 dense padding/attention 成本。

建议首先拆分 encode、batching、IPC serialization/wait、native、CPU/GPU forward、backward
与 optimizer；CUDA 用正确的事件/同步计时，不依赖未同步 perf_counter 推 GPU 耗时。
按顺序考虑静态数值内容复用、vectorized 编码/批处理、减少分配/复制和消息量，最后才考虑
mixed precision/compile/改变 worker 数量。不得缓存会变化的费用、遗物计数、合法 action，
也不能跨 optimizer update 缓存带参数的 Transformer features。

已有代码将 H2D 按 dtype 合并、合并 D2H，并批量编码 recurrent minibatch；不能把这些
已做的优化再当作未实现收益。理想化地把编码耗时减半、其余不变，也只是约 16% 的总耗时
加速估计，并非数倍。工程试验的合理首个目标为端到端 ≥20% 提速，仍需 benchmark 验证。

[ICML 2025 并行采样研究](https://proceedings.mlr.press/v267/mayor25a.html)提醒 rollout 长度、
并行环境与训练遍数影响学习本身。换 worker 数/rollout/minibatch 不能当无语义成本的加速。
验收需固定状态比较 action candidates、tensor、logprob、returns/loss、更新后的参数与
checkpoint 重放；GPU kernel 若发生数值改变，明确身份与容差，并检查 greedy 分叉。

## 统计与研究效率

现在最大的证据限制是单训练 seed、开发集反复查看及尚无全面真实两幕 parity。
少量 seed 看明显健康问题/跨幕学习是否起步，大开发集用于冻结配方的确认，最终保留集
一次性评估；三者分工不同。环境 seeds 控制地图/奖励，不等于独立训练 seeds。
日常不要为每个小 checkpoint 单独排队大型评估；把开发评估嵌在训练作业中。

优化看正常开局联合胜率和墙钟成本，不只看每秒步数、训练 reward、conditional Act2 胜率
或所选峰值。模拟器资格与吞吐工程可以在当前训练期间准备；核心算法改动等待诊断。
