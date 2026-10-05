# SLS 独立工程与逻辑审计 · 2026-09-05

审计基线：`6aeaa9f6c5fda8c8e1b6479b54af440e4bf92f51`，Windows / Conda DL。
开始时唯一 Git 未跟踪文件为用户提供的根目录 `AGENTS.md`；已完整阅读。
本报告基于源码、原版 JAR 字节码、本地 native 实际运行、测试及服务器日志。
已有 README、docs、audit 的结论未被用作实现正确性的证据；最后才阅读维护文档以修正过时描述。

**结论：项目已具备清晰的研究工程骨架和可用的恢复机制，但不能认定其核心语义已成熟或全面 parity 通过。**
最重要的未解决问题是 power 归属在模型编码中丢失、动态卡牌信息不完整，以及战斗中升级费用与原版不一致。
9M checkpoint 的计数及状态检查未发现损坏；服务器日志反映进展停滞，不能把稳定运行等同于训练有效。

## 范围与证据强度

对基线全部 **242 个跟踪文件**建立路径、尺寸、行数和 SHA256 清单：58 个 src 文件、85 个 native 文件、
50 个 tests 文件、30 个 tools 文件，以及配置、依赖、CI、文档等。文件数包含 JSON、头文件和第三方库。
对训练、恢复、模型输入、终止条件、评估、native 动作链等高风险调用路径做实现级检查。
这是跨全仓模块的风险审计，**不是逐行证明所有游戏内容和所有组合均正确**；第三方 JSON 库未做逐行安全审计。

| 层 | 本轮直接检查 | 结论边界 |
| --- | --- | --- |
| repository/build/CI | 包发现、资源打包、忽略规则、构建工具、Slurm 命令及测试配置 | 分层基本合理；源码安装与运行产物边界明确；未新建 Linux 环境验证远端 CI |
| contracts/content | action identity、Decision/Transition 不变量、scope/vocabulary、两侧适配 | 防止私有字段进入输入的机制存在；公共信息仍存在丢失 |
| native | RNG、队列/容器边界、升级调用链、public snapshot、恢复、原版字节码对照 | 复现一项真实规则错误；常规测试通过不代表全部内容组合 parity |
| model | 结构化 batching、引用、map adjacency、padding、GRU reset、三域输出 | 未发现当前合法候选 padding 进入采样；power owner/dynamic card 字段不完整 |
| RL | GAE、clipping、value loss、序列切块、优势归一化、entropy、limiter | 主要公式和序列对齐检查通过；有非零 dropout 及指标口径风险 |
| training/curriculum | 配置身份、阶段迁移、调度、中断、promotion | 累积训练步数与 horizon 迁移明确；修复两项 CLI 控制缺陷 |
| checkpoint | CPU RNG 恢复、optimizer、worker、memory、迁移白名单 | 48 个真实 worker 可恢复并复现一步；未验证 A100 9M 完整训练更新的逐位复现 |
| evaluation/selection | argmax、固定/旋转 seeds、boss 指标、rank、最终导出 | 修复 Boss 战术识别；历史成功率命名及 held-out 防线仍需处理 |
| original/runtime/audit | transport、适配、action journal、artifact digest、parity projection | 有确认边界和故障证据机制；未启动游戏或重跑 stock 全量场景 |

本轮没有启动本地大规模模型 evaluation、服务器作业或原版游戏。
checkpoint 仅 CPU 读取；48 个 worker 的双副本各执行一次普通合法动作，无模型推理。
未改动、移动、删除任何已有 checkpoint、训练日志、游戏资产或 native 二进制。

## 发现与处理

### F01 · P1 · 模型丢失 power 的归属，未修复

位置：`src/sls/backends/simulator/environment.py:477`、`src/sls/model/batching.py:201`、
`src/sls/model/transformer.py`。

适配器把归属写在 `PLAYER_POWER:0` / `MONSTER:0:POWER:0` 这样的实例 ID 中；power properties 只有 amount。
batching 将所有 power 编成同一种实体，只用 instance ID 建引用表；没有 power→owner 关系边，
也没有 owner 特征。模型没有把实例 ID 字符串或行号当作位置编码。

