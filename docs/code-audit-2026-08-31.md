# 本地代码与结构审计（2026-08-31）

## 范围

本次审计覆盖 Python 强化学习主链、checkpoint/评估/实机运行契约、C++ 原生模拟器
构建，以及根目录与生成数据的归属。Git 历史和远端状态不在范围内。

## 已处理的重要问题

1. 原生源码摘要从 Git index 改为本地文件内容摘要，并统一 CRLF/LF，Git 不再是
   构建、preflight、benchmark 或训练的硬依赖。
2. 原生模拟器、构建缓存、外部游戏文件、运行结果和审计结果迁移到明确的新根目录。
3. 修复字节码审计器对旧原生模拟器目录的路径硬编码。
4. 统一 C++ 前向声明的 `class/struct` 标签，消除 MS ABI 链接风险；为被复制赋值的
   核心状态类型显式声明默认赋值运算符。
5. 修正一个依赖运算符优先级的 free-attack 条件表达式，使含义显式。
6. 更新自校验内容范围资产、配置、工具默认路径、结构测试和操作文档。
7. 删除旧 Python build 副本、旧 CMake 树、字节码、egg-info 和测试/静态检查缓存；
   checkpoint、游戏 JAR、Oracle 与历史证据均保留。

## 验证结果

- `python -m ruff check src tools tests`：通过。
- `python -m pytest -q`：247 passed，1 skipped。
- 跳过项是需要 CUDA 的 smoke test，本机没有 CUDA。
- Windows 原生扩展已从 `native/simulator` 重新构建并成功导入。
- 原生扩展内嵌 source digest 与当前本地源码 digest 一致。
- Slurm dry-run 已确认输出路径进入 `local/runs/slurm-logs`。

## 保留限制

完整 GPU 训练、A100 preflight 和最终 1,000-seed 评估无法在当前无 CUDA 的本机执行；
这些仍应在正式训练机器上按 `docs/nus-training-zh.md` 执行。
