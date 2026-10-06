# Act1→Act2 4M pilot 结案与下一阶段决策

2026-10-06。主指标始终是正常 Neow 开局的 A20 Ironclad **Act1+Act2 联合通关概率**。本轮没有达到预注册的延长训练判据，不建议从 94M 终点原样继续训练，也不建议现在扩展 Act3。

## 身份、完整性和归档

- job **912171**，xgpg6 / A100 PCIe 40GB / Linux / Torch 2.6.0+cu124；64 workers、16 shards，干净 revision `d9ee32d67003f8ce7567a77df21916e96cbbdfef`。
- 90,013,696→**94,027,776** decisions，新增 **4,014,080**，245 updates；超预定目标 14,080，不足一个 16,384 rollout。累计步数包含此前 Act1 训练，不能说已有 94M 的 Act2 训练。
- 下载 `runs/archives/sls-act12-pilot-lite-20261006.tar.gz`，43,100,801 bytes，SHA256 `596a4a543107a4b44a6764427e2e0e0ae5869ddb4fe1c98a9037f0cc2ea224da`。完整读取 gzip/tar；路径、重复成员、链接和特殊文件检查通过。
- canonical run：`local/runs/ironclad-a20-act12-win-pilot-r1/`；preparation：`local/runs/preparation/ironclad-a20-act12-win-pilot-r1/`；原始身份和 Slurm 日志：`local/imports/act12-pilot-20261006/`。没有覆盖历史模型或运行目录。原根目录包在验证同字节归档副本后移出根目录。
- bundle 注册 11 项，**实际包含的 10 项全部 SHA 匹配**。唯一缺少的是服务器独立 policy 导出 `ironclad-a20-act12-win-pilot-r1.pt`。中间 checkpoint 文件也未下载；周期评估原始行仍在已核验 metrics 中。不能声称整个服务器目录完整下载或全部 checkpoint 已检查。
- final/latest/selected 三个实际 checkpoint 的模型、PPO、profile、词表、native、训练身份、步数及 Git revision 均核对。model / optimizer / trainer tensor 数值有限，64 个环境状态存在，next_seed 在训练空间；final/latest 模型逐 tensor 相等。没有声称跨平台逐位恢复成功。
- native `fe354a23…`、implementation `03ab4c6e…` 与当前源码匹配；父模型 SHA `274963f4…` 与已归档 90M final 一致。本轮换 horizon 时是 all-weights transfer，重新初始化 Adam/RNG/workers/memory；不是旧 Act1 训练的 exact resume。
- 本地另外导出两个 ACT2 推理候选到 `model/`，命名包含 pilot 和 endpoint/periodic-selected，不补写原 run 缺少的服务器 export，不晋升 champion。

本目录 `*.json.gz` / `metrics.jsonl.gz` 是原服务器 JSON / JSONL 的**无损压缩副本**，解压后的 SHA 对应原 bundle；未重排或修改字段。`analysis.json` 是本地复算，`credit-probe.json` 是本地采样诊断，两者身份不同。详见 `archive-inventory.json`、`evidence-placement.json`、`checkpoint-audit.json`。

## 配对结果

同一批 `[8000007000000,8000007001024)` 开发确认种子，三个模型在同一 runtime、simulator 和正常开局条件下评估；按**全部 1024 个开局**配对，没有按 Act2 幸存者筛样本。此次三个模型的通关 seed 集两两不重叠。

| 模型 | 两幕联合通关 | Act2 到达 | 到达 Act2 后通关 | Act2 Boss 入场 |
|---|---:|---:|---:|---:|
| 冻结 90M 父模型 | **6/1024 = 0.586%** | **812/1024 = 79.30%** | 6/812 = 0.739% | 79 |
| 固定 94,027,776 终点 | **3/1024 = 0.293%** | **508/1024 = 49.61%** | 3/508 = 0.591% | 49 |
| 周期所选 91,504,640 | **2/1024 = 0.195%** | **572/1024 = 55.86%** | 2/572 = 0.350% | 55 |

