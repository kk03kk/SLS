# A20 Act2 分层差分核验

当前状态：**本地验收完成，准备提交修复版有限pilot**。最新启动设计及验证见[TRAINING_READY.md](TRAINING_READY.md)、`training-ready-r34.json`。用户已授权完成全部准备并提交main；尚未运行新的NUS训练。

当前Native r30：`b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f`。72受控重放通过，5条正常production全程匹配。Discovery新增独立stock计时证据15次，对应条件重放187边界全程匹配；无条件差异仍保留。其他自然覆盖及更广机制未验证，原严格全范围门禁不改。

编码优化已验证全部输入/模型输出/固定rollout/PPO更新一致，小型Windows端到端耗时减少约3.4%，不是NUS收益承诺。下一实验以新环境λ=.98/1匹配对照，各4M；一次48h同节点串行作业，预算、恢复、来源与健康门禁通过才训练。旧pilot不充当严格control，最终保留集封存。

## 身份及原始证据

- Stock JAR SHA256：`cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`。
- 原版 `javap -c -p` 输出、JAR、完整载荷、日志及备份只保存在
  `local/audits/act2-qualification-20261006/` 和 launcher 的恢复目录，不上传。
- 项目自有不可变场景清单：`native/oracle/resources/spirecomm/parity/act2-scenes.json`。
  24 个义务各占三个 seeds：`[131100000, 131100072)`；与最终保留集分离。
- 每个 stock class 与 disassembly 的摘要写入清单。来源为 stock 字节码，
  native 只用于执行和比较，不提供正确答案。
- 生产 Oracle 默认不开放 scenario 命令或隐藏 RNG；`parity_act2` 仅在 validation 开放。
- 新增独立 `_stock_direct` 投影直接读取 stock 对象；原有 adapter 投影仍一起比较。

## 首批义务与原版审核定位

| 义务 | 原版方法 | 比较重点 |
|---|---|---|
| Book 单刺、多刺增长、格挡/Wound | BookOfStabbing constructor/takeTurn/getMove；PainfulStabsPower.onInflictDamage | A3+ 单刺24、多刺7；刺数更新；穿透伤害才生成 Wound |
| Byrd 击落、药水、目标独立 | Byrd constructor/changeState/getMove；FlightPower.atDamageFinalReceive/onAttacked/onRemove | A17+ Flight4；伤害类型；击落；各目标计数 |
| Parasite 护甲、Fungi 死亡、AoE 顺序 | ShelledParasite；PlatedArmorPower.wasHPLost/onRemove；FungiBeast；SporeCloudPower.onDeath | 每次合格 HP 损失扣护甲；死亡 Vulnerable 回调顺序 |
| Slavers 开场、Entangle、Taskmaster | SlaverBlue/SlaverRed.takeTurn/getMove；EntanglePower；Taskmaster.takeTurn | 开场 intent；攻击 mask 与持续时间；A18+ 每次攻击加1力量 |
| Snake Plant 单次、多次、跨回合 | SnakePlant.usePreBattleAction；MalleablePower.onAttacked/atEndOfTurn | 非致死 NORMAL 攻击触发；本轮增长；怪物回合结束重置 |
| Champ 两个半血边界 | Champ.getMove/takeTurn | HP严格小于maxHP/2；Anger、Execute及力量 |
| Collector 召唤及循环 | TheCollector.takeTurn/getMove | 召唤位置、增益、mega debuff与持续时间 |
| Automaton 双循环及 Stasis | BronzeAutomaton.takeTurn/getMove；BronzeOrb | A19+ Hyper Beam后跳过stun；回合计数；Stasis抽牌及随机流 |
| 跨幕、战后领取、恢复 | AbstractDungeon/GameActionManager/AbstractRoom | 状态延续、实际领取时点、序列与RNG；尚待系统场景补齐 |

不得把相同测试结果扩大为整个敌人或全部 Act2 认证。原24项清单保持不可变；
三项系统义务的初态、冻结动作请求和比较边界由 `system-scenes-v2.json` 补充。
v1请求及失败载荷保留，不覆盖。系统义务未全部通过，尚不能称72次运行已完成。

## 已确认的规则及恢复问题

