# 项目架构

## A20 校验清单与重验边界

`sls.audit.validation_scope`与`tools.prepare_a20_validation_scope`负责保留完整候选分母，冻结游戏JAR、Oracle源码/成员/二进制、native源码/实际加载产物及比较器文件身份，将原版类方法、显式字节码分派点、native引用、历史证据和缺分支关联到审查清单。静态类映射、旧FIXED_REGRESSION标签、受控注入及仅资源子集比较均不授予正常流程可达性或规则资格。可达性见证只索引原版正常轨迹中持有或遭遇的内容，商店展示不等于获取；未确认项目继续留在分母。字节码索引不覆盖隐式异常边、间接动作调用或所有交互组合，这些仍由来源分支审查义务约束。

`tools.revalidate_act12_simulator`仅离线重放已有原版记录，复用严格比较器并逐份保存原始结果，独立于PPO/训练准备门。无条件、独立计时条件和不完整前缀分别报告；工具运行完成不等于资格通过。当前输出显式NOT_QUALIFIED，完整心脏终止清理、按幕恢复/决策门槛和正常资源路线仍需独立证据。历史采集Oracle与当前Oracle分别冻结，不能重标成同一来源。

## Act1–2 时钟合同与训练起点

全幕审计保留原始尸体能力差异：原版 `AbstractMonster.updateDeathAnimation` 在 deathTimer结束时才清空powers，模拟器没有相同渲染时钟。已死/逃离怪物的能力不进入公共Observation的powers集合；原始对象列表仍保留在差异证据中。该分类只适用于已证明HP为0、原版dying=true、其余已比较字段/动作/RNG一致的死亡动画残留，不能用于忽略活怪能力、Darkling半死或Awakened One复活机制。受控比较投影不是全部Observation字段或v6预测等价证明。

隔离 `LightspeedBattle` 的死亡checkpoint现在保留可选的私有 `terminal_display_moves`：原版死亡帧可能已经滚出下一招，但仍显示上一招意图，恢复必须保留这两种状态。旧checkpoint缺少该字段时沿用旧行为，无法补回当时未保存的显示状态；完整Run原已有独立的终止显示字段。本轮来源变化仍须重新验收，不能因此放行旧训练来源。

动作排序还须分别核查内部未来招式与显示字段：原版 `setMove` 更新私有 `EnemyMoveInfo.move`，`AbstractMonster.nextMove` 可能直到 `createIntent` 才更新；Oracle反射的 `_monster_intents.next_move` 对应前者，独立 `_stock_direct.next_move` 对应后者。模拟器 `move_id` 比较内部招式，公开意图比较显示状态。盾的普通攻击、矛的灼烧攻击、心脏多段/大攻击的后续招式切换和AI随机数均入队在伤害后；致死时不执行。已经完成出招的前一只敌人仍可保留新内部招式与旧显示意图，不能把所有怪物的终止招式一律还原。

生产构建的战斗抽牌/弃牌/消耗牌堆使用动态vector，不能套用可选fixed-list构建的64项容量。Violence与Stasis临时选牌列表也使用动态容器，顺序和RNG保持不变；审计输入仍限制手牌10张及16位有符号卡牌身份范围。永久Deck、匹配/升级/选牌候选及战斗初始化索引/先天牌标记也按实际大小存储，取消96张假设。战斗卡牌身份仍为16位有符号整数，超范围须明确失败，不宣称无限牌组已经验收。

永久牌组选牌的GameAction使用独立完整索引解码，候选bits、公开idx1与执行映射一致，小于256的旧bits不变；其它动作域保留原有字段布局，不把GRID的大索引误解为idx2。篝火升级/Peace Pipe删牌的返回地图回调通过私有 `sls-rest-grid-map-v1` 明确登记，才允许直接保存恢复；类型/房间不匹配时拒绝。其它GRID仍遵守历史重放合同，不自动把完整选牌对象误当成完整回调恢复资格。这些native来源变化不豁免旧训练模型或入口库来源门。

FlightPower非致死普通攻击的减层在伤害后入队，头槌弃牌选择可先于减层暴露状态；移除最后一层又入队GROUNDED招式变化，死敌不执行。不能把这个顺序泛化到所有反应能力：原版Malleable对怪物的格挡入队，但能力数值即时增加；既有Plated Armor减层队列保留。

蒙面强盗与神秘球体的共享事件初始化执行敌人构造阶段的HP RNG消费，才暴露事件选项；不执行意图掷骰或开战能力。战斗初始化从该楼层初始HP流重建同一群体，最终状态与事件已消费状态一致，不额外消费第二遍。该规则由生产和审计探针共用，禁止只在探针补随机数而遗漏正常入口。其它事件的构造时机逐类依据原版，不全局提前。

