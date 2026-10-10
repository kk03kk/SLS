# Act2 学习瓶颈：本地研究与首轮实验资格

日期：2026-10-10。研发分支：`codex/act2-learning-research`。基线：`ce7713673629f45381da5b195f2fab8dde833505`。

## 结论与边界

当前最值得检验的是：**在保留正常开局学习的前提下，增加自然可达的 Act2 经验，能否提高两幕联合完成能力，并保住 Act1？** 这仍是假设，不是已证实的解法。本轮完成诊断工具、自然状态初始化、严格研究 checkpoint、两臂执行器和本地资格验证；没有启动正式训练，也没有合并 main 或更新历史训练发布。

历史 Critic20M 结果不支持继续沿用“降低 critic 误差就能解决 Act2”的简单判断。4096 个相同开发种子上，父模型到达 Act2 为 2953/4096，best 为 2616，94M 为 2148，latest 为 1938；两幕成功分别为 24、20、17、19。latest 相对父模型的到达数净损失 1015，而联合成功 lost=24/gained=19，配对检验 p≈0.542。循环减少并没有转化为正常开局能力改善。这是已核验历史开发集的结果，不是本轮诊断集的胜率。

选择性 CPU 诊断显示 critic 在失败轨迹上的实际 shaped-return 误差明显缩小，但 actor 也发生了显著变化。它们共同解释“价值拟合看起来改善，而通关不改善”的表象，尚不能确证因果。父模型原训练目标是 Act1，诊断是 Act1–2 greedy；目标、策略和 horizon 不同，不能把 value 当成功概率，也不能据这些样本证明 critic 已正确校准。

## 版本和输入身份

| 对象 | 身份 |
|---|---|
| native 源码 | `6efbb772958c06d1b9133f374d046d8838eaa256da1a146eaf7e1ee530eefd3d` |
| 本地独立构建 DLL | `1a3c7e34d35a560944dc65e6e9bbc556989f5f1610398c8d372d2100493f0439` |
| 冻结 90M 父模型 | `274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0` |
| critic best | `a804334af0060351a5e83a946c37a42a1a30445d809b21de3a220f2617e82870` |
| critic 94M | `70103ae500ebf154360fbb6ec16f897f300cebcb16ba6b8244082d946c45def1` |
| critic latest | `be05e515fc892e1b09a2d8a87e30cac430e2f295c7ce820f3f91c7f0a838693d` |

本地 Python 为 DL/3.12.12，Torch 2.10.0+cu130，但所有本地命令明确设置 `CUDA_VISIBLE_DEVICES=-1`、使用 CPU。NUS 历史评估为 Torch 2.6/CUDA 12.4；本地重采集不是服务器原轨迹。

本轮沿用 Act1–2 发布环境，没有移植另一工作树的晚幕规则修复，不声称具备完整 A20H 模拟器资格。native 在独立工作树构建；原 `D:\SLS` 的 197 个待提交文件和 Git 状态均按字节核验未变。

## 机制证据

| 假设 | 当前支持证据 | 反证、限制与下一检查 |
|---|---|---|
| 后段经验不足 | 历史正常开局只有 279–348 次真实 Act2 Boss 暴露；联合成功极少 | 曝露少不等于唯一瓶颈。独立 2048 seed 的自然库先检查可达性和覆盖，不按未来成功筛选；两臂测试分布干预 |
| 前段能力流失 | 4096 配对集 Act2 到达显著下降。诊断相同公开历史上有动作分布和选择变化 | 最早动作分叉可能只是重复牌的等价实例。不能把每次不同 argmax 都叫作错误；需结合条件后续和正常开局配对指标 |
| 共享表示/记忆漂移 | 完整公开前缀重建工具已落地；一次小 CPU 更新后保存与重建 memory 有可测差异 | 前缀仅四个早期决策，策略 KL 很小，无法说明后段漂移严重。actor/value 梯度、共享夹角、合并裁剪和长前缀漂移在正式短实验中持续记录 |
| 选择语义/规则 | 八条自然循环均在 floor17，已选数量 0↔1、同一卡牌选择与取消反复出现；公开 selected 状态存在 | 新读取原版字节码支持取消选择规则，既有动态对照/重复牌测试通过；未新做原版动态全程对照。未发现确定性模拟器错误，不把规则语义猜测当结论 |

初始 16 seed 按四类失败固定 SHA256 选择，四模型共 64 条完整自然轨迹。初始每 seed 四状态抽样覆盖不足，保留其报告并标为初始抽样；随后从同一批自然轨迹全局分层选 64 状态，优先保留 MASTER_DECK 已选数量/跨幕分支与六种 Boss，再固定 SHA256 填充其他幕、界面和敌人组合。没有注入状态或用未来成功标签补覆盖。

