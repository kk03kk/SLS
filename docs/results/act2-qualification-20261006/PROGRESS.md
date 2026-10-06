# 当前项目推进判断

2026-10-06 恢复优先级1工作。主目标为正常 Neow 开局 A20 Ironclad 的 Act1+Act2 联合通关率。

## 训练证据与决策

90M Act1 阶段已有独立配对改善证据；这不能外推为 Act2 能力。
随后新增4M的正常开局 Act1→Act2 pilot，1024个独立开发确认开局中，冻结父模型、固定终点、周期所选模型分别联合通关6、3、2局。
联合成功过少，尚不能证明训练使联合胜率确切下降；但 Act2 到达从812降至508，属于明确的前段能力退化。
这不自动证明 catastrophic forgetting，路线、构筑、动作执行、稀疏信用和环境差异都可能参与。

当前不直接延长旧配方，也不改网络或训练吞吐。优先完成环境核验，是因为已找到会改变HP、费用、合法动作、怪物循环及随机流的具体差异。
这些差异不证明它们独自解释了 pilot 结果，但意味着后续训练对照必须绑定修复后的同一环境，旧 λ=.98 运行不能继续当作严格单变量 control。

## 环境工作推进情况

已建立合法 stock JAR/字节码来源、固定24个有限义务、显式A20/Act2探针、动作重放、逐边界差分、checkpoint续接和存档恢复流程。
已修复多项有独立证据的根因，详见 README 与迁移记录。暂停前完整Python测试1138通过，并不等于真实游戏全部验收。

恢复后新增两项发现：

- Magic Flower：原版战斗结束仍处于COMBAT阶段，Burning Blood应回血9而非6。真实轨迹seed8000011000002边界153、字节码及修复前失败回归一致；修复后该边界通过。
- 原版等待边界：Oracle的EnemyMoveInfo投影已显示攻击时，真正monster.intent仍可能为DEBUG。Spot Weakness此时不加力量。这属于接入时序问题，不应错误修改native的Spot Weakness规则。原版执行契约升级v6，等待实际intent初始化。

后一项发现使此前三条production和跨幕stock捕获需要重采集；旧报告保留为诊断证据，不认证。具体原始文件摘要和受影响边界见 `boundary-invalidation-v6.json`。
受控Discovery比较仍使用独立stock实测动画输入，范围明确为条件核验；production不注入该输入。

## 收敛标准

完成72次受控运行和当前修复版固定规则选择的8条正常开局轨迹，解释首个实质分歧，补齐局部/邻接回归、来源/迁移/恢复证据后，才讨论下一轮pilot资格。
8条轨迹不用于估计胜率，不挑checkpoint；最终保留集不使用。
即使完成，也只认证本轮有限范围，其他Act2内容仍保持未验证。

本次恢复继续确认角斗场第一战的弃置药水roll/额外牌堆shuffle及第二战同房间RNG续接、召唤Mad Gremlin的Angry、Wizard偏移造成的目标次序，以及宝箱checkpoint无关偷金字段。已保存原版载荷、字节码定位和修复前失败回归；当前来源r18局部验证通过。Original执行契约随后升级v7，修复多选GRID撤销到零张的稳定边界。全部范围仍未验收，不启动训练。

## 本次恢复：r22

用户撤销赶时间的有限pilot收尾要求，恢复72次受控/8条production的原定有限验收。当前NativeSource为b515808c78fa8d9355921c721a4da01c15a3e8d375c645f2d75c75f9127a2a38。

新增Plated Armor队尾扣层、Centurion孤立Defend回退自身修复，以及validation逐动作条件输入的完整checkpoint重放契约。旧Fire Breathing解释被实际Headbutt动作和stock字节码否定，详见ACTIVE_SCOPE.md。

r22的63次有效战斗义务全部逐边界匹配（combat旧Entangle三行由boss-entangle新三行替代）。完整Python测试1166通过、1跳过；后续历史配置身份专项9通过；新stock脚本工具和canary选择11通过。27/27训练配置检查通过。测试通过不替代stock系统流程和production验收。

