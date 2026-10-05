# SLS：训练《杀戮尖塔》智能体

文档入口：[使用与结果导航](docs/README.md) · [仓库地图](docs/repository-map.md) · [本地产物保留](docs/local-artifacts.md)。

SLS 用原生模拟器和循环策略网络训练智能体游玩 **Slay the Spire 1**。项目提供游戏状态与合法动作的统一协议、C++ 模拟器、PPO 训练流程、评估工具，以及在本地游戏中逐步观察模型决策的界面。

目前研究对象是**战士 A20 第一幕**。后续计划逐步延长至第二幕、第三幕和心脏；这些阶段尚未声称完成。项目还在开发中，模拟器与原版游戏的一致性已有针对性审计，但没有覆盖所有种子与分支。

## 当前模型与阶段结果

当前 Win 分支已完成 **90,013,696 decisions**，完整服务器归档已核验。相同 2,048 个新开发确认种子、相同 runtime 下，固定 90M 为 **1640/2048（80.08%）**，冻结 70M 为 **1574/2048（76.86%）**，净 +66、+3.22pp，配对 p=.002404，达到预注册开发成功判据。周期选择的 76M 为 1619/2048，与 90M 差异未获确认（p=.333）。见[90M 结案、模型身份和逐 Boss 结果](docs/results/win90m-20261005/README.md)。下一阶段建议以固定 90M 为 parent 进行正常开局 Act1-2 小规模试训，尚未提交。

90M 固定终点及 76M 周期所选模型已分开导出为开发候选，最终保留集仍封存；历史演示模型 **56,000,512 步 champion** 保留，不覆盖。历史 checkpoint SHA256 为 `9555c8608155ba262901757cd57d76f854f1cd76a5b6126375e832fa0a714710`。此前 [70M 结案](docs/results/win70m-20261001/README.md)的开发块不同，不能把两轮点估计直接作配对或相加。**以下为历史测量，跨环境、runtime 或种子差异不能直接归因于模型变化。**

| 测量 | 环境（native source SHA256） | 种子 | 结果 |
| --- | --- | --- | ---: |
| 本阶段终评 | `95e5b651…`（服务器训练环境，已退役） | `[7e12, +2048)` | 1,585/2,048 = **77.39%**（95% CI 75.53–79.15） |
| 当时仓库重测 | `1e30bb6c…`（重测时规则，非当前 HEAD） | 同一批 `[7e12, +2048)` | 1,569/2,048 = **76.61%**（95% CI 74.73–78.39） |

两次使用**完全相同的种子**，因此可以配对：胜→负 49 局、负→胜 33 局，净 −16，精确 McNemar p = 0.097。也就是说，这次历史配对的两种规则环境之间**没有显著变化**（点估计 −0.78pp）。运行错误、截断、超时、步数上限、循环上限在两次测量中均为零。

固定的 512 个选模种子上，46M、56M、最终 60M 分别通关 363、388、361 局，因此使用 56M。历史 46M 独立评估为 766/1,024（74.80%），与本轮终评使用不同种子，不能作配对比较。

**跨种子块的点估计需要不确定性。** 同一模型的 `[8e12,+512)` 为 375/512（73.24%），`[7e12,+2048)` 为 1,569/2,048（76.61%）。09-29 报告所称“p≈0.0014、8e12 系统更难”经独立复算被纠正：两块没有共享 seed，合法 Fisher p≈0.119（Pearson p≈0.111），尚未证明系统块效应。新服务器基线在同一 8e12 块为 374/512，说明评估 runtime 也要随结果登记。[阶段结果](docs/results/a20-act1-60m-stable/README.md)保留历史身份。

**2026-09-30：两组 2M reward 筛查完成。** Progress 与 Win 所选模型在开发确认集分别 392/512、394/512，配对 p=0.922；Win 的 78.52% 周期峰值没有稳定复现，不据此替换 champion。[完整复算、最佳 checkpoint 与逐 Boss 结果](docs/results/plateau-reward-screen-20260930/README.md)已归档。随后固定 Win objective，不再追加 reward A/B；58M→70M 作业现已完成，结论见上方与[70M 结案](docs/results/win70m-20261001/README.md)。

**以上都不是原版游戏胜率**，也不是任何其他环境的成绩。[2026-09-29 独立审计](docs/audits/2026-09-29-independent-audit.md)记录测量方法、环境与种子身份的核对结果、以及尚未修复的问题。

## 开始使用

2026-09-28 环境已有一次开发基线：56M 在新的 512 个开发种子上通关
375 局（73.24%）；它与历史终评使用不同种子，不能直接归因为环境退步。
两块点估计差约 3.4pp，但合法统计检验未确认系统块效应；不能归因为环境变化。
详见[平台期诊断结果](docs/results/act1-plateau-56m-v1/README.md)。

支持 Windows 和 Linux、Python 3.12 及以上。首次构建需联网；Linux 需要 C++ 编译器和 Python 开发头文件。macOS 暂无受支持的原生构建流程。

