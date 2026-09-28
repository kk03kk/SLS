# 和朋友一起看模型玩《杀戮尖塔》

目前支持 **Slay the Spire 1、战士、与模型匹配的 Ascension**。A20 Act1
模型到第一幕目标边界就停止。窗口可以选模型、自动运行、暂停和单步。

默认模型为 `ironclad-a20-act1-56m-champion.pt`。维护者提供的
`sls-ironclad-a20-act1-56m-policy.zip` 可直接解压到仓库根目录，模型和清单
会进入 `model/`。训练模拟器独立成绩为 77.39%，详情见
[阶段结果](results/a20-act1-60m-stable/README.md)；这不是原版实机胜率。

## 需要准备的文件

1. 自己的 Steam 游戏，以及 ModTheSpire、BaseMod、CommunicationMod。
2. 与本项目观测协议匹配的 `SpirecommParity.jar`。将其放进游戏的 `mods/`
   目录。Git 仓库只有补丁源码，没有可直接使用的基础 Oracle JAR。
3. 维护者单独提供的 `sls-policy-artifact-v5` 导出模型 `.pt`，放进仓库
   `model/` 目录。训练 checkpoint（例如 `latest.pt`）不能直接使用。

游戏、第三方 Mod 和模型工件不会随着 `git clone` 下载。分享 Oracle 前
须确认基础 JAR 的分发许可；没有许可时可按[构建说明](local-runtime.md)
从合法取得的基础 JAR 重新编译补丁。

## Windows 安装

在仓库根目录运行：

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements\model.lock
.\.venv\Scripts\python.exe -m pip install -e . --no-deps
.\.venv\Scripts\python.exe tools\configure_live_inspector.py --select-mods
.\.venv\Scripts\python.exe tools\check_live_setup.py
```

如果 ModTheSpire 还没有生成 `mod_lists.json` 或 CommunicationMod 还没有
生成 `config.properties`，先用这些 Mod 启动一次游戏，再运行最后两条命令。
检查结果的 `ready` 应为 `true`、`issues` 应为空。游戏不在常见 Steam
目录时给检查工具添加 `--game-dir`；Mod 在另一个库时添加
`--workshop-dir`。检查工具只读，不会替你启动游戏。

## 观看与控制

从 Steam 的 ModTheSpire 入口启动游戏，确认勾选 BaseMod、
CommunicationMod 和 SpirecommParity。游戏启动后会弹出单独的控制窗口。
选择模型，点击“加载模型并连接游戏”，随后开启与模型 Ascension 匹配的
战士新局。到 Neow 时控制台应显示“已暂停”。

点击“自动运行”开始观看；点击“暂停”会在当前动作结束后的安全决策点
停住。空格键可暂停或继续，句号键可单步。需要更换模型时，请开始新局并
重新启动控制器。关闭控制窗口前先暂停模型。

完整的实机说明、备份恢复和诊断命令见[本地运行文档](local-runtime.md)。
