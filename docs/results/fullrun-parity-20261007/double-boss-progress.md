2026-10-08：六种有序组合已有受控代表性证据，最后两种见double-boss-awakened-second-qualification-r1.json（来源9bb、Oracle1.3.38）。不是所有随机分支或正常局认证；共享RNG来源6805的变化另有记录。以下历史进度保留。

# A20 六种有序双 Boss：独立源码审核

当前状态：**原版调用链已重新导出并人工审核；六种组合全部仍待运行对照。** 没有把旧单 Boss、跳战或高 HP 机制场景升级为双 Boss 完整流程资格。

来源为本机合法 JAR `cfad868a…`。`tools/audit_stock_double_boss.py` 拒绝不同 JAR 身份和覆盖已有输出，对四个原版类执行有超时的 javap，逐项记录 class 和输出 SHA。原版字节码仅保存在本地 `stock-double-boss-r2/`；项目自有紧凑记录见 `double-boss-source-review-r1.json`。

独立确认：TheBeyond、A20、剩余 bossList 数量为2时，ProceedButton 才进入第二 Boss。MonsterRoomBoss.onPlayerEntry 会移除列表首项；goToDoubleBoss 取剩余首项设置 bossKey，创建新房间再调用原版转场。转场增加楼层、按 seed+floor 重建五条战斗随机流，并执行遗物入房回调。native 的 Act3 afterBattle 静态结构与这些调用一致，但静态一致不能证明奖励生成、恢复或回调次序完全相同。

当前工具缺口是可复现的：parity_scene 替换遭遇并未建立第一 Boss 已消耗的 bossList/bossKey；直接 stock 诊断也未暴露这两项。因此下一步必须补**仅初态设置与独立读数**，再支持实际胜利按钮到第二战稳定边界。不能只强制两个敌人先后出现就称为原版双 Boss。

六种组合的每项义务均登记为 RUNTIME_UNVERIFIED，比较第一战初态、胜利、第二战初态和终点；核对资源、永久牌组、药水、遗物回调、奖励构造与实际领取、全部 RNG、合法动作及恢复。Awakened One 的复活、Donu/Deca 的双目标死亡必须由原版执行，不能在第一战结束后重新造局。

本批只运行轻量源码导出和静态检查，没有启动游戏、模型或训练，也没有修改 native 生产规则、服务器 checkout 或推送 GitHub。全幕目标继续保留；本节不是完成证书。

## 工具补齐批次：Oracle1.3.31 / 诊断 v13

新增 validation-only 的直接 `boss_key`、`remaining_bosses` 和 `sls-stock-boss-flow-v1` 身份。受控清单可指定全部三个不同 Boss 的 `initial.boss_order`，首项必须等于当前遭遇；仅在初态建立原版首战入场后已消耗的列表。后续胜利、奖励和第二战构造均不打补丁。Python 启动前和 Java 执行端分别拒绝错误上下文和顺序。

采集器新增 `proceed_to_second_boss` 语义步骤：先取得实际稳定奖励页并核对首战列表，再执行原版 proceed，等待正确楼层、第二 Boss、剩余第三 Boss、房间和 WAITING_ON_USER 边界。超时、缺诊断或跨过该楼层均不计通过。

新 Oracle 从源码编译38个类成功，SHA `f736f1ebae43dbbd8047693e5e2460b2b572687fa5ffdac45bf9429176374251`。首次 production smoke 因读取端只接受旧诊断版本而失败，原始日志保留；显式支持 v13 后，production 和 validation 两种实际启动检查分别通过。v13 的诊断仍被 production 隐藏，live 入口继续拒绝隐藏载荷和未知版本。

48项轻量工具／mock／构建测试、Ruff及差异检查通过。三次启动结束后分别独立重算63项保护文件，0差异。没有模型、训练、GPU、native生产修复或服务器操作。构建和启动检查证明工具可加载、模式隔离与新增读数可用；**没有执行 boss_order 场景或双 Boss 战斗，因此六种组合依然未验证**。

下一项是冻结六种来源绑定场景及实际脚本，先运行代表性连续转场，再对照 native；不能把本节工具检查升级为双 Boss 资格。

## 原版连续转场首批运行

