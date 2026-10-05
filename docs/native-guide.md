# Native 目录说明与整理记录

日期：2026-10-05。此次只整理 `native/` 的文件用途和发布边界，不修改模拟器规则，不代表重新完成原版保真审计。

后续状态：同日已从 Git 历史恢复完整基础 Oracle，完成从零构建和 v11 模式隔离。本文的 87 文件清单及“基础来源缺失”描述属于首次整理快照；当前 Oracle 以 [正式说明](../native/oracle/README.md) 和 [恢复验证](results/oracle-source-recovery-20261005/README.md) 为准。

## 总体结论

目录包含 87 个正式文件，总计 2,505,627 字节（约 2.39 MiB）：50 个 `.h`、30 个 `.cpp`、1 个 `.hpp`、2 个 Java 文件，以及 CMake 配置、工具链、来源记录、许可证各 1 个。实际文件与 Git 跟踪清单完全一致，没有未跟踪文件或编译产物。

全部保留上传。本轮没有为了清理而删除文件，也没有更改任何 native 源码。正式项目需要同时包含 `src/` 的 Python 接口和此处的 C++ 引擎。

## 各层目录

| 路径 | 数量 | 内容与用途 | 处理 |
| --- | ---: | --- | --- |
| `simulator/include/combat/` | 11 | 战斗状态、玩家、怪物、卡牌实例与管理、动作/卡牌队列、选牌与输入状态声明 | 核心源码，上传 |
| `simulator/src/combat/` | 10 | 战斗推进、行动执行、玩家与怪物回调、招式伤害、卡牌生命周期实现 | 核心源码，上传 |
| `simulator/include/game/` | 11 | 完整局、地图、牌组、商店、奖励、Neow、随机数、遗物及存档声明 | 核心源码，上传 |
| `simulator/src/game/` | 9 | 完整局推进、地图/Neow/商店/奖励生成、牌组和存档实现 | 核心源码，上传 |
| `simulator/include/constants/` | 15 | 卡牌、怪物、招式、状态、事件、房间、药水、遗物与内容池等枚举和表 | 必需数据源码，上传 |
| `simulator/include/data_structure/` | 1 | `fixed_list.h` 固定容量容器 | 依赖源码，上传 |
| `simulator/include/sim/` | 11 | 模拟运行、打印、辅助函数、基线策略与搜索器声明，包含 `search/` | 保留上传 |
| `simulator/src/sim/` | 10 | 上述模拟运行与搜索实现，包含 `search/` | 保留上传 |
| `simulator/include/sts_common.h` | 1 | 公共编译开关与基础声明 | 保留上传 |
| `simulator/python/module.cpp` | 1 | pybind11 桥接、状态投影、动作接口、存取状态和大量受控验证 probe | 核心源码，上传 |
| `simulator/third_party/nlohmann/nlohmann/json.hpp` | 1 | 固定版本 JSON for Modern C++ 3.10.2 单头库 | 第三方源码，保留上传和文件内许可证 |
| `simulator/cmake/zig-windows-toolchain.cmake` | 1 | Windows Zig 编译工具链 | 构建配置，上传 |
| `simulator/CMakeLists.txt` | 1 | C++17、Python/pybind11、编译选项、输出位置、身份与 sanitizer 选项 | 构建配置，上传 |
| `simulator/SLS_VENDOR.json` | 1 | 上游仓库、固定 commit 和源码维护策略 | 来源证据，上传 |
| `simulator/LICENSE.lightspeed.md` | 1 | gamerpuppy 的 MIT 授权与版权声明 | 必须保留上传 |
| `oracle/src/spirecomm/parity/` | 2 | 原版游戏的公开观测 Java 补丁 | 正式可选功能，保留上传 |

## 阅读核心文件的顺序

1. `python/module.cpp`：Python 训练环境如何调用游戏引擎，以及如何获取合法动作和状态。
2. `include/game/GameContext.h`、`src/game/GameContext.cpp`：完整局状态与跨房间流程。
3. `include/combat/BattleContext.h`、`src/combat/BattleContext.cpp`：战斗状态与回合流程。
4. `src/combat/Actions.cpp`、`Player.cpp`、`MonsterSpecific.cpp`：具体行动与效果。
5. `src/game/Map.cpp`、`Neow.cpp`、`Shop.cpp`、`CombatReward.cpp`：开局、路线与资源分布。

`module.cpp` 大约 290 KiB，除了正式运行接口，也集中包含校验用 probe。文件较大，是以后可考虑拆分的维护问题；本轮不拆分，因为构建和 native 身份绑定整个源码树。拆分应作为单独的代码变更验证与记录。

