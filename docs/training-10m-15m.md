# Ironclad A0 FullRun：10M → 15M 训练准备

本轮准备完成于 2026-09-05。**采用显式 v3→v4 model warm-start，不能 exact resume。**
job 821775 后的 contract 修订与直接重试命令见 [runtime contract审查](training-runtime-contracts.md)。
已有验证可通过受hash约束的审查转换复用；本页的完整准备流程适用于首次建立训练链。
保留全部旧网络参数，重置 Adam、在途环境、循环记忆、episode limiter 和 RNG 流。
累计计数从 10,002,432 steps / update 814 继续；验证用的更新不计入正式训练。
本地没有重新运行大规模模型 evaluation，没有提交服务器训练作业。
本地验收：416 tests passed、1 skipped（历史 v3 输入被当前编码正确拒绝），Ruff 与
diff whitespace 检查通过。真实 10M 模型以 CPU、2 workers、64-step rollout 完成
更新/恢复/同一步重放验证，验证更新已丢弃；该报告明确 `production_ready=false`。
完整 48-worker A100 验证及新环境固定面板 baseline 仍须在服务器通过。

## 已有训练的独立结论

输入来自 `runs/local/runs/ironclad-a0-fullrun-v3` 和 `slurm-logs/sls-train-820787.*`。
可复现分析工具为 `tools/analyze_training_history.py`。机器证据保存在
`local/audits/10m-15m/{history,checkpoints,local-compatibility}.json`。

固定 seeds `[3000000000000, 3000000001000)` 的结果：

| 模型实际步数 | FullRun 通关 | 进入 Act 2 | 进入 Act 3 | 失败楼层中位数 | 平均终局 reward |
| --- | ---: | ---: | ---: | ---: | ---: |
| 7,004,160 | 2/1000 | 75.1% | 8.7% | 28 | -0.569792 |
| 8,011,776 | 0/1000 | 72.0% | 6.6% | 25 | -0.593616 |
| 9,007,104 | 2/1000 | 73.2% | 7.1% | 25 | -0.587296 |
| 10,002,432 | 3/1000 | 65.5% | 6.1% | 24 | -0.610640 |

不能把 2→3 次通关称为提升：对应 Wilson 95% 区间约为 0.055%–0.726%
和 0.102%–0.878%。Act 2 下降 9.6 个百分点和失败楼层回退更值得关注。
结论是**未显示 FullRun 的可靠进步，并伴有前中期能力回退**，不是单纯平稳平台。
旋转 256 seeds 的波动不能作为模型间提升证据。

训练系统本身：244 次连续更新，每次 12,288 个决策，无缺号；学习指标无 NaN/Inf。
10,958 次死亡、5 次训练通关、0 backend truncation/cycle limit/step limit。
训练策略使用采样，固定评估使用确定性动作，不能直接比较两者的通关率。
最初/最后 50 次更新：normalized entropy 0.341→0.334，未见整体熵坍缩；
value explained variance 0.566→0.499，final KL 0.0191→0.0223，clip fraction 0.144→0.150。
现有日志没有按决策域拆分的熵，不能据总体熵断言每类行为探索都充分。
64.3% 的更新带 KL stop 标记，但真正只做一个 epoch 的是 13/244；不能混淆二者。

所有已下载分段 checkpoint 的模型与 Adam 张量有限。`latest.pt` 与 `final.pt`
都在 update 814，模型张量完全相同。选定源文件为 `final.pt`，SHA256：

```text
24b920f81417d0c289c44f8eace4f1f1b466f94bb0817029b17a63128b96749b
```

stderr 的 NumPy warning 在这次任务中无害：后续训练及周期评估均正常完成，
没有使用 NumPy 的失败 traceback。真正异常是 Slurm 的 Terminated/CANCELLED：
stdout 停在最终 2000-seed evaluation 的 66,000 个决策、0 个完整 episode。
因此**旧最终 held-out 评估没有完成**。不能由日志确定是人工取消还是其他外部终止。
manifest 的 RUNNING、7M 计数以及旧 contract error 是残留状态，不是 10M 文件不存在；
新代码将旧错误移入 previous_failures，并保留之前修复的信号保存/最终化恢复逻辑。

