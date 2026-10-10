# 同 seed 原版重复：帧驱动 RNG 与自然同局三钥匙

2026-10-10，跟进 `FROZEN-THREE-KEY-ROUTES.md` 的首次失败。
DL / CPU，隔离分支。生产 native、模型、奖励、PPO、训练分布及恢复契约保持不变。
增加两次真实原版运行；不替换或删除首次失败，不操作服务器。

## 相同动作的三次实际运行

三次都从正常 Neow 开局，seed 131200532，同一封存的实际动作记录，
同一 JAR 和 Oracle 1.3.52，同一 60 FPS 限制，无 parity 场景或世界状态注入。
参考是冻结 90M 模型加公开选路/钥匙干预后的 native 动作；原版本身没有模型推理。

| 实际原版运行 | boundary58 Discovery 更新次数 | boundary59 card_random counter | 结果 |
|---|---:|---:|---|
| r1（上轮保留） | 15 | 51 | 首次 RNG 分歧后停止，60 边界/59 动作 |
| r2（本轮） | 15 | 51 | 同一处首次 RNG 分歧后停止，60 边界/59 动作 |
| r3（本轮） | 14 | 48 | 与默认 native 继续匹配，75 边界/74 动作，自然集齐三钥匙后停止 |

三个两两对照：共同 60 个公开 Observation 和合法动作一致，共同已执行动作一致；
r1/r2 有效 RNG 一致；r1/r3、r2/r3 仅在共同 boundary59 的 RNG 不同。
0–58 的公开历史、实际动作和有效 RNG 均相同。
这证明相同 seed/动作也会有原版运行时差异；三次不是更新次数的总体分布估计，
不报告 14/15 的频率、模型胜率或总体一致性通过率。
未执行的失败分支后缀不计作通过，也不能断言其未来可见结果必然与 r3 一样或不同。

绑定 game JAR 的 `AbstractGameAction.tickDuration()` 字节码明确读取
`Gdx.graphics.getDeltaTime()`，从 duration 减去实际帧时长，duration < 0 时完成。
`DiscoveryAction.update()` 则先生成候选、后检查 duration/retrieveCard 并计时。
配合实际被动时钟记录和上轮条件化因果重放，证据支持帧驱动 RNG 变化。
FPS 限制不是固定时间步；这里没有测量逐帧 delta、施加受控负载或执行时钟干预实验。

## 一局自然三钥匙的真实覆盖

r3：全部初始钥匙 false，从实际原版正常开局执行。

- boundary47，第五层真实合法 `TAKE_BLUE_KEY / reward-key:sapphire`，下一边界 Sapphire=true。
- boundary49，实际沿 `map:3:5` 进入第六层燃烧精英；开场 Lagavulin HP112、Metallicize8、Regen3。
- 击败后 boundary66 `TAKE_REWARD / reward-key:emerald`，下一边界 Emerald=true。
- boundary73，第七层 `RECALL / rest-option:2`，下一边界 Ruby=true。

终点是 Act1 floor7、三钥匙 true 的非终止状态。没有赢得 Act1 Boss，未验证完整 Act2–4，
未完成 A20H；主动诊断停止不记游戏失败。此前首次报告“缺同局三钥匙”是 r1 当时的结论，
本轮 r3 只补足这一局前缀的规则证据，不回写旧失败的身份。

## 默认和条件化重放分开验收

新增 CPU CLI `tools/replay_frozen_key_reference.py`：
绑定参考报告、public/private 历史哈希、capture producer、stock JAR、封存 Oracle、
当前 native 来源和二进制。重新计算封存数据中的差异，拒绝错误版本、篡改历史、
伪造零差异、隐式环境迁移和不连续动作。失败捕获仍可以作为诊断输入，不能冒充通过。
默认不传 validation_evidence；条件化必须显式 `--condition-on-stock-clock`。

- r3 默认重放：75 个完整 Observation、合法动作、有效 RNG 匹配；74 个 transition 的
  base reward、公开状态和 horizon 信息匹配。75 个完整 checkpoint 的全部已记录后缀恢复一致。
- r2 默认重放：准确保留 boundary59 RNG 不匹配，60 个自身 checkpoint 后缀恢复一致。
- r2 显式条件化 15 次更新：60 个已观察边界匹配，60 个自身 checkpoint 后缀恢复一致。

没有清除 replay_required、截掉私有 checkpoint 字段或用 r3 填补 r2 后续缺失状态。
本轮三个最终重放合计 195 个恢复后缀；这不是 195 个独立原版游戏。
初版重放 r1 输出与其执行源码精确封存；加强身份/差异验证后又以 r2 输出完整复验，
最终证据引用 r2，不把旧验证器版本改称新版。生产 native 与训练实现摘要不变。

同局三钥匙前缀已冻结单独回归，使用默认 clock，无 validation_evidence 输入，
并断言实际合法获取动作、钥匙变化、非终止身份和全部恢复后缀。
另增加错误版本、private 历史篡改、伪造比较、隐式迁移、首动作 transition 分歧及 CUDA 禁用测试。

## 对项目下一阶段的影响

1. **区分规则错误和运行时随机性。** RNG 差异继续保留，不能建立一般 RNG 忽略白名单；
   也不能只因 seed 对照分叉就推断伤害/卡牌规则错误，或把已知时钟差异当作全局通过理由。
2. **不直接把默认 14 改成 15。** 当前原版同时观察到两者。固定时钟部署与帧驱动部署应有
   不同环境身份；任何生产建模修改先做真实时钟采样、行为影响测试和可审查的环境迁移方案。
3. **继续自然后段能力诊断。** 现在可用真实三钥匙前缀研究继续向 Act1 Boss/Act2 推进，
   保留完整公开前缀和每个模型自己的 recurrent memory。该状态尚不是“后段成功课程”样本。
4. **扩大 Heart 和其它规则覆盖。** 同局三钥匙不消除其它燃烧 buff、Act3 双 Boss、
   Act4 长攻防/药水、尸体清理与召唤等缺口。原版重复 clock 与这些工作可分别推进。

本轮没有依据三个样本选择或实现生产时钟分布，没有接入课程或启动训练。
CPU 全量 **1753 passed、2 skipped、4 warnings**，263.39 秒；Ruff、词表、diff 检查通过。
配置仍为 31/33，两个既有 lambda 来源身份问题保留。原工作区 197 个待提交文件/status
未改变；新增游戏进程均由 owned guardian 结束，63 个备份目标恢复，全部 journal 为 RECOVERED。
`training_gate=NOT_QUALIFIED`。完整证据与版本绑定见 `stock-discovery-repeats-evidence.json`。
