# A20 战士正常开局 Act 1–2：90M 后的训练准备

2026-10-05 状态：90M 完整归档已核验，[固定终点达到开发成功判据](results/win90m-20261005/README.md)，
推荐固定 90,013,696 parent。最终 Act1-2 config/plan 尚未绑定，当前不提交新作业；
不改已完成 90M 配置，不将此草案当作可直接执行的 bound plan。
研究目标为正常 Neow 开局连续打通两幕，非单独第二幕开局。Act2 成功仍不是 Act3/Heart 完整局。

## 暂定配方

[机器可读草案](../configs/experiments/act12-win-pilot-recipe.json)不能直接提交。
首轮新增约 2M decisions；一个新 training seed 130000000，64 workers/16 shards；
保持父模型的 PPO scalars、Win ±1/gamma=1 与网络。native 使用已核验的 Snecko 顺序修复，
由独立环境迁移证据绑定；父模型和候选均在修复后的两幕环境评价。正式 `curriculum-stage` 权重迁移，
包括 value head；重置 Adam/RNG/workers/memory/update/best，继承累计 steps。
这是新任务，不是原实验 exact resume。训练 target 由真实父模型 steps+预算自动计算。

- 开发选择 `[8000006000000, +256)`，约每 0.5M。
- 独立开发确认 `[8000007000000, +1024)`，冻结父模型、固定 endpoint 与 selected 在同进程
  同 seeds/runtime 下评价。开发确认不用于再选 checkpoint。
- `[9000000000000, +4096)` 继续封存；本地 32-seed 探针与 90M 的开发集不重复使用。
- 主假设是现有配方能够开始学会第二幕；单 seed 小试跑只判断可行性。若仍缺成功轨迹，
  诊断探索/critic/入幕分布，再选一个变量，不能自动扩长训或同时改多个机制。
- 提交申请暂定 24 小时，非 measured Act2 GPU cost；GPU 节点会重新 preflight/benchmark，
  不将 Act1 benchmark 伪装成 Act2 吞吐。

## 90M 归档回来后的本地流程

1. 保留原始包，校验服务器身份、SHA、COMPLETE manifest、bundle、checkpoint 和原始结果。
   用既有 Win continuation 分析工具分析 endpoint 与 selected，按已登记规则结案；保留
   历史 champion。审核第二幕高影响规则与原版资格缺口，所有胜率先明确为 simulator evidence。
2. 冻结 endpoint 或该 run 正式注册的 selected 父模型，记录选择理由。
   `prepare_act12_pilot.py` 仅接受完成且 target 至少 90M 的 A20 Act1 run，核验 bundle、
   healthy development confirmation、native、模型与 steps。native 变化必须匹配明确的
   weights-only 迁移证据；没有对应 source/target SHA 与证据时拒绝。对 pending selection、损坏或
   未完成的父训练直接拒绝。父模型路径由已核验归档确定，不靠猜测。
3. 审阅草案；必要的单变量优化在此时明确登记。基础生成器保留父 PPO，不自动改 entropy、
   λ、normalization、LR、模型或 simulator。若未来决定改变变量，必须更新配方/生成器和
   对应 control/验证证据，不手改 bound config 来绕过工具。
4. 使用生成器绑定实际 SHA，生成 TOML 与 hash-bound plan，然后在本地验证、提交并推到 main。
   生成器拒绝覆盖 config、plan 和既有 run。

本地绑定命令格式（提交新计划前检查当前源码资格并填入结案理由，本页不代表已经执行）：

```powershell
conda activate DL
python tools/prepare_act12_pilot.py --parent-run local/runs/ironclad-a20-act1-win-90m-continuation --parent-role endpoint --decision-note "这里填写基于真实90M结果的父模型选择理由"
python tools/check_training_configs.py
python tools/submit_act12_pilot.py --plan configs/experiments/act12-win-pilot.json --dry-run
```

`endpoint` 绑定 `final.pt` 与 endpoint-evaluation；`selected` 绑定 best_progress.pt 与正式
selection/final-evaluation。source SHA/profile 不会被改写为 Act2。

