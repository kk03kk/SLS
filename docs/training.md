# 训练、评估与产物管理

## 实验合同

主要研究目标由任务合同明确登记：角色、Ascension、正常开局分布、终点及成功判据。当前工作围绕战士 A20，Act1/Act2 是阶段目标，完整 Heart 目标还涉及双 Boss、三钥匙和 Act4。

训练种子、周期选模种子、开发确认种子和最终保留种子分开。保留集在候选确定前封存；周期峰值不自动成为独立确认结果。对照模型须使用相同环境、runtime、种子和评估方法。

固定终点与周期所选 checkpoint 分别报告，不能因文件叫 `final` 就认定它是训练终点。循环终止计为策略失败并披露，backend 错误、超时与执行故障另外核对。条件子组和不同种子块的百分比不能替代合法配对比较。

## 续训与迁移

- 精确恢复要验证并保留权重、Adam、worker、RNG、记忆、配置与 runtime 合同。
- 相邻任务的 weights-only 迁移重新初始化优化器和环境状态，不称为精确续训。
- native 规则、Observation/Action、词表、reward、PPO、布局和软件栈变更按合同检查；没有证据时不添加兼容豁免。
- 整理后的证据路径解析属于训练实现的来源变化，已有作业不能绕过旧 source gate 直接恢复；未来提交须重新核验相应合同。

历史 `configs/`、manifest、checkpoint 和证据 JSON 保留原字节及哈希，不通过改 hash 把旧实验包装成当前实验。旧路径可由 `src/sls/evidence_paths.py` 定位到新目录，验证仍核对原始证据字节。

v6角色/字段编码是可选研究架构，当前训练默认和历史配置仍使用v5。90M父权重可用 `migrate_v5_role_policy` 作显式weights-only迁移；部署导出和正式训练需另行登记新架构、来源、预算及配对验收，不能把局部迁移验证当成第二幕能力提升或服务器启动资格。新模型中的option variant和绑定参数需要后续学习，迁移初始化刻意保留旧预测。

## 本地产物位置

首段第二幕课程拟新增20M decisions，全段50%正常开局、50%真实第二幕入口，以episode开始次数计数；不是每个rollout固定各半。真实入口库从冻结90M策略的正常A20开局生成，独立合法动作前缀重放必须与入口checkpoint完全一致，保留全部到达入口，不筛选第二幕胜者或高血量状态。入口按固定顺序轮转，保存轮转计数、episode来源及分组结局；恢复时库SHA、比例、来源和策略输入合同必须一致。更换课程比例须登记新任务或明确迁移，不用于精确恢复旧run。

正式周期与最终评估均从正常开局运行，只按第二幕通关数选模；第一幕到达、循环/步数限制和分组结局用于解释。循环与步数限制计为失败局，实际后端异常、后端截断和超时使评估不可用。入库过程中策略的记忆不能带入新训练episode。没有提高初始血量，也没有自动拿奖励规则。

三天作业限制通过提前TERM通知、训练自身时钟截止和完整latest保存处理。正常处理停止时进程可返回0，但run-manifest必须写INTERRUPTED而不是COMPLETE；重复启动同一配置从latest继续累计到目标，不重新做90M weights-only初始化。第一次初始化只迁移父模型权重，重新建立Adam与训练episode，并验证独立来源迁移证据。服务器Linux/A100的构建、吞吐、显存与微训练恢复仍由该服务器准备流程实测，本地GPU诊断不替代它们。

| 位置 | 内容与保留方式 |
| --- | --- |
| `model/` | 可部署推理 `.pt` 与来源 `.json`；默认历史56M、76M/90M候选及两份旧pilot导出分开保留 |
| `local/runs/` | 90M父主线、旧Act1–2 pilot、两组λ实验，以及相关准备记录 |
| `local/archives/` | 当前90M来源原包、λ原包、critic20m及相关第二幕下载记录 |
| `local/audits/` | 第二幕/晚幕规则轨迹、当前迁移证据与测试所需的原版来源 |
| `local/reports/` | 当前诊断输出；旧报告工作副本已移出项目，保留最新独立审计 |
| `local/build/` | 工具链、当前native/Oracle与构建记录；混有运行依赖和恢复材料，不整目录删除 |
| `local/operator/`、`local/logs/` | 本机路径、服务器操作脚本及实机日志，保持私有 |
| `tests/fixtures/evidence/` | 可移植、小型、不可变的回归和实验证据，随源码提交 |

2026-10-11进一步整理：历史原包、56M/70M与奖励消融工作副本、90M中间快照、旧Act1审计和历史构建已移到项目外 `D:/SLS-archive/20261011-local-cleanup/`。搬迁清单逐文件记录原路径、新路径和SHA256；没有永久删除原包或权重。

90M主线保留 `final.pt`、`latest.pt`、编号90M终点、76M周期所选checkpoint及其身份/评估记录。第二幕旧pilot、两组λ终点和最新critic下载仍保留，便于下一阶段父模型比较与问题诊断。`local/archives/critic20m/` 保存10-10原包。

99份Oracle与28份演示备份的journal均为RECOVERED、无恢复失败记录，已原样归档。当前工具链、native、Oracle和安装备份保留。旧60M操作笔记、被fix2替代的启动脚本及旧构建日志也已归档；operator中保留最近的fix2与其核验记录。未来运行会自行创建新的日志/恢复目录。

历史脚本或配置如需读取已归档的私有文件，先按项目外 `relocation-manifest.json` 恢复到原路径，再执行旧流程；不要把历史目录恢复成当前待提交实验，也不要直接运行已归档的恢复journal。旧记录中的绝对backup路径保持原样。归档仍是原文件的搬迁，不等于新增另一份独立备份。

