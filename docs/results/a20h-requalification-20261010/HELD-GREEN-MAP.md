# 持绿钥匙后的换幕地图：静态规则与 native 续跑

后续原版实际构造器与地图 RNG 对照见 [STOCK-HELD-KEY-MAP.md](STOCK-HELD-KEY-MAP.md)。本文保留原静态/native 探针范围，不把新证据改称自然换幕。

日期：2026-10-10。独立诊断入口：`tools/probe_held_green_transition.py`；独立 C++ Map 构造探针：`tools/probe_key_map.cpp`。生产模拟器和恢复契约未改动。

## 原版证据与范围

从固定游戏 JAR `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673` 重新提取 `AbstractDungeon` 等 6 个类的 bytecode，保存类、反汇编和工具 SHA256。

人工检查 `AbstractDungeon.setEmeraldElite`：offset 0/3 检查 `Settings.isFinalActAvailable`，假时跳转 211 返回；offset 6/9 检查 `Settings.hasEmeraldKey`，真时也跳转 211 返回。否则遍历精英节点，在 offset 109–119 使用 mapRng.random(0, size-1) 选择节点，offset 131 设置 hasEmeraldKey。generateMap 在 offset 344 调用该方法。燃烧强化 RNG 的抽样在进入战斗时发生，不应混同地图节点选择。

脚本核验反汇编哈希及必要符号；符号存在检查本身不是字节码语义证明，上述分支结论来自逐指令检查。此轮没有运行原版换幕，不能将静态证据写成原版轨迹认证。

## 本地实际检查

以既有原版来源 boss reward checkpoint（seed 131100064）为模板，分别干预幕号及绿钥匙状态，在 Act1→2、Act2→3 生成 4 条 native 换幕路径。初始干预明确为 synthetic；其 RNG、遭遇列表和资源不是该幕自然开局的完整证明。

| 下一幕 | 持绿钥匙 | 实际燃烧坐标 / buff | 独立 Map.cpp 构造 | checkpoint / 下一动作恢复 |
|---|---|---|---|---|
| Act2 | 否 | 实际生成一个燃烧精英 | 匹配 | 匹配 / 匹配 |
| Act2 | 是 | -1 / -1 / -1 | 匹配 | 匹配 / 匹配 |
| Act3 | 否 | 实际生成一个燃烧精英 | 匹配 | 匹配 / 匹配 |
| Act3 | 是 | -1 / -1 / -1 | 匹配 | 匹配 / 匹配 |

独立 C++ 探针直接调用未改动的生产 Map.cpp，比较全地图文本及燃烧坐标/强化，不只是检查节点计数。公开 Observation 在持钥匙分支无 BURNING_ELITE，无钥匙分支恰有一个。恢复检查逐项比较完整 snapshot、合法动作及执行同一下一动作后的完整 snapshot。

已知派生字段 `derived_rng.map.assign_burning_elite` 可能保留旧幕生成标志。加载器按其重建地图后，用 checkpoint 保存的实际燃烧坐标和 buff 覆盖。此轮四个案例恢复一致，没有证据要求更改该恢复路径；不添加兼容白名单或静默重写旧 checkpoint。

CPU 定向验证 29 passed（包含新增换幕检查、既有 card RNG 换幕边界和五个原版绿钥匙回归）；Ruff 通过。此前版本的 CPU 全量 1633 passed 见 [GREEN-EXPANSION.md](GREEN-EXPANSION.md)，不把此前计数冒充新增版本全量结果。

## 尚未资格通过的部分

- 实际原版 Act1→2 / Act2→3 换幕地图采集，以及原版与 native 的地图节点选择 RNG 消耗对照。
- native 尚无 Settings.isFinalActAvailable 对应的恢复字段。默认 A20H 目标应处于已解锁环境；未解锁对照仍有行为缺口，不代表完整环境资格通过。
- 当前仅比较恢复后一个动作，不代表所有未来后缀；地图规则的静态条件也不能认证实际遭遇分布。

本轮数据 `natural_trajectory=false`、`training_eligible=false`、`training_gate=NOT_QUALIFIED`。输入及原始结果封存于 [held-green-evidence.json](held-green-evidence.json)。

下一步为隔离验证模式添加固定 namespace 的原版持钥匙/无钥匙地图采集，先证明原版构造与 RNG；之后再连接 A20 双 Boss 与 Act4。生产流程、训练发布和服务器任务保持原身份。
