# Command entry points

Run these from the repository root with the active Python environment. Use `--help` for supported options.

| Task | Command |
| --- | --- |
| Install dependencies, build native, run tests | `python tools/bootstrap.py --with-model` |
| Rebuild native | `python tools/build_native.py` |
| Train on a local machine | `python tools/train_full_run.py --help` |
| Submit an NUS Slurm job | `python tools/submit_slurm.py --help` |
| Prepare current A20 baseline and failure analysis | `python tools/submit_slurm.py plateau --config configs/diagnostics/ironclad_a20_act1_plateau.toml --constraint xgpg` |
| Check training readiness | `python tools/preflight_training.py --help` |
| Evaluate a checkpoint | `python tools/evaluate_checkpoint.py --help` |
| Export a policy | `python tools/export_policy.py --help` |
| Capture a model trajectory | `python tools/capture_policy_trajectory.py --help` |
| Run a recoverable stock-game parity canary | `python tools/run_original_canary.py --help` |
| Compare simulator and stock trajectories | `python tools/compare_policy_trajectories.py --help` |
| Replay the pinned Act1 regression corpus | `python tools/replay_act1_corpus.py --help` |
| Inspect Act1 acquisition routes | `python tools/audit_act1_acquisition.py --help` |
| Execute short stock-bytecode mechanics comparisons | `python tools/audit_act1_mechanics.py --help` |
| List or inspect local policies | `python tools/play_live_inspector.py --list-models` |
| Check Steam game, Mods and model setup | `python tools/check_live_setup.py` |
| Configure CommunicationMod to open the model window | `python tools/configure_live_inspector.py` |

The NUS login node is for light Git and submission work. Native builds, preflight, evaluation and training must run on a compute node through the preparation flow. The completed A20 job is documented in [its operator instructions](../docs/a20-act1-60m-stable-launch.md). The new [plateau workflow](../docs/act1-plateau-workflow.md) is prepared but not launched. Older analysis and migration tools remain here because past experiments reference them; their presence is not a recommendation to launch an old workflow.

For local parity audits, `run_original_canary.py` accepts an optional
`--superfast-mod PATH` pointing to a local SuperFastMode JAR. The canary
backs up and restores game configuration, saves and Mods after the run.
The speed mod is excluded by default; record its version and hash with any
trajectory evidence that uses it.