旧 `boss_success_rate` 的分母是按预定 Boss 分组的章节流程，包含到 Boss 前的死亡；
不是“实际进入 Boss 战后的胜率”。Boss 战动作统计的 entries 才是实际进入次数。
当前代码已修复部分 Boss encounter/monster ID 对不上及 Slime Boss 分裂统计问题，
不要把新旧动作细项直接视为相同测量口径。

## 兼容性和具体迁移

旧 checkpoint 的 Git 是 `6aeaa9f6c5fda8c8e1b6479b54af440e4bf92f51`，
编码 v3；当前编码 v4。模型结构仍是 recurrent relational policy v5。
变化包括 power-owner 关系、动态卡牌字段、瓶装标志、Match attempts/双卡引用、
已选卡牌及顺序，以及 Simulator 的升级费用、多选、药水、Lizard Tail、Tea Set 等修复。
reward schema、PPO 数学、FullRun horizon 没有随这些修复改掉。

`src/sls/rl/model_migration.py` 校验旧词表哈希、配置、参数名、维度和有限性，
按字段名映射数值与 presence-mask 两个独立块，按 token 映射 embedding。
**全部 1,261,572 个旧参数元素保留**；新网络 1,266,692 个元素，新增 5,120 个权重置零。
不能简单在旧矩阵右侧补零，否则旧 presence-mask 列会错位。
Transformer、GRU、actor、critic 和旧关系投影均保留。

新公开关系、实体和真实环境会改变输出，迁移不保证与旧环境同动作或同胜率。
新增输入权重可继续学习；没有删掉新信息来伪造兼容性。Adam 重置，避免把旧输入/
旧环境下的动量当作新优化问题的精确状态。环境从旧 next_seed=10048819 后的新自然
起点开始；计数保留是 lineage，不代表 optimizer 或 RNG 被 exact resume。

10M 仍有较强 Act 1 能力，值得保留。7M 固定面板的进展更好，也必须保留为备份；
不能宣称 10M 是所有旧候选中最优。如果新环境 10M baseline 不过下述门槛，停止并
复查迁移/候选源，不能凭借“参数复制成功”就开始 5M 长训练。

## 下一阶段设置

配置：`configs/train/ironclad_a0_fullrun_15m.toml`。

| 设置 | 下一轮 | 相对旧配置 |
| --- | --- | --- |
| profile/start | Ironclad A0 FullRun，自然 Neow 起点，不要求 Heart | 不变 |
| workers/shards | 48 / 16 | 不变；只验证这个已有可用布局 |
| rollout/sequence/minibatch sequences | 256 / 64 / 16 | 不变；每更新 12,288 个样本 |
| epochs / clip / target KL | 2 / 0.2 / 0.02 | 不变 |
| learning rate | 0.000125，常数 | 从 0.00025 减半 |
| gamma / GAE lambda | 1.0 / 0.98 | 不变 |
| value coefficient / value clip / grad norm | 0.5 / 0.2 / 0.5 | 不变 |
| entropy | 0.02→0.002，40M 线性衰减，累计步数继续 | 10M 约 0.0155，15M 约 0.01325 |
| reward / potential / failure progress | progress-v3 / 0.2 / 0.8 | 不变 |
| max episode decisions / boundary visits | 4096 / 4 | 不变 |
| target | 15,000,000 | 对齐整次更新后实际为 15,003,648，新增 5,001,216 |

学习率减半是结合后期 KL/能力回退、新环境输入变化、Adam 重启选择的保守幅度，
不是证明该数值最优。保持其余主要学习设置，以免多项变化无法归因。

