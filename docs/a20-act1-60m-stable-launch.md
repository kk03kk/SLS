# A20 Act1 46M → 60M stable run

This is a fresh weight-transfer experiment from the 46,006,272-step champion,
not an exact continuation of its optimizer or environment state. The source
checkpoint remains untouched. The current native source is rebuilt and qualified
inside the Slurm job. The earlier high-learning-rate 60M experiment and 54M
recovery are not parents of this run.

The operator runs the following block on an NUS login node. It refuses a dirty
server checkout or a source-checkpoint hash mismatch before submitting one
physical-A100 `gpu-long` job. No Torch workload or native build runs on the login
node.

```bash
bash <<'BASH'
set -euo pipefail
cd /home/h/hengzhi/SLS
if [ -n "$(git status --porcelain)" ]; then
  echo 'Server checkout is dirty; no job submitted.' >&2
  exit 1
fi
git fetch origin
if git show-ref --verify --quiet refs/heads/codex/a20-act1-60m-stable; then
  git switch codex/a20-act1-60m-stable
  git merge --ff-only origin/codex/a20-act1-60m-stable
else
  git switch --track origin/codex/a20-act1-60m-stable
fi
if [ -n "$(git status --porcelain)" ]; then
  echo 'Training branch is dirty; no job submitted.' >&2
  exit 1
fi
printf '%s  %s\n' \
  '2db39fde737445b109d31cc522dde42dfe2c7372b34007e108ce702b03acc70d' \
  'local/runs/ironclad-a20-act1-v1-50m/stages/train/selection/best_progress.pt' \
  | sha256sum --check
export CUBLAS_WORKSPACE_CONFIG=:4096:8
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a20_act1_60m_stable.toml \
  --constraint xgpg --prepare
BASH
```

The submission helper requests one `a100-40` GPU, 16 CPUs, 64 GiB, and up to
three days, and sends `TERM` five minutes before walltime. The preparation
wrapper rebuilds a stale native module, runs the required preflight and one
64-worker/16-shard benchmark, then starts training. The same submission command
can be rerun if walltime stops the job before the target; exact resume uses the
new run's own checkpoint and unchanged source/configuration.

New artifacts are written under
`local/runs/ironclad-a20-act1-v4-60m-stable/`. The selection checkpoint may
remain the 46M source if no later candidate wins on the fixed 512 seeds. The
independent 2,048-seed result is `final-evaluation.json`; the terminal training
state is `final.pt`. The target is 60M cumulative steps, rounded to 60,014,592
at complete 64 × 256 PPO updates. A completed run does not by itself establish
an improvement over the historical champion.
