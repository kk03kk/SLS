# Act1+Act2 正式 20M 长训准备

2026-10-07。当前 λ=.98/1 匹配 pilot 为用户报告已开始的 NUS job 916291，固定提交 `6e54adbc0aa6fca81b994490837db382b245e42b`。本目录是后续工程准备，不是该作业的训练结果。**状态：工具已准备；父模型和长训启动计划等待本轮完整结果。**

## 决策与预算

保留 `lambda-study-r1.json` 的原统计标准及 `SUPPORTS_SECOND_TRAINING_SEED_REPLICATION` 历史标签。用户随后决定：有效则直接从固定终点续训20M，不增加前4M复制门槛。λ=1达到原标准时选择experimental；否则对control与同环境冻结90M作明确的次级比较：联合通关至少+1pp、exact McNemar p≤.05、健康完整结束、两个晚期窗口均有真实联合成功，才能选择control。次级比较未作整个研究过程的多重校正，不能冒充预注册λ优势结论。两者均不满足时拒绝创建可启动计划。

续训预算为父checkpoint实际累计步数+20,000,000，最多超出不足一个rollout。约94M→114M只是历史累计计数，不是114M Act2训练。仍以正常Neow开局A20 Ironclad两幕联合通关为主；不扩展Act3/Heart，不添加Act1生存终奖。

## 续训契约

`initialize_act12_continuation.py` 复用Act1初始化的原子创建路径，但只允许A20 ACT2完整固定终点，不能继承半学习率或规则迁移权限。验证父bundle、endpoint评估、manifest、latest/final完整学习状态一致，当前native与训练实现必须相同。20M预算、输出和评估/存盘安排可改变；λ、LR、PPO、model、reward、normal-start分布、worker layout及seed上界保持不变。

保留model、Adam、update、next_seed、Python/Torch/CUDA RNG、原生环境/RNG、episode limiter、GRU memory与previous action/reward。新目录记录父SHA、配置差异、旧/新训练身份；周期选择从新baseline重新开始。它是**改变运行预算的状态保持续训**，不是原pilot整个实验身份不变的exact resume。相同runtime的下一次rollout和更新等价由测试验证；跨节点允许的Git/GPU名称rebind仍按现有规则记录，不保证跨runtime逐位一致。

当前native `b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f`、training implementation `9dca86829696613903d07b7fcbc6d4928f1c957c98fd6eaad534d6e76519ba94` 均未改变。不更新规则、observation/action/reward或模型格式。准备契约因初始化工具更新而必须在新GPU作业重新验证；正在运行的服务器checkout不更新。

## 评估与调度

- 每2M进行512个周期开发seed评估；每1M存checkpoint，安全中断另存latest。没有前4M研究晋级门槛，也不因reach下降或某次峰值停止。
- 新周期区间 `[8000012000000,8000012000512)`，新终点开发确认 `[8000013000000,8000013004096)`。绑定和提交均扫描实际JSON/JSONL及压缩评估记录，发现重复则拒绝。最终保留集 `[9000000000000,9000000004096)` 仍封存。
- 长训终点评估的冻结reference是被选中的约94M Act2父终点，在同一新开发区间配对；不保留需要Act1→Act2 warm-start契约的90M跨幕reference，避免把状态保持续训伪装成再次权重迁移。
- 以本轮实际pinned-layout benchmark估算allocation数：`ceil((20M/rate*1.5+8h)/40h)`，每个48h。100 decisions/s时约3个保守allocation；不是三个独立实验，也不是完成时间保证。
- 单一逻辑训练通过afterok链续接。每次运行加独占锁、一次性allocation ledger；checkpoint SHA必须与前次安全退出记录一致，实际恢复preflight通过后才采样。SOAK_COMPLETE或明确处理SIGTERM的INTERRUPTED可续接；失败、SIGINT、缺失/变更checkpoint阻断。最后一段仍未完成则退出非零、保留checkpoint并要求检查，不降低预算。
- 完成后剩余allocation跳过；部分sbatch提交失败保留已提交job IDs，不能重复首提交。真实Slurm信号/排队行为尚待NUS执行验证，本地测试不能替代服务器运行。

