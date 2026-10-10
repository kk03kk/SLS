# Act4 地图公共契约修复及原版连续回归

日期：2026-10-10。基于上一版 `b4c498e` 的真实原版连续证据修复 SimulatorBackend；只在独立研发分支修改 Python 适配层，不改 C++ 游戏规则、网络、PPO、奖励、课程、恢复白名单或服务器发布。

## 问题和修复

原版 TheEnding 的休息、商店、精英是三个普通地图节点，Boss 在实际 `boss_available` 时以公共身份 `map:boss` 出现。之前 native 适配层提前展示第四个普通 Boss 节点，并用前三幕的第 14 行条件生成最后一个地图动作；Act4 精英后得到 `map:3:3`，与原版合法动作 `map:boss` 不同。这既改变策略的公开输入，也阻止同一动作跨后端执行。

修复仅改变 Act4 的公开呈现：普通节点列表不包含 native 内部 Boss 节点；到达 Act4 最后普通行 2 后生成 `map:boss`，沿用已有公共 Boss 节点 `(0,15)` 的呈现。native 地图拓扑、合法 bits 和游戏状态不改。前三幕继续使用原来的第 14 行规则。

独立加载准确的旧 Python 文件及保留的旧二进制，确认冻结原版回归在入口公开地图处拒绝 predecessor；其余完整 Observation 字段相同。这不是只检查新实现自身。

## 真实原版目标验证

只读使用原版 `stock-r2.json`、Oracle 1.3.50 sealed JAR/build、场景和完整恢复记录；在新来源身份下重新生成双 Boss 入口和 flow 证明，不修改旧报告的来源摘要。新 native 的 C++ 游戏规则与旧版相同，但项目来源摘要包含 Python adapter，因此必须重新构建，使用独立 `local/build/native/act4-map-fix`，原二进制仍在原路径且 SHA 不变。

固定 seed 131200440 的 **19/19** 原版 Act4 决策，从入口经休息、商店、Shield/Spear、奖励、Heart 到实际胜利，完整公开 Observation、完整合法动作集合、所选动作、有效 RNG、terminated/truncated、success 和 reason 均相同。工具保留每个检查结果与完整 native checkpoint，没有忽略地图差异或强制替换动作。

新增集成回归同时使用冻结的全部原版公共目标和实际记录动作，并从 **20/20** fullrun checkpoint（包括 Heart 胜利终点）执行全部剩余决策，逐边界严格比较完整 native snapshot。此结果补齐了此前 isolated battle loader 无法验证胜利终点恢复的**这个受控成功案例**，不推广为所有死亡、断点或循环策略。

CPU 全量 **1711 passed / 2 skipped / 4 warnings**，117.10 秒；Ruff、词表检查通过。跳过为本地导出策略缺失与 CUDA 不可用，警告来自现有 checkpoint 来源重绑定测试。配置检查 **31/33**，两个 `act12-lambda-*-r1.json` 的 bound training implementation 不匹配；这些绑定路径本轮与 base commit 无变化，属于已有过期计划，未重新绑定或改写历史配置。测试耗时不是性能基准，不宣称加速。

原工作区 197 个待提交文件、status 和游戏恢复目标与快照一致。本轮不启动原版、训练或 GPU，只离线重放既有真实原版轨迹。

## 版本与限制

旧来源摘要 `6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0`；新摘要 `e4f8452ff3cea688e49a362488814d4c2e29d9b721bcc6cd71c8d9f1d9fa733a`。现有契约确认没有 old→new 状态恢复例外；服务器训练、旧 checkpoint 和历史报告继续保留原身份。新构建在 base commit 加这份工作树修改时完成，完整来源摘要和二进制 SHA 单独绑定，不能把内嵌 base Git commit 当成无修改构建。

只有一条受控强牌组成功轨迹，商店仅进入并离开，没有购买、移除牌或自然构筑；Heart 不覆盖全部强化周期，尚无连续死亡路径。这不是 A20H 正式胜率或完整 simulator qualification，仍 `training_gate=NOT_QUALIFIED`。下一步增加连续死亡、商店交易及更长 Heart 战斗，并检验冻结策略的公开历史/循环记忆影响；不用小样本强牌组结果替代智能体能力。

已有本地构建的重放命令（在本研发 worktree，DL / PowerShell）：

```powershell
conda activate DL
$env:CUDA_VISIBLE_DEVICES = '-1'
$env:PYTHONIOENCODING = 'utf-8'
$env:PYTHONDONTWRITEBYTECODE = '1'
$env:PYTHONPATH = "$PWD/src;$PWD"
$env:SLS_NATIVE_BUILD_DIR = "$PWD/local/build/native/act4-map-fix"
python -m pytest tests/simulator/test_continuous_ending_stock.py -q
```

这是本地重放，不是服务器执行授权或迁移命令。版本、校验和封存清单见 [act4-map-fix-evidence.json](act4-map-fix-evidence.json)。
