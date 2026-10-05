# AGENTS.md

## Project context

SLS is a Slay the Spire 1 simulator and recurrent PPO agent project for training and evaluating agents that play Slay the Spire.

This `AGENTS.md` file defines persistent top-level instructions for agents working on the project. **Do not modify, rewrite, update, or delete this file unless the human user explicitly asks you to do so.** Project progress, experiment status, results, and other changing information belong elsewhere, not in this file.

## Local development

The primary workstation is Windows at `D:\SLS`. Use the Python 3.12 Conda environment `DL` for development, native builds, tests, parity auditing, and other local tasks. Activate it with `conda activate DL`.

Code changes should be completed and appropriately validated locally first.

## GitHub and server workflow

The normal workflow is:

**local development → local validation → push to GitHub → server pulls from GitHub → server-side execution/training**

The agent works on the local repository.

When work needs to be performed on the NUS server:

1. Complete the necessary code/configuration changes and appropriate validation locally first.
2. Commit and push the required changes to GitHub.
3. Provide the human user with exact commands to run on the server, including pulling the corresponding changes from GitHub and performing the required server-side work.
4. The human user executes those commands on the NUS server and pastes the output back.
5. Inspect the pasted output and determine the next step.

Do not assume local changes are already present on the server. Server-side work should follow the local change → GitHub push → server pull workflow.

## Independent audits

When asked for an independent audit, verify claims against source, native implementation, tests, artifacts, and reproducible runtime evidence. Documentation and earlier audit prose may explain intent but are not proof.