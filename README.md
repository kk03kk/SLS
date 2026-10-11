# SLS

Slay the Spire 1 模拟器与循环 PPO 智能体项目。使用 C++ 模拟器训练战士策略，并通过原版游戏接入进行公开观测与规则对照。

## 开始使用

本机开发使用 Python 3.12 Conda 环境 `DL`，在 `D:\SLS` 执行：

```powershell
conda activate DL
python tools/bootstrap.py --with-model
```

只运行模拟器可省略 `--with-model`。训练权重、游戏本体和第三方 Mod 需另行取得；Git 仓库只包含项目源码、配置与可移植回归证据。

## 文档

日常文档统一放在 [docs/](docs/README.md)，以后更新这五份文件，不再按每次分析新建报告。

- [架构](docs/architecture.md)：模块职责、策略与环境边界。
- [开发与运行](docs/development.md)：安装、构建、测试、实机运行及 main 工作流。
- [训练规范](docs/training.md)：实验、恢复、评估与模型管理。
- [当前状态](docs/status.md)：已确认结果、已知问题与下一步。

## 目录

| 目录 | 内容 |
| --- | --- |
| `src/sls/` | Python 协议、后端、策略、PPO、审计与运行接口 |
| `native/` | C++ 模拟器和 Java Oracle 源码 |
| `tools/` | 构建、训练、评估、导出与规则核验命令 |
| `configs/`、`requirements/` | 配置、历史实验合同与依赖锁文件 |
| `tests/` | 自动回归；`fixtures/evidence/` 保存不可变实验与规则证据 |
| `docs/` | 五份持续维护的文档 |
| `model/` | 推理模型与来源 JSON，不提交 Git |
| `local/` | 本机训练、构建、审计、归档与运行数据，不提交 Git |

项目使用 [MIT 许可](LICENSE)。模拟器上游归属见 [原始许可](native/simulator/LICENSE.lightspeed.md) 和 [来源记录](native/simulator/SLS_VENDOR.json)。
