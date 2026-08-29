# NUS 战士 A0 FullRun 训练手册

本流程从随机权重训练一个 Ironclad A0 普通三幕 FullRun，不使用旧模型或专家轨迹。
Smoke、pilot 和 train 是同一学习链上的 Act 1、Act 2、FullRun 三段自生成课程。
模型、优化器、计数和 RNG 连续继承；课程切换时环境、episode limit、belief memory
及上一动作/奖励一起重置。不得删除输出目录后继续下一阶段。

v2 的真实训练终局分数为：通关固定 `+1`；失败为
`-1 + 0.8 * clamp(死亡楼层, 0, 50) / 50`。因此更远的失败明确更好，但任何失败仍为
负数且严格低于通关。同一楼层内增加 decision 或拖到 501 回合不会加分。沿途另保留
scale 0.2 的 potential shaping 做局部 credit assignment；`gamma=1.0`，不会再通过
推迟负终局奖励获利。step/cycle/backend limit 一律按 `-1` 处理，不给楼层进度分。

## 1. 登录与同步

~~~bash
ssh -J hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg
cd ~/SLS
git switch main
git pull --ff-only origin main
git status --short

TRAIN_PY=/home/h/hengzhi/venvs/sls/bin/python
test -x "$TRAIN_PY"
"$TRAIN_PY" -m pip install -e . --no-deps
~~~

git status --short 必须为空。不要在内存受限的 login node 上导入 Torch、构建
native simulator 或执行训练；这些工作全部提交到 compute node。

## 2. Preflight 与 worker benchmark

~~~bash
"$TRAIN_PY" tools/submit_slurm.py preflight --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-preflight-*.out

"$TRAIN_PY" tools/submit_slurm.py benchmark --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-benchmark-*.out
"$TRAIN_PY" -c 'import json; p=json.load(open("runs/worker-benchmark.json")); print(p["selected_workers"], p["selected_shards"])'
~~~

Preflight 必须报告 A100、CUDA、native import、Decision invariant、seed 8335、
GRU forward/backward 和 checkpoint exact-resume 全部通过。Benchmark 会在
16:8、24:8、32:8、32:16、48:16 中选择达到峰值 95% 的最小 worker/shard
组合，并绑定当前 Git commit、native source digest 和实际二进制 SHA-256。后续任务
只接受同一个原生二进制。

## 3. Smoke：Act 1 课程，累计 500 万环境步

~~~bash
"$TRAIN_PY" tools/submit_slurm.py smoke --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-smoke-*.out

"$TRAIN_PY" -c 'import json; p=json.load(open("runs/ironclad-a0-fullrun-v2/run-manifest.json")); print(p["status"], p["environment_steps"], p["cuda_peak_memory_bytes"])'
tail -n 5 runs/ironclad-a0-fullrun-v2/stages/smoke/metrics.jsonl
find runs/ironclad-a0-fullrun-v2/crashes -type f 2>/dev/null
~~~

状态必须为 COMPLETE，不得出现 crash、NaN、非法 Decision 或 OOM。只有 128-seed
评估达到 20% Act 1 成功率且无 backend/limit/self-loop 错误，smoke artifact 才会生成。用
compute node 对保存点重复计算两次下一 update：

~~~bash
srun --account=allusers --qos=normal --partition=gpu --gres=gpu:a100-40:1 --cpus-per-task=16 --mem=64G --time=03:00:00 "$TRAIN_PY" tools/verify_training_resume.py runs/ironclad-a0-fullrun-v2/latest.pt --device cuda
~~~

输出中的 metrics、model 和 trainer state 三项必须全部为 true。

## 4. 可选：下载通过晋级门的 smoke artifact 做模拟器诊断

在本地 PowerShell 执行：

~~~powershell
scp -o ProxyJump=hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg:~/SLS/runs/ironclad-a0-fullrun-v2/stages/smoke/ironclad-a0-fullrun-v2-smoke.pt D:/SLS/runs/ironclad-a0-fullrun-v2-smoke.pt
~~~

该 artifact 的 goal 是 ACT1，不能交给 FullRun 实机控制器。它只用于独立模拟器
回放、轨迹捕获和阶段诊断。实机接管必须等待最终 FullRun 晋级门通过。

## 5. Pilot：Act 2 课程，累计 2,500 万环境步

~~~bash
"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-pilot-*.out
~~~

Pilot 默认申请 `gpu-long` 和 12 小时；它的 200 万步目标在 3 小时 `gpu` 配额内
没有可靠余量。

只有 smoke 晋级门通过后才允许启动 Pilot。Pilot 自动迁移 smoke 的 latest.pt，保留学习
状态并用新 seed 重置环境，且先记录 Act 2 horizon 的阶段基线。完成后要求 Act 2
成功率至少 5%、Act 2 到达率至少 50%，且无 backend/limit/self-loop 错误。

## 6. 正式训练：FullRun 课程，累计 1 亿环境步

~~~bash
"$TRAIN_PY" tools/submit_slurm.py train --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-train-*.out
~~~

训练每跨过 50 万环境步保存编号 checkpoint，每 500 万步在固定 128 个保留种子
上评估。Slurm 会在 72 小时 walltime 到达前 5 分钟发送 SIGTERM，程序会在安全边界
保存并将 manifest 标为 INTERRUPTED。此时原样再次提交 train；不得重新提交 smoke
或删除输出目录。

需要人工安全停止时，必须把 TERM 发给 batch step（`--wrap` 已用 `exec` 让 Python
成为该进程）：

~~~bash
scancel --batch --signal=TERM JOB_ID
~~~

不要用 `scancel --signal=TERM JOB_ID` 代替；Slurm 默认不会把该信号发给 batch step。
收到信号后程序最多完成当前一个 PPO update，原子保存 `latest.pt`，把 manifest 标为
`INTERRUPTED`，然后以 0 退出。普通 `scancel JOB_ID` 会撤销 allocation，并可能在安全
checkpoint 完成前进入强制终止阶段，只应用于不要求保留本批进度的停止。

达到 1 亿步后，train job 会用独立的 1,000 个最终种子评估 best checkpoint，
并生成：

- stages/train/selection/best_progress.pt：定期评估选出的最佳进度 checkpoint。
- latest.pt：训练链最后的精确恢复点。
- final.pt：达到 1 亿环境步时的最终 checkpoint。
- final-evaluation.json：1,000-seed 最终结果。
- ironclad-a0-fullrun-v2.pt：本地真实游戏使用的 simulator-only artifact。
- run-manifest.json、stages/*/metrics.jsonl 和 crashes/：完整运行证据。
- training-bundle.json：上述文件的 SHA-256 清单；实机 action journal 保留在本地 logs/。

只有 Pilot 晋级门通过后才允许启动正式训练，并先记录 FullRun horizon 的阶段基线。
只有周期门和独立最终门都通过时才生成根目录 FullRun artifact。最终门要求至少 1%
通关率、Act 2 到达率 75%、Act 3 到达率 25%，且 backend error、truncation、timeout、
step limit、cycle limit、self-loop 全部为零。未通过时只保留 checkpoint 与诊断，不生成实机文件。
