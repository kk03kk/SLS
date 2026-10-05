# 本地文件与归档保留规则

以下均不上传 GitHub。忽略不等于可删：模型、原包、训练状态和审计证据可能只有这一份。统计为 2026-10-05 整理前快照，构建和日志会继续增长。

| 路径 | 文件数 | 字节 |
| --- | ---: | ---: |
| `local/(root files)` | 6 | 16,877 |
| `local/audit-2026-09-29` | 27 | 5,044,722 |
| `local/audits` | 5058 | 158,471,771 |
| `local/build` | 24208 | 534,178,287 |
| `local/cleanup-candidates` | 21 | 92,658 |
| `local/imports` | 14 | 4,680,166 |
| `local/logs` | 47 | 3,151,429 |
| `local/operator` | 3 | 73,487 |
| `local/reports` | 881 | 429,570,866 |
| `local/runs` | 2236 | 1,682,425,207 |
| `local/tmp-audit` | 73 | 2,122,052 |
| `runs/archives` | 164 | 3,416,107,752 |
| `model/(root files)` | 7 | 15,499,309 |
| `sendToDevs/logs` | 1 | 0 |

`model/`：保留 56M 历史 champion，以及分别标识的 90M 固定终点和 76M 周期候选，三个权重各有身份 JSON。只提交 README，不覆盖 champion。

`runs/archives/`：原始服务器包与历史模型是复算根证据。保留原始文件名、哈希和提取记录，不能仅留下汇总图。

`local/runs/`：训练包的工作副本、checkpoint、优化器状态、评估与日志。`imports/` 是导入检查材料。已归档不表示可随意删除，因为精确恢复训练还需要 trainer 状态和模型以外的契约。

`local/audits/`、`audit-2026-09-29/`、`reports/`、`tmp-audit/`：审计来源、原版对照、复算脚本、结果和整理恢复材料。历史来源可能已经不对应 HEAD，但仍有证据用途；保留日期，不作为当前代码导入路径。

`local/build/` 同时含可再生编译输出、测试缓存、下载工具、Oracle 构建记录和旧 JAR 备份，不能整目录删除。只有确认可重建的缓存适合单独清理。已移出的缓存集中保留在 cleanup-candidates 或本轮 reports 恢复目录。

打包也会临时生成根目录 `build/` 与 `src/sls.egg-info/`；它们不是正式源码，已有忽略规则。本轮构建残留移到 `local/build/repository-review-packaging/` 保留，之后打包仍可能再生成。

`local/logs/`、`operator/` 与根部本地笔记：游戏日志、用户机器路径及操作记录，不公开。`sendToDevs/` 是游戏生成的日志目录，旧非空日志已归档；游戏会再次创建空日志，保留忽略规则即可。

`D:/SLS-recovery/20261005/` 的历史 bundle 和 tracked-worktree.zip 已验证，但不包含这里的全部忽略模型和训练包。不能据此认为所有训练数据已有第二份备份。本轮未删除这些独有数据，也未重建远端仓库。
