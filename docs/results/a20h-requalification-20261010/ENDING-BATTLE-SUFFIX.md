# Act4 战斗归档：构建核验及完整 checkpoint 后缀

日期：2026-10-10。在隔离 worktree、DL / CPU 中只读核验历史归档，没有启动原版游戏、GPU 或训练，没有修改生产 native。当前 native 源码身份 `6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0`；旧服务器环境保留原身份。

新增工具 `tools/verify_ending_archive_suffix.py` 对历史 Oracle 完整 JAR 与每个 member SHA、stock JAR、场景 manifest、capture 与 frozen fixture SHA、成功完成及恢复记录进行核验。逐例检查 seed、场景、实际 TheEnding dungeon/class、A20、幕、楼层、实际 Boss/Elite 房间、初始 RNG、受控初态及实际 resolved action。期望状态重新从原版 raw 边界提取，并与现有 fixture 比较，不能仅凭旧测试通过认定归档有效。

| 原版归档 | 案例 | 内容 | 证据性质 |
|---|---:|---|---|
| ending-interaction-capture-r2 / Oracle r12 | 12 | Heart 上限及 reset、强化；Shield/Spear 交互 | 受控孤立战斗片段 |
| ending-death-capture-r1 / Oracle r13 | 6 | 单个精英死亡后存活者行为 | 怪物初始 HP 条件化 |
| heart-loss-capture-r1 / Oracle r14 | 6 | 低 HP 出牌触发致命死亡律动 | 玩家 HP 条件化 |
| heart-lethal-capture-r1 / Oracle r15 | 6 | 致命出牌与药水的顺序、死亡及胜利资源 | Heart 初始 HP 条件化 |

共 **30 案例、102 边界**。Heart lethal 的六个终点只比较实际 outcome、HP/maxHP、药水及 RNG，不将无战斗态的终点记为完整战斗投影匹配。其他边界比较现有 raw direct combat 投影及 RNG，并保留所有差异；不是完整 FullRun Observation、编码或循环记忆校验。

三个边界仍有严格差异：seed 131200114–116 的第一个动作后，死 Shield 原版仍含 Back Attack1，native 已移除，Artifact2 两端相同。工具保留原始数组并记 raw_equal=false，不使用旧 fixture 的 declared_residue 作为资格豁免。随后原始边界相同不能自动证明所有召唤或目标行为安全。

从每个 native 边界恢复后执行**全部**剩余实际动作，逐边界比较完整 native snapshot，而非只验证下一步。**99 个可加载 checkpoint 全部严格匹配**；另外三例 Heart 胜利终点没有 combat_state，孤立 battle loader 无法加载，明确 supported=false / equality=null。这不证明 fullrun 胜利终点恢复有缺陷，也不证明其已通过，需连续 run 的独立测试。

新增 30 个回归同时检查原版 fixture 期望和所有可支持的完整后缀，保留尸体差异。初版工具在 Heart 胜利终点调用仅接受 combat_state 的 production_combat_projection 而拒绝；改为保留终点 snapshot，用独立 terminal_resources 比较。初版调用的失败原因记录在证据目录，原始归档未修改。早期报告与最终工具版本分别保留，不覆盖或重命名旧结果。

## 对下一步的影响

这些归档通过真实 Act4 战斗上下文核验，但初态通过 parity 场景设置。**没有从三钥匙入口连续走过休息、商店、精英、奖励和 Heart**，也不包含自然构筑、完整 Heart 强化周期或 win rate。不能把 99 个 native 后缀通过当作完整 A20H 通关证明；`training_gate=NOT_QUALIFIED`。

下一步优先补采连续转幕轨迹，保存完整公开决策历史和实际 UI 选择。每个原版/native 自动转场不一致的边界先标注，再检查策略循环记忆，不能用折叠 UI 步掩盖策略输入差异。连续轨迹需要覆盖 Heart 胜利与死亡，并用 fullrun checkpoint 执行所有剩余决策。无需服务器操作，保持训练和模型不变。

CPU 全量 **1707 passed / 2 skipped / 4 warnings**，82.38 秒；跳过为本地导出策略缺失和 CUDA 不可用，警告为现有 checkpoint 来源重绑定测试。Ruff 通过。原工作区 197 个待提交文件及 status 与保护快照一致，游戏运行目标保持恢复状态。版本、测试及封存清单见 [ending-battle-evidence.json](ending-battle-evidence.json)。
