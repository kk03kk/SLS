# 正常开局的红、蓝钥匙获取与完整恢复

2026-10-10，隔离分支、DL / CPU。生产 native 来源仍为
`e4f8452ff3cea688e49a362488814d4c2e29d9b721bcc6cd71c8d9f1d9fa733a`；
原版 JAR `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`，
Oracle 1.3.52 `0710d21de30dc28dc7c137d63c587eec0e0d16a4b0e96d6708bde89699044f44`。
不修改生产规则、权重、奖励、PPO 或 checkpoint 兼容契约。

## 方法及实际覆盖

从实际正常 Neow 开局，无 parity 场景、人工牌组、HP、金币、钥匙或奖励写入。
公开固定启发式只从实际合法动作中选择；地图策略沿公开边寻找休息/宝箱，
战斗使用初始牌组。该启发式用于规则定位，不代表训练模型能力。
诊断私有 raw、RNG、checkpoint 单独保存，策略只接收公开 Decision。

- seed 131200480：76 个实际动作、77 个边界，第五层真实死亡；未取得钥匙。
- 对新 namespace `[131200481,131200489)` 做 8 个 native-only 小规模前瞻筛选，
  保存所有尝试和选择依据。所选 131200488 是有利规则样本，不能用于胜率统计。
- seed 131200488：原版独立运行，boundary 53 实际 RECALL 后取得 Ruby，
  boundary 88 实际宝箱奖励交换取得 Sapphire。随后退出奖励、实际进入第十层战斗。
  92 动作预算用尽，共 93 边界，明确 `DIAGNOSTIC_LIMIT_UNFINISHED / game_failure=false`；
  没有把中断记成死亡或通关。

两条原版轨迹合计 **168 动作、170 边界**，完整公开 Observation、所有合法动作、
全部有效 RNG、base reward、terminated/truncated、success/reason 均一致。
没有时钟校验输入；完整 raw 历史在每步前后连续，并保留实际原版执行命令。
本轮没有编码 tensor 或 recurrent 模型输入比较，不将公共契约匹配扩展成模型行为等价。

## 自然 checkpoint 恢复

按各自真实 seed 从 native reset 重放全部实际原版动作，逐边界检查完整 native checkpoint。
随后从 **170/170 checkpoint** 恢复，每个都执行全部已记录剩余动作，
逐步完整 snapshot 一致。包含真实死亡终点、休息换钥匙、宝箱/奖励界面及后续战斗。
93 边界路径的后缀验证仅到实际诊断终点，不声称验证了未执行的 Act2–4 后缀。
没有清除 replay_required、不完整屏幕或替换开局历史。

## 原始战斗状态差异

170 个边界中，122 个具备成对 raw 战斗投影：82 个相同、40 个不同；
其余 48 个界面/终点没有成对投影，不计通过。
全部差异为怪物 powers 字段，共 53 处；53 个差异 owner 的实际 HP 都为 0。
原版清空了死亡怪物能力，native 保留 Vulnerable 等能力。

独立从上述原版 JAR 导出 AbstractMonster 字节码：`updateDeathAnimation()` 在
deathTimer < 0 后标记 isDead、dispose，再清空 powers；不是在所有 die() 回调起点清空。
native Monster::die 的普通死亡路径保留状态，Awakened/Darkling 等另有专门处理。
这给出了观察差异的清理时机证据，但本轮没有验证所有召唤、槽位复用或复活回调，
也不把所有尸体差异自动豁免。不能直接在 die() 开头清空能力，以免破坏 onDeath 效果。

## 采集失败和身份

stock-r1/r2 启动失败都保留：生产者读取相对路径的策略源码摘要，
而 CommunicationMod 的工作目录是游戏目录，触发 FileNotFoundError；第一次尚未发出握手。
修正为绝对路径并提前握手后，新 stock-r3 才产生完整游戏证据。
这两次属于采集工具失败，未计作游戏失败或模拟器差异。

实际执行生产者源码按 capture 内 SHA 独立保存为 executed-producer-r1.py。
后续 CLI 增加独立源码路径初始化和仅允许 --device cpu；旧执行源码与新入口不混称同一版本。
完整版本、失败/成功启动、恢复 journal、CPU 验证及封存清单见 natural-key-flow-evidence.json。
最终 CPU 全量 1735 passed、2 skipped、4 warnings，222.51 秒；Ruff、词表和 diff 检查通过。
配置检查 31/33，两个既有 lambda 实验的来源绑定错误不改写。
原工作区 197 个待提交文件及 status 保持不变，全部原版运行 journal 已恢复，未干扰服务器。

## 尚未证明

未覆盖 Emerald 的自然获取、持三钥匙后的自然完整转幕、后段模型构筑能力或正式 A20H 胜率。
自然红蓝取得证据比初始注入持钥匙更强，但仍是两个诊断样本，不能认证整个模拟器。
`training_gate=NOT_QUALIFIED`；状态不自动接入生产课程。下一步补自然燃烧精英路线，
并继续 Heart 攻防/药水交互及可恢复公开历史的数据资格。