终点 vs 父模型联合通关 lost6/gained3，−0.293pp，精确双侧 McNemar **p=.5078125**；描述性配对正态 95% 区间 [−0.867,+0.281]pp。只有 9 个 discordant，区间近似较弱，主要参考精确检验和原始计数。三个模型 Wilson 区间分别 [0.269,1.272]%、[0.100,0.858]%、[0.054,0.709]%。**没有证明联合能力提高，也没有充分统计证据断言联合胜率确切下降。**

Act2 到达配对 lost349/gained45，净 −304，**−29.69pp，p=2.18e−59**；这不是普通抽样波动。周期模型 vs 父模型联合 lost6/gained2，p=.289；周期模型 vs 终点 lost3/gained2，p=1。没有任何已验证的两幕“最佳模型”晋升依据。只保留“91.5M 是小开发集选择产物”的身份；下一实验继续使用预先固定的 90M parent。

到达条件比例只是描述，不能证明 Act2 战斗策略本身因果变差。三种策略制造的入幕状态和幸存者不同。即便只看两种策略都到达的 463 个 seed，牌组、路线、药水、relic 和记忆仍然不同。

## 曲线与瓶颈位置

固定周期开发集 256 seeds，success/reach：

| 累计 M | 联合成功数 /256 | 到达 Act2 数 /256 |
|---:|---:|---:|
|90.014|0|202|
|90.505|1|194|
|91.013|1|185|
|91.505|2|144|
|92.013|0|125|
|92.504|0|111|
|93.012|0|97|
|93.503|0|100|
|94.011|1|128|

256 集的 0–2 局波动无法可靠选择低于 1% 的策略。周期选中 91.5M 的原因是 **HORIZON_CLEAR_COUNT** 最高，平局保留最早；实际终点 94.027M 不等于最后一次周期评估 94.011M。固定终点独立确认避免以小样本峰值代替主结果。

训练完整结束：50 次真正 ACT_2_CLEARED /19,510 次终止（0.256%）；四个约 1M 窗口成功17/14/8/11次，后两个窗口确有成功信号，符合“并非完全无法探索到通关”，但没有持续改善的证据。每 update 平均成功约 .20 次；这是稀疏正例，不是网络完全不会输出成功动作。

Act1 Boss 分组通关率（分母按初始 Boss 分配，父→终点）：Hexaghost **76.49→44.05%**；Slime **85.42→66.07%**；Guardian **76.14→39.20%**。Boss 入场数 952→780，Act1 Boss 死亡140→272；Boss 前死亡72→244。因此前段路线/构筑/资源和 Boss 执行都值得检查，不能把问题缩成某个 Boss 的战斗遗忘。

父模型到达 Act2 后失败806局，其中733局在 Act2 Boss 前；终点到达后失败505局，其中459局在 Boss 前。两者均约 **91% 的 Act2 失败在 Boss 前**。只做 Act2 Boss 专项训练不能覆盖当前主要失败面。

终点幸存者的 Act2 入幕平均 HP比例 .866（父 .876），牌组20.60张（父19.53），药水 .563（父 .633）。这些数据不能证明牌组质量更好，也没有显示资源质量普遍改善。正常 Ascension 恢复会影响入幕 HP，不能以它代替 Act1 Boss 末期战斗质量。

## Reach 下降是否自动意味着 catastrophic forgetting？

**不自动意味着。** Reach 是联合任务的中间量；真正有利的构筑/路线风险取舍可以降低 Reach，同时提高联合通关。可以写成 P(joint)=P(reach Act2)×P(clear Act2|reach)，但条件人群随策略变化，不能用这个等式单独做因果归因。

本轮确认的是“迁移训练中的严重前段能力退化”，且没有后段收益证据。遗忘/参数干扰是合理假设，但还不能据此证明它是唯一或主要机制，也不能证明是 combat 技能本身丢失：

