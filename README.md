# SLS

SLS 是一个以原生 Slay the Spire FullRun 模拟器为环境、使用 recurrent PPO
训练策略，并可通过 CommunicationMod 接入本地游戏的强化学习项目。

当前 10M→15M 的准备、显式模型迁移和 NUS 执行顺序见
[训练方案](docs/training-10m-15m.md)。旧 v3 checkpoint 不能直接 resume 到 v4 输入。

## 项目主体

- `src/sls`：Python 核心，包括公共协议、环境适配、模型、PPO 和实机运行。
- `native/simulator`：C++ FullRun 模拟器和 Python binding。
- `configs`：训练与运行配置。
- `tools`：构建、训练、评估、诊断和策略导出入口。
- `tests`：自动化测试。
- `docs`：架构与操作文档。
- `local`：构建缓存、运行结果、外部游戏文件、日志和审计报告；可移植源码不依赖其中的历史产物。

## 快速验证

~~~powershell
python tools/bootstrap.py --with-model
~~~

只运行现有环境中的测试：

~~~powershell
python -m pytest -q
python -m ruff check src tools tests
~~~

训练入口与阶段说明见 [docs/nus-training-zh.md](docs/nus-training-zh.md)，架构边界见
[docs/architecture.md](docs/architecture.md)。

源码与现有训练产物的独立审计见
[2026-09-05 工程审计](docs/audits/2026-09-05/report.md)，包含已复现的
模拟器规则差异、模型输入信息丢失、训练控制修复和 9M 日志分析。
