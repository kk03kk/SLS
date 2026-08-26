# NUS 单 GPU 训练操作手册

服务器只运行已由本地 Original 证据锁定的 native simulator。不要把游戏、Mod、autosave 或大型 truth bundle 上传到集群，也不要在 `xlogin` 上直接训练。

## 1. 登录、拉取与环境

```bash
ssh -J hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg
git clone https://github.com/kk03kk/SLS.git
cd SLS
git switch main
git pull --ff-only origin main
git status --short

TRAIN_PY=/home/h/hengzhi/venvs/sls/bin/python
test -x "$TRAIN_PY"
"$TRAIN_PY" -m pip install -e . --no-deps
```

`git status --short` 必须为空。该固定 venv 已包含训练依赖；不要在约 1 GiB
虚拟内存限制的 `xlogin` 上导入 Torch、构建 native、运行测试或训练，也不要在这里
重新解析/安装模型依赖。`pip install -e . --no-deps` 只刷新同一 checkout 的 editable
入口。所有 Torch/native/GPU 工作均由下述 Slurm compute job 完成。若集群要求加载
编译器或 CUDA module，应按 NUS 当前说明在 batch 环境中提供；preflight 会对缺失项
直接失败。

## 2. Preflight 与 worker benchmark

```bash
"$TRAIN_PY" tools/submit_slurm.py preflight --python "$TRAIN_PY"
squeue -u "$USER"
tail -f runs/slurm-logs/sls-preflight-*.out

"$TRAIN_PY" tools/submit_slurm.py benchmark --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-benchmark-*.out
"$TRAIN_PY" -c 'import json; print(json.load(open("runs/worker-benchmark.json"))["selected_workers"])'
```

默认资源是 `allusers/normal`、一张 `a100-40`、16 CPU、64 GB。若 `sinfo` 显示的 GRES 名不同，使用 `--gpu NAME` 覆盖；短任务默认 `gpu`，最长三小时。benchmark 测试 8/16/24/32 workers，并选择达到峰值吞吐 95% 的最小数量。记下输出的 `selected_workers`，以下以 `N` 表示。

## 3. Smoke、pilot 与正式训练

NUS 的 preflight、benchmark、20-update smoke、pilot 和正式训练统一要求提交的
`TRAINING_READY` lock。`ENGINEERING_READY` 只保留给本地证据工程，不在任何默认
NUS 生产命令中使用。服务器不生成 lock，也不能用低等级 lock 或命令行参数绕过。

```bash
"$TRAIN_PY" tools/submit_slurm.py smoke --python "$TRAIN_PY" --workers N
tail -f runs/slurm-logs/sls-smoke-*.out

test -f configs/validation/act1_training_readiness.lock.json
"$TRAIN_PY" tools/verify_readiness_lock.py configs/validation/act1_training_readiness.lock.json
"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY" --workers N
tail -f runs/slurm-logs/sls-pilot-*.out
```

仓库现在已包含真实生成并验签的 `TRAINING_READY` lock；若文件缺失或验签失败，
必须安全停止，不应创建占位文件或绕过等级检查。先检查 smoke 的
`run-manifest.json` 为 `COMPLETE`，没有 NaN/Inf，checkpoint exact resume 正常。
pilot 每 10 updates 在固定 100 seeds 上评估。只有最近连续三次评估的成功率或
median failure floor 优于未训练基线，同时 entropy、KL、value loss、gradient norm
和 step/cycle limit 触发率正常，才提交长期训练：

```bash
"$TRAIN_PY" tools/submit_slurm.py train --python "$TRAIN_PY" --workers N
```

正式任务默认使用 `gpu-long`，时限三天。当前实现是 centralized inference 的单 GPU PPO，不要申请多 GPU，也不要用 `torchrun`。

## 4. 中断与恢复

Slurm 会在终止前 300 秒向 batch task 发送 SIGTERM；提交器使用 shell `exec`，确保
信号直接到达 Python。训练器完成当前 PPO update 后原子保存 `latest.pt`，将 manifest
标为 `INTERRUPTED`。同一配置和 worker 数量下恢复：

```bash
"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY" --workers N --resume
# 或正式训练
"$TRAIN_PY" tools/submit_slurm.py train --python "$TRAIN_PY" --workers N --resume
```

恢复会严格核对模型、PPO、课程、readiness lock、native source、Python/Torch/CUDA 和 worker 数。任何不一致都应新建训练，不要修改 checkpoint 绕过检查。metrics 中晚于 checkpoint update 的残留记录会被移除，避免重复曲线。

Act 1 晋级门槛仍是固定 held-out 100 seeds 成功率至少 80%，三个 Boss 各至少 60%，连续三次达到。训练表现不能替代 Original parity 证据。
