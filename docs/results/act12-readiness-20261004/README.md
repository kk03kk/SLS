# Act 1–2 development evidence

See [independent report](../../audits/2026-10-04-act12-readiness.md).

`zero-shot-act12.json`, `horizon-probe.json` and `pytest.txt` are unchanged copies of
local evidence generated on 2026-10-03. `manifest.json` binds their bytes and hashes.
Git attributes preserve this evidence directory's bytes across LF/CRLF checkouts.
The model probe performs real battles; the boundary probe skips battles and is not
performance evidence. Neither updates weights or represents NUS training.

To repeat the model diagnostic on the Windows development environment, restore the
SHA-pinned 70M endpoint at the path checked by the script, build the matching native
module, activate Conda DL and run from the repository root:

```powershell
conda activate DL
python docs/results/act12-readiness-20261004/zero_shot_probe.py
```

The script writes only under `local/reports/act12-audit-20261003/`; it checks the parent
SHA and saves actual runtime/native identity. Its portable repository-root lookup,
output-directory creation and SHA assertion were added when preserving this probe;
the evaluation body is the body used for the recorded run. The recorded run used
CUDA, four environment shards, two Torch CPU threads and high matmul precision.
Different runtimes can change greedy trajectories; compare recorded identities.

Seeds `[8000005000000, 8000005000032)` are exposed development data. Do not relabel
them as final evaluation, extrapolate their 0/32 result to the 90M model, or use
boundary-probe results as evidence of learned gameplay.
