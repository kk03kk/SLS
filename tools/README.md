# 工具入口与发布说明

20M后续入口：`prepare_act12_long_run.py` 在本地深验证真实study结果并绑定计划；`initialize_act12_continuation.py` 保留完整学习/环境状态；`submit_act12_long_run.py` 登录节点轻量提交，`run_act12_long_run.py` 在GPU续接allocation。结果/机制复算使用 `report_act12_progress.py`、`diagnose_act12_learning.py`；静态字段缓存仅有 `benchmark_act12_field_cache.py` 原型，不集成生产。见[契约与限制](../docs/results/act12-long20m-20261007/README.md)。

在项目根目录、已安装依赖的 Python 环境中执行。先用 `--help` 查看参数；能显示帮助不代表训练、服务器提交或实机采集已验证。

2026-10-05 整理及 Oracle 修复后：这里有 64 个 Python 脚本、2 个 Java probe 和本说明，共 67 个正式文件。全部保留上传。自动生成的 `__pycache__/` 不上传；训练、分析与采集输出另存本地或实验归档目录。

## 常用入口

Act2审计补充：`replay_act2_clock_conditioned_trajectory.py`只用于读取独立stock日志的Discovery计时条件诊断，结果不能替代严格production通过；范围见[计时证据说明](../docs/results/act2-qualification-20261006/CLOCK_CONDITIONAL.md)。

| 工作 | 命令 | 说明 |
| --- | --- | --- |
| 安装、构建、测试 | `python tools/bootstrap.py --with-model` | 安装依赖，不是下载预训练权重 |
| 构建 native | `python tools/build_native.py` | Windows/Linux；产物在 `local/build/` |
| 配置检查 | `python tools/check_training_configs.py` | 检查 TOML、实验 JSON 和兼容性记录，包括配置哈希及种子范围；不证明实验最优或提交就绪 |
| 注册表检查 | `python tools/generate_content_registry.py --check` | 只读，不写正式 JSON |
| 当前词表检查 | `python tools/generate_policy_vocabulary.py --check` | 只读，当前输入 v5 |
| 训练/评估 | `python tools/train_full_run.py --help` / `python tools/evaluate_checkpoint.py --help` | 正式执行需配置、证据与相应算力 |
| 导出策略 | `python tools/export_policy.py --help` | 需要本地 checkpoint |
| 原版环境检查 | `python tools/check_live_setup.py --help` | 本机游戏、Mod 与模型依赖 |

NUS 登录节点只用于轻量 Git、身份检查和提交；native 构建、preflight、评估和训练应在计算节点执行。Act 1–2 准备工具要求完整的 90M 父实验及其证据，未完成评估的目录不能直接用作父实验。

## 全部文件索引

各类工具代码均可上传。历史入口保留是为了复算与来源追溯，不是推荐重跑旧实验。工具说明摘自当前源码；执行约束以参数、配置和证据校验为准。

### 通用开发、训练与评估

| 文件 | 职责 |
| --- | --- |
| `bootstrap.py` | Install the source checkout and build the native simulator. |
| `build_native.py` | Build the canonical native simulator on Windows or Linux. |
| `generate_content_registry.py` | Regenerate canonical content IDs from the committed simulator headers. |
| `generate_policy_vocabulary.py` | Generate or verify the committed collision-free policy vocabulary. |
| `check_training_configs.py` | Validate every checked-in run configuration without touching the simulator. |
| `check_training_sources.py` | Read-only source-gate diagnosis; safe on login nodes, without Torch/native imports. |
| `benchmark_workers.py` | Benchmark and select the smallest near-peak FullRun worker layout. |
| `preflight_training.py` | Fail-fast Linux/GPU training preflight with an exact-resume micro-test. |
| `prepare_and_train.py` | Prepare a single-stage curriculum run on a Slurm compute node, then exec training. |
| `train_full_run.py` | Train one versioned Ironclad curriculum recurrent PPO run. |
| `submit_slurm.py` | Submit one-GPU SLS qualification, evaluation, diagnosis, or training jobs. |
| `evaluate_checkpoint.py` | Evaluate one training checkpoint against the current native simulator. |
| `compare_checkpoints.py` | Evaluate two checkpoints on one identical unseen seed set. |
| `diagnose_checkpoint_contract.py` | Print a read-only checkpoint/current-trainer contract diff as JSON. |
| `verify_training_resume.py` | Verify that a saved PPO checkpoint produces an exact next update. |
| `seal_training_milestone.py` | Fail-closed milestone seal with exact-next-update reproducibility evidence. |
| `export_policy.py` | Export a strict standalone live-game policy artifact. |
| `initialize_act1_continuation.py` | Fork a verified best checkpoint into a new Act1 budget, without resetting learning. |

### Act 1–2 准备，待完整父实验绑定