```bash
git clone https://github.com/kk03kk/SLS.git
cd SLS
python -m venv .venv
# Windows PowerShell: .\.venv\Scripts\Activate.ps1
# Linux: source .venv/bin/activate
python tools/bootstrap.py --with-model
```

Windows 可用 `py -3.12 -m venv .venv` 创建环境。`bootstrap.py` 安装锁定依赖、构建 native 并运行测试；只开发模拟器时可省略 `--with-model`。也可激活自己的 Python 3.12 Conda 环境。若仅需 Python 代码，可查看 `python tools/bootstrap.py --help`。

无需游戏、GPU 或模型即可运行模拟器。将以下代码保存为 `quickstart.py` 并执行 `python quickstart.py`：

```python
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_ACT1

backend = SimulatorBackend(IRONCLAD_A0_ACT1)
decision = backend.reset(seed=42)
print(decision.observation.screen.value)
print([action.kind for action in decision.actions])
decision = backend.step(decision.actions[0]).decision  # 仅演示合法动作
print(decision.observation.screen.value)
```

策略只接收当时公开的 Observation，并从合法的语义动作中选择。这个示例没有加载智能体；`decision.actions[0]` 不是策略建议。

## 模型、演示与训练

Git 仓库**不包含预训练权重、游戏文件或 Mod JAR**。`model/` 是唯一的演示模型目录，本地默认文件为 `ironclad-a20-act1-56m-champion.pt`，只包含推理权重和元数据。选中 checkpoint 和原始报告保存在 `local/runs/`，历史归档保存在 `runs/archives/`。朋友 clone 后需另外取得模型，或解压维护者提供的模型分享包；放到 `model/` 后即可被控制窗口识别。[模型发布说明](docs/model-release.md)列出了分享方式。

拥有与当前 `sls-policy-input-v5` 编码兼容的 A20 Act1 checkpoint 后，可导出并采集一局模拟器轨迹：

```bash
python tools/capture_policy_trajectory.py simulator model/ironclad-a20-act1-56m-champion.pt --seed 42 --max-actions 20 --output local/reports/seed-42.jsonl
python tools/play_live_inspector.py --list-models
```

上述命令需先取得默认模型。旧编码模型不能通过改版本号复用。连接真实游戏需自备游戏、ModTheSpire、BaseMod、CommunicationMod 与 Observation Oracle；Oracle 已支持[从仓库源码构建](native/oracle/README.md)，需要四项运行依赖，不再需要已有 Oracle 基础 JAR。`python tools/check_live_setup.py` 可以检查本机准备情况；`python tools/configure_live_inspector.py --select-mods` 配好启动命令和 Mod 列表后，游戏会弹出可选模型、自动运行、暂停和单步的独立窗口。[朋友安装速览](docs/friend-quickstart.md)和[本地游戏与观察界面说明](docs/local-runtime.md)列出了前提和操作。朋友仅 clone 仓库不会自动得到模型、游戏或 Oracle。没有这些文件时，可以使用模拟器。

已完成实验的配置为 [`ironclad_a20_act1_60m_stable.toml`](configs/train/ironclad_a20_act1_60m_stable.toml)。[提交说明](docs/a20-act1-60m-stable-launch.md)保留为操作记录；配置依赖原 46M checkpoint 和当时的源码环境，不能在修改后的模拟器上声称精确复现原成绩。历史配置保留用于追溯；见 [配置索引](configs/train/README.md)。

## 开发与协作

```bash
python tools/build_native.py --jobs 4
python -m pytest -q
python -m ruff check .
python tools/generate_policy_vocabulary.py --check
```

请先读 [贡献指南](CONTRIBUTING.md) 和 [架构说明](docs/architecture.md)。涉及模拟器规则、公开观测或训练契约的修改，需要说明行为差异与验证证据；已有训练模型不能在语义变化后直接称为同一实验的精确续训。

本次模型归档、全量检查与项目整理见 [2026-09-28 阶段审核](docs/audits/2026-09-28-stage-closeout.md)；此前端到端代码审计与验证边界见 [2026-09-26 审计记录](docs/audits/2026-09-26-project-audit.md)。

| 路径 | 用途 |
| --- | --- |
| `src/sls/` | 协议、后端、模型、训练与实机运行 |
| `native/simulator/` | 带上游许可与来源记录的 C++ 模拟器 |
| `native/oracle/` | 原版游戏公开观测补丁源码 |
| `configs/train/` | 当前与历史训练配置 |
| `tools/` | 构建、训练、评估和导出命令 |
| `tests/` | 回归与契约测试 |
| `docs/` | 维护文档；`docs/history/` 保存历史证据 |
| `local/`、`runs/archives/`、`model/*.pt` | 本机产物与外部文件，不提交 Git |

更多路径见 [仓库地图](docs/repository-map.md)。项目代码采用 [MIT 许可](LICENSE)；模拟器上游的原始版权声明保留在 [`native/simulator/LICENSE.lightspeed.md`](native/simulator/LICENSE.lightspeed.md)。游戏与 Mod 属于各自权利人，不由本仓库提供。
