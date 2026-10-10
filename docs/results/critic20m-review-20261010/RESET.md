# 自然后段 reset：隔离原型与资格检查

NUS 作业 927398 已提交四模型开发确认；它只做评估，不更新权重。尚未收到作业运行日志或结果。这里的研发独立进行，不要求切换服务器正在评估的 checkout。

新增 `tools/qualify_natural_reset.py` 和 `sls.diagnostics.reset_readiness`，只做 CPU 只读资格检查，不接入 PPO，不更改训练恢复契约。固定使用自然公开轨迹和版本绑定的 private checkpoint；现有诊断库始终标记 `training_eligible=false`，不得用于后续训练。

检查包含：从原 seed 正常 reset 后逐动作重放全部前缀；每步公开观测、合法动作、实际 reward 与历史一致；当前模型独立重建 memory，使用真实已选动作和 raw reward；到达边界时完整 native 状态（包括不可见的内部状态）与 private checkpoint 一致；独立 restore 后公开边界一致；steps 和循环 visits 与历史完全一致。到达后段时保持 `episode_start_mask=false`，不能刚重建记忆又让网络清空它。private 状态只进入 backend，从未进入策略输入。

选择首先按 episode 聚合，仅取每条轨迹最早的已选 Act2 状态，避免长轨迹贡献过多候选；这不是最早 Act2 入口，也不是最终课程分布。选择不看成功标签或 teacher value。不同 teacher 同一 seed 仍有关联，报告分别给轨迹数和独立 seed 数。

本地现有小诊断库的 8 个 Act2 状态形成 4 个 episode anchors，但只有 3 个不同 seed；覆盖三类普通战斗及一个事件，缺少精英、Boss 和入口选择边界。4 个边界的公开/完整 native/计数/当前模型前缀检查均通过；前缀分别 301、139、264、152 个决策，中位数 208。前缀推理和 native replay 是额外计算，不能当学生 PPO 决策，也不能不计成本地声称课程更省预算。CPU 单次耗时仅用于定位，不能推广到 NUS。

复现（新 output 名，避免覆盖）：

```powershell
conda activate DL
$env:CUDA_VISIBLE_DEVICES = '-1'
$env:PYTHONPATH = "$PWD/src;$PWD"
python tools/qualify_natural_reset.py `
  --checkpoint D:/SLS/local/runs/ironclad-a20-act1-win-90m-continuation/final.pt `
  --corpus local/reports/critic20m-followup-20261010/natural-corpus `
  --output local/reports/natural-reset-readiness-recheck.json --states 4 --device cpu
```

6 项新测试覆盖 episode 加权、success/value 不参与选择、公开边界篡改、前缀跨终止、相同可见状态下 hidden RNG 篡改、循环/步数重置、当前参数记忆和 boundary mask。最终诊断测试集 **111 通过、1 个历史编码检查跳过**；全仓 Ruff 通过。干净代码 `2cb0aa6319bda4cfae0cffe6e02c8f8cd6409c85` 上的真实 4 状态完整回放证据见 [reset-readiness.json](reset-readiness.json)，其中来源/模型 hash、跨环境及 horizon 迁移均显式标注。

下一步先取得 NUS 确认，确定 weights-only 起点；独立注册训练 seed、teacher 和采集预算后生成专用训练库。未来 sampler 必须明确以 seed/episode 为单位的权重、资源/构筑覆盖、正常开局比例、模型每次更新后的前缀重算费用；不得复制 teacher hidden memory。先用小 CPU smoke 验证 suffix reward/GAE、终止、worker reset 和 checkpoint 身份，再注册同预算正常开局对照。生产训练目前没有这个 sampler，本原型不提供启动训练的命令。
