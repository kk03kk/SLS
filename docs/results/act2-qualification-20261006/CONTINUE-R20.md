# 第二次暂停：r20 续接记录

2026-10-06 19:20左右，用户要求立即暂停收尾，下次继续。状态 **PAUSED_NOT_QUALIFIED**；不继续修复、采集、训练准备、commit或push。

## 范围与当前身份

用户已要求尽快开展下一阶段训练，完整72次/8条认证不再作为无限前置条件。下次沿用有限pilot收尾范围，见 PILOT_GATE.md；未验证项不能改写为通过，已复现实质错误仍需处理。

- HEAD仍为 `d9ee32d67003f8ce7567a77df21916e96cbbdfef`。工作区保留原有pilot整理及本轮修改，尚未提交。
- 当前native源码与编译SHA一致：`a47d41519d3dea06a245da38fef8ba5956742a404c5dc9d09bf9b967ecdb560c`（r20）。
- Oracle1.2.1 r6：`f59e2960838a5b744b9a146b8c489560d532f411c813886955ca2fa6dce7a4ef`。
- Original执行契约v7；GRID契约v2；冻结90M模型训练来源仍为Act1。
- AGENTS.md未改。游戏与Python任务均已退出；本次journal的63个用户文件SHA/存在性全恢复，受保护目录没有新增遗留文件。独立检查：`local/audits/act2-qualification-20261006/pause-recovery-r20.json`。

## 本次恢复已完成

除首次暂停已有修复外，新增Magic Flower回血取整/胜利阶段、实际DEBUG intent等待、角斗场第一战弃置药水roll与牌堆prep及第二战同房间RNG、召唤Mad Gremlin的Angry、Wizard公共目标顺序、宝箱无关偷金checkpoint字段、多选GRID撤销至零边界。

r19补齐Hexaghost Inferno升级已有draw/discard Burn并加入三张Burn+：真实stock seed131100070边界191、原版字节码和修复前失败测试均保留，修复后该局部回归通过。

r20补齐角斗场第二战药水roll及卡牌奖励：原版rewardAllowed/AbstractRoom/CombatRewardScreen调用链独立确认，修复前类别回归失败、修复后通过。**这项尚无第二战胜利原版数值对照，只有源码依据与类别回归，不能称运行认证。**

诊断筛选修正普通Slaver/Colosseum Taskmaster与地图精英的区别、Boss遭遇名与实际怪物ID的区别。新覆盖契约 `sls-act2-map-room-coverage-v1`，只影响评估诊断，不改模型输入。尚未做新128-seed scout，也未重新选择8条。

## 实际结果与未完成项

- `combat-after-r20.json`与`boss-entangle-after-r20.json`：63个有效关键战斗受控场景在当前source上匹配；旧combat中的Entangle三个旧harness行仍须明确剔除，使用r5替代。
- `system-rewards-after-r20.json`：seed67/68匹配155/293边界；67终止于Act1，不认证Act2。66仅248边界前缀匹配，不通过。
- 本次原版r12批次正常退出并恢复：69完整271边界至Act2终止；71完整142边界但Act1终止；70在325边界脚本不可用；66在162边界脚本不可用。原始载荷未覆盖。
- r12正式差分工具在seed66边界129 checkpoint恢复抛异常，未产出整批报告。不得当作通过或忽略。
- 当前完整Python检查：**1154 passed、4 failed、1 skipped、4 warnings，83.19秒**。见 `pytest-final-r20.log`。不是全绿。
- 三个失败来自新canary测试夹具：它错误地把只有一次Boss entry的行写成普通战末态，导致该行先占普通战角色。夹具已改成真实Boss monster IDs，**修改后尚未重跑验证**。
- 另一失败为旧Act1确定性生命周期测试对Hexaghost步数/轨迹的硬编码。Inferno修复已改变行为；下次查明日志中的具体差值及独立回归后更新旧期望，不能直接为了绿而改值。

## 下次只处理这些具体收尾点

1. **Fire Breathing与Plated Armor实质差异**：修复Inferno后，seed131100070第215边界原版护甲9、native8；其余公开状态当时一致。原始action为HAND:3，对应状态牌抽取触发。最小证据已保存 `checkpoint70-second-divergence-r20.json`（before/action/stock/native/RNG）。尚未写回归或修复。
   优先审核stock FireBreathingPower的THORNS伤害、PlatedArmorPower.wasHPLost、native CardManager.cpp及Monster.cpp.damage/damageUnblockedHelper。不要盲目修改所有AOE调用；区分NORMAL与THORNS，保留相邻Flight/Malleable/death回调验证。
2. **条件重放checkpoint恢复**：`checkpoint-restore-131100066-first-divergence-r20.json`，第129边界COMBAT。此前Discovery原版检索动画14/15次有实测条件输入。检查native完整replay_actions是否保留对应validation evidence；不要改成跳过checkpoint一致性。69逐边界restore诊断没有异常。
3. 修正两个已定位失败后做局部/邻接和完整检查。更新迁移文件的最终SHA和变化状态：`semantics-migration.json`目前仍写r18旧SHA，需要收尾同步；stock-flow identity尚未合并 `bytecode-final-r20/identity.json`。
4. 不再重采全部系统场景或扩大审核。原拟有限生产回归为旧诊断seeds8000011000000、1、2，针对既有药水边界/死亡block/Magic Flower/DEBUG问题；**尚未运行**。这不是重新按覆盖选择的8条名单，不能估胜率或完整认证。新完整scout/8条保留以后。
5. 完成恢复核对、文档/紧凑证据及有限pilot准备资格判断后再提交GitHub。旧NUS SHA绑定训练计划不能直接复用。旧λ=.98运行不是修复后环境的严格单变量control。

本次用户要求暂停时，没有仍在后台运行的游戏、测试或采集进程。不要自动启动服务器训练，不声称pilot已就绪。
