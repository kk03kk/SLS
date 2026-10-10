# 原版持绿钥匙地图与 RNG：实际构造器对照

日期：2026-10-10。承接 [HELD-GREEN-MAP.md](HELD-GREEN-MAP.md) 的静态规则及条件化 native 换幕，补充真实原版地图构造器运行。生产 native 源、恢复契约、训练参数和服务器发布均未修改。

## 实际结果与边界

Oracle 1.3.48 从源构建（44 个 class），JAR SHA256 `c1a50739d847afe65685b0a12f7d996c67f2f3e2bdbf2c5bb8fabcead280bc57`。固定原版游戏 JAR SHA256 `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`。新 namespace `[131200410,131200411)` 在创建前搜索资源、测试、工具与配置，未发现占用。

使用同一个 seed **131200410**，在 Act2、Act3 各运行三种显式初始旗标。重复 seed 为预先声明的配对设计，不是六个独立随机样本。正常开局公开前缀保留，随后由验证命令设置初始旗标并调用实际 TheCity / TheBeyond 构造器，再进入实际根可达休息节点暴露稳定边界；没有设置地图节点、房间或燃烧坐标，也没有取得钥匙或完成 Boss。

| 幕 | 初始条件 | 原版节点 | 原版燃烧坐标 | 原版 map RNG counter |
|---|---|---:|---|---:|
| Act2 | 未持绿钥匙、已解锁 | 58 | (3,11) | 96 |
| Act2 | 已持绿钥匙、已解锁 | 58 | 无 | 95 |
| Act2 | 未持绿钥匙、未解锁 | 58 | 无 | 95 |
| Act3 | 未持绿钥匙、已解锁 | 62 | (4,5) | 92 |
| Act3 | 已持绿钥匙、已解锁 | 62 | 无 | 91 |
| Act3 | 未持绿钥匙、未解锁 | 62 | 无 | 91 |

六个案例的公开节点坐标、房间 symbol、有序子边及燃烧坐标都与独立生产 Map.cpp 阶段探针一致。map RNG 的 counter、seed0、seed1 完全一致；进入休息室未改变 map RNG。同幕三种条件的地图拓扑相同；持钥匙与未解锁分支的完整 RNG 状态相同，选择燃烧节点恰好多消耗一次随机抽样。

独立 C++ 探针调用生产 `initNodes/createPaths/filterRedundantEdgesFromFirstRow/assignRooms/assignBurningElite`，在节点选择后记录 RNG，再按 native 实现抽样 buff，逐项核对与真实 `Map::fromSeed` 的全地图文本、坐标和 buff 一致。没有修改 Map.cpp 或缓存动态地图。必须区分阶段：原版 buff 在进入燃烧战斗时抽样，native 在 Map 构造内提前抽样；本报告地图阶段比较使用抽样 buff **之前**的 native RNG，另保留抽样后的完整值，未把两者混为同一边界。燃烧战斗本身的对照见 [GREEN-EXPANSION.md](GREEN-EXPANSION.md)。

## 生产路径与回归

新增冻结回归 `tests/fixtures/regressions/held-key-stock-map-r1.json`，期待值直接来自原版公开 Observation，而非 native 输出。四个**已解锁**案例用显式干预的 boss reward checkpoint 测试真实 native 换幕，比较 node_id、x/y、visible_room_type、有序 outgoing_node_ids；恢复 checkpoint 后继续三个合法动作，逐步完整状态一致。

原版在休息室、native 在新幕地图，二者的当前位置与可达标记不同，因此不声称完整 Observation 对齐，回归明确排除 `reachable`。初态幕号、seed、钥匙及持久 RNG/库存/奖励均为条件化输入，不是正常开局实际通关轨迹。

**未解锁的两个案例只验证独立 Map 构造器指定 `assignBurning=false` 时的行为。生产 native 换幕仍没有终幕解锁旗标，不能按此条件自动抑制燃烧精英。** 对照 MATCH 不代表这两个生产路径已经支持；冻结回归保留缺口，未添加新恢复字段、兼容白名单或修改历史模型。

## 安全、封存与下一步

生产模式运行检查通过：验证命令不被广告、验证私有状态不进入生产输出。验证模式六例完整结束，两个新增 runtime journal 均 RECOVERED；游戏进程退出，保存、配置与模组恢复逐项核验。原目录 197 个待提交文件及 status 与快照一致。

工具、固定场景、原始 capture/build/launch、原版 class SHA、阶段探针、回归期待值和验证结果封存在 [stock-held-key-map-evidence.json](stock-held-key-map-evidence.json)。样本 `natural_trajectory=false`、`training_eligible=false`，完整环境资格仍为 `NOT_QUALIFIED`。单 seed 的地图一致不能推广为全 seed / 全分布一致，也不证明新幕遭遇列表、奖励池或 Boss 分布。

代码提交 `7ac865db7a0b74931827b901658568b2b8bbc51a`。CPU 全量 **1647 passed / 2 skipped / 4 warnings**，87.96 秒；跳过为本地导出策略缺失和 CUDA 不可用，警告为现有恢复来源重绑定测试。Ruff 通过。封存包 `local/reports/stock-held-key-map-20261010-delivery-r1.zip` SHA256 `1a089014760cffd5134e99b4108b3a95a2e89e6e4f2e833679bc40a17bb3859c`。

下一优先项是原版 A20 双 Boss 到 Act4 的连续流程与 FullRun 恢复。先复核已有归档是否足够，再为真实缺口构造最小场景；随后连接实际钥匙取得与自然后段轨迹。本轮不需要服务器操作，不将诊断样本接入生产 PPO。
