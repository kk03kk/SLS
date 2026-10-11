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

## Git 与服务器

本项目仅在 `main` 上持续推进。后续工作不新建分支，也不为了单次分析创建独立 worktree。用小而明确的提交保留修改过程；合并文档不会要求压缩或清空 Git 提交历史。

正常流程是本地修改与验证 → 提交并推送 `main` → 人类在 NUS 拉取 → 人类执行服务器命令并反馈输出。模型、训练状态、游戏文件、个人配置和凭证不提交 Git。

NUS 操作由人类执行。先同步代码，再按当前已核验实验提供完整命令；不要直接重跑旧文档的 sbatch 命令，不在 login node 训练，也不在正在训练的目录热替换代码。本次本地整理未执行服务器操作。

原版规则或观测改动必须说明前后行为、回归证据与模型兼容性。训练方案至少登记配置、源码、父 checkpoint SHA、种子区间、runtime、worker 布局、预算、选模规则与独立终评。
