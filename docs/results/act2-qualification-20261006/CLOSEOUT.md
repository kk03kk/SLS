# 优先级1有限收尾总结

2026-10-07，按用户要求结束本轮优先级1，并暂停刚开始的优先级2。这里的结束指缩减后的有限审核范围完成，不是全部Act2保真认证。

## 本轮结论

- 当前Native来源：`b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f`。
- 首批24个义务、72个固定seed受控运行，按该来源全部重放通过。机器汇总：`bounded-closeout-r30.json`。
- 已采集6条原版production正常Neow开局完整轨迹，其中5条严格逐边界匹配，长度为200、450、216、378、397。另一条首次分歧在边界124，尚未获得Discovery计时的独立实测证据，不算通过。
- 自动检查原版实际覆盖，不把native预选结果冒充stock覆盖。Automaton/Collector自然轨迹未采集，部分预选普通战死亡实际属于事件房。原定8轨迹严格认证仍为NOT_QUALIFIED。
- 优先级1最终完整Python测试：1183通过、1跳过、4条checkpoint runtime/provenance测试警告。日志：`local/audits/act2-qualification-20261006/full-tests-r30.log`。跳过的是旧策略被当前encoding正确拒绝的历史诊断测试。
- Native构建、配置检查27/27、定点修复回归通过。63个受保护存档/配置/Mod文件的恢复哈希一致，无游戏进程。AGENTS.md未修改。
- 没有新增服务器训练，没有证明联合通关率提高。本地更改尚未commit/push，当前不是新的NUS提交入口。

## 工作为何不止检查几种敌人

实现了A20、Act/floor上下文、多决策稳定边界、同语义动作重放和首次分歧定位；核对公共状态、合法动作、直接stock数值，以及validation模式可审计的RNG和checkpoint续接。正常production模式与validation干预隔离。保留构建、stock JAR和原版字节码身份；原版JAR/字节码/完整日志留在本地。

24个义务覆盖Book、Byrds、Parasite+Fungi、Slavers、Snake Plant、三个Act2 Boss及跨幕/奖励/恢复。战斗63次，系统9次。系统场景使用stock冻结90M策略产生动作，native只重放；未触发分支单列，例如seed131100067在Act1死亡，不宣称它验证了Act2奖励。

静态看似正确不能替代动态证据。修复某个首次分歧后，才可能到达先前被遮挡的后续分歧。本轮确实在完整正常局里继续发现了永久牌组同步、荆棘杀敌后的连击取消和遭遇池重抽错误，这些会影响策略输入、HP或后续随机序列。

## 主要修复

1. 战斗规则与时序：Parasite动作替换重抽、A17+蓝奴隶Rake限制、Mad Gremlin召唤后的Angry、Gremlin Leader目标顺序、Plated Armor队尾扣层、Mystic死亡后的Centurion自我格挡、Byrd死亡后取消余下NORMAL攻击、Hexaghost升级既有Burn并加入新Burn。
2. 牌与公共信息：Confusion下Anger复制费用、Blood for Blood生成预览费用、Ritual Dagger击杀后立即同步永久牌组与同身份战斗实例、角斗场Nob攻击降益意图、Black Blood替换遗物位置。
3. 奖励/资源/流程：Calling Bell额外奖励随机消耗与稀有卡计数、角斗场第一战弃置奖励及第二战实际奖励、同房间随机流、Sozu购买药水行为、Smoke Bomb胜利回调、Magic Flower治疗取整与战斗退出时机、事件药水锁持续范围、真实死亡清空格挡。
4. 恢复与选择：多选GRID候选/撤销/永久牌组身份、Automaton多怪恢复、Boss额外奖励续接、无关stolen-gold元数据、validation稀疏计时输入随动作历史保存并严格恢复。
5. 遭遇生成：TheCity首强敌排除规则和拒绝候选后的monsterRng消耗。Spheric Guardian排除Sentry+Sphere；3Byrds排除Chosen+Byrds；Chosen排除两个含Chosen组合。

详细分类和迁移在`semantics-migration.json`；修复前失败日志索引在`first-divergence-evidence.json`。这些包含规则、投影、恢复及核验工具修复，不能简单称为同等严重的若干模拟器规则bug。

## 纠正与未验证范围

护甲差异实际是Headbutt选牌边界前的队列时序，不是之前猜测的Fire Breathing伤害规则。Spot Weakness相关问题需区分Oracle尚未稳定的意图投影与native卡牌规则。奖励卡牌预览不能仅凭显示就判为泄漏，须核验原版免费可达的UI信息。Smoke Bomb也已有处理，本轮修的是具体回调差异。

DiscoveryAction在动画更新中重复生成候选并消耗cardRandomRng，源码支持production后续随机卡差异的计时解释；但旧捕获没有独立更新次数记录，因此该解释仍是尚未运行确认的假设。已准备Oracle1.2.2被动时钟见证与单独条件诊断工具，尚未采集其production运行证据。条件匹配不会升级为无条件production通过。

未覆盖的敌人组合、遗物/药水交互、事件、随机分支和另外两种Boss自然轨迹继续保持未验证。Act3/Heart未审核。本次有限收尾接受这些明确边界，不降低旧严格认证器的要求。

## 对下一训练的影响

保真度工作没有改PPO、reward、网络结构或正常Neow开局分布。模型shape、encoding和action词表保持，但规则及部分策略可见数值改变，Native来源与恢复契约已记录，不能自动将旧训练状态按新环境精确续接。

旧λ=.98 pilot不能充当修复版λ实验的严格单变量control。下一次仍以正常开局Act1+Act2联合通关率为目标；训练配方、同环境control及父模型权重迁移需另行准备。90M父模型保留Act1训练来源，不能称为Act2训练模型。

## 优先级2暂停点

已开始固定原版输入的CPU编码profiling：64决策基线编码266.75ms、padding9.98ms；交替原型比较编码+padding250.64→172.98ms，96样本全部编码字段一致。该测量不是端到端训练吞吐。

留下单项NumPy→CPU tensor转换初步改动、显式model依赖与固定输入/模型输出/Adam更新等价测试。尚未完成最终完整测试、固定rollout等价、端到端墙钟或NUS验证。按用户要求此处暂停，不将其登记为已验收的吞吐优化，不继续训练准备或服务器提交。
