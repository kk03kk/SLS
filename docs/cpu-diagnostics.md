# CPU 自然状态诊断与性能门槛

这些入口只做研究诊断，不训练、不恢复 optimizer、不发布训练环境。CLI 在导入 Torch 前设置 `CUDA_VISIBLE_DEVICES=-1`，策略加载和推理固定为 CPU；只接受 `--device cpu`。同一输出路径禁止覆盖。

## 自然状态库

在 Python 3.12 的 DL 环境和已构建 native 的隔离工作树执行：

```powershell
conda activate DL
$env:CUDA_VISIBLE_DEVICES = '-1'
$env:PYTHONPATH = "$PWD/src;$PWD"
python tools/diagnose_cpu.py capture --artifact-root D:/SLS --output local/reports/cpu-diagnostics-20261010/corpus
python tools/diagnose_cpu.py compare --artifact-root D:/SLS --corpus local/reports/cpu-diagnostics-20261010/corpus --output local/reports/cpu-diagnostics-20261010/comparison.json
python tools/diagnose_cpu.py returns --corpus local/reports/cpu-diagnostics-20261010/corpus --output local/reports/cpu-diagnostics-20261010/returns.json
python tools/summarize_cpu_diagnostics.py --corpus local/reports/cpu-diagnostics-20261010/corpus --comparison local/reports/cpu-diagnostics-20261010/comparison.json --returns local/reports/cpu-diagnostics-20261010/returns.json --output local/reports/cpu-diagnostics-20261010/summary.json
```

默认三个 checkpoint 是冻结 90M、lambda098 94M、lambda100 94M 的 `final.pt`。可重复传入 `--checkpoint LABEL=PATH` 指定只读权重。配置与现有 run/report JSON、TOML 中的 seed 注册若与 `[132100000,132100032)` 冲突，capture 会报错。后续研究必须选用另一个未使用的诊断区间。此处不是正式评估 seed。

`public/*.jsonl.gz` 保存每个决策前的完整公开 Observation、合法动作顺序、真实动作、raw reward、shaped reward 和 value，以及完整终止或诊断截断结尾。`private/*.json.gz` 单独保存 native 恢复状态及 episode limiter；不会传给网络。manifest 绑定各文件 SHA256、native 源摘要、模型 SHA256、环境与诊断目标。

最多 64 个自然状态按 act、screen、敌人组合分层，以 SHA256 排序后轮转选择。没有自然遇到的 Boss 或选择分支不会用注入状态补齐。每个模型使用相同公开行为前缀重新计算自己的 recurrent memory，包括记录的上一步动作类别和 **raw** reward。后续最多 256 个决策；诊断上限表示未完成，return 为 null。训练定义的 cycle/step limit 仍保留训练终止与失败目标语义。

完整轨迹 MC 目标使用训练实际存储的 float32 shaped reward，gamma=1，不做 bootstrap。报告明确列出训练来源、诊断 Act1–2 horizon、奖励配置与 greedy 策略。90M 的 Act1 来源及历史 native 版本与本次发布环境之间的差异是显式诊断迁移，绝不视为历史训练恢复。greedy 的 MC 是行为诊断参照，不能冒充随机训练策略的 critic 校准；value 也不是胜率。

## 固定轨迹性能验收

```powershell
python tools/benchmark_cpu_diagnostics.py --corpus local/reports/cpu-diagnostics-20261010/corpus --baseline-ref 80bfb37a806c18154ab38f6a845160fbf59634ce --candidate-ref working-tree --output local/reports/cpu-diagnostics-20261010/performance.json
```

工具从 Git 提取冻结 helper 源代码，可用 `--baseline-ref`、`--candidate-ref` 比较独立优化提交，也可用 `directory:PATH` 检查未进入生产路径的原型。每组交替 A/B 顺序，至少 7 组，双方相同 warmup。分别计时 Observation 序列化与公开字段校验、循环指纹、编码、已编码的 batch 组装、固定真实动作的完整 native 重放；记录实体、候选、8 状态 batch padding。完整重放包括 backend、编码、单状态 batch、limiter，不包括策略推理，不能据此宣称整个 PPO 或 NUS 的加速。

计时前比较所选公开状态、合法动作、全部编码 tensor、指纹，并在每个重放决策恢复 checkpoint，比较真实动作后状态、奖励、limiter、终止标志及最终 Observation。结果 SHA256 不同就停止。每项优化必须独立满足目标阶段中位耗时改善至少 10%，且完整重放退化不超过 2%，才能进入生产路径。所有动态状态仍逐次读取，没有 Observation、native 或指纹值缓存。

训练实现源摘要会随生产优化正常变化。现有恢复契约保持不变；没有新增兼容白名单。该研发分支不应直接恢复正在运行的历史训练。
