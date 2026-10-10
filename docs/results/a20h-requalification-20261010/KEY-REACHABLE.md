# 可达红蓝钥匙场景 R2

本轮使用独立 seed `[131200330,131200354)`、不可变场景 `fullrun-key-acquisition-r2.json` 与 Oracle 1.3.44。R1 场景及其结果保持原身份。R2 是受控初态的规则探针，不是从 Neow 开始的自然轨迹，不用于训练或胜率统计。

## 场景与来源

所选休息/宝箱节点必须从 Act2 地图第零行沿真实边可达。Java 用真实 parent 链筛选；Python 独立从原版公开 map 的 children 边重建完整路径，并将路径写入每个采集案例。原版 row14 到虚拟 Boss `(3,16)` 的边不属于普通房间图；仍拒绝内部跨行边。不会为不可达节点添加边。

楼层按 Act2 行号 `y + 18` 推导。五条房间流 ai、shuffle、cardRandom、misc、monsterHp 按原版 `seed + floor` 初始化；其他持久 RNG 不重置。来源绑定实际 stock JAR 中的 AbstractDungeon、MapRoomNode 与原有十个钥匙/房间类字节码 SHA256。

Oracle JAR SHA256：`041ed8752842ec168dc1cfffcb11ccce076648a0d4e747b76519a97b3d101e7d`。Native 源摘要仍为 `6805e8c991ff6e3ca1b57248678babbbc1ea904b6a1a35e17d631c426765dcc0`，二进制摘要 `ce8f500b5116980f2bca854488da334cac58a679a1c1c59e3a8f70410890f3e8`；本轮未修改模拟器生产实现。

## 结果

- 原版 24/24 案例采集完成，独立完整性审计通过。真实选择及钥匙动画完成后才记录最终边界；REST 继续到实际 MAP。
- 红钥匙/休息 15/15 资源、14 条 RNG、克隆执行及恢复一致；15/15 后续 MAP 观测、动作和 RNG 一致。
- 初始休息观测与动作 12/15 一致。3 个“原版终幕未解锁”场景仍存在缺口：native HEART 配方无法表达 finalActAvailable=false，因而错误提供 RECALL。它不在目标“已解锁完整 A20H”域内，但不是全规则通过；未通过伪造持钥匙状态消除差异。
- 蓝钥匙 9 个案例、18 个动作边界的观测、合法动作、资源、关联奖励、14 条 RNG、5 个有序遗物池、克隆及恢复全部一致。9 个初始观测一致。
- 9/9 宝箱均可从实际父节点执行 native 合法 MAP 动作进入；宝箱元数据、treasure 流及完整 14 条 RNG 一致。报告中的入口范围仍标为 `CHECKED_CHEST_METADATA_AND_TREASURE_STREAM_ONLY`，不将这个局部入口检查称为全场景或自然轨迹等价。
- Burning Blood、Regal Pillow 原始中性 counter 的 stock=-1/native=0 差异保留；资源比较使用既有 ABI 规范化，不新增豁免。

生产模式独立烟测通过，校验命令和私有字段不暴露。原工作区 197 个待提交文件及 git status 均与保护快照相同；所有运行恢复 journal 完成，每份 63 个受保护目标逐项 hash/不存在性一致。

DL 环境、CUDA_VISIBLE_DEVICES=-1 下全量测试 1618 passed、2 skipped、4 warnings（163.67s），Ruff 全库通过。两个跳过项分别是本地未提供的导出 Act1 policy 与 CUDA 测试；警告来自既有 checkpoint runtime/provenance rebind 测试。Oracle 原生 Java 构建通过；当前工作树 native 构建产物保持上述摘要。

第一次 R2 采集因 Python 将上述虚拟 Boss 边误判为普通跨行边而失败，属于采集工具错误。原始失败记录 `key-capture-r1.json` 保留，修正后另用 `key-capture-r2.json` 完整重采，不将失败记录覆盖或计为游戏失败。

## 下一步

优先绿钥匙真实燃烧精英战斗、胜负和奖励回调，再做 Act3 有序双 Boss、三钥匙入口、Act4 连续边界和 FullRun 恢复。已有 Heart 单战资源匹配不能证明整个取得和路由流程。环境资格证据补齐前，不据这些受控样本宣布完整 A20H 可训练或已认证。

并行准备自然可达后段课程的研究协议：公开前缀按当前模型重建记忆，训练/验证 seed 隔离，明确 suffix return/GAE 与截断语义，保留正常开局指标及前缀计算成本。独立诊断样本保持 training_eligible=false，暂不接入生产训练。

原始报告位于隔离工作树 `local/reports/key-reachable-20261010/`，摘要、SHA256 与交付包见 `key-reachable-evidence.json`。