新system-scenes-v3.json预登记stock单一冻结策略记录脚本、native随后重放同一动作。正常开局的128-seed诊断扫描进行中；没有使用最终保留集，也没有启动新训练。

## 受控义务已完成，production进行中

Native r23（f6fad929448200bd593218f9f1e0e7a6cae422d26c316d92cdc2ac73f4b481bd）修复角斗场Nob意图投影。r24三条奖励轨迹全程匹配，角斗场第二战实际胜利后奖励/RNG回归取代先前仅源码依据的synthetic fixture；旧fixture与失败日志保留。r25 seed70完整370边界匹配。复用当前Oracle身份下有效v7的69/71完整原版捕获并重放验证；没有为节省时间删掉任何固定seed。当前72/72受控义务匹配。

当前源完整Python测试1171通过、1跳过、4个已有来源重绑定警告；Ruff、27/27配置、Windows CRLF允许的空白检查通过。随后补强过期Original执行契约拒绝门禁，专项14通过。最终仍会按交付时源码核验。

Native r23固定128诊断seeds在本地CUDA扫描完成，设备/runtime记录于raw scout；只为覆盖选8条，不估胜率或挑checkpoint。首批production 0/1/2开始采集。production无validation诊断注入；门禁仍关闭，见qualification-r25-interim.json。

## production首批发现与r27

首批正常开局3条完成原版终止及恢复。seed8000011000001全程200边界匹配；0002在352→353发现Ritual Dagger击杀后永久牌组15→18的更新滞后。原版RitualDaggerAction.update直接按UUID更新masterDeck及GetAllInBattleInstances；native只在战后复制misc。修复前完整状态回归失败已保留，r27同步玩家边界的永久增长及同身份战斗实例，修复后0002完整450边界匹配，checkpoint恢复回归通过。

Native r27来源为af39371c2d671a1c16d3ce137ba90ce32b1ed5b75e24641ffcf86d8aa3bb2560。规则/公开观测语义发生变化，没有自动兼容白名单；已有r23的72/72只能作为历史证据，交付前要对当前来源复算。

0000在边界124出现Infernal Blade生成Perfected Strike与Iron Wave的实质差异。原版InfernalBlade.use与AbstractDungeon.returnTrulyRandomCardInCombat(CardType)证实使用有seed的cardRandomRng，不能归为可忽略随机展示。此前117边界使用Power Potion/Discovery；原版DiscoveryAction.update在duration/retrieveCard判断之前每帧生成并弃置选择，帧数可改变后续RNG。该方向目前属于有源码依据但尚未独立确认的轨迹根因；不把该报告改成通过，不注入validation字段到production。其余5条原版轨迹继续按固定selection采集。


## 2026-10-07：用户缩减收尾范围；r29/r30定点修复

不再新增自然局，原严格8轨迹认证延期。r29修复DamageAction正常攻击在攻击者死亡/halfDead后应取消：Byrd被Thorns杀死仍多打4次，HP17对stock21。r30依据TheCity.generateExclusions补齐首强敌排除：Spheric Guardian→Sentry and Sphere；3Byrds→Chosen and Byrds；Chosen→Chosen and Byrds/Cultist and Chosen。重抽必须消耗monsterRng。seed8000011000005修复后378边界全部匹配，原失败/回归保留。

当前source b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f。已有5/6production严格匹配，Discovery计时分歧尚未独立确认，不能归为已修复。实际coverage较预选少：事件战不冒充地图普通战。保护的63个存档/配置/Mod文件恢复哈希一致，无游戏进程。

优先级2新增tools/profile_act12_batching.py：固定原版完整轨迹的等间隔公共边界，核对当前adapter投影后测量编码与padding；固定单线程、warmup、拒绝覆盖结果。仅CPU microbenchmark，不作为服务器端到端吞吐或训练收益证据。


2026-10-07最新指示：先总结任务1，任务2等下再开始。优先级1有限收尾记录于CLOSEOUT.md/bounded-closeout-r30.json；优先级2已做初步profiling和编码转换原型，未完成端到端验收，现暂停。尚未提交Git或NUS训练。
