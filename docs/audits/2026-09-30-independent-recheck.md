# 2026-09-30 独立复核：源码、产物与审计后修改

起点 HEAD `4b4028c`，工作区已存在一批 09-29 审计后未提交改动；本次分别检查 HEAD 的服务器
执行行为与当前工作区，不把文档当证明。未修改 `AGENTS.md`，其进入本次前的修改不纳入提交。
主要可复算数字在[JSON](2026-09-30-independent-recheck.json)，本轮新产物在
[筛查结案](../results/plateau-reward-screen-20260930/README.md)。本次不运行 NUS 训练。

## 已独立确认

- 56M 的历史终评 1,585/2,048、当前规则的 1,569/2,048，以及相同 seed 的 lost=49、gained=33、
  McNemar p=0.09703，可以直接从原始结果复算。不能将不显著解释为规则完全等效。
- Reward objective 确实错配：Progress failure 是 `−1 + .8·floor/16`，Win ±1 且 gamma=1
  才直接对应 clear probability。源码 `reward.py` 的 PBRS 是 `γΦ(next)−Φ(current)` 且
  `Φ(terminal)=0`；同起点的整局总和为常数。奖励单测与终局/reset 单测通过。
- GAE `rollout.py` 的 terminal mask、bootstrap、returns=advantage+values 正确；原始 returns
  用于 value loss，按 combat/run/choice 的 normalization 只作用于 policy loss。
  `ppo.py` 中 previous reward 输入使用 backend reward，两种训练目标均不把终局塑形奖励
  泄漏到下一局 memory；reset 置零。
- λ=.98 的直接终局 residual 在 170 决策前的权重约 .03224、半衰期 34.31 决策，算术正确。
  这不说明 critic 的 TD bootstrap 无法传递长程信息，也不证明 credit assignment 已坏。
- 两组 2M 的 KL 小、少量 ratio clip、无 early stop、频繁 norm clipping 与吞吐约 98/s 是实测。
  `target_kl` 是安全上限，不是必须达到的更新目标；不能仅因 KL 是阈值六分之一就增加学习率。
- Neow 512 个四臂原始数据和 grouped-CV 探针重算：常量 slot1=374，logistic=356、boosting=359，
  foresight oracle=479。这反对“oracle 差距就是可学收益”，不能证明所有条件策略都不可能更好。
- 卡牌奖励身份在 COMBAT_REWARD 已由 `environment.py:999+` 编成 reward entity；Oracle
  `CardStatePatch.java` 会注入未点击的卡牌信息。当前未 mask，属于待处理观测语义问题。
- native `GameContext.cpp:addPotionRewards` 注明假设怪物没有逃跑，没有 Java
  `AbstractRoom.java:588` 的逃跑检查。静态差异仍在；影响多少局/胜率尚未用匹配原版执行量化。

## 必须修正的审计结论

1. **“开发 seed 块显著更难，p=.0014”不成立。** 375/512 与 1569/2048 没有共享 seed。
   合法的独立块比较为 pooled z=−1.595、Pearson p=.11068、Fisher p=.11858。
   当前只观察到 3.37pp 点估计差异；没有建立 RNG seed 映射异常或系统难度差异。
   README 及结果说明已纠正。历史报告保留但添加了明确修正入口。
2. **“Weak/Strength/PenNib 按获得顺序导致 native 伤害错误”被源码投影反驳。**
   `ApplyPowerAction.java:160–161` 添加后 `Collections.sort(target.powers)`；
   `AbstractPower.java:57,357–358` 默认 priority=5，按 priority 比较；Strength 未覆盖；
   PenNib priority=6，Weak priority=99。`AbstractCard.java:2221` 确实遍历列表，但该列表已排序。
   native Strength → PenNib → Weak 与这些 priority 一致，且原版在完整计算尾部 floor，
   不是每个 power 后都取整。原审计举出的两种顺序差异不成立，不能据此改 native。
   本机缺 JAR，只验证了归档反编译投影与其文件哈希；没有声称本次执行过原版字节码。
3. **“Neow 常量就是当前信息下最优、无条件化 headroom”过强。** 两个有限数据拟合器的失败
   不是所有策略的上界；没有包含完整可见地图等特征，还固定了后续策略。最多说明当前证据
   不支持优先改 Neow 表征。工具输出已去掉“no policy can reach”这类不成立断言。
4. **“约 8pp 是未开发 headroom”应改为假想策略的回报无差异。** 实测 Progress return=.706445，
   若失败在 floor0，无差异胜率=.853223；若真死在 floor1，reward=−.95，胜率=.849459。
   这些不是已知可实现策略，不能保证换 Win 就能提高 8pp。
5. **“全部 GPU 利用率只有 6.5%、编码下沉可达 3×”无证据。** 6.5% 是 policy forward 时间占比，
   optimizer 也在 GPU；编码占比给出的消除该部分理想上限约 1.37×。工程性能目标需 benchmark。
6. **“ACT1 progress guard 空转”部分正确，“加 failure median veto 就解决”错误。**
   原 act-reach gate 在 ACT1 确实不能拦截，但 runtime exception 本来会终止 evaluation，
   `backend_errors=0` 不等于异常被当胜利。失败群体是策略选择后的条件子集；多救深层失败会使
   剩余失败 median 变浅，不能否决更高胜率。当前 v5 clear-count selection 使用 health 优先、
   clear count 次之，等 clear count 保留早者，不引入 depth/速度/Boss 的额外目标。

