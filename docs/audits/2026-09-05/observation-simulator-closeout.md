# Observation / Simulator 收尾记录

用户于 2026-09-05 明确调整验收标准：整体检查、修复明显问题，工程判断差不多后停止。
本轮按此标准完成；没有把未完成的逐规则 parity 认证当作已完成。

最终复查覆盖公开状态到模型编码、动作映射与多选、战斗与跨房间状态、
奖励及终止边界、checkpoint 恢复与输入兼容性。复查当前实现及改动后，
没有再发现需要立即处理的新增明显大问题。

## 主要修复

- Power 所属对象进入模型，避免玩家与敌方同名效果失去归属。
- 动态卡牌伤害、费用相关标志、瓶装标志贯通不同卡牌入口。
- Blood for Blood 与通用升级费用语义修正。
- 多选支持任意子集及点击顺序；Sacred Bark + Liquid Memories 支持两张牌和较大弃牌堆。
- 多选期间的药水动作、已选状态、延后执行及连续选择状态修复。
- Lizard Tail 初始可用状态、遗物负计数语义、Ancient Tea Set 跨房间充能修复。
- Match and Keep 的两张牌引用与剩余次数进入模型。
- 已选卡牌内容、可变属性、来源及顺序进入 Observation，并参与恢复比较。
- Observation 2 / policy input v4 严格检查输入身份；旧编码不能静默加载。

## 最终证据

- 全量测试：403 passed, 1 skipped。跳过项先验证历史模型因旧编码被拒绝。
- Ruff、生成词表校验、Git whitespace 检查通过。
- 8 个 seed 的无模型流程检查：1,582 个决策点，11 种屏幕状态；8 条流程均到达终止状态。
- 43 次 JSON checkpoint 恢复及下一步决策比较全部一致；编码数值全部有限。
- 此流程检查实际到达 Act 2；Act 3/Heart 结构边界由已有结构测试覆盖，
  不能据此声称真实战斗遍历了所有后期内容。
- Native 已构建；维护的 Oracle 补丁编译为单独文件。未启动原版游戏，
  未运行大规模模型 evaluation，未修改历史训练 checkpoint 或日志。

本地日志：`local/observation-simulator-final-tests.txt`；
`local/audits/repository-20260905/final-integration-smoke.json`。
详细逐项证据见 `docs/observation-simulator-work.md`。

## 保留事项

选择操作/数量的显式编码、事件动态数值、非手牌费用重置时机以及完整逐规则
组合覆盖仍是后续待办。本轮结果不等于与原版绝对一致，也不支持零缺陷承诺。

本轮修改了模型输入：已有约 9M 的旧模型不能直接按原配置续训到新输入，
后续训练需明确选择新训练或显式迁移方案。Native 变更后，正式长训练前仍须
按根目录 AGENTS.md 在服务器运行新的 Preflight 和需要的 worker Benchmark。

当前停止继续扩展审计；保留全部修复与可复查记录。
