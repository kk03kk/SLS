# 项目文档入口

当前 Act1 Win 训练已到 90,013,696 decisions。固定 90M 在同一批 2,048 个开发确认种子上为 1640/2048，70M 为 1574/2048；这是模拟器开发结果，最终保留集尚未使用。历史 56M champion 保留，90M 和周期所选 76M 分开保存。见 [90M 结案](results/win90m-20261005/README.md)。

Oracle 已有完整仓库内源码构建路径，仍需用户提供原版游戏及 Mod 依赖，见 [Oracle 说明](../native/oracle/README.md)。Act1-2 的4M试训现已完成：冻结90M与固定94M在配对1024开发种子上联合通关6/3局，前段能力明显退化，未达到延长判据。见 [本轮结果与下一阶段规划](results/act12-pilot-20261006/README.md)；[试训说明](act12-pilot-launch.md)保留原设计。

| 需要了解的内容 | 入口 |
| --- | --- |
| 给同学安装、运行 | [快速开始](friend-quickstart.md)、[本地运行](local-runtime.md) |
| 各文件夹用途和上传范围 | [仓库地图](repository-map.md)、[本地产物](local-artifacts.md) |
| 本轮整理与完整验证 | [整理结案](repository-cleanup-20261005.md) |
| 每一份文档的位置与类别 | [逐文件索引](INDEX.md) |
| 核心实现 | [Python 源码](source-guide.md)、[Native](native-guide.md)、[架构](architecture.md) |
| 测试范围与限制 | [测试说明](../tests/README.md) |
| 配置与工具 | [配置](../configs/README.md)、[工具](../tools/README.md) |
| 训练与评估证据 | [90M](results/win90m-20261005/README.md)、[70M](results/win70m-20261001/README.md)、[Reward 筛查](results/plateau-reward-screen-20260930/README.md) |

`audits/`、`history/`、带日期的报告和旧 launch 文档保留当时的发现与决策，不能代替当前源码或复算证据。`results/` 中原始 JSON、manifest 和脚本承担可追溯作用，不为排版而改动其字节或把旧结果改名为当前结果。

修复版Act1+Act2匹配λ pilot已完成本地准备，见[唯一新启动设计](results/act2-qualification-20261006/TRAINING_READY.md)。旧launch文件保留历史身份。
