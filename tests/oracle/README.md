# Oracle JVM 探针

`OracleModeProbe.java` 是仓库自有测试源码，应上传 GitHub；编译后的 `.class`、依赖 JAR 和运行日志保存在 `local/`，不上传。

它在独立 JVM 中检查 production 是否在触碰 game 对象前委托原版 CardGroup RNG、是否不初始化替代 math RNG、是否不开放场景修改命令，以及 validation reset 是否清除旧 relic probe 基线。**pytest 不会执行 Java 文件**；Python 构建/安装测试也不能替代这个探针。

前提：按 [Oracle 构建说明](../../native/oracle/README.md) 从当前源码构建 Oracle；准备 JDK、游戏、ModTheSpire、BaseMod、CommunicationMod。不得上传四个依赖。将 Oracle、四项依赖和探针 class 输出目录加入 classpath，编译时使用 `javac --release 8 -proc:none -cp <classpath> -d <local-output> tests/oracle/OracleModeProbe.java`。

模式缓存是 JVM 级的，每次测试必须启动新 JVM：默认 production、显式 `-Dsls.oracle.mode=production`、显式 `-Dsls.oracle.mode=validation` 分别运行 `OracleModeProbe production` / `OracleModeProbe validation`，应输出 `ORACLE_MODE_OK`。非法 mode 应在初始化时报错，不能误读为模式通过。

2026-10-05 Oracle 源码恢复阶段已用真实依赖执行这些检查，并实际完成两个模式的有限游戏 smoke。记录见 [Oracle qualification](../../docs/results/oracle-source-recovery-20261005/README.md)。本次 tests 整理没有重新启动原版游戏；已有 JVM/游戏证据不能泛化为所有卡牌和完整局都与原版一致。