每个新实验使用唯一 run 名，保留配置、manifest、原始metrics、逐seed评估、被采用的checkpoint及恢复状态。停止或失败的实验照实登记到 [status.md](status.md)，日常说明持续更新本文件，不新增分析日记。

## 模型导出与分享

```powershell
python tools/export_policy.py <checkpoint> --output model/<name>.pt --goal ACT1 --ascension-min 20 --ascension-max 20
```

按真实模型目标设置 `--goal`。分享推理文件与同名身份 JSON，训练 checkpoint 不作为 live 模型。模型列表可用 `python tools/play_live_inspector.py --list-models` 检查。推理模型、游戏和 Mod 不随 clone 获取。

## 首段混合课程服务器启动

当前冻结配置为 `configs/train/ironclad_a20_act12_real_entry_20m_r1.toml`。整段新增约20M、50%正常开局/50%真实第二幕入口，固定 v5、正常资源、正常开局第二幕通关选模。实际比例按新 episode 开始次数计算；入口清零策略记忆，不继承此前轨迹。正常开局评估始终不使用入口库。配置、资格和显式 weights-only 来源迁移均由 SHA 固定，旧90M checkpoint 不修改。

本地已通过规则/接口验收、18,432 决策混合 PPO 精确恢复及真实训练 CLI 预检。正式64:16布局、序列64、minibatch16未在本地8GB显存上通过；A10040GB上的完整准备是启动条件，不能跳过。`--prepare` 在每个作业验证库SHA与所有合法动作前缀，检查GPU、正式配方benchmark及保存恢复，然后才执行训练；Windows诊断凭证不上传为Linux ready凭证。

真实 CLI 的独立小批量诊断从90M权重初始化，45秒预算后24次更新保存为 INTERRUPTED；再次按同一配置启动，继续4次更新、256决策，累计目标未重置。该诊断共1792个新决策，没有导出或采用策略；来源与日志SHA记录在 `tests/fixtures/evidence/act12-foundation/training-readiness.json`。诊断 checkpoint 在任务收尾时归档，不是正式20M作业的起点。

先将本地 `D:/SLS/local/runs/preparation/act12-mixed20m/entry-bank-release.json.gz` 上传到服务器同样的相对路径。文件SHA必须是 `71a972903827712ff808440bac7a7e8bc8de1f3dc8980e5bd1330666b42d1745`。90M父checkpoint沿用服务器现有文件；若缺失，也上传本地 `local/runs/ironclad-a20-act1-win-90m-continuation/final.pt`，SHA见下列命令。这两个私有文件不进入Git。

在服务器执行：

```bash
cd /home/h/hengzhi/SLS
git switch main
git pull --ff-only origin main
mkdir -p local/runs/preparation/act12-mixed20m
export SLS_PY=/home/h/hengzhi/venvs/sls/bin/python
export PYTHONPATH="$PWD:$PWD/src"
export PYTHONPYCACHEPREFIX="$PWD/local/build/python-cache"
export CUBLAS_WORKSPACE_CONFIG=:4096:8
printf '%s\n' \
  '71a972903827712ff808440bac7a7e8bc8de1f3dc8980e5bd1330666b42d1745  local/runs/preparation/act12-mixed20m/entry-bank-release.json.gz' \
  '274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0  local/runs/ironclad-a20-act1-win-90m-continuation/final.pt' | sha256sum --check --strict
```

只有拉取及两个SHA检查均成功，才执行：

```bash
"$SLS_PY" tools/submit_slurm.py train \
  --config configs/train/ironclad_a20_act12_real_entry_20m_r1.toml \
  --prepare --python "$SLS_PY" \
  --account allusers --qos normal --partition gpu-long \
  --gpu a100-40 --cpus 16 --memory 64G --time 3-00:00:00 \
  --stop-signal-seconds 900
```

把作业号和 `local/runs/slurm-logs/sls-train-<jobid>.out/.err` 的准备结果贴回检查。当前冻结run目录为 `local/runs/ironclad-a20-act12-real-entry-20m-r1`。训练从父累计90,013,696走到110,013,696，按完整16,384决策更新取整，预计新增20,004,864。作业内部66小时预算、Slurm三天上限及15分钟提前通知共同用于安全停止；不保证一个作业一定完成20M。

状态为 `INTERRUPTED` 时，重新执行同一提交命令，继续原 `latest.pt`、Adam、worker、随机数、记忆、课程轮转计数和累计目标。不要改output、布局、比例、奖励、模型或目标。`FAILED` 时保留日志和crash，先检查原因；`COMPLETE` 时不再提交该run。运行中不拉取变更代码；续提也使用相同冻结源码，后续课程调整另作显式迁移。

本地最终证据可重现核验（Conda DL、已设置项目PYTHONPATH）：

```powershell
python -m tools.verify_act12_foundation_checks --root local/reports/act12-foundation
python -m tools.qualify_act12_foundation --root local/reports/act12-foundation --output local/reports/act12-foundation/qualification-recheck.json
python -m tools.build_act2_entry_bank --verify-only --output local/runs/preparation/act12-mixed20m/entry-bank-release.json.gz
```

资格工具拒绝覆盖已有收据；再次核验应使用新的输出名。原版原始载荷和恢复journal需要当前本地布局；公开Git中只有小型不可变凭证，不能在缺少私有原始证据的clone上声称重跑了完整原版验收。无条件 Discovery 匹配与条件解释继续分别记录。
