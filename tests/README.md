# 测试目录

这里是正式项目的一部分，应该整体上传 GitHub。测试源码、Java 探针和六份小型回归 fixture 都保留；模型权重、游戏/Mod JAR、原版反编译源码、运行日志和编译缓存仍在忽略的本地目录。

2026-10-05 整理时有 **93 个 pytest 测试模块、1 个 conftest、6 份 fixture 和 1 个 Java 探针**。参数化会展开成更多用例，不能拿源码函数数和 pytest 用例数直接比较。逐文件及测试入口见 [INDEX](INDEX.md)。目录不按年代搬迁：旧 bug、checkpoint 迁移、服务器故障等测试仍保护当前代码，也有其他工具直接引用 fixture 路径。

## 各部分作用

| 位置 | 测试模块数 | 检查什么 | 依赖与边界 |
| --- | ---: | --- | --- |
| 根目录 | 22 | curriculum、A20 分支、Neow/event 观测、训练/提交入口、构建工具、实验配对分析与配置 | 主要是单元及集成测试；Slurm 命令被模拟，不会提交作业 |
| `contracts/` | 3 | Observation、Action、Decision 不变量与非法输入 | 统一协议的基础验证 |
| `content/` | 5 | seed 编码、卡牌特征/能量、registry、训练内容范围 | 不等于所有卡牌机制均与原版一致 |
| `model/` | 5 | 策略形状、合法动作引用、牌组选择、配对牌与 power ownership | 需要 Torch；不是训练质量或胜率验证 |
| `rl/` | 18 | reward、GAE/PPO 数学、rollout、workers、checkpoint、恢复/迁移、选模、评估身份、真实小批训练 | Torch/native；部分在 CPU 真实执行短更新；CUDA 测试仅有 GPU 时执行 |
| `simulator/` | 12 | native 状态重放、机制、内容执行、完整局结构、A20 晚幕规则、变换/选择牌、遗物范围 | 需要编译 native；强制状态与跳过战斗只用于测试，不是正常开局胜率证据 |
| `original/` | 4 | CommunicationMod 适配、transport、原版 backend、live mode 安全契约 | 使用构造消息/假 transport，不启动真实游戏 |
| `runtime/` | 3 | 推理 artifact、控制器、模型发现、HTTP/线程暂停恢复、live 配置 | 临时模型、模拟 backend/HTTP；不替代实机通关测试 |
| `audit/` | 12 | 原版日志/字节码/覆盖记录解析、证据身份校验、机制比较工具、Act1 map/targets | 大部分使用合成证据；四项 stock 源码对照依赖本地投影，不能把工具通过当作 parity 已通过 |
| `diagnostics/` | 9 | 失败/宏观/种子诊断、配对评估、canary 启动命令、证据和 corpus 选择 | 多数不需要私人模型；一项旧 5M 轨迹测试是可选历史证据 |
| `fixtures/` | — | 六份可重放的 bug/stock 探针输入 | 正式保留，逐文件说明见 [fixture 说明](fixtures/README.md) |
| `oracle/` | — | 一个 Java 生产/验证模式探针 | 不由 pytest 执行；需 JDK、构建好的 Oracle 和真实依赖 JAR，见 [Oracle 测试说明](oracle/README.md) |
| `conftest.py` | — | 将当前 `src/` 加入测试导入路径 | 测试配置，不是可以删掉的临时文件 |

## 运行

主工作站使用 Conda `DL`，native 和依赖已构建时：

```powershell
conda activate DL
$env:PYTHONDONTWRITEBYTECODE = '1'
python -m pytest -q
python -m ruff check src tools tests
```

新 clone 使用项目的 bootstrap 安装测试与模型依赖并构建 native，具体安装入口见根 README。完整测试需要 Torch/native；少数模块的 `importorskip` 不意味着不安装 Torch 也能完整收集全套测试。游戏、历史模型和 stock 反编译文件不作为普通自动测试的必备条件。

快速定位修改范围可以只运行相关目录，例如 `python -m pytest tests/rl/test_ppo_math.py tests/rl/test_rollout.py -q`；提交前仍应运行当前改变所要求的检查，不能用一个小集合替代全量结论。

`local_evidence` 标记仅用于**私人历史模型/stock 源码投影**依赖的五项测试。默认仍运行它们，缺失时显式 skip；CI 不会因为缺私有证据而把这些项目当作成功。可分别查看：

```powershell
python -m pytest -m local_evidence -q
python -m pytest -m "not local_evidence" -q
```

原版 Java 投影的约定位置是 `local/audits/stock-decompilation-tree/desktop-1.0/source/`，不上传其内容。CUDA 不可用时，对应恢复测试会 skip；skip 是未执行，不是通过。

## 整理与修复

发现 `audit_policy_seed.py` 原先仅按 goal 选择 A0 profile，忽略 A20 模型登记环境，可能把诊断实际跑在 A0。现改用模型的明确 environment profile；诊断 schema 升至 **`sls-policy-seed-audit-v2`** 并写入实际环境。缺 profile 的旧非 Act1 模型只在 A0 位于其支持范围内时保留旧 fallback，避免静默越界。

新增不依赖训练权重的 A0/A20 种子审计测试：在临时目录生成小模型，实际执行 native audit，核对实际 backend profile、负 seed 与 uint64 等价 seed 的 baseline/counterfactual 一致性。它验证诊断逻辑，不声称生成模型有效。

旧 5M 固定轨迹断言保留，路径改为 `runs/archives/policies/ironclad-a0-act1-5m.pt`。当前旧模型输入编码不兼容，必须显式 skip，不能改版本号或替换权重后继续沿用 196 actions 等旧断言。

未删除测试源码，也未把相似但检查不同屏幕/恢复契约的测试合并。10 个 `__pycache__` 目录、241 个缓存文件（4,211,495 bytes）已移至忽略的 `local/reports/project-reset-20261005/test-cache-backup/`；没有删除原始 fixture。后续以禁用 bytecode 的方式验证，pytest/Ruff 自身缓存由 `pyproject.toml` 定位到 `local/build/`。

本次只改变诊断工具输出与测试分类，reward、PPO、模型、observation/action 和 native semantics 未改。完整测试 **1051 passed、1 skipped**，四条受控恢复警告；`local_evidence` 单独运行 **4 passed、1 skipped**，Ruff 与差异检查通过。详细结果见 [整理记录](../docs/tests-review-20261005.md)；运行日志和逐文件哈希清单保存在 `local/reports/project-reset-20261005/`。
