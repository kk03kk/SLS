# A20 正常开局 Act1–2：4M 适应试训

2026-10-05：训练前准备已完成；本地验证和证据见 [准备结案](results/act12-preparation-20261005/README.md)。服务器训练尚未执行，不存在本轮训练胜率。

## 固定实验

固定 90,013,696 的 `final.pt` 为 parent，SHA256 `274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0`。70M→90M 在同一 2048 开发种子净增 66 局；周期所选 76M 与 90M 没有确认差异，因此选事前终点。保留所有历史模型与归档。

正常 Neow A20 战士开局，连续通关 Act1 和 Act2；新增 4,000,000 decisions，累计目标 94,013,696，允许不足一个 rollout 的超量。这不是单独第二幕开局，也不是 Act3/Heart 通关。

使用一个训练 seed 130000000、64 workers /16 shards。继承全部权重含 critic，重置 Adam、RNG、workers、memory、update、selection。保持网络、LR 3.125e-5、gamma=1、lambda=.98、两轮 PPO、域内 advantage normalization、entropy=.002、Win ±1 与 PBRS scale=.2。新 horizon 同时改变终止目标和进度势函数的分母，这是新阶段的必要契约变化，不是旧训练 exact resume。Snecko 修复沿用已审阅 weights-only 环境迁移；control 和 candidate 在同一修正环境评价。

| 项目 | 预先登记 |
| --- | --- |
| Hypothesis | 正常开局两幕数据上的有限预算适应，能提高联合通关率 |
| Control | 冻结 90M parent，在同一 Act1–2 环境、runtime、解码和 seeds 下正常开局 |
| 周期开发 | [8000006000000,8000006000256)，约每 0.5M |
| 主开发确认 | [8000007000000,8000007001024)，parent 对固定 4M endpoint |
| 次要确认 | 同一块上比较仅从实际周期评价中注册的 selected；确认集不用于重新选模 |
| 最终保留 | [9000000000000,9000000004096)，此次不使用 |
| 时间预算 | 单个 24h GPU 作业，包含 preflight、固定布局 benchmark、训练、开发评价 |

周期 checkpoint 保留约每0.5M，因此包含约2M中途状态。训练不自动延长。准备时按实测训练吞吐×1.5时间系数，加4h开发评估预留及实际准备时间估算；放不进24h则拒绝训练并记录报告。短 benchmark 仍不能保证后期吞吐和全评估成本，超时不视为完成预算后的方法失败。

## 判断与诊断

建议扩大预算的事前门槛：完整预算、健康计数零、数值有限；两个不重叠后期窗口（新增2–3M、3–4M及rollout超量）均有真实训练成功；固定endpoint在1024配对开发种子上提升至少2pp且近似配对95%区间下界>0。2pp是研究决策阈值，非理论定律。单训练seed与模拟器结果不能证明原版或跨训练seed稳健性。

点估计改善但不达门槛为证据不足；无开发改善需结合训练成功和失败状态诊断；未完成预算单列INCOMPLETE。不要仅因Act1胜率下降否决联合胜率改善。

评价原始逐seed记录新增 `sls-act-entry-value-diagnostic-v1`：每幕第一个可决策状态的HP、deck、relic、potions、gold、shaped value和势函数。对于新Act2模型，gamma=1和Win±1下记录未裁剪的 `(V + scale*Phi + 1)/2` 与完整greedy结果；critic预测的是训练随机策略，greedy结果只提供描述性对照，不能据此证明on-policy校准有问题。Act1 parent critic不能转换成Act2胜率。既有 `terminations_success` 按完成rollout所属窗口统计，边界误差最多一个rollout，两个窗口不重叠。

记录变化不修改observation、action、reward、encoding或模型形状；新的implementation摘要随评价实现变化。checkpoint schema保持v5，新增诊断自身标识v1，分析schema升为v3。旧训练身份保留，不添加旧checkpoint exact-resume白名单。

## 提交与恢复

正式 [TOML](../configs/train/ironclad_a20_act12_win_pilot.toml)、[bound plan](../configs/experiments/act12-win-pilot.json) 和 [recipe](../configs/experiments/act12-win-pilot-recipe.json) 绑定parent原始证据、模型SHA、预算、seed、训练implementation和提交工具源摘要。生成器拒绝覆盖；提交器复核recipe、config、parent、源码、clean Git，拒绝已有输出和重复receipt。

GitHub仓库重建后，NUS旧checkout属于另一历史。使用新目录克隆，`import_act12_parent.py`只复制已绑定parent及必要原始证据，不移动或删除旧目录。故意不用跨仓库symlink，避免绕过路径契约。该副本用于transfer，不代表复制完整原run；完整历史仍留在旧目录和本地归档。服务器继续使用已验证的 `/home/h/hengzhi/venvs/sls` 环境，GPU内从新源码构建native，login node只进行身份检查与sbatch提交。

同一个GPU作业由 `prepare_and_train.py` 完成准备后运行训练；被冻结parent、fixed endpoint和selected开发确认已集成训练流程。作业结束下载完整目录，再运行：

```bash
python tools/analyze_act12_pilot.py --run local/runs/ironclad-a20-act12-win-pilot-r1 --output local/reports/act12-r1-analysis.json
```

分析器先校验完成预算、bundle与健康身份，再对全部正常开局配对；第二幕Boss和入幕状态属于策略相关群体，不能只对幸存局配对。若作业未完成，先保留日志/manifest/latest并判断恢复，不能改配置绕过身份后重训。

## 未完成的资格与后续方向

已有Snecko及11个Boss/Spheric边界案例；此次新增六个有原版字节码依据的Book、Slavers、Leader、Chosen、Byrd受控案例，未发现这些检查中的新规则差异。跨幕/Boss奖励结构已有回归，但不能据此宣布全Act2原版交互/RNG和所有Boss relic全面parity。资格范围与原始证据见准备结案。

先运行这一个4M试训。若缺少成功，下一候选是正常局自然状态的Act2后缀课程；若成功存在且独立on-policy诊断确认critic异常，再做一个critic变量；lambda消融和吞吐优化随后根据数据决定。不新增reward A/B、不重置actor、不扩大网络，不立即承诺长训有效。
