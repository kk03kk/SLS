# 仓库地图

当前阶段以 [90M 结案](results/win90m-20261005/README.md) 为准。固定 90M 是下一阶段建议的 parent，76M 是周期选择候选，56M 历史 champion 保留。Act1-2 试训尚未绑定或提交。源码 Oracle 已恢复；游戏、Mod 和训练权重另行提供。

| 目录 | 内容 | GitHub |
| --- | --- | --- |
| `src/sls/` | Python 协议、内容、模型、PPO、运行与审计实现 | 上传正式源码和包内 JSON |
| `native/` | C++ 模拟器、Oracle Java 源码及资源、来源与许可证 | 上传；不含游戏源码或 JAR |
| `tools/` | 构建、训练、评估、导出、校验命令 | 上传 |
| `configs/` | 当前和历史训练配置、计划、兼容契约 | 上传，旧身份保持原样 |
| `tests/` | 自动测试、最小复现 fixture、Oracle harness | 上传；私有材料缺失应明确 skip |
| `docs/` | 使用说明、设计、日期审计、紧凑结果证据 | 上传；逐文件见 [索引](INDEX.md) |
| `requirements/` | 开发与模型依赖锁 | 上传 |
| `.github/` | CI 与 PR 模板 | 上传 |
| `model/` | 历史 champion、90M/76M 导出及身份 JSON | 只上传 README；权重另行分享 |
| `runs/archives/` | 原始服务器包、历史产物 | 本地保留，不上传 |
| `local/` | 构建、训练解包、审计与日志、用户依赖 | 本地保留，不上传 |

根目录正式文件为 README、CONTRIBUTING、LICENSE、AGENTS、pyproject 及 Git 配置。AGENTS 是人类维护的长期规则，不存放本轮进度。忽略规则防止根目录训练压缩包、权重、wheel、环境文件误上传。

新克隆包含可构建源码、配置、测试及文档，不包含预训练模型、游戏或 Mod。见 [安装](friend-quickstart.md)、[模型分发](model-release.md)、[本地产物保留规则](local-artifacts.md)。

历史审计属于日期证据。旧文档中的“当前”、未执行计划或未验证假设不自动适用于 HEAD。尤其跨种子块约 3.4pp 差异未获合法统计检验确认；最新结果和纠正以根 README 与相应结案记录为准。