最终状态库、模型比较和完整行为回报分别在 `local/reports/act2-learning-global-diagnostics-r3/{manifest,comparison,returns}.json`。机器证据摘要见 [global-mechanism-evidence.json](global-mechanism-evidence.json)。完整历史和动作在 public；native checkpoint 独立在 private，仅供恢复 backend，不进入模型输入。每个模型对同一真实历史重建自己的循环记忆，后续上限 256 决策；上限标记 `diagnostic_step_limit`、complete=false，**不记作死亡**。

最终覆盖六种 Boss、MASTER_DECK 跨幕 selected0/selected1，以及自然出现的 HAND/GENERATED/DISCARD/EXHAUST 分支；未自然出现 DRAW 选择，不补注入。256 次后续中 220 次死亡、15 次既定循环终止、21 次诊断截断。62 个非强制状态上 best/94/latest 相对父模型 argmax 不同 9/15/18，平均 TV 为 0.152/0.226/0.251。26 个 Act2 战斗边界的四模型后续均为死亡、未完成两幕；后续还包含其他战斗和选择，不能等同于当前战斗输赢，也不能估计正式胜率。当前库未显示 critic 后续模型带来条件联合完成改善，但不能排除局部战斗收益或推断这批构筑无解。

原始历史保留采集时身份：采集期间未用于推理的研究训练模块有变化，因此存在七个当时的训练实现摘要。推理模型、编码、奖励、limiter、native 源码保持一致。最终全局重放与比较在干净提交 `5360e195cfeede5bff9cab3dce3d2caf0b824cea` 启动，拥有自己的身份；不能将早期采集身份改写成该提交。

本地和 NUS 记录有五处运行时差异，逐项保存在初始 [mechanism-evidence.json](mechanism-evidence.json)，其中两处改变了到达楼层。缺少服务器公开逐步历史，尚无法定位首个差异或归因于浮点计算。所有条件比较均基于实际本地状态，未冒充服务器状态。

初始抽样 58 个非强制决策状态，相对父模型，best/94/latest 的 argmax 不同数量为 14/22/23，平均动作概率 TV 为 0.221/0.370/0.401。完整失败轨迹的每轨迹 MC-MSE 再等权平均约为 2.660/0.0036/0.0015/0.0011；这是选择性 greedy 轨迹、不同历史训练目标的描述性指标。更低 MSE 不能直接转换为赢率或支持继续扩大 critic 训练。

原版只读字节码证据绑定 jar SHA256 `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`，检查 GridCardSelectScreen、EmptyCage 和 Astrolabe（要求选择 2/3 张及选中后取消）。原版游戏文件未改动。必要的动态投影来自已有原版 fixture；本轮没有新增动态游戏导出。

## 工程实现与资格

研究 observer 只旁观训练。固定 RNG 检查比较动作、GAE/奖励、损失、参数和 Adam 状态；head-only warmup 同样检验。小规模完整父模型 CPU probe 共 32 个学生决策，诊断开关和 checkpoint 后续精确一致。CPU probe 不是 GPU 资格，不消耗正式两臂预算。

一次 probe 的首 minibatch actor 梯度范数约 17.58，weighted-value 约 0.205，共享表示夹角余弦约 −0.0072，合并裁剪系数约 0.0284。更新前保存与重建 memory RMS 为约 7e−8，更新后两个 worker 为 0.0135/0.0112；这只说明仪器能观察到变化，不能外推长期训练机制。概率 KL 的微小负值可能来自 float32 舍入，不能当作负散度现象。

CPU 全量测试 **1393 passed、6 skipped**；最终工具收尾后另有恢复/契约/规则相关 **124 项通过**。六个跳过项是四个本地原版投影、一个本地导出策略、一个 CUDA 资格；研究新增测试没有跳过。Ruff、词表和差异格式检查通过。配置检查 35/36 通过：唯一失败是历史 Critic20M 配方既有的绑定错误，被明确保留；新研究配置通过，没有加白名单或重写历史身份。

新增测试覆盖：公开前缀重建、private/public/limiter 对齐及篡改拒绝、缓存版本和克隆、sampler RNG 恢复、teacher 前缀排除 PPO 损失、诊断等价、恢复等价、CUDA 禁用、命名空间碰撞、覆盖不足拒绝、失败归档/停止后续阶段；复用原版多选与重复卡牌测试。详细检查和 artifact SHA 见 [local-acceptance.json](local-acceptance.json)。

登录节点提交入口不导入 Torch。Windows 已验证原子保存和失败打包；真实 Linux 信号转发仍必须在 NUS allocation 内通过资格门禁，不能用本地 Windows 检查代替。GPU 正确性也在 allocation 内先检查，失败即归档停止，不启动训练。

## 封存首轮实验

