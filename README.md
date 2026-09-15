# SLS — 用强化学习学习《杀戮尖塔》

SLS 的目标是训练一个能够自主游玩 **Slay the Spire 1** 的智能体，也让人通过观察它的决策理解它学到了什么。项目使用 C++ 模拟器生成游戏交互，使用带循环记忆的策略网络和 PPO 训练，并提供连接本地游戏的模型观察界面。

**当前目标：战士（Ironclad）A20 Act1，击败第一幕 Boss 即胜利。** A20 30M 阶段已完成：27,000,832-step champion 的独立 1024-seed 胜率为 54.10%。下一轮建议从该 champion 保留训练状态、减半 LR，续训至累计 40M。结果、同 seed 原版验证和方案见 [A20 30M 审计](docs/a20-30m-audit.md)。

找文件时先看 [项目目录索引](docs/repository-map.md)：下载的服务器压缩包统一放在 `runs/archives/`，运行中的 checkpoint 放在 `local/runs/`，开发验证记录放在 `local/audits/` 和 `local/logs/development/`。这些本地产物不提交 Git。

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

**当前输入编码为 `sls-policy-input-v5`。旧 13M / 15M 等历史模型不能直接当作当前模型加载，也不能靠改版本号绕过检查。** 5M 实验从零训练完成；Act1 环境规则版本为 4，编码仍为 v5。以下示例要求已有当前编码兼容、目标匹配的 checkpoint；请替换为自己的路径。

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

将路径换成实际路径。`game-libs` 需包含 `desktop-1.0.jar`、`CommunicationMod.jar`、`ModTheSpire.jar`；JDK 需支持 `--release 8`。输出默认为 `local/build/oracle/SpirecommParity-observation-v4.jar`。在 Mod 环境中使用更新后的 Oracle，避免同时加载重复版本。当前补丁已完成编译和重点事件实机对照；覆盖范围及开训条件见 [Act1 环境收尾验证](docs/act1-environment-closeout.md)。

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

### 当前 A20：发布后在 NUS 操作

先确保服务器已拉取包含 A20 配置的提交，且原 30M run、`local/runs/ironclad-a20-act1-v1-30m/stages/train/selection/best_progress.pt` 及原 preparation benchmark 存在。入口会验证其 SHA256；不要用 final 替代。

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a20_act1_40m.toml --prepare
```

准备流程在 compute node 构建 native、验证环境兼容和保存恢复，全部通过才训练。保留已测量的 64 workers / 8 shards。新输出目录为 `local/runs/ironclad-a20-act1-v1-40m/`；保留旧实验。初始化继承 champion 的网络、Adam、RNG、worker 状态和 best，LR 改为 0.00003125。这是新的 continuation 分支，不是原 30M 实验的 exact resume。

### 历史 A0 流程

下面保留 5M/10M/20M 实验说明用于追溯，不是当前 A20 的启动入口。旧环境 checkpoint 的 exact resume 需要匹配其环境版本，不能在新版代码下绕过 contract。

从零 A0 Act1 实验使用 `configs/train/ironclad_a0_act1_5m.toml`，预算 5M steps。它是独立单阶段训练，不需要历史 FullRun checkpoint、warm-start 或 smoke/pilot 晋级。

### 历史 A0 5M：提交示例

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a0_act1_5m.toml --prepare
```

入口会在 Slurm compute node 检查 native、需要时构建，运行实际网络 preflight、短 benchmark 和 worker 保存/恢复验证，然后开始训练。默认 1×A100 40GB、16 CPUs、64GB RAM。不要在 xlogin 上运行构建、评估或训练。首次缺少 PyTorch 时准备入口会安装模型锁定依赖；已有环境不会为 GPU 名称变化强制升级依赖。

benchmark 比较 32/64/128 个环境的完整“采样＋PPO 更新”耗时，选择接近最快的较小配置并固定。中断后执行**同一条提交命令**恢复；有效准备结果自动复用。训练配置、Observation、PPO 或环境语义不兼容仍会拒绝恢复，不能删除旧 checkpoint 后假装续训。运行目录需保持完整。

结果在 `local/runs/ironclad-a0-act1-v4-5m/`：

- `stages/train/metrics.jsonl`：训练指标、固定 seeds 评估、逐 seed 胜负变化和失败摘要。
- `latest.pt`、`checkpoint-steps-*.pt`、`final.pt`：恢复 checkpoint；约每 0.25M 保存一次。
- `stages/train/selection/best_progress.pt`：每 0.5M、固定 512 seeds 按通关数选择的 best，同分保留更早模型。
- `final-evaluation.json`：best 的独立 1024 seeds 结果。
- `ironclad-a0-act1-v4-5m.pt`：实验完成后的独立策略，可复制到本地 `model/` 使用。导出不要求高胜率；模型质量以评估结果为准。

间隔与 5M 目标均在完整 PPO update 边界执行，因此实际 step 数可能略高于标称值。配置、完整步骤与失败处理见 [Act1 训练说明](docs/training-act1-5m.md)。历史 FullRun 配置与 [旧服务器指南](docs/nus-training-zh.md) 保留供追溯，不作为本轮启动流程。

### 已完成 5M 实验后的续训

[5M 审计、100-seed 完整诊断与下一轮方案](docs/act1-v4-5m-audit.md) 指定了 4,505,600-step best。已有对应服务器产物时，可提交：

```bash
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a0_act1_10m.toml --prepare
```

这份配置绑定该 best 的 SHA256，并在新目录创建半学习率分支；原模型、Adam moments、RNG 和 worker 状态保留，旧实验不修改。准备阶段验证实际恢复 checkpoint，复用原 worker 布局。它不是任意 checkpoint 的通用迁移命令，也不是重新训练一个独立 10M。新终评 seeds 与本次诊断集合不重叠。

### 已完成 10M：继续到累计 20M

使用 7,766,016-step champion（不是 final），LR 从 `0.000125` 减半到 `0.0000625`，其他 PPO、网络和奖励不变。固定 512-seed 评估约每 1M 一次；best 最终使用新的 1024 held-out seeds。模型、Adam、RNG、worker 状态和历史 best 保留，输出到独立的 `local/runs/ironclad-a0-act1-v4-20m/`。

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a0_act1_20m.toml --prepare
```

保留原 5M/10M 目录及准备目录中的 benchmark。入口先验证 champion 和恢复链路，通过后才开训；已有合法 20M latest 时恢复。参数依据、seed 范围和验证说明见 [20M continuation](docs/act1-v4-20m-continuation.md)。

### 与普通原版局的区别

- Act1 Boss 击败立即结束；使用指定 seed 的完整 Neow 开局。
- 棱彩碎片照常生成、展示，禁止购买/领取；不替换、不重抽。
- 给自己的纸条照常出现，但自动选择离开，不读写训练 worker 的跨局存牌。
- 钥匙可正常选择并支付真实代价；Act1 不提供额外钥匙奖励。
- 折叠纯确认 UI，因此模型决策步数不等于鼠标点击数。

没有为了提高胜率简化战斗、路线或其他事件。当前验证是针对 Act1 的源码、回归与实机对照，**不代表所有 seed、所有分支已获得完全 parity 证明**。详见 [环境收尾证据](docs/act1-environment-closeout.md)。

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
