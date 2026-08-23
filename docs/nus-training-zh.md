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

conda create -n sls-train python=3.12 -y
conda activate sls-train
python -m pip install --upgrade pip
python -m pip install -e '.[model,test]'
TRAIN_PY="$(command -v python)"
```

`git status --short` 必须为空。批处理命令使用 `TRAIN_PY` 的绝对路径，不依赖 batch shell 是否自动激活 Conda。若集群要求先加载编译器或 CUDA module，应在提交前按 NUS 当前说明加载；preflight 会对缺失项直接失败。

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

```bash
"$TRAIN_PY" tools/submit_slurm.py smoke --python "$TRAIN_PY" --workers N
tail -f runs/slurm-logs/sls-smoke-*.out

"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY" --workers N
tail -f runs/slurm-logs/sls-pilot-*.out
```

先检查 smoke 的 `run-manifest.json` 为 `COMPLETE`，没有 NaN/Inf，checkpoint exact resume 正常。pilot 每 10 updates 在固定 100 seeds 上评估。只有最近连续三次评估的成功率或 median failure floor 优于未训练基线，同时 entropy、KL、value loss、gradient norm 和 step/cycle limit 触发率正常，才提交长期训练：

```bash
"$TRAIN_PY" tools/submit_slurm.py train --python "$TRAIN_PY" --workers N
```

正式任务默认使用 `gpu-long`，时限三天。当前实现是 centralized inference 的单 GPU PPO，不要申请多 GPU，也不要用 `torchrun`。

## 4. 中断与恢复

Slurm 会在终止前 300 秒发送 SIGTERM。训练器完成当前 PPO update 后原子保存 `latest.pt`，将 manifest 标为 `INTERRUPTED`。同一配置和 worker 数量下恢复：

```bash
"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY" --workers N --resume
# 或正式训练
"$TRAIN_PY" tools/submit_slurm.py train --python "$TRAIN_PY" --workers N --resume
```

恢复会严格核对模型、PPO、课程、readiness lock、native source、Python/Torch/CUDA 和 worker 数。任何不一致都应新建训练，不要修改 checkpoint 绕过检查。metrics 中晚于 checkpoint update 的残留记录会被移除，避免重复曲线。

Act 1 晋级门槛仍是固定 held-out 100 seeds 成功率至少 80%，三个 Boss 各至少 60%，连续三次达到。训练表现不能替代 Original parity 证据。