| 文件 | 职责 |
| --- | --- |
| `audit_act2_pilot_rules.py` | Record six bounded high-impact Act2 scenarios against SHA-identified stock bytecode; no complete parity claim. |
| `import_act12_parent.py` | Copy only SHA-bound parent evidence into a fresh clone without overwriting or removing the old run. |
| `prepare_act12_pilot.py` | Bind an Act1-2 pilot to a completed, reviewed 90M parent; never submit a job. |
| `submit_act12_pilot.py` | Submit one hash-bound Act1-2 pilot after local validation and GitHub push. |
| `analyze_act12_pilot.py` | Verify a completed Act1-2 pilot and recompute normal-start paired outcomes. |

### 历史实验复算、固定提交与迁移

| 文件 | 职责 |
| --- | --- |
| `submit_win70m.py` | Submit one preregistered Win continuation job; no duplicate submissions. |
| `submit_win90m.py` | Submit one verified Win 70M-to-90M job; preparation runs on the GPU node. |
| `analyze_win_continuation.py` | Verify a completed Win continuation and recompute paired development evidence. |
| `analyze_reward_screen.py` | Verify downloaded reward-screen evidence and recompute all paired outcomes. |
| `analyze_training_history.py` | Summarize archived FullRun logs without executing a model evaluation. |
| `recheck_audit_claims.py` | Recompute the old audit's statistical claims without trusting its prose. |
| `prepare_model_warm_start.py` | Verify and migrate the audited v3 policy into the current input contract. |
| `prepare_training_migration.py` | Create a new FullRun chain from an exact curriculum checkpoint. |
| `prepare_act1_plateau.py` | Build a current-environment baseline and failure corpus in one Slurm GPU job. |
| `verify_fullrun_audit_evidence.py` | Gate the 30-trajectory/9-boss and 10k-seed acceptance evidence. |

### 可复用诊断与保真校验

| 文件 | 职责 |
| --- | --- |
| `compare_run_arms.py` | Compare two training runs arm-to-arm on their recorded per-seed evaluations. |
| `analyze_act1_failures.py` | Summarize Act 1 failures from a canonical evaluation artifact. |
| `diagnose_a20_act1_macro.py` | Run a compact, decision-level A20 Act1 policy diagnostic. |
| `diagnose_act1_corpus.py` | Capture a stratified Act1 corpus through the canonical deterministic evaluator. |
| `analyze_act1_corpus.py` | Replay every recorded decision and summarize diagnostic behavior, not policy rules. |
| `replay_act1_corpus.py` | Replay pinned stock trajectories against the current native build on CPU. |
| `replay_failed_state.py` | Replay a fail-fast Simulator decision boundary from a worker crash dump. |
| `audit_policy_seed.py` | 按模型登记 profile 审计确定性 seed 与诊断防御反事实；v2 记录实际环境，修复 A20 被误测为 A0 的问题 |
| `audit_neow_headroom.py` | Is the Neow headroom learnable from the offered blessings, or does it need foresight? |
| `intervene_policy_decision.py` | Measure what a single decision point is worth by intervening on it. |
| `audit_simulator_seeds.py` | Long-run simulator invariant and checkpoint round-trip audit. |
| `audit_a20_act1_targets.py` | Write a hash-bound, conservative A20 Act1 audit target inventory. |
| `audit_act1_acquisition.py` | Record acquisition routes without promoting native pool membership to stock parity. |
| `audit_act1_map_paths.py` | Compare native Act1 map paths with stock JAR bytecode without opening the game. |
| `audit_act1_mechanics.py` | Bounded stock-bytecode comparisons for RNG, transform selection and gremlin moves. |
| `audit_act2_rule_scenarios.py` | Record bounded Act2 native rule scenarios against an identified stock JAR. |
| `audit_stock_bytecode.py` | 从本地原版 JAR 生成内容字节码清单。 |
| `audit_stock_parity.py` | 构建带来源的原版/模拟器保真清单。 |
| `build_semantic_coverage.py` | Expand stock bytecode inventory into fail-closed method obligations. |
| `capture_policy_trajectory.py` | Capture one simulator or CommunicationMod policy trajectory. |
| `compare_policy_trajectories.py` | Compare simulator and Original policy trajectories lock-step. |

### 原版接入、演示与 Oracle

| 文件 | 职责 |
| --- | --- |
| `build_oracle.py` | 从全部正式 Java/资源源码构建完整 Oracle，不依赖旧 Oracle JAR |
| `verify_oracle.py` | 检查构建身份、受保护的有限实机验证与有备份安装 |
| `capture_oracle_smoke.py` | 游戏子进程中的模式/字段/场景检查，不需要模型 |
| `check_live_setup.py` | Check what is still needed before demonstrating a model in the Steam game. |
| `configure_live_inspector.py` | Safely configure CommunicationMod to launch the local live inspector. |
| `play_live.py` | Attach a production policy to the currently running Ironclad game. |
| `play_live_inspector.py` | Inspect and control a live SLS policy from a loopback browser dashboard. |
| `run_original_canary.py` | Launch one recoverable Original-game policy trajectory canary. |
| `run_original_card_audit.py` | 启动可恢复的原版卡牌批量采集，当前仅支持 Windows。 |
| `capture_original_card_batch.py` | 与 CommunicationMod 通信并记录原版卡牌批量场景。 |
| `build_full_audit_oracle.py` | 扩展已有 Oracle JAR 的卡牌、药水和遗物 allowlist。 |
| `build_observation_oracle.py` | Compile public-observation patches into a new Oracle JAR without launching the game. |