隔离Battle checkpoint的master_deck和gold属于恢复资源，不能由reset默认开局代替；永久牌组有瓶装牌时额外保存私有 `master_deck_bottle_indices`，默认无瓶装时省略以保持历史快照形状。负升级数、不可准确恢复的升级/misc组合及悬空瓶装索引明确失败；完整run仍使用既有GameContext与BattleContext恢复。心脏受控终止资源比较使用独立原版房间/幕数/怪物死亡/玩家HP证据和无动作边界，终止资源通过不表示全部清理字段或正常全局轨迹通过。新native规则来源与旧训练来源不同，旧checkpoint/入口库来源门保持拒绝。

宝箱奖励按原版回调顺序建立：套娃额外遗物先加入，主遗物与蓝钥匙建立关联，再由饥饿脸移除第一件遗物。移除主遗物时同时移除蓝钥匙；移除套娃额外遗物时保留主遗物与钥匙。该修复不改变奖励生成的RNG调用，也不把金币作为钥匙的关联奖励。

当前native的Discovery使用固定14次retrieval update；原版受渲染时钟影响，独立探针已观察到15次。因此同seed不意味着跨后端的Discovery后续卡牌RNG和选项逐项相同。训练环境采用`DISCOVERY_FIXED_RETRIEVAL_UPDATES_14_V1`，不是对原版全部时钟轨迹的无条件复刻。验收分别保留无条件差异及原版独立计时条件下的完整回放；条件通过不能登记为无条件一致。不完整前缀、未解释差异或计时证据缺失仍阻断资格。该合同未把时钟、RNG或隐藏状态加入策略输入，未改原版源文件或历史checkpoint身份。

真实第二幕入口课程只从正常A20开局合法动作前缀取得状态，保留实际资源，所有到达入口的样本均纳入，不按未来胜负筛选。每个辅助入口是一个新episode：当前策略记忆、上一动作和上一奖励重置；旧策略的动作、log probability和收益不进入PPO更新。正式评估仍全部正常开局。入口库绑定native/profile/SHA，保存恢复同时绑定课程计数和各worker当前起点类型；50%指episode启动比例，不表示两幕决策数相等。入口库变更或比例调整须登记新合同，不能当作原作业的精确恢复。

SLS 的公共边界是：

```text
公开 Observation + 合法语义 Action 候选
    → 结构化编码、关系注意力、GRU 记忆
    → 策略概率与状态价值
    → Backend.step(Action)
    → 下一决策、奖励和终止信息
```

策略不读取 native RNG、隐藏抽牌顺序或模拟器内部恢复状态。原版游戏与模拟器共享公开观测和语义动作协议；验证用诊断信息独立于策略输入。

## 实现分工

实机 Visual Inspector 继续使用 `AgentRuntime` / `InteractiveAgentRuntime`：一个控制线程负责评分、INTENT、原版执行及 ACK；HTTP 线程只读取快照或排队控制命令。评分不提交 GRU，动作事务沿用已有确认与 journal，暂停不打断已发送动作。CLI `play_live.py` 与网页共享该执行核心；进程级 OS 文件锁防止第二个 live 控制器取得动作通道。

`runtime/visual.py` 是独立的只读展示投影。地图节点和边直接取原版适配后的公开 Observation；当前位置、Seed、公开事件文字、回合等仅从原版载荷白名单读取，不能回流到策略编码。Boss 展示节点只补齐原版已公开边的目标，不生成地图。实际路线由确认状态中的位置记录，不由选择动作预测。`VISUAL_ACK` 保存完整动作评分、选择、前后公开状态和实际库存变化；历史分页与回顾不执行动作。原版中间动画不作为新的策略决策。

React/TypeScript/SVG 前端与现有 loopback HTTP 服务共用一个端口，默认 `127.0.0.1:8765`，约300ms读取已确认快照；不需要第二个 Vite/FastAPI 服务。`/api/state`、`/api/history` 是读接口，`/api/control` 排队命令；单步/自动携带当前边界标识，重复或过期的边界命令被拒绝。静态产物放在 `runtime/web/dist/` 并随安装包提供；旧界面仍可从 `/legacy` 打开。

