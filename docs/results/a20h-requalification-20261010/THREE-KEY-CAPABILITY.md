# 同一自然三钥匙前缀的五模型 CPU 续跑与本轮收尾

本轮按用户要求停止扩展实验。已完成一条原版验证前缀上的五模型、两种 horizon 诊断，
封存原始数据、执行源码、模型身份和派生分析；不启动新训练、不合并 main、不更新训练发布。
完整 A20H 的模拟器资格与稳定通关目标均未完成，后续自主推进暂停。

## 证据范围与版本

起点是 seed `131200532`，正常 Neow 开局、实际执行 74 个公开动作后，
Act1 floor7 MAP 的同局三钥匙状态。此前路线含公开选路/钥匙干预；不是任何模型的纯 greedy 开局。
第三次原版运行的 75 个公开、合法动作和有效 RNG 边界默认重放匹配，
这部分证据见 [STOCK-DISCOVERY-REPEATS.md](STOCK-DISCOVERY-REPEATS.md)。
续跑只在 native 执行，没有将它们改称原版续跑、正式胜率或已通过的生产课程。

每个模型从零记忆、实际完整公开前缀重建自己的 recurrent memory，previous action 是
实际执行动作的类型加一，previous reward 是实际 base reward。没有恢复历史 trainer、
optimizer、worker 或 RNG，没有把 native checkpoint 放入策略输入。
每次重建同时逐项核对 Observation、候选动作及其顺序，最终完整 native 状态与封存参考一致。
五模型分别在 HEART 和自身原训练 horizon 下续跑，最多 256 个新动作。
共 10 条相关续跑、1,225 个新动作；它们共享一个前缀，不是 10 个独立评估游戏。

|模型|原 checkpoint SHA256|步数|训练目标|
|---|---|---:|---|
|parent90|274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0|90,013,696|Act1|
|lambda098|4eab4cb154f17b9f48e81e160b799bd99ed1d58fcae9938f735b29aa26418098|94,027,776|Act2|
|lambda100|0aebb484bbb8d75fdf78fdf6b0871f86938887bc00e0281bfbcc67133d56c1e3|94,027,776|Act2|
|criticbest|a804334af0060351a5e83a946c37a42a1a30445d809b21de3a220f2617e82870|92,012,544|Act2|
|criticlatest|be05e515fc892e1b09a2d8a87e30cac430e2f295c7ce820f3f91c7f0a838693d|97,009,664|Act2|

当前分支 loader 的训练 checkpoint 契约仍是 v5；critic checkpoint 是 v6。
使用 `codex/critic20m-evidence-followup` 的既有 `tools/export_policy.py`，
版本 `ce7713673629f45381da5b195f2fab8dde833505`，只读导出五个新 policy artifact。
当前工具按既有严格 policy artifact 契约加载；没有新增 checkpoint 兼容白名单。
原 checkpoint 导出前后与收尾时 SHA 均一致，artifact 权重摘要与原 checkpoint 权重摘要一致。
各模型训练 native 不同于本次环境，全部明确标记为诊断迁移，不是训练恢复。

执行时基线 commit `8353364b3e3d87342526dc479e5d0f999082b139`，
实际工具另按精确源码字节封存，SHA256
`2f388369780ca0753d679be432e75192f313805464397168a7728b58e08e99a5`。
native source `e4f8452ff3cea688e49a362488814d4c2e29d9b721bcc6cd71c8d9f1d9fa733a`；
native binary `6ee14bf97312bab04b66fcb0a6767f52c4a688eeb0c1fcf5953b5ab8764f570b`；
training implementation `d31392af86ba43bfae1686193d1c91b9861b11ebbf224b141be38f771ff90096`。
全部执行 CPU、单 Torch 线程、`CUDA_VISIBLE_DEVICES=-1`，CLI 只接受 `--device cpu`。

## 实际结果

|模型|HEART 续跑结果|最深位置|新动作数|起点 value|完整 shaped return|
|---|---|---|---:|---:|---:|
|parent90|策略循环上限|Act1 floor17|116|0.443666|−1.079663|
|lambda098|实际死亡|Act1 floor16|99|−1.080336|−1.079663|
|lambda100|策略循环上限|Act1 floor17|117|−1.088132|−1.079663|
|criticbest|策略循环上限|Act1 floor17|109|−1.044422|−1.079663|
|criticlatest|实际死亡|Act2 floor24|178|−1.073587|−1.079663|

