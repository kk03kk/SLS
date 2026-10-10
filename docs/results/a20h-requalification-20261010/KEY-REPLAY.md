# 红、蓝钥匙 native 对照与恢复：有限范围通过，剩余问题明确保留

2026-10-10。研究代码 `f5c1300d9b2ba1cf498d5ab65656d1734fd3e2e5`，native 仍为 `6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0`，二进制仍为 `ce8f500b…`。本阶段没有修改 native 规则、模型、PPO、奖励、训练配置或恢复契约。与 NUS 927398 的 80b 环境继续分开。

## 实际执行与比较范围

新增 Oracle 1.3.43，源码独立构建，SHA256 `51a3db3e7208b2c98ffc3861a7296c9f619f12df4ce57597e0cab42f96c9f870`。只读记录五类有序遗物池、宝箱初始属性、终幕开启标志及卡牌 misc；全部位于 validation 的 `_stock_direct`，production 原版 smoke 验证审计字段与验证命令不暴露。新增测试实际扰动池、宝箱和 RNG 后，公开决策不变。

重采同一批24个诊断 seed，增加休息结束后的实际 `proceed` 与地图边界。它们是替代/补充证据，不计作新的独立样本，不是自然到达房间的轨迹、训练课程或胜率。原版与 native 在受控房间开始前对齐公开库存与相关内部初态；没有复制原版的动作结果或已生成奖励给 native。

| 检查 | 实际结果 | 范围限制 |
|---|---|---|
| 休息/回忆资源、14条 RNG、原生恢复后缀 | 15/15匹配 | 资源计数器沿用现有公开 ABI，原始计数器差异另存 |
| 开启终幕时休息页完整公开观测及合法动作 | 12/12匹配 | 剩余3条为主动禁用终幕的分支 |
| 休息后真实自动进入地图的观测、合法动作、RNG | 15/15匹配 | 只校验该地图边界，没有证明从 Neow 自然到达它 |
| 开箱＋选钥匙/遗物的公开观测、合法动作、资源、奖励互斥 | 9场景、18决策边界匹配 | 起始宝箱属性与遗物池来自原版，不等于已验证全部房间生成 |
| 开箱及选取后的 RNG 与五类遗物池顺序 | 18/18匹配 | 不使用未对齐的初始池猜测奖励，也不注入开箱后的奖励 |
| 蓝钥匙每个决策前恢复并执行相同后缀、决策后完整恢复 | 18/18匹配 | native 自身完整快照相同；不是原版存档跨实现恢复 |
| 实际 native 地图进入生成宝箱属性与 treasure RNG | 8/9可检查且匹配 | 1条受控原版节点不可达；其它 room-entry RNG 时钟差异保留，未宣称全部时钟匹配 |

初始蓝钥匙页9条完整公开观测也全部匹配。所有原始比较与差异保存，不把表中的有限范围归并为“完整 A20H 全部通过”。

休息页原版结束后仍处于 REST/proceed，native 已处于 MAP，这是不同 UI 边界。本轮使用**现有生产 OriginalBackend 的自动处理逻辑**记录原版实际进入地图的命令与状态，再比较地图，没有伪造 REST→MAP 投影。

## 保留的问题及判断

**终幕未开启的3条：** 原版没有回忆选项，而原生引擎没有 `Settings.isFinalActAvailable` 对应状态，HEART profile 下仍提供回忆。报告保留公开观测/动作差异，`final_act_flag_representable=false`；没有靠伪设红钥匙已持有消除选项。这是环境能力表示的缺口。完整 A20H 目标要求终幕已开启，当前不能将此差异定为该目标内训练失效的根因；普通 horizon 的 profile 过滤须另行区分，不能冒充原生旗标已实现。

**不可达的受控宝箱节点：** seed131200319 的原版初态选择第一格 TreasureRoom，却没有通向该格的上一层路径。当前工厂只检查 room class，没有检查可达性。受控开箱/奖励方法可检查且匹配，但实际进入该格的对照明确 `UNSUPPORTED_CONTROLLED_NODE_NOT_REACHABLE`、结果非通过。没有添加边或改选另一个 native 节点假装原状态对齐。下一步应引入独立冻结的新场景版本，要求真实可达节点；旧工厂/manifest/JAR/轨迹继续按旧身份封存。

**计数器的表示差异：** Burning Blood、Regal Pillow 等原版中性显示 counter=-1，native 公共 counter=0。按照已有 `normalize_relic_counter` 的策略 ABI 匹配，但所有 `raw_resource_differences` 原样保留。没有新加兼容白名单，也不把 raw 资源写成严格逐字节相同。该范围没有覆盖所有已消耗遗物的内部旗标及后续回调。

**受控上下文的不足：** 开箱比较已对齐遗物池和初始宝箱；休息无需这些池。燃烧精英 buff、其它事件/遭遇池、Boss 顺序及从正常开局到此位置的隐藏历史未全面资格验证。宝箱创建检查只核对宝箱属性及 treasure stream；地图进入会重置房间派生 RNG，而原版工厂直接执行受控 `onPlayerEntry`，其它时钟差异完整保留，不能由构造器局部检查冒充自然路由。

## 验收与重现

提交代码后的 CPU 全量 **1615 passed、2 skipped、4 warnings**，全仓 Ruff 通过。两个跳过仍为隔离目录缺 exported Act1 policy、CUDA被禁用。独立 Oracle build 身份再次通过。8个历史及本轮 runtime journal 都为 RECOVERED，每个63个受保护文件逐项 hash 复核相同；原 D:/SLS 的197个待提交文件与 status hash 未变，无游戏进程遗留。

在 DL / CPU 环境设置 `CUDA_VISIBLE_DEVICES=-1` 和 `PYTHONPATH="$PWD/src;$PWD"`，归档 replay 不启动游戏：

```powershell
python tools/replay_rest_key_archive.py `
  --capture local/reports/key-native-20261010/key-continuation-r1.json `
  --oracle-build local/build/oracle/key-continuation-r1.build.json `
  --manifest native/oracle/resources/spirecomm/parity/fullrun-key-acquisition-r1.json `
  --scenes ruby-recall ruby-rest-alternative ruby-rest-regal-pillow ruby-already-held ruby-final-act-disabled `
  --output local/reports/key-native-recheck/rest-new.json

python tools/replay_blue_key_archive.py `
  --capture local/reports/key-native-20261010/key-continuation-r1.json `
  --oracle-build local/build/oracle/key-continuation-r1.build.json `
  --manifest native/oracle/resources/spirecomm/parity/fullrun-key-acquisition-r1.json `
  --output local/reports/key-native-recheck/blue-new.json
```

全部输出独占创建。版本、输入/原始报告和交付包 hash 见 `key-replay-evidence.json`。本轮不更换服务器 checkout、不追加训练，不合并 main。下一步先修受控工厂的可达节点选择并补入口，再以可达燃烧精英初态校验绿钥匙，接入三钥匙/双 Boss/Act4 连续流程与恢复。
