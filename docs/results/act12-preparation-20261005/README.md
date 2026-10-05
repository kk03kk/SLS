# Act1–2 4M 训练前证据：2026-10-05

这是本地准备与诊断证据，不是NUS Act1–2训练结果。正式方案见 [试训说明](../../act12-pilot-launch.md)。

## 已知结果

固定90M parent的完整Act1服务器结案保留在 [90M归档](../win90m-20261005/README.md)。本地新暴露的32个诊断seed为 [8000008000000,8000008000032)：24入Act2、4入Act2Boss、1真正ACT_2_CLEARED，成功Wilson95%区间约0.554%–15.744%。23次Act2失败中20次发生于Boss前。原始 [90m-zero-shot-32.json](90m-zero-shot-32.json) 字节保留，SHA256为 `199c25d4629c726e184e00d54b539dda8e8d4ca4063b53df3666ad4aca9057c2`。

这说明存在可成功轨迹，不是总体胜率的精确估计；不同seed、原生规则和runtime的旧70M探针不能用于估计训练收益。Windows DL/Torch2.10/CUDA13的本地结果不是Linux NUS/Torch2.6或原版游戏胜率。该块已登记为暴露诊断集，正式开发和最终保留集均不使用它。

## 有限规则资格

原版JAR SHA256 `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`。私有javap字节码重新核对BookOfStabbing、GremlinLeader、Taskmaster、Byrd、Chosen、SlaverRed/Blue；类与字节码输出SHA记录在 [native-rule-traces.json](native-rule-traces.json)。Flight/Hex/PainfulStabs三个power的字节码身份另外保存在 [stock-power-reference.json](stock-power-reference.json)。可用同一JAR与 `javap -classpath <stock-jar> -c -p com.megacrit.cardcrawl.powers.FlightPower`（另两类替换类名）复算；Flight非HP_LOSS/THORNS伤害除以2，Hex非攻击牌加Dazed，PainfulStabs正常造成伤害时加Wound。此轮没有验证这些power的所有边界。

不上传游戏JAR或原版反编译内容。

独立预期：A20 Book多刺初始7×2并产生2张Wound，单刺24并产生1张；三奴隶开场13+7+14，Taskmaster加3张Wound并在伤害后加1力量；Leader鼓舞给自己和活小怪5力量、只给小怪10格挡；Chosen开场Hex；A17+Byrd需要四次攻击命中，每次6点Anger在Flight下造成3，第四次后眩晕，不影响其他Byrd。

对应六个受控native场景与回归已执行。native源身份仍是 `fe354a23c7584d68d0a2b6681b8dfd4e97d98e107b7ddf57c479060d91468f39`，此次不更改模拟器语义。字节码核对加native受控场景不等于原版runtime逐轨迹parity，也没有覆盖所有多目标、relic、事件、奖励、隐藏RNG交互。既有跨幕结构/奖励回归保留此限制。

## 本地实现证明

真实90M权重迁移脚本 [transfer_micro_probe.py](transfer_micro_probe.py)：全权重含critic逐tensor相等，fresh Adam/update、worker/RNG保持，当前新阶段checkpoint同runtime重放产生完全相同更新指标。两个16-step小更新仅验证实现，不能推断正式PPO性能；没有使用开发确认或最终保留seed。

[diagnostic_probe.py](diagnostic_probe.py)在已暴露32诊断seed重复固定90M评价，验证新增记录前后的action/route/outcome一致并保存入幕anchors。它不训练、不跳过战斗，使用单独输出路径拒绝覆盖原始证据。冻结Act1 critic不能当作Act2胜率预测。

最终本地验证摘要与证据SHA写入validation.json；不会将本地短更新命名为服务器训练成绩。下一步服务器命令在任务交付中固定commit，只提交一个24h作业。

最终当前源码全量回归：**1068 passed、1 skipped、4 warnings**（196.55秒）；跳过是历史encoding不兼容模型，warnings为明确的runtime/provenance rebind测试。Ruff、27/27配置、registry、vocabulary及提交dry-run通过。原始本地日志保留于local/reports/act12-preparation-20261005/，SHA见validation.json。
