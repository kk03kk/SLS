# 绿钥匙校验：来源与运行协议

本阶段已从实际 stock JAR 重新反汇编六个类，绑定 JAR、class、javap 工具及文本 SHA256。工具 `tools/capture_green_key_sources.py` 只读 JAR，不启动游戏；拒绝错误输入摘要及覆盖已有目录。原始反汇编在 `local/reports/green-key-20261010/stock-sources-r1/`，摘要见 `green-source-evidence.json`。这是静态证据，不是运行等价认证。

## 已核对的规则

1. AbstractDungeon.setEmeraldElite 仅在终幕已解锁且尚未持绿钥匙时指定节点。实际地图节点上的 hasEmeraldKey 是战斗/奖励规则依据，不凭界面文本推断。
2. MonsterRoomElite.applyEmeraldEliteBuff 在进入精英时从 mapRng 抽 0..3。四种增强分别为 Strength=act+1、Java Math.round(maxHP*0.25f) 的额外最大/当前 HP、Metallicize=2*act+2、Regeneration=2*act+1。增强作用于该场全部怪物。
3. addEmeraldKey 检查终幕可用、未持绿钥匙、奖励非空、当前节点有绿钥匙。绿钥匙取得必须经真实 RewardItem 与 ObtainKeyEffect，等待 Settings.hasEmeraldKey 变为 true 后才能报告完成。
4. RewardItem 双参数构造器虽然接受一个已有奖励，但只有 SAPPHIRE_KEY 分支设置双向 relicLink；EMERALD_KEY 分支不设置。不能将蓝钥匙/遗物互斥规则套在绿钥匙上。

## 两个待运行验证的差异候选

- native Map::fromSeed 在地图生成时预抽 burningEliteBuff；原版在战斗进入时抽取。若地图生成与入场之间没有其他 mapRng 消耗，预抽可能等价。需要被动记录实际原版 mapRng（独立私有证据字段，不加入生产策略观测或既有14流契约），逐 seed 比较生成结束、进场前后及 buff。单看抽取时间差不能确认为策略状态偏差。
- native GameContext::createEliteCombatReward 只根据燃烧节点坐标设置 emeraldKey，没有检查已持绿钥匙。必须区分合法自然流程、受控但可达初态和无法自然到达的重复访问；先证明实际可触发范围再决定修复优先级。

## 运行案例设计

第一组用真实根可达的燃烧精英节点，楼层随 Act/地图行推导，五条房间 RNG 随 seed+floor 初始化。冻结初始牌组、遗物、HP；强牌组允许作为明确标注的规则探针，之后不得修改怪物、动作队列、胜负、资源、奖励或钥匙。不得使用 skip_battles。

覆盖四种实际抽取的 buff，以及战斗胜利、死亡、绿钥匙先拿/遗物先拿、已持绿钥匙（合法获得后检查后续幕地图）。优先用独立 seed 正常生成所需 buff；如果没覆盖到某种，报告未覆盖，不覆盖实际抽取结果。已持绿钥匙的受控节点反例另列，不能当自然案例。

每个稳定决策边界保存公开完整历史、真实动作、HP/牌组/遗物/药水、怪物/意图/powers、14条既有RNG、独立mapRng、五个有序遗物池和奖励状态。由同一初态执行实际 native 合法动作，对照观测与动作；每个边界恢复后执行同一后缀，检查终点和 RNG。固定步数上限记未完成，不记战败。最终同场景封存 stock 类、Oracle 源/产物/manifest、native 摘要和原始捕获。

第二组连接 Act3 双 Boss、三钥匙检查、Act4 休息/商店/Shield-Spear/Heart。绿钥匙局部通过不替代连续路由证据。生产模式必须独立验证校验命令、地图RNG及场景 metadata 隔离。

## 本阶段验证

实际六类反汇编采集完成，工具的错误版本与重复输出保护测试 2 passed，Ruff 通过。没有修改 native、网络、奖励、PPO、checkpoint 契约或正在运行的发布；没有启动训练。下一执行步骤是实现受控战斗入口与只在 validation 模式暴露的被动 mapRng 证据，然后先跑一个案例端到端。