`configs/research/act2_learning_r1.json` 固定现有网络、奖励、PPO 和优势缩放。冻结 90M 仅只读迁移模型参数；不恢复旧 optimizer、随机状态或 worker。新研究 checkpoint 是严格外层契约，绑定 source/config/native、状态库及 sampler RNG/诊断历史；旧 checkpoint 恢复规则不放宽。

两臂均为 64 workers/16 shards，rollout256，128 updates = **每臂 2,097,152 学生决策**。前32 updates 仅正常开局 critic head warmup，必须核验 actor/backbone 未变、两臂完整状态一致。随后对照全部正常开局；课程每次 episode 初始化 75% 正常、25% 自然后段。25% 是 episode 概率，不是决策占比；记录实际按幕决策、成功和 Boss 暴露。

teacher 使用恰好2048独立训练 seed，四层 entry/ordinary/elite/boss；每 seed 每层取最早有效边界，不使用未来成功或 teacher value。先均匀选层，再均匀选该层 seed。每层不少于32不同 seed且 Boss 层三种齐全，否则停止，不扩大采集预算。private 恢复后验证公开边界；当前模型重建整段前缀，保留实际 previous action/reward 和全量 limiter 步数/visits。前缀无梯度、不进 PPO，更新后缓存失效；teacher、前缀、诊断成本分开计时。

| 用途 | 封存范围（右端开区间） |
|---|---|
| 正常训练 | `[6100100000000,6100101000000)` |
| 状态库 | `[6100110000000,6100110002048)` |
| 周期评估 | `[8000014000000,8000014000512)` |
| 最终确认 | `[8000015000000,8000015004096)` |

本地扫描通过；服务器提交和 allocation 内再次检查已有登记，冲突立即停止。历史开发诊断和封存 holdout 不进入训练。

每32 updates 完整保存并评估同512 seed。连续两次相对同运行时父模型 Act2 到达下降≥3pp且配对 p<0.01，停止该臂；故障立即停止。两臂提前停止只比较共同完成的预算检查点，不选择 best-progress。最终4096新开发种子比较父模型及两臂，报告 lost/gained、配对 bootstrap CI、exact discordant-pair p；课程相对父模型 Act2 到达的单侧95%下界须高于−3pp。

解释规则进一步保守封存：要推荐扩大课程预算，课程对父模型和同预算对照的联合完成差值均须为正、配对95%区间下界>0、p<0.05，并满足保留门槛。只优于父模型却未能区分课程与对照，标为“分布干预效果尚未确认”。未显著不等于证明无效；报告证据不足、是否能排除有意义改善。这是单训练 seed 的探索性结果，不自动扩大预算。

## 运行与交付

NUS 一次 A100/16 CPU/64GB/48h allocation 依次进行：native 构建→Linux信号门禁→GPU/诊断等价/恢复门禁→teacher库与资格→对照→课程→配对最终评估→完整归档。46h 主动收尾预留2h，SIGTERM 先转给阶段主进程，允许 worker 存活完成边界保存；不依赖最后 SIGKILL。每阶段独立日志，失败也生成 failure、inventory 和 archive SHA，不自动续训。

只在拉取最终固定提交后运行 `tools/submit_act2_learning_research.py --source-root <历史发布根目录>`。最终归档为新工作树的 `local/runs/act2-learning-research-r1.tar.gz`；对应 `.tar.gz.sha256.json` 和 Slurm stdout 提供归档 SHA。完整阶段结果和逐文件摘要包含在归档中。服务器具体固定提交命令随本轮交付，不使用 main 的浮动 HEAD。

本地可复核：DL 环境，`CUDA_VISIBLE_DEVICES=-1`，运行 `python -m pytest tests/research tests/rl/test_training_contract.py tests/rl/test_ppo_math.py tests/rl/test_checkpoint_policy.py tests/test_critic20m_lightweight.py tests/simulator/test_deck_grid_selection.py`，以及 `python -m ruff check .`、`python tools/check_training_configs.py`（上述唯一历史绑定拒绝预期存在）。自然诊断 CLI 默认 CPU 并拒绝自动 GPU；新命名空间必须为空。

下一决策只依据两臂结果：自然分布干预有收益且保留前段才扩大；前段继续损失则研究保留约束；后段暴露增加但不学习则检查优势/梯度和成功经验利用；长前缀表现暴露记忆漂移再研究重建策略。不会同时加入 BC、KL、奖励修改或网络扩张。A20H 晚幕规则一致性是后续完整目标的独立资格要求，本轮两幕学习改善也不代表 A20H 已解决。

相关研究：[R2D2](https://openreview.net/forum?id=r1lyTjAqYX)、[反向课程](https://arxiv.org/abs/1707.05300)。仅作为问题背景；离线 recurrent replay 的结论不能直接代入本项目 on-policy PPO，自然状态库也不是已验证的反向课程算法。
