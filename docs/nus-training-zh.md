# NUS 战士 A0 FullRun 训练手册

本流程只训练一个从随机权重开始的 Ironclad A0 普通三幕 FullRun。Smoke、pilot
和 train 是同一 checkpoint 的三个累计步数边界，不得删除输出目录后继续下一阶段。

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
组合，并绑定当前 Git commit 与 native source digest。后续任务自动读取该文件。

## 3. Smoke：累计 10 万环境步

~~~bash
"$TRAIN_PY" tools/submit_slurm.py smoke --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-smoke-*.out

"$TRAIN_PY" -c 'import json; p=json.load(open("runs/ironclad-a0-fullrun-v1/run-manifest.json")); print(p["status"], p["environment_steps"], p["cuda_peak_memory_bytes"])'
tail -n 5 runs/ironclad-a0-fullrun-v1/metrics.jsonl
find runs/ironclad-a0-fullrun-v1/crashes -type f 2>/dev/null
~~~

状态必须为 COMPLETE，不得出现 crash、NaN、非法 Decision 或 OOM。Smoke 不要求
通关率或楼层提升。用 compute node 对保存点重复计算两次下一 update：

~~~bash
srun --account=allusers --qos=normal --partition=gpu --gres=gpu:a100-40:1 --cpus-per-task=16 --mem=64G --time=03:00:00 "$TRAIN_PY" tools/verify_training_resume.py runs/ironclad-a0-fullrun-v1/latest.pt --device cuda
~~~

输出中的 metrics、model 和 trainer state 三项必须全部为 true。

## 4. 下载 smoke artifact 做本地五步联调

在本地 PowerShell 执行：

~~~powershell
scp -o ProxyJump=hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg:~/SLS/runs/ironclad-a0-fullrun-v1/ironclad-a0-fullrun-v1-smoke.pt D:/SLS/runs/ironclad-a0-fullrun-v1-smoke.pt
~~~

启动带 CommunicationMod 的游戏，创建一局新的战士 A0，在 Neow 选择界面接管：

~~~powershell
python tools/play_live.py D:/SLS/runs/ironclad-a0-fullrun-v1-smoke.pt --device cpu --max-actions 5
~~~

这里只验证 artifact、公开状态编码、GRU memory journal 和真实操作链。随机阶段的
策略表现没有评估意义。检查 logs/live-agent.jsonl 中每个 INTENT 都有 ACK。

## 5. Pilot：累计 200 万环境步

如果 `latest.pt` 来自 terminal-outcome 修复之前的 104448-step smoke，先为新提交
重新运行 `preflight` 和 `benchmark`，然后只执行一次显式环境迁移：

~~~bash
"$TRAIN_PY" tools/submit_slurm.py preflight --python "$TRAIN_PY"
"$TRAIN_PY" tools/submit_slurm.py benchmark --python "$TRAIN_PY"
"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY" --resume environment-migration
~~~

迁移严格保留模型、优化器、累计步数、update、下一训练 seed 和 RNG；仅放弃旧
Simulator 中尚未结束的 worker episode，并清零对应 GRU memory 与 episode limit。
原始 checkpoint 保留为 `latest.pre-environment-migration.pt`，旧评估选出的 best
也改名保留。迁移成功后的 `latest.pt` 使用新合同；以后恢复仍使用默认 `auto`，
不得再次传 `environment-migration`。

~~~bash
"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-pilot-*.out
~~~

Pilot 自动从 smoke 的 latest.pt 精确续训。完成后查看 baseline 与最新 evaluation
的 Act 2/Act 3 到达率、死亡楼层和 limit/error 计数。性能变化是诊断项，不是阻止
首次 FullRun 实验的硬门槛。下载 ironclad-a0-fullrun-v1-pilot.pt，从全新 A0
Neow 界面开始运行一整局，并永久保留 action journal。

## 6. 正式训练：累计 5,000 万环境步

~~~bash
"$TRAIN_PY" tools/submit_slurm.py train --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-train-*.out
~~~

训练每跨过 50 万环境步保存编号 checkpoint，每 200 万步在固定 128 个保留种子
上评估。Slurm 在 72 小时前发送 SIGTERM，程序会在安全边界保存并将 manifest 标为
INTERRUPTED。此时原样再次提交 train；不得重新提交 smoke 或删除输出目录。

达到 5,000 万步后，train job 会用独立的 1,000 个最终种子评估 best checkpoint，
并生成：

- best_success.pt：定期评估选出的最佳训练 checkpoint。
- latest.pt：训练链最后的精确恢复点。
- final.pt：达到 5,000 万环境步时的最终 checkpoint。
- final-evaluation.json：1,000-seed 最终结果。
- ironclad-a0-fullrun-v1.pt：本地真实游戏使用的 simulator-only artifact。
- run-manifest.json、metrics.jsonl 和 crashes/：完整运行证据。
- training-bundle.json：上述文件的 SHA-256 清单；实机 action journal 保留在本地 logs/。

即使最终通关数为零，artifact 仍可下载用于诊断，但不得把它描述为已训练成功。
