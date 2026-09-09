# Act1 环境收尾验证（2026-09-09）

结论：当前环境、单阶段 Act1 配置和启动工具已完成本地验证，可以提交新的 5M 实验准备作业。服务器必须在 compute node 通过实际网络、选定 worker 布局和 checkpoint 恢复验证后才进入长训练。这不是全游戏、全分支 parity 认证。

## 本轮最终收尾

- Act1 profile 升为 version 4：钥匙可正常选择；纸条自动离开；棱彩碎片保留生成与展示，仅禁购/禁领。网络和输入编码仍为 v5，旧 checkpoint 不冒充 exact resume。
- 棱彩碎片只出现在 shop relic pool；正常 tier 抽取和 fallback 不流入 SHOP。Neow、Calling Bell 及事件自动遗物路径使用普通/罕见/稀有/Boss tier 或固定指定遗物，没有发现标准 A0 Act1 强制获得该遗物的路径。对异常已持有状态报错；不重抽奖励。
- 自然实机重放发现并修正 Calling Bell 的隐藏卡牌生成：原版在 Neow 中打开奖励界面后再清空奖励，仍消耗 card RNG 和稀有度状态；模拟器现在同样生成并丢弃，不展示给模型。
- 净化泉的出现资格改为原版 `AbstractPlayer.isCursed`：不把 Ascender's Bane、Curse of the Bell、Necronomicurse 算作资格。先前多加入净化泉，改变了事件抽样池；没有修改全局诅咒计数或遗物战斗效果。
- 修复后实机 seeds `3000000000025`、`3000000000047` 共 **81 个自然动作边界**，界面、HP、金币和牌组一致，包含修复后的奖励序列和纸条自动离开。没有使用跳过战斗开关。**63 个保护路径已恢复并逐一核对 hash**。
- 回归测试覆盖纸条自然动作重放、手动/自动离开等价、事件 checkpoint 恢复、钥匙代价、Calling Bell 后首战奖励和净化泉资格。完整测试 **604 通过、1 跳过**，两条 warning 为预期 runtime-rebind 用例。
- 本地 RTX 5070 Laptop GPU 上实际网络短 preflight 通过：循环记忆 256、一次更新后保存/恢复，再更新的指标与权重一致。该短测使用一个 worker、缩短 rollout；服务器还会验证选定布局与完整 rollout，不能拿本地短测替代 A100 结果。
- 新训练入口已验证随机初始化、真实 PPO 更新、soak/信号中断、续训、补做中断评估、best 同分保留、final 和独立策略导出。准备或 benchmark 失败时禁止 exec 长训练。
- 修复了训练配置副本未写入的问题；inspector 会跳过训练 checkpoint，不再因安全权重加载拒绝 checkpoint 中的 profile 对象而中断模型列表。

新增证据在忽略目录 `local/audits/act1-training-closeout/`：`live-corrected.json`、`launch-corrected.txt`、`summary.json`、`native-build.txt`、`local-preflight.json`。最初未通过的实机批次也保留，用于定位 Calling Bell 与净化泉差异，不能作为通过证据。自然动作前缀已作为回归 fixture 提交，不含模型权重或游戏 JAR。

## 上一批事件投影验证的证据


实际启动本地原版游戏，通过 CommunicationMod 和更新后的 Observation Oracle 获取公开状态；原版事件使用 `EventHelper` 构造，模拟器探针接收构造前相同 RNG 状态。比较适配后的选项实体、卡牌预览和语义动作。没有加载旧模型做大规模评估。

- Neow seeds **0、1、42、100**：四组公开奖励/代价投影一致。
- **9 类事件**：World of Goop、We Meet Again、Dead Adventurer、Scrap Ooze、Knowing Skull、Falling、N'loth、Note for Yourself、Designer，选项/预览/语义动作全部一致。
- 前 **8 类事件**各执行一个代表性选项，结算后的 HP、最大 HP、金币、牌组、遗物、药水一致；Knowing Skull 额外重复交血换金币，结算和递增成本一致。
- Designer 本次只验证服务选项投影；随机升级扣费的结算仍以原版反编译代码和已有 native 回归测试为证据。不能将这一项写成实机分支全通过。
- Act1 结构测试覆盖 seeds 0、3、4，对应史莱姆 Boss、守护者、六火亡魂：目标幕完成即终止、无后续合法动作。这里用跳过战斗的测试开关检查终止流程，不评估战斗水平；正常 backend 默认不开此开关。
- 全套测试 **591 通过、1 跳过**。跳过的是新编码不兼容的历史模型诊断；两条 warning 来自预期的 runtime-rebind 测试。
- native 已重新构建，当前源码摘要与内嵌摘要匹配。源码摘要为 `bcf25fb89c85f0c58a896bcbaa9bc2207866850ea8533b0343670eb12edad1ab`。
- 原版审计会话结束后，备份涉及的 **63 个路径**全部恢复并核对；没有修改历史训练 checkpoint。

