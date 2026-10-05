# tests/ 目录整理与核验：2026-10-05

本次范围是测试源码、fixture、执行依赖和诊断工具的环境选择问题。没有重新审计所有游戏机制，没有服务器训练，也没有新增真实游戏运行。既有 Oracle JVM/游戏 qualification 属上一阶段独立证据。

## 文件与保留结论

测试代码为 93 个 `test_*.py` 模块、1 个 conftest；另有 6 份 JSON/gzip fixture 和一个仓库自有 Java mode probe。新增四份说明文件：`tests/README.md`、`INDEX.md`、`fixtures/README.md`、`oracle/README.md`。这些源码、fixture 和说明全部作为正式项目保留并上传。

未移动测试源文件和 fixture，未删除相似但覆盖不同 screen、输入合同、恢复状态或故障分支的测试。93 个模块的逐文件测试入口在 [INDEX](../tests/INDEX.md)，分类与运行命令在 [README](../tests/README.md)。六份 fixture 都有实际消费者，其字节 SHA256 与整理初次清单逐一一致。

`__pycache__` 的 241 个文件、4,211,495 bytes 来自编译缓存，不是正式来源。递归删除被自动审批拒绝，改为将十个目录移动到 `local/reports/project-reset-20261005/test-cache-backup/`；内容可恢复，原测试目录不再包含缓存。验证使用 `PYTHONDONTWRITEBYTECODE=1`。

## 已确认并修复的问题

`tools/audit_policy_seed.py` 的 `_PROFILES` 全部是 A0，旧 `_run` 只读取 artifact.goal，而不读取其明确的 environment_profile。因此以 A20 模型运行种子诊断会选择 A0 backend，诊断难度与模型身份不一致。现在优先使用登记的 curriculum profile，保留受 ascension 支持范围约束的旧非 Act1 fallback。诊断 schema 从 `sls-policy-seed-audit-v1` 升为 **v2**，新增实际 `environment` 身份。

此修改只影响诊断工具，不改变 SimulatorBackend、模型输入、合法动作、reward、PPO 或训练逻辑。不能把旧 v1 A20 诊断当作已验证的 A20 结果；没有重写任何旧诊断报告。

`tests/diagnostics/test_seed_audit.py` 原历史路径位于已清空的 `model/ironclad-a0-act1-5m.pt`，与保留策略不一致。现在引用规范历史归档路径，保留旧 196 actions 与反事实断言，但当前旧编码不兼容，所以该项明确 skip。新参数化测试使用临时小模型实际执行 A0/A20 native audit，核对真正创建的 backend profile，验证负 seed 与等价 uint64 seed 的 baseline/counterfactual 一致性。不依赖私人训练模型，不用测试模型成绩代替训练结果。

## 测试边界与依赖

四项 stock Java 投影对照与一项历史 policy 轨迹测试添加 `local_evidence` 标记，注册在 `pyproject.toml`。默认仍运行；缺失资源报告 skip，不改变普通 CI 的通过判据。此主机上的四项投影对照通过，但不是从原版游戏现场重新采集证据；CI 缺反编译投影时不能声称执行了它们。

大量 audit 测试使用合成原版日志、Jar member 清单或比较对象，验证解析、聚合、拒绝错误身份和覆盖状态语义。original/runtime 测试使用 fake transport/backend 或临时模型。Java mode probe 不被 pytest 收集，需要明确的 JVM 执行。需要把这些与实际原版 parity、策略胜率以及封存最终评估分开。

未发现需要为了整理删除的源测试；这不是“全部测试都完美”结论。完整测试需要 Torch/native，少数 `importorskip` 不能当作完整无模型安装支持；CUDA-only 测试缺 GPU 时应 skip。本次 CUDA 测试已在本地主机执行通过。

## 验证记录

| 检查 | 结果 |
| --- | --- |
| `python -m pytest -q --junitxml=local/reports/project-reset-20261005/tests-review-junit.xml` | **1051 passed, 1 skipped, 4 warnings，223.08s** |
| `python -m pytest -m local_evidence -q` | **4 passed, 1 skipped，1047 deselected** |
| `python -m ruff check src tools tests` | 通过 |
| `python tools/audit_policy_seed.py --help` | 通过 |
| `git diff --check` | 通过；仅正常 LF/CRLF 提示 |
| 六份 fixture 原始 SHA256 | 全部保持一致 |
| `AGENTS.md` | SHA256 保持 `90132f3c…`，没有修改 |

唯一 skip：旧 5M 历史 artifact 输入编码与当前契约不兼容。四条 warning 均来自 checkpoint/provenance rebind 验证，测试有意行使受限 rebind 并提醒跨 runtime 不保证逐位重放。

按 JUnit 实际展开的用例数量：根目录 396、audit 38、content 38、contracts 11、diagnostics 68、model 47、original 68、rl 196、runtime 29、simulator 161；合计 1052，包括一项 skip。完整逐文件哈希、fixture 哈希、目录清单、测试日志和 XML 保存在忽略的 `local/reports/project-reset-20261005/tests-review-final.json` 及邻近记录中。

训练 implementation 摘要保持 `04fa179d6bc36cd3c595c9c9639c3112546bbb343bc06f3b8c9c3dab88f265e7`；native source 保持 `fe354a23c7584d68d0a2b6681b8dfd4e97d98e107b7ddf57c479060d91468f39`。未修改原始 70M/90M 实验配置、模型权重或结果身份，未新增训练兼容性例外。
