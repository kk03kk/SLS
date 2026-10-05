# 模型分享与发布

2026-10-05：另有固定 90M 及周期所选 76M 开发候选，分别导出到 `model/ironclad-a20-act1-90m-endpoint.pt` 与 `model/ironclad-a20-act1-76m-periodic-selected.pt`，各有同名 JSON 来源清单。[90M 结案](results/win90m-20261005/README.md)与[候选哈希](results/win90m-20261005/model-candidates.json)记录身份。没有覆盖以下历史 champion，也未把开发确认成绩当作原版胜率或保留最终集结果。

46M → 60M 阶段已完成，当前默认模型是 56,000,512 步 A20 Act1 champion。
独立终评为 1,585/2,048（77.39%），对应服务器训练时的模拟器。
[阶段结案](results/a20-act1-60m-stable/README.md)和
[机器可读摘要](results/a20-act1-60m-stable/summary.json)保存成绩、逐 Boss
结果、训练环境与模型哈希。当前本地规则已经修改，尚未重新大规模评估。

## 分享给朋友

本地分享包：`runs/archives/sls-ironclad-a20-act1-56m-policy.zip`。
朋友将它解压到克隆仓库的根目录后，`model/` 中应有：

- `ironclad-a20-act1-56m-champion.pt`：约 4.93 MiB 的推理模型。
- `ironclad-a20-act1-56m-champion.json`：哈希和来源清单。

```powershell
python tools\play_live_inspector.py --list-models
Get-FileHash model\ironclad-a20-act1-56m-champion.pt -Algorithm SHA256
```

推理文件 SHA256：`fdc820cf0f5d807d15ab7834981f280ae370d54288b9d02b45dafe6d3c987536`。
Git 不包含权重，仓库也没有公开模型下载链接；必须另行发送分享包。
分享包不含训练优化器、游戏或 Mod 文件。连接原版需朋友自己的游戏与
合法取得的 Mod / Oracle，见[安装速览](friend-quickstart.md)。

## 保留的来源证据

选中 checkpoint、完整终评、metrics、配置及 manifest 保存在
`local/runs/ironclad-a20-act1-v4-60m-stable/`；下载原包保存在
`runs/archives/sls-ironclad-a20-act1-56m-champion.tar.gz`。原包未包含
`latest.pt` 和服务器导出策略，不是完整精确恢复备份。原 46M checkpoint
仍保留在历史归档中。历史审计轨迹使用 38M 模型，该模型位于
`runs/archives/policies/`，不进入默认模型列表。

## 如需公开发布

将推理 `.pt`、同名 JSON 和阶段摘要作为 Release 附件上传，再把实际下载
链接写入 README。不要上传游戏 / Mod、个人路径、凭证或完整训练 checkpoint。
当前未创建 Release、未推送 Git、未开始下一轮训练。
