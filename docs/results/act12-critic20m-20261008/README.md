# Act1+Act2 critic预热＋20M：当前启动入口

2026-10-08：已完成训练前有限收尾，准备提交带强制启动门禁的NUS作业。真实模型计算验收和训练尚未运行；不宣称预热有效。

本次发布更新了盗贼药水奖励与跨幕cardRng边界规则，环境来源为6efbb772958c06d1b9133f374d046d8838eaa256da1a146eaf7e1ee530eefd3d。冻结90M父模型仍从显式weights-only迁移初始化。旧aa63准备及其b100环境不作为本次启动提交。

完整当前说明见[训练准备与边界](../act12-critic20m-20261007/READINESS_20261008.md)、[本地验收](../act12-critic20m-20261007/local-validation-r1.json)、[有限模拟器资格](../act12-critic20m-20261007/simulator-qualification-r1.json)、[迁移契约](../act12-critic20m-20261007/simulator-transition-r1.json)。

总新增20M包含前524288 decisions预热，其余为lambda.98 PPO。目标是正常Neow开局A20 Ironclad两幕联合通关。固定终点4096开局配对冻结父模型；最终保留集封存。新环境下历史lambda结果不是严格同环境control，没有20M预算匹配对照，不将收益单独归因为预热。

原始首次准备说明保存在[修正前历史README](PRE_CORRECTIONS_README_20261008.md)，其中旧提交/启动命令仅作历史记录，不是本次执行入口。唯一启动命令由本次实际推送的提交绑定后交付。
