# Discovery 动画计时：独立证据与结论边界

本页记录一个需要独立验证的首个分歧，不把条件重放算成production通过。

## 原始发现

`production-r26-a-8000011000000.jsonl`边界117选择Power Potion生成的Corruption，边界124在相同Infernal Blade动作后，stock生成Perfected Strike，native生成Iron Wave。HP、费用、卡牌身份均为实质字段，严格报告保留失败。

原版字节码确认InfernalBlade.use通过AbstractDungeon.returnTrulyRandomCardInCombat(CardType)使用cardRandomRng。DiscoveryAction.update在检查duration和retrieveCard之前先生成一组三张候选；随后动画更新也消耗这个流。动画帧数不是策略动作，因此仅凭同seed和同动作不能证明随机流会一致。当前native默认14次检索更新没有被本轮修改。

旧production轨迹没有实际帧数证据。不能反过来搜索一个让后续卡牌匹配的次数，然后声称它是实测值。

## 新证据路径

Oracle1.2.2在已有DiscoveryTimingPatch计数完成后，向owned launcher的本地stdout输出`SLS_DISCOVERY_CLOCK_V1`记录：seed、floor、完成serial及实际更新次数。它不修改stock RNG、卡牌、动作或duration，不向CommunicationMod消息添加隐藏字段，策略不读取这份日志。日志操作可能影响未来帧调度，因此不声称它能使跨次实机运行完全复现；每条记录只绑定该次实际捕获。

`replay_act2_production_trajectory.py`默认仍然是严格production比较，不读取clock日志或设置validation输入。

独立的`replay_act2_clock_conditioned_trajectory.py`只从已完成且恢复成功的原版进程日志读取实测次数，检查捕获/launch/build身份、seed/floor、计数范围、重复serial和遗漏记录，再使用native的validation条件重放接口定位原因。它完整比较公共状态、动作和终止原因，不替换生成卡牌、不忽略字段、不搜索计数。

该工具的全程匹配状态是`CONDITIONAL_PUBLIC_TRAJECTORY_MATCH`，不是`TRAJECTORY_MATCH`。汇总门禁拒绝把它计作无条件production通过，另有回归测试。缺少实测clock日志、过期构建、未完成执行或恢复失败均拒绝运行。

## 待核验

新捕获、严格比较及clock条件比较尚待完成。只有实际实测输入解释整个首次偏差及后续公共状态，才把该次分歧归为计时依赖；否则继续查规则或adapter。其他未见过的计时组合和production RNG分支仍未验证。


## 2026-10-07独立运行确认

Oracle1.2.2 production新捕获实测seed8000011000000、floor16、serial1、updates15。无条件重放仍在边界124分歧；只注入该次实测15，完整187边界/终止原因全部匹配。见discovery-r31-strict.json及discovery-r31-clock-conditioned.json。原捕获旧运行未倒推计数；新捕获只证明本次时序解释。训练默认14次未改，不宣称跨帧调度同seed bitwise一致。诊断入口与native统一支持1..120，超范围拒绝，不截断。
