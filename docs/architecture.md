# 项目架构

## Act1–2 时钟合同与训练起点

当前native的Discovery使用固定14次retrieval update；原版受渲染时钟影响，独立探针已观察到15次。因此同seed不意味着跨后端的Discovery后续卡牌RNG和选项逐项相同。训练环境采用`DISCOVERY_FIXED_RETRIEVAL_UPDATES_14_V1`，不是对原版全部时钟轨迹的无条件复刻。验收分别保留无条件差异及原版独立计时条件下的完整回放；条件通过不能登记为无条件一致。不完整前缀、未解释差异或计时证据缺失仍阻断资格。该合同未把时钟、RNG或隐藏状态加入策略输入，未改原版源文件或历史checkpoint身份。

真实第二幕入口课程只从正常A20开局合法动作前缀取得状态，保留实际资源，所有到达入口的样本均纳入，不按未来胜负筛选。每个辅助入口是一个新episode：当前策略记忆、上一动作和上一奖励重置；旧策略的动作、log probability和收益不进入PPO更新。正式评估仍全部正常开局。入口库绑定native/profile/SHA，保存恢复同时绑定课程计数和各worker当前起点类型；50%指episode启动比例，不表示两幕决策数相等。入口库变更或比例调整须登记新合同，不能当作原作业的精确恢复。

SLS 的公共边界是：

```text
公开 Observation + 合法语义 Action 候选
    → 结构化编码、关系注意力、GRU 记忆
    → 策略概率与状态价值
    → Backend.step(Action)
    → 下一决策、奖励和终止信息
```

策略不读取 native RNG、隐藏抽牌顺序或模拟器内部恢复状态。原版游戏与模拟器共享公开观测和语义动作协议；验证用诊断信息独立于策略输入。

## 实现分工

| 路径 | 职责 |
| --- | --- |
| `src/sls/contracts/` | Observation、Action、Decision、Transition 与验证协议 |
| `src/sls/content/`、`curriculum.py` | 内容登记、角色/难度、幕目标与终止判据 |
| `src/sls/backends/simulator/` | C++ 引擎的 Python 适配 |
| `native/simulator/` | 战斗、地图、奖励、事件、RNG 与状态序列化 |
| `src/sls/backends/original/`、`native/oracle/` | CommunicationMod 接入与 Java 公开观测补丁 |
| `src/sls/model/` | 结构化编码、关系注意力、GRU 与合法动作评分 |
| `src/sls/rl/` | 分片 rollout、循环 PPO、评估、checkpoint、恢复和迁移 |
| `src/sls/audit/`、`diagnostics/` | 原版差分、轨迹与策略诊断 |
| `src/sls/runtime/` | 推理产物、实机控制器与日志恢复 |

当前策略输入是 `sls-policy-input-v5`；具体字段、词表与 checkpoint 版本以源码为准。旧权重不能靠修改版本号变成兼容模型。

默认模型架构仍是 `sls-recurrent-relational-policy-v5`，旧参数布局和前向行为保留。可选研究架构 `sls-recurrent-relational-policy-v6` 使用相同v5输入张量/词表，但增加类别字段与动作引用角色的逐通道缩放，并将选项variant放入独立embedding。v5把类别词表中的OPTION编号用于内容embedding，造成选项与无关内容参数共享；v6拆开该共享。角色与字段缩放使求和前的表示能区分槽位，不代表所有语义碰撞均已消除。

通过 `migrate_v5_role_policy` 显式转移权重：共享参数原样复制，新缩放初始化为零，选项独立行复制旧模型实际使用的向量，因此初始预测保留；以后才能通过学习改变这些表示。新架构不可直接strict加载旧state_dict，迁移不携带Adam、环境、RNG或循环状态，不是精确续训。`tools/verify_role_model_migration.py` 验证当前模拟器合法决策上的预测与动作一致性，只生成诊断JSON，不导出权重或启动训练。

首段20M的正式配置显式选择v6，库级默认v5及旧配置保持原样。`initialize_act1_weights` 仅在 `warm_start.model_migration = "v5-to-v6-role-bindings"` 时执行角色迁移，仍检查原90M文件SHA、相邻课程、累计步数及独立模拟器来源迁移证据。首次建立新run；后续恢复加载v6完整checkpoint，不重复迁移父模型。

入口库的可移植证据 `tests/fixtures/evidence/act12-foundation/entry-prefixes.json.gz` 只保存原始合法动作前缀、每个原存档SHA及原库元数据。服务器缺库时逐项重放重建，最终压缩库必须匹配固定SHA；不是在服务器重新筛选训练状态。已有库仍按原流程核对SHA并重放全部前缀。

公开观测和编码数值必须有限，NaN/Infinity在进入模型前拒绝。当前动作metadata只支持登记的数值字段，类别字段不能被静默丢弃；新增类别动作特征须显式扩展输入合同。native战斗动作只接受已登记的0–4类型，未知类型不能降级为END_TURN。这些是输入正确性防线，不替代原版规则对照。

## 兼容性与验证边界

checkpoint 包含模型、优化器、worker、RNG、循环记忆及训练合同；推理产物仅用于部署，不可代替完整续训状态。精确恢复与跨任务权重迁移是不同操作。

原版对照覆盖已记录的场景与分支。自动回归通过不代表全部种子、事件、Act3/Act4 路线已经原版一致。历史证据须同时核对源码、runtime、种子、原始载荷和适用范围。

不可变小型证据在 `tests/fixtures/evidence/`，私有原版来源和大量轨迹在 `local/audits/`。历史合同路径由 `src/sls/evidence_paths.py` 解析；该解析仍执行仓库内路径限制，证据 SHA 校验保留。

当前 Act1/Act2 课程以 native `public_run.completed_act` 与原版公开 Boss 房间 COMPLETE 判定 Boss 击败，在战斗奖励边界终止；公开幕号前进仅为补充判据。不能只阅读 `completed_act_between` 的 fallback 就登记为“进入下一幕才终止”。第二幕成功先于Boss奖励，无第三幕动作。

Act1–2 最终基础资格为 `PASS_CONTRACT_BOUNDED_ACT12`；Discovery固定检索14次的环境合同与独立时钟条件回放分别登记。真实第二幕入口库只保留合法正常开局前缀可达状态，入口episode清零循环记忆、上一动作和上一奖励；其库SHA、来源、轮转计数及episode来源纳入checkpoint合同和恢复状态。正式终点评估仍是正常开局。