| 路径 | 职责 |
| --- | --- |
| `src/sls/contracts/` | Observation、Action、Decision、Transition 与验证协议 |
| `src/sls/content/`、`curriculum.py` | 内容登记、角色/难度、幕目标与终止判据 |
| `src/sls/backends/simulator/` | C++ 引擎的 Python 适配 |
| `native/simulator/` | 战斗、地图、奖励、事件、RNG 与状态序列化 |
| `src/sls/backends/original/`、`native/oracle/` | CommunicationMod 接入与 Java 公开观测补丁 |
| `src/sls/model/` | 结构化编码、关系注意力、GRU 与合法动作评分 |
| `src/sls/rl/` | 分片 rollout、循环 PPO、评估、checkpoint、恢复和迁移 |
| `src/sls/audit/`、`diagnostics/` | 原版差分、轨迹与策略诊断 |
| `src/sls/runtime/` | 推理产物、实机控制器与日志恢复 |

当前策略输入是 `sls-policy-input-v5`；具体字段、词表与 checkpoint 版本以源码为准。旧权重不能靠修改版本号变成兼容模型。

默认模型架构仍是 `sls-recurrent-relational-policy-v5`，旧参数布局和前向行为保留。可选研究架构 `sls-recurrent-relational-policy-v6` 使用相同v5输入张量/词表，但增加类别字段与动作引用角色的逐通道缩放，并将选项variant放入独立embedding。v5把类别词表中的OPTION编号用于内容embedding，造成选项与无关内容参数共享；v6拆开该共享。角色与字段缩放使求和前的表示能区分槽位，不代表所有语义碰撞均已消除。

后续主线讨论固定采用v6结构、以90M父权重的显式迁移作为基点；现有90M导出文件仍保留原结构和身份，尚未统一导出新的90M主模型文件。日常可称“主模型的90M基线”和“候选模型”，内部架构、输入编码、训练checkpoint及推理文件格式各有独立版本，不能批量改成相同编号或删除历史读取规则。正式第二幕配置已显式选择v6，但库级默认仍为v5，后续统一日常入口需保留旧配置解释规则。本次未修改默认值、导出模型或升为v7。

2026-10-15再次核对网络、输入和迁移：55项CPU模型检查通过（排除优化器更新、训练恢复及CUDA用例），实际90M checkpoint迁移后在4个已有诊断seed、512个合法前缀决策上，logits/value/state/next_memory最大绝对差均为0，原checkpoint字节未变；证据为`local/reports/role-encoding/current-network-review-cpu.xml`和`current-90m-migration-review.json`。这支持现有迁移实现及本批检查范围内的正确性，不证明v6胜率更高、网络最优、完整第二幕或全幕输入覆盖；当前没有据此提出v7升级。

通过 `migrate_v5_role_policy` 显式转移权重：共享参数原样复制，新缩放初始化为零，选项独立行复制旧模型实际使用的向量，因此初始预测保留；以后才能通过学习改变这些表示。新架构不可直接strict加载旧state_dict，迁移不携带Adam、环境、RNG或循环状态，不是精确续训。`tools/verify_role_model_migration.py` 验证当前模拟器合法决策上的预测与动作一致性，只生成诊断JSON，不导出权重或启动训练。

首段20M的正式配置显式选择v6，库级默认v5及旧配置保持原样。`initialize_act1_weights` 仅在 `warm_start.model_migration = "v5-to-v6-role-bindings"` 时执行角色迁移，仍检查原90M文件SHA、相邻课程、累计步数及独立模拟器来源迁移证据。首次建立新run；后续恢复加载v6完整checkpoint，不重复迁移父模型。

入口库的可移植证据 `tests/fixtures/evidence/act12-foundation/entry-prefixes.json.gz` 只保存原始合法动作前缀、每个原存档SHA及原库元数据。服务器缺库时逐项重放重建，最终压缩库必须匹配固定SHA；不是在服务器重新筛选训练状态。已有库仍按原流程核对SHA并重放全部前缀。

公开观测和编码数值必须有限，NaN/Infinity在进入模型前拒绝。当前动作metadata只支持登记的数值字段，类别字段不能被静默丢弃；新增类别动作特征须显式扩展输入合同。native战斗动作只接受已登记的0–4类型，未知类型不能降级为END_TURN。这些是输入正确性防线，不替代原版规则对照。

## 兼容性与验证边界

正常开局两幕对照使用现有v6，不改变编码v5、词表、网络输入、动作或参数布局。`PPOConfig.advantage_normalization`显式选择`domain`（兼容缺省）或`global`，随PPO配置进入严格checkpoint合同。rollout额外保存幕号、归一化探索熵和终局原因，仅作采样元数据；按幕价值误差比较旧估计与本rollout的GAE目标，不能冒称完整episode的Monte Carlo误差。共享梯度探针用`autograd.grad`和隔离的Torch RNG，在一个序列块上测未裁剪的策略/价值梯度范数及夹角；不调用Adam、不写参数或`.grad`。