## 诊断与有界核验

`report_act12_progress.py` 严格复算bundle、固定终点、周期曲线、入幕资源、失败上下文及KL/critic/advantage/Neow指标。旧pilot缺export仅在显式 `--allow-missing-export` 下保留缺项身份；新结果默认拒绝缺项。

`diagnose_act12_learning.py` 使用冻结checkpoint、新本地诊断seed空间132000000起，收集完整死亡/成功片段、GAE .98/1、Neow公共内容与概率、共享actor/critic梯度方向、循环旧memory偏差和cProfile热点。一次PPO更新只作用于临时副本；不导出模型，原权重不变。一个rollout内完整轨迹受长度筛选，不作为一般胜率或因果证据。

本地冻结90M、4workers/2shards、256rollout：四个完整开局均失败，真实shaped return约−1.036；GAE98开局return为+.186至+.533，GAE1与实际求和一致。这确认当前修复环境下初始化credit风险仍存在，不证明λ=1训练有效。128样本共享梯度cosine约−.061；一次临时更新后重算前缀与旧memory存在偏差。都不是已证明的主要病因。

公共序列化/episode fingerprint是本地热点。`benchmark_act12_field_cache.py` 的ABBA静态字段缓存原型保持rollout及临时PPO结果完全一致；仪器化小样本/冷启动不足以推断稳定NUS吞吐。**原型不集成生产，继续保留本轮已验证的NumPy编码优化。** 周期开发评估从每500k/256改为每2M/512，20M预算的周期评估局数约减半；实际评估耗时仍待服务器测量。

重新从合法JAR取得Collector/Automaton/BronzeOrb的javap，class和bytecode哈希与独立来源清单一致；原始内容仅本地保留。复用原stock受控捕获，21次重放全匹配，其中Collector/Automaton各6次。新增Collector的2/3层Artifact次序，以及Automaton第二beam循环checkpoint回归。没有新实机运行，不升级为全Act2或缺失自然轨迹认证。

## 复算入口与尚待绑定的事项

在DL环境、项目根目录并设置PYTHONPATH后：

```powershell
python tools/diagnose_act12_learning.py --checkpoint local/runs/ironclad-a20-act1-win-90m-continuation/final.pt --output local/audits/new-frozen90-diagnostic.json
python tools/report_act12_progress.py --run local/runs/ironclad-a20-act12-win-pilot-r1 --allow-missing-export --output local/reports/new-historical-pilot-report.json
python -m pytest tests/test_act12_long_run.py tests/simulator/test_act2_long_run_boss_boundaries.py -q
```

本轮完整结果下载后，本地运行 `prepare_act12_long_run.py --study docs/results/act2-qualification-20261006/lambda-study-r1.json`。它深验证真实结果并生成ignored的绑定config/plan；GPU上初始化与preflight后才训练。登录节点提交入口 `submit_act12_long_run.py` 保持Torch-free；不能在登录节点运行深分析/绑定工具。后续会将真实父SHA和配置证据提交GitHub，再给唯一服务器命令。现在不发布未绑定的长训提交命令，不占用GPU作业。

## 本地最终验收

完整测试 **1216 passed、1 skipped、4 warnings**（68.38秒）。skip为旧策略按当前encoding正确拒绝；warning来自受控runtime/provenance rebind测试。Ruff、33/33配置、词表/注册表、wheel构建及CRLF-aware diff检查通过。模型/native来源未变，AGENTS哈希未变。

完整测试另外发现配置恢复工具的时间戳备份碰撞：安全备份可能覆盖原恢复源。已改为独占创建并加碰撞后缀，固定时间戳回归通过。只修改工具，不涉及训练/游戏规则；未操作用户真实游戏文件。机器证据见 `evidence.json`，原始本地日志保留。
