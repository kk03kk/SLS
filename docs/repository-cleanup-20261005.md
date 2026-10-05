# 项目文件整理结案（2026-10-05）

本轮完成剩余目录及全仓库上传边界复核。正式源码、测试、配置、依赖锁、CI 和文档保留；历史 champion、90M/76M 候选、原始服务器包和审计材料保留在本地。没有重建远端仓库，也没有启动下一轮训练。

## 各文件夹的处理

| 目录 | 内容与处理 |
| --- | --- |
| `src/`、`native/` | 正式核心实现已整理。模拟器身份不变；Oracle 从零构建源码与模式契约已补齐。见 [源码](source-guide.md)、[native 初次清单](native-guide.md)、[Oracle 当前说明](../native/oracle/README.md) |
| `tools/` | 入口和历史工具保留。此次修正 bootstrap 安装测试选择，见 [工具清单](../tools/README.md) |
| `configs/` | 保留当前/历史配方、计划和兼容契约；25 项检查通过，未替换旧来源身份。见 [配置说明](../configs/README.md) |
| `tests/` | 保留测试、必要 fixture 和 Oracle harness；生成缓存移到本地恢复位置。见 [测试说明](../tests/README.md) |
| `docs/` | 增加入口、逐文件索引、当前仓库地图和本地产物保留规则；修正安装、Oracle、70M/90M 和 Act1-2 状态说明；日期审计和机器证据保留。见 [文档索引](INDEX.md) |
| `requirements/` | 两个依赖锁保留，增加平台与运行身份说明；官方 PyPI 31 项版本存在，Windows/Linux 声明约束无冲突。没有升级锁或重新安装整套依赖。见 [说明](../requirements/README.md) |
| `.github/` | 保留模板和 Linux CI。消除完整测试重复执行，增加 content registry 检查。见 [说明](../.github/AUTOMATION.md)；使用 AUTOMATION 文件名避免遮住 GitHub 首页的项目 README |
| `model/` | 只公开 README；三个权重重新计算哈希，与身份记录一致，56M champion 未覆盖。见 [模型清单](../model/README.md) |
| `runs/`、`local/` | 原包、训练状态、用户依赖、来源与审计证据本地保留；不上传。不将整个 build 或旧审计目录判为垃圾。见 [详细清单](local-artifacts.md) |
| 根目录 | 正式入口、许可证、包配置及 Git 规则保留。打包生成的 build/egg-info 移入 local；新增根压缩包、权重、wheel、环境文件忽略规则。人类维护的 AGENTS 未改 |

## 确认并修复的问题

1. 不装 Torch 的 bootstrap 曾收集依赖 Torch 的全套测试。现在 simulator-only 路径运行明确的无 Torch 测试集合；模型安装仍运行全套。新增两个行为回归测试。
2. CI 的 bootstrap 与独立 pytest 步骤重复运行全套。新增 `--skip-tests` 后，CI 安装/构建与测试各执行一次。
3. 当前使用文档残留“必须有基础 Oracle JAR”、等待 90M、旧阶段为当前状态等描述。已修正当前入口；历史快照通过日期说明和链接界定，没有重写原结果。
4. 根目录新下载包和零散模型缺乏额外忽略防线。补充 Git 规则，发布候选未出现本地训练包、权重或游戏 JAR。

## 本地验证

在 Windows Conda DL / Python 3.12 执行：

| 检查 | 结果 |
| --- | --- |
| 全套 pytest | **1053 passed，1 skipped，4 warnings，218.08 秒** |
| 禁止导入 Torch 的 simulator-only 实测 | **134 passed** |
| Ruff | 通过 |
| 训练配置及契约 | 25/25 |
| 注册表、策略词表生成一致性 | 通过 |
| wheel 构建与仓库外隔离加载 | 通过；包含两个 content JSON 和三个 vocabulary JSON |
| 正式 Markdown 本地链接 | 无缺失 |
| 有 manifest 的结果文件 | 27 个文件哈希匹配 |
| 本轮前后日期审计/结果文件 | 原有文件未改动 |
| Git diff whitespace | 通过；普通文本有平台换行提示，固定原始证据的 -text 规则保留 |

唯一 skip 是历史模型不兼容当前编码、按设计拒绝，不能冒充已复现。四个 warning 来自允许的 checkpoint runtime/provenance rebind，不代表跨 runtime 位级重放已证明。GitHub workflow 和 Linux sanitizer 本轮没有远端执行；依赖元数据核验也不等于全新 Windows/Linux 环境安装实测。

此次安装/文档/CI 整理没有改变 observation、action、reward、PPO、模型或 simulator semantics。native `fe354a23…`、implementation `04fa179d…`、training validation `36ba6dbc…` 与本轮整理前一致，未新增迁移例外。此前 Oracle 和 seed-audit 修复分别见其独立记录。

全部上传候选的路径、大小和 SHA256 保存于 `local/reports/project-reset-20261005/upload-file-manifest.json`；测试 XML、依赖元数据、最终检查和整理前后盘点同目录保留。这个清单来自工作树，没有 stage、commit、push 或修改 Git 历史。后续新仓库首次提交应使用这些正式文件，并单独处理模型分享与训练备份。
