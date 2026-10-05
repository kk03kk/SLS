# GitHub automation

`workflows/ci.yml` has two Linux/Python 3.12 jobs:

- Install/build once, run Ruff and the full tests, check generated vocabulary/content and training configs, then verify installed-wheel package data outside the checkout.
- Build native code with sanitizers and run simulator/training smoke tests with the required ASan preload.

Bootstrap uses `--skip-tests` because the first job has an explicit test step. Actions checkout/setup-python v7 tags were checked against their official repositories on 2026-10-05. A local pass does not imply this workflow has run on GitHub. Windows validation uses the local DL environment; these jobs do not certify Windows or original-game parity.

`PULL_REQUEST_TEMPLATE.md` asks contributors to record behavior, compatibility and validation evidence. Keep this automation and template in the public repository.
