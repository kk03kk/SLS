# 新配方：完整轨迹 critic 预热＋20M Act1+Act2 PPO

这是训练准备记录，**不是训练结果或效果证明**。本地验收只覆盖纯Python、mock、源码和已有证据；真实模型验收在同一次NUS allocation内进行。启动门禁通过后，才开始生产20M。

## 为什么重新从90M开始

匹配λ实验已经完成，固定终点联合成功为λ=.98的7/2048、λ=1的6/2048，冻结90M为4/2048；没有一组达到原来既定的自动延长标准。旧实验结果、零循环门禁和统计标准保持历史身份，见[归档结论](../act12-lambda-20261008/README.md)。

本次绑定90M固定终点SHA `274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0`。显式Act1→Act2权重迁移，保留全部权重，重新初始化Adam、训练seed游标、环境和循环记忆。不是94M终点续训。总计约110M decisions，其中新增20M用于两幕联合任务。

假设是：原critic的目标从Act1成功变成Act2成功，先用当前任务的实际完整轨迹校准value head，可能让随后PPO更稳定。该假设尚未确认。本次是完整预热方案，包含采样、目标、冻结和更新方式的阶段变化；没有20M预算匹配control，不能独立证明预热的因果优势。

## 固定配方和恢复

配置为`configs/train/ironclad_a20_act12_critic20m_r1.toml`；SHA绑定计划为`configs/experiments/act12-critic20m-r1.json`。

| 项目 | 固定设置 |
|---|---|
| 任务 | 正常Neow、Ironclad A20、Act1+Act2联合通关、Win reward |
| 预算 | 新增20,000,000 decisions，允许不足一个16,384决策rollout的超出 |
| 预热 | 前32 rollout＝524,288 decisions，计入20M |
| 参数 | 只训练value_head的LayerNorm和Linear；actor、encoder、GRU冻结 |
| 目标 | collector实际float32 shaped reward的完整终止轨迹反向累计，gamma=1，无bootstrap |
| 更新 | 新完成轨迹状态打乱，2 epochs，batch≤1024；原LR、value coefficient、梯度裁剪 |
| 后续 | 恢复原PPO，λ=.98；同一个包含全部参数的Adam |
| 布局 | seed130000000，64 workers/16 shards；原control模型与PPO配置 |

每worker跨rollout保留未终止轨迹的冻结隐藏特征与reward；死亡、成功、策略cycle/step limit均有实际失败回报。backend故障硬退出，不能作为样本。完成轨迹更新后释放，不重复使用。第32次更新结束丢弃尚未终止的**目标缓冲**并报告状态数，实际环境、RNG、memory和previous action/reward原样进入PPO；不排空环境。这存在有限窗口截尾偏差。

启用预热时训练checkpoint为v6，记录阶段、cursor、未完成轨迹、模型/Adam、seed/环境/RNG、episode limiter、GRU及previous experience。配置与阶段不一致拒绝；新配方exact resume不自动runtime-rebind或environment-migrate。关闭预热仍采用v5。旧v5父模型只经过显式weights-only迁移；未增加任何续训白名单。实现身份发生变化，native/observation/action/reward语义保持本轮λ环境身份，见[身份与多选审核](READINESS.md)。

## 评估与健康门禁

每2M评估同一开发区间`[8000012000000,8000012000512)`，每1M存checkpoint，安全中断另存完整checkpoint。周期选模仅看联合成功计数，平局保留更早候选；不以Act1 reach、reward或失败深度否决更高成功计数。策略循环计作失败并报告terminal screen、last action和选择上下文；step limit单列。backend错误、truncation、timeout、nonfinite和证据损坏阻断。

固定20M终点是主要结果。终点、冻结90M和实际周期所选模型在同runtime/环境的4096个正常开局`[8000013000000,8000013004096)`评估，分别保存endpoint/reference/final-evaluation。**final.pt是固定终点；final-evaluation.json评估实际所选checkpoint，两者不可混淆。** 周期最高计数另列。缺少deployment export可分析，其他bundle缺项或hash错误拒绝。

主要判据：固定终点相对冻结90M联合成功率≥+1pp（4096局至少净增41局），精确双侧McNemar p≤.05，执行和证据健康。未达到记`NO_CONFIRMED_GAIN`；不降低标准，不自动加预算。历史4M control仅作早期参照。最终保留集`[9000000000000,9000000004096)`继续封存。

发布前扫描实际评估记录；submit时再扫新checkout的已导入记录。与新开发区间冲突即拒绝，不静默换seed。纯配置中的未执行区间不算已用。

## NUS启动门禁和48小时续接

登录节点只导入父证据、校验SHA/配置和提交作业，不导入Torch。GPU节点先构建native，执行完整配置构造校验，并用固定64:16布局测量PPO阶段吞吐（诊断副本，未计入生产）。预算率取本次测量与历史114.7 decisions/s的较小值；不是预热阶段测速或性能收益证明。

真实父模型门禁在临时副本检查：父SHA/tensor有限性、GRID取消/同名不同实例/完成/恢复和编码、value-only参数及Adam变化、固定actor/GRU输出、完整回报独立复算，以及跨rollout、最后预热更新、首次PPO更新三个checkpoint的下一次采样/更新和**完整保存状态逐项精确一致**。不使用数值容差。约36个探针更新全部丢弃，生产恢复门禁之前保存的初始状态，生产预算仍从0新增决策开始。失败保存报告并退出；门禁报告hash贯穿allocation链。

预先提交三段48小时allocation，以afterok连接，一个逻辑运行目录，单写者锁和独占ledger。只有正常预算切片完成或记录确认的SIGTERM安全中断可续接；SIGINT、崩溃、配置/runtime不兼容或checkpoint损坏拒绝。preflight在每段对实际checkpoint做真实恢复检查。预算已经完成，后续段校验checkpoint SHA后退出。

普通切片留6小时评估/收尾余量，并对训练速度使用1.5倍安全系数。可完成预算的段留24小时给三次4096局确认；时间不足则将最后一个rollout留给后段。三段是保守规划，不是必定完成保证。若最后一段仍不足，保存安全checkpoint并退出要求检查，不能自动增加预算或重复训练。

## 本地验收与明确延期

本地仅执行纯Python/mocked测试、全配置轻量检查、静态检查、身份复算和真实记录seed扫描。结果见`local-validation.json`。没有加载Torch、真实模型、native库或GPU，也没有运行rollout、构建或测速。

真实梯度冻结、tensor有限性、实际collector回报、worker/native恢复、真实PPO等价、实际吞吐和GRID编码运行验证均**尚未执行**，必须在NUS门禁取得结果。完整计算型Python测试集未在本地运行；现有native/模型回归范围在门禁覆盖的机制有限，不宣称整个测试集或全部Act2通过。

服务器训练开始后，本地继续有界模拟器校验。当前训练checkout固定提交，不随本地修复变化。晚幕问题登记后续；影响Act1/Act2或共享机制的实质差异重新评估训练资格。任何新修复收益不能归因于critic方案。
