# Training configurations

| Configuration | Status |
| --- | --- |
| `ironclad_a20_act1_win_90m_continuation.toml` | COMPLETE at 90,013,696; fixed endpoint 1640/2048 vs frozen 70M 1574/2048, +3.22pp; periodic selected 76M retained separately; [results](../../docs/results/win90m-20261005/README.md) |
| `ironclad_a20_act1_win_70m_continuation.toml` | COMPLETE at 70,008,832; fixed endpoint improved over frozen 56M on paired development; [results](../../docs/results/win70m-20261001/README.md) |
| `ironclad_a20_act1_plateau_progress_2m_r1.toml` / `ironclad_a20_act1_plateau_win_2m_r1.toml` | Completed reward screen (123 updates each); [results](../../docs/results/plateau-reward-screen-20260930/README.md) |
| `ironclad_a20_act1_plateau_progress_2m.toml` | Job 886247 failed on standalone reward encoding after update 1; evidence retained |
| `ironclad_a20_act1_plateau_win_2m.toml` | Job 886248 blocked by failed dependency; superseded by r1 recovery config |
| `ironclad_a20_act1_60m_stable.toml` | Completed; selected 56M champion, 1,585/2,048 final wins; see [results](../../docs/results/a20-act1-60m-stable/README.md) |
| `ironclad_a20_act1_54m_recovery.toml` | Completed recovery experiment; not promoted |
| `ironclad_a20_act1_60m_optimization.toml` | Earlier optimization experiment; stopped after regression |
| `ironclad_a20_act1_50m.toml` | Completed run containing the 46M historical champion |
| Other A20 / A0 / FullRun files | Historical experiments and reproducibility records |

The 2026-10-05 completed 90M archive is locally verified. Its `final_evaluation` is development confirmation, not the reserved 9e12 final test. The Act1-2 recipe remains provisional until qualification and the new parent/config are bound; completed results alone do not submit it.

These files are experiment records, not a menu of jobs safe to submit against current HEAD. Inspect the required source checkpoint and source/runtime contracts before a new submission. A `.toml` file does not include weights or runtime data. Keep old config paths and bytes stable because manifests, audits and server artifacts refer to them. The completed 60M job used a separate directory and the source identity recorded in its result summary. See the [folder guide](../README.md) for all configurations and retention policy.
