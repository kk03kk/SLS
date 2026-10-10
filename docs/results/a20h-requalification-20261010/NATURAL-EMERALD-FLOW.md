# 正常开局自然获取 Emerald：燃烧 Gremlin Nob

2026-10-10，隔离分支、DL / CPU。生产 native 来源保持
`e4f8452ff3cea688e49a362488814d4c2e29d9b721bcc6cd71c8d9f1d9fa733a`；
原版 JAR `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`，
Oracle 1.3.52 `0710d21de30dc28dc7c137d63c587eec0e0d16a4b0e96d6708bde89699044f44`。
未修改生产模拟器、模型、训练参数或恢复契约。

## 自然路线与选择偏向

新 namespace `[131200490,131200506)` 运行前检查冲突。
16 条 bounded native 路线使用独立 Emerald-first 公开启发式：沿公开地图真实边寻找燃烧精英，
休息时恢复 HP，战斗只使用实际合法卡牌/药水。所有失败和未到达案例保留，
其中 2 条取得 Emerald；选动作数较少的 seed 131200503 做原版独立复验。
这是有利规则样本选择，不是模型能力、正式胜率或随机总体通过率。

原版从正常 Neow 开始，无 parity 场景、人工构筑、HP、金币、钥匙、敌人或奖励注入。
boundary 47 实际沿 map:2:6 进入第七层燃烧精英，原版位置证据与公开图标均匹配。
敌人为 Gremlin Nob，初始 HP 88、Regeneration 3。实际打赢后，boundary 65 选择
`TAKE_REWARD / reward-key:emerald`，下一边界 Emerald 从 false 变 true。
随后领取剩余奖励、退出、实际进入第八层战斗并继续出牌。

72 个动作、73 个边界，至预算上限明确 `DIAGNOSTIC_LIMIT_UNFINISHED / game_failure=false`。
未记作死亡或通关；当前仍无 Ruby/Sapphire，不将三条不同路线合并为同一局集齐三钥匙。

## 两种独立重放及范围

全部 73 个公开 Observation、合法动作、有效 RNG、base reward、terminated/truncated、
success/reason 一致；从 73 个完整 checkpoint 恢复，执行全部已记录后续动作，
逐边界完整 native snapshot 相同。未执行的 Act2–4 后缀没有验证。

原版在 boundary 22、23 的实际 Discovery 选择分别记录 retrieval updates=14。
常规对照使用现有 validation_evidence 输入这两个原版时钟见证；这份报告按条件化身份保留。
为避免把条件化结果冒充默认训练路径，又做独立正常 reset 重放：完全不传任何校验时钟或
card-soul 输入，仍逐项对照原版全部公开状态、合法动作、RNG、奖励和 horizon。
默认路径 **73/73** 边界匹配；其自行产生的 **73/73** 完整 checkpoint 后缀恢复也匹配。
没有清除不完整状态或 replay_required，没有忽略已记录的游戏状态字段。
默认报告的 checkpoint 来源是它自己的正常执行，而不是条件化 capture 中的审计历史。
两组恢复是同一条原版路线的两种检验，不计作新增独立游戏。

默认成功只证明本例两个真实时钟恰好与 native 默认一致；不能推断其它帧率、
其它 Discovery 路线或生产版未采集时钟的所有策略轨迹都等价。

原始 direct combat 投影有 **47** 个可对齐边界，全部相同；其余 **26** 个界面/终点
缺少成对战斗态，不计通过。本例没有新的 raw 差异，不消除其它案例已发现的尸体能力差异。
没有模型推理、编码 tensor 或 recurrent memory 等价比较。

## 交付及下一步

固定原版公开轨迹已冻结回归；完整 raw、自然路径、原版实际命令、两份重放、所有 native
筛选尝试、版本摘要和恢复 journal 封存，见 natural-emerald-flow-evidence.json。
CPU 全量 1739 passed、2 skipped、4 warnings，233.97 秒；Ruff、词表及 diff 检查通过。
配置检查保持 31/33，两个既有 lambda 实验的来源身份不匹配未改写。
原工作区 197 个待提交文件及 status 保持不变，游戏运行目标已恢复，服务器任务未干扰。
`training_gate=NOT_QUALIFIED`，状态不自动接入生产课程。

红、蓝、绿现在都有正常开局自然获取案例，但仍缺同一局集齐三钥匙后的自然完整转幕、
其它燃烧 buff、长 Heart 攻防/药水交互与模型能力诊断。下一步按这些实际缺口推进，
不能据三条诊断路线宣布完整 A20H 已认证或继续加训练步数就能解决问题。
