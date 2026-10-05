# Dependency locks

`test.lock` pins pytest, Ruff and their support libraries. `model.lock` pins the model/training stack, including Torch 2.6.0 and its Linux x86_64 CUDA 12.4 dependencies. Platform markers prevent installing those NVIDIA and Triton packages on Windows. These are installation inputs, not records of every existing developer environment.

Use `python tools/bootstrap.py --with-model` for a complete development installation, or omit `--with-model` for simulator-only work. The package build tools are pinned separately in [pyproject.toml](../pyproject.toml). No game or Mod dependency is distributed here.

The NUS 90M run used Torch 2.6.0. The local DL environment may contain a different Torch version; local checks do not establish cross-version training reproducibility. Preserve each run's runtime identity and checkpoint contract. Do not update dependencies merely to tidy this directory, or label a different runtime an exact reproduction.
