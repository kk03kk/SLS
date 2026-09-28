# A20 第一幕：突破平台期的执行流程

## 当前已完成的准备

已实现一键基线与失败诊断作业，并准备约 2M 新决策的奖励对照配置。
本地未执行长评估、未训练新模型。56M 仍为默认演示模型。

第一阶段先取得当前模拟器下的真实基线，分析失败资源和动作；第二阶段
比较奖励目标。不会从历史 77.39% 推算修复后环境胜率，也不会把奖励
改动已经写入代码表述为已验证有效。

## 第一阶段：只提交一次诊断作业

使用 GitHub 同步时，在 NUS 登录节点复制下面整段。它遇到未提交的源码
修改会停止；不会重置工作区或删除训练结果。更新到 `main` 后校验 56M
checkpoint，再提交计算节点作业：

```bash
bash <<'BASH'
set -euo pipefail
cd /home/h/hengzhi/SLS
if [ -n "$(git status --porcelain)" ]; then
  echo '服务器源码有未提交修改，请贴出 git status 输出；本次未提交作业。' >&2
  git status --short
  exit 1
fi
git fetch origin
git switch main
git merge --ff-only origin/main
printf '%s  %s\n' \
  '9555c8608155ba262901757cd57d76f854f1cd76a5b6126375e832fa0a714710' \
  'local/runs/ironclad-a20-act1-v4-60m-stable/stages/train/selection/best_progress.pt' \
  | sha256sum --check
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py plateau \
  --config configs/diagnostics/ironclad_a20_act1_plateau.toml \
  --constraint xgpg
BASH
```

打印的数字为 Slurm job ID。作业结束后读取
`local/runs/act1-plateau-56m-v1/status.json` 和 `report.json` 即可。
该命令只启动诊断，不启动第二阶段的 2M 训练。

在服务器取得本次源码后，确认原始 checkpoint 存在于：

```text
local/runs/ironclad-a20-act1-v4-60m-stable/stages/train/selection/best_progress.pt
```

提交：

```bash
python tools/submit_slurm.py plateau --config configs/diagnostics/ironclad_a20_act1_plateau.toml --constraint xgpg
```

此命令用当前环境的 Python；如服务器需要指定既有环境，添加
`--python /你的环境/bin/python`。默认 1 A100 GPU、16 CPU、64 GiB，
`gpu` 分区、3 小时。实际 Torch、构建、预检和评估都在计算节点发生。

作业顺序：

1. 校验 checkpoint SHA256 和当前 native 源码身份，要求物理 A100。
2. 新 native 构建与现有预检流程，含小规模迁移和保存恢复检查。
3. 从完整开局评估 56M：512 个开发种子，从 `8000000000000` 起。
4. 从完整开局另评估 256 个诊断种子，从 `1000000000` 起，属于训练
   种子范围。开发种子不用于制作练习状态或加入后续训练。
5. 从诊断结果按 Boss、成功、Boss 死亡、早死选择最多 100 局，重新记录
   全部观察、合法动作、决策、native 状态及 RNG 快照，并逐步重放验证。
   某个分层没有足够局数时保留实际数量，不复制补齐。
6. 汇总 Boss 入场生命、牌组大小、药水和局部动作顺序例子，写出报告。

主要结果：

```text
local/runs/act1-plateau-56m-v1/report.json
local/runs/act1-plateau-56m-v1/status.json
local/runs/act1-plateau-56m-v1/baseline.json
local/runs/act1-plateau-56m-v1/corpus/analysis.json
```

`report.json` 即可作为下一步分析输入。每阶段 stdout/stderr、原始结果、
快照和状态日志都会保留。身份不符、种子缺失、非有限结果或运行错误会
停止作业。输出目录必须是新目录，拒绝覆盖证据；中断后可以复制诊断
配置并更换 output 重跑。这里未实现整条诊断管线的断点恢复。

分层分析不是总体胜率估计。不同推理 batch 大小可能改变 argmax 轨迹；
这些重采集差异会记录，不被解释为规则差异或当作旧结果的精确复现。
规则审查目前暂停，完整一致性仍未认证。