不可变场景 `fullrun-double-boss-entry-r1.json` 固定 seeds131200219–221，在实际 TheBeyond/MonsterRoomBoss/A20/floor50 建立 Time Eater 初态；只在初始设置 HP1和动作牌，随后击杀、胜利回调、proceed、第二房间构造与开场均由原版执行。三个 seed 均进入 floor51 的 Donu/Deca，bossKey和只剩 Awakened One 的列表相符，处于 WAITING_ON_USER。此批不是自然局，也尚未完成第二战或 native 比较。

首次 r1采集失败暴露了工具错误：Act3 Boss 胜利是 COMPLETE/proceed 边界，不是普通 COMBAT_REWARD；正常战斗的原始 screen_type 为 NONE，不是 adapter 的 COMBAT。失败原始日志保留；改为核对独立房间完成证据及正确的稳定操作边界后，r2三次执行完成。49项轻量测试、Ruff与相关差异检查通过。两次启动各63项保护文件独立复算0差异，游戏进程已结束。

Oracle1.3.32 仅更新版本及冻结场景资源，Java诊断与31保持同一v13；源码构建 SHA `201f10de724c42f77faff2631570ee02ef62679efd7f0cfc0a8a47bc75681b3c`。32的上述validation场景已运行，32 production smoke尚未执行，不能借用31的烟测改写身份。紧凑原版见证为 `double-boss-entry-stock-witness-r1.json`。六种组合的完整对照义务仍全部未关闭，下一步使用该原版初态重放 native，比较首个实质分歧。


## 连续战斗初始化遗漏：已确认并最小修复

三个受控初态的HP/maxHP、敌人HP、全部RNG一致。旧native击杀第一Boss后已到floor51，但没有combat_checkpoint、合法动作为空；恢复却创建第二战并产生8/9/8个动作。失败snapshot、旧源码/二进制、三个失败回归已保留。

根因是FullRun.step在exitBattle之后释放旧battle，却未初始化GameContext直接进入的新战斗。修复只在BATTLE且UNDECIDED时调用已有start_battle。单任务构建成功，新source81e83de9…；三个新增恢复回归和Heart/Ending/完整局结构/Implant等邻接合计64项通过，Ruff及差异检查通过。没有模型、GPU或训练，也未运行完整计算型套件。

初次after-r1报告的金币差异来自探针错误：MawBank依据usedUp而非显示counter。本场景明确安装新遗物且未花金币，native初态应为active=1；该有限映射由合法JAR字节码独立确认，不适用于未知历史遗物状态。旧错误报告保留；after-r2三个seed的楼层、HP/maxHP、金币、牌组组成、怪物身份和全部RNG一致。

完整身份与限制见double-boss-source-transition-r1.json。环境转场语义改变，布局不变也不批准state-preserving或自动exact-resume；当前服务器checkout不修改。此批仅关闭初始化根因及枚举开场字段，完整powers/牌组属性/计数/mask、第二胜利和其他五种组合仍待验证。


## 扩展严格边界比较：修正初态与意图稳定义务

旧r2虽然资源与RNG相符，但其第二战公共意图仍为DEBUG，严格差分器拒绝完整决策资格。旧原始证据保留，原报告只支持其枚举字段，不支持全部稳定观察。采集器新增等待实际意图物化；固定脚本/seed重采r3成功，没有更换seed或忽略意图字段。

探针set_potions默认三槽，原版A20为两槽；已明确配对初态potion_capacity=2，不通过裁剪比较结果隐藏第三槽。使用双方相同extended_direct投影后，三个seed的第一战初态和第二战开场均匹配：公共观察/实际合法语义动作、HP/格挡/energy/powers、敌人状态、四个有序牌区及费用/升级/retain等属性、药水槽和遗物计数、全部RNG。永久牌组仍另核对组成，并不代表所有永久成长属性已认证。

新增三项原版派生扩展回归，包含第二战恢复。连同原转场、Heart/Ending/Implant/结构、边界及人为差异检测，本批120项通过；Ruff及相关差异检查通过。native仍81e83de9，本批无生产规则改动。Oracle1.3.32 production smoke实际通过，补齐其独立运行身份；两次新启动各63项保护文件独立复算0差异，游戏已退出。

紧凑证据见double-boss-entry-extended-evidence-r1.json。这提高了选定两边界证据强度，没有关闭第二胜利、其余五种组合、钥匙/转场、自然晚幕或完整全幕义务。旧“稳定开场”描述以本节显式意图核验为准。


