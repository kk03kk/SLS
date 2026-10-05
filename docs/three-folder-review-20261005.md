# src、native、tools 整理后的修复记录

日期：2026-10-05。范围是目录整理及证据明确的维护问题修复，不是一次完整算法/模拟器保真认证。

后续状态：同日已找回 Git 历史中的完整 Oracle 源码，完成独立构建、v11 模式修复和有限实机验证。本文后面的“基础来源不完整”是修复前状态，不再是当前限制。见 [Oracle 恢复记录](results/oracle-source-recovery-20261005/README.md)。

## 文件整理

`src/` 保留 71 个正式文件；第一次整理清除 131 个 Python 缓存。`native/` 的 87 个文件均为正式源码或构建/来源记录，没有需要删除的生成产物。`tools/` 保留 64 个正式文件（61 Python、2 Java、1 README），清除 74 个缓存文件、938,363 字节。

工具目录的逐文件说明与分类已更新到 [tools/README.md](../tools/README.md)。历史工具保留原路径，以维护复算、文档引用和身份契约；没有把旧固定提交器当作下一阶段推荐入口。目录内未发现其他未跟踪产物。

## 确认并修复的问题

| 问题 | 修复 | 验证与边界 |
| --- | --- | --- |
| `model/transformer.py` 顶部仍写 input v3 | 改为实际使用的 v5 | 仅模块说明；没有改变字段、权重布局或模型计算 |
| `train_full_run.py` 说明仍限定 A0；旧 warm-start 说明误写当前目标 v4 | 更新为 curriculum 训练及当前输入迁移 | 仅模块说明，不改变训练行为 |
| 两个原版启动器默认写死某台机器的游戏路径 | 使用 Steam 检测或明确 `--game-root`，提前检查游戏文件与受支持平台 | 显式路径、自动检测、缺失依赖与平台拒绝测试；不启动真实游戏 |
| canary 在解析帮助前读取 `LOCALAPPDATA`，无 Windows 环境时连帮助都报错 | 将运行前置条件检查移到参数解析后 | 清除环境变量后 `--help` 仍以 0 退出 |
| 卡牌启动器使用裸脚本名导入，模块方式不稳定 | 统一为 `tools.run_original_canary` 导入并设置项目根路径 | CLI 检查、模块导入与全量测试 |
| `build_full_audit_oracle.py` 可覆盖输入 JAR | 拒绝解析后相同的输入/输出路径 | 源文件字节保持不变的拒绝测试 |
| 上述工具在基础 JAR 缺少 allowlist 资源时仍可输出成功 | 写入前检查全部必需资源，缺失时失败；原输出保持完整 | 缺失资源且输出已有证据的回归测试 |
| 上述输出缺少来源哈希、替换方式不明确 | 记录输入/输出 SHA-256，暂存后使用同文件系统原子替换 | 内容替换、无关 class 保持一致、哈希检查 |
| 配置检查器 `--root` 指向仓库外时路径展示报错 | 根据路径是否属于仓库展示相对/绝对路径 | 仓库外真实临时配置检查通过 |
| 内容注册表生成器没有只读检查入口 | 增加 argparse 与 `--check`；生成时暂存后替换 | 当前注册表完全匹配；陈旧注册表检查不写文件 |
| 工具 README 混合旧状态与旧启动建议 | 改为全文件索引，区分常规、校验、原版、历史和待父实验绑定工具 | 覆盖全部 61 Python 与 2 Java 文件 |

之前已修复的 CI 跨平台测试问题保留：`test_configure_live_inspector.py` 仅在 Python 路径包含 Windows drive 时检查 `\:`。本轮仍通过该测试。没有推送新 CI，不能据此声称 GitHub Actions 已通过。

## 身份与兼容性

训练身份覆盖的三处文件只修改模块 docstring，不修改可执行逻辑。对 HEAD 与当前文件去除模块说明后逐一比较 Python AST，三份可执行语法树完全一致；证据保存于本地 `docstring-transition.json`。observation/action/reward、curriculum、模型架构、参数、checkpoint schema 与模拟器行为均未改变，因此不增加语义版本或模型迁移规则。

严格源码哈希包含说明文字，因此它们发生变化，并记录如下：

```text
原 training implementation:
a4d23e8bea1996e2148660924323317db633dcfc5657b1c71b4a98b7269ac6de
新 training implementation:
04fa179d6bc36cd3c595c9c9639c3112546bbb343bc06f3b8c9c3dab88f265e7
新 training validation:
36ba6dbcdac3d7d5f1c2c52e8bbc2166dbc8578b7184bd42d594f3a73235f3db
native（保持不变）:
fe354a23c7584d68d0a2b6681b8dfd4e97d98e107b7ddf57c479060d91468f39
```

没有添加旧验证复用许可或精确续训豁免；没有修改固定 70M/90M 配置中的来源哈希，也没有重写旧 checkpoint 身份。正在服务器执行的版本不受本地修改影响。未来新的 Act 1–2 准备必须绑定当前源码并产生新验证证据，不能套用旧实验准备记录。

原始目录快照继续保留在本地：`src-review.json`、`native-review.json`、`tools-review.json`；维护后的身份及清理清单为 `three-folder-maintenance.json`，最终工具快照为 `tools-review-final.json`，均位于 `local/reports/project-reset-20261005/`。原始快照不改写成“当前结果”。

## 验证结果

- Conda `DL`：`python -m ruff check src tools tests` 通过。
- `python -m pytest -q`：1031 passed、1 skipped、4 warnings。跳过项依赖本地导出的 Act 1 策略；warnings 来自测试中的显式 runtime/provenance rebind，不代表跨 runtime 位级一致性。
- 全量测试后新增的 `tests/test_tool_checks.py`：2 passed；覆盖外部配置路径与注册表只读失败路径。
- `python tools/check_training_configs.py`：20/20 通过；只代表结构检查，不表示旧配置应再次启动。
- `python tools/generate_content_registry.py --check` 与词表 `--check` 均通过，无数据重写。
- 初次 CLI 检查：59 个直接构造 argparse 的入口帮助均通过。最终检查包含新注册表参数入口与 Win90 包装器，61/61 帮助均通过；结果保存在 `tools-review-final.json`。
- native 87 个正式文件与首次清单逐字节一致；本轮未重新编译 C++、Oracle，未启动原版游戏或服务器任务。

## 仍需补齐的事项

**基础 Oracle 的从零构建来源仍不完整。** 仓库里的两个 Java 补丁和两个 JAR 修改工具依赖已有基础 Oracle，不能凭空还原完整实现。本轮没有找到足够来源证据来安全补齐，因此明确保留这一限制。以后整理外部依赖与原版验证产物时继续核实来源，不能将有本机旧 JAR 等同于新 clone 可复现。

**native 桥接文件较大，工具入口较多。** 这是维护成本，不是已确认的运行错误。暂不拆分：拆分需要独立依赖检查、身份更新和验证，并不提高本轮训练效果。

**模拟器完整保真与研究假设仍需独立证据。** 当前回归通过不证明所有 Act 2/3/Heart 行为一致，也不证明 reward、GAE 或网络已经最优。这些应在完成父实验证据分析后按实验方案验证，不能借目录清理同时改变多个训练变量。

`AGENTS.md` 未被修改；本轮没有提交、推送、重建 Git 或删除远程仓库。
