# SLS — 用强化学习学习《杀戮尖塔》

SLS 的目标是训练一个能够自主游玩 **Slay the Spire 1** 的智能体，也让人通过观察它的决策理解它学到了什么。项目使用 C++ 模拟器生成游戏交互，使用带循环记忆的策略网络和 PPO 训练，并提供连接本地游戏的模型观察界面。

**当前开发目标：战士（Ironclad）A0 Act1，从全新模型训练，击败第一幕 Boss 即结束并计为胜利。** 模拟器也支持更长的流程；这不代表目前已得到成熟的高胜率通关模型。

没有游戏、GPU 或 checkpoint 也能运行模拟器。观察已训练模型需要另外准备兼容的模型文件；连接真实游戏还需要自己的游戏和 Mod 环境。

## 1. 安装与构建

支持 **Windows 和 Linux**，建议使用 **64 位 Python 3.12**（项目最低版本为 3.12）。macOS 暂无受支持的 native 构建流程。

- 安装 Git 和 Python，或使用 Conda 创建 Python 3.12 环境。
- Windows 构建脚本会下载固定版本的 Zig、CMake、Ninja 和 pybind11，无需手工配置 Visual Studio 工程。
- Linux 需要 C++ 编译器和 Python 开发头文件；Ubuntu 可先安装 `build-essential`、`python3.12-dev`、`python3.12-venv`。
- 首次安装需要联网下载依赖。CPU 可用于开发、测试和少量推理；正式长训练建议使用 GPU。

以下命令均在仓库根目录执行。

### Windows（PowerShell）

```powershell
git clone https://github.com/kk03kk/SLS.git
cd SLS
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python tools/bootstrap.py --with-model
```

若 PowerShell 不允许执行激活脚本，可以直接用 `.\.venv\Scripts\python.exe` 替代后续命令中的 `python`。使用 Conda 时，激活自己的 Python 3.12 环境后执行相同的 bootstrap 命令即可。

### Linux（Bash）

```bash
git clone https://github.com/kk03kk/SLS.git
cd SLS
python3.12 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python tools/bootstrap.py --with-model
```

bootstrap 会安装锁定依赖、以 editable 方式安装 SLS、构建 native 并运行测试。仅开发模拟器时可省略 `--with-model`；依赖 PyTorch 的测试可能被跳过。`--skip-native` 只适用于已有构建或暂时仅处理 Python 代码的情况。

## 2. 跑通模拟器，不需要模型

将下面代码保存为仓库根目录的 `quickstart.py`，然后执行 `python quickstart.py`：

```python
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_ACT1

backend = SimulatorBackend(IRONCLAD_A0_ACT1)
decision = backend.reset(seed=42)
print("当前界面：", decision.observation.screen.value)
print("Neow 公开奖励与代价：")
for option in decision.observation.event_options:
    print(option.content_id, dict(option.properties))

# 这里只演示执行一个合法动作，并不是智能策略。
action = decision.actions[0]
decision = backend.step(action).decision
print("执行后界面：", decision.observation.screen.value)
print("后续合法动作数：", len(decision.actions))
```

模型通过结构化 Observation 接收玩家当时可见的信息，并从合法动作中选择。事件里已经展示的目标卡牌、遗物、药水、金额和风险会进入输入；未知奖励和隐藏随机结果不会提前提供。

## 3. 使用模型并保存玩法日志

仓库不附带预训练权重。`local/runs/` 保存训练 checkpoint，`model/` 保存导出的独立策略，两者均被 Git 忽略。

**当前输入编码为 `sls-policy-input-v5`。旧 13M / 15M 等历史模型不能直接当作当前模型加载，也不能靠改版本号绕过检查。** 后续计划是从零训练。以下示例要求已有当前编码兼容、目标匹配的 checkpoint；请替换为自己的路径。

导出 A0 Act1 策略：

```bash
python tools/export_policy.py local/runs/my-act1/latest.pt --output model/my-act1.pt --ascension-min 0 --ascension-max 0 --goal ACT1
```

在模拟器中跑一个 seed 并保存轨迹：

```bash
python tools/capture_policy_trajectory.py simulator model/my-act1.pt --seed 42 --output local/reports/seed-42.jsonl
```

首次试用可添加 `--max-actions 20` 限制决策次数。轨迹用于检查决策、Observation 和循环记忆上下文。列出本地导出模型：

```bash
python tools/play_live_inspector.py --list-models
```

## 4. 可选：连接真实游戏，观察智能体

