# 绿钥匙真实战斗采集 R2

本阶段完成原版真实燃烧精英战斗及取绿钥匙证据链，尚未完成 native 整场战斗、奖励、FullRun 恢复对照。受控强牌组是规则探针，不是自然开局轨迹、训练库或胜率样本。

## 版本与执行

Oracle 1.3.46 JAR SHA256 `4e05d182af0b21eb937afcdbe43d9e39cf08111794abb254a3bdfac1f9e23d8c`，原版 JAR `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`。新场景为 `fullrun-green-key-r2.json`，独立 seeds `[131200362,131200364)`；native 生产实现未改动。

原版 actual TheCity map 中已有的燃烧节点必须有完整根路径。冻结初始 inventory，调用真实 `onPlayerEntry()` 后调用真实 `AbstractPlayer.preBattlePrep()`，对应 AbstractDungeon.nextRoomTransition 的调用顺序。没有注入怪物、强化类型、胜负、奖励或取得钥匙标志，没有跳过战斗。公开动作选择后调用既有 OriginalBackend.step；它负责实际 wire 动作、异步效果完成与稳定边界。

首个场景 HP/maxHP=400、十张 Bludgeon 加 AscendersBane，遗物 Burning Blood/Necronomicon/Lantern。第二个场景 HP=1/maxHP=80、十张 Defend 加 AscendersBane，仅 Burning Blood，只结束回合。它们是受控初态，不宣称自然可达构筑。

## 原版运行结果

| seed | 节点 | 实际强化 | 动作数 | 实际终点 |
|---|---|---|---|---|
| 131200362 | (2,10) | Metallicize=6，全部初始怪物 | 30 | 实际战斗胜利后选择绿钥匙，动画完成，实际 flag=true |
| 131200363 | (5,5) | Metallicize=6，全部初始怪物 | 1 | 实际结束回合后玩家死亡 |

独立审计验证：初态 HP、楼层/幕/难度、首回合牌堆和意图、真实根路径、完整动作/边界数量、每步公开合法性及 wire 命令、最终取得或死亡事实，以及 capture/manifest/Oracle build/launch 恢复身份。两场 mapRng 恰好一次 random(0,3) 推进，unsigned 64 位状态与计算值一致；实际 powers 与对应分支一致。

追加库存审计验证牌组重数及 upgrades/misc、遗物顺序和 gold。实际首次公开状态中的 masterDeck 把 AscendersBane 列在前，与 setup 资源列序不同；完整实际顺序仍保留，库存审计仅证明重数与修饰，不宣称初始化/抽牌顺序已与 native 等价。原先要求资源与边界 masterDeck 顺序完全相同的审计检查拒绝了这两份记录，因此修正为明确的库存范围后另输出 stock-audit-r3.json，不覆盖初版审计。

独立 C++ 小程序直接编译当前生产 Map.cpp（没有复制地图算法或加载 stock 结果），两 seed 的燃烧节点和预抽 buff 都与原版匹配。因此“预抽与进场抽取时机不同”在这两个受控初始化中没有造成强化差异；不推广到所有地图 RNG 中间消耗或全战斗。源码入口 `tools/probe_green_map.cpp`，编译参数 `zig c++ -std=c++17 -O2 -target x86_64-windows-gnu -I native/simulator/include -include native/simulator/include/sts_common.h tools/probe_green_map.cpp native/simulator/src/game/Map.cpp -o <独立输出>`。实际测试产物用相同内容的 local 源编译，源码/依赖/编译器/产物摘要均封存。编译器报告既有 Map.cpp 变长数组 Clang 扩展警告，未修复或改变生产算法。

没有覆盖 Strength、HP、Regeneration 三个分支。HP 分支必须另做独立 base-HP 构造器对照；审计工具不会用实际 HP 自证额外 25% 正确。没有覆盖先拿遗物、已持绿钥匙后的后续幕地图或 native 后缀恢复。

## 保留的失败原型

R1（Oracle 1.3.45、seeds360/361）仅调用 onPlayerEntry，遗漏原版 preBattlePrep，捕获空初始手牌。两个实际死亡终点不可用于资格判定。R1 资源、JAR/build、完整原始 capture 与恢复 journal 保留；R2 使用独立场景 schema/seed/JAR/路径，不覆盖旧证据。新入口等待已初始化怪物意图、首回合手牌和稳定状态，防止把旧 actionManager 的 WAITING_ON_USER 当作新战斗准备完成。

## 隔离与后续

原版被动 mapRng 和当前节点 hasEmeraldKey 只在 validation 的 `_stock_direct` 证据中输出，没有加入既有14条_rng ABI 或生产策略输入。Oracle R2 的生产模式烟测通过。运行后全部配置/存档/模组目标按恢复 journal 核对；D:/SLS 的197个待提交文件及状态与保护快照一致。

下一步对这两份完整轨迹执行 native 独立构造、同动作回放和逐边界恢复，特别检查 Gremlin Leader 死亡 minion 的旧差异是否影响合法目标或战斗终点；不能因只影响显示而预先豁免。之后扩展独立 seeds 覆盖四种强化与后续幕持钥匙规则，再连接 Act3/Act4 连续流程。

CPU 全量回归 1624 passed、2 skipped、4 warnings（292.46s）；随后库存审计与入口边界测试追加验证 68 passed。Ruff 通过。跳过项为未提供导出 Act1 policy 与 CUDA；警告来自既有 checkpoint provenance/runtime rebind 测试。

证据摘要与原始交付包见 `green-capture-evidence-r2.json`，初版 `green-capture-evidence.json` 保留；原始目录 `local/reports/green-key-20261010/`。训练资格仍未通过。
