# 连续 Act4 商店交易与恢复边界

2026-10-10，DL / CPU，隔离分支。Oracle 1.3.52 JAR SHA256
`0710d21de30dc28dc7c137d63c587eec0e0d16a4b0e96d6708bde89699044f44`。
生产 native 来源仍为 `e4f8452ff3cea688e49a362488814d4c2e29d9b721bcc6cd71c8d9f1d9fa733a`，
本轮不修改生产模拟器、训练参数或 checkpoint 兼容名单。

## 原版连续交易

独立 namespace `[131200460,131200461)`，明确受控的 Act3 强牌组、三钥匙、双 Boss 和初始金币 1000。
之后通过实际游戏动作进入 Act4；不在商店写金币、价格、货物、删牌结果或敌人。
实际完成买 Clothesline（25 金）、买 Lee's Waffle（157 金）、买药（79 金）及删牌（75 金）；
删牌通过真实 CONFIRM → REMOVE_CARD 两步。25 个 Act4 决策后原版实际 Heart 胜利。

全部 25 个公开 Observation、合法动作、实际动作后的有效 RNG、base reward、
terminated/truncated、success/reason 与 native 一致。原版 raw 全部保留，
此结论不声称 raw 战斗状态、所有商品机制或正式策略胜率得到完整验证。
另有 11 个可对齐 raw 战斗边界，9 个相同、2 个不同：boundary 11 原版死 Shield
仍有 Back Attack/Artifact，native 只有 Artifact；boundary 12 原版 powers 已清空，native 保留 Artifact。
其余 15 个界面/终点缺少成对战斗态，不计通过。保留原始投影与差异，不做一般尸体豁免。

## 24 个恢复通过，2 个拒绝

26 个 fullrun 边界中，24 个能恢复并完整重放全部后续实际动作，逐步 native snapshot 完全相同。
boundary 7 为不完整 PURGE 选择界面，boundary 17 为 Heart 开场 pending callback；
两者按现有恢复逻辑要求从正常 seed 开局重放历史。
本例人工 Act3 初态不具有该历史，因此恢复报 `Run action is not legal in the current state`。
报告保留两次失败，CLI 对这种不完整通过返回非零；没有清除 replay_required、屏幕状态或替换历史。
所以本例不能作为可恢复后段课程样本。

为核验是否所有 PURGE 状态均无法恢复，另做 native-only 正常开局探针：
seed 131200470 在 65 决策后死亡；seed 131200471 从未修改的 reset 经 67 个实际合法动作进入商店。
PURGE checkpoint 正常恢复，完整原生 snapshot 相同，随后实际 REMOVE_CARD 的结果也相同。
该结果只证明这一条自然 native 历史的删牌恢复；不替代原版 parity，不是胜率样本，
也未解决 Heart pending callback 的自然历史覆盖缺口。

预备工具第一次构建入口时漏同步 combat player 的内部 gold，造成第二 Boss 边界 1012 对 111 的差异。
失败 native-entry-r1.json 保留。修正预备工具后，同时对实际公开金币、fullrun 和 combat 金币做初态断言，
native-entry-r2 与实际第二 Boss 入口相同。这是受控入口预备器问题，不能写成生产游戏规则修复。

## 证据与约束

固定公开交易轨迹和独立自然删牌历史已冻结为回归 fixtures，均与模型输入隔离。
完整原版 capture、版本、入口、失败和成功重放、测试及保护核验见交付 evidence 清单。
CPU 全量 1725 passed、2 skipped、4 warnings，187.48 秒；Ruff、词表和 diff 检查通过。
配置检查 31/33，两个既有 lambda 实验的实现身份不匹配仍保留，不改历史实验绑定。
原工作区 197 个待提交文件及 status 未变，原版运行目标均恢复，已无运行中的游戏进程。
`training_gate=NOT_QUALIFIED`；强牌组和初始金币探针禁止进入生产训练课程。
原版未来仍须覆盖自然钥匙获取、Heart 攻防和药水交互、尸体/召唤状态及自然路径的完整恢复。
