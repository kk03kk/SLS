# AGENTS.md

## Local Environment

* OS: Windows
* Repository: `D:\SLS`
* Shell: PowerShell
* Python environment: Conda `DL`
* Activate with:

```powershell
conda activate DL
```

Use the local environment for development, testing, native simulator work, parity auditing, and other non-Slurm tasks.

## NUS Server Environment

Facts in this section were re-verified from `xlogin1` and a short `xgpg2`
allocation on 2026-09-20. Slurm availability and free-space figures are
snapshots; paths, resource contracts, and software versions are the durable
facts.

### Access and identity

* User: `hengzhi` (UID 59466, primary group `grad`).
* Home: `/home/h/hengzhi`.
* Repository: `/home/h/hengzhi/SLS`.
* Login hosts: `xlogin1.comp.nus.edu.sg` and `xlogin2`.
* Documented SSH route:

```bash
ssh -J hengzhi@sjump.comp.nus.edu.sg hengzhi@xlogin.comp.nus.edu.sg
```

The human operator runs every server command. A local agent must provide
commands and inspect pasted output; it must not claim to have directly accessed
NUS.

### Operating system and login-node limits

* OS: Ubuntu 24.04.5 LTS, x86-64.
* Verified kernel: `6.8.0-139-generic`; glibc: `2.39`.
* `xlogin1` is a small virtual login node: 4 QEMU vCPUs and 15 GiB RAM were
  visible in the 2026-09-20 probe.
* Important login-session limits observed by `ulimit -a`: 300 seconds CPU time,
  1 GiB virtual memory, 64 user processes, 1,024 open files, and an 8 MiB stack.
  These limits reinforce that login nodes are for lightweight orchestration,
  not Torch workloads or builds.

### Python and build toolchain

Canonical training interpreter:

```text
/home/h/hengzhi/venvs/sls/bin/python
```

Verified environment:

* Python `3.12.3`, built with GCC `13.3.0`.
* pip `26.2.1`; setuptools `84.0.0`.
* PyTorch `2.6.0+cu124`; packaged CUDA runtime `12.4`.
* cuDNN runtime reported by Torch: `90100` (9.1.0).
* NumPy is not installed. PyTorch consequently emits a recurring
  `Failed to initialize NumPy: No module named 'numpy'` warning. `pip check`
  reports no broken requirements; do not treat this warning as a failure unless
  an actual traceback requires NumPy.
* System GCC/G++: Ubuntu `13.3.0-6ubuntu2~24.04.1`.
* System CMake: `3.28.3`; Git: `2.43.0`.
* The project build tool installs and uses its pinned tools under
  `local/build/tools` (currently CMake 4.4.2, Ninja 1.13.0, pybind11 3.1.0,
  and Zig 0.16.0) rather than relying on system CMake.

### Slurm account and partitions

* Slurm version: `26.05.2`; cluster: `soc`.
* Account: `allusers`; QOS: `normal`.
* Association limits observed on 2026-09-20: at most 16 running jobs and 32
  submitted jobs; no association-level maximum wall time was reported.
* GPU partitions:
  * `gpu`: maximum wall time 3 hours.
  * `gpu-long`: maximum wall time 3 days.
* Canonical physical A100 request for this project:

```text
--constraint=xgpg --gres=gpu:a100-40:1
```

The `xgpg` pool contained eight scheduler nodes (`xgpg0`-`xgpg7`) in the
2026-09-20 inventory. Each advertised 96 CPUs, 224,000 MB scheduler memory, and
one `a100-40` GRES. Other available pools include `xgph` A100 80GB or dual
A100-40 nodes, plus T4, Titan, H100, and H200 pools. Do not move a training run
to another GPU family or topology merely because it is available: checkpoint
runtime contracts and a new benchmark may be required.

Historical jobs have run on both physical `NVIDIA A100-PCIE-40GB` nodes in the
`xgpg` pool and `NVIDIA A100 80GB PCIe MIG 3g.40gb` allocations in the `xgph`
pool. GPU marketing-name changes are recorded as runtime rebinds, but a
device-count, software-stack, precision, deterministic-setting, or native-source
change must not be silently treated as equivalent.

Typical training allocation:

```text
1 × A100 40GB
16 CPUs
64 GB RAM
1 node
```

The short physical-A100 probe ran on `xgpg2.comp.nus.edu.sg` and observed:

* two AMD EPYC 7352 24-core sockets, 96 logical CPUs total;
* approximately 251 GiB physical RAM and 8 GiB swap;
* one `NVIDIA A100-PCIE-40GB`, 40,960 MiB, compute capability 8.0, 108 SMs;
* NVIDIA driver `580.178.04`;
* node-local `/tmp` on XFS, about 3.5 TB in that allocation.

The node-wide CPU/RAM values are not the job allocation. Respect the requested
16 CPUs and 64 GiB rather than assuming all visible node resources are usable.
Exact compute-node hardware and free capacity can vary between jobs.

For deterministic CUDA workloads, preserve:

