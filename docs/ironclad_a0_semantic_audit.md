# 战士 A0 语义审计

本审计把“代码里存在一个枚举或 `case`”与“机制已经得到等价证据”明确分开。机器可读的权威范围在
`configs/validation/ironclad_a0_content_scope.json`，逐项证据账本在
`configs/validation/ironclad_a0_semantic_audit.json`。

## 当前范围

- 130 张战士可达卡牌：75 张红牌、35 张无色奖励牌、4 张事件/特殊牌、4 张状态牌、12 张 A0 可达诅咒牌。
- 33 种战士药水。
- 151 件战士可达遗物，包括公共、战士、Boss、商店、事件/特殊遗物和初始遗物。
- 52 个完整游戏事件；其中单独标识 11 个 Act 1 基础事件、6 个 Act 1 神龛及 14 个 A0 一次性事件候选。
- 20 个 Act 1 普通、精英和 Boss 遭遇。
- 从这些遭遇的 C++ 构造闭包确定的 25 种 Act 1 怪物实体。

范围由 registry、C++ 实际池和反编译 Java 来源确定性生成，而不是由上述数量反向硬编码。

## 棱彩碎片

`PRISMATIC_SHARD` 仍保留在 registry、词表、存档映射和底层商店遗物池中，因此不会改变遗物池洗牌或后续 RNG。战士 A0 策略范围只在 canonical 商店 observation 和 `BUY_RELIC` 候选处隐藏它；过滤保留其他商品已有的实例 ID、原始选择序号和 native action bits。

## 证据等级

- `SOURCE_MATCHED`：模拟器 metadata 与反编译来源一致。
- `NATIVE_VERIFIED`：存在对具体效果的确定性断言。
- `ORIGINAL_VERIFIED`：存在真实游戏局部运行或命名白名单场景证据。
- `ROUTE_VERIFIED`：真实路线 truth segment 在当前实现下重放一致。

`NATIVE_EXECUTED` 只表示内容可初始化并进入执行管线，不等于效果正确，也不会单独把条目标为 `VERIFIED`。任何缺少逐效果断言的条目保持 `BLOCKED`。

## 本轮已发现并修复的问题

1. Original adapter 原先只在战斗中暴露药水动作，漏掉原版允许在非战斗使用的 Blood Potion、Fruit Juice 和 Entropic Brew。
2. Native run action 枚举原先完全没有非战斗药水使用/丢弃动作。
3. Native 执行药水动作后没有返回，会继续把同一 action bits 当成地图、事件或商店动作执行。
4. 加入库存动作后，事件/休息实体构造曾把药水错误计入事件/休息选项；整段 truth replay 发现并修复了该组合问题。
5. 历史 checkpoint 将派生的 `legal_actions` 当成状态身份，修复动作枚举后无法恢复。现在仍严格验证持久状态，但在恢复时重新生成合法动作，并由 replay 层比较 canonical candidates。

## 训练门槛

当前 ledger 已闭环为 `418 VERIFIED / 0 DIFFERENCE / 0 BLOCKED`，并明确给出
`act1_pilot_ready=true`。三条 Boss 完整路线及两轮、每轮四个独立 seed 的扩展证据
已逐段离线重放，结果固化在 `act1_training_readiness.lock.json`；200-update pilot
和正式训练仍必须通过服务器端 Linux/CUDA preflight，不能只凭该汇总数字启动。

生成和检查命令：

```text
python tools/generate_content_scope.py --check
python tools/audit_content_semantics.py --check
python tools/audit_content_semantics.py --require-pilot-ready
python tools/verify_readiness_lock.py configs/validation/act1_training_readiness.lock.json
```

以上检查在干净、与锁定源码合同一致的 checkout 中都必须成功。
