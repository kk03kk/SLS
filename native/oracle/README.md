# Oracle：完整源码、构建与运行模式

本目录现在包含完整的项目 Oracle 源码。构建只依赖 Java 编译器和四个外部 JAR，**不依赖预先存在的 SpirecommParity.jar**。

Oracle 是原版游戏的观测与验证接入 Mod，不是训练模拟器。它将 CommunicationMod 消息补齐为 SLS 所需的公开状态，并在明确的验证模式下提供 RNG 证据和受控场景命令。

## 源码组成

| 文件 | 职责 |
| --- | --- |
| `CardStatePatch.java` | 公开卡牌动态字段和奖励卡预览 |
| `EventStatePatch.java` | 已展示事件信息、Neow 选项和 Match 计数 |
| `CommunicationStatePatch.java` | 公共运行状态、敌人意图与版本/模式标记；验证模式的诊断投影 |
| `BatchResetPatch.java` | 使用原版流程返回主菜单 |
| `ContinuePatch.java` | 触发原版存档继续 |
| `MatchInputPatch.java` | 将语义 Match 选择送入原版输入路径 |
| `DiscoveryTimingPatch.java` | 记录 Discovery 帧更新信息，不替换游戏动作；1.2.2在production完成后输出独立本地clock日志，字段不进入策略消息 |
| `GeneratedChoiceNotificationPatch.java` | Oracle1.2.1：通知CommunicationMod同屏连续生成选择；不修改stock动作、选项或RNG |
| `ParityRng.java` | 验证用 math RNG 和探针基线；新局/载入时清除旧基线 |
| `CardGroupRngPatch.java` | 仅验证模式替换未显式使用游戏 RNG 的随机卡牌选择 |
| `DungeonSeedPatch.java` | 仅验证模式初始化 math RNG |
| `OracleScenarioPatch.java` | 仅验证模式的卡牌、药水、遗物、遭遇、事件等受控场景命令 |
| `OracleMode.java` | 每个 JVM 固定的 production/validation 模式契约 |

`resources/` 中的五个 allowlist 是已有校验环境的固定内容/原版构造键映射。资源不是游戏 JAR，也不代表全部内容已完成保真验证。卡牌、药水、遗物、遭遇和事件范围保持历史包的映射，不在恢复源码时悄悄扩展。

`act2-scenes.json` 固定首批24项A20义务/72个seed；三项正常开局系统义务由
[系统场景补充](../../docs/results/act2-qualification-20261006/system-scenes-v2.json)绑定。
`parity_act2`显式设置A20/Act2/floor及受控初态，后续由原版动作系统执行；
legacy `parity_encounter`调用保持A0兼容，不能代替A20资格。
`_stock_direct`只在validation提供独立对象投影。详见[本轮证据与限制](../../docs/results/act2-qualification-20261006/README.md)。

基础源码恢复自 Git revision `4848867580b348e32c876b2d53ac278f32c0e463` 的 `java/oracle-mod/`；该目录在 `5dd256369eb4f73f50dfa06fa925c5f0d2cfd9e1` 被移除。已有两个新版观测补丁保留。逐文件原始哈希、资源来源和旧包清单见 [source-recovery.json](source-recovery.json)。这是恢复项目自己的源码，不是反编译并发布原版游戏代码。

## 从零构建

需要可支持 `--release 8` 的 JDK（本次验证 JDK 21.0.9），以及本机合法安装的游戏和 Mod：

1. `desktop-1.0.jar`
2. `ModTheSpire.jar`
3. `BaseMod.jar`
4. `CommunicationMod.jar`

Python 工具显式接收这些路径。路径包含空格时由 shell 正常加引号，勿将自己的安装目录硬编码到源码中。

```powershell
conda activate DL
python tools/build_oracle.py --javac D:/java/bin/javac.exe --game-jar D:/Steam/steamapps/common/SlayTheSpire/desktop-1.0.jar --mod-the-spire D:/Steam/steamapps/workshop/content/646570/1605060445/ModTheSpire.jar --base-mod D:/Steam/steamapps/workshop/content/646570/1605833019/BaseMod.jar --communication-mod D:/Steam/steamapps/workshop/content/646570/2131373661/CommunicationMod.jar
python tools/verify_oracle.py local/build/oracle/SpirecommParity.jar
```