```text
CUBLAS_WORKSPACE_CONFIG=:4096:8
```

The 2026-09-20 compute probe confirmed one visible CUDA device, CUDA available,
the setting above, and the expected Torch/CUDA/cuDNN stack.

### Filesystems and server repository state

* `/home` is NFSv4 from `cfs.comp.nus.edu.sg:/mnt/storpool/home`.
* The shared filesystem snapshot was 159 TB total, 142 TB used, and 18 TB free
  (89% used). These are shared totals, not the user's quota.
* `quota -s` reported no limited resources for `hengzhi` on 2026-09-20. Do not
  infer that storage is unlimited or that this result overrides site policy.
* Login-node `/tmp` was a separate 9.8 GB ext4 filesystem. A compute node showed
  a much larger node-local `/tmp`; neither is persistent project storage.
* No persistent scratch path or retention policy has been verified. Do not
  invent one; use the repository under `/home` unless the operator supplies an
  approved alternative.
* Runtime artifacts and checkpoints live under `local/runs/` and are ignored by
  Git. Downloaded archives belong under `runs/archives/` locally.

At the 2026-09-20 probe, the server repository was clean on `main` at
`de2c941f813f3279e925193098dda2f50ffde97e`. Disk usage was approximately:

```text
local/   4.6G allocated
runs/    307M allocated
model/   9K allocated
```

`local/runs/` contained 307 `.pt` files with 5,630,344,416 apparent bytes. It
contained the A0 FullRun v2/v3/v4-15M runs, A0 Act1 5M/10M/20M runs, A20 Act1
30M/40M/50M runs, preparation data, and Slurm logs. It also retained
`ironclad-a0-fullrun-v3.failed-820556`; do not delete failed-run evidence merely
because a later run succeeded.

The completed 50M run is `local/runs/ironclad-a20-act1-v1-50m`. Its manifest
records job `857791`, host `xgpg5`, partition `gpu-long`, 64 workers/8 shards,
50,003,968 environment steps, Torch 2.6.0+cu124, and a physical A100 40GB.
Historical `sacct` lookup for jobs 850182 and 857791 returned Slurm error 1054,
so preserved manifests, stdout/stderr, and archives are the authoritative job
evidence when accounting history is unavailable.

## NUS Operating Rules

`xlogin1` / `xlogin2` are login nodes. Use them for Git, small file inspection,
log inspection, source-gate checks, editable installation of pure Python code,
and Slurm submission.

Do not import/run large Torch workloads, run training, perform CUDA evaluation,
or build the native simulator directly on `xlogin`. Submit such work through
Slurm compute nodes. Prefer `tools/submit_slurm.py` and the project preparation
flow over ad-hoc `srun`/`sbatch` commands.

Do not assume free space reported by `df` for `/tmp` means it is safe for large
builds. Login-node `/tmp` is small and has previously hit a per-user write quota;
compute-node `/tmp` is node-local and ephemeral.

Before submission, normally verify:

```bash
cd /home/h/hengzhi/SLS
git status --short
export SLS_PY=/home/h/hengzhi/venvs/sls/bin/python
export CUBLAS_WORKSPACE_CONFIG=:4096:8
```

The Git status should be empty unless the exact dirty state has been reviewed.
Do not assume the server automatically has the local workstation's uncommitted
changes.

Do not modify or delete checkpoints unless explicitly required.

Never run destructive cleanup such as:

```bash
git clean -fdx
```

on the NUS repository without explicit approval, because important runtime data
under `local/` is Git-ignored.

A native simulator/source change normally requires a new Preflight and, when
required by the training contract, a new worker Benchmark before long training.
The established A20 layout is 64 workers and 8 shards, but old throughput is
advisory after workload, native, GPU, or software changes.

Training submissions normally request one GPU, 16 CPUs, and 64 GiB. Long jobs
use `gpu-long`; short qualification/evaluation work can use `gpu` within its
3-hour limit. Project submissions arrange a batch `TERM` signal before walltime
so the trainer can checkpoint at a safe boundary; do not replace this with an
unreviewed signal or launch wrapper.

When a Slurm job fails, inspect its actual `.err` traceback and manifest before
changing dependencies, checkpoints, or resubmitting. A stale-native probe may
be followed by a successful rebuild in the same preparation job. Likewise, the
NumPy warning and the one-time lazy cuBLAS-context warning are known noise unless
the actual traceback identifies them as the cause.

Shared-NFS directory renames made by a compute node may be briefly invisible
from a login node. Retry reads for up to about one minute before declaring a
successfully written migration/checkpoint missing.

## Independent Audit Rule

When asked to perform an independent audit, audit the implementation independently.

Treat source code, native implementation, tests, generated artifacts, and reproducible runtime evidence as primary evidence.

Do not assume that README files, documentation, comments, plans, previous audit reports, or other descriptive documents are correct merely because they claim a feature is implemented or verified.

Documentation may be used to understand intended behavior, but independently verify material claims against the implementation and evidence. Report discrepancies rather than reconciling them silently.
