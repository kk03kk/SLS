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

* OS: Ubuntu 24.04 LTS
* User: `hengzhi`
* Home: `/home/h/hengzhi`
* Repository: `/home/h/hengzhi/SLS`
* Python: `/home/h/hengzhi/venvs/sls/bin/python`
* Python version: 3.12.3
* Scheduler: Slurm
* Account: `allusers`
* QOS: `normal`
* GPU partitions: `gpu`, `gpu-long`
* Primary training GPU: NVIDIA A100 40GB
* Runtime artifacts/checkpoints: `local/runs/`

Typical training allocation:

```text
1 × A100 40GB
16 CPUs
64 GB RAM
1 node
```

Observed A100 compute node hardware includes AMD EPYC 7352 CPUs and approximately 251 GiB RAM. Exact compute-node hardware may vary between jobs.

Compiler environment:

```text
GCC/G++ 13.3
System CMake 3.28.3
```

The project may install/use its own pinned native-build tools instead of the system CMake.

## NUS Operating Rules

`xlogin1` / `xlogin2` are login nodes. Use them for Git, file inspection, logs, and Slurm submission.

Do not run heavy training, CUDA evaluation, or native simulator builds directly on `xlogin`. Submit such work through Slurm compute nodes.

Do not assume free space reported for `/tmp` means it is safe for large builds. The login-node `/tmp` has previously hit a small per-user write quota despite `df` showing ample free space.

For deterministic CUDA workloads, preserve:

```text
CUBLAS_WORKSPACE_CONFIG=:4096:8
```

Prefer project-provided Slurm and training tools over ad-hoc commands.

Do not modify or delete checkpoints unless explicitly required.

Never run destructive cleanup such as:

```bash
git clean -fdx
```

on the NUS repository without explicit approval, because important runtime data under `local/` is Git-ignored.

A native simulator/source change normally requires a new Preflight and, when required by the training contract, a new worker Benchmark before long training.

Do not treat the recurring PyTorch `No module named 'numpy'` warning as the cause of a failure unless the actual traceback shows that NumPy is required.

When a Slurm job fails, inspect its actual `.err` traceback before changing dependencies, checkpoints, or resubmitting the job.

## Independent Audit Rule

When asked to perform an independent audit, audit the implementation independently.

Treat source code, native implementation, tests, generated artifacts, and reproducible runtime evidence as primary evidence.

Do not assume that README files, documentation, comments, plans, previous audit reports, or other descriptive documents are correct merely because they claim a feature is implemented or verified.

Documentation may be used to understand intended behavior, but independently verify material claims against the implementation and evidence. Report discrepancies rather than reconciling them silently.
