# 连续 Act4 失败路径与 Heart 长战斗

日期：2026-10-10。独立 worktree、DL / CPU；原版 JAR SHA `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`，Oracle 1.3.51 JAR `75379f4d1930ebfee58849be4445c245ac645bfd86c2abadf3f3bbb5c2c8f7c7`。沿用 Act4 地图修复后的来源 `e4f8452ff3cea688e49a362488814d4c2e29d9b721bcc6cd71c8d9f1d9fa733a` 和独立二进制，本轮不改生产适配层、C++ 规则、模型、奖励、PPO 或恢复契约。

## 实际执行及结果

新 namespace `[131200450,131200451)` 已检查冲突。仍从明确声明的 Act3 强牌组、三钥匙和双 Boss 初态开始；此后通过真实原版动作连续进入 Act4，休息、进入并离开商店、击败 Shield/Spear、处理奖励，再进入 Heart。只在公开敌人身份为 CORRUPT_HEART 时选择合法 END_TURN；不写 HP、怪物、强化、房间或终点，不强制死亡。

实际 **29 个 Act4 决策**，其中 **17 个 Heart END_TURN**，在第 17 回合真实死亡，原版 horizon `DEATH / success=false`。并未达到 128 决策上限；上限分支仍独立标为 unfinished，不记游戏失败。初始 HP/强牌组属于受控探针，不是自然训练状态或胜率样本。

Heart 公开边界实际见到：起始 BeatOfDeath2；第 5 回合 Strength2/Artifact2；第 8 回合 BeatOfDeath3/Strength4；第 11 回合 Strength6/Painful Stabs；第 14 回合 Strength18；第 17 回合 Strength70。Invincible 在这些边界为 200。本例不攻击 Heart，因此不证明强化后出牌的死亡律动伤害或 Invincible 消耗；只验证记录的强化状态、正常敌方攻击、状态牌/牌堆变化和随后的死亡路径。

## 对照和完整恢复

重新核验 sealed Oracle JAR/member、场景、实际双 Boss 和 Act4 入口，成功完成及恢复日志，然后以当前来源身份生成新 native 入口/flow，未修改旧报告。**29/29** 决策的完整公开 Observation、所有合法动作、所选动作、有效 RNG、terminated/truncated、success、reason 均一致。新增回归还对每个 transition 的 base reward 与原版一致性做断言；不是 shaped return 或策略 critic 的新分析。

从 **30/30** fullrun checkpoint（含实际死亡终点）恢复后，执行全部剩余原版记录动作，逐边界完整 native snapshot 严格相同。加上此前 seed 131200440 的 19 步胜利路径，现在是两个受控连续案例，48 个决策、50 个 fullrun checkpoint 完整后缀。不是 50 个独立游戏或正式胜率。

## 原始战斗差异保留

另从 fullrun 的完整 combat_checkpoint 加载 battle，只在两端都有 combat_state 时独立比较 raw direct combat 投影。**21** 个可对齐边界中 **19** 个严格相同、**2** 个不同；其余 **9** 个缺少成对战斗态的界面/终点不记通过。

boundary 6、7 的死 Shield：原版 powers=[]，native 仍有 Artifact2。与此前孤立探针的 Back Attack 死亡 residue 不同，不能套用同一清理时点解释或一般尸体豁免。完整投影、差异和原始 capture 都保留；公开输入匹配不等于原始状态完全一致，也不能证明所有尸体/复活机制安全。

## 限制及下一步

商店只进入并离开，未执行买牌、买药、买遗物或移除；Heart 被动路径没有卡牌/药水触发交互。下一步增加实际商店交易和更长、有攻击/防御决策的 Heart 轨迹，继续把公共契约、原始状态、终止与恢复分层检查。自然构筑、训练分布、循环记忆和正式模型胜率仍须独立评估。

CPU 全量 **1714 passed / 2 skipped / 4 warnings**，179.27 秒；Ruff 通过。跳过为本地导出策略缺失和 CUDA 不可用，警告为现有 checkpoint 来源重绑定测试。耗时仅为本次测试日志，不是性能基准或加速结论。

本轮 `training_gate=NOT_QUALIFIED`、`training_eligible=false`、`natural_trajectory=false`。原工作区 197 个待提交文件、status 与游戏运行目标恢复均核验保持不变。没有启动长训，服务器任务未干扰。版本、测试和封存清单见 [ending-continuous-death-evidence.json](ending-continuous-death-evidence.json)。
