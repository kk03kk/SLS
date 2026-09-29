# SELECT_CARD 编码故障与安全恢复

## 故障证据和根因

维护者报告：886247 完成准备、512 局基线及 PPO update 1 后，编码报错
`ValueError: unresolved action subject_id: select-card:5`；886248 等待
`afterok:886247`，原因为 DependencyNeverSatisfied。本地未访问 NUS，
未检查服务器 checkpoint 或失败局面 dump。

发现并复现了匹配该错误的适配缺陷：原生 REWARDS 的 idx2=5/6 分别为
Singing Bowl/跳过，不是卡牌位置。`GameAction.cpp::executeRewardsAction`
对 idx2=5 增加生命上限；原版适配器同样输出 TAKE_SINGING_BOWL。
普通 COMBAT_REWARD 映射正确，但 standalone Neow/Dream Catcher 分支
没有处理 5，错误生成 SELECT_CARD(select-card:5)，而公开列表通常只有
三张奖励牌，因此模型拒绝无法解析的引用。

仅凭堆栈不能证明服务器具体发生在涅奥还是捕梦网页面；已证实这个
映射缺陷可精确产生所报异常。没有放宽模型引用检查或加入伪造卡牌。

## 审查范围和修复

- standalone 卡牌奖励统一映射特殊操作 5/6；普通选牌索引必须在公开
  卡牌列表范围内。Singing Bowl 使用 option_id=reward-card:0，和原版一致。
- 审查一般网格选择的 TRANSFORM、TRANSFORM_UPGRADE、UPGRADE、REMOVE、
  DUPLICATE、OBTAIN、BOTTLE、BONFIRE_SPIRITS：idx1 是可选列表索引，
  与牌组 deck_index 不同；真正存在的 select-card:5 仍是合法卡牌。
- 战斗选择使用 CHOICE:<source>，与网格 select-card:<index> 分开；
  已选卡牌使用 SELECTED:<order>。现有模型/选牌检查保持通过。
- 新回归精确重现旧 SELECT_CARD(select-card:5) 的编码异常，然后验证
  两类 standalone 奖励的完整动作集能够编码；另覆盖八类网格选择。
- 服务器 preflight 加入同一类特殊奖励操作的编码检查，避免完整基线
  之后才发现这个分支。该检查没有依赖服务器安装 pytest。

新 native source SHA256：
`1e30bb6cfa32f600cd63c59983c14ab8928fac4452c1586d630295d2c6ff9453`。
本地 native 已重建，模型、适配器、奖励和 Slurm 相关 110 项检查通过；
Ruff 通过。未做长评估、未启动新训练，未宣称完整规则一致性认证。
模型编码字段与版本保持 v5，但适配语义及 native source 身份已变化，
所以失败实验不会按精确续训恢复。

## 恢复策略

1. 检查 886248 仍为 PENDING 后取消这个无法满足的依赖作业；保留失败
   886247 的全部日志、manifest、metrics、checkpoint、准备报告。
2. 同步修复源码。两个新配置为 `*_plateau_progress_2m_r1.toml` 和
   `*_plateau_win_2m_r1.toml`，运行和准备目录均以 `-r1` 结尾。
3. 新目录必须不存在。两实验都从原始 56M checkpoint 哈希
   `9555c8608155ba262901757cd57d76f854f1cd76a5b6126375e832fa0a714710`
   重新迁移权重，重建优化器、环境、随机流、循环状态。
4. 计算节点重新构建、预检及完整 512 局基线；进度奖励先运行，通关奖励
   使用新进度作业的 afterok 依赖。不要引用旧 886247。
5. afterok 表示 Slurm 作业成功退出；训练信号处理可能安全保存后退出零。
   比较结果前仍须检查两份 manifest 的阶段 COMPLETE、达到目标步数和
   final-evaluation.json，而不能仅凭 Slurm COMPLETED 判定实验预算完成。

新配置仅改变输出和准备目录；训练参数、父模型、种子和预算保持相同。
不要删除旧目录、覆盖 latest.pt、移动唯一证据或在服务器使用 git clean。
2026-09-28 的诊断配置仍绑定旧 source hash，是历史记录；若要在修复后
重做独立诊断，需要新配置身份和新输出，不直接重复旧配置。