## 训练与迁移身份变化

训练 entrypoint 的 frozen reference 原来要求与 target 同 profile。现在仅对显式相邻
A20 curriculum transfer 放行跨幕 control：参考 checkpoint 必须与 warm_start 同路径、
同 SHA、source profile 为立即前一幕，且模型配置匹配。native 必须相同或匹配明确审阅的
weights-only 环境转移证据。普通 reference 原限制
保持；不能随意比较其他模型/环境。`development_reference_profile` 纳入新 config identity。

新 reference-evaluation schema 为 `sls-frozen-reference-evaluation-v2`，明确 source_profile、
evaluation_profile 与 cross_horizon_reference。checkpoint/encoding/native schema 未改；
新的 training implementation digest 随 entrypoint 修改变化，bound plan 固定它。
现有 90M config/plan 的旧 digest 保留，它们是已提交实验，不随当前 HEAD 改写。
带规则变更的初始化 record 使用 `sls-curriculum-weight-transfer-v2`，写入语义修订、前后 SHA
和证据。旧环境不能在当前 native 下 exact resume；不添加 state-preserving 白名单。

**90M 已完成并归档。** 新阶段必须重新绑定、验证并推送代码后再由服务器拉取。
之后由人类在服务器拉取最终提交，并用 `submit_act12_pilot.py --plan ...` 提交单个作业。
该工具检查 clean Git、config/source/parent 身份，拒绝已有目标和重复 submission receipt；
准备、preflight、benchmark、训练放在同一个 GPU 作业，不在 login node 运行训练。
实际唯一推荐服务器命令将随最终 bound plan/commit 一起给出；当前没有已生成的 bound plan，
不能用本页草案启动训练。

## 结果分析与本地验证

未来 `analyze_act12_pilot.py --run ... --output ...` 校验 bundle/模型/选中 checkpoint，
比较正常开局两幕联合成功。不会只保留到达第二幕的幸存子集；不要求未到第二幕的
父模型具有第二幕 Boss 元数据。第一幕 clear 不能记作两幕成功。
分析 schema `sls-act12-pilot-analysis-v2` 区分逐幕通过数、Boss 入场后的逐幕通过率和
完整两幕成功数；原始 `boss_successes` 是逐幕通过数，不能与两幕终局 success 等同。
第二幕 Boss 分组依赖各策略实际入幕分布，不构成固定人群的因果配对。
零 Boss entry 或零入幕时的条件比率报告 undefined，联合胜率始终以全部正常开局计。

本地证明分为：

- `tests/test_act12_pilot.py`：目标/预算、recipe 保持、身份/path/seed/健康门禁和损坏拒绝。
- `tests/test_act12_analysis.py`：端到端 pairing、不同 reach rates、错误成功定义及 runtime 拒绝。
- 既有相邻课程迁移与 native/selection 回归。
- [真实70M小探针](results/act12-readiness-20261004/transfer_micro_probe.py)：真实权重，正常
  Act2 profile、1 worker×16 rollout、两个小更新，验证 fresh transfer 与 exact checkpoint
  replay。它使用缩小 minibatch/sequence，仅是本地实现验证，不能冒充90M或NUS训练结果，
  不能推断正式64-worker训练性能或胜率。

完整本地结果和源身份保存到 `docs/results/act12-readiness-20261004/preparation-validation.json`。
后续 Snecko 修复与新环境迁移的结果另存于
`docs/results/act12-qualification-20261004/`；旧验证记录保留原样。
后续三个 Boss 与 Spheric Guardian 的 11 个新边界案例、原版字节码身份和 native 轨迹见
[有限规则资格](results/act2-rule-scenarios-20261004/README.md)。它们未改变 native 身份，
且不替代完整第二幕交互/RNG 资格；其中人工高 HP/强制意图场景不是策略评估。
90M 结果现已取得，见 [结案记录](results/win90m-20261005/README.md)。建议固定 90M 为 parent，但正式 bound plan、训练配置和提交命令仍需按当前 HEAD 重新准备与验证；本页不构成已提交作业。
