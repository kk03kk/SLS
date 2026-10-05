# Python 源码目录说明与整理记录

本次整理日期：2026-10-05。范围仅为 `src/`，不是整个项目的清理结论。

本文记录首次整理快照。同日后续修复已更正模型顶部的旧版本说明，并更新严格源码身份；当前状态见 [三个目录复查](three-folder-review-20261005.md)。下方“未修改”与哈希描述指首次整理，不是后续修复状态。

同日 Oracle 完整恢复后，`backends/original/live.py` 新增 v11 production 接入要求，`runtime/live_setup.py` 同步检查模式契约。策略输入与模拟器训练身份保持不变；见 [Oracle 恢复验证](results/oracle-source-recovery-20261005/README.md)。

`src/sls/` 是正式 Python 包，包含环境接口、策略输入、网络、PPO、评估、模型身份和原版游戏接入。实际游戏状态推进的 C++ 引擎在 `native/`，命令行入口在 `tools/`，实验参数在 `configs/`，回归检查在 `tests/`。仅复制 `src/` 不能构成可运行的完整项目。

## 上传与清理结论

- 保留并上传全部 71 个正式文件：66 个 Python 文件、5 个 JSON 包数据，总计 801,990 字节。
- 清除 131 个自动生成的 Python 字节码缓存文件，共 1,531,676 字节，以及清理后为空的 `__pycache__/` 目录。缓存可由 Python 重新生成，不是模型或实验记录。
- 未删除、移动或修改任何正式源码和词表；未修改 observation、action、reward、模型或模拟器语义。
- `audit/`、`diagnostics/`、原版接入、旧词表与迁移模块均保留。它们是可复用代码或兼容性资产，不是历史运行垃圾。
- 已检查目录中未跟踪文件：除上述缓存外，没有发现其他未跟踪产物。定向搜索个人绝对路径、密码和 API key 相关字样未发现对应源文件匹配；这不等同于完整安全审计。

## 核心调用关系

```text
tools/ + configs/ → rl/训练与评估
                         ↓
                    model/策略
                         ↕
       contracts/统一状态、动作与决策 + content/公共内容语义
                         ↕
        backends/simulator/ → native/C++ 游戏引擎
        backends/original/  → 本地原版游戏与 CommunicationMod

curriculum.py 定义训练终止范围；runtime/负责模型交付与现场运行；
audit/和 diagnostics/检查环境差异与策略轨迹。
```

最先理解 `contracts/`：两种环境和模型之间的数据边界由这里定义。理解训练方法再看 `model/` 与 `rl/`；理解游戏规则要继续看 `native/`。

## 每个子目录与文件

下列数量包含各目录的 `__init__.py`。初始化文件负责包导出；部分采用延迟导入，使内容与审计工具不必提前加载 Torch。

### 根部：2 个文件

| 文件 | 职责 |
| --- | --- |
| `__init__.py` | Python 包入口。 |
| `curriculum.py` | 角色、难度、自然开局分布、Act 1/2/3/Heart 等 episode horizon 与成功/终止判断。不是旧训练记录。 |

### contracts/：6 个文件，核心契约

| 文件 | 职责 |
| --- | --- |
| `__init__.py` | 统一契约导出。 |
| `observation.py` | 模型可以看到的公开游戏状态与实体。 |
| `action.py` | 语义动作类型和动作参数。 |
| `decision.py` | 决策边界、合法候选动作与 transition。 |
| `validation.py` | 差分验证专用状态，不能直接当作策略输入。 |
| `continuation.py` | 提取验证用的继续推进证据。 |

全部上传。这里的变动可能改变训练问题与模型兼容性，不能当作普通目录重命名处理。

### content/：11 个文件，共享公共语义

