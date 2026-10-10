# 绿钥匙完整动作回放：语义匹配与保留的原始差异

对 Oracle 1.3.46 的原版 R2 capture 执行 CPU native 回放：胜利案例30动作/31边界、死亡案例1动作/2边界。两个案例的33个边界在所列比较范围内全部匹配，每个边界恢复后执行完整剩余后缀也得到同一最终 native 状态。Gremlin Leader 的原始 battle serializer 仍有27个边界差异，不能写成全状态逐字节匹配或完整A20H认证。

## 独立构造及明确条件

native 生产源摘要仍为 `6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0`，未修改生产规则。由实际幕/楼层计算五条 room RNG 初态；原版独立确认的当前 Map.cpp 探针提供燃烧坐标与buff，再直接调用 native 战斗构造器。没有复制原版生成的怪物、HP、powers、手牌、抽牌顺序、能量、胜负或奖励。

三个部分是诊断条件，不是本轮认证内容：识别原版实际精英组合后指定同一 encounter；对齐持久 RNG；对齐五个有序遗物池。由此能检查同遭遇、同资源、同持久时钟下的战斗及奖励行为，不能认证从 Neow 到此处的自然遭遇分布或历史资源生成。

回放工具校验 capture/manifest/Oracle build/launch 完整性，要求原版恢复 journal 已完成；重新校验 map 探针源摘要、可执行文件摘要和实际输出；拒绝 stale native binary、覆盖已有输出和非唯一动作映射。CPU命令及子进程禁用CUDA。

## 匹配范围

| 检查 | 结果 |
|---|---|
| 完整策略 Observation及合法动作 | 33/33 |
| HP/maxHP、gold、牌组、遗物、药水、钥匙资源 | 33/33 |
| 14条既有 RNG 与5个有序遗物池 | 33/33 |
| decision终止及动作产生的 reason/success | 33/33终止标记，31/31动作结果 |
| 胜利奖励：金币、遗物、卡牌、绿钥匙 | 2/2奖励边界（取钥匙前后） |
| FullRun checkpoint逐边界原样恢复 | 33/33 |
| 每个checkpoint运行完整剩余后缀 | 33/33 |

奖励实际生成的是32金币、Matryoshka、Havoc/Entrench/Cleave及绿钥匙。原版实际取钥匙后，native对应动作、flag、剩余奖励及RNG相同；死亡案例得到相同DEATH/success=false。上述奖励是期待值，未从stock复制到native初态。

## 两个回放工具问题与版本历史

R1 首边界不匹配源自错误解释受控牌组资源：原版 `CardGroup.addToBottom` 字节码调用 `ArrayList.add(0, card)`，依次添加实际逆序。按这条独立原版语义构造native输入后，手牌/RNG一致。这也解释了上一阶段实际masterDeck首项AscendersBane，不能简单认为是排序。

R2 在边界6误报HP=367对400：休息/宝箱资源投影读取FullRun非战斗player_state，而战斗中真实HP在public_combat.player。修正诊断投影读取活动战斗HP；没有修改native战斗规则。R1/R2输出保留；R3扩展前初步语义通过，R4补遗物池和奖励，R5补终止原因与完整后缀恢复，R6补全部原始直接投影差异。

## 原始差异及不支持项

胜利轨迹原始direct battle投影在29个可比较战斗边界中有27个不一致，第一处是边界2死去minion仍保留 native Metallicize/Minion powers，而原版清空。后续涉及原版持续保留的尸体列表与native重用槽位。完整差异未删除或手动归零。现有生产公开投影、合法目标和这条完整后缀匹配；这不能证明所有其他动作分支、死亡触发和召唤组合都正确，需要专门的最小反例继续验证。死亡案例2个battle边界原始投影一致。

胜利后的两个reward边界 native 已释放独立battle serializer，标明该原始投影不可比较；按FullRun公开资源/奖励/RNG及恢复检查，不伪造战斗尸体状态补足。

只覆盖Metallicize。其余三种强化、后续幕已持绿钥匙、自然遭遇构造与连续Act3/Act4仍待验证。诊断库保持training_eligible=false；不新增checkpoint兼容白名单，不更换服务器发布或恢复旧训练。

## 复现与回归

入口 `tools/replay_green_key_archive.py`，输入、CLI、raw报告与校验源摘要见 `green-replay-evidence.json`。冻结回归 `tests/fixtures/regressions/green-key-stock-r2.json` 的公开状态/动作哈希与资源/RNG期待来自独立原版capture，未用native输出自证；native受控初态与31个实际动作bits作为重放输入。回归遍历33个状态及全部checkpoint完整后缀。

DL/CUDA禁用下全量1628 passed、2 skipped、4 warnings（145.42s）；随后最终CLI输出和原始direct诊断实际重放通过，相关8个测试及全库Ruff通过。跳过项为导出Act1 policy缺失及CUDA；警告来自既有checkpoint重绑定测试。原工作区197个待提交文件/status保护快照不变，所有游戏运行journal均恢复；本轮没有再次启动原版游戏，没有修改原工作区产物或训练发布。

下一步优先扩展原版独立seeds的Strength/HP/Regeneration案例，验证额外HP的独立构造与奖励；再连接已持绿钥匙后续地图和Act3/Act4连续流程。当前证据不足以改变训练发布身份或宣布A20H稳定通关。