### Java probe

| 文件 | 职责 |
| --- | --- |
| `java/StockAct1MapProbe.java` | 使用本地原版游戏类检查 Act 1 地图生成 |
| `java/StockAct1MechanicsProbe.java` | 使用本地原版游戏类检查有限机制 |

## 清理与执行边界

- 保持入口路径：测试、文档、提交脚本与部分身份哈希依赖这些位置，未仅为美观重命名或迁移。
- `submit_win70m.py`、`submit_win90m.py` 绑定旧实验的固定配置和源码身份；当前版本不能靠跳过检查重跑它们。下一阶段新计划必须重新绑定当前源码。
- 原版启动器目前支持 Windows，默认检测 Steam 库，也可通过 `--game-root` 显式指定；`--help` 不要求本机游戏环境。
- Java 源码与补丁上传；原版游戏 JAR、Mod、javac、Oracle JAR、class 和采集输出不放入这个源码目录。
- 两个旧 Oracle 构建器修改已有基础 JAR，保留历史用途。完整来源现已恢复；正常构建使用 `build_oracle.py`，参见 [Oracle 说明](../native/oracle/README.md)。原版自然局使用 v11 production，确定性探针必须显式使用 validation。
- 原版 canary 可选择 `--superfast-mod`，需把该运行条件及哈希记录到轨迹证据中。
- 两个生成器不加 `--check` 时会写正式注册表/词表。修改正式数据后，必须检查契约变化并重新准备证据。

本轮修复、源码身份变化与验证见 [三个目录复查](../docs/three-folder-review-20261005.md)。

`analyze_act12_download.py`：核验Act1–2 lite下载并复算联合通关、配对reach、训练窗口和critic描述统计；只显式允许缺服务器policy export，其他证据必须存在且哈希正确。`analyze_reward_screen.analyze_run` 默认完整性要求保持严格。见[2026-10-06证据](../docs/results/act12-pilot-20261006/README.md)。

## A20 Act2 差分核验

见[当前证据与训练门禁](../docs/results/act2-qualification-20261006/README.md)。工具不启动 NUS 训练，不覆盖证据。

| 入口 | 用途 |
| --- | --- |
| `capture_act2_stock_sources.py` | 本地合法 JAR 的 javap 与来源摘要；原始反汇编仅留本地 |
| `prepare_act2_scene_manifest.py` | 从已审核 stock 方法准备 24 个场景及固定 seed 清单 |
| `run_act2_encounter_batch.py` / `capture_act2_encounter_batch.py` | 显式 A20、多决策边界、单进程、备份恢复的 validation 批次 |
| `replay_act2_encounter_batch.py` | 独立 stock 对象、公共状态、实际 mask 与 RNG 差分 |
| `capture_act2_reference_scripts.py` | 冻结正常 Neow 动作请求；native 结果不能当 stock 正确答案 |
| `run_act2_flow_batch.py` / `capture_act2_system_batch.py` | 正常开局 validation 脚本；每批不超过30分钟 |
| `capture_act2_stock_policy_system_batch.py` | 固定系统seeds的原版单一冻结策略记录完整脚本，native随后重放同一动作；实测条件输入仅用于validation |
| `replay_act2_system_batch.py` | 跨幕、奖励及逐边界 checkpoint 恢复/延续核验 |
| `select_act2_canaries.py` | 按固定区间和 seed 顺序选覆盖轨迹；缺类别则报缺失 |
| `capture_act2_production_batch.py` / `replay_act2_production_trajectory.py` | 冻结90M、显式Act2 horizon、production自然局及相同动作 native 重放 |
| `check_probe_source_preservation.py` | 隔离旧/新 native 验证纯接口修改；不能覆盖后续规则修复 |
| `check_act2_choice_identity.py` | 从实际stock边界复算15个PolicyBatch张量的UI身份等价证明 |
| `summarize_act2_qualification.py` | 严格汇总72义务/8自然轨迹；缺项、失败、过期来源不会通过 |


## 修复版联合通关λ pilot

`submit_act12_lambda_study.py`是新实验唯一提交入口；`run_act12_lambda_study.py`在同一GPU节点串行两臂并核对完成状态；`analyze_act12_lambda_study.py`严格比较固定终点联合通关。配置迁移和本地验收见[TRAINING_READY.md](../docs/results/act2-qualification-20261006/TRAINING_READY.md)。`profile_act12_batching.py`与`verify_encoder_optimization.py`保留编码性能及行为等价的复算入口，不导出模型作为服务器训练结果。

## 三幕＋心脏校验（进行中）

`prepare_fullrun_audit.py`生成保守全范围台账；`prepare_shared_scenes.py`冻结有stock身份的共享场景并拒绝seed冲突；`summarize_fullrun_parity.py`汇总证据但不升级为整体资格。现有遭遇工具通过`--manifest`支持新增语料，旧Act2入口保留。实际进展、首次分歧与尚未实现的晚幕上下文见[首批记录](../docs/results/fullrun-parity-20261007/README.md)。
