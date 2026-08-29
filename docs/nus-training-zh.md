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
policy-transfer gate。全新 clone 的 preflight 会从已提交的随机样本证据与真值路线
自动生成绑定当前 clean main SHA 的 `runs/policy_transfer_v1.json`。它校验公开
Observation、合法 Action、编码/词表、禁用内容、确定性机制探针与随机分布；
整局隐藏 RNG 同轨迹不阻塞训练。20-seed Original canary 是模型部署放行项，不阻塞
生成 teacher、BC 或 PPO 训练。

先在 compute job 或等价 Python 3.12 + native 环境生成自动教师状态库；三个生产
配置默认从该文件以 50% 概率恢复中途公开状态：

```bash
"$TRAIN_PY" tools/generate_teacher_corpus.py --seed-count 1000 --output runs/teacher-act1.json.gz
```

```bash
"$TRAIN_PY" tools/submit_slurm.py smoke --python "$TRAIN_PY" --workers N
tail -f runs/slurm-logs/sls-smoke-*.out

test -f configs/validation/policy_transfer_v1.json
"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY" --workers N
tail -f runs/slurm-logs/sls-pilot-*.out
```

若 transfer gate 缺失、词表过期或不覆盖目标 profile，必须安全停止。先检查 smoke 的
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

恢复会严格核对模型、PPO、课程、transfer gate、native source、Python/Torch/CUDA 和 worker 数。任何不一致都应新建训练，不要修改 checkpoint 绕过检查。metrics 中晚于 checkpoint update 的残留记录会被移除，避免重复曲线。

课程加入下一阶段的信号是独立 held-out seeds 连续三次成功率至少 20%；旧阶段继续混合采样。产品最终胜率门槛需由完整 A0–A20 曲线决定。