独立构造两个其他字段相同的 Observation，分别给玩家和怪物 STRENGTH=3，**所有 EncodedDecision 张量逐项完全相同**。
同一 recurrent memory 下，策略不可能从当前输入区分它们。不同怪物间转移相同 power 也存在这个问题。
历史记忆或 intent 可能部分补偿，不能保证恢复被丢弃的公共状态。

处理建议：建立显式 owner 引用/关系，再升级 encoding/vocabulary/model contract；对 owner 交换添加非等价测试。
这是新输入语义，应建立新训练或明确的模型迁移方案，不能悄悄沿用 v3 输入身份继续声称 exact resume。
本轮保留原模型接口和 9M checkpoint，未以同 schema 改写输入。

### F02 · P1 · Blood for Blood 战斗升级费用错误，未修复

位置：`native/simulator/src/combat/CardInstance.cpp:141`，尤其 `:173`；
`native/simulator/src/combat/Actions.cpp` 中 Armaments/Apotheosis 的升级调用。

实际 native 复现：手牌 `Bloodletting, Armaments+, Blood for Blood`；先打 Bloodletting，后打 Armaments+。
Blood for Blood 从 4 费因受伤降至 3 费，升级后仍为 3 费；原版应为 2 费。
原因是专门分支计算了降费，随后通用升级分支又用静态基础费用覆盖 `cost` 和 `costForTurn`。

已直接对 SHA256 为 `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673` 的
本地原版 JAR 执行 javap。`BloodForBlood.upgrade()` 字节码明确在 cost<4 时执行 `cost - 1` 后调用
`upgradeBaseCost`，没有随后统一重设为 3。反编译 Java 与这条字节码路径一致。

处理建议：单独修复 native 升级路径，覆盖已降费至 3/1/0、临时零费、重复升级，以及其他降费升级牌。
修复后需要新 native 构建与 parity 回归，按 AGENTS 在 Slurm 上完成新 Preflight 和需要的 Benchmark，
再决定环境迁移方式。本轮是审计与工程整理，保留可复现的原 native 基线，不把修改模拟器后继续训练混入此次交付。

### F03 · P2 · 动态卡牌状态在公共输入中丢失，未修复

位置：`native/simulator/python/module.cpp:444`、`src/sls/backends/simulator/environment.py:443`、
`src/sls/contracts/observation.py`、`src/sls/model/encoding.py`。

native combat snapshot 有 `special_data`、retain/free-to-play 等字段，公共 Card 适配主要保留 ID、升级次数和费用。
例如 Ritual Dagger 的基础伤害增长值 15 与 45 在 `_combat_cards` 后得到相同 Card；
public run deck 同样不导出 misc。玩家可见的变化不应被当作不可观测历史处理。
这也限制了 Rampage 等依赖特殊数值的决策；并非所有卡牌伤害都能只从 card ID+upgrades 推导。

处理建议：和 F01 一起设计新公共卡牌 schema，逐字段区分原版可见信息与私有执行状态；
对两端适配及编码同时添加“改变可见数值会改变输入”的测试。未在本轮破坏现有 checkpoint 的输入身份。

### F04 · P1 · exact runtime rebind 的代码语义约束不足，未修复

位置：`src/sls/rl/training_contract.py:15`、`src/sls/rl/checkpoint.py:262`、
`tools/train_full_run.py:478`。

native digest 覆盖 native、simulator adapter、content、build 工具，但不覆盖 PPO/reward/rollout/model/curriculum 源码。
恢复契约记录配置/schema/Git commit；`auto` 恢复时允许 Git commit 和 native digest 变化。
所以只改 PPO 或 reward 实现、保持配置/schema 不变，即可沿该白名单恢复；并没有独立验证“仅无语义变化”。
重绑 native 后旧环境快照可被加载，也不等于新旧 transition 完全等价。

这不证明此次 7M→9M 发生错误迁移：实际日志重绑前后 native digest 相同，且 checkpoint 与当前基线 commit 相同。
风险是机制本身不能可靠阻止未来在同 schema 下改变训练算法却称为 exact。

