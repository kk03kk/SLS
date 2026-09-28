# 60M 阶段审核与整理记录

## 结论

本阶段已经结束，默认模型为 56,000,512 步 A20 Act1 champion。
[结果摘要](../results/a20-act1-60m-stable/README.md)与机器可读 JSON 记录
实际成绩和环境身份。没有启动新训练或进一步模拟器审计。

本次检查完成文件归档、模型加载验证和项目自动检查，不是完整游戏规则
一致性认证。[第一幕审计记录](2026-09-26-simulator-improvement-plan.md)
保留尚未验证的内容与组合。

## 导入与验证

- 导入用户下载的 `sls-ironclad-a20-act1-56m-champion.tar.gz`，在独立
  staging 目录解压，未覆盖工作区配置或旧 checkpoint。
- 原包包含的 7 个 bundle 登记文件逐一通过 SHA256 核验；其余 2 个登记
  文件（`latest.pt`、服务器推理导出）不在包中，明确记录了这一缺口。
- 所选 checkpoint 的 trainer 步数、更新编号、原生源码身份和编码与
  选模/终评报告一致；2,048 条结果种子无重复，完整覆盖终评种子区间，
  汇总通关数为 1,585。
- 当前配置文件哈希与服务器保存配置一致；当前模型编码、词表、profile
  仍兼容。导出为独立推理模型，导出文件与权重哈希均已记录。
- 当前本地 native 源码与已构建产物身份一致：
  `c28b45cefa2e2d8529ae21c53bcf722b1ab33efc75693d759887da554b2480a8`。
  它不同于服务器训练源码，不能把历史成绩标成当前环境成绩。
- 56M 在当前模拟器 seed 42 下完成第一幕：160 个动作、161 个边界。
  这是可用性检查，未启动大规模评估或真实游戏。
- 全量测试：854 passed、1 skipped（默认模型目录不含可选历史策略）。
  4 个 warning 均来自测试明确覆盖的 checkpoint provenance rebind。
- Ruff 检查通过；修正两处测试 import 排序，相关 28 项测试再次通过。
- 词表生成检查通过；所有 README / docs / 模型说明的本地文档链接有效；
  Git whitespace check 在正确识别 Windows CRLF 后通过。

## 文件布局

| 路径 | 本次后的内容 |
| --- | --- |
| `model/` | 56M 推理 `.pt`、同名 JSON、使用说明 |
| `docs/results/a20-act1-60m-stable/` | 可提交的精简结果、身份与验证摘要 |
| `local/runs/ironclad-a20-act1-v4-60m-stable/` | 下载的完整原始终评、metrics、manifest、checkpoint |
| `runs/archives/` | 原下载包、朋友模型分享 ZIP、历史下载包 |
| `runs/archives/policies/` | 旧推理模型；38M 原版审计基准仍保留 |
| `runs/archives/extracted-history/` | 退出活动目录的旧训练结果 |
| `local/audits/`、`local/reports/`、`local/runs/canary/` | 独有模拟器/原版对照证据，仍有用途 |

根目录的下载 tar 包已归档，旧 `sendToDevs/` 日志已移至
`local/logs/legacy-send-to-devs/`。模型分享 ZIP 经解压 CRC 与模型 SHA256
核验，仅含推理模型、清单、说明及结果摘要，不含游戏、Mod 或优化器。
模型、原始产物与 ZIP 均被 Git 忽略；源码和精简结果可正常提交。

对历史训练文件逐一比对本地保留 tar 成员后，确认 141 个文件共
1,438.47 MiB 完全重复，没有独有文件。自动审批拒绝批量删除命令，
未提供具体原因，因此本次改为归档移动，**没有释放该部分磁盘空间**。
校验清单保存于 `local/operator/60m-cleanup-plan.json`，其中原路径对应
移动前布局，当前路径为 `runs/archives/extracted-history/`。

本次操作发生在 D 盘工作区；未访问服务器、未修改服务器备份，未推送
Git 或创建公开 Release。朋友仍需单独获得模型和合法的游戏连接依赖。
