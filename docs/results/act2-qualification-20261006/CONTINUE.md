# Act2 核验暂停与续接记录

2026-10-06 按用户要求暂停。状态为 **PAUSED_NOT_QUALIFIED**，不启动新 NUS 训练。
本轮修改全部保留在本地，尚未 commit/push；不得用旧报告改写成当前版本通过。

## 暂停时身份与验证

- Git HEAD：`d9ee32d67003f8ce7567a77df21916e96cbbdfef`，工作区包含本轮未提交修改以及此前 pilot 分析修改。
- 最新 native r15 source/compiled SHA：`db731e011f78fc3b484854234a8995032b8888728ac05f90c1fecfa56c95d9af`。
- Oracle 1.2.1 r6 SHA：`f59e2960838a5b744b9a146b8c489560d532f411c813886955ca2fa6dce7a4ef`。
- 原版执行契约：`sls-original-choice-public-boundary-v5`；GRID 契约：`sls-stock-grid-toggle-v2`。
- 冻结90M模型 SHA：`ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c`，保留 Act1 训练来源。
- r15 完整测试：1138 passed、1 skipped、4 warnings，117.69秒；日志 `local/audits/act2-qualification-20261006/pytest-after-r15.log`。
- 此前 Ruff、27/27 配置检查通过；后续修改后需重新完成适用检查。
- 游戏已退出；暂停时对第一份备份的63个目标核对通过，三个受保护用户目录无新增遗留文件。结果在 `local/audits/act2-qualification-20261006/pause-recovery-check.json`。

## 已有证据与剩余范围

1. 63次战斗受控运行：原始 stock 批次 `combat-r4` 与 `boss-entangle-r5`；排除 r4 的旧初始化 Entangle seeds 131100030/31/32，以 r5 重测替代。r14 差分通过，须在 r15 重放。
2. 跨幕 seeds 131100063/64/65：stock `system-transition-r6` 完整，r14 全边界及 checkpoint 重放通过。Discovery 采用 stock 实测动画输入，属于条件核验，不能升级为 production RNG 认证。须在 r15 重放。
3. 奖励 seeds 131100066/67/68：`system-rewards-r6` 失败/不完整，不记通过；修复后需要重新采集完整批次。`system-scripts-after-r10` 仅是新版 native 请求脚本，不是正确答案。
4. checkpoint seeds 131100069/70/71：`system-checkpoint-r7` 在 Empty Cage 部分选择失败，70/71 未运行。GRID 修复后须重跑；不能把 Act1 提前死亡冒充 Act2 覆盖。
5. production `production-first3-r9` 完整执行成功并恢复，包含正常 Neow 开局 seeds 8000011000000/1/2。seed 0 在 r14 诊断重放匹配190边界；seed 1 前199个决策边界匹配，但终止时 block 8/0 差异触发 r15 修复；seed 2 尚未完成 native 正式重放。
6. 当前修复会改变自然轨迹。旧 `production-selection.json` 仅为旧 source 历史证据，须在最终修复版重扫128个诊断 seeds，覆盖不足才扩到512，按既定类别及 seed 顺序选8条。r13 的重扫因中途 source 改变被拒绝，没有有效结果。

## 下次继续顺序

1. 读取本记录、迁移契约、最新 git diff，确认 native 源码与编译身份一致。原始日志不覆盖，新的复算报告使用新文件名。
2. 正式重放 production r9 三条已有完整轨迹，优先定位 seed 2 首个分歧；重放 r15 战斗与跨幕受控证据。
3. 完成奖励及 checkpoint 两批剩余6个 seed，修复每个有独立证据的根因，保留失败前证据并运行相邻回归。
4. 源码稳定后重新扫描当前 native 128/必要时512，重新选择并完成8条 production canary。扫描/编译不与 stock 批次同时运行，避免动画时序干扰。
5. 更新逐义务台账、差分/恢复证据汇总、迁移身份与门禁；完成适用检查后再准备 GitHub 交付。尚未完成72次和8条验收时仍保持 NOT_QUALIFIED。

原版 JAR、字节码、完整日志、权重和备份仅在本地。没有自动 source 兼容白名单；规则、动作和数值观测已有实质变化，旧 λ=.98 pilot 不能作为严格单变量 control。PPO、reward、网络、训练分布未改；最终保留集未使用。
