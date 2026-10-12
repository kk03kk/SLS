# 开发与运行

## 本机环境

在 `D:\SLS` 使用 Python 3.12 Conda 环境 `DL`：

```powershell
conda activate DL
$env:PYTHONPYCACHEPREFIX = Join-Path (Get-Location) "local\build\python-cache"
python tools/bootstrap.py --with-model
```

PowerShell 尚未初始化 Conda 时，当前会话可先运行：

```powershell
& D:\Anaconda\shell\condabin\conda-hook.ps1
conda activate DL
```

缓存设置只影响当前PowerShell会话及其子进程，不写入用户全局环境。按照AGENTS.md在每个开发会话设置，避免src/tools/tests重新堆积__pycache__。

新机器需要 C++ 构建工具。Linux 还需要 Python 开发头文件。依赖锁文件在 `requirements/`；macOS 暂无受支持的原生构建流程。

## 构建与验证

从仓库根目录执行：

```powershell
python tools/build_native.py --jobs 4
python -m pytest -q
python -m ruff check .
python tools/generate_policy_vocabulary.py --check
python tools/generate_content_registry.py --check
python tools/check_training_configs.py
```

按改动范围选择检查。native 规则变更需重建；仅文字整理不必重编。部分原版证据测试需要本机来源，不能把缺少证据的 skip 当作资格证明。源码、词表、回归 fixture、Java 探针和依赖锁文件都是项目资产。

每个 CLI 的准确参数以 `python tools/<命令>.py --help` 为准。构建、训练、提交、评估、恢复核验与导出分别使用 `build_native.py`、`train_full_run.py`、`submit_slurm.py`、`evaluate_checkpoint.py`、`verify_training_resume.py` 与 `export_policy.py`。

## 实机与 Oracle

运行 Slay the Spire 1 需要游戏本体、ModTheSpire、BaseMod、CommunicationMod 和本项目 Oracle。Git 不包含游戏或第三方 JAR。

Oracle 源码在 `native/oracle/`，从源码构建需要 JDK 和上述四项运行依赖，不需要旧 Oracle JAR：

```powershell
python tools/build_oracle.py --javac "<JDK>\bin\javac.exe" --game-jar "<desktop-1.0.jar>" --communication-mod "<CommunicationMod.jar>" --base-mod "<BaseMod.jar>" --mod-the-spire "<ModTheSpire.jar>"
python tools/verify_oracle.py local/build/oracle/SpirecommParity.jar
```

默认输出和构建身份记录在 `local/build/oracle/`。关闭游戏后，用核验命令加 `--install` 备份旧 JAR 并安装。production 用于真实部署，validation 用于明确的受控场景；live backend 会检查模式与协议，不要用 validation 成绩冒充原版自然开局结果。

推理权重及同名来源 JSON 放入 `model/`，不要放带优化器的 `latest.pt`：

```powershell
python tools/play_live_inspector.py --list-models
python tools/configure_live_inspector.py --select-mods
python tools/check_live_setup.py
```

第三方 Mod 首次运行后会生成自己的配置。检查结果应 `ready=true` 且 `issues` 为空。游戏位于其它目录时使用工具提供的 `--game-dir`、`--workshop-dir`。

在 ModTheSpire 勾选依赖和 Oracle，启动游戏后加载模型，开启难度匹配的新局。控制窗口提供自动、暂停与单步；空格暂停/继续，句号单步。更换模型时重新开启新局。目标为 Act1 的模型到该目标就停止，不能据此宣称完整通关。

动作及确认日志在 `local/logs/`。控制器只有在已确认边界匹配时才恢复记忆；无法证明动作是否送达时会停止。运行恢复 journal、游戏存档备份和当前 native/JAR 不能当作普通缓存删除。

### Visual Inspector 一键使用

启动命令同时检查网页服务和实际游戏进程。关闭游戏后，旧网页可能仍显示 `EOFError: CommunicationMod closed stdin`；再次运行入口会先关闭这个无游戏的旧控制器、等待端口释放，再启动原版。游戏仍运行时只打开现有页面，不终止游戏或改写配置。

关闭未接入 Inspector 的游戏后，在本机 DL 环境执行：

