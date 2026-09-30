# 2026-09-30：Progress / Win 2M 筛查结案

结论：**本轮没有证明 Win Reward 更强，也没有证明它无效。** 后续目标确定为正常 A20 Act1
开局到全幕通关的概率，因此固定 Win Reward；不再追加 Progress / Win 长训练 A/B。
保留历史 56M champion，不依据单个周期峰值晋升。下一阶段是一个 Win 分支推进至 70M 的
有上限预算实验，见[执行说明](../../win-70m-launch.md)。没有在本机进行服务器规模训练。

## 身份、完整性与保存位置

- 原下载包 `runs/archives/sls-plateau-ab-20260930.tar.gz`，SHA256
  `134a5410d5598729cfa9460b335c0e2bfcc8a70d6ce13444e65b4da4cc6d60d3`。
  包保留原样；先在 `local/imports/plateau-ab-20260930/` 隔离解压，再移动两个训练目录。
- 原始运行分别为 `local/runs/ironclad-a20-act1-plateau-{progress,win}-2m-r1/`。
  两份 bundle 的 **18/18 登记文件** SHA256 正确；另核对每组 8 个周期 checkpoint、
  `latest.pt`、`final.pt`、`best_progress.pt`，共 **22 个训练 checkpoint** 的步数、
  model/PPO/profile/词表/native/training identity/commit、有限权重和恢复状态。
  两个服务器推理导出的全部权重与对应所选 checkpoint 逐 tensor 相同。
- 每组 123 次 PPO 更新；56,000,512 → 58,015,744，新增 2,015,232 决策。
  服务器 commit `4b4028cf5c831bb4c22a5e2edd6b7599e1779730`，工作区干净；native
  `1e30bb6cfa32f600cd63c59983c14ab8928fac4452c1586d630295d2c6ff9453`。
  两组 training implementation 都是 `cfc4a31b…`，不是审计后本地代码。
- 两组均从 `9555c860…` 的 56M 权重开始，保留 critic，重建 Adam/RNG/workers/memory，
  训练 seed 都是 120,000,000、64 workers / 16 shards。除 reward schema / failure progress
  scale 以及输出/准备路径外，配置相同。基线逐种子记录完全相同，不能把训练 seed 相同误称为
  之后每一步经历相同；策略分叉会改变轨迹与 reset/seed 消耗。
- preparation 与 Slurm 日志原件在 `local/runs/plateau-ab-20260930-evidence/`；导出配置原件
  仍在 staging `configs/train/`。为本地 continuation 还复制了两份 benchmark 到原规范路径，
  原件不变。两个 Slurm err 无训练异常终止，包含缺少 NumPy 和 CUDA context 的 warning。
- 每次 512 条 evaluation 均无重复、无缺失，覆盖其完整登记区间。periodic
  `[8000000000000,+512)` 与 development confirmation `[8000001000000,+512)` 不相交，
  均在训练硬上限 2e12 之外。包里的 `final-evaluation.json` 是 **所选模型的开发确认**，
  不是最后 checkpoint，也不是 `9e12` 最终保留集。其 v2 文件没有独立 runtime 块；run manifest
  有训练 runtime，但不能据此补造缺失的评估线程/批处理身份。

机器可读复算：[summary.json](summary.json)。命令：

```powershell
conda activate DL
python tools/analyze_reward_screen.py --progress local/runs/ironclad-a20-act1-plateau-progress-2m-r1 --win local/runs/ironclad-a20-act1-plateau-win-2m-r1 --archive runs/archives/sls-plateau-ab-20260930.tar.gz --output docs/results/plateau-reward-screen-20260930/summary.json
```

## 全曲线与配对比较

以下均为同一组 512 开发选模种子的完整 Act1 clear count。p 是两个 reward 臂在同一步数
上的精确双侧 McNemar；这些时间点被重复查看，p 不能作为预注册独立终评。

| 累计决策 | Progress | Win | Win − Progress | 配对 p |
| --- | ---: | ---: | ---: | ---: |
| 56,000,512 基线 | 374 (73.05%) | 374 (73.05%) | 0 | 1.0000 |
| 56,508,416 | 383 (74.80%) | **402 (78.52%)** | +19 / +3.71pp | 0.0842 |
| 57,016,320 | 379 (74.02%) | 381 (74.41%) | +2 / +0.39pp | 0.9241 |
| 57,507,840 | **386 (75.39%)** | 388 (75.78%) | +2 / +0.39pp | 0.9291 |
| 58,015,744 最后模型 | 373 (72.85%) | 383 (74.80%) | +10 / +1.95pp | 0.4191 |

Win 最早峰值随后回落 19 局；Progress 峰值到最后回落 13 局。最后模型不等于最佳模型。
不能把四个周期测量相加当作 2,048 独立种子，不能挑 Win 最好时刻与 Progress 最后时刻比较。
Win 在选模集早期提升和后续回落是真实记录，但不能区分优化漂移、greedy 决策边界敏感与
固定开发集的选择偏差。没有第二个训练 seed，尚不能宣布配方已到平台期。