| 文件 | 职责 |
| --- | --- |
| `__init__.py` | 内容接口导出。 |
| `registry.json`、`registry.py` | 内容标识注册表及其加载、校验。 |
| `scope.json`、`scope.py` | 策略可见内容范围及其版本化接口。 |
| `normalize.py` | 将不同后端的内容 ID 统一。 |
| `card_features.py` | 卡牌公开且可变的特征。 |
| `energy.py` | 两个后端共享的公开能量语义。 |
| `neow.py` | 公开 Neow 选项语义，不预先抽取奖励结果。 |
| `event_options.py` | 已展示的事件选项信息，不暴露未揭示结果。 |
| `seed.py` | 游戏种子字符串与数值转换。 |

全部上传。两个 JSON 是安装包必需的数据；并非可随手删除的缓存，也不是原版游戏 JAR。

### backends/：11 个文件，环境接入

| 文件/目录 | 职责 |
| --- | --- |
| `__init__.py`、`protocol.py` | 统一环境 reset、step、验证快照和 checkpoint 接口。 |
| `simulator/__init__.py` | 模拟器后端导出。 |
| `simulator/environment.py` | 将 native 游戏状态映射到统一决策/状态，处理环境推进与保存恢复。 |
| `simulator/native.py` | 加载本机编译的桥接模块，默认从 `local/build/native/` 读取，也支持环境变量指定。 |
| `original/__init__.py` | 原版接入导出。 |
| `original/adapter.py` | CommunicationMod 消息与统一状态、动作之间的转换。 |
| `original/environment.py` | 原版游戏环境与 curriculum 终止逻辑。 |
| `original/live.py` | 接入已在运行的原版游戏。 |
| `original/session.py` | 用于原版差分验证的命令会话。 |
| `original/transport.py` | 按行传输 CommunicationMod 消息。 |

全部代码上传。原版接入是正式但可选的功能，同学只训练模拟器时不必启动原版游戏。本机 `.pyd`、服务器 `.so`、游戏安装与 Mod JAR 属于运行依赖，不应放入 `src/` 随源码上传。

### model/：7 个文件，网络与输入编码

| 文件 | 职责 |
| --- | --- |
| `__init__.py` | 模型导出与延迟加载。 |
| `encoding.py` | 字段、类别、引用关系、词表和输入版本；当前是 `sls-policy-input-v5`。 |
| `batching.py` | 将可变数量的实体和合法动作组织成模型 batch。 |
| `transformer.py` | Transformer、GRU 记忆、动作评分与 value 网络。 |
| `policy_vocabulary_v5.json` | 当前模型词表，正式包数据。 |
| `policy_vocabulary_v3.json`、`policy_vocabulary_v4.json` | 旧输入契约的词表，迁移时按语义映射参数并核验身份。 |

全部上传。这里的 `model/` 是网络源码；项目根目录的 `model/` 是另一处模型交付目录，不要混淆。v3/v4 属于历史兼容资产，但 `rl/model_migration.py` 仍直接读取它们，`pyproject.toml` 也将它们列为包数据，不能只保留 v5。

首次检查时 `transformer.py` 顶部说明仍写 input v3；实际版本由 `encoding.py` 与模型配置导入的契约决定，为 v5。同日后续修复已更正说明并记录严格哈希变化，未改变模型计算。

### rl/：13 个文件，训练、评估与证据

| 文件 | 职责 |
| --- | --- |
| `__init__.py` | RL 接口导出。 |
| `ppo.py` | PPO 更新与相关诊断。 |
| `rollout.py` | 采样数据与 GAE 计算。 |
| `workers.py` | 并行环境、分片与集中推理。 |
| `reward.py` | Progress/Win reward 身份及势函数 shaping。具体使用什么由训练配置和调用路径决定。 |
| `episode_limit.py` | 训练中的决策上限、循环边界判断。 |
| `evaluate.py` | 策略评估。 |
| `best_checkpoint.py` | 基于评估结果确定性选择 checkpoint 与保存元信息。 |
| `checkpoint.py` | checkpoint 写入、读取、精确恢复及兼容性检查。 |
| `training_contract.py` | 源码、配置、版本与训练来源的身份记录。 |
| `preparation.py` | 启动前共享的语义准备证据。 |
| `model_migration.py` | v3/v4 输入参数迁移，不恢复旧 optimizer 或运行中环境。 |
| `act1_transfer.py` | 跨 curriculum 的权重迁移，例如 Act 1 到 Act 1–2，同样不能冒充精确续训。 |

