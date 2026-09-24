# A20 模拟器本地审计收尾（2026-09-23）

本轮保留此前工作区修改，对照 `decompiled/desktop-1.0/source/com/megacrit/cardcrawl/` 的 Java 投影检查后续幕与终局。未运行服务器任务、未改动历史模型。用户要求快速收尾后，停止扩展范围。

## 已修复的实际差异

| 问题 | 源码依据与修复 |
| --- | --- |
| 收藏家 A19+ 大减益仅施加 3 回合 | `monsters/city/TheCollector.java` 构造函数将 `megaDebuffAmt` 设为 5；native 改为 A19+ 5、低升阶 3，并测试 A0/A18/A19/A20 |
| 时间吞噬者减益顺序反转 | `monsters/beyond/TimeEater.java` 的 `takeTurn` case 3 先易伤、再虚弱、最后脆弱；修复后一层人工制品正确抵消易伤 |
| 致死攻击提前消费 AI RNG | BronzeAutomaton、Donu、Deca、SpireShield、SpireSpear 的 Java `takeTurn` 在伤害后排入 `RollMoveAction`；对应六种攻击现在延后随机数消费，死亡时不再执行；盾的降力量也排到伤害之后 |
| 匕首召唤战检查点无法恢复 | Reptomancer 使用 0/1/3/4 四个匕首槽，但加载器只接受 1/4；放行实际合法的四槽，保留首领槽 2 的拒绝。新增自然动作序列的逐步恢复及后续执行一致性测试 |
| 心脏战未计入 Boss 行为统计 | 补齐遭遇 `THE_HEART` 到怪物 `CORRUPT_HEART` 的对应关系 |

最初的 12 项战斗回归中，旧 native 有 10 项失败、2 项通过；修复后这些用例全部通过。另检查了心脏 A18/A19/A20 伤害上限、死亡律动和下一回合上限恢复，未发现对应机制差异。

## 训练环境契约

顺序明确为：

1. `IRONCLAD_A20_ACT1`
2. `IRONCLAD_A20_ACT2`
3. `IRONCLAD_A20_ACT3`
4. `IRONCLAD_A20_HEART`

四阶段均为战士、A20、自然新局开始、可获取三把钥匙。Act1/Act2 在目标 Boss 击败后停止。A20 Act3 必须通过两个 Boss；有齐钥匙时 native 会立即进入第四幕地图，Act3 阶段在这个边界停止，不执行第四幕选择。Heart 阶段继续打矛盾精英与心脏；无钥匙的第三幕结局不能计作心脏胜利。

新增第二幕和第三幕 profile，训练 `single-stage` 入口支持上述四阶段；导出目标随 horizon 为 ACT1/ACT2/ACT3/HEART，不再一律写 ACT1。后续阶段按通关数选择模型。

跨阶段需要独立输出目录，在配置的 `run.profile` 与 `stages.train.profile` 使用同一目标 profile。`warm_start.transfer_kind = "curriculum-stage"` 仅允许相邻 A20 阶段；同时必须指定父 checkpoint 路径、SHA256、真实父步数，以及大于父步数的累计训练目标。复制模型权重，重建 Adam、环境、循环记忆与随机状态，不冒充精确续训。既有 Act1 optimization-experiment 流程保留。

本轮没有臆定后续训练步数、创建长训练作业或把未产生的 Act2/Act3 权重填成来源。每个新阶段仍须通过原有 preparation / benchmark / preflight 门。

## 验证与边界

- native 使用本地固定工具链重新构建。
- 收尾前全量测试：765 passed、1 skipped；跳过仍为旧模型编码不兼容，4 个 warning 为已有显式环境重绑定测试。
- 最后发现并修复匕首检查点问题后，61 项针对性测试全部通过；63 类 A20 遭遇共完成 18,495 个动作、1,245 次检查点恢复，零失败。结果保存在 `local/audits/2026-09-22-a20-simulator/`。
- 本地 CUDA preflight 通过（decision invariant、exact resume、seed 8335 均 PASS）。该检查使用默认 A0 FullRun 配置及本地 RTX 5070，不替代 A20 阶段或 NUS A100 的资格验证。Ruff 与 Git 差异空白检查通过。
- 遭遇抽查每类 4 seeds，共 252 场，使用 5000 HP 与随机合法动作，每场最多 120 动作；这是状态和检查点不变量检查，不是胜率或所有游戏分支的等价证明。首次抽查准确复现了四个 Reptomancer 恢复失败。
- 课程结构测试跳过战斗、验证路由与终止；战斗规则另用真实 native 测试。三种后续 horizon 均完成了小型真实 PPO 启动、恢复、评估、导出回归。
- 反编译投影可读，但其 manifest 指向的原始 JAR 在本机记录路径下已不存在。本轮没有重新校验该 JAR 或启动原游戏，不声称完成字节码或实机重验。
- native 语义已改变，旧 native 哈希对应的 benchmark/preflight 不再作为新环境资格证据。本轮没有加入兼容性白名单；历史 Champion 的成绩需在新环境重新评估。
