# 三钥匙校验：红、蓝钥匙原版证据已取得

2026-10-10，研究代码 `c0b05856ee52d318008ccddac4329e171d38d001`。本阶段只增加验证入口、原版采集/审计工具和工具修复，没有修改 native、网络、奖励、PPO、生产训练配置或恢复契约。下一步仍是 native 对照、绿钥匙及 Act4 连续流程，完整 A20H 资格尚未完成。

## 已执行的原版场景

源码构建 Oracle 1.3.42，SHA256 `390de14fe255602004a1ca281170d36d3201d7c79f71e52e0bafb8f6e8a4e1ae`，41 个 Java8 class；stock JAR 为已绑定 `cfad868a…`。新资源包含 8 场景 × 3 seed，区间 `[131200300,131200324)`；已有 Oracle 场景及指定配置/原版归档命名空间检查无冲突。

正常 A20 开局取得第一个战斗边界后，仅验证模式的 `parity_key_room` 设置受控初态，实例化实际 TheCity 并选择实际地图中的休息/宝箱房。之后只执行原版可用命令及回调。所有场景 HP40/maxHP80、gold99、战士起始牌组；不是自然到达该房间的轨迹，不是胜率或训练课程。production 原版 smoke 已通过：无任何 `parity_` 命令，无隐藏审计字段。

| 场景 | 独立声明 seed 数 | 原版实际观察 |
|---|---:|---|
| 回忆 | 3 | 红钥匙 false→true；HP 保持40，牌组/遗物/RNG不变 |
| 选择休息 | 3 | HP40→64；未获得红钥匙 |
| Regal Pillow 休息 | 3 | HP40→79；未获得红钥匙 |
| 已持红钥匙 | 3 | 红钥匙保持；HP40→64；选择页无再次回忆 |
| 未开启终幕 | 3 | 选择页无回忆；HP40→64；无红钥匙 |
| 选择蓝钥匙 | 3 | 实际效果后蓝钥匙 false→true；未取得关联遗物 |
| 选择关联遗物 | 3 | 获得 MawBank / Smiling Mask / Gremlin Horn；蓝钥匙保持false |
| 已持蓝钥匙 | 3 | 获得 Mummified Hand / Potion Belt / Vajra；蓝钥匙保持true |

原始奖励和选择边界保留于 capture，包括房间及屏幕奖励的 done/ignored 状态。后续 native 对照须检查链接取消、合法动作、资源、RNG、药水容量及恢复，不能只比较钥匙布尔值。

## 查明并修复的工具问题

1. `Properties.load(InputStream)` 按 Java properties 编码读取，而旧工具直接写 UTF-8 中文路径，子进程路径被误读。改为 ASCII Unicode 转义，保留已有冒号转义及引用；涵盖中文、空格、非 BMP 字符检查。
2. 自定义采集命令需使用正斜杠路径，否则 properties 会吞掉 Windows 反斜杠。新入口统一使用 `as_posix()`。
3. 旧 runtime 在创建结果目录前发生错误，写 launch 报告又失败，遮蔽原始错误；现先创建目录，且检测子进程启动失败。
4. 初次蓝钥匙采集在 `proceed` 已可用时就停止，此时实际获得钥匙动画尚未完成，3 个 false 标志是提前边界。改成等待实际效果并加入回归检查；9 个蓝钥匙场景使用相同声明 seed 重采，计作替代证据，不增加独立样本数。

首次失败日志、提前边界及之后成功结果均保留。初次 `rest-blue-r1` 中的 12 个休息分支可独立审计；其旧蓝钥匙部分及旧 aggregate summary 不作为稳定终点证据。当前有效汇总是 `ruby-recall-summary-r2`、`rest-qualified-summary-r1`、`blue-settled-summary-r1`，分别3/12/9条。严格审计绑定 build/manifest/capture/launch、声明 seed 和初态、合法动作、完成与恢复；报告显式 `simulator_comparison=NOT_YET_PERFORMED`。

6 次原版启动的 journal 全部 RECOVERED，逐项重新校验每次63个受保护文件与原 hash 相同，无原版进程遗留。原 D:/SLS 的197个待提交文件及完整 status 字节 hash 未变。用户存档及备份不上传 GitHub。

提交后的最终 CPU 全量 **1609 passed、2 skipped、4 warnings**，全仓 Ruff 通过；跳过仍为无本地 exported Act1 policy 和禁用 CUDA。Oracle 完整源码/资源/产物身份再次通过检查。原始 capture、日志、build metadata、恢复 journal 和汇总封存在本地交付 ZIP；输入和包 hash 见 [key-capture-evidence.json](key-capture-evidence.json)，不包含 stock JAR、模型或用户存档。

## 重现与下一步

在该研究工作树使用 DL、CPU、CUDA禁用、`PYTHONPATH="$PWD/src;$PWD"`。先按 `tools/build_oracle.py` 构建新独占 Oracle 路径，并用 `tools/verify_oracle.py` 验证；通过 production runtime smoke 后，执行：

```powershell
python tools/run_key_room_batch.py `
  --oracle local/build/oracle/key-acquisition-r1.jar `
  --manifest native/oracle/resources/spirecomm/parity/fullrun-key-acquisition-r1.json `
  --scenes ruby-recall `
  --output local/reports/key-acquisition-new-id/recall.json

python tools/audit_stock_key_room_capture.py `
  --capture local/reports/key-acquisition-new-id/recall.json `
  --oracle-build local/build/oracle/key-acquisition-r1.build.json `
  --manifest native/oracle/resources/spirecomm/parity/fullrun-key-acquisition-r1.json `
  --output local/reports/key-acquisition-new-id/summary.json
```

所有输出独占创建；不能覆盖本轮归档。此命令会短暂启动本地原版并自动备份/恢复，运行前必须无原版游戏进程。

下一步按原版公开初态、真实奖励及 RNG 设置一致的 native 初始房间，执行相同合法选择并逐边界差分，包括生产 FullRun restore。随后补绿色燃烧精英分支，再连接双 Boss 与 Act4；不得从这24个原版场景推断模拟器已经完全匹配。
