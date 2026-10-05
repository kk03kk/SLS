# Oracle 完整源码恢复与 v11 验证

日期：2026-10-05。[完整构建与使用说明](../../../native/oracle/README.md)。

## 结论

基础 Oracle 的完整源码已经从本地 Git 历史恢复到正式目录。新构建入口 `tools/build_oracle.py` 不读取旧 Oracle JAR；四个外部游戏/Mod JAR 仅作为编译依赖，不打包进项目产物。

先恢复旧行为作为基线，与本机旧包对照：成员清单没有增加或丢失；28 个 class 字节相同，另外 4 个 class 的反汇编指令、签名和常量相同，字节差异属于编译元信息；Mod 描述元数据有差异。随后明确修复模式隔离和跨局 RNG 探针基线，不将这些行为修复冒充旧包字节恢复。

最终产物：Mod 1.1.0、`spirecomm-parity-v11`、`sls-oracle-mode-v1`，33 个 Java 8 class。

```text
Oracle JAR SHA-256:
d41a1cae5587fc7f2ea9abedf172431dd36e7bb8199deea135e417d01030faa9
```

完整哈希绑定、依赖版本、基线对照、JVM probe 与有限实机检查见 [qualification.json](qualification.json)。原始 JAR、采集载荷、启动/恢复日志与完整测试输出留在忽略目录 `local/`，没有将游戏文件或个人原始载荷上传。

## 实质修复

- v10 的 production 模式只隐藏诊断字段，仍替换部分随机卡牌选择。v11 的 production 明确调用原版路径，不初始化验证 math RNG，不提供场景改写命令。
- validation 必须显式指定；继续保留确定性 RNG、受控场景和诊断证据，不能把该模式的结果标为正常原版自然局胜率。
- 模式每 JVM 固定，未知值失败；数据带版本与模式标记。
- 新局/载入时清除遗物探针的旧 RNG 基线，避免跨局复用。
- 实时 AI 后端要求 v11 production；缺失/旧版本、validation 或含隐藏诊断字段的消息在发出动作前拒绝。
- 构建与检查绑定源码、资源、依赖、编译器、JAR 成员和最终文件；拒绝默认覆盖旧证据及覆盖依赖。
- 实机检查有备份恢复，拒绝与已运行游戏并行；安装入口先核验产物，保存校验过的旧 JAR，再原子替换。

## 验证结果

两次独立源码构建的 JAR 完全一致。JVM probe 验证默认 production、显式 production、显式 validation、未知模式拒绝；production 的三条随机卡牌补丁均直接委托原版，验证 seed patch 不初始化替代 RNG，场景命令不开放。validation 的基线重置测试通过。

最终 JAR 在原版游戏、BaseMod、CommunicationMod 三个 Mod 的条件下，分别通过 production 与 validation 短程检查：A20 Ironclad 正常开局，进入第一场战斗，核验版本/模式/字段，validation 执行 `parity_card STRIKE_RED 0`，返回主菜单。两个恢复日志均为 `RECOVERED`，且采集结束后逐项复核保护文件哈希。

本地 Python 验证：全量 1043 passed、1 skipped、4 warnings；随后新增安装备份/幂等检查，Oracle 工具相关 7 项测试全部通过。Ruff 通过。全量测试中发现并修复了查看器测试的异步等待错误：需要等到较新 revision 且状态 PAUSED，不能只把 resume 发布的 revision 当作 pause 已完成。第一次失败输出保留，最终通过输出保留。

跳过项需要本地导出的 Act 1 策略。warnings 是测试中显式 runtime/provenance rebind 提示。Java 编译提示 Java 8 target 和已有弃用 API；编译成功，不声称消除了这些兼容性维护提示。

## 本机安装与来源边界

已通过安全安装入口将最终 JAR 安装到本机游戏，旧包完整备份在 `local/build/oracle/installed-backups/`。旧包 SHA-256 为 `d034ed56bf7c93de8c82404ff57aaba62a9256def8402441251481f91c50e191`。安装不修改存档、配置或 Mod 选择；当前 Mod 列表为 BaseMod、CommunicationMod、Oracle。

模拟器 native 身份与训练 implementation 身份未改变。Oracle 的修复是原版接入/验证契约变更，不改模型输入、权重、PPO 或 NUS 训练。旧 Oracle 的实验记录保留原哈希；不追改历史成绩，也不宣称旧 production 已是无 RNG 替换条件。

这里解决的是完整源码构建、模式隔离与有限运行验证。**没有运行完整胜率评估，也没有证明 Oracle 或模拟器的全部 Act 1/2/3/Heart 行为与原版完全一致。**
