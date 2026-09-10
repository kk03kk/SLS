# Job 837265: continuation direct-script import failure

The supplied fatal traceback identifies `from tools.train_full_run import ...`
inside `initialize()` as the failure. Directly executing a file under `tools/`
puts that directory, not the checkout root, on Python's import path. The script
added `src/` but omitted the checkout root. Pytest imported `tools` in-process;
the earlier validation used `python -m`, which also exposed the checkout root.
Neither exercised the preparation subprocess entry point.

The initializer, checkpoint preflight, and contract diagnostic now bootstrap both
the checkout root and `src/`. Preflight uses a qualified sibling import. The
other tools importing `tools.train_full_run` already bootstrap the root. No
manual `PYTHONPATH`, training-contract exemption, or dependency change is needed.

## Failure boundary and retry

The failing import precedes config reads, checkpoint loading, target directory
creation, and all initializer writes. Job 837265 therefore could not create a
partial continuation through this initializer. Preparation can have created the
benchmark parent directory and `run.lock`; process exit releases its OS lock.
The local continuation target was absent when inspected. This is not a claim
that the remote filesystem was inspected.

Child creation writes into a separate temporary sibling directory and publishes
the complete run with one rename. Regression coverage injects a failure before
publication and verifies that retry succeeds and the parent SHA is unchanged.
Re-entry into a matching initialized child leaves its latest checkpoint intact.
An unrelated/incomplete existing target is refused, never silently overwritten.
No cleanup of the original 5M run is needed.

## Validation and unchanged experiment

Regression coverage calls the real `prepare_and_train.run_tool()` subprocess
launcher from the checkout root with `PYTHONPATH` removed. It creates a small
temporary continuation, performs actual worker/PPO checkpoint preflight through
the direct script, and repeats initialization without rewriting latest. A second
initializer invocation uses Python isolated mode. The diagnostic CLI is also
checked in isolated mode. These are local CPU integration tests; Linux Slurm and
the server CUDA stack remain checked by `--prepare` on the compute node.

The training config and learning implementation are unchanged. Read-only inspection
of the downloaded parent verifies SHA256
`e0a86734786b520ad50d23ff05bed9241e4a4d7cc8442d6372c8fa5a47645b73`
and 4,505,600 steps. The target remains cumulative 10,000,000 steps at LR
0.000125, preserving model, Adam state, RNG, recurrent memory and worker states.
Tests verify restored next-update equivalence and the existing half-LR branch.

Run the related and full pytest suites and `python -m ruff check .` before
publishing. `submit_slurm.py ... --prepare --dry-run` checks submission generation
without submitting a job. Changed preparation scripts invalidate preparation
evidence; the entry point reruns the required preflight while retaining a matching
existing benchmark and worker layout. There is no new warm-start or native change.

After pulling the fix, the user can submit:

```bash
cd ~/SLS
git pull --ff-only origin main
/home/h/hengzhi/venvs/sls/bin/python tools/submit_slurm.py train \
  --config configs/train/ironclad_a0_act1_10m.toml --prepare
```