| 根因 | 独立stock证据 / 修复 | 回归入口 |
|---|---|---|
| Parasite重复Fell重掷 | seed131100022边界2：同AI RNG，stock Suck18、native双击10×2；replacement roll必须替代原roll | `test_act2_parasite_reroll.py` |
| Blue Slaver A17+连续Rake | seed131100034边界2：同AI RNG，stock Stab19、native Rake12；A17+只检查上一动作 | `test_blue_slaver_rake_limit.py` |
| Automaton开场恢复丢召唤槽 | seed131100057边界1：native恢复后右Orb不在公共状态/mask；恢复必须保留三槽容量 | `test_automaton_checkpoint_slots.py` |
| Calling Bell Boss奖励RNG | seed131100064边界145：stock card counter91、native82；BossTreasure会构造再丢弃普通卡奖励 | `test_calling_bell_boss_rng.py` |
| Boss额外奖励恢复续接 | Calling Bell后恢复checkpoint返回旧Boss地图并IndexError；stock/不中断native进入Act2 | `test_calling_bell_boss_rng.py` |
| Black Blood替换顺序 | seed131100065边界164：stock原位替换Burning Blood，native删旧并追加；修复为保留位置 | `test_black_blood_replacement_order.py` |
| Anger副本费用 | seed131100063边界190：stock副本保留Confusion后的1费，native新建为0费；使用等价副本并保留费用 | `test_anger_confusion_copy.py` |
| Blood for Blood生成预览 | seed131100067边界66：stock候选已按此前受伤降至3费，native显示4费；修复预览初始化 | `test_blood_generated_preview.py` |
| Sozu购买药水 | seed131100068边界191：stock购买函数直接返回，native仍提供购买并扣钱；修复可用性 | `test_sozu_shop_actions.py` |
| Smoke Bomb遗物回调 | seed131100063边界255：stock逃跑后Burning Blood回至7HP，native保持1HP；补齐原版胜利回调 | `test_smoke_victory_heal.py` |
| Empty Cage部分选择 | seed131100069选第1张后，stock保持19个候选并允许取消，native减至18且不显示已选牌；修复可逆选择、已选deck identity和恢复契约 | `test_deck_grid_selection.py` |
| We Meet Again地图边界 | production seed8000011000000边界51：stock地图仍禁止药水使用/丢弃，native提前恢复；锁定保持至进入下一房间 | `test_we_meet_potion_boundary.py` |
| Magic Flower战后回血 | production seed8000011000002边界153：stock Burning Blood回血9点，native只回6点；战斗结束仍是COMBAT阶段，奇数回血按stock向最近整数取整 | `test_magic_flower_victory.py` |
| 自伤死亡清理格挡 | production seed8000011000001终止边界199：stock清零，native保留8block；修正真正死亡后的清理，复活分支不改 | `test_death_block_cleanup.py` |

原版原始载荷、旧native失败差分、修复前失败测试和旧DLL均保留本地。
Calling Bell/Black Blood测试夹具只含项目自有native状态及紧凑stock预期，不包含原版源码。
上述修复没有改模型形状、encoding v5、reward或PPO，但生产行为/source身份发生变化。
完整迁移与恢复约束见 `semantics-migration.json`；禁止自动把旧来源白名单化。

补充：角斗场同房间随机流、召唤Mad Gremlin的Angry与Wizard目标排序、宝箱偷金字段序列化已修复并通过局部回归。原版奖励批次r10在r18重放中，seed131100067/68全程匹配155/293边界；seed131100066匹配248边界但脚本提前停止，仍不记通过。后续完整重采集运行中。

## 原版桥接与身份差异

- Singing Bowl：stock通信接口公开的是追加在卡片列表后的`choose`选项，未公开`bowl`命令。
  已修复父奖励页命令展开及独立卡奖励页mask，保留旧接口兼容。
- production选牌/死亡收尾：读取公共`game_state.action_phase`/`screen_type`，不依赖
  validation专用continuation。故障批次不计通过，存档恢复有独立记录。
- 连续Discovery：两瓶药水连续创建生成卡选择时，stock完成第一选择并打开第二个
  CARD_REWARD，但CommunicationMod没有报告同屏内容变化。现场对象/线程栈保留。
  Oracle1.2.1仅给桥接层发送新选择通知，Python按新stock卡实例组识别独立决策；
  不设置伤害、选项、debuff或RNG。运行复核尚在进行，不能以静态解释直接认证。
- Armaments候选ID偏移：按完整有序选项及属性进行UI身份映射；实测15个
  PolicyBatch字段逐张量相同，见 `choice-identity-proof.json`。费用、HP、升级、mask
  和RNG均不归一化，扰动测试证明实质变化仍会被发现。
- Collector/Automaton公共目标索引与native预留内部槽不同：通过完整公共怪物列表
  的instance slot映射；不按content ID合并同名怪物，不丢死亡目标。

`sls-original-choice-public-boundary-v7`记录桥接执行契约；它不授权绕过native来源检查。
validation的`CardGroup.getRandomCard(false)`确定性补丁会影响Stasis等分支，相关通过项
仅具有受控模式资格；未覆盖的production随机分支仍未验证。

Discovery的原版动作在检索动画每帧生成候选，实测14与15次更新都会出现。
受控重放使用stock独立计数提供的显式动画输入，同时完整比较RNG；adapter声明必须
与stock计数一致。这只取得**给定stock计时输入的受控资格**，没有修改生产/训练默认值，
不能外推为production随机流认证。自然轨迹重放禁止注入这些validation参数。

Empty Cage等多选GRID保留全部候选及取消选择能力；既有observation v2中的
`selected_cards`和encoding v5已有的`deck_index`现在在此处正确填入。
模型形状和action vocabulary未变，但观察数值/动作语义已变，不能宣称旧训练严格可比。
新checkpoint使用`sls-stock-grid-toggle-v2`，旧部分选择状态不自动转换。

