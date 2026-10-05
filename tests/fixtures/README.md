# 测试数据

这些是项目自有的小型结构化输入及回归证据，均应上传 GitHub。JSON/gzip 不是训练日志缓存；没有游戏 JAR、反编译 Java 或模型权重。原路径被测试、server preflight 引用，整理时保留文件字节和路径。

| 文件 | 内容、使用者与保留理由 |
| --- | --- |
| `act1-transform-selection-stock.json` | stock JAR 摘要和变换牌过滤选择的少量种子/结果；`simulator/test_transform_selection.py` 使用。有限受控探针结果，不是全部变换流程 parity 认证 |
| `regressions/act1-note-seed-3000000000025.json` | Note for Yourself 的 seed 与动作序列；`test_act1_training.py` 用于 AUTO_LEAVE 行为回归 |
| `regressions/act1-note-seed-3000000000047.json` | 另一路 Note for Yourself 状态及动作序列；与前一文件覆盖不同分支，保留 |
| `regressions/nus-worker-23-seed-8335-invalid-decision.json` | 真实服务器 worker 23 的故障状态、合法动作、RNG 和 profile；simulator 测试及 `tools/preflight_training.py` 重放，必须保留 |
| `regressions/original-purity-multi-select.json.gz` | Purity 多选流程的 simulator 恢复状态、动作与原版来源证据；用于原版合法动作筛选和状态重放回归 |
| `regressions/potion-during-card-choice.json.gz` | 选牌过程中使用药水的恢复锚点、动作和证据；防止状态或合法动作转换退步 |

这些 fixture 不是保留评估集的自然开局抽样，不用于宣称胜率。模拟器状态含 RNG 是为了受控重放；并不允许策略 observation 读取隐藏信息。此次检查六份文件均可解码，原内容未改写，哈希清单保存在本地测试目录整理记录中。
