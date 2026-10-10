# A20 双 Boss 与 Act4 入口：既有原版归档重新绑定

日期：2026-10-10。只读使用 `D:/SLS/local/audits/fullrun-parity-20261007` 的历史归档、对应历史 Oracle JAR/build 和当前固定场景资源，在隔离 worktree 的 DL / CPU 重放。没有启动原版、GPU、训练或修改生产 native。

当前 native 来源 `6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0` 与 NUS 冻结评估环境分别保留身份。每套归档通过历史 Oracle 全 JAR/member SHA、stock JAR SHA、场景资源 SHA、完整结束和恢复记录检查；随后验证实际初始 boss context 与第二 Boss 入口。

## 实际覆盖

| 输入 | 有效案例 | 内容 |
|---|---:|---|
| complete / reverse / awakened-first / awakened-second | 18 | 六种有序 Boss 组合，每种 3 个受控案例 |
| terminal | 3 | Time Eater→Donu/Deca 后进入 VictoryRoom |
| key-act4-entry | 3 | 初始三钥匙，两个 Boss 后进入 Act4 |
| key-act4-zero-reset-r2 | 3 | 正确重置 card RNG 的 0 边界，进入 Act4 |
| key-act4-boundaries-r1 显式有效子集 | 11 / 12 | counter 0/250/500/750 边界，进入 Act4 |
| 合计 | **38** | 受控初态及动作；不是自然构筑、通关率或课程数据 |

旧 `key-act4-boundaries-r1` 全批检查失败：seed **131200248** 声明初始 card RNG counter 0，原版实际 250。逐例核查其余 11 个符合声明；新增 `--seeds` 只用于显式诊断子集选择，报告记录原始总数、选取与排除 seed，capture SHA 仍指向完整未修改的原归档。没有改写历史场景、过滤后伪造 launch，或把 counter250 案例重新命名为 counter0。两个符合声明的旧 counter0 案例与独立 zero-reset R2 分开保留。

## 战斗对照及保留差异

38 个实际重放包含 250 个动作后边界；其中 **174** 个原版/native 对齐的稳定战斗边界，公开战斗 adapter 投影、合法语义动作及全部 `_rng` 一致。这里的 adapter 投影不是完整 FullRun Observation，也不包含循环记忆对齐。

174 个对齐边界中 **106** 个原始投影严格相同，**68** 个仍有原始差异：36 个死亡 Cultist Ritual5、32 个死亡 Deca Artifact3。已有窄分类只在剩余所有比较字段相同、死目标不再合法时标注其来源，不能更改 equal=false、归零 powers 或一般豁免尸体状态。所有原始差异与完整 native 状态均保留。

其余 **76** 个动作后边界为原版 victory UI 与 native 自动跳转不对齐，明确不记通过。原版 `proceed_to_second_boss` 记录为 native 无动作 UI 步；实际战斗动作逐项验证 legal bits，不能把 UI 无动作解释成 agent 已正确选择或循环记忆完全一致。

35 个案例具备额外原版 VictoryRoom / Act4 终点，所枚举的 HP、maxHP、gold、永久牌、floor、RNG，以及适用时 act/keys 均匹配。seed 131200222–224 只有第二 Boss 胜利 UI，没有稳定终点；不将空 final_comparisons 视为通过。17 个 Act4 案例均为 Time Eater→Donu/Deca，不能推广为六种顺序都已验证 Act4。

## 完整 checkpoint 后缀与终止

新增 CPU 入口 `tools/verify_boss_flow_suffix.py`：从每个初始及动作后 checkpoint 恢复，执行全部剩余实际战斗动作，并逐边界比较完整 native snapshot。**288 / 288** checkpoint 的完整剩余轨迹与最终状态匹配，不只是恢复后一个动作。

首次工具试验因 JSON 数组解码为 list、native bottle_indices 返回 tuple 而拒绝比较；失败日志保留。这是工具容器表示差异，不是已证实模拟器恢复缺陷。修正为先对持久化目标比较全部字段的 canonical JSON（不删除字段、不归一化数值），再用重新运行所得 native 类型执行严格 runtime snapshot 等式。新增测试同时证明改变 gold 会被拒绝。

A20H horizon 下，21 个 native Act3 终点判定 `terminated=true / success=false / HEART_NOT_REACHED`；17 个 Act4 入口为非终止、success=false。未将 Act3 游戏胜利当成 Heart 成功，也未把进入 Act4 当成成功。原版缺终点的 3 个案例的 horizon 判断仅为 native 运行证据，非原版终止对照。

新增 10 个冻结回归：六种 Boss 顺序各一个代表，加 Act4 counter0/250/500/750 各一个。期待的公开战斗投影、动作与 RNG 由原始 stock payload 独立重新提取；初态与原始 direct 差异明确分离。回归没有断言原始 direct 完全匹配，也不强制保留已知错误行为。

## 限制与下一步

初始 hand、monster HP、牌组、遗物、RNG 和 boss_order 经过受控设置/条件化。库存强度、遭遇分布、地图自然可达性未被这些样本证明；victory UI 折叠对循环策略的影响未检查。完整环境仍 `training_gate=NOT_QUALIFIED`、`training_eligible=false`。

本轮证据已推进至 Act4 **入口**，尚未连接休息、商店、Shield/Spear、奖励及 Heart，也没有测试分别缺一枚钥匙的入口分支。下一步优先检查已有后段归档是否能提供这些连续边界；缺失部分使用固定新 namespace 的最小原版场景补采。同时保持 Reptomancer / Gremlin Leader 尸体与召唤差异独立，不用本轮 Boss 小样本的公共投影一致将其豁免。

证据封存清单见 [boss-act4-evidence.json](boss-act4-evidence.json)。原目录、模型及服务器任务保持原身份；本轮无需服务器操作。

代码提交 `1de82b6`。CPU 全量 **1658 passed / 2 skipped / 4 warnings**，81.50 秒；跳过为本地导出策略缺失和 CUDA 不可用，警告为现有来源重绑定测试。Ruff 通过。封存包 `local/reports/boss-act4-requalification-20261010-delivery-r1.zip` SHA256 `d72ac29d64831f9496e42cd76c5aa6c03b4fb3e0628694a8702cb3a36825996a`。原工作区 197 个待提交文件和 status 再次与快照一致，未改动。
