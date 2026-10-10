# Act1/Act2 校验续办：2026-10-08

> 2026-10-08 最新补充：[盗贼奖励修复与真实 Act2 收尾](ACT12_REWARD_CLOSEOUT_20261008.md)。新 native 身份 c07116cc…，15 次 TheCity 奖励对照完成；历史收尾及训练路径保留原身份，不再代表当前发布状态。

当前范围仍为正常 Neow 开局的 A20 Ironclad Act1+Act2。晚幕计划延期。
此前有界资格继续保留，不能据此宣称整个模拟器或所有内容已经认证。

本次首先独立复查 Discovery / Automaton 证据。原版缓存 javap 中
DiscoveryAction.update 在 duration / retrieveCard 判断之前调用候选生成；
因此没有展示给玩家的后续动画更新也推进 cardRandomRng。native
openDiscoveryScreen 默认检索更新数为14，chooseDiscoveryCard 按该次数
生成并丢弃候选；Oracle DiscoveryTimingPatch 只计数并在完成后输出日志。
这解释了为何同 seed、同策略动作仍可能生成不同卡牌；不是 Automaton
攻击逻辑本身已经被证明有误，也没有证据支持把默认数改成15即可修好。

## 本次实际验证

- 重新计算历史有界审计的17份输入 SHA，全部与记录一致。
- 从原始 batch / launch / recovery journal / stdout / trajectory 核验
  Automaton seed8000011000009：334个历史边界，边界112、floor16、serial1
  的更新数独立实测为15，与条件报告完全一致。
- 这次没有重新执行native、策略或游戏。结论只是历史证据完整性通过；
  production隐藏RNG不暴露，也没有获得新的无条件通过证据。
- 新增纯标准库 stock_clock 模块，日志中存在标记但格式损坏时拒绝，
  重复、零值、倒序serial和越界次数拒绝。原入口延迟加载重放依赖，
  离线校验不再加载native或模型。
- 19项轻量测试通过，涵盖畸形日志、序号、构建成员篡改、轨迹身份篡改、
  条件资格冒充严格通过及隔离导入；相关Ruff检查通过。

复算入口：`tools/verify_act12_archived_evidence.py`，输出要求新路径，
禁止覆盖。原始结果保存在本地
`local/audits/fullrun-parity-20261007/offline-evidence-review-20261008-r1.json`。
该入口不替代stock构建认证或新的原版/native差分运行。

## 尚待完成

Discovery无条件随机序列等价仍未成立。保留已知时序边界，不能搜索使
结果匹配的次数冒充实测，也不能将日志中的15改为生产规则。下一项工作
是审阅尚未覆盖的共享机制及现有首个分歧，选择有明确独立原版依据的
受控义务；只有确有证据缺口时才补短原版对照。

生产native、PPO、reward、模型及服务器发布版本本次未修改。
aa63a540…发布目录与本地晚幕审计目录继续隔离；没有commit/push、
没有启动服务器作业，没有使用最终保留集。此前报告中“结果与训练绑定
尚待完成”属于历史时间点，最新训练准备见独立发布文档，不改写旧证据。

后续共享机制审阅新发现盗贼全逃跑后的药水概率分支不一致，已重新开启
单项Act1/Act2义务。独立字节码身份及旧native源码原件已保存，运行差分
尚未采集。见[新义务及邻接分支](THIEF_REWARD_REOPEN_20261008.md)。
因此不能以本页17份归档身份通过推断当前没有新规则风险。
