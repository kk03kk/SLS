# 演示模型

2026-10-06：新增 `ironclad-a20-act12-pilot-94m-endpoint.pt` 和 `ironclad-a20-act12-pilot-91m5-periodic-selected.pt`（各带`.json`来源清单）。由下载的checkpoint在本地导出，goal=ACT2；开发联合通关3/1024和2/1024，没有晋升champion，不覆盖历史56M/90M。服务器原独立policy导出未包含在lite包里，未补造。见[Act1–2结案](../docs/results/act12-pilot-20261006/README.md)。

本目录只放可供推理和实机控制窗口加载的 `sls-policy-artifact-v5` 模型。
当前默认模型：**战士 A20 第一幕，56,000,512 步 champion**。

2026-10-05：Win 90M 完整归档已核验。在同一批新开发确认种子上，固定 90M 为 1640/2048（80.08%），冻结 70M 为 1574/2048（76.86%），配对 +3.22pp、p=.002404；周期所选 76M 为 1619/2048。两个候选已分别导出，见[90M 结案](../docs/results/win90m-20261005/README.md)。保留原 56M 文件，未通过保留最终集或原版完整局测量晋升 champion。

2026-10-02：Win 70M 作业已完成，68M 周期最佳是新的开发候选，确认集 1581/2048；固定
70M 终点比同种子 frozen 56M 提升 +2.78pp，p=.01263。原始 checkpoint/导出仍在服务器
run 目录，旧演示 champion 不覆盖；最终 9e12 保留集未使用。开发候选不等于原版通关率
认证，见[70M 结案](../docs/results/win70m-20261001/README.md)。

| 文件 | 用途 |
| --- | --- |
| `ironclad-a20-act1-56m-champion.pt` | 推理权重及模型元数据，约 4.93 MiB |
| `ironclad-a20-act1-56m-champion.json` | 文件哈希、原 checkpoint 身份、训练步数 |
| `ironclad-a20-act1-90m-endpoint.pt` / `.json` | 固定 90,013,696 终点的推理候选及来源清单，推荐后续阶段 parent |
| `ironclad-a20-act1-76m-periodic-selected.pt` / `.json` | 76,005,376 周期所选推理候选及清单，与服务器自动导出权重一致 |

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
