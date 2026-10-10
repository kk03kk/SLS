# 首条连续 Act4 原版轨迹与实际适配层阻断

日期：2026-10-10。隔离 worktree、DL / CPU；原版 JAR `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`，新 Oracle 1.3.50 JAR `10df4da648b89fc1d914fb823e2194228b59da94ae98ed4d2952bd25d6cc30e0`。生产 native、模拟器 Python 适配层、网络、奖励和训练参数均未修改。

## 原版实际连续证据

namespace `[131200440,131200441)` 已检查现有场景、配置、工具和测试中不存在冲突。受控初态是 Act3 的强灼热攻击牌组、三把钥匙、最终幕解锁与两个 Boss；之后不再注入房间、怪物、牌组、钥匙或资源。完成实际双 Boss 和 VictoryRoom 对话，进入 Act4 后使用完整公开 Observation 和合法动作选择：休息、离开商店、击败 Shield/Spear、领取/跳过奖励、进入 Heart、击败 Heart。

`stock-r2.json` 实际完成 **19 个 Act4 决策**，原版终点 `GAME_VICTORY / success=true`。保留每步公开 Observation、完整合法动作、所选动作、实际 backend 命令、前后 raw payload、validation evidence 与 horizon。原版 backend 内部折叠的界面步骤仅有命令记录，不声称已经逐帧保存所有 UI 历史。这里是人工强牌组的规则探针，不是智能体胜率、自然课程数据或自然构筑成功样本。

首轮 `stock-r1.json` 实际走过休息、商店、进入精英，在采集器误读 `Card.content_id` 后停止；真实公开字段是 `Card.card_id`。失败证据完整保留，游戏 journal 已恢复，未记为游戏死亡。修正后先用真实 Card 类型及重复牌实例验证公开选择逻辑，再用同 seed 另存 R2。

## 当前环境确实不匹配

双 Boss 初始/第二入口/Act4 入口的既有枚举字段匹配，六个 native checkpoint 完整后缀匹配，但完整公开输入在 Act4 入口已经不同：原版地图有休息、商店、精英三个节点，native 额外暴露第四个 Boss 节点。`public-replay-r1.json` 严格在首个决策停下；合法动作及 RNG 此时相同。

`diagnostic-transfer-r2.json` 独立保留全部公开差异，按原版记录的实际动作做诊断迁移。完成 **11 个动作**，含休息、商店、双精英和奖励处理；每步有效 RNG、horizon 与合法动作到这个前缀均一致，公开 Observation 仍有地图差异。十二个 native checkpoint 的完整已执行前缀后缀匹配；不能推广为 Heart 后缀或完整两端一致。

第 12 个原版决策（index 11）被拒绝：原版 Boss 动作为 `CHOOSE_MAP_NODE / map:boss`，native 为 `map:3:3`。对应 Boss 公开节点坐标原版 `(0,15)`、native `(3,3)`。不强行转换动作，也不进入后续 Heart 对照，报告 `RECORDED_PUBLIC_ACTION_UNAVAILABLE`。原版真实完整成功和 native 未完成迁移分别保留身份。

源码解释：simulator/environment.py `_run_action` 使用 `current_map_y < 14` 判断普通地图节点，未区分 Act4 的短地图；`_adapt` 直接展示全部 native public_map 节点。Original adapter 按实际 `boss_available` 生成 `map:boss`，地图 Boss 与普通房间节点表示不同。下一修复须针对这个公共契约，不更改原版规则或隐藏失败。

首版迁移工具曾比较 run RNG，而 native 战斗期间由 `combat_checkpoint.rng` 持有最新流，直到战斗结束才回写。`diagnostic-transfer-r1.json` 的 9 个 RNG 字段差异保留为工具读取所有权错误，不能列为已证实游戏规则缺陷。R2 使用实际战斗 RNG，全部已执行前缀 RNG 相同；未归零、删除或同步修改状态。

## 后续验收要求

固定原版 19 个决策及全部公开目标已冻结为 `tests/fixtures/regressions/ending-continuous-stock-r1.json`，native 初态作为独立条件化 checkpoint 保存，不作为策略输入。下一步修复 Act4 地图节点呈现和 Boss 动作身份，用这份真实原版轨迹验证全部公开 Observation、合法动作、RNG、终止、以及每个 fullrun checkpoint 的完整剩余决策。若遇到下一处差异，继续保留，不增加兼容白名单。

任何生产适配层修改都属于训练实现身份变化，按现有摘要与恢复契约处理，不能直接给 NUS 旧模型或归档换环境身份。本轮仍 `training_gate=NOT_QUALIFIED`。CPU 全量 **1710 passed / 2 skipped / 4 warnings**，76.65 秒；Ruff 通过。跳过为本地导出策略缺失和 CUDA 不可用，警告为现有来源重绑定测试。三次实际原版运行 journal 均恢复，原工作区 197 个待提交文件及 status 与快照相同。无需服务器操作；代码与证据只进入独立研发分支。版本和封存清单见 [ending-continuous-evidence.json](ending-continuous-evidence.json)。
