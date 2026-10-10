# Giant Head A20 后期攻击

全范围恢复后的新增义务，使用真实 TheBeyond 精英房间、三个新测试 seed 131200195–197。先核验合法 JAR 的 GiantHead、TheBeyond 等 class 摘要，再冻结 `fullrun-giant-head-late-r1.json`。这些是受控高 HP 机制测试，不是自然完整局。

三个场景各执行 12 次 end turn，原版自然进入 It Is Time。稳定边界意图依次出现 40、45、50、55、60、65、70，随后仍为 70；前几回合随机普通动作也纳入差分。39 个边界的状态、合法动作和 RNG 全部 HARNESS_MATCH，未修改 native 规则。原版最终 HP 分别为 462、462、449。

新增原版派生回归 `test_giant_head_stock_late.py`，逐边界检查实际 HP、攻击伤害、意图、RNG 及 checkpoint 恢复。与既有 Slow 和 Oracle 构建测试共 16 项通过。非攻击诊断伤害的 stock -1/native 0 按无攻击语义映射；真实攻击伤害没有宽松容差，完整原差分仍保留。

执行 Oracle 1.3.27 SHA `4ebd1abfd0aa18c3db182dbbf977faf75d44f8fb3b8b52e8ca657dd0d5a16864`；native 为 c07116cc…。本次独立核验 63 项保护文件恢复，0 差异。完整原始证据、重放和恢复日志在 `local/audits/fullrun-parity-20261007/giant-late*`，原版字节码不上传。

本项补齐后期攻击增长和上限分支，不把 Giant Head 的所有卡牌/遗物交互或整个精英胜利流程升级为认证。