## 上游源码不等于可删历史

`SLS_VENDOR.json` 将模拟器来源固定为 `gamerpuppy/sts_lightspeed` 的 commit `7476a81954020087da31d41d16fddf475746ec2d`。这是项目内维护的 fork；构建脚本使用本地版本化源码，不在构建时下载上游或临时打补丁。

`include/sim/search/` 与 `src/sim/search/` 中的 `SimpleAgent`、`BattleScumSearcher2` 等属于上游的策略/搜索设施，不是 PPO 网络。但当前 `module.cpp` 直接引用它们，暴露 scripted playout 和 search 接口；测试也调用 `scripted_playout_act1()`。`CardManager.cpp` 另有搜索器头文件依赖。CMake 当前递归收集 `src/*.cpp` 编译，不能按文件名猜测无用后直接移除。

对 `src/`、`tools/`、`tests/` 的定向引用搜索发现 scripted playout 的一个 native 回归测试调用，未发现这些具名 scripted/search 接口被 Python PPO 路径调用。搜索器代码存在并不表示 PPO 自动进行搜索。本轮没有做完整的静态依赖裁剪证明；如以后要减小构建范围，应独立修改依赖、构建配置并验证行为。

`SaveFile`、其他角色/后续 Act 的枚举和实现也不能因为目前主要训练 Ironclad Act 1 而删除。它们参与公共完整局引擎，后续 Act 1–2 也需要延续同一引擎。

## Oracle 两个 Java 文件

- `CardStatePatch.java`：向 CommunicationMod 补充公开的动态卡牌字段与奖励卡预览。
- `EventStatePatch.java`：补充事件已展示的对象、数值、Neow 选项等信息，并按事件阶段限制输出。

它们用于原版游戏接入与观测对齐。文件是补丁源码，不是原版游戏 JAR；保留它们也不等于已经证明每个字段和所有事件都符合预期，保真验证仍需对应证据。

当前 `tools/build_observation_oracle.py` 编译这两个文件，再将生成的 class 写入已有的基础 Oracle JAR。它要求 Java 编译器、原版游戏、CommunicationMod、ModTheSpire，以及本地基础 `SpirecommParity.jar`。因此：

- 新 clone 可以从现有源码构建 C++ 训练模拟器（先安装相应构建依赖）。
- 新 clone 不能仅凭这两个 Java 文件从零构建完整的 Oracle JAR。原版校验路线仍有本地基础产物依赖。

这是可复现性限制，不是删除 `oracle/` 的理由。后续整理 `tools/` 和外部依赖时，应明确基础 Oracle 的取得方式或补齐它的来源与构建方法。

## 本机文件与发布边界

| 文件类型 | 存放位置/性质 | 是否进入源码仓库 |
| --- | --- | --- |
| C++/Java 源码、枚举表、固定 JSON 库、CMake、来源与许可证 | 当前 `native/` | 是 |
| `_lightspeed*.pyd`、`.so`、目标文件与 CMake/Ninja 缓存 | `local/build/` | 否，各机器自行构建 |
| 下载的 CMake、Ninja、Zig、pybind11 工具 | `local/build/tools/` | 否，由构建工具准备 |
| Oracle JAR、class 文件 | `local/build/oracle/` 等本地产物位置 | 不放源码仓库；来源与共享策略另行整理 |
| 原版游戏 JAR、安装文件及本机 Mod | 本地游戏安装或 `local/external/` | 不作为本项目源码上传 |
| 训练 checkpoint、模型权重、原始采集日志 | 模型与实验记录目录 | 不应塞入 `native/`；后续逐目录审查 |

两份第三方版权记录必须保留：`LICENSE.lightspeed.md` 中的 gamerpuppy，以及 `json.hpp` 开头的 Niels Lohmann 声明。重建 Git 历史不会使上游代码变成本项目原创；这些名字是源码来源，并非 GitHub 仓库权限的证据。

## 整理证据

全部 87 个文件的大小与 SHA-256、文件扩展名统计和未跟踪文件检查保存于本地 `local/reports/project-reset-20261005/native-review.json`。原始 native 身份保持为：

```text
fe354a23c7584d68d0a2b6681b8dfd4e97d98e107b7ddf57c479060d91468f39
```

本轮只新增目录说明，没有修改模拟器源码、版本契约或运行产物。

本地 Conda `DL` 验证：`python -m pytest tests/test_build_native.py tests/simulator -q`，167 项通过，输出保存在 `local/reports/project-reset-20261005/native-tests.txt`。检查覆盖构建命令以及现有 native 模拟器回归场景；本轮未重新编译 C++，未编译 Oracle，也未运行原版游戏。
