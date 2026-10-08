# 20M训练发布前结论

可以提交带强制启动门禁的NUS作业。实际20M训练只在同一次allocation中的真实模型与恢复验收全部通过后开始。尚无本轮服务器训练结果，也不宣称critic预热已经有效。

## 为什么现在可以进入训练

本次训练范围是正常Neow开局A20 Ironclad的Act1+Act2联合通关。当前发布环境从已审阅b100来源提取两项必要共享修复：全盗贼逃跑药水奖励，以及跨幕cardRng严格区间边界。未混入本地晚幕整体修改、序列化优化、吞吐实验或新的PPO改动。

发布来源6efbb772958c06d1b9133f374d046d8838eaa256da1a146eaf7e1ee530eefd3d独立构建成功，并重放原版记录：63项有效受控战斗（3条旧Oracle错误初态保留失败，由修正捕获替代）、36项共享机制、9个完整系统分支、6条正常Neow轨迹共1990边界严格匹配。15项实际TheCity盗贼奖励与全部14条RNG比较通过。22项Act1→2/Act2→3合成边界探针通过；它们以原版字节码为规则依据，但不冒充原版自然轨迹。系统有两条未完成前缀，单独记录，不计完整通过。

已知Discovery计时依赖仍在：seed8000011000000与8000011000009的首个差异和此前来源完全相同；后者只有采用被动原版日志记录的15次更新时才匹配334边界。不把条件匹配写成无条件等价。其余未覆盖内容继续登记；没有新增未解释阻断差异。Act3、钥匙取得、Act4/Heart的完整认证和正常晚幕见证不作为本次Act1+Act2预算的无限前置。

## 固定配方与身份

冻结90M父checkpoint SHA274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0，显式weights-only迁移至新环境，保留全部模型权重，重置Adam、环境、RNG、seed游标和循环状态。不从94M终点exact resume，也不加入来源转换白名单。新环境改变了奖励/RNG语义；历史lambda结果保持旧身份，不能作同环境或20M预算匹配control。

总新增预算20,000,000 decisions，最多超出不足一个16384-decision rollout。前32个rollout=524288 decisions计入预算，冻结actor/encoder/GRU，仅更新value_head LayerNorm/Linear，以实际collector shaped reward的完整终止轨迹MC return为目标，gamma1、2 epochs、batch≤1024。未终止目标缓冲在预热结束时记录并释放；实际环境/记忆/RNG继续进入PPO，不排空环境。随后沿用control PPO lambda.98、LR3.125e-5、64workers/16shards、rollout256、sequence64、epochs2。相同Adam保留value-head状态，其他参数恢复训练。

这是一个完整新配方，不是预热优于普通PPO的因果证明。历史4M结果没有支持继续机械延长lambda1；稀少联合成功及跨任务critic校准风险使当前有界预热方案值得检验，但其效果尚未验证。

周期每2M/512开发开局，checkpoint每1M；周期seed8000012000000起，终点确认8000013000000起4096开局。实际记录冲突扫描通过，服务器提交时再次扫描。固定20M终点与冻结90M在同runtime/新环境/同4096开局配对；净增至少1pp且精确双侧McNemar p≤.05并执行健康才记确认收益。无确认收益不降阈值或自动延长。选模按联合成功数，平局更早；固定终点与选中checkpoint分别报告。最终保留集9000000000000起4096开局封存。

## 验证与执行边界

本地只做纯Python/mock、源码审阅、单线程native构建、已有动作重放和短受控游戏检查；未执行模型、rollout、GPU、测速或真实PPO更新。轻量测试不能证明真实恢复等价。原版短批次及production隔离smoke均结束，独立63项文件恢复核验无差异。

Linux启动门禁v2重新验证37项共享规则探针、Boss后多选、父SHA与有限性、value-head更新后actor/GRU不变、独立完整回报，以及跨rollout/最后预热/首次PPO三个边界的采样和更新精确恢复。探针学习结果丢弃，生产从原始初始化checkpoint开始；失败保存证据并退出，不进入20M也不自动重试。checkpoint保持阶段v6，旧父v5只作显式权重迁移。

三段48小时allocation按afterok续接同一逻辑运行；仅完整完成或验证过的安全中断可继续。SIGINT、崩溃、损坏、不兼容拒绝自动续接，预算已完成则后续allocation退出。旧114.7 decisions/s下20M纯采样更新约48.4小时，启动门禁与评估另计；三段是保守规划，不能保证耗时。固定64:16服务器benchmark只用于预算保守估计，不改变布局或声称性能优化。

## 可追溯入口

simulator-qualification-r1.json记录逐范围资格、失败/条件证据与原始文件摘要；simulator-transition-r1.json绑定weights-only迁移与支持证据。原版JAR、字节码、原始载荷和日志留在D:/SLS/local，不上传GitHub。local-validation-r1.json记录本地验收与延期项目。
