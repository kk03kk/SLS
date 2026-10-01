# 单作业 Win 58M → 70M

2026-10-02 状态更新：job 891389 已 COMPLETE 到 70,008,832，固定终点通过预注册开发
判据，见[结案](results/win70m-20261001/README.md)。下一步只推荐[90M 作业](win-90m-launch.md)；
以下保留提交前的历史设计，勿再次提交 70M。

截至 2026-09-30 已准备、未提交服务器。最终选择是扩大 **Win 本身的训练预算**，不再进行
Progress vs Win A/B，也不单独排一个“56M vs 56.5M”大评估作业。
依据与结果见[筛查结案](results/plateau-reward-screen-20260930/README.md)和
[独立审计复核](audits/2026-09-30-independent-recheck.md)。

## 实验与契约

- hypothesis：正确 Win objective 下，进一步训练可能提高正常 A20 Act1 的完整通关率。
  70M 不是承诺；只允许这一次有限预算扩展。
- control：冻结历史 56M champion `9555c860…`，与新模型使用同一确认种子和 inference runtime。
  单分支预算验证不是 reward 因果 A/B，也不能泛化成训练 seed 分布的效果。
- 从完整 Win 58,015,744 endpoint 的 `latest.pt` (`a229b73c…`) 继续，不从 56.5M 事后峰值重启。
  保留 model、Adam moments/step=3936、RNG、worker episodes、memory、update=123；saved
  next_seed=120012081。训练 seed 仍 120000000；不另开第二 seed。
- 目标 70,000,000；64×256 整数 batch，使实际终点至多超出 16,383 决策。新增约 11.99M。
  LR=3.125e−5、λ=.98、γ=1、domain normalization、critic/model、Win reward、native 都不改。
- 当前实现迁移明确登记 old `cfc4a31b…` → config 的 exact new implementation digest。
  PPO 的 KL diagnostic estimator 和 best selection 行为有变化，故不称为旧提交 bitwise exact
  resume。checkpoint 结构仍 v5，encoding/vocabulary/native 不变；BEST metadata 是 v5，
  新 selection 从 58M baseline 建立。旧运行和旧 champion 不动。
- 编译/production-layout preflight/必要 benchmark 都在 compute node，必须通过后才训练。
  原 benchmark 的 worker layout 可以复用，源/恢复验证不可跳过。

## 评估预算与判据

- periodic selection：`[8e12,+512)`，每 2M；checkpoint 每 1M。512 只用于廉价开发选模，
  不用于宣布小幅改善，周期峰值不算最终成功。
- 一次 development confirmation：`[8000002000000,+2048)`。结束后在同一进程、相同配置上
  评价 frozen reference、70M endpoint、周期 selected model，分别保存
  `reference-evaluation.json`、`endpoint-evaluation.json`、`final-evaluation.json`，均标为
  `development-confirmation`。不回填历史 v2 的未知 runtime。
- primary：固定预算 endpoint 比 frozen reference 的完整 Act1 clear rate；secondary：selected
  模型。Boss 分解是探索指标，不能按其再选模型。mean_reward 不比较。
- 成功：primary 配对提升至少 +2pp、精确双侧 McNemar p<.05、描述性配对 95% 区间下界>0、
  无健康失败，secondary 不能给出明确相反证据。单训练 seed 仍限制配方泛化。
- 若 endpoint/selected 的配对区间上界都≤0，说明本次预算策略没有改善；出现 runtime/contract
  failure 则先修复。其余情况是无定论，不是等效。**不自动继续加到 90M。**
- 无改善时冻结当前分支，利用本次域 advantage、critic 与 KL 日志决定一次 λ 或 normalization
  消融，控制其余变量。没有证据时不宣称 λ=.98 已坏，不因低 KL 自动增加 lr/epochs。
- 最终保留：`[9e12,+4096)` 此作业不用。开发结论后先固定 candidate/hash、baseline、runtime、
  batch/decoding、分析规则，再只评一次最终保留集，不从终评重新选 checkpoint。
  以上 simulator Act1 数字都不等于原版 A20 完整游戏或 Act3/Heart 胜率。

预计 38–48 GPUh，申请 60h；一张 A100 40GB，16 CPU，64GB 内存，`xgpg` constraint。
时间来自本轮 97–99 decisions/s 与新增预算，并含 preparation/评估余量，不是已运行测量。
Slurm 提前终止时检查 manifest 再恢复；安全退出 exit=0 不等于训练已 COMPLETE。

## 服务器唯一推荐动作

代码已本地验证后推送到 GitHub；使用本阶段最终总结给出的明确 branch/commit 拉取。
服务器原始 `local/runs/...win-2m-r1/` 和原 preparation benchmark 必须保留，Git 不携带模型。
提交工具会核对 config digest、source native、旧/新 implementation、两个 pinned checkpoint，
拒绝已有目标目录、脏源码或重复 submission receipt；失败时先检查 receipt/队列，不盲目重试。

```bash
python tools/submit_win70m.py --dry-run
python tools/submit_win70m.py
```

`--dry-run` 不提交；真正提交仍由人类执行。无需单独提交 benchmark 或评估作业。
作业结束后下载整个输出目录、preparation 与 Slurm stdout/stderr，保持 bundle 和 checkpoint
原样；不要只下载 best，也不要覆盖旧 archive。

机器可读预注册：[win-70m-20260930.json](../configs/experiments/win-70m-20260930.json)；
配置：[ironclad_a20_act1_win_70m_continuation.toml](../configs/train/ironclad_a20_act1_win_70m_continuation.toml)。

## 本地验证

Python 3.12 Conda DL；全量 pytest、ruff、词表生成一致性、19 个配置结构/种子命名空间校验；
945 passed、1 skipped（可选旧模型未安装），4 个预期 runtime-rebind warning；Ruff 和 whitespace check 通过。
归档 18 个 bundle 登记文件、22 个 checkpoint 及 2 个导出模型权重；真实 endpoint 全恢复
状态保留；生产尺寸 model 的 2,048-decision 微型 PPO 更新及逐位重放；5×64 开发种子
运行检查；Neow grouped-CV 与审计统计重算。最终计数与 commit 见本阶段总结。
本机没有执行 NUS 训练、完整 A100 production-layout 恢复更新、Linux ASan 或新 stock JAR
对照，服务器 compute preflight 负责前两者中对应的运行验证。