**新增边界发现：** stock构造器的实际`monster.intent`可能仍为DEBUG，但Oracle的move投影已显示攻击。Spot Weakness读取实际intent，立即出牌会改变结果。v6等待实际字段初始化；此前三条production与跨幕stock捕获包含该状态，必须重采集，旧“匹配”报告仅保留为诊断证据。

## 工具首次分歧及分类

1. legacy native encounter probe 固定 A0，旧比较仅开场与第一轮：已确认工具缺口。
   新接口显式接收 ascension/act/floor，旧三参数调用仍是 A0。
2. 第一轮 harness：Book 的开场与三个回合匹配；Byrd 使用错误遭遇ID导致
   stock执行故障及超时，不能计PASS。已补启动前 allowlist 校验。
3. r2 固定场景：Book 下回合出现能量与抽牌顺序差异。定位为 Oracle 初态错误：
   `EnergyManager.energy` 是每回合补充量，`EnergyPanel` 才是当前可用能量；
   drawPile 构造顺序也反了。原始 r2 载荷未覆盖，修复在 r3 Oracle。
4. 差分器通过伤害、合法动作 mask、power持续时间、RNG推进四种扰动测试。
   native合法动作直接取实际 `_legal_actions`，避免只用同一adapter重算而漏掉mask错误。
5. r3药水初态使用slot=-1导致stock执行故障；改为`obtainPotion(0, potion)`后重跑。
   r4强制Entangle的初始intent误写DEBUFF；stock字节码为STRONG_DEBUFF，r5三seed
   重跑匹配，旧r4三行仅作为harness失败证据，显式由新批次替代。

## 复算入口

使用 Windows DL Python，设置 `PYTHONPATH=src;.`。

```powershell
python tools/capture_act2_stock_sources.py --stock-jar <合法JAR> --javap <javap.exe> --output-dir <新本地目录>
python tools/run_act2_encounter_batch.py --oracle <当前源码构建的Oracle> --manifest native/oracle/resources/spirecomm/parity/act2-scenes.json --scenes <清单ID...> --output <新载荷.json>
python tools/replay_act2_encounter_batch.py <载荷.json> --oracle-build <对应.build.json> --output <新差分.json>
python tools/replay_act2_production_trajectory.py <正常Neow生产轨迹.jsonl> --output <新首次分歧.json>
python tools/run_act2_flow_batch.py validation --oracle <Oracle1.2.1> --scripts <冻结脚本目录> --seeds <清单seed...> --output <新载荷.json>
python tools/run_act2_flow_batch.py validation --oracle <Oracle1.2.1> --artifact <冻结90M Act1导出> --seeds <固定系统seed...> --output <新载荷.json>
python tools/run_act2_flow_batch.py production --oracle <Oracle1.2.1> --artifact <冻结90M Act1导出> --seeds <已冻结8条之一...> --output <新载荷.json>
```

批次最多30分钟；执行失败不能认证。每次启动拒绝已有游戏进程，备份和恢复存档、配置与mods，
并校验恢复哈希。原始文件使用新路径，禁止覆盖。

## 来源变化与迁移

native probe接口增加上下文导致source digest变化，必须使用新构建。
已保留旧DLL，并在隔离进程中比较 A0/A20、四seed各最多256步，共1381个
生产边界：旧/新完整状态与实际合法动作逐字节一致。
该证据只支持本次测试接口扩展的来源转换，不证明全部Act2规则。
后续任何规则修复必须另外登记，不能沿用本次转换资格。

若发现并修复生产环境差异，旧λ=.98 pilot不再是新环境下λ实验的严格control。
当前恢复原定72次/8条有限验收，不能把缺失证据改成通过。PILOT_GATE.md只保留此前赶时间的历史决策，已由ACTIVE_SCOPE.md取代。

## 自然轨迹与验证边界

冻结90M模型SHA `ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c`。
诊断区间 `[8000011000000,8000011000128)` 的旧版扫描及8条选择保存在
`production-selection.json`；修复改变了轨迹，旧选择不能绑定当前source。r22的128-seed扫描及`production-selection-r22.json`也因随后Nob意图修复而成为历史记录，当前Native r23已重扫，选择保存在`production-selection-r23.json`。扫描只为选覆盖，不估胜率，
不挑checkpoint，不使用最终保留集。production显式Act2 horizon、模型来源仍为Act1。

暂停前 r15 完整Python测试为1138 passed / 1 skipped / 4 warnings，耗时117.69秒（已有来源重绑定警告）；
ruff、27/27配置检查及diff空白检查通过；AGENTS.md哈希保持不变。
真实游戏验收仍须完成，测试通过本身不打开训练门禁。
原版文件/反编译输出/完整日志不提交；本地raw证据供复算，GitHub仅项目代码、摘要和入口。

多选GRID撤销至0张仍是玩家边界：原版r11在seed131100069返回2张需求、空selected_cards，v6误等待失败；v7已修复并以相同脚本重采集。失败批次不能计通过。
