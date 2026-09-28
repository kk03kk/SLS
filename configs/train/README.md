# Training configurations

| Configuration | Status |
| --- | --- |
| `ironclad_a20_act1_plateau_progress_2m.toml` | Prepared 56M reward-screening control; not launched |
| `ironclad_a20_act1_plateau_win_2m.toml` | Prepared win-objective comparison; not launched; [workflow](../../docs/act1-plateau-workflow.md) |
| `ironclad_a20_act1_60m_stable.toml` | Completed; selected 56M champion, 1,585/2,048 final wins; see [results](../../docs/results/a20-act1-60m-stable/README.md) |
| `ironclad_a20_act1_54m_recovery.toml` | Completed recovery experiment; not promoted |
| `ironclad_a20_act1_60m_optimization.toml` | Earlier optimization experiment; stopped after regression |
| `ironclad_a20_act1_50m.toml` | Completed run containing the 46M historical champion |
| Other A20 / A0 / FullRun files | Historical experiments and reproducibility records |

No new training run is scheduled. Inspect a configuration and its required source checkpoint before submitting. A `.toml` file does not include weights or runtime data. Keep old config paths stable because manifests, audits and server artifacts refer to them. The completed 60M job used a separate directory and the source identity recorded in its result summary.
