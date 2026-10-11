# 文档导航与维护规则

| 文件 | 维护内容 |
| --- | --- |
| [architecture.md](architecture.md) | 模块职责、数据边界与兼容性 |
| [development.md](development.md) | 安装、构建、测试、实机运行、main 与服务器工作流 |
| [training.md](training.md) | 实验合同、续训、评估、权重与产物保留 |
| [status.md](status.md) | 当前结果、问题与下一步 |

本文件负责导航。日常说明只维护上述文件；状态改变就在 `status.md` 修改，操作方法改变就在对应指南修改。不要再为每次分析、检查或进展新建日期文档、重复索引或整理结案文档。

实验原始记录仍按 run 保存，使用 JSON/JSONL、配置、manifest 和 checkpoint。它们属于证据与运行数据，不用写成新的长篇日记。可移植回归证据在 `tests/fixtures/evidence/`，本机原始产物在 `local/`。

2026-10-11 整理前的文档、一次性探针及旧报告工作副本已原样归档到本机 `D:/SLS-archive/20261011-cleanup/`。其中保存 Git bundle、工作区 ZIP、逐文件盘点和搬迁清单。归档不随 clone 分发；源码和测试必须使用仓库内的证据。

旧合同中的 `docs/results/`、`docs/audits/`、`docs/training-runtime-contracts.md` 和 `runs/archives/` 由证据路径解析器对应到新位置。原始 JSON、训练配置和权重保持原字节；旧记录里的日期、路径和身份说明表示当时的布局，不是当前操作指令。

根目录 `AGENTS.md` 是人类维护的项目指令，整理不修改它。

旧local训练与已恢复游戏备份另归档于 `D:/SLS-archive/20261011-local-cleanup/`。当前local保留范围和历史文件恢复方法见 [training.md](training.md)；已移到项目外的私有文件不会由路径解析器自动取回。
