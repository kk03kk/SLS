# 实机演示准备验证（2026-09-26）

本记录只覆盖本机 `D:\SLS` 与用户自己的 Windows 游戏安装；不代表
其他电脑上已验证，也不替代 60M 模型的独立评估。

## 实施

- 用游戏目录现有的 `SpirecommParity.jar` 作基础，编译仓库中的
  `CardStatePatch.java` 和 `EventStatePatch.java`。构建输出及已安装 JAR 的
  SHA256 都是 `d034ed56bf7c93de8c82404ff57aaba62a9256def8402441251481f91c50e191`。
  原 JAR 的备份为
  `D:\steam\steamapps\common\SlayTheSpire\mods\SpirecommParity.jar.bak-20260926-215348-876`。
- 将 CommunicationMod 命令改为 `D:\Anaconda\envs\DL\python.exe` 调用
  `tools/play_live_inspector.py`，保留配置备份。ModTheSpire 默认列表补齐
  BaseMod、CommunicationMod 和 SpirecommParity，也保留了备份。最近的配置
  备份是 `config.properties.bak-20260926-221843-530315`，Mod 列表备份
  是 `mod_lists.json.bak-20260926-221843-534024`，均在各自原文件旁。
- 控制台在 Edge 应用窗口中提供模型选择、运行、暂停、单步和动作查看；
  新增 UI 失联后自动暂停的安全边界。

## 验证证据

- `tools/check_live_setup.py` 报告 `ready: true`、`issues: []`，识别到
  A20 Act1 的 38M 导出策略。`load_policy_artifact` 完整验证了模型权重摘要。
- 使用 `tools/run_original_canary.py` 启动原版游戏，种子 `20260926`、
  38M 策略，限制 **5 个模型动作**。结果为 6 个决策边界、5 个动作确认，
  从 Neow 到第一层 `COMBAT_REWARD`，退出码 0。
- 原版探针的完成记录、动作日志和恢复日志位于忽略目录
  `local/reports/live-demo-canary/`、
  `local/runs/canary/runtime-backups/20260926T141515.492865Z/`。
  恢复日志状态为 `RECOVERED`、失败列表为空。结束后游戏 Oracle 哈希
  与探针前一致，原版进程已退出。Steam 客户端未连接提示出现在 stderr，
  但未阻止这次 5 动作探针。
- 控制台相关 19 项测试通过（含 HTTP 选模、暂停、单步和窗口失联暂停），
  Ruff 通过，窗口 JavaScript 语法检查通过。
  在不连接游戏的设置状态下用 Edge headless 截图人工检查了布局、中文文本、
  模型下拉框与控制按钮；截图保存在忽略目录
  `local/build/inspector-setup.png`。

## 验证边界

此探针验证了真实游戏的观测、模型评分与短序列动作执行；它没有跑完整
第一幕，也没有通过人工点击独立窗口验证每个控件的交互行为。
60M 工件尚未下载。朋友电脑上的 Steam 路径、Mod 版本与 Oracle 来源
仍需逐机检查。不能据此声称原版游戏胜率与模拟器评估胜率相同。