上例是维护者本机路径；其他机器替换四个依赖和 javac 路径即可，Linux 也可编译。原版启动/恢复验证器当前仅支持 Windows。

输出为忽略目录下的 `SpirecommParity.jar` 和 `SpirecommParity.build.json`。构建不会清空整个目录、不会将依赖打包进 Oracle，也不会默认覆盖已有产物。重新构建可指定新 `--output`；确认需要替换时才用 `--force`。

构建记录绑定所有 Java/资源输入、四个依赖哈希、编译器版本和哈希、每个 JAR 成员及最终 SHA-256。JAR 文件顺序、时间戳、权限固定；同一编译器与依赖下两次独立构建字节一致。Java class 目标版本为 52（Java 8）。不同 JDK 版本的输出可能不同，不能要求跨编译器二进制相等。

## 两种模式，不能混为自然局评估

| 模式 | 启用方式 | RNG/场景 | 用途 |
| --- | --- | --- | --- |
| `production` | 默认；也可指定 `-Dsls.oracle.mode=production` | 随机卡牌选择交还原版；无 math RNG 替换和受控场景入口；隐藏诊断字段不输出 | 正常原版游戏、AI 演示及真实自然局验证 |
| `validation` | 必须指定 `-Dsls.oracle.mode=validation` | 启用固定 math RNG、诊断字段、受控场景与 RNG 重置 | 差分检查、机制探针、确定性重放 |

未知模式拒绝执行。模式每个 JVM 固定，不能在同一局切换。两种模式保留公开观测补丁和必要的 UI 命令，但这不等于证明 Oracle 对原版所有行为完全无影响。

**旧 v10 的 production 只隐藏诊断字段，没有禁用随机卡牌选择补丁。** 因此不能仅凭“production”标签将历史结果宣称为未修改 RNG 的原版自然局结果。v11 修复这一点，旧实验记录保留原版本身份，不追改结论。

新协议标记：`_parity_schema=spirecomm-parity-v11`、`_oracle_contract=sls-oracle-mode-v1`、`_oracle_mode=production|validation`；Mod 版本 1.1.0。这些是传输来源信息，不新增策略输入字段或改变模型参数。实时 AI 接入拒绝旧/未知 Oracle 和 validation 模式；普通验证后端仍可显式用于校验模式。

## 验证与安装

静态产物核验不会启动游戏，且拒绝 JAR、成员哈希或当前源码身份不一致的构建。

关闭正在运行的游戏后，可以执行有备份与恢复的有限实机验证：

```powershell
python tools/verify_oracle.py local/build/oracle/SpirecommParity.jar --runtime --mode production --output local/reports/oracle-production.json
python tools/verify_oracle.py local/build/oracle/SpirecommParity.jar --runtime --mode validation --output local/reports/oracle-validation.json
```

必要时加 `--game-root`。每次使用新的输出路径。验证器临时安装候选 Oracle，备份游戏配置、Mod、已有存档与偏好，结束后恢复；恢复日志保存在 `local/runs/oracle-qualification/runtime-backups/`。不要同时运行两次验证或与自己的游戏局并行。此检查只涵盖正常开局、进入战斗、模式/字段、场景命令与返回菜单，不测通关率、不代替完整原版保真认证。

长期使用时，将核验后的 JAR 安装到本机游戏 `mods/SpirecommParity.jar`，保留原文件备份，并选中 BaseMod、CommunicationMod 和 Oracle。手动正常启动默认 production；受控采集启动器会明确传 validation 标志，策略 canary 可用 `--oracle-mode production`。

也可使用验证器的安全安装入口：`python tools/verify_oracle.py local/build/oracle/SpirecommParity.jar --install`。游戏必须关闭；工具保存并核验旧 JAR 备份，原子替换安装文件，不更改 Mod 列表、游戏配置或存档。安装记录和备份在 `local/build/oracle/installed-backups/`。

`build_observation_oracle.py` 和 `build_full_audit_oracle.py` 保留为历史 JAR 修改工具，正常从零构建应使用新的 `build_oracle.py`。

本次恢复、编译、JVM 模式和原版游戏有限验证的可追溯摘要见 [验证记录](../../docs/results/oracle-source-recovery-20261005/README.md)。
