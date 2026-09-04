# Local test-model library

This directory is the single model source for the live-game inspector.

- Put only exported `sls-policy-artifact-v5` `.pt` files here.
- Do not copy training checkpoints such as `latest.pt` here. They contain
  optimizer and resume state and must first be exported with
  `tools/export_policy.py`.
- Use descriptive names such as `ironclad-a0-act1-5m.pt`.
- The inspector validates model schema, weight digest, goal, and Ascension
  range before listing or loading a file.

List all currently testable models:

```powershell
D:\Anaconda\python.exe tools\play_live_inspector.py --list-models
```

Export a training checkpoint into this library (output defaults here):

```powershell
D:\Anaconda\python.exe tools\export_policy.py <checkpoint> `
  --goal ACT1 --ascension-min 0 --ascension-max 0
```
