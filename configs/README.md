# 配置目录

当前2026-10-08方案：`train/ironclad_a20_act12_critic20m_r1.toml`及SHA绑定`experiments/act12-critic20m-r1.json`。冻结90M显式迁移、32 rollout critic预热、新增20M、NUS先运行计算门禁。λ计划已标记历史完成，原参数、来源和标准保留。见[新配方说明](../docs/results/act12-critic20m-20261008/README.md)。

这里存放可以上传 GitHub 的正式项目配置、实验设计和兼容性契约。配置里出现的 `local/` 路径是运行时输入、输出的位置；对应模型、日志、基准报告不随配置一起上传。目录整理没有移动或改写任何原始 TOML、实验 JSON 或兼容性 JSON。

截至 2026-10-05：70M 与 90M 实验均已完成并核验归档；[90M 固定终点](../docs/results/win90m-20261005/README.md)在开发确认集达到预注册成功判据。Act1-2 只有待绑定的方案，不能据此提交训练。

| 目录 | 内容 | GitHub / 保留判断 |
| --- | --- | --- |
| `train/` | 19 份训练 TOML 和状态索引 | 正式上传；历史实验也保留原路径、原参数及身份 |
| `experiments/` | 3 份实验设计 JSON，包含对照、预算、种子、判据 | 正式上传；区别于可直接执行的训练配置 |
| `diagnostics/` | 1 份历史 56M 平台诊断配置 | 正式上传；仅作历史诊断复现，不当作当前基准 |
| `compatibility/` | 2 份精确、单向身份转换记录 | 正式上传；运行代码仍读取它们，不能按年代清理 |

## 训练文件逐项说明

下面的 M 是配置目标累计 decisions，并非本次新增预算，也不保证运行恰好结束在整数目标。完成状态详见 [训练索引](train/README.md) 与结果目录。

| 文件（均位于 `train/`） | 用途与处理 |
| --- | --- |
| `ironclad_a0_act1_5m.toml` | A0 Act1 5M，早期训练记录，保留 |
| `ironclad_a0_act1_10m.toml` | A0 Act1 10M，早期延续记录，保留 |
| `ironclad_a0_act1_20m.toml` | A0 Act1 20M，早期延续记录，保留 |
| `ironclad_a0_fullrun.toml` | 早期 A0 分阶段完整局方案，保留；不是已验证的 A20 完整局方案 |
| `ironclad_a0_fullrun_10m.toml` | A0 分阶段 10M 方案，包含诊断安排，保留 |
| `ironclad_a0_fullrun_12m.toml` | A0 分阶段 12M 延续方案，保留 |
| `ironclad_a0_fullrun_15m.toml` | A0 分阶段 15M / warm start 方案，保留源身份 |
| `ironclad_a20_act1_30m.toml` | A0→A20 转阶段及 30M 训练记录，保留 |
| `ironclad_a20_act1_40m.toml` | A20 Act1 40M 延续记录，保留 |
| `ironclad_a20_act1_50m.toml` | 包含历史 46M champion 的训练配置，保留 |
| `ironclad_a20_act1_54m_recovery.toml` | 恢复实验，未晋升 champion，保留失败/比较证据 |
| `ironclad_a20_act1_60m_optimization.toml` | 出现回退并停止的优化方案，保留；不推荐重跑 |
| `ironclad_a20_act1_60m_stable.toml` | 产生历史 56M champion 的稳定延续，保留 |
| `ironclad_a20_act1_plateau_progress_2m.toml` | 最初失败的 Progress 分支配置，保留而非删除 |
| `ironclad_a20_act1_plateau_win_2m.toml` | 最初因依赖失败受阻的 Win 分支配置，保留 |
| `ironclad_a20_act1_plateau_progress_2m_r1.toml` | 已完成的 Progress 恢复实验，保留 |
| `ironclad_a20_act1_plateau_win_2m_r1.toml` | 已完成的 Win 恢复实验，保留 |
| `ironclad_a20_act1_win_70m_continuation.toml` | 已完成的 Win 70M 延续，保留 |
| `ironclad_a20_act1_win_90m_continuation.toml` | 已完成并核验的 Win 90M 延续；保留提交时身份 |

## 其余文件逐项说明

| 文件 | 作用与限制 |
| --- | --- |
| `experiments/win-70m-20260930.json` | 70M 预注册设计、配置哈希、配对开发评估及保留最终集政策 |
| `experiments/win-90m-20261001.json` | 90M 预注册设计，固定 70M 对照；不能事后用漂亮峰值替换主终点 |
| `experiments/act12-win-pilot-recipe.json` | 4M正常开局Act1-2正式recipe；实际提交使用绑定90M parent的act12-win-pilot.json |
| `diagnostics/ironclad_a20_act1_plateau.toml` | 旧 56M checkpoint、旧 native 哈希及 preflight 引用；诊断种子不等同独立最终测试集 |
| `compatibility/state-preserving-source-transitions.json` | 两个精确 native/source 方向例外，用于保存状态恢复；不是任意版本通行证 |
| `compatibility/training-validation-transitions.json` | 两个历史验证证据转换，另含 Git 身份条件；不是当前 HEAD 自动复用许可 |

## 校验及清理结论

在 Conda `DL` 中执行：

```powershell
conda activate DL
python tools/check_training_configs.py
```

检查覆盖 TOML 的 PPO/model/profile、运行输出及训练配置中评估区间，以及 JSON 的已知 schema、配置规范化哈希、开发/保留最终区间一致性和交叉泄漏、证据路径、转换摘要格式与重复记录。检查是只读的；通过不代表模型文件齐全、历史配置符合当前运行身份或可以直接提交。正式训练仍须专门的 preparation / preflight / checkpoint 验证。

本次未发现可安全删除的配置或本地产物。没有在当前代码下重跑历史实验，也没有更改 reward、网络、PPO 或模拟器语义。已有训练提交的哈希保持原样；未来训练必须重新完成当前代码验证，不能把旧哈希改成当前值来绕过契约。

本地验证：25/25 配置通过；配置检查、Win 延续分析、训练证据复用和 runtime rebind 的相关测试 48 项通过；修改的 Python 文件通过 Ruff。25 份 TOML/JSON 与本地 HEAD 的规范化内容逐一一致，native 与 training implementation 摘要保持此次整理前的值。完整逐文件 SHA256 清单保存在忽略的 `local/reports/project-reset-20261005/configs-review.json`，不随项目上传。
