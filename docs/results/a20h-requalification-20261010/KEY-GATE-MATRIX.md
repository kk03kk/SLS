# A20 三钥匙门槛：八种组合的实际原版结局

日期：2026-10-10。在隔离 worktree、DL、CPU 执行。固定原版 JAR SHA256 `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`；Oracle 1.3.49 JAR `69d48458ab8a96c74d8527b46aa7e327487dc8244c44be0fbb90eafa40b5f2ac`。生产 native 源码与二进制均未修改，分别是 `6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0` 和 `ce8f500b5116980f2bca854488da334cac58a679a1c1c59e3a8f70410890f3e8`。这些证据不重新绑定 NUS 历史环境。

## 方法和实际覆盖

新 namespace `[131200420,131200428)`，运行前在现有场景、工具、测试和配置中检查冲突。每例使用明确声明的初始钥匙组合、已解锁最终幕、受控强牌组和 Time Eater→Donu/Deca 顺序。在原版真实 TheBeyond 中构造初态后执行两个真实 Boss 战斗，通过实际 VictoryRoom 的四个 `choose 0` 对话选择取得结局；中途不重设钥匙、不注入终点。完整原始战斗边界、实际动作、对话历史、终点及构建身份均保存。

| 初始钥匙 | seed | 实际原版终点 | A20H 判定 |
|---|---:|---|---|
| 无 | 131200420 | Act3 GAME_OVER，victory=true | HEART_NOT_REACHED |
| Ruby | 131200421 | 同上 | HEART_NOT_REACHED |
| Emerald | 131200422 | 同上 | HEART_NOT_REACHED |
| Ruby、Emerald | 131200423 | 同上 | HEART_NOT_REACHED |
| Sapphire | 131200424 | 同上 | HEART_NOT_REACHED |
| Ruby、Sapphire | 131200425 | 同上 | HEART_NOT_REACHED |
| Emerald、Sapphire | 131200426 | 同上 | HEART_NOT_REACHED |
| 三把齐全 | 131200427 | TheEnding Act4 MAP | 非终止，尚未成功 |

所有缺钥匙案例的原版 HP 仍为 712/1000，不能把这个界面名称解读为战斗死亡，也不能把原版 victory=true 解读成 A20H 成功。从原版实际终点独立计算的 Heart horizon 与 native 一致。原版 SpireHeart 的独立 javap 证据保留最终幕解锁及三把钥匙共同决定分支的条件。

## 比较结果及保留差异

八例的 HP、maxHP、gold、钥匙、药水与槽位、完整公开牌组和遗物描述、act、floor、全部 RNG 对齐。**48/48** 初态及每个动作后 checkpoint 从当前 native 恢复后，全部剩余动作、逐边界完整 snapshot 与终点一致。公开状态的匹配仅指这里枚举的字段，不包含完整 FullRun Observation、策略循环记忆或 Act4 后续行为。

原始资源比较仍有 **88** 个差异：每例十张 Searing Blow 的 `special_data` 原版 0/native 30，以及 Burning Blood 的 inactive counter 原版 -1/native 0。新工具保存原值，并报告 `raw_resources_equal=false`。原版 SearingBlow 字节码显示升级通过 `timesUpgraded` 存储和递增；native Card::upgrade / getUpgraded 使用 misc。现有公开 card 特征明确不把 stock misc 与 native specialData 当作同一语义；公开升级次数均为 30。现有公开 relic counter 投影对 inactive counter 的表示一致。这些是公共投影证据及内部表示解释，不是删除差异或证明所有后续效果一致。

`comparison-r1.json` 和 `comparison-r2.json` 的严格原始比较失败均保留；最终 `comparison-r3.json` 明确使用 `ENUMERATED_FIELDS_MATCH_RAW_RESOURCE_DIFFERENCES_RETAINED`，没有新增恢复白名单或 raw equality 豁免。原版/native 战斗仍有八个死 Deca Artifact 差异，十六个 victory UI 边界未对齐，也保留为差异或不支持，不计通过。

新增八个冻结原版资源/RNG/horizon 回归和十一个对话采集器测试，包含钥匙变更、缺钥匙却进入 Act4 的拒绝，以及诊断超时不计游戏失败。CPU 全量 **1677 passed / 2 skipped / 4 warnings**，78.32 秒；跳过为本地导出策略缺失和 CUDA 不可用，警告为现有 checkpoint 来源重绑定测试。Ruff 通过。实际采集与 production Oracle 检查均完成；两个运行 journal 已 RECOVERED，原工作区 197 个待提交文件及 status 与保护快照相同，63 个游戏运行目标均恢复。

## 结论边界

已补齐**已解锁最终幕条件下，八种初始钥匙组合的门槛**。未证明钥匙的自然获取、完整自然路线、未解锁最终幕的 native 契约、全部 Boss 顺序的 Act4 分支、循环策略的界面历史或正式通关率。受控牌组为灼热攻击 +30，不是可用于训练的自然构筑样本。`training_gate=NOT_QUALIFIED`，`training_eligible=false`，`natural_trajectory=false`。

下一步是连接 Act4 休息、商店、Shield/Spear、奖励与 Heart，分别覆盖成功、死亡、时间上限及恢复后的完整后缀，先核验旧原版归档，缺少连续证据再补采。本轮无需 NUS 执行，保持服务器任务和训练恢复契约不变。版本与封存清单见 [key-gate-evidence.json](key-gate-evidence.json)。