1. 父与候选在同一新的 Act2 规则、runtime、seed 上评估，排除了直接拿旧 Act1 80% 与新 Act2 统计比较的错误。
2. 任务目标从 Act1成功变成 Act2成功；需要重新学长期构筑和路径。旧目标表现下降并不等于新目标应该加 Act1 保持约束。尤其不能重新把 Act1 reach 排进选模目标。
3. λ=.98 与旧 critic 目标错配、稀疏正例、共享 actor/critic 表征更新、advantage 标准化以及探索不足都可能造成前段退化，并非互斥。
4. 若要把“构筑/路线改变”与“同一状态战斗能力下降”分开，需正常开局采集的开发状态，保留原生 RNG、前缀、GRU memory、previous action/reward；只存截图/牌组不够。需要匹配状态的分叉探针，或冻结 actor 的 critic 适应对照；当前包没有这类因果证据。
5. 文献对 interference 的测量强调与任务/数据分布变化区分；已有结果不能直接将本文针对 DQN 的测量照搬成当前 PPO 的已确认病因。[Liu 等原论文](https://proceedings.mlr.press/v232/liu23a.html)

## 源码及可复算机制审核

**已确认实现合理：** Win ±1、gamma=1 对应联合成功概率；没有给 Act1 clear 偷加终奖。potential terminal归零，其完整局和为 terminal reward−.2Φ(start)，正常开局时相同常数；它不直接奖励“败得更深”。GAE terminal mask 不跨 episode，开放 rollout边界才bootstrap；采样/evaluation 使用一致的 raw previous reward token。Neow 内容属性已编码，不能说只有选项序号。

**已确认存在风险，但效果需实验：**

- 保留 Act1 critic 的全部权重后，其价值函数目标已经不适合 Act2。共享 backbone+GRU 同时受 actor 和 critic 梯度作用，确实存在参数干扰路径；未量化其因果贡献。
- λ=.98 的终末 TD残差在提前100/200/300 decisions的系数为 .133/.0176/.00233；这不是策略只记得50步，也不是 GRU容量限制，而是优势估计更依赖中间 critic。[GAE 原论文](https://arxiv.org/abs/1506.02438)
- **本地采样只读探针**：16workers/4shards、256步，实际90M/94M权重、正常开局训练诊断 seeds从131000000起；Windows Torch2.10，不冒充Linux服务器复现。父模型10条在 rollout内从开局到真实死亡的轨迹，真实 shaped return约−1.036；λ=.98 的开局 return估计却在 **+.021至+.688**。例如209 decisions死亡：V=.414、GAE98 return=.136、GAE1 return=−1.036。同一信号改 λ=1，所有完整死亡轨迹的 return与直接求和误差<1e−5；权重逐tensor未变、Adam为空。它证明初始化期错误 bootstrap价值可以主导开局 credit，**不证明 λ=1 训练能提高胜率**，也不能用于估计一般胜率。94M critic 上这种巨大错配已缓和，退化却已经发生。
- 终点 greedy价值 anchors不能当作on-policy校准检验。Act2到达样本中的平均未裁剪隐含概率约−.002，286/508在[0,1]外；当前线性 value head允许越界。极低成功率下拟合“几乎都失败”可得到很小 MSE，却不能证明 critic辨识成功策略的能力。
- domain std归一化尺度从约10–11升到约29–32；三个domain尺度相近，没有证据显示只某一类被极端放大。小幅有偏优势仍可被标准化成明显更新，保留作为第二优先问题。不能单凭尺度就说梯度实际被放大32倍；原始优势同时变小。
- KL均值 .00430（阈值 .02），clip fraction .0364，无early-stop；gradient被裁剪的minibatch比例 .785。没有梯度数值爆炸证据，**单步KL小也不保证245次更新无累积退化**；采样KL允许负估计。gradient clipping频率不是已经证明学习率过大的证据。
- Neow训练选择位置1约84%，位置3零次，swap概率约1e−6量级；存在探索覆盖不足。内容条件已经编码，所以进一步工作应审计内容条件行为，不能泛称“没有Neow表征”。与联合退化的因果关系未确认。

**选模与运行健康：**12份评估（9周期+3确认）backend error/truncation/step/cycle/timeout均0；训练相应终止计数均0，metrics有限。`promotion_passed=true` 的 single-stage分支只证明完整健康评估可导出，**不是科研成功、不是联合能力提升**。原预注册 extension规则没有通过。日志开头 AssertionError对应尚未构建native的探测子进程；父进程随后完成build/preflight，是处理过的探测失败。NumPy缺包和cuBLAS首次上下文warning记录保留，未发现当前训练核心依赖NumPy转换；不能凭warning断言训练无效，也不将它写成环境完全无warning。

**吞吐：**更新99.66 decisions/s，更新累计11.19h，manifest覆盖13.51h（含评估等，非全部调度成本）。采样73.27%、优化26.61%；编码28.35%、transition构造23.72%、worker step13.95%、policy inference6.83%均占update总时长。这更支持先优化Python编码/对象转换，而不是直接扩大网络或只优化GPU。改变批处理/推理设置可能影响轨迹，要有same-runtime轨迹一致性证据。

**Simulator：**同规则比较和有限资格测试通过不是全Act2 parity认证。没有本轮证据证明全部退化由simulator引起。先针对自然失败高频战斗/能力做原版状态对照；不将未经定位的规则修改混进下一λ实验。[Potential shaping原论文](https://ai.stanford.edu/~ang/papers/shaping-icml99.pdf)的理论适用条件已在本实现按terminal归零/gamma核对。

## 下一阶段唯一首选

补充：[项目改进优先级](project-priorities.md)明确区分训练实验与整体工作。新GPU提交前先完成有限Act2保真核验、吞吐profiling和必要修复；不能把以下λ设计理解为忽略工程及模拟器问题直接提交。

**从原90M父模型重新作同预算 +4M normal-start Act1+Act2试验，只将 λ=.98→1.0。** 保留critic初始化、网络、Win±1、PBRS、gamma1、学习率、epochs、entropy、normalization、rollout256、workers64/shards16及native规则。核心假设是减少迁移期及长程credit对不适配critic的依赖，会改善正常开局联合学习；不是认为λ越大永远越好。λ=1在跨rollout的开放边界仍依赖critic，方差也可能升高。

这次历史 λ=.98 +4M 作为训练配方对照，无需再排队重跑同一个失败配方。重新在**同一个新GPU评估runtime**测冻结90M、本轮λ=.98固定94M、下一λ=1固定终点，使用未使用过的新开发确认集。它是单个training seed的历史受控比较，不是同时运行的随机重复试验；若有效，再用第二training seed确认。

详细约束见 `next-experiment-design.json`，目前为**设计文件，不是可直接提交配置**：约24h作业，+4M，training seed130000000（复用原对照初始化），新periodic `[8000009000000,+256)`、新确认 `[8000010000000,+2048)`；旧确认已暴露不再作为独立确认。9e12最终保留集继续封存。

成功要同时有真实late-window训练成功、健康预算，以及固定endpoint在新开发确认上联合率实用提升和配对统计支持。中间reach仅作为效率/诊断信号，不因恢复79%就判成功，不因reach下跌就自动否决联合提升；固定budget收尾，不依据小周期峰值连续改超参。失败/不确定时优先研究自然Act2入幕状态的suffix训练与critic适应，仍以normal-start联合评估验收；不再机械延长，也不立即混入Act3。

## 复算

DL环境：

```powershell
python tools/analyze_act12_download.py --run local/runs/ironclad-a20-act12-win-pilot-r1 --output local/reports/act12-pilot-recomputed.json
python docs/results/act12-pilot-20261006/recompute_outcomes.py
python -m pytest tests/test_act12_download.py tests/test_act12_analysis.py tests/rl/test_rollout.py tests/rl/test_reward.py tests/rl/test_advantage_domain_diagnostics.py tests/rl/test_ppo_math.py -q
```

输出使用exclusive creation，已有报告不会覆盖；完整包工具默认仍拒绝任何缺项。下一轮之前需生成新的SHA绑定训练计划、实际warm-start/preflight/预算验证、测试并提交GitHub。本次没有改training/native/observation/action/reward/model contract，没有开始新训练。

本地验证：**1079 passed /1 skipped /4 warnings**，67.08s；skip是历史policy按当前encoding被拒绝，4个warning来自已有runtime/provenance rebind测试。全仓库ruff通过，27/27训练配置通过；压缩原始结果独立复算一致、checkpoint额外审计通过。详见`validation.json`。本轮新增工具和缺项边界测试不改变训练implementation/native摘要。