## 第二阶段：预算有限的奖励对照

配置：

- `configs/train/ironclad_a20_act1_plateau_progress_2m.toml`：现有进度奖励。
- `configs/train/ironclad_a20_act1_plateau_win_2m.toml`：成功 +1，真实死亡 −1。

两者从 SHA256
`9555c8608155ba262901757cd57d76f854f1cd76a5b6126375e832fa0a714710`
的 56,000,512 步模型迁移全部权重，目标累计 58,000,000 步。
64 workers，每次 16,384 新决策，按完整更新完成到 **58,015,744**，
新增 **2,015,232**。优化器、环境、随机流和循环状态重建；不精确续训。

只改变 reward schema 和失败进度系数，及必要的输出/准备目录。两者
使用相同训练起始种子 120000000、学习率 3.125e-5、熵 0.002、gamma=1、
模型/PPO 参数和潜势 shaping。旧 critic 也保留，奖励尺度改变后的价值
适应是实验的一部分，可能造成短期波动。

新 schema `sls-curriculum-win-v1` 必须显式选择并设失败进度系数为零、
gamma=1。历史 progress schema 继续要求系数严格大于零，因此旧配置的
语义没有变。保存恢复契约包含完整 PPO 配置，不能以精确恢复方式偷偷
替换奖励目标。完整轨迹的潜势项在 gamma=1、终止势为零时抵消至初始
势常数；仍保留它提供局部学习信号。

第一阶段结果复核后，执行两个配置的 `train --prepare`：

```bash
python tools/submit_slurm.py train --config configs/train/ironclad_a20_act1_plateau_progress_2m.toml --constraint xgpg --prepare
python tools/submit_slurm.py train --config configs/train/ironclad_a20_act1_plateau_win_2m.toml --constraint xgpg --prepare
```

这些命令目前仅为准备好的下一阶段入口，没有从诊断作业自动启动。
不要在相同 checkout 同时进行构建和训练；推荐依次提交、完成后再提交
下一个配置。相同训练种子不是完全相同的经历：两条策略变化后路线会
不同。重复随机种子的验证仍需后续补做。

周期选模使用开发种子 `8000000000000` 起的 512 个；两配置所谓
`final-evaluation` 使用 `8000001000000` 起的 512 个开发确认种子。
**它不是最终未见测试集**。`9000000000000` 起的 2,048 个种子留到方案
确定后再作独立最终评估。6e12/7e12 历史评估种子仍在训练范围外。

筛选看完整幕通关率、同种子胜负变化及逐 Boss 成绩，不能比较不同奖励
定义的 mean_reward 来决定模型强弱。不要把 512 局的微小差值直接判定
为突破。保留每个实验内的初始化模型和历史 champion；默认演示模型不
被这两个实验自动替换。

## 源码传递，无需先推送 GitHub

本地另提供 `runs/archives/sls-act1-plateau-source-20260928.zip` 源码包，
含当前源码、配置、测试和文档，以及逐文件哈希 manifest；不含游戏、
模型、训练目录或旧日志。包里的 Git commit 只是父提交，实际工作区
包含尚未提交的修改，文件 manifest 才对应这份源码快照。

可上传后解压到服务器一个新目录，避免与现有运行中的代码混用。
只需把原始 56M checkpoint 复制到上面的相对路径，保留服务器原文件。
使用既有 Python 3.12 模型环境，然后提交第一阶段命令。
本地工作者不直接登录或操作 NUS；作业提交仍由维护者完成。

## 本地验证记录

2026-09-28：RL、奖励、恢复契约、诊断选择/编排与 Slurm 相关检查
206 passed（37.65 秒）；Ruff 全仓通过，`git diff --check` 无错误。
编排检查使用模拟子进程，验证阶段顺序、保存报告、错误基线停止和拒绝
覆盖已有输出；它不代替服务器 A100 运行。真实 56M checkpoint 哈希、
56,000,512 步、模型尺寸、native source pin 和内容 scope 源码身份已核对。
没有本地长评估，也没有声称奖励方案提高了胜率。
