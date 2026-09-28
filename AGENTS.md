# AGENTS.md

## Project context

SLS is a Slay the Spire 1 simulator and recurrent PPO agent. The Ironclad A20 Act1 46M → 60M stage is complete; its selected 56M champion is the default demonstration model. No further training stage is scheduled. Results and environment identity are under `docs/results/a20-act1-60m-stable/`. Start with `README.md`, `docs/architecture.md`, and `docs/repository-map.md`; older experiments are indexed under `docs/history/`.

## Local development

The primary workstation is Windows at `D:\SLS`. Use the Python 3.12 Conda environment `DL` for development, native builds, tests, parity auditing, and other local tasks. Activate it with `conda activate DL`. Keep generated files under ignored `local/`, archives under ignored `runs/archives/`, and exported policies under ignored `model/`.

## Training and evidence

The NUS server is operated by the human user. Local agents must provide commands and inspect pasted output; they must not claim direct server access. Login nodes are for Git, inspection, and Slurm submission only. Native builds, Torch workloads, preflight, evaluation, and training belong on compute nodes through `tools/submit_slurm.py` and the project preparation flow. Do not modify or delete checkpoints, manifests, Slurm logs, or unique audit evidence during source cleanup. Do not run `git clean -fdx` on the server.

Preserve source/config/checkpoint identity and seed separation. A weight transfer is not an exact resume. Native or observation changes require appropriate qualification before long training. Report independent evaluation and selected checkpoint hashes, not just training steps.

## Independent audits

When asked for an independent audit, verify claims against source, native implementation, tests, artifacts, and reproducible runtime evidence. Documentation and earlier audit prose may explain intent but are not proof.
