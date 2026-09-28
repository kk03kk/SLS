# 从零训练 Warrior A0 Act1：5M 实验

使用 `configs/train/ironclad_a0_act1_5m.toml`。模型与优化器随机初始化，不能接入历史 13M/15M 权重。环境 profile 为 `IRONCLAD_A0_ACT1` version 4，Observation 为 v5；这两个版本号描述不同契约。

## 服务器操作

在 NUS 登录节点执行：

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a0_act1_5m.toml --prepare
```

返回 Slurm job ID 后查看状态、stdout/stderr：

```bash
squeue -u hengzhi
cat local/runs/slurm-logs/sls-train-JOBID.err
tail -n 30 local/runs/slurm-logs/sls-train-JOBID.out
```

将 `JOBID` 替换为实际编号。失败先读 traceback；缺少 NumPy 的 PyTorch warning 本身不等于失败。不要在 xlogin 上构建或运行 CUDA。

默认申请 gpu-long、A100 40GB、16 CPUs、64GB、三天。作业剩余五分钟收到 TERM 后在安全边界保存；中断后提交同一条命令即可恢复。不要同时提交同一运行目录：准备入口持有跨 exec 的文件锁，会拒绝重叠启动。完成 5M 的目录不能重新当作新实验覆盖。

准备入口检查 Python/PyTorch/native，必要时构建，然后按顺序执行实际配置 preflight、benchmark、选定布局的更新与恢复测试。缺少 PyTorch 时安装 `requirements/model.lock`；已有可用环境不因 GPU 名称变化升级依赖。任何必要步骤失败都不会进入正式训练。

准备证据和 benchmark 位于 `local/runs/preparation/ironclad-a0-act1-v4-5m/`。匹配的结果会复用；机器名/GPU 名称不要求相同。PPO、模型、profile、实现语义和运行库变化需要验证。恢复不会自动换布局或迁移环境；保存整个运行目录和相应 preparation 目录。

## 参数及选择理由

| 参数 | 初始值 |
| --- | --- |
| 网络 | embedding 128、Transformer 4 层/4 heads、FFN 256、循环隐藏层 256、dropout 0 |
| LR | 0.00025 |
| rollout / recurrent sequence / minibatch sequences | 256 / 64 / 16 |
| PPO epochs / clip / value clip | 2 / 0.2 / 0.2 |
| gamma / GAE lambda | 1.0 / 0.98 |
| value coefficient / target KL / gradient clip | 0.5 / 0.02 / 0.5 |
| entropy | 0.02，40M steps 线性衰减到 0.002 |
| episode limit / repeated boundary limit | 4096 / 4 |
| budget | 5M environment steps，向上取完整 update 边界 |

保留既有网络和 PPO 基线，没有证据支持为 Act1 重新设计网络或激进调参。5M 时 entropy 约为 0.01775，避免在早期通关尚不稳定时过快收缩探索。

胜利 +1；失败为 `-1 + 0.8 × clamp(floor, 0, 16)/16`，任何正常失败都小于胜利。保留 scale 0.2 的 potential shaping，终止时势函数为 0；Act1 钥匙权重为 0。超限与 backend truncation 按异常终止统计，不假装正常死亡。没有把高手选牌、精英路线、休息或能量使用规则写入奖励。

benchmark 是完整训练更新测速，不是模型性能评估。比较 `(workers, shards) = (32,4)、(64,8)、(128,8)`；各一次预热、三次完整采样/优化计时。距最快不超过 5% 时选较小布局。记录采样与优化耗时、总 steps/s、GPU 峰值内存、Linux 采样进程 RSS、CPU 线程数；RSS 汇总包含共享页，不当作精确独占内存。OOM 候选排除，全部失败则停止。仅使用独立临时模型，不读取正式训练 checkpoint 做测速。

不能用本地 RTX 的结果推断 A100 实际利用率。服务器 benchmark 执行后才有新配置的实际吞吐证据。

## 评估、checkpoint 与下一步决策

训练 seeds 从 10,000,000 开始，固定评估为 `[3000000000000, 3000000000512)`，最终独立评估为 `[2000000000000, 2000000001024)`。三者分离。确定性 argmax、同一输入契约、相同 episode limit；不更换固定 seeds 制造进步。

- 初始化和每 0.5M 评估固定 512 seeds；每 0.25M 保存完整 checkpoint。均在整次 PPO update 完成后执行，记录实际 steps。
- Best 只看固定 seeds 通关数，同分保持更早 checkpoint。中断的固定评估在续跑、下一次更新前补完。
- 5M 时保存 final；只对选定 best 运行 1024 个 held-out seeds。`final-evaluation.json` 记录被评估 checkpoint 的 hash 与实际训练步数，不能误认为它评估的是 final 权重。
- 输出 Wilson 95% 区间、死亡楼层直方图、按预告 Boss 分组的整局通关率和分母、进入 Boss 后的条件胜率与实际进入次数。
- 每个 seed 保存胜负、结束原因、楼层、路线、牌组和末次敌人/事件摘要；记录相邻固定评估的胜→负与负→胜。最多保留 16 类失败各一条最后 32 步轨迹，避免逐局翻大量日志。
- 正常策略失败、step/cycle limits 不作为“禁止导出模型”的高胜率门槛。Backend 错误仍需处理；所有胜率和异常计数原样记录。

产物目录为 `local/runs/ironclad-a0-act1-v4-5m/`，包括配置副本、manifest、latest、周期 checkpoint、best、final、最终独立评估和独立策略文件。将 `ironclad-a0-act1-v4-5m.pt` 复制到本地 `model/`，按 README 导入 inspector 或采集模拟器轨迹。

5M 是首轮预算，不保证达到近 100%。首先看固定/held-out 通关率是否一致、哪个 Boss/敌人或路线系统性失败、胜负转换是否显示退步，以及超限和训练 KL/entropy 是否异常，再决定继续训练或针对证据修改方案。