## 审计后修改的独立检查

| 修改 | 验证与限制 |
| --- | --- |
| 配置摘要 LF 规范化 | 单测核对 LF/CRLF，相同 LF 与旧 byte hash 一致；continuation 的配置摘要本次也统一 |
| final evaluation runtime | 新 v3 写入实际 native/runtime；旧 v2 原件保留，不回填未知字段 |
| self-loop / timeout | 源码区分可见无变化与重复边界终止，新增边界路径单测通过；错误仍抛异常 |
| advantage domain diagnostics | 统计原始 mean/std 和真实 scaling；不改变策略 loss 的 normalization |
| PPO epoch diagnostics | 两个 epoch 用同一预算，去掉一次全量 diagnostic；采样改为 time-major stride，可能改变 early-stop 判定，因此是实现行为变化，不只是日志变化；固定 stride 也不是随机无偏样本 |
| selection guard | 原新失败 median veto 被本次改为 clear-count health-only；BEST schema v4→v5，旧元数据只读兼容；新实验重新建立 selection |
| continuation | 完整 endpoint 需 COMPLETE manifest + bundle 哈希 + checkpoint identity；实现变化必须旧→新 exact digest 配对授权，不能任意恢复旧实验 |
| single-stage final export | 发现旧终评只检查 backend error/truncation，遗漏 step/cycle/timeout；本次统一健康门禁并加回归，避免有人工限额失败时仍宣称可晋升 |
| CI / configs | 新增配置与词表检查；本次不声称已在 GitHub 跑完 CI 或 ASan |

同一个 56M checkpoint 的本地真实模型 probe：8 workers×256，生产 model，Win .98，
minibatch_sequences 因 8GB GPU 缩至 2；同一 rollout 复算 .995/1.0，不对两者作训练效果比较。
更新所有数值有限，初始 checkpoint 重载后 actions/weights/metrics 完全重放。
只有 8 个从头到 terminal 的完整训练 episode，无法做强 critic 结论；且 Progress 训练的 critic
首次换成 Win target，因此 start MC calibration 还混入 reward 迁移，不解释为网络结构缺陷。
在 .98 下 raw advantage std combat/run/choice≈.321/.199/.206，域缩放≈3.11/5.02/4.86，
没有发现方差近零导致的爆炸归一化。较大 λ 在同轨迹上增加 residual 方差符合预期，但不证明
胜率会提高。probe 的小 minibatch 导致 KL early-stop，不能拿它推断生产 64-worker 更新强度。
原始完整 probe、runtime、脚本与 log 均在 `local/reports/plateau-ab-20260930/`。

## 当前瓶颈与行动排序

尚未有证据定位一个确定的算法缺陷。能确认的是：原目标与胜率不完全一致；Win 只有一个
123-update 短筛查，峰值不稳定；固定小开发集和单训练 seed 不能决定配方效果；每个训练决策
约 .01s 让探索预算有限。模拟器/观测仍有未量化差异，限制最终实机结论。

1. 直接固定正确 Win objective，**单作业**从完整 58M endpoint 保留 Adam/state 到 70M；
   这是预算假设，不能保证提升。不再开 Progress 对照，终点和所选模型都与冻结 champion 配对。
2. 若 70M 不能提供改善，停止单纯堆步数；优先以本轮新域统计/critic 日志选择一个 λ 或归一化
   变量。λ 需要 bias/variance 证据，归一化需要实际域 scaling 病态证据，不同时修改。
3. 训练吞吐优化可作为独立工程分支，必须保持采样/logprob/updates/结果可复现后 benchmark；
   不把它与核心 PPO 消融混合。
4. Neow 表征和困难状态训练暂不抢先：前者无确认可学收益，后者会改 start distribution，不能
   用困难状态胜率代替正常开局全局胜率。critic 改架构也缺诊断证据。
5. 逃跑药水、预领取奖励信息应做 stock 探针后单独修复，绑定 native/observation 版本、迁移和
   同种子重测；不要在本次 Win 70M 作业中途引入 simulator semantics 变化。

## 本地验证记录

进入本次时全量测试 938 passed、1 skipped；本次增改后的完整结果与静态检查记录于
[执行说明](../win-70m-launch.md)。新 endpoint continuation 回归对 model、optimizer、trainer、
python/torch/cuda RNG、environments 全字段逐项比对，损坏 bundle 被拒绝。
真实下载的 64-worker endpoint 也通过同样完整状态比对：next_seed=120012081，update=123，
Adam step=3936，源文件未改。当前 native source 与构建嵌入身份一致；词表仍为 v5。

原版十条轨迹的 1,748 边界应解释为**有限轨迹语料的可观测字段一致**，不是内部 RNG/隐藏状态
全面等价，更不是所有组合规则证明。本次校验保存的原始轨迹哈希；缺 JAR，因此没有重新
执行 stock bytecode 或声称新增原版资格认证。Act2/3/Heart 尚无本次训练成功证据。
