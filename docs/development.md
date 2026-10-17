# 开发与运行

## 第二幕战斗研究的服务器交接

本次入口是`tools.prepare_battle_study`，以status.md和机器readiness.json为准。下方A/B/C与pilot是历史流程，不能用于本次续训。`READY_FOR_SERVER_SMOKE`表示本地证据齐备，服务器CUDA和学习效果仍为NOT_RUN。

本轮最终CPU凭证目录为`local/reports/act2-study-r1/validation-r6`。复核用`python -m tools.prepare_battle_study --check-ready --output local/reports/act2-study-r1/validation-r6`；重新完整验证须选择全新输出目录，不能覆盖旧失败或通过证据。提交已验证源码后，用`python -m tools.battle_server pack --validation local/reports/act2-study-r1/validation-r6 --output local/archives/act2-battle-r1.tar.gz`打包。包内数据用SHA核验，源码只通过Git同步。

代码推送完成后，人类在Windows上传并登录：

```powershell
Set-Location D:\SLS
powershell -ExecutionPolicy Bypass -File tools/deploy_battle_study.ps1
ssh -J hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg
```

NUS登录后执行（登录节点不import torch）：

```bash
cd /home/h/hengzhi/SLS
squeue -u hengzhi
# 确认没有仍依赖这个checkout的运行作业后，再更新源码；不自动取消其它作业。
git pull --ff-only origin main
git rev-parse HEAD
PY=/home/h/hengzhi/venvs/sls/bin/python
export PYTHONPATH="$PWD/src"
export PYTHONPYCACHEPREFIX="$PWD/local/build/python-cache"
$PY -m tools.battle_server install --bundle "$HOME/act2-battle-r1.tar.gz"
$PY -m tools.battle_server inspect
```