reward 不是纯稀疏通关信号：失败得分 `-1 + 0.8*floor/50`，通关 +1；另有
`0.2*(gamma*Phi(next)-Phi(current))`，终点 potential 为零。gamma=1 时形势项
沿完整 episode 抵消，不会靠反复回血/绕圈增加总目标。较远的失败仍优于较早失败。
GAE lambda=.98 的直接残差半衰期约 34 个决策，长程 credit 主要依赖 bootstrap
critic；64 是截断反向传播长度，不是把整局记忆每 64 步清空。
这确实限制稀有后期成功的传播，但现有日志不足以证明调 lambda/胜利奖励倍数会更好；
critic 仍有约 0.5 explained variance，因此保留 critic 和 reward，比无依据重写目标稳妥。
不强制切回 Act 2、不伪造后期起点、不同时增大学习率/熵/奖励尺度。

## Evaluation / checkpoint 方案

- 新环境的迁移 10M baseline + 11M、12M、13M、14M、15M：始终使用同一固定
  1000 seeds `[3000000000000,3000000001000)`，greedy、同一环境/编码、上限4096。
  评估按整次 PPO 更新边界触发，报告实际步数，不伪装成精确整数百万。
- 每 0.5M 保存里程碑 checkpoint，并用 256 个旋转 seeds 做诊断；从 4e12 开始，
  stride=256。旋转结果不选 best，不用于与固定面板混算。
- `latest.pt` 用于异常恢复；`initial-10m.pt` 是固定迁移起点；每0.5M的文件保留。
  `best_progress.pt` 与原始终点 `final.pt` 分开，不能用 best 冒充15M。
- 新 best guard：相较 incumbent，若 Act 2/3 到达率的 Wilson 区间已完全下降，
  而通关区间未明确分离，则拒绝仅由少数额外通关驱动的晋升。原始快照仍保留。
  这是保守选模规则，不是正式配对检验，也不意味着通过 guard 就证明进步。
- 最终保留的2000 seeds `[2000000000000,2000000002000)` 不用于训练或周期选模。
  训练结束自动评估 best；结束后再对迁移初始10M和原始15M作同一held-out端点比较。
  不根据这个面板反复调参。稀有成功下2000仍不能辨别0.2%与0.3%的细小差异，
  需结合 Act 2/3、失败楼层及成功置信区间判断。
- 旧环境10M成绩与新环境baseline分别标注；只把新环境baseline之后视为同条件曲线。
- 旧0.5%通关、65%到Act2、8%到Act3及最差Act1 Boss分组60%的最终导出门槛保留；
  未达门槛仍保存训练终点，但不宣称获得合格部署模型。

按旧约77 decisions/s，新增学习约18小时；加固定/旋转/最终端点评估，预留约24–30小时，
并以新A100验证测量为准。正式作业给48小时；不把省略评估当成提速。

## 本地完成 → GitHub

代码已在当前工作树实现；没有代替用户提交/推送 Git，也没有上传历史产物。
提交前确认包含原 Observation/Simulator 修复、新迁移工具、v3/v4 两份词表、新配置和测试。
不要提交 runs/local 下的大文件。一个可用的本地提交顺序：

```powershell
cd D:\SLS
git diff --check
git add README.md docs model/README.md native src tests tools configs/train/ironclad_a0_fullrun_15m.toml pyproject.toml
git commit -m "Prepare validated v4 warm start from 10M to 15M"
git push origin main
```

## NUS 顺序执行

在 xlogin 做 Git、安装纯Python项目及提交；所有 native build、CUDA 验证/评估/训练
均由下面工具提交到 Slurm compute node。保留现有 Python3.12 / torch2.6+cu124 环境，
不为 NumPy warning 重装 PyTorch。每一步必须等前一个作业成功，不能一起盲目排队。