处理建议：为 Python 学习语义建立独立源摘要/版本；runtime-only 重绑必须有明确的差异和等价证据。
对改变 reward/GAE/model forward 的恢复尝试增加拒绝测试。不要用允许任意 commit 改变代替这类验证。

### F05 · P1 · 诊断评估收到中断会丢失最新训练进度，已修复

基线 `tools/train_full_run.py` 的 diagnostic evaluation 没有捕获 `InterruptedError`，
会进入外层 FAILED，跳过循环后的 latest 保存；selection evaluation 有专门处理，行为不一致。
Slurm 在诊断评估期间发 TERM 可导致从上次周期 checkpoint 回退，而不是安全退出。

修复：记录 `diagnostic_evaluation_interrupted`，停止追加 selection evaluation，走正常 latest 保存及 INTERRUPTED 状态。
通过 CLI 级模拟测试验证“已完成一次 update→diagnostic 收到 TERM→只保存一次 latest→退出码0”，未运行模型训练。

### F06 · P2 · 完成状态被预先覆盖，重复最终评估，已修复

基线先把 manifest 状态写为 RUNNING，再传给 `_can_resume_finalization`，使其无法看见原 COMPLETE。
已完成目标的 train 因此可再次进入 finalization，覆盖完成状态和最终评估产物。

修复：保留原 manifest status；在写入 RUNNING 前拒绝已完成且达到同目标的 stage。
CLI 回归测试检查拒绝时 manifest 字节完全不变。仍允许未完成 finalization 的训练恢复处理。
同时移除不可达的重复 `elif not loaded_exactly` 分支。

### F07 · P2 · Boss 战术统计漏掉所有 Act 2 和 Donu/Deca，已修复

位置：`src/sls/rl/evaluate.py:23`、`:161`。
基线用 visible encounter ID 直接匹配 enemy monster ID；
`AUTOMATON != BRONZE_AUTOMATON`、`CHAMP != THE_CHAMP`、`COLLECTOR != THE_COLLECTOR`、
`DONU_AND_DECA` 也不是单个 monster ID。9M 日志因此有 Act 2 通关记录，却没有 Act 2 boss_action_metrics。
Slime Boss 分裂后同样会停止计数。

修复：明确 encounter→monster 集合，分裂阶段在已经进入 Slime Boss 战后继续计数，
同时避免把普通大史莱姆战误记为 Boss。添加 7 个参数化测试场景。
改变仅涉及诊断统计，不改动作、reward、boss_success_rate 或 checkpoint 选择排序。
历史日志保留原样；新旧战术字段的覆盖范围应注明审计日期，不能用新口径补写历史数字。

### F08 · P2 · boss_success_rate 实际是按 Boss 分组的整幕通关率，未改口径

位置：`src/sls/rl/evaluate.py:248`、`:286`、`:302`；`src/sls/rl/best_checkpoint.py:15`。
任何在当前幕死亡/触发 limiter 的 episode 都算该幕可见 Boss 的一次失败，即使没有进入 Boss 战。
例如 9M 固定评估的 Act 1 三类 boss_attempts 合计1000，而 entries 合计939。
所以此字段不是“进入 Boss 战之后的胜率”；把它解释成 Boss 对战能力会误导训练诊断。

该定义也参与 promotion/selection，不能为修正命名直接替换分母，否则旧阈值和 best metadata 不可比。
建议保留历史字段并清楚标注为按最终 Boss 分组的幕通关率，另增 boss_encounters / boss_combat_wins。
minimum_boss_lcb 是所有已有 Boss 分组的最小值；存在零成功的 Act 3 分组时为0，常常不能起到 Act 1 平衡作用。

### F09 · P2 · held-out 种子保护不完整，未改恢复契约

位置：`tools/train_full_run.py:95`、`:661`、诊断 rotation；`src/sls/rl/ppo.py:186`。
当前 10M 配置 final seeds 从2e12开始，periodic从3e12开始，但 training_seed_limit 仅为3e12。
因此保护代码允许训练使用2e12的 final seeds。diagnostic 也仅检查第一次区间与 held-out 重叠，
没有统一验证实际训练起点和全部旋转区间。