## 有界完整两战探针：初态工具准备

为避免第二战重造局，新增仅初态flow_master_deck设置，严格限制为5–30张Searing Blow+30及合法boss_order上下文；Python启动前和Java执行端均拒绝未知配方。升级逐次调用合法原版card.upgrade，既有CardLibrary原型不修改；hand/draw同样支持受限升级规格。后续抽牌、洗牌、复活、伤害、死亡、跨房间仍走原版，不保留人为力量至第二战。

原版SearingBlow字节码独立复核：初伤12，第i次升级增加4+i，因此30次为567；来源摘要见double-boss-flow-card-source-r1.json。短native CPU探针确认牌的升级数30、misc30、cost2；尚未把这一初态在原版实际执行，不能认定实际伤害或完整牌机制认证。此极端初态只服务有界流程核验，不属于正常Neow轨迹或胜率证据。

Oracle1.3.33源码构建38类成功，SHA69a2d54cb99bd3ff920735cd02ec1e9fae9eefb104818d2a4a8729c1d425dcb8；production smoke通过，63项保护文件独立重算0差异。39项初态拒绝/构建/已有双Boss回归通过，Ruff和差异检查通过。新master-deck场景仍未运行；接下来冻结两战脚本并从初态连续执行。

本批没有native生产规则修改、模型、GPU、训练或服务器操作。已有81e83de9来源和历史原版场景分别保留。六种完整组合依然开放，工具准备不算组合通过。


## 完整两战首批与终点恢复修复

冻结场景fullrun-double-boss-complete-r1.json，seeds131200222–224，真实Time Eater→Donu/Deca。只设置初始Searing Blow+30牌组，不调整第二战HP。三个原版场景均执行五步脚本并取得第二Boss死亡后的稳定COMPLETE/proceed，HP700→706→712，金币99→111；原版仍构造Boss金币RewardItem但没有实际领取。Oracle1.3.34构建SHAed6a3496…，validation批次完成，63项保护文件独立核验0差异；34 production smoke尚未运行。

完整native重放发现terminal snapshot仍保留BATTLE屏幕；load_state无条件按屏幕创建新的Boss战，违反胜利终点与恢复一致性。三个新增回归均在旧81e83de9失败，旧源码/二进制及失败快照已保留。最小修复要求BATTLE且UNDECIDED才初始化，原死亡combat checkpoint分支保持。新source见double-boss-terminal-source-transition-r1.json，单任务构建成功，131项局部/邻接/工具检查通过；各重放边界均恢复一致，Ruff和相关差异检查通过。

仍存在直接状态差异：Deca已死后的下一end turn，原版尸体powers为空，native仍Artifact3。差异只出现在直接投影，当前公共观察及合法动作一致；没有删除字段掩盖差异，仍待原版/原生回调审核决定是否有规则影响。

最终native已经直接进入floor52胜利房间，而stock证据尚在floor51第二Boss胜利页，不能把这两点的金币和RNG直接作为同一边界比较。下一步补实际stock proceed后房间证据，再完成端点对齐。三个场景虽已真实打完两战，组合完整资格仍开放；其余五种顺序、钥匙/Heart/内容清单义务不缩减。新来源改变恢复语义，布局不变也不批准state-preserving/自动exact-resume；服务器与训练保持独立。


## VictoryRoom 对齐及尸体差异分类：有限资格收尾

Oracle1.3.35（SHA8b9b4265…）在 seeds131200225–227 实际执行两战和第二次 proceed，抵达原版 floor52 VictoryRoom。native9bbf5664 在相同终点的楼层、HP/maxHP、金币、永久牌组顺序及升级数、全部14条RNG流均匹配，每个重放边界的snapshot恢复一致。此前 floor51胜利页与floor52之间的差异不是同边界比较，现已补齐原版证据。

独立字节码证明 Deca powers.clear 位于死亡动画结束清理；死亡敌人不执行回合能力，policy adapter也排除死亡敌人的powers。唯一残留差异为已死亡Deca的Artifact3。原始equal:false保持，新增分类器只接受此确切差异，拒绝活敌人、其他能力、HP/RNG变化或指向尸体的合法动作；不推出所有尸体/复活机制等价。