```bash
cd /home/h/hengzhi/SLS
git pull --ff-only
export SLS_PY=/home/h/hengzhi/venvs/sls/bin/python
export CUBLAS_WORKSPACE_CONFIG=:4096:8
$SLS_PY -m pip install --no-deps -e .
sha256sum local/runs/ironclad-a0-fullrun-v3/final.pt

# 1. 构建新native，做Linux/A100通用preflight，写 local/runs/preflight.json。
$SLS_PY tools/submit_slurm.py preflight --python "$SLS_PY" --time 01:00:00

# 2. 第1个作业COMPLETED/ExitCode=0且preflight.ok=true后，仅验证原48:16布局。
$SLS_PY tools/submit_slurm.py benchmark --python "$SLS_PY" \
  --benchmark-layouts 48:16 --benchmark-output local/runs/worker-benchmark-15m.json

# 3. benchmark成功后：实际10M迁移+完整48worker/256rollout更新及exact重放。
$SLS_PY tools/submit_slurm.py warm-start --python "$SLS_PY" \
  --checkpoint local/runs/ironclad-a0-fullrun-v3/final.pt \
  --config configs/train/ironclad_a0_fullrun_15m.toml --time 02:00:00

# 4. 第3步成功后提交正式作业。它会先做新环境10M固定面板baseline，过门才更新。
$SLS_PY tools/submit_slurm.py train --python "$SLS_PY" \
  --config configs/train/ironclad_a0_fullrun_15m.toml \
  --resume auto --partition gpu-long --time 2-00:00:00
```

不重新跑旧的 smoke/pilot curriculum：完整模型warm-start验证已覆盖新链路，不能
拿随机小网络smoke替代它。此次 native/Observation 变更必须新preflight和benchmark，
但没有必要机械扫描五套已比较过的workers布局。提交工具默认A100-40GB、16CPU、64G。

## 长训练硬门槛与恢复

1. 源SHA必须匹配上文；旧checkpoint任何字段都不被原地改写。
2. Linux/A100 preflight成功，native源/二进制哈希一致；48:16 benchmark用同一二进制。
3. production warm-start 用完整模型和真实生产PPO尺寸验证：参数/指标有限、确实更新，
   保存恢复后同一步更新指标和全部模型张量一致。首个完整更新 KL>0.05、clip fraction>0.5
   现在记诊断warning；单步统计阈值不是checkpoint兼容性硬门槛。
4. `migration.json` 必须 `production_ready=true`；CPU微验证不能授权生产。
   训练入口校验相关训练实现/native哈希及初始checkpoint文件哈希。无关工具或文档变化不要求
   重验；相关实现变化须重验或有绑定新旧摘要的明确审查转换。配置按训练identity保护。
5. **训练入口先跑1000-seed新环境baseline**：Act2≥60%、Act3≥4%，且无backend错误、
   truncation/cycle/step limit。60%/4%是相对旧65.5%/6.1%预设的保留能力下限，
   是计算预算保护门槛，不是声称统计等效或奖励超参数。失败时记录结果并停止，不能继续更新。

中断后，保持代码、配置、Python/Torch、native和worker布局不变，重复第4条正式提交命令；
它从新链路 latest 恢复。不得再次运行 warm-start 覆盖新链路，也不要用旧7M环境迁移工具。
看到error先读 `.err`；没有 final-evaluation.json 且manifest未COMPLETE时，不称为最终评估完成。

## 训练完成后的独立端点评估

以下为两个独立Slurm作业；profile必须显式FullRun（工具仍保留旧默认Act1供兼容）。
已有自动best评估，不必重新评估best。初始10M与原始15M同一held-out面板，各跑一次：

```bash
$SLS_PY tools/submit_slurm.py evaluate --python "$SLS_PY" \
  --checkpoint local/runs/ironclad-a0-fullrun-v4-15m/initial-10m.pt \
  --evaluation-profile IRONCLAD_A0_FULLRUN --evaluation-episodes 2000 \
  --evaluation-seed-start 2000000000000 \
  --evaluation-output local/runs/evaluations/v4-initial-10m-heldout.json

$SLS_PY tools/submit_slurm.py evaluate --python "$SLS_PY" \
  --checkpoint local/runs/ironclad-a0-fullrun-v4-15m/final.pt \
  --evaluation-profile IRONCLAD_A0_FULLRUN --evaluation-episodes 2000 \
  --evaluation-seed-start 2000000000000 \
  --evaluation-output local/runs/evaluations/v4-final-15m-heldout.json
```

最后同时保留 config、源码revision、migration、preflight、benchmark、manifest、metrics、
stdout/stderr、initial/latest/best/final/每0.5M快照和三个held-out结果。不要仅下载一个best文件。