此次 next_seed=10,045,227，离这些区间极远，**没有本次种子污染证据**。
建议统一 namespace validator，以所有保留区间的最早起点约束训练并检查配置种子；诊断每次轮换都验证。
修正 training_seed_limit 会改变现有 checkpoint contract，应通过明确迁移处理。

### F10 · P2 · 非零 dropout 会使 PPO 分子分母来自不同策略模式，未修复

位置：`src/sls/model/transformer.py` 的 ModelConfig.dropout、`src/sls/rl/ppo.py:197`、`:347`。
collect 用 eval 模式，optimize 用 train 模式；若配置 dropout>0，更新前重算概率已有随机差异。
本次 checkpoint 和三份训练配置均为 dropout=0，未受此配置问题影响。
建议 PPO 明确拒绝非零 dropout，或统一训练时策略分布的定义并验证更新前 ratio=1。

### F11 · P2 · 统计、CLI 和证据持久化仍有易误读边界

- `kl_early_stop` 只表示 final KL 超阈值，即使已执行完最后一个 epoch 也标为1。
  此日志194次 update 中122次标1，真正只运行1 epoch的仅8次；不能说62.9%的 update提前结束了第二轮。
- `tools/submit_slurm.py:115` 的 evaluate 分支硬编码 ACT1，CLI 没有 profile 参数。
  `evaluate_checkpoint.py` 本身支持显式 `--profile`；默认 Slurm evaluate 不等于 FullRun evaluation。
- `update_best_checkpoint` 先保存 pt 再替换 JSON；两文件不是事务。进程在两步之间退出可能留下权重/评分不一致。
  建议使用不可变 checkpoint 文件、哈希绑定的 metadata，最后原子更新选择指针。
- metrics append、周期 pt、latest 和 manifest 也不是同一个事务。崩溃恢复可能产生重复 update 日志；
  本次194次 update 连续且无重复，未观察到这一问题。后续日志消费应按 run identity、resume segment、update 显式核对。

## 9M 训练系统的实际行为

输入文件及完整 SHA256 见 `summary.json`；原文件为 `runs/9m/` 下的 pt/out/err。
err 只有 PyTorch 缺 NumPy 警告，没有实际异常 traceback。日志结尾没有 completion/final-evaluation 记录，
不能据此判断服务器作业最后是完成、仍运行还是截取尚未结束。

| checkpoint 字段 | 实际值/核验 |
| --- | --- |
| schema / profile | `sls-full-run-ppo-v5` / `IRONCLAD_A0_FULLRUN` version3 |
| update / steps | 733 / 9,007,104；733×256×48 完全相等 |
| workers / shards | 48 / 16；环境列表48项，memory为[48,256] |
| episodes | 45,083；累计 success15,155 + death29,928 相等 |
| next_seed | 10,045,227；不是 held-out namespace |
| 模型/optimizer/memory | 全部有限值；没有 NaN/Inf |
| native source identity | 当前源码、本地嵌入摘要、checkpoint 均为 `68aad8e2…dba9` |
| worker 恢复 | 48/48 双副本 initial Decision、一步 Transition 和一步 checkpoint 完全相等 |
| checkpoint 对应日志 | stdout 第398行 update733/steps9,007,104/episodes45,083 完全对上 |

累计15,155个 success 包含早期 Act1/Act2 curriculum，**不能当作 FullRun 胜局数**。
本日志只覆盖 update571–764，共194次更新、2,383,872 steps，来自7,004,160 steps后的恢复段。
这段的新增终止为8,718 death + 4 success，step/cycle/backend truncation均为0。
日志比本地 checkpoint 多31次 update，即380,928 steps；不可把9.388M尾行的统计赋给9.007M权重。

固定 selection seeds 始终为 `[3000000000000, 3000000001000)`：

| steps | FullRun成功 | 到达Act2 | 到达Act3 | 失败楼层中位数 | stdout行 |
| --- | --- | --- | --- | --- | --- |
| 7,004,160 baseline | 2/1000 | 751/1000 | 87/1000 | 28 | 56 |
| 8,011,776 | 0/1000 | 720/1000 | 66/1000 | 25 | 225 |
| 9,007,104 | 2/1000 | 732/1000 | 71/1000 | 25 | 398 |

