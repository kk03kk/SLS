# 本地实机运行

本项目有两个入口：用于演示和人工控制的 `tools/play_live_inspector.py`，
以及用于受限自动运行的 `tools/play_live.py`。两者都通过 CommunicationMod
读取游戏公开状态与合法动作，不能代替游戏本体和 Mod。

控制器检查模型适用的 Ascension、目标和权重摘要。每次动作及其确认写入
`local/logs/` 的日志；只有当前边界与已确认日志匹配时才可恢复循环记忆。
在任意半局状态新启动、模型不匹配或上一个动作是否送达无法证明时会停止。
`--wait-for-neow` 允许从主菜单等待新局，但不会自行开始或重置游戏。
`FULLRUN` 模型在第三幕后停止，`HEART` 模型继续第四幕。

## 实机控制台（Windows）

Steam 安装游戏本身不足以连接模型。本机还需要 ModTheSpire、BaseMod、
CommunicationMod 和本项目使用的 Observation Oracle JAR。游戏和第三方 Mod
由使用者自行安装；本仓库不分发它们。Oracle 完整源码现位于 `native/oracle/`，
`tools/build_oracle.py` 从源码构建，不需要旧 `SpirecommParity.jar`。
朋友仍需自己的四项依赖和 JDK；只 clone 不会下载这些运行依赖。

准备 JDK、游戏、ModTheSpire、BaseMod 和 CommunicationMod 后可构建当前版本。
构建命令需要明确指出依赖 JAR 的位置，避免复制 300 MB 以上的游戏文件：

```powershell
python tools\build_oracle.py `
  --javac "<JDK>\bin\javac.exe" `
  --game-jar "<游戏目录>\desktop-1.0.jar" `
  --communication-mod "<CommunicationMod.jar>" `
  --base-mod "<BaseMod.jar>" `
  --mod-the-spire "<ModTheSpire.jar>"
```

输出默认为 `local/build/oracle/SpirecommParity.jar`，同名 build JSON 绑定源码和依赖。
先运行 `python tools/verify_oracle.py local/build/oracle/SpirecommParity.jar` 核验；
关闭游戏后，用该命令加 `--install` 备份旧 JAR 并安装，详见[Oracle 说明](../native/oracle/README.md)。
`check_live_setup.py` 检查模式类及运行文件；实际连接要求 v11 / `sls-oracle-mode-v1` production 状态。
旧版本和 validation 被拒绝，模式不能在同一 JVM 中切换。

运行环境需 Python 3.12+、`torch` 与本项目依赖。模型必须是 v5 导出的
`sls-policy-artifact-v5` 文件，放在 `model/` 下；训练目录的 `latest.pt`
不能直接作为演示模型。当前默认模型为 `ironclad-a20-act1-56m-champion.pt`，
下载该文件及其清单即可使用，详见[模型发布说明](model-release.md)；若只有训练 checkpoint，应先运行
`tools/export_policy.py`。分享给朋友时需单独提供模型和 Oracle JAR，
确认各自的许可与分发权限，并提供模型 SHA256 与适用的 Ascension。

先运行只读检查：

```powershell
conda activate DL
python tools\check_live_setup.py
```

若 Steam 安装在其他目录，可加 `--game-dir "D:\path\to\SlayTheSpire"`；
若 Mod 放在其他 Steam workshop 目录，可加 `--workshop-dir`。
检查会确认文件、启动命令和 Oracle 关键补丁类存在；它不能证明某一局游戏的端到端正确性。
在配齐前提后配置 CommunicationMod：

```powershell
python tools\configure_live_inspector.py --select-mods
```

配置工具使用当前 Python，备份原来的 `config.properties` 和 Mod 列表，
设置游戏启动时调用控制台，并在默认 Mod 列表补齐必需的三个 Mod。
若 Mod 列表尚不存在，先启动一次 ModTheSpire 让它生成列表。
启动游戏时在 ModTheSpire 中勾选 BaseMod、CommunicationMod 和
SpirecommParity。游戏启动后会弹出独立的 Edge 应用窗口；若系统没有 Edge，
会在默认浏览器中打开本机页面。控制台只绑定 `127.0.0.1`，无需网络服务。

在窗口选择模型并点击“加载模型并连接游戏”，然后创建匹配的战士新局。
第一次到 Neow 时保持暂停；点击“自动运行”开始观看，点击“暂停”会在当前
动作结束后的安全决策点停住。支持模型单步、手选合法动作、动作间隔与奖励
展示时长。空格键切换暂停/继续，句号键单步。更换模型须开启新局并重新
启动控制器，避免把前一模型的循环记忆带入新模型。Act1 模型到第一幕目标
边界就停止，不会自动接管第二幕。

本机不启动游戏也能检查导出模型：

```powershell
python tools\play_live_inspector.py --list-models
```

`tools/play_live_inspector.py` 的 HTTP 页面仍可从终端用
`--no-open-browser` 启动，适用于无窗口调试。

## 控制台实现说明

服务只监听 `127.0.0.1:8765`。启动游戏时不加载模型，也不会发送游戏动作；
点击“加载模型并连接游戏”后，连接成功仍先保持暂停。窗口显示全部合法动作的
logit、softmax 概率和状态价值估计（不是动作 Q 值）。动作间隔和卡牌奖励展示
时间都可在 0–10 秒内调整。奖励卡牌的一次语义决策可能包含“打开奖励”和
“选择卡牌”两个游戏点击。

配置工具保留无关属性并建立带时间戳的同目录备份。用打印出的备份路径恢复：

```powershell
python tools\configure_live_inspector.py --restore <backup-path>
python tools\configure_live_inspector.py --restore-mods <mod-list-backup-path>
```

控制台接受兼容的 `ACT1`、`ACT2`、`ACT3` 策略；常规 `play_live.py`
仍仅接受 `FULLRUN` 与 `HEART`。课程模型到达目标 Boss 清除边界后停止，
不会越界替下一幕做决定。

## Reproducible seed audit

Live journals use `sls-live-action-v4`. Every new intent includes a process
`session_id`, the complete public Observation, all ranked candidate actions,
the model recommendation, the action actually selected, and the public run seed
when CommunicationMod supplies it. The ACK carries the same session ID. Older
v3 logs remain readable, but cannot reconstruct observations that they never
recorded; start a new inspector session to obtain the v4 evidence.

Replay a signed Java `long` seed with the exact recurrent runtime and generate a
baseline plus a clearly-labelled diagnostic block-deficit counterfactual:

Seed audit v2 uses the artifact's registered environment profile and records it;
it no longer silently audits A20 at A0. Historical v1 A20 results must not be
relabelled as verified A20 evidence.

```powershell
python tools\audit_policy_seed.py `
  model\your-act1-policy.pt `
  --seed -1466613676819842358 `
  --output local\reports\live-audit\seed-audit.json
```

Capture a boundary-by-boundary canary trajectory with the same previous-action
and previous-reward recurrent inputs used by live play:

```powershell
python tools\capture_policy_trajectory.py simulator `
  model\your-act1-policy.pt `
  --seed -1466613676819842358 `
  --output local\reports\live-audit\seed-trajectory-v2.jsonl
```

Trajectory v2 hashes and records recurrent context. It therefore replaces v1
captures for exact live/simulator comparisons.