```powershell
cd D:\SLS
& D:\Anaconda\shell\condabin\conda-hook.ps1
conda activate DL
$env:PYTHONPYCACHEPREFIX = 'D:\SLS\local\build\python-cache'
python tools/launch_inspector.py
```

入口先检查现有网页服务；若已有控制器只打开其窗口。否则备份并配置 CommunicationMod、选择所需 Mod，再用现有 ModTheSpire 启动真正的原版游戏，游戏通过 stdio 启动唯一的 Python 控制器及浏览器。默认地址为 `http://127.0.0.1:8765`。缺少依赖时明确失败；不会另开模拟器充当游戏。手动启动 ModTheSpire 时可先运行 `python tools/launch_inspector.py --configure-only`。

网页默认选择 `ironclad-a20-act1-90m-endpoint`。可选模型来自 `model/` 的推理工件及身份信息，加载仍执行原有权重 SHA、词表、架构及课程检查。原版停在主菜单时，可输入64位整数 Seed，点击“从原版主菜单开始新局”；使用模型登记的难度，不会重置或放弃正在进行的游戏。也可点击“连接所选模型”，再从原版界面手动开始战士 A20 新局。只允许从涅奥或匹配 journal 的已确认边界初始化。

地图可点击节点筛选历史、拖动空白区域平移、缩放并定位当前位置。评分明确区分动作概率、Logits 与 State Value；卡牌主体实例及目标可区分。历史回顾展示当时的公开状态和评分，不回滚游戏。单步执行一个模型动作，组合卡牌奖励可能包含两个原版点击。暂停在当前事务确认后停止后续动作；关闭网页超过10秒会在安全边界暂停。

新启动的游戏采用独立时间戳日志，保留每局 `INTENT`、`ACK` 与 `VISUAL_ACK`。如需恢复，关闭旧控制器，通过 CommunicationMod 使用 `play_live_inspector.py --log <原journal>`，不要加 `--new-session-log`；无法证明送达时仍拒绝继续。第一幕任务到 Boss 击败停止，不进入第二幕。第一版切换模型或再开一局需关闭并重启游戏/控制器，在初始模型选择页选择；不会继承旧模型的 GRU。不要在暂停期间手动推进游戏决策。

普通使用不需要 Node.js 或联网下载前端依赖。开发前端时安装 Node.js 与 pnpm，然后执行 `python tools/build_inspector.py`；工具在 `local/build/inspector-web/` 安装依赖、检查 TypeScript 并构建，将已验证静态产物更新到源码目录。不要在 `src/` 留下 node_modules。React 等第三方许可证随前端产物保留。

保护用户文件的真实游戏验收命令为 `python tools/verify_live_inspector.py --output local/reports/visual-inspector/<唯一目录>`。它使用 production Oracle、90M 推理工件及正常原版开局，验证单步、暂停评分保持、自动执行和终点；保存实际源码/模型/JAR身份、逐步记录，并复用既有 BackupJournal 恢复配置、偏好、存档和运行记录。必须检查 `source_unchanged=true`、`status=PASS`、journal为 `RECOVERED` 且无恢复失败。源码变化的中途结果标为 `SOURCE_CHANGED_REPLAY_REQUIRED`，不能签发最终通过。

## Git 与服务器

本项目仅在 `main` 上持续推进。后续工作不新建分支，也不为了单次分析创建独立 worktree。用小而明确的提交保留修改过程；合并文档不会要求压缩或清空 Git 提交历史。

正常流程是本地修改与验证 → 提交并推送 `main` → 人类在 NUS 拉取 → 人类执行服务器命令并反馈输出。模型、训练状态、游戏文件、个人配置和凭证不提交 Git。

NUS 操作由人类执行。先同步代码，再按当前已核验实验提供完整命令；不要直接重跑旧文档的 sbatch 命令，不在 login node 训练，也不在正在训练的目录热替换代码。本次本地整理未执行服务器操作。

原版规则或观测改动必须说明前后行为、回归证据与模型兼容性。训练方案至少登记配置、源码、父 checkpoint SHA、种子区间、runtime、worker 布局、预算、选模规则与独立终评。