`rl/value_adaptation.py`提供PPO之前的独立阶段：冻结策略、共享编码器及GRU，采集正常开局完整训练episode，缓存脱离计算图的GRU输出和实际训练奖励的完整累计回报，仅拟合独立复制的`value_head`（含LayerNorm）。采集使用专属模拟器及CPU Generator，拟合使用独立Adam；不借用正式PPO的worker、记忆、随机数或优化器。不可变episode文件由原子journal登记SHA；journal保存当前episode、限步状态、头部Adam、拟合游标、排列和私有RNG。每次模拟器调用前另保存小型交互计数，进程恢复重做步骤也计入预算。拟合批次（包括轮末验证）失败会恢复到批次开始；只有验证优于未适配基线才复制最佳价值头并原子保存正式checkpoint。

适配配置与完成摘要分别进入checkpoint合同和trainer状态；适配未完成时正式latest仍是安全父权重。`NO_ADAPTATION_GAIN`显式停止C组，不伪装成适配成功。全部新增交互按“适配＋PPO”计数；正式PPO仍使用全新Adam。`tools/prepare_act12_normalstart_study.py`只准备配置、身份、种子、CPU验证及未来命令，不启动正式训练，也不继承旧混合课程的资格。

动作引用在[B,E,D]实体张量上按展平引用gather，再恢复[B,A,5,D]输出；不展开[B,A,E,D]输入，避免反向生成动作数倍的稠密实体梯度。v5/v6参数布局、词表和前向输入合同不变，梯度累加的浮点顺序仍需按runtime验证，训练实现来源改变不能自动豁免恢复门禁。训练收尾用独立reference/selected策略实例评估，训练器权重、Adam和GRU不被替换；推理副本构造和模型导出隔离CPU RNG。故障诊断只记录上下文和原安全checkpoint身份，不把部分PPO更新发布为可恢复状态。

战斗治疗及最大生命增长显式接收BattleContext，使红头骨跨半血线的力量撤销按原版动作队列执行。负数StrengthPower仍为BUFF类型，不使用会消耗人工制品的减益路径；蜥蜴尾巴复活至少恢复1生命。这些规则修复改变native来源身份，不能直接在服务器旧checkpoint合同上精确续训。

checkpoint 包含模型、优化器、worker、RNG、循环记忆及训练合同；推理产物仅用于部署，不可代替完整续训状态。精确恢复与跨任务权重迁移是不同操作。

原版对照覆盖已记录的场景与分支。自动回归通过不代表全部种子、事件、Act3/Act4 路线已经原版一致。历史证据须同时核对源码、runtime、种子、原始载荷和适用范围。

不可变小型证据在 `tests/fixtures/evidence/`，私有原版来源和大量轨迹在 `local/audits/`。历史合同路径由 `src/sls/evidence_paths.py` 解析；该解析仍执行仓库内路径限制，证据 SHA 校验保留。

当前 Act1/Act2 课程以 native `public_run.completed_act` 与原版公开 Boss 房间 COMPLETE 判定 Boss 击败，在战斗奖励边界终止；公开幕号前进仅为补充判据。不能只阅读 `completed_act_between` 的 fallback 就登记为“进入下一幕才终止”。第二幕成功先于Boss奖励，无第三幕动作。

旧27eba742来源的Act1–2基础资格为 `PASS_CONTRACT_BOUNDED_ACT12`；当前90628cf已由独立`audit/act12_qualification.py`按同一有限72义务和8 canary重新签发，原始差异与Discovery固定14次环境合同的独立时钟条件分别登记。`rl/normalstart_release.py`验证新资格的source、比较器身份、A20正常起点、覆盖计数和原版恢复摘要；规则资格与PPO准备分别绑定，不能以旧混合训练或全803项未完成审计替代当前有限合同。历史真实第二幕入口库的SHA、轮转及记忆重置合同保留，当前三组不加载入口库，评估仍从正常开局开始。

受控差分的尸体分类保留原始不等，当前仅登记已见盾BackAttack、重用匕首Minion与爆炸者Explosive残留；独立死亡证明、双方零HP、无合法目标、其余投影和RNG全部一致才可分类，不称精确状态相等。胜利记录用显式终点比较器，限定HP/怪物/RNG子集不代表完整奖励流、清理或恢复资格；不再把没有combat_state的胜利快照送入战斗接口。

双Boss流程恢复验收同时记录快照/合法动作一致及 `restored_continuation_equal`：恢复后的副本执行下一条相同记录动作，比较完整结果状态与合法动作，再建立下一边界副本。原版免费proceed与native自动入场的无动作边界通过 `restored_continuation_action_executed=false` 单列，不能计作实际动作恢复验证；终点仍只比较明确列出的字段。
