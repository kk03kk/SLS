# NUS 单 GPU 实验训练操作手册

当前下一步仅运行 `EXPERIMENTAL` teacher、BC 和 20-update smoke，不启动
1000-update production 长训。实验模式不会伪装成策略迁移已验证：其 checkpoint、
artifact 和 manifest 永久记录 `training_mode=EXPERIMENTAL` 与
`policy_transfer_verified=false`，不能用于 production export、resume 或真实游戏。

## 1. 拉取与刷新 editable 环境

```bash
ssh -J hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg
cd SLS
git switch main
git pull --ff-only origin main
git status --short

TRAIN_PY=/home/h/hengzhi/venvs/sls/bin/python
test -x "$TRAIN_PY"
"$TRAIN_PY" -m pip install -e . --no-deps
```

`git status --short` 必须为空。不要在约 1 GiB 内存限制的 `xlogin` 上导入 Torch、
编译 native 或训练；下面命令均提交或申请 compute 资源。

## 2. Experimental preflight 与吞吐 benchmark

```bash
"$TRAIN_PY" tools/submit_slurm.py preflight --mode experimental --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-preflight-*.out

"$TRAIN_PY" tools/submit_slurm.py benchmark --mode experimental --python "$TRAIN_PY"
tail -f runs/slurm-logs/sls-benchmark-*.out
N=$("$TRAIN_PY" -c 'import json; print(json.load(open("runs/worker-benchmark.json"))["selected_workers"])')
echo "$N"
```

Experimental preflight 仍强制检查 clean Git/source identity、Linux/Python/compiler、
native build/import、CUDA、Policy v3/词表、Decision invariant、seed 8335 crash replay、
单 worker 启动和 checkpoint exact-resume；唯一省略的是 production transfer gate。

## 3. 1000-seed teacher、独立 validation 与 BC

以下 `srun` 使工作实际发生在 compute node。训练集使用 seeds 0–999，validation
使用 20000–20099，二者不重叠。

```bash
srun --account=allusers --qos=normal --partition=gpu --gres=gpu:a100-40:1 --cpus-per-task=16 --mem=64G --time=03:00:00 \
  "$TRAIN_PY" tools/generate_teacher_corpus.py --seed-start 0 --seed-count 1000 --workers 16 --output runs/teacher-act1.json.gz

srun --account=allusers --qos=normal --partition=gpu --gres=gpu:a100-40:1 --cpus-per-task=16 --mem=64G --time=03:00:00 \
  "$TRAIN_PY" tools/generate_teacher_corpus.py --seed-start 20000 --seed-count 100 --workers 16 --output runs/teacher-act1-validation.json.gz

"$TRAIN_PY" -c 'import gzip,json; p=json.load(gzip.open("runs/teacher-act1.json.gz","rt")); print({k:(len(p[k]) if k=="examples" else p[k]) for k in ("teacher_successes","rejected_labels","examples","corpus_sha256")})'

srun --account=allusers --qos=normal --partition=gpu --gres=gpu:a100-40:1 --cpus-per-task=16 --mem=64G --time=03:00:00 \
  "$TRAIN_PY" tools/pretrain_behavior.py runs/teacher-act1.json.gz --validation-corpus runs/teacher-act1-validation.json.gz --output runs/act1-bc.pt --artifact-output runs/act1-bc-artifact.pt --device cuda
```

BC 会验证每个 label 在 checkpoint 中恰好对应一个合法 candidate，并要求 held-out
accuracy 比随机初始化至少提高 5 个百分点。corpus 与 BC 都记录 Git、native、词表、
生成/训练配置 digest、成功数、拒绝 label 数及 corpus digest。

## 4. 20-update experimental smoke

```bash
"$TRAIN_PY" tools/submit_slurm.py smoke --python "$TRAIN_PY" --workers "$N" --warm-start runs/act1-bc.pt
tail -f runs/slurm-logs/sls-smoke-*.out
```

完成后检查：

```bash
tail -n 5 runs/act1-smoke-v3/metrics.jsonl
"$TRAIN_PY" -c 'import json; p=json.load(open("runs/act1-smoke-v3/run-manifest.json")); print({k:p.get(k) for k in ("status","training_mode","policy_transfer_verified","best_checkpoint")})'
```

重点查看 evaluation、`approx_kl_final`、`clip_fraction`、`epochs_completed`、
`kl_early_stop`、`terminations_step_limit` 和 `terminations_cycle_limit`。确认 smoke
无 crash/NaN/非法 Decision 且曲线合理后，再决定是否提交 200-update pilot：

```bash
"$TRAIN_PY" tools/submit_slurm.py pilot --python "$TRAIN_PY" --workers "$N" --warm-start runs/act1-bc.pt
```

## 5. Production 状态

Production preflight 命令是：

```bash
"$TRAIN_PY" tools/submit_slurm.py preflight --mode production --python "$TRAIN_PY"
```

它在全部 experimental 检查之上，严格验证当前 clean commit 绑定的完整
`policy-transfer-v1`：提交的 Original-derived immutable routes、随机分布、确定性
语义套件以及已接受的 Original policy canary。缺失任何一项都会 fail-fast。
仓库现在已修复 fresh clone 不含 truth routes 的问题；但在生成并接受当前 v3 的
20-seed Original canary 之前，production preflight 与 `act1_train.toml` 仍应保持阻塞。