全部上传。Progress 实现与迁移代码的存在不表示下一阶段必须再做 Progress 对比；它们支撑已有实验的解释与复算。具体是否下线旧模式应另行做契约迁移，而不是这次清理顺手删除。

### runtime/：6 个文件，模型交付与演示

| 文件 | 职责 |
| --- | --- |
| `__init__.py` | 运行接口导出。 |
| `artifact.py` | 独立策略产物的保存、加载与身份校验。 |
| `controller.py` | 决策控制、动作评分与合法性边界。 |
| `inspector.py` | 浏览器查看状态、策略和交互控制。 |
| `live_setup.py` | 检查本地游戏、Mod、配置和模型前置条件。 |
| `window.py` | 打开本机浏览器查看器，含 Windows Edge 检测与浏览器回退。 |

全部上传。为本地演示编写的代码也是正式项目功能；游戏路径、用户配置、现场日志和权重才是本地产物。`artifact.py` 还参与训练身份契约，不能把整个目录当作可删除的演示附件。

### audit/：13 个文件，模拟器验证工具库

| 文件 | 职责 |
| --- | --- |
| `__init__.py` | 审计接口。 |
| `act1_targets.py` | 保守的 A20 Act 1 验证目标集合。 |
| `act1_encounters.py` | 原版与 native Act 1 遭遇表比较。 |
| `act1_map.py` | Act 1 地图公开结构的有限检查。 |
| `bytecode_inventory.py` | 基于本机原版 JAR 建立可追溯字节码清单。 |
| `card_parity.py` | 卡牌受控场景比较。 |
| `potion_parity.py` | 药水受控场景比较。 |
| `relic_parity.py` | 遗物回调受控场景比较。 |
| `encounter_parity.py` | 遭遇构造及首回合比较。 |
| `event_parity.py` | 事件首个决策边界比较。 |
| `mechanism_parity.py` | 共享战斗规则与机制比较。 |
| `stock_parity.py` | 原版校验清单、证据来源和状态分类。 |
| `semantic_coverage.py` | 语义覆盖义务校验，不将未检查的项目当作匹配。 |

全部上传。对应代码仍被 `tools/audit_*.py` 和 `tests/audit/` 使用。部分范围是 Act 1 或旧 A0 scope；保留并不表示已证明 A20 Act 2 全面保真。原版 JAR、反编译源码与大量现场采集输出不属于这个源码目录。

### diagnostics/：2 个文件，策略轨迹检查

| 文件 | 职责 |
| --- | --- |
| `__init__.py` | 诊断接口导出。 |
| `canary.py` | 记录公开输入、循环记忆和动作轨迹，逐决策比较差异。 |

全部上传。采集与比较入口在 `tools/`，有 `tests/diagnostics/` 回归检查；产生的轨迹 JSON 等输出应放实验/本地记录目录。

## 整理证据

逐文件大小、描述、清理列表与 SHA-256 存于本地 `local/reports/project-reset-20261005/src-review.json`。71 个正式文件清理前后的字节哈希完全一致；native 与 training implementation 身份也完全一致：

```text
native:   fe354a23c7584d68d0a2b6681b8dfd4e97d98e107b7ddf57c479060d91468f39
training: a4d23e8bea1996e2148660924323317db633dcfc5657b1c71b4a98b7269ac6de
```

已有 `.gitignore` 排除 `__pycache__/`、`*.py[cod]` 与 native 二进制。缓存日后再次出现是正常情况，不需要反复手动清理，更不应提交。

本地验证在 Conda `DL` 中执行：`python -m ruff check src` 通过；模型迁移、训练契约、模型、审计与诊断相关测试共 156 passed、1 skipped。跳过的是依赖本地导出 Act 1 策略的 seed audit，本次未执行该检查。测试禁用了 pytest cache provider，因此既有 `cache_dir` 配置产生一条 unknown option warning；不是源码测试失败。`AGENTS.md` 哈希与整理前一致。