需要自己的 **Slay the Spire 1、ModTheSpire、BaseMod、CommunicationMod**，以及本项目的 Observation Oracle。游戏 JAR、Mod JAR、存档和模型不会随 Git clone 下载。

目前仓库保存了 Oracle 的观测补丁源码，构建工具还需要已有的基础 `SpirecommParity.jar`；**仅克隆源码尚不能从零构建完整实机 Mod 环境**。没有这些本地文件时，请先使用模拟器流程。

已有基础 Oracle 和游戏依赖时，构建当前补丁：

```bash
python tools/build_observation_oracle.py --javac /path/to/jdk/bin/javac --source /path/to/SpirecommParity.jar --game-libs /path/to/game-libs
```

将路径换成实际路径。`game-libs` 需包含 `desktop-1.0.jar`、`CommunicationMod.jar`、`ModTheSpire.jar`；JDK 需支持 `--release 8`。输出默认为 `local/build/oracle/SpirecommParity-observation-v4.jar`。在 Mod 环境中使用更新后的 Oracle，避免同时加载重复版本。当前补丁已完成编译验证，新增事件信息尚未完成新一轮实机联机验证。

Windows 下，先让 CommunicationMod 生成配置，再在激活的 Python 环境中执行：

```powershell
python tools/configure_live_inspector.py
```

工具会备份原配置，并让 CommunicationMod 使用当前 Python 环境启动 inspector。非默认配置位置可用 `--config` 指定，其他参数见 `--help`。

随后：

1. 通过 ModTheSpire 启动相应 Mod。CommunicationMod 启动 inspector，浏览器打开本机 `127.0.0.1:8765`。
2. 选择兼容的导出模型，点击 **Load and connect to game**。
3. 创建与模型目标匹配的全新战士局，在 Neow 阶段连接。
4. 使用 **Single step** 逐步观察，或使用 **Run** 连续运行；也可以暂停、调整延迟或手工选择动作。

界面展示合法动作概率和状态价值估计。动作概率不是通关概率，价值头也不是各动作的 Q 值。Act1 模型使用 inspector 路径；普通 `play_live.py` 入口仅接受 FullRun / Heart。项目不支持随意打开中途存档并凭空恢复模型的历史记忆。

日志与更多操作见 [实机运行说明](docs/local-runtime.md)。

## 5. 开发、测试与训练

修改 native 源码或切换 Python 版本后重新构建；内存紧张时降低 `--jobs`：

```bash
python tools/build_native.py --jobs 4
python -m pytest -q
python -m ruff check src tools tests
python tools/generate_policy_vocabulary.py --check
```

正式训练入口是 `tools/train_full_run.py`，配套工具包括 preflight、worker benchmark、checkpoint 兼容性检查、评估和 Slurm 提交。接口见各工具的 `--help`。

`configs/train/` 和 [10M→15M 训练方案](docs/training-10m-15m.md) 对应历史 FullRun 实验，**不是新版从零 Act1 训练的一键配置**。新实验需要准备目标与当前编码一致的配置，并在新 native 环境完成 preflight；需要时重新做 worker benchmark。NUS 操作见 [服务器指南](docs/nus-training-zh.md)。

## 项目结构

| 目录 | 内容 |
| --- | --- |
| `src/sls/` | 公共协议、环境适配、模型、PPO、实机运行与诊断 |
| `native/simulator/` | C++ 模拟器和 Python binding |
| `native/oracle/` | 原版游戏公开观测补丁源码 |
| `configs/` | 训练配置和兼容性记录 |
| `tools/` | 安装、构建、训练、导出、评估和日志工具 |
| `tests/` | 自动化回归测试 |
| `docs/` | 架构、操作说明与审计记录 |
| `local/` | 本地构建、外部依赖和运行产物，不提交 Git |
| `model/` | 用户自己的导出策略，不提交模型权重 |

## 常见问题

- **无法导入 native / 要求 rebuild**：确认使用正确 Python 环境，在仓库根目录重新运行 `python tools/build_native.py`。
- **模型 schema 不匹配**：使用兼容模型；迁移必须显式验证，不能将旧权重误当作 exact resume。
- **缺少公开事件信息**：更新模拟器或 Oracle，不能静默丢弃字段继续推理。
- **找不到 checkpoint / 游戏 JAR**：这些是本地产物或外部依赖，仓库不附带。
- **只有 CPU 能否使用？** 可以运行模拟器、测试和少量推理；训练速度需在自己的硬件上实测。

进一步阅读：[架构](docs/architecture.md) · [仓库地图](docs/repository-map.md) · [事件修复及验证边界](docs/event-observation-repairs.md)