循环上限是既有训练 episode 结束原因，不是游戏实际死亡。
三个模型的尾迹都在 Boss 后 CARD_REWARD 多选界面，重复 REMOVE_CARD，
保留了选中、取消及 selected_cards。criticbest 在 boundary179–182 反复选择/取消
同一张 BLUDGEON（`select-card:18`）；选中状态有 `selected=true` 和 `selected_order=0`。
不能据此断言编码完全看不见选择状态，更不能把单例直接定性为模拟器规则错误。
当前 raw 记录足以支持之后研究具体多选任务、策略记忆和选择完成条件；本轮没有添加逃逸动作。

五个 HEART 模型起点都有相同两个 MAP 候选，均选择 `map:2:7`。
已从现有日志核实下一 boundary75 的完整 SHOP Observation 和候选顺序仍完全相同，
但实际选择首先分叉：parent90/lambda100/criticbest 买 `shop-potion:0`，
lambda098 买 `shop-card:3`，criticlatest 离开商店。
因此后续 Boss 表现同时受构筑分叉影响，不能当作同一牌组的纯战斗能力比较。
起点完整动作分布几乎一致，不能用该 MAP 单状态的分布差异给五模型排名。
没有计算该 SHOP 的完整动作分布，只记录实际选择概率和 value。

原训练 horizon 的结果必须单独看：parent90 在 Act1 通关时正常结束，103 个新动作，
实际 shaped return `+0.9056097465509083`，不是 HEART 的失败回报。
其它四个 Act2 horizon 续跑分别为死亡/循环/循环/死亡，动作数与 HEART 对应项相同，
实际 shaped return 约 `−1.058330`；这来自不同 profile 的潜势，不能混称同一 value 目标。

## critic 与下一阶段判断

五模型此处原 PPO 均为 gamma=1、potential scale=0.2、failure progress scale=0，
保留既有 failure limit reward −1 和完整前缀累计的 episode limiter。
回报按每步训练 shaped reward 的 float32 值完整回算，区分原生终止、策略上限与诊断预算。
本次无诊断预算截断；工具对预算截断给出未完成、无完整 MC，不将其作为游戏失败。

gamma=1 且失败进度项为零时，潜势项在完整终止轨迹上望远镜消去，
同一 HEART 起点下，无论 Boss 死亡、循环退出还是 Act2 死亡，失败回报都几乎相同。
这解释了四个 Act2 模型 value 靠近失败 return 的现象，但单个 greedy MC
不是训练中的随机策略期望或带 rollout bootstrap 的 GAE 目标，不能用它证明总体校准通过。
原始 value 不是成功概率；parent90 的 HEART 差值也不能用于指控其 Act1 critic 失配。

本例支持先解决非战斗决策循环、拆分构筑与战斗诊断、补自然后段成功经验来源，
不支持只凭 critic loss 或更接近负回报就继续堆训练量。
criticlatest 进入 Act2 是一个值得后续复验的能力线索，不是 Critic20M 配方优越性的证据。
本轮没有改奖励、网络、PPO、训练分布或优势标准化，也没有接入课程。
恢复研发后仍需优先补自然 Act2–4/Heart 对照；原版 Discovery 的 14/15 更新运行时差异
继续保留独立身份，不能建立一般 RNG 忽略规则。

## 封存与验收

原始报告与日志位于 `local/reports/three-key-capability-20261010/`，包括每模型每 horizon 的
74 边界公开 teacher-forcing 历史、完整新动作公开日志、独立 native checkpoint 日志。
`closure-validation.json` 重新核验所有文件 SHA、模型身份、动作计数、完整回报、
同状态 SHOP 分叉，并从原日志派生带 selected_cards/public_context 的完整动作尾迹。
未对新增续跑的每个 native checkpoint 独立执行全部恢复后缀；不能声称它们已取得该资格。

实际执行源码 `executed-source.py` 不覆盖。收尾只修正工具 import 顺序，并补齐摘要尾迹里的
selected_cards/public_context，原始 report 与动作日志保持原字节，没有以新版工具名义重跑旧结果。
新增测试覆盖实际动作 teacher-forcing、previous base reward、状态错位、隐藏信息隔离、
终止奖励覆盖/float32、预算与循环语义，以及 CLI 拒绝 CUDA。

CPU 全量 **1758 passed、2 skipped、4 warnings**，136.69 秒；Ruff、词表与 diff 检查通过。
两项跳过分别为缺少约定路径上的本地导出策略、CPU 环境没有 CUDA。测试包含模拟训练 smoke，
不属于新历史模型训练，不会触碰历史运行目录。配置检查结果随交付证据封存。
配置历史上已有两份 lambda 来源身份不匹配，
不得把 31/33 改称全通过。原工作区的 197 个未提交文件、Git status、五份原模型不变，
原版运行备份 journal 全部 RECOVERED，无运行中的本地游戏。

下一步暂不执行。本轮无需用户操作服务器；收到服务器结果或明确恢复研发指令后再继续。