核对代码与数据哈希、Python3.12、torch/numpy/pytest安装元数据、磁盘、实际partition/GRES、账号和QOS。NUS的a100-40可能是物理40GB或80GB卡的40GB MIG，不能根据历史节点名猜显存；实际以分配内nvidia-smi和正式形状测量为准。[NUS说明](https://www.comp.nus.edu.sg/~cs3210/student-guide/soc-gpus/)、[Slurm GRES](https://slurm.schedmd.com/gres.html)。登录节点曾无法映射libtorch_cuda，因此install/inspect/submit/status/export只使用标准库，所有PyTorch工作在分配内。

仅当inspect实际显示gpu分区提供a100-40且账号权限有效，执行以下命令；否则把两个参数替换为实际资源名称，不改批量或学习配方：

```bash
$PY -m tools.battle_server submit --partition gpu --gpu-type a100-40 --dry-run
$PY -m tools.battle_server submit --partition gpu --gpu-type a100-40
squeue -u hengzhi
$PY -m tools.battle_server status
```

默认单卡、16CPU、64GB主存、12小时、account allusers、qos normal；可按权限显式传--account/--qos。一次排队内依次执行包核验、Linux构建、恢复回归、原父正式形状CUDA Smoke、耗时/显存/恢复门和固定预算训练。Smoke失败或保守预计超11小时即停止，不自动减批量、延预算或进入长训练。冻结父评估在同一个作业内完成。

查看日志、请求安全停止、同合同续跑及导出（JOBID替换为实际值）：

```bash
tail -n 50 local/runs/slurm-logs/battle-JOBID.out
tail -n 50 local/runs/slurm-logs/battle-JOBID.err
cat local/runs/ironclad-a20-battle-r1-smoke-JOBID/result.json
scancel --signal=TERM --batch JOBID
# 完整更新边界写安全latest和STOPPED；不要kill -9。
# 原代码/包/合同、相同run路径再次submit，先重验Smoke再从安全latest继续。
$PY -m tools.battle_server submit --partition gpu --gpu-type a100-40
$PY -m tools.battle_server export --output "$HOME/act2-battle-results.tar.gz"
```

安全停止只给batch主进程发信号，不能用--full同时杀掉环境子进程，依据[Slurm scancel](https://slurm.schedmd.com/scancel.html)。Windows下载：`scp -J hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg:~/act2-battle-results.tar.gz local/archives/`。结果包只有JSON/metrics，不覆盖checkpoint。确认集只比较固定预算终点，解封后恢复只允许同一endpoint SHA；COMPLETE还需看comparison.pass_learnability，STOPPED/FAILED不算完成。请回传inspect、JOBID、Smoke与comparison；本地准备不能证明CUDA或学习收益。

默认研究run为`local/runs/ironclad-a20-battle-r1`。导出入口只收该run内的JSON/JSONL；相邻Smoke目录与Slurm原日志仍留在服务器，失败时先按上述命令查看和回传，不能把空结果包解释为通过。首次Smoke未完成时没有result.json，应查看battle-JOBID.err。

## 第二幕恢复与短战斗CPU诊断

最新结论与来源以status.md首节为准。Orrery通用恢复修复后旧normalstart release应拒绝新来源，不可通过重写旧凭证或checkpoint规避。下方A/B/C交接命令属于历史流程，当前不用于重启服务器。

在下节相同DL会话及CPU环境变量下执行（输出目录/文件必须全新，不能覆盖证据）：

```powershell
python -m pytest -q tests/simulator/test_nested_callback_restore.py tests/audit/test_short_battle_history.py tests/audit/test_short_battle_corpus.py tests/audit/test_repeated_boundary_diagnosis.py
python -m tools.prepare_act2_short_battles --output local/reports/act2-short-foundation/new-pilot --sources 32 --max-decisions 16384
python -m tools.verify_act2_short_battles --corpus local/reports/act2-short-foundation/new-pilot --output local/reports/act2-short-foundation/new-verification.json
python -m tools.analyze_act2_short_foundation --corpus local/reports/act2-short-foundation/new-pilot --output local/reports/act2-short-foundation/new-analysis.json
python -m tools.probe_short_battle_random --corpus local/reports/act2-short-foundation/new-pilot --output local/reports/act2-short-foundation/new-random-control.json
```

采集器固定来源角色、扫描配置种子范围、校验父SHA、隐藏CUDA并限制2线程。默认source_plan是同一个pilot种子计划；换输出重跑只算复现，不能增加独立样本数。扩展正式数据之前必须实现新来源登记、单一第三战根且到达后即停的采集模式；现工具收前3战并继续原episode以校准完整回报，不能充当封存最终确认采集器。载入检查SHA、来源、重复项、按run隔离、种子命名空间、baseline完整性和确认污染；不能仅凭文件哈希认定来源未在全历史中用过。已有corpus-r1及smoke来源重叠，禁止合计规模。

离线原版重放命令见下方A20节，新来源结果在`local/reports/act2-short-foundation/act12-replay/`；不启动原版、不改备份或journal。当前全量CPU只有旧来源资格拒绝1项失败，其余1898通过；新增最终18项聚焦验证单独登记，禁止报全量全绿。所有诊断保留原始失败输出，修订结果使用v2/v3新文件，不覆盖旧测量。

## 历史正常开局 A/B/C 准备（非本次入口）

使用DL环境，在当前PowerShell会话隐藏CUDA并限制CPU并行：

```powershell
& D:\Anaconda\shell\condabin\conda-hook.ps1
conda activate DL
$env:PYTHONPYCACHEPREFIX = 'D:\SLS\local\build\python-cache'
$env:CUDA_VISIBLE_DEVICES = '-1'
$env:SLS_TEST_CUDA = '0'
$env:OMP_NUM_THREADS = '2'
$env:MKL_NUM_THREADS = '2'
$env:OPENBLAS_NUM_THREADS = '2'
python -m tools.prepare_act12_normalstart_study --validate
```

该入口自身也强制隐藏CUDA、限制2个计算线程。CPU验证只运行有限小模型及至多2个正式worker，真实90M只做32步迁移/推理核验；不执行512局正式适配或1M训练。三组配置写入`configs/train/ironclad_a20_act12_normalstart_{a,b,c}_r1.toml`，不同内容拒绝覆盖；机器结果在`local/reports/act12-normalstart-study/`，含来源、种子扫描、JUnit和`server-commands.json`。旧checkpoint、配置、入口库及证据不改写。重复准备会重新扫描种子并重验当前来源，不等于精确复用旧结果。

`python -m tools.prepare_act12_normalstart_study --check-ready`检查已生成配置、实现、验证源码、种子登记、CPU凭证及已发布规则资格，成功表示本地可交接，`launch_ready=false`仍表示服务器正式规模检查未执行。新资格独立于旧混合课程，限定72项Act1–2规则/流程义务及8条自然canary，不是全内容或心脏资格。

证据包在`tests/fixtures/evidence/act12-normalstart/`，禁止改写。`tools/qualify_normalstart_act12.py`从当前来源的离线重放、实际原版备份恢复、合法边界和规则影响回归重新核验；`tools/publish_normalstart_release.py`检查实际Git基线native身份、原始载荷SHA并登记源码变更桥接。`--bind-release`只允许给尚未启动、其它内容完全相同的A/B/C配置绑定已验证证据；运行目录存在或配方不同均拒绝覆盖。

未来服务器命令在`local/reports/act12-normalstart-study/server-commands.json`，由人类执行。本轮不操作服务器、不提交或推送。授权同步后，在NUS `/home/h/hengzhi/SLS`先`git pull --ff-only origin main`，激活`/home/h/hengzhi/venvs/sls/bin/activate`，再执行正式规模benchmark与带benchmark的preflight（256步rollout、64 worker/16 shard），检查显存和精确恢复。`submit_slurm.py train --prepare`也会先执行这些准备门禁，通过后才进入约1M训练；初次先提交A组并回传结果，再依次B、C。不能只凭Slurm退出0判断完成，应核对manifest状态/累计步数；C的NO_ADAPTATION_GAIN为明确停止，不是完成PPO。

## A20 模拟器离线校验

本轮禁止本地GPU测试。使用DL环境并设置`PYTHONPYCACHEPREFIX=D:\SLS\local\build\python-cache`、`CUDA_VISIBLE_DEVICES=-1`、`SLS_TEST_CUDA=0`。以下工具不启动训练或原版游戏，输出使用新路径，不能覆盖旧原始证据：

```powershell
python -m tools.audit_stock_bytecode --stock-jar 'D:\steam\steamapps\common\SlayTheSpire\desktop-1.0.jar' --javap D:\java\bin\javap.exe --workers 8 --output local/audits/a20-validation-stage1/stock-method-branches.json
python -m tools.prepare_a20_validation_scope --ledger local/reports/fullrun-foundation/current-ledger.json --bytecode local/audits/a20-validation-stage1/stock-method-branches.json --stock-jar 'D:\steam\steamapps\common\SlayTheSpire\desktop-1.0.jar' --oracle local/build/oracle/fullrun-parity-r29.jar --stock-source-root local/audits/fullrun-current --natural-batch local/reports/act12-foundation/natural-0.json --natural-batch local/reports/act12-foundation/natural-1-retry.json --output local/reports/a20-simulator-validation/scope-final.json
python -m tools.revalidate_act12_simulator --foundation local/reports/act12-foundation --oracle-build local/build/oracle/Act12Foundation.build.json --output-directory local/reports/a20-simulator-validation/act12-replay
```

上列路径为本次已用位置，复跑须选择新输出位置。scope清单保留所有未知项及历史缺分支；命令成功仅代表索引/重放完成，必须读`stage1_complete`、`qualification`、逐条status和first_divergence。不完整前缀、条件匹配与终点资源子集均不可提升为无条件整项通过。历史journal核对的是原备份字节和当时恢复凭证，不要求今天仍在使用的游戏配置等于旧批次配置，也不会恢复或改动它们。

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
$env:CUDA_VISIBLE_DEVICES = '-1'
$env:SLS_TEST_CUDA = '0'
python tools/build_native.py --jobs 2
python -m pytest -q
python -m ruff check .
python tools/generate_policy_vocabulary.py --check
python tools/generate_content_registry.py --check
python tools/check_training_configs.py
```

按用户要求，禁止本机GPU测试；本地验证使用上述CPU设置，作用域仅当前会话。新增CUDA对照默认跳过，只有明确设置`SLS_TEST_CUDA=1`才可能执行；本机不要启用。服务器CUDA验证由人类在服务器运行，仍遵循本地验证、授权推送、服务器拉取的流程。GPU测试跳过不作通过证据。

按改动范围选择检查。native 规则变更需重建；仅文字整理不必重编。部分原版证据测试需要本机来源，不能把缺少证据的 skip 当作资格证明。源码、词表、回归 fixture、Java 探针和依赖锁文件都是项目资产。

本轮有界收尾的重点回归可用 `python -m pytest tests/simulator/test_suicide_order.py tests/simulator/test_exploder_fatal_stock.py tests/simulator/test_battle_master_deck_restore.py tests/simulator/test_chest_key_interactions.py -q` 重放；最终全量结论及原始诊断归档映射维护在 `status.md` 和 `local/reports/fullrun-foundation/bounded-closeout.json`。原版来源回放保留已有SHA和范围，不能用本地单元测试替代完整全幕资格。

每个 CLI 的准确参数以 `python tools/<命令>.py --help` 为准。构建、训练、提交、评估、恢复核验与导出分别使用 `build_native.py`、`train_full_run.py`、`submit_slurm.py`、`evaluate_checkpoint.py`、`verify_training_resume.py` 与 `export_policy.py`。

## 实机与 Oracle

运行 Slay the Spire 1 需要游戏本体、ModTheSpire、BaseMod、CommunicationMod 和本项目 Oracle。Git 不包含游戏或第三方 JAR。

审计规则必须绑定当前游戏JAR及实际类字节SHA；旧反编译目录仅用于定位方法，未验证来源时不能作为现用游戏规则证据。需要阅读时优先使用与类SHA绑定的javap字节码，或从同一已核验JAR重新反编译。受控实机仍须独立登记原始载荷、Oracle来源、脚本、恢复journal和适用范围。

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

本地工作默认在本地完成并验证，任务结束不自动提交或推送；修改保留到未来统一推送。只有人类明确要求推送，或改动需要用于服务器训练时，才提交并推送 `main`。更新文档、完成审计或整理项目本身不代表需要推送。模型、训练状态、游戏文件、个人配置和凭证不提交 Git。

需要服务器训练时，流程是本地修改与验证 → 提交并推送 `main` → 人类在 NUS 拉取 → 人类执行服务器命令并反馈输出。远端分支清理须有明确授权，先核查并保全未合入 `main` 的历史；删除旧分支不改变 `main`，也不意味着发布当前本地修改。

NUS 操作由人类执行。先同步代码，再按当前已核验实验提供完整命令；不要直接重跑旧文档的 sbatch 命令，不在 login node 训练，也不在正在训练的目录热替换代码。本次本地整理未执行服务器操作。

原版规则或观测改动必须说明前后行为、回归证据与模型兼容性。训练方案至少登记配置、源码、父 checkpoint SHA、种子区间、runtime、worker 布局、预算、选模规则与独立终评。

### NUS 服务器已知环境与只读研究

外部访问统一通过跳板机。2026-10-17用户确认的登录入口为 `hengzhi@xlogin.comp.nus.edu.sg`，跳板机为 `hengzhi@sjump.comp.nus.edu.sg`。后续提供SSH、SCP命令时必须包含 `-J`；登录后提示符出现的 `xlogin1`、`xlogin2` 是实际登录节点，不应据此替换用户已确认的外部入口或省略跳板机。

在本地PowerShell登录：

```powershell
ssh -J hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg
```

在本地PowerShell下载服务器文件（此例保存到桌面；按实际文件替换远端路径和本地文件名）：

```powershell
scp -J hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg:/home/h/hengzhi/SLS/local/reports/normalstart-c-failure.json "$env:USERPROFILE\Desktop\normalstart-c-failure.json"
```

上传使用同一跳板机，将SCP源和目标改为本地文件及 `hengzhi@xlogin.comp.nus.edu.sg:/home/h/hengzhi/SLS/<目标路径>`。连接和文件传输由人类执行；不要在命令或文档中保存密码。

2026-10-12依据用户提供的提交和调度输出：使用SoC集群登录节点 `xlogin1.comp.nus.edu.sg`，账户 `hengzhi`，项目路径 `/home/h/hengzhi/SLS`，Python入口 `/home/h/hengzhi/venvs/sls/bin/python`；Slurm账户 `allusers`、QoS `normal`。历史提交作业号为934705，用户随后确认训练正常开始；这不代表已经取得当前运行状态或实际分配节点。

当前v6启动脚本申请一张 `a100-40` GPU、16 CPU、64G内存，分区 `gpu-long`、72小时上限、提前900秒TERM通知；训练配置自身预算66小时，64环境/16 shards。这些是申请和配置值，实际硬件、分配与吞吐需以运行记录为准。用户此前的 `sinfo` 输出还显示A100 80GB及H100池，`gpu`分区3小时、`gpu-long`分区3天；节点状态和账户权限随时可能变化，不据此自动切换GPU或修改正在执行的训练合同。

研究分两步：先在登录节点读取作业、分区、账户关联、源码版本、日志、manifest和存储容量/配额；再根据结果判断是否需要独立计算节点诊断。登录节点不跑训练、GPU基准或加载大checkpoint，不通过新作业争抢当前训练资源。只读研究不需要Git拉取或推送；保持当前作业源码、配置和文件位置。仍缺当前作业号/节点、GPU实际利用率与显存、CPU效率、训练决策吞吐和评估耗时、文件系统类型/配额与checkpoint写入耗时的运行证据。

集群背景参考 [NUS SoC GPU指南](https://www.comp.nus.edu.sg/~cs3210/student-guide/soc-gpus/) 与 [Slurm指南](https://www.comp.nus.edu.sg/~cs3210/student-guide/slurm/)。课程指南说明基础使用方式，不替代用户账户的实际调度权限或当前集群配置；其它NUS集群的存储、清理和配额规则不能套用到此SoC环境。