| 各组所选模型 | 步数 | checkpoint SHA256 | 开发确认 |
| --- | ---: | --- | ---: |
| Progress | 57,507,840 | `df701b00879367178873bdb7781120359527c11240c20543df948102de74f508` | 392/512 = 76.56% |
| Win | 56,508,416 | `e416801c849ba3e339074c05d52a47512fbede65dfee108ef27305edb6477a6b` | 394/512 = 76.95% |

所选模型配对：Progress 独赢 51，Win 独赢 53；净 +2，p=0.9219，Win − Progress 的
描述性正态配对 95% 区间约 **−3.51 至 +4.29pp**。这不是等效性证明，也不是 2% 改善的否证。
确认集包含一个固定所选模型/臂，避免了最终 checkpoint 混淆，但此处已看过，今后属于开发数据。

## 逐 Boss 与其他指标

分母为种子的预定 Boss，包含到 Boss 前死亡；括号为真实进入 Boss 后的 win/entry。

| 开发确认 Boss | Progress | Win | 配对净差与 p |
| --- | ---: | ---: | ---: |
| Hexaghost | 135/168 = 80.36% (135/160) | 125/168 = 74.40% (125/157) | −10，p=0.1214 |
| Slime Boss | 141/183 = 77.05% (141/166) | 152/183 = 83.06% (152/163) | +11，p=0.0522 |
| Guardian | 116/161 = 72.05% (116/149) | 117/161 = 72.67% (117/150) | +1，p=1.0000 |

两组提升/退步在 Boss 间互相抵消；不能说 Win 普遍改善 Boss 战。56.5M Slime 的周期配对
p=0.0125 也是事后多子组/多时刻探索，不能晋升为确认发现。所有评估健康计数为零。
两组失败的 median floor 都为 16；failure mean 在周期内变化，Win 早期峰值为 13.9，
说明“多救下深层失败后剩余死亡变浅”与胜率改善可以同时发生。

| 训练指标（123 次更新） | Progress | Win |
| --- | ---: | ---: |
| 实际整作业时长 | 6.61h | 6.73h |
| 仅更新决策/s（总决策/总更新时间） | 99.13 | 97.16 |
| final KL 均值 | 0.00387 | 0.00346 |
| ratio clip fraction 均值 | 3.76% | 3.59% |
| 梯度裁剪命中率均值 | 84.43% | 92.68% |
| GAE target 的 critic EV 均值 | 0.541 | 0.549 |
| critic EV，末 10 次均值 | 0.506 | 0.581 |
| KL early stop | 0/123 | 0/123 |
| Neow 抽样选项 1 | 11,726/12,203 | 11,566/12,080 |

Win 的 value loss 数值约为 Progress 的 2.2 倍，奖励尺度改变时不能直接据此判定 critic 更差。
EV 计算的是 GAE returns、不是完整局 Monte Carlo 胜率校准。GPU policy inference 只占更新
时间约 6.4%；optimizer 又占约 30%，因此不能说 GPU 总利用率只有 6.4%。编码约 27%、
Python transition 记账约 22%；只消除编码的 Amdahl 理想上限约 1.37×，不是已有 3× 吞吐证据。

本地使用当前 DL / Torch 2.10 / RTX 5070 Laptop，2 CPU threads、4 shards，对 baseline、
两组选中及两组末尾模型分别做了同一批 64 开发种子的运行检查，健康计数均为零，完整 runtime
与逐种子结果在 `local/reports/plateau-ab-20260930/*-runtime.json`。这是已使用开发数据的
兼容性检查，不用其胜率晋升或声称复现服务器逐局结果。

## 下一步与证据边界

采用 Win 是因 `gamma=1`、终局 ±1 与最终 clear probability 一致，PBRS 的终局势能归零使
整局 shaping 为起点常数；不是因本轮 A/B 已证实 Win 获胜。未来完整 Act2/3/Heart 必须分别
检查终局判定和 horizon 契约。模拟器胜率依然不能代替原版游戏完整局胜率。

从 Win **完整 58,015,744 endpoint/latest** 继续到 70M，保留学习状态，训练 seed 流继续
120,012,081 起，实际新增约 11.99M 决策（有 rollout 整数超出）。不从事后 56.5M 峰值重启。
只安排一个作业，预计 38–48 GPUh，60h 上限。周期评估每 2M、512 个开发种子；结束后一次
共同 2,048 开发确认种子评价冻结 56M reference、70M endpoint 和周期所选模型。
`9e12` 的 4,096 最终种子不自动运行。

70M 是值得验证的预算假设，不是保证有效。固定终点是主要 endpoint，所选模型是次要 endpoint。
若两者都未取得可靠改善，停止只加步数；依据新日志的逐域 advantage、critic 与 KL 证据选择
一项 Win-only λ 或归一化消融。当前没有足够证据把它们直接当作已定位缺陷。

真实下载 endpoint 的 model/Adam/trainer/RNG/environments 全字段保留检查见
[continuation-verification.json](continuation-verification.json)。代码版本边界与审计修正见
[独立复核](../../audits/2026-09-30-independent-recheck.md)。
