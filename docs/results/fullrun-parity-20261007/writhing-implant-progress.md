# Writhing Mass：Implant 永久牌组时点差异

最新状态：**永久获得时点已修复；12 个战斗中场景及新增 9 个实际击杀／稳定奖励页场景匹配所列字段。** 奖励领取后的继续流程和其余遗物组合尚未关闭。下面保留各阶段独立证据。

独立字节码链：WrithingMass.takeTurn 排队 AddCardToDeckAction(Parasite)；该动作创建 ShowCardAndObtainEffect。Oracle validation-only 直接读取 `player.masterDeck`，另标识 `sls-stock-master-deck-v1`，不使用公共 adapter 作为唯一正确答案。

三个场景的初始永久牌组分别核对并配对了正常 Neow 已造成的升级/添加/移除。第一、第二个 end turn 后，原版直接读数均已包含 Parasite；native FullRun 的永久牌组均未增加。首个差异全部为边界 1。最小复算入口 `tools/reproduce_writhing_implant.py` 只比较牌组组成，不冒充完整战斗差分或胜率证据。

native 调用链解释：MonsterSpecific.cpp 的 WRITHING_MASS_IMPLANT 标记 miscInfo，但没有当场更新 GameContext.deck；BattleContext.exitBattle 才获得 Parasite或扣 Omamori。单独战斗探针的牌堆/HP一致无法认证永久牌组时点。

后续修复必须核对真实获得回调、Omamori 剩余次数、Darkstone Periapt、Du-Vu Doll，以及退出时不重复执行、checkpoint 中断恢复。不能只修改 observation 显示，也不能把原版获得过程改成战后才发生。先保留当前 c071 源码/二进制和失败证据，再逐根因修复。

当前原版构建 1.3.28 SHA `945742d8f1a1a94dfc3dfb16a28973bd0a5948f5704d329df7967a3eb3c2c326`。本次还修正受控初始 WrithingMass 名称映射和 Implant STRONG_DEBUFF 意图；没有修改原版后续回调。这些是工具初态改动，不能算 native 修复。

原始 stock、native-before、构建及字节码记录在本地 `local/audits/fullrun-parity-20261007/implant*`、`stock-implant-timing-r1/`。采集完独立核验 63 项保护文件，0 差异；失败/初态不配对的尝试不计通过。

此问题来自 Act3 分支，但所需永久获得机制修复可能触及共享代码，届时必须重新审核 Act1/Act2 影响和环境身份。当前服务器来源不修改，既有 λ/critic 结果不归因于本问题。

## 已执行修复与恢复

FullRun 每次动作后调用 `syncImplantObtain`，在下一个玩家操作边界之前执行真实永久牌组获得和 Omamori/Du-Vu 计数更新。战斗资源回调在 Implant 的实际执行中处理，新增 Ceramic Fish 金币获得；原有 Darkstone 最大 HP/治疗保持战斗路径，永久 obtain 不再重复施加其资源效果。

现有已序列化的 `BattleContext.miscBits` 位 1 记录永久获得是否已同步；位 0 仍是盗贼金币检查。战后 exit 检查该位，防止再次获得 Parasite或再次消耗 Omamori。字段布局未变，但位 1 的状态语义是新的：不能因此登记跨来源 exact resume。

旧 c071 源码和二进制保留于 `implant-before-c071/`。在旧二进制上运行新增原版派生回归，12 项均失败；新实现通过。另有 12 项 native-only 结束战斗检查，通过去重验证；这些合成结束不宣称获得了原版完整胜利资格。

邻接原版场景：Omamori 两次剩余（201–203）、Omamori 已耗尽（204–206）、Ceramic Fish + Darkstone Periapt + Du-Vu Doll（207–209）。合计 12 个场景比较永久牌组组成、live combat HP/maxHP/金币、Omamori/Du-Vu 计数和完整 RNG，全部相等。初始 Neow 已造成的牌组变化均从独立 stock 对象配对；不是让 native 提供预期值。

首次资源报告误用了 checkpoint 外层的幕间 player_state HP，导致第二回合假差异；修正为 active combat 的真实 HP，旧报告 r1 保留为工具失败诊断，不计通过。后续 r3 保存全部 RNG 及明确资源字段，仍不是全部战斗投影证书。

新 native 来源：`32cbf55731f5b48d798799d9d77d874f26ce45f06aa133d1b1f5aabeab766e2e`。单编译任务构建和导入完成；89 项局部、相邻机制、原版工具检查通过，Ruff 通过。没有模型、训练、GPU、测速或服务器操作。完整计算型 Python 套件未重跑，不借用历史完整测试为新来源背书。

Oracle 1.3.29 只增加受控初态 relic counter 设置和新的清单，来源 SHA `c9ad935b349334694b2c8bbec90d98bc2cf28aa7fe61c0a83f18497f46532e75`；production smoke 已实际通过。原始 capture/build/恢复证据继续留本地。恢复哈希以本批最终收据为准。

剩余义务：stock/native 完整击杀与战后继续、其他获得/治疗组合（Magic Flower、Bloody Idol、Ectoplasm、Red Skull 等）、更广泛共享永久变化及自然晚幕轨迹。全幕目标仍进行中，不升级为整个 Writhing Mass 或全模拟器认证。

## 实际击杀与奖励页补充

不可变场景 `fullrun-implant-victory-r1.json` 使用 seeds131200210–218：普通、Omamori 和 Ceramic Fish/Darkstone/Du-Vu 三组，各三个 seed。仅设置初态，Implant、攻击击杀和奖励生成均由原版动作系统执行。九局共27个边界，永久牌组、HP/maxHP/金币、相关计数及14条 RNG 比较一致；最终稳定奖励页另核对金币、药水、遗物、卡牌和两类保底计数。

Oracle1.3.30 validation 直接读取原版 cardBlizzRandomizer，避免把初始卡牌保底值猜为常量。该新增诊断不改变公共 observation。原版构建 SHA `0ccafd03c95e988d8ea1de9ca68479b54dac95975686fc446c4c3f07f9609cb4`，production smoke 实际通过；native仍为32cbf557来源，本批未新增规则修复。

新增9项 stock 派生回归，在每个边界恢复并严格比较下次动作结果、snapshot和合法动作；连同既有 Implant及构建测试，本批40项通过。Ruff和相关差异检查通过。两次启动的63项保护文件分别独立重算，均0差异，进程已结束。紧凑证据见 `implant-victory-evidence-r1.json`，原始日志留本地。

这关闭了“实际击杀到奖励页”的选定分支；不等于奖励领取/下一房间、所有治疗回调或完整敌人认证。旧段落中的完整击杀待办保留为历史状态，以本节为准。