4次旋转诊断评估每次256 seeds，成功数依次为1、0、0、0；Act3到达数19、13、9、27。
这些不是同一批 seeds，不能拿最后一次10.55%与固定集7.1%直接宣称变好。
没有独立 final seed set 的完成结果，本表是服务器正式 periodic/diagnostic evidence，不是最终泛化验收。

PPO update 日志无间断、无重复、无非有限 scalar；平均77.02 decisions/s、normalized entropy0.3296、
final approximate KL0.02143、value explained variance0.5578。
这些说明优化器在工作、当前运行未表现为自循环/数值崩溃，不说明学到了足够好的 FullRun 策略。
9M 固定集未达到10M配置的0.5%成功率及8% Act3到达率阈值。
9M 的 best_checkpoint_updated=false 符合当前排序：baseline同为2胜且87次Act3到达，高于9M的71次。

观察性判断：系统运行稳定但此段未显示固定评估进步。缺失输入信息、credit assignment、curriculum分布变化等
都值得后续实验，但本轮没有通过消融实验把退步归因于任一原因，不能直接调整reward或丢弃checkpoint。

## 工程整理结果与后续顺序

保留 src / native / tools / configs / tests 的主体布局。没有发现需要大规模搬迁的重复 Python 架构。
补全 repository map 的命令分组和真实产物位置；新审计放 `docs/audits/<date>`，大文件证据放本地目录。
原 `runs/9m`、`local/imports`、历史 audit/reports 位置记录为已有布局，保留稳定路径。
修正 architecture 中“所有 held-out 已被保护”和固定1,000-seed最终验收的过度描述，以及模型目录使用错误 Python 环境的示例。
未删除历史报告；它们是历史记录，不能替代当前实现验收。

native module.cpp有6,154行、GameContext.cpp有4,050行、MonsterSpecific.cpp有3,662行，绑定/快照/验证探针
和游戏规则集中，维护成本明显高。建议在独立变更中按序列化、公共投影、探针拆分 translation units；
不要在本轮仅为目录整洁改变 native digest 或删除仍参与全量构建的旧搜索代码。

建议后续工程顺序：

1. F01/F03 输入语义设计及等价/非等价测试，确定新 schema 与模型迁移边界。
2. F02 native 单独修复并核对原版规则，增加组合场景回归；完成新的服务器资格验证。
3. F04/F09 收紧学习代码/种子契约；显式处理旧 checkpoint 的兼容性。
4. 分离幕通关统计与实际 Boss 对战统计，修正 KL 命名、Slurm profile 参数和评分文件事务性。
5. 之后才根据正式实验决定是否延长训练、改变 curriculum 或 reward。

## 验证与复现

- 修改前现有测试：326 passed，23.80s。
- 新增的首批7个回归在基线源码上全部失败（两项 CLI + 五项 Boss ID），证明旧测试没有覆盖问题。
- 修复后全套：335 passed，16.44s；ruff通过；git diff --check通过。
- 全套包含小模型/微型训练测试，不加载9M权重做模型 evaluation。
- 48 worker检查、模型/optimizer有限性、日志聚合、输入碰撞和native卡牌复现统一由 `collect_evidence.py` 生成。

```powershell
conda activate DL
python docs/audits/2026-09-05/collect_evidence.py
python -m pytest -q
python -m ruff check .
```

读取的 checkpoint 必须是本项目可信的训练产物，加载方式与项目恢复工具一致。
收集器只输出 `local/audits/repository-20260905/evidence.json`；不会训练、评估策略或改写输入文件。
本地补充证据包括 `baseline-regressions.txt` 和 `blood-for-blood-bytecode.txt`。

限制：没有重跑全量原版场景、Linux sanitizer、新A100 preflight或9M CUDA训练更新。
已有内容执行测试主要证明“进入动作管线/初始化”，部分 audit 测试只检查比较器或解析器；
不能把335项测试通过解释成所有牌×遗物×药水×怪物×事件×升阶组合都与原版一致。
本报告明确保留未修复项，**审计交付完成不等于项目已无缺陷或可无条件延长训练**。
