# 当前范围：有限收尾，进入吞吐测量

2026-10-07，用户再次明确要求优先级1可以差不多收尾，进入优先级2并尽快准备训练。本指示取代下面10月6日恢复全部8条production验收的要求。保留24义务/72次受控证据；正常局使用已经采集的6条，不新增另两种Boss自然轨迹。严格原计划门禁仍保持未认证，不能把缩减范围称为完整Act2认证。

当前已知的实质伤害、永久牌组和遭遇池错误必须修复；Discovery动画计时差异保留未验证限制。下一步是固定输入吞吐profiling及单项语义保持优化，之后冻结新环境的训练设计。旧环境λ=.98结果不能充当严格匹配control。

---

# 恢复原定有限认证范围

2026-10-06，用户明确撤销赶时间的有限pilot收尾取向，要求继续优先级1，完成后再推进后续工作。

本次恢复原计划：首批24个义务、72个固定机制seeds，以及8条正常Neow开局production轨迹。不是全Act2内容认证，不进入Act3/Heart，不改变PPO/reward/network/start distribution。

PILOT_GATE.md和CONTINUE-R20.md保留为之前阶段的历史决策及暂停记录；它们关于缩减本次范围的要求已被本次用户指示取代。

## 已定位问题的独立复核

- 暂停前把护甲问题归为Fire Breathing的假设不成立。seed131100070边界215之前实际动作是Headbutt；stock PlatedArmorPower.wasHPLost把ReducePowerAction放入队尾，选牌边界仍显示9层，native提前扣成8层。r21修复队列时序，并验证选牌结束后的8层状态。没有盲目改动非攻击AOE伤害。
- Discovery条件重放遗漏独立stock实测动画输入。r21增加`sls-stock-conditional-replay-v1`，将稀疏逐动作验证输入与完整动作历史一起序列化。无输入的production/训练不增加该字段。没有降低checkpoint比较标准；缺少输入的旧条件捕获仍被拒绝。
- seed70继续到边界313，Mystic已死亡、Centurion之前承诺Defend；原版GainBlockRandomMonsterAction候选为空时回退到自身且不消耗目标选择RNG，native原来没有格挡。r22修复，并以stock公共状态/RNG回归确认。

## 系统流程动作脚本补充

冻结Native长脚本会因stock实测Discovery动画14/15等差异而过早失效，原失败记录保留。为取得完整系统流程证据，新增`tools/capture_act2_stock_policy_system_batch.py`：

1. 初态保持同一normal A20 Neow和固定seeds131100063..131100071。
2. 只在stock validation中运行冻结90M策略，模型SHA固定，保持Act1训练身份和显式Act2评估契约。策略不读取validation诊断字段。
3. 记录不可覆盖的完整原版载荷、动作journal和单一语义脚本，再由native逐边界重放该脚本。Native不另行选择动作，不提供预期答案。
4. stock实际计时输入只用于validation条件核验；production没有诊断注入。逐边界验证状态、mask、RNG、直接stock对象和checkpoint续接。

这项是系统场景脚本来源补充，不是修改63个战斗义务的初态或动作清单。场景清单、请求协议和旧结果分别保留身份，未触发分支仍单列未验证。

69/71的r12原版v7捕获已经完整终止，Oracle构建SHA与当前一致；当前Native r23全程重放、checkpoint及独立stock死亡原因核对通过。复用这两条完整原版证据；70旧脚本停止，重新由stock冻结策略采集。固定九个系统seed及比较义务不变。

新r24完整奖励轨迹确认角斗场Nob意图投影错误，已最小修复为ATTACK_DEBUFF（普通构造器canVuln=true）。第二战胜利的奖励和RNG现在有实际stock证据。NativeSource变为r23，已有r22开发扫描/选择不直接重绑定新源码。

## 后续优先级

此前 `docs/results/act12-pilot-20261006/project-priorities.md` 的顺序保留：2是吞吐测量/语义保持优化，3是学习机制与策略诊断。λ仍是下一训练候选；环境规则已经改变，旧λ=.98 pilot不能继续作为严格单变量control。最终正常开局Act1+Act2联合通关目标不变。