本批53项有界CPU/工具测试通过，Ruff通过。35 production smoke通过；validation批次及production smoke各63项保护文件独立重算0差异，游戏进程均已退出。没有运行模型、训练、GPU或测速，完整计算型测试套件未运行。

紧凑证据为double-boss-terminal-qualification-r1.json。只授予所测Time Eater→Donu/Deca初态/脚本的有限资格；VictoryRoom仅入场，未验证对话终评、正常开局胜率、任意药水遗物延续或全部Boss分支。其余五种顺序及钥匙/Act4/可达内容义务继续开放。native保持9bbf5664，无新增生产修复；既有来源迁移限制仍有效，服务器训练身份独立。


## 第二种顺序：Donu/Deca→Time Eater

冻结fullrun-double-boss-reverse-r1.json，seeds131200228–230。分配前扫描现有场景清单，未发现冲突。只交换初始Boss顺序，继续使用已审核的初态牌组；实际原版先击杀Deca、换回合、击杀Donu，proceed到Time Eater并完成第二战，最后进入VictoryRoom。第二战没有重新造局。

此前entry重放入口只执行首张牌，无法验证需要多步的首战；现按已记录的首战动作执行至proceed，保留目标槽位并拒绝未知动作。旧Time Eater→Donu三场在泛化工具下重放仍匹配。新三场初态和第二战开场的扩展公共/直接字段、合法动作与RNG匹配，终点六类字段及每边界恢复匹配；唯一raw equal:false仍为已审核的死亡Deca Artifact清理，未做通用忽略。

新增三个原版终点派生回归与动作拒绝测试，63项有界检查及Ruff通过。Oracle1.3.36源码构建38类，SHAcb16b5ba…；validation实际对照和production隔离烟测通过，每次63项保护文件独立复算0差异，游戏已结束。native9bbf5664保持不变。本批无模型、GPU、训练、测速或服务器改动。

紧凑证据见double-boss-reverse-qualification-r1.json。目前取得两种顺序的有限代表场景资格，仍有四种包含Awakened One的顺序开放；不推出完整Boss机制/自然局资格。下一批需要明确穿过Awakened One首阶段死亡、复活、最终死亡的脚本，不能把首次致命伤当成胜利。钥匙/Act4与全部内容清单继续保留。


## Awakened One 首战的两种顺序

重新从合法JAR导出AwakenedOne/Cultist字节码，冻结fullrun-double-boss-awakened-first-r1.json。分配前检查现有场景seed，131200231–236无冲突；分别执行Awakened→Time Eater和Awakened→Donu/Deca各三次。首战脚本先击杀两个Cultist，再完成Awakened首次致命伤、end turn复活、最终致命伤，之后实际proceed第二战并进入VictoryRoom。造局只发生在初态，第二战没有改HP、牌组或RNG。

新增独立生命周期门禁：第一次死亡必须HP0/half_dead且仍是战斗，复活必须320/320且重新存活，最后才允许COMPLETE/proceed及room COMPLETE。六次原版均见证上述过程；native初态/第二战开场扩展投影与动作、终点楼层/HP/maxHP/金币/永久牌组顺序升级/14条RNG一致，各动作边界恢复一致。

新增直接差异是已死亡第二Cultist的Ritual5，原版清空、native保留。独立AbstractMonster.updateDeathAnimation字节码及双方死亡跳过调用链、adapter排除死亡powers解释本场景不影响策略/后续状态。分类器限定该槽位、敌人、能力与数量，并要求其余全部投影相同且没有合法尸体目标。raw equal:false和旧r1报告保留；未泛化忽略死亡或复活能力。第二战Deca差异沿用已有独立限域依据。

Oracle1.3.37源码构建38类，SHAe0d66483…；validation六次完成，production烟测通过。两次启动各63项保护文件独立复算0差异，游戏退出。新增六项原版终点/恢复回归及阶段、差异拒绝测试；84项有界检查和Ruff通过，未运行完整计算型测试。native9bbf5664不变，无生产规则、模型、PPO、训练、测速或服务器操作。

证据见double-boss-awakened-first-qualification-r1.json。目前四种顺序获得代表性受控证据，尚余Time Eater→Awakened、Donu/Deca→Awakened。也未覆盖Awakened最终死亡时Cultist仍存活的队列分支；不以已测脚本代替全部Boss认证。钥匙、Act4、内容和正常晚幕轨迹义务继续开放。
