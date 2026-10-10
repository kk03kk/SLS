# A20H 隔离模拟器校验：首轮实际结果

研发分支 `codex/a20h-parity-qualification` 从原工作区干净 HEAD `e1902d6a1bb56166bc55f6d7c646c6b560dfe541` 隔离建立。原目录 197 个待提交文件完整保存为本地 ZIP 和哈希；仅将 183 个校验相关文件复制到候选工作树。保持基线主要行尾风格以减少无意义 diff，转换仅发生在候选目录，canonical native 来源不变。后续实际复核原目录 status 与全部 197 个文件 SHA256 未变。AGENTS.md、训练模型、PPO/网络/奖励/生产训练 CLI、服务器 run 均未修改。

该候选集含此前未提交的晚幕规则及 Oracle/校验工具修复，不是新的训练发布，也没有证明全部代码已完整原版认证。canonical native 来源为 `6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0`，与 NUS 927398 的 `6efbb772…` 分开。DL/Python3.12，CUDA 禁用，单任务 native 编译完成；产物仅位于新工作树。构建使用已验收 compiler 工具作为输入，不覆盖原工作区 native 产物。

## 已完成的独立 runtime 验证

调用现有严格 replay CLI，把原版 capture/build/launch/场景/action 身份重新绑定到当前 native；启用 production-public projection，并保存 first divergence 与后续边界。没有启动原版游戏，没有使用 GPU，没有恢复训练。

| 原版归档 | 案例数 | 严格匹配 | 原始不相等 |
|---|---:|---:|---:|
| Act3 普通敌人 | 21 | 17 | 4 |
| Act3 Boss/精英 | 21 | 18 | 3 |
| Heart/Shield/Spear 受控交互 | 12 | 12 | 0 |
| Heart 死亡分支 | 6 | 6 | 0 |
| Awakened 重生 | 6 | 6 | 0 |
| Darkling 复活 | 6 | 6 | 0 |
| Reptomancer 目标/召唤 | 3 | 0 | 3 |
| 合计 | 75 | 65 | 10 |

10 个原始差异的首处位于死随从 MINION 或 Exploder EXPLOSIVE residual powers。这里不把它们一般归零，不把 75 条写成全部严格通过，也不从首处差异推断后续所有字段都相同。原始 differences 全部保留；历史窄分类可指导后续独立检查，不能直接成为当前发布豁免。统计与原始输入 hash 见 [evidence.json](evidence.json)。这些是受控案例，不是正常开局通关率。

通用 encounter 比较器在 Heart 胜利后没有 combat_state 时先前抛 KeyError。本轮补成明确的“不支持此边界，使用终点/奖励流程比较”错误，增加回归检查，没有用空战斗对象掩盖它。

新增独立 CPU 入口 `tools/replay_heart_terminal_archive.py`。先验证原版 capture/fixture/build/manifest/recovery 全部身份、场景与 action/seed/初态；独立从 raw stock terminal 提取 HP/maxHP、胜负、药水与 RNG，再验证历史 fixture 确实来自该原始边界，最后与新 native 实际运行比较。6 条全部匹配，覆盖 3 死亡/3 胜利终点。

3 个死亡终点的独立 battle restore 完整相同。3 个胜利终点的独立 battle loader 要求 combat_state，无法恢复胜利后的无战斗快照；报告为 **unsupported、equal=null**。这不是通过，也不是已证明生产 FullRun restore 有缺陷；FullRun 有单独的恢复实现，必须另行绑定原版完整流程验证。终点资源匹配不等于牌堆/所有回调/自然 A20H 完全一致。

可复现终点检查（新的 output 文件名）：

```powershell
conda activate DL
$env:CUDA_VISIBLE_DEVICES = '-1'
$env:PYTHONPATH = "$PWD/src;$PWD"
python tools/replay_heart_terminal_archive.py `
  --capture D:/SLS/local/audits/fullrun-parity-20261007/heart-lethal-capture-r1.json `
  --oracle-build D:/SLS/local/build/oracle/fullrun-parity-r15.build.json `
  --output local/reports/heart-terminal-recheck-new-id.json
```

## 本地验收与检查边界

首轮相关测试 744 通过、17 个缺本地原版文件的检查跳过。随后只读复制 4 个原版文本输入到新 worktree 的 ignored cache，记录文件 SHA；原版概率/地图/内容池的 23 项检查全部通过。

首次全量测试发现测试自己的配置使用默认编码写入中文路径，而实际读取约定 UTF-8，出现 1 失败。修复仅为测试 fixture 显式 `encoding="utf-8"`；3 项相关测试通过，首次失败日志保留。最终 CPU 全量 **1600 passed、2 skipped、4 expected warnings**。跳过为隔离目录缺 exported Act1 policy 和 CPU 禁用 CUDA；不代表模拟器规则检查被忽略。全仓 Ruff 通过。

配置检查 **31/33**，两个历史 lambda experiment 拒绝原因均为 `bound implementation changed`。候选集合改变了契约来源，不能沿用其历史身份；没有增加兼容白名单、修改历史 config 或绕过检查。当前没有可直接运行的新训练配置。

## 接下来做什么

1. 三钥匙实际取得流程：原库存 17 个 bytecode 文件身份已核验，但只有部分红钥匙/蓝钥匙方法完成本轮静态核对。下一步启动新受控原版场景，从未持有钥匙开始真实回忆/开箱/燃烧精英取得；检查选择和资源机会成本、奖励链接及 relic callbacks。不得把初态已有三钥匙的入口测试冒充取得流程。
2. Act4 实际连续路由和生产 FullRun restore：覆盖入口、休息/商店、Shield/Spear、奖励、Heart，保存每个稳定边界。独立 Heart battle loader 的不支持不能替代这个验证。
3. 六种有序 A20 双 Boss 既有证据按该来源重新绑定，并复查尸体窄分类对目标、触发顺序与后续奖励无影响。
4. 正常 Neow 到后段的连续轨迹，及可达卡牌/遗物/事件组合覆盖。受控场景用于找规则差异，自然轨迹用于验证连接；均不作为训练样本或正式胜率。

NUS 927398 继续使用已绑定的评估版本；用户尚未提供运行/结束结果，本轮不推断它仍在运行或已经完成。研究分支不合并 main，不更新训练发布。本轮证据推进了完整 A20H 的资格检查，但全项目目标仍未完成。
