# Win 70M 结案与 90M 决策

2026-10-02 独立复核服务器 2026-10-01 归档。job 891389，commit
`e0024449e2ef7c5adc3360a283542e5dec6b3a5c`，干净源码，A100 40GB / Torch 2.6.0+cu124，
64 workers / 16 shards；正常战士 A20 Act1 开局。不是原版完整 Act3/Heart 胜率。

**固定 70M endpoint 达成上一阶段预注册开发成功判据。** 在同一进程、相同 2,048 个新开发
确认 seed 上，比冻结 56M 增加 57 局，+2.78pp、精确 McNemar p=.01263，描述性配对 95%
区间 [+0.64,+4.93]pp；全部健康计数为零。周期最佳也改善，但不代替固定终点主指标。
这证明本次 Win 续训分支有收益，不隔离 reward 改变与更多训练的各自因果贡献，也不证明
多训练 seed 的配方泛化。训练 implementation 从 58M 到 70M 的迁移仍按此前说明解释。

## 完整性与身份

- 原包保存为 `runs/archives/sls-win70m-20261001.tar.gz`，SHA256
  `ddfa0afe496912bc3a51897f738cc7790903cf93ce2c3c77f2f0cec38582365d`。
- 已隔离解压并检查成员路径、链接与特殊文件；完整服务器 run 放到
  `local/runs/ironclad-a20-act1-win-70m-continuation/`，preparation 在对应规范目录。
  同名本地未训练恢复验证目录移到 `local/reports/win70m-20261001/previous-local-*`，未覆盖。
  SERVER-IDENTITY、Slurm stdout/stderr、原配置与计划保存在 `local/imports/win70m-20261001/`。
- bundle 的 11/11 登记文件哈希正确；12 个周期 checkpoint + final/latest/best 共 15 个
  checkpoint 的步数、PPO/model/native/profile/词表/训练身份、有限权重与恢复状态已检查。
  导出模型与 68M 所选模型逐 tensor 相同；70M 的 final 与 latest 模型权重也逐 tensor 相同。
- COMPLETE，58,015,744 → 70,008,832，新增 11,993,088 decisions，732 次新 PPO update，
  累计 update=855。每次 16,384 decisions，记录连续完整；整作业 35.59h。
- native source 仍 `1e30bb6c…`，training implementation `9da7e28c…`；native 二进制标记旧
  Git commit 4b4028c，但嵌入源码摘要与当前 source 一致，不把标记日期误判为环境变更。
- 三次确认评估均有相同实际 runtime / simulator identity，16 CPU threads、16 shards、
  high matmul precision。确认区间 `[8000002000000,+2048)`；9e12 未被本作业使用。

## 曲线、最佳模型与逐 Boss

| decisions | 周期开发胜局 /512 |
| ---: | ---: |
| 58,015,744 基线 | 383 |
| 60,014,592 | 381 |
| 62,013,440 | 379 |
| 64,012,288 | 380 |
| 66,011,136 | 374 |
| 68,009,984 | **405** |
| 70,008,832 终点 | 386 |

| checkpoint | 开发确认胜率 | 相对 56M 配对变化 |
| --- | ---: | --- |
| 冻结 56,000,512 | 1516/2048 = 74.02% | control |
| 固定 70,008,832 | 1573/2048 = 76.81% | lost224 / gained281，+57，p=.01263 |
| 周期所选 68,009,984 | 1581/2048 = 77.20% | lost200 / gained265，+65，p=.00296 |

68M 比 70M 净 +8/2048，p=.73632：没有证据证明回退更好。68M 在旧 512 选模集的峰值
与终点回落不等于 3.7pp 泛化退步。三个 checkpoint 不能与历史不同 seed 或 runtime 的
77.39% 等数字直接排序。68M 是开发候选，历史演示 champion 保留，最终保留集仍封存。

| 预定 Boss | 56M | 70M | 68M |
| --- | ---: | ---: | ---: |
| Hexaghost | 476/674 | 502/674 | 492/674 |
| Slime Boss | 541/676 | 545/676 | 568/676 |
| Guardian | 499/698 | 526/698 | 521/698 |

分母包含 Boss 前死亡。所有 Boss 点估计相对 56M 非负，但这些是探索子组；68M Slime
p=.01496 未校正多重查看，不能据此改 Boss 权重或训练分布。原始 entry/动作/失败楼层等
数据保留在确认 JSON，摘要保留各 checkpoint entry 与每 Boss 配对结果。

## 训练诊断与方案取舍

EV（GAE target）均值 .570，最后十次 .524；不是 Monte Carlo 胜率校准，不能据此宣布
critic 完好或损坏。final KL 均值 .00358、ratio clip 3.51%、norm clip 91.45%、early stop
0/732。高 norm clip 不等于优化崩溃；低 KL 也不构成加 LR 的理由。

combat/run/choice 的 raw advantage std 均值 .336/.286/.286，缩放均值 2.99/3.51/3.51，
最大值 3.98/4.79/4.49，未发现近零方差爆炸。三个域均值都接近零，normalization 仍可能
改变域权重，但日志不支持现在优先改变它。λ=.98 直接 residual 衰减问题仍是待验证假设，
不是已确认故障。Neow option1 占 68,308/72,881 次选择，没有新增可学条件策略证据。

仅训练更新吞吐约 102.77 decisions/s；编码 27.94%、Python transition 23.46%、policy
forward 6.99%、optimizer 27.03%（部分计时项并非独立互斥层级）。无法从这些数据宣称
GPU 利用率 7% 或改编码必得数倍加速。未因本次结果修复未量化的 simulator/observation
差异：它们仍限制原版真实性结论，不混入这次训练预算实验。

选择：保留有正面证据的 Win PPO，从完整 70M endpoint 到约 90M，继续 Adam/RNG/memory。
不回退 68M，不重置优化器，不改 LR/λ/normalization/critic/Neow/reward/start distribution。
这不是未经分析的复制配置：control 改为 70M，开发 seed 轮换、周期评估每 4M、固定 90M
终点为主指标，避免把相对旧 56M 的收益再次算成 70→90 的收益。

相比 λ 消融、域 normalization、critic 改架构、困难状态训练或模拟器变更，继续当前配方
有已完成主指标的支持，且不会混合多项未验证机制。吞吐优化值得独立开展，但本次不把
语义或采样顺序变化加入训练分支。若 90M 无可靠增益，不自动加到 110M。

## 可复算与本地验证

```powershell
conda activate DL
python tools/analyze_win_continuation.py --run local/runs/ironclad-a20-act1-win-70m-continuation --archive runs/archives/sls-win70m-20261001.tar.gz --evidence local/imports/win70m-20261001 --output docs/results/win70m-20261001/summary.json
```

[机器摘要](summary.json)、[完整续训状态验证](90m-state-verification.json)、
[90M 预注册](../../../configs/experiments/win-90m-20261001.json)、[服务器执行说明](../../win-90m-launch.md)。
没有在本机执行 90M 训练或新原版测试。当地 Windows/Torch 2.10 与服务器 Linux/Torch 2.6
的逐位轨迹不能混称同一结果；新工作需通过服务器 compute-node preflight 后才训练。
