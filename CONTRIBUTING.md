# Contributing to SLS

Thanks for improving the project. Start with the [README](README.md), [architecture](docs/architecture.md), and [repository map](docs/repository-map.md). Small, focused pull requests are easiest to review. You can work on simulator rules, public observations, evaluation, training performance, docs, or the live inspector.

## Local setup

Python 3.12 is the validated development version on Windows and Linux; the package metadata permits newer versions, which need separate qualification. From the repository root, run `python tools/bootstrap.py --with-model` to install the locked development dependencies, build the native module and run the full test suite. Simulator-only work can omit `--with-model`; bootstrap then runs a Torch-free simulator test selection. `--skip-tests` performs installation and build without tests, for workflows that run tests separately. Run `python -m ruff check .` before proposing a change. CI also runs native sanitizer tests on Linux.

Do not commit `local/`, checkpoints, exported `.pt` files, archives, game assets, Mod JARs, secrets or credentials. They are intentionally ignored. If a bug needs a large artifact to reproduce, share a minimal description and artifact hash first, then coordinate a separate transfer.

## Changes that affect trained models

Describe the rule or observation change, the old behavior, the new behavior, and evidence from source/tests or a stock-game comparison. Update the appropriate regression test and compatibility contract when the policy input, simulator semantics, action set, reward or resume state changes. A checkpoint trained under a different contract must not be labelled an exact continuation. Native changes require a fresh build and preflight before long training; GPU work belongs on a compute node, not an NUS login node.

For training experiments, include the config, source revision, starting checkpoint hash, seed ranges, worker layout, runtime details, selection rule and independent final evaluation. Report both the chosen checkpoint and the terminal training state when they differ. Do not report a step count as a win-rate improvement. See the [model release checklist](docs/model-release.md).

## Pull requests

Explain the intended behavior and relevant evidence in the [PR template](.github/PULL_REQUEST_TEMPLATE.md). Keep historical configs and dated audit records for reproducibility; add new findings rather than rewriting an old experiment as if it were current. Fix documentation links if files move. The root [MIT license](LICENSE) covers project contributions; the original native simulator attribution remains in [`native/simulator/LICENSE.lightspeed.md`](native/simulator/LICENSE.lightspeed.md).