## 这次修正了什么

1. 纸条卡牌预览原先遗漏了其他卡牌共有的显式布尔字段。即使值为 false，字段是否存在也影响模型输入。Oracle 现在复用 `CardStatePatch`，两端对同一张牌给出相同字段。
2. Falling 的旧通用阶段读取可能先取得父类 `screenNum=0`，而没有读取实际的私有 `screen`。Oracle 现在提供明确阶段，适配器优先使用它；无可删牌时的离开编号也增加了回归覆盖。普通 Falling 决策已实机验证；无可删牌边界在本次由回归测试验证。

第一次纸条比较显示原版账号存的是「疑虑」，模拟器默认「铁斩波」。这是不同账号起始条件，不是卡牌生成错误。将模拟器纸条条件对齐为「疑虑」后，预览和交易结算一致。

旧场景探针强行从战斗切换事件后，自动返回地图曾触发原版 `AbstractPlayer.releaseCard` 空引用。该轮证据没有作为通过依据。最终每个事件从独立运行构造，结算比较停在选项执行后的稳定边界，避免跨越带有人工场景残留的地图 UI。此验证不能替代自然局完整实机 canary。

最终实机日志中的 BaseMod 缺少控制台历史文件，以及原版 Neow 对 `NONE` drawback 打出的 `[ERROR]` 文本，不是本次失败：前者是可选历史文件；后者来自原版无代价分支的日志输出，实际奖励继续执行。最终批次正常完成。

## 与真实游戏不同的实验设定

| 设定 | 对训练与解释的影响 |
| --- | --- |
| Act1 Boss 击败即胜利终止 | 有意缩短任务；不让模型决策战后奖励、Boss 遗物或 Act2。 |
| 不获取棱彩碎片（Prismatic Shard） | 限定受支持构筑范围；不能宣称任意原版跨色牌组均可迁移。其余内部池保留，避免仅为过滤内容改动 RNG 池。 |
| Act1 正常选择钥匙 | 按原版支付代价，保留燃烧精英；Act1 不给钥匙额外奖励。历史其他非 Heart profile 的过滤行为仍保留。 |
| 指定 seed 的完整 Neow 开局 | 与本次原版 seeded-run 对照一致；不学习非 seeded 模式下依赖账号上一局表现的受限祝福。 |
| Act1 纸条自动离开 | 保留事件池与发生概率，不交换、不修改跨局存牌。交互式探针仍能显式配置卡牌，用于原版对照。 |
| 秘密传送门使用固定资格设定 | 不以训练机器运行速度模拟人的游玩计时；只影响晚幕，与本轮 Act1 无关。 |
| 折叠纯继续/确认 UI | 模型步数不等于原版点击数，计算速度和轨迹长度需据此解释。 |

## 允许开训的边界

环境层面可以开始新的 Act1 试训练，无需继续无限增加审计 seed，也无需为了这些修复重写网络或 reward。胜率应明确标为“上述设定下的战士 A0 Act1 胜率”。

当前配置和操作流程见 [Act1 5M 训练说明](training-act1-5m.md)。`train --prepare` 会自动执行必要的服务器验证、完整更新 benchmark 和 worker 恢复检查，通过后开始训练。5M 是首轮实验预算，不设“先通过 99% 胜率才能训练”的循环门槛；目标胜率需要实际评估证明。历史 10M→15M 配置不属于本轮启动流程。

原始实机载荷、执行脚本、构建日志、测试输出和带 SHA256 的摘要保存在忽略目录 `local/audits/act1-closeout/`。最终证据为 `live-effects.json`、`launch-effects.txt`、`summary.json`、`tests-final.txt`；早期失败或不对齐的批次保留以供追溯。验证用 Oracle 为该目录下的 `SpirecommParity-verified.jar`，源代码位于 `native/oracle/`。
