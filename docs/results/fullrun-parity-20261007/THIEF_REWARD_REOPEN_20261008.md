# 新开启的 Act1/Act2 义务：盗贼全部逃跑后的药水奖励

状态（最新）：**已取得原版运行证据并修复药水概率缺陷；15 次真实 TheCity 奖励邻接验收完成**。

下文保留重新开启时的历史过程；最新收尾见 [ACT12_REWARD_CLOSEOUT_20261008.md](ACT12_REWARD_CLOSEOUT_20261008.md)，不得将历史“未编译／未运行”作为当前状态。
这是新证据引发的单项重新开启，不删除或改写此前有界校验结果。

## 独立来源与调用链

重新计算本机合法游戏JAR、AbstractRoom.class和缓存javap摘要，均与
缓存identity一致。AbstractRoom.addPotionToRewards 字节码偏移0将chance
初始化为0；MonsterRoom分支在31调用haveMonstersEscaped，在34为true时
直接跳到65，不执行37–44的40+blizzardPotionMod。随后White Beast Statue
覆盖为100，奖励数>=4再覆盖为0，仍消耗一次potionRng概率抽取。

MonsterGroup.haveMonstersEscaped仅在所有monster.escaped为true时返回true；
死亡不等于逃跑。因此两个盗贼全逃跑与一个被击杀、一个逃跑必须分别测。
MonsterRoomBoss继承MonsterRoom，普通Boss胜利并不属于这个全逃跑分支；
不能根据类名误推Boss药水概率也必须为0。

native BattleContext.exitBattle 已识别所有盗贼逃跑并设置
info.suppressCombatGold；GameContext.createCombatReward据此跳过金币，
但随后仍调用addPotionRewards。后者直接以40+potionChance开始，没有
全逃跑条件。当前工作区与旧已提交源码都包含这个分支。

在没有White Beast Statue、奖励不足4项、potionChance=0时，stock抽取
0..99后必不产药水，保底计数增加10；native抽到0..39可能生成药水并
减少10，同时额外推进药水生成随机流。剩余roll可暂时碰巧一致，不能
以三个seed恰好没掉药水认定此分支正确。

烟雾弹是另一条路径：原版monster.escaped不因此全为true，仍执行奖励
构造及胜利遗物回调后隐藏奖励。已有Smoke/Burning Blood证据支持该路径；
修复盗贼分支不能把所有“逃跑”统一成同一种无奖励／无回调行为。

## 最小运行义务与邻接分支

先保存旧native输出，再取得stock原始对象／奖励／RNG／保底计数读数。
受控A20 Act2 TWO_THIEVES场景仅设置初始条件，逃跑由正常动作循环执行。

| 义务 | 必须验证 |
| --- | --- |
| 两盗贼全逃跑、无特殊遗物、保底0 | 无金币、无药水、卡牌仍存在、概率抽取仍推进、保底+10 |
| 两盗贼全逃跑、正药水保底 | 基础chance仍是0，不能加入保底 |
| 两盗贼全逃跑、White Beast Statue | 覆盖为100，药水生成及后续RNG一致 |
| 一个被杀、另一个逃跑 | 不进入全逃跑分支；已追回金币与正常掉落一致 |
| 两盗贼被杀 | 原正常奖励流程不回归 |
| Smoke Bomb离开普通战 | 保留已有构造／隐藏奖励／治疗回调行为 |

新seed尚未冻结或运行：项目场景已使用131200000–131200179；分配前还需
核对本地原始记录，不能仅根据文件名或资源清单断言无冲突。
对照入口还需支持战后奖励稳定边界；现有战斗差分器拒绝胜利边界，
不能借用只比较战斗前缀的HARNESS_MATCH声称本义务通过。

## 训练关系与本地限制

本轮仅源码／标准库证据校验，没有native执行、游戏启动、模型、GPU或
构建。旧实现原件与摘要保存
`local/audits/act12-thief-rewards-20261008/static-evidence-r1.json`。
没有生产修复、契约迁移、commit/push，也不修改固定服务器checkout。

这一分支影响Act1/Act2奖励和RNG，应在后续训练资格中显式登记为待处理，
不能再笼统宣称“没有新发现的Act1/Act2规则问题”。运行差分、修复回归
及新来源绑定完成之前，不将新实现自动带入训练；也不把该问题或未来
修复的收益归因于critic预热。

## 纯Python字节码复算进展

新增`stock_potion_reward.py`按缓存javap的实际偏移执行概率前缀0–93，
不使用native生成预期。未知指令、缺失方法或错误初态拒绝。入口
`tools/audit_stock_potion_reward_prefix.py`先核验合法JAR、class和javap摘要，
再记录各条件下的概率及0..99中满足条件的抽取数量。

本地实际复算400种结构条件，15项针对性轻量检查通过，Ruff通过。
结果为`local/audits/act12-thief-rewards-20261008/stock-probability-prefix-r1.json`。
其中无法自然出现的全逃跑精英/Boss组合只是类分支检查，不算可达性证据。
该复算不执行RNG、药水生成、逃跑／死亡动作或房间流程，不能替代运行差分。

下一步仍须补完整战后奖励边界的受控采集和重放。不能修正概率前缀后
就跳过RNG、保底、卡牌奖励及Smoke邻接分支验收。

## 战后独立证据接口准备

Oracle源码新增validation-only `_stock_reward_state`，直接读取房间实际类、
phase、smoked、全部逃跑标志、blizzardPotionMod，以及room/screen两份奖励
列表。列表保留类型、金币及bonus、done/ignored、药水、遗物和卡牌升级/misc。
公共策略输入仍不使用该诊断字段；production入口和smoke检查会拒绝泄漏。

诊断身份升级至`spirecomm-parity-v12`，Mod版本1.3.23；旧v11历史证据继续
按旧身份保留。Live入口支持两个明确诊断版本并补拒绝隐藏字段，未增加
训练checkpoint兼容白名单。**新Oracle尚未编译、安装或启动**，不能将源码
准备完成算为Java构建或真实游戏验收通过。既有sealed r25不可声称包含此接口。

相关纯Python/mock测试累计51项通过，包括新旧production诊断版本、隐藏
字段拒绝、资源版本检查及字节码分支；对应Python Ruff通过。Java仅核对
合法JAR对象字段声明，未执行编译或游戏。没有native生产规则修复。
