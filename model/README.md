# 演示模型

本目录只放可供推理和实机控制窗口加载的 `sls-policy-artifact-v5` 模型。
当前默认模型：**战士 A20 第一幕，56,000,512 步 champion**。

| 文件 | 用途 |
| --- | --- |
| `ironclad-a20-act1-56m-champion.pt` | 推理权重及模型元数据，约 4.93 MiB |
| `ironclad-a20-act1-56m-champion.json` | 文件哈希、原 checkpoint 身份、训练步数 |

模型和清单不提交 Git。朋友需另外下载这两个文件并放入本目录；仅 clone
仓库不会得到权重。当前独立终评为 1,585/2,048（77.39%），该成绩对应
服务器训练时的模拟器；当前本地规则已修改，不能直接沿用该成绩。
完整结果与哈希见[阶段结案](../docs/results/a20-act1-60m-stable/README.md)。

```powershell
python tools\play_live_inspector.py --list-models
```

不要放 `latest.pt`、`final.pt` 或其他带优化器状态的训练 checkpoint。
旧演示模型已退出此目录，保存在忽略的 `runs/archives/policies/`，其中
38M 模型仍是已有原版对照轨迹的来源。导出新模型时使用：

```powershell
python tools\export_policy.py <checkpoint> --output model\<name>.pt `
  --goal ACT1 --ascension-min 20 --ascension-max 20
```

2026-09-30：Progress / Win 2M 候选已核验，推理产物与原始 checkpoint 保存在各自 `local/runs/ironclad-a20-act1-plateau-*-2m-r1/`。未证明优于历史 champion，默认模型保持不变；见[本轮结果](../docs/results/plateau-reward-screen-20260930/README.md)。
