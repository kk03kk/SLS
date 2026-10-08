# 多选循环复核、来源与发布边界

本次从原main建立独立detached发布worktree，原`D:\SLS`工作区未清理、reset或覆盖。只纳入本次训练工程与必要结果归档，未纳入原工作区晚幕规则修复、公共序列化缓存或新吞吐优化。AGENTS.md不变，不创建远端分支。

## Boss后GRID审核

已有失败尾迹显示Boss奖励后反复REMOVE_CARD同一`select-card`；这是可逆选择动作，不能仅凭最近Boss信息将其算作战斗死亡。并非所有循环都在第17层，现有floor分布和原始尾迹保留在`../act12-lambda-20261008/cycle-trace-evidence.json`。

独立依据为本地合法JAR SHA `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`的`GridCardSelectScreen.update`及`EmptyCage.update`字节码和旧stock受控原始载荷。原版以对象实例维护selectedCards，达到数量前可以移除同一已选对象，最后一张达到numCards时提交。仅保存来源摘要和项目解释，未上传游戏字节码或反编译内容。

当前native `GameContext::chooseSelectCardScreenOption`按deckIdx取消已选项，多选时保留候选；最后一张提交deck mutation并regainControl。adapter映射候选`select-card:<ordinal>`与已选`SELECTED:<order>`，保留deck_index、selected和selected_order。模型batching将selected_cards作为CHOICE实体，编码字段已有selected/selected_order，未发现这些信息被静态删掉。checkpoint既有`sls-stock-grid-toggle-v2`和旧部分状态拒绝逻辑保持。

| 义务 | 当前证据和此次验收 |
|---|---|
| 首次选择及候选保留 | 原stock fixture及r12差分报告；NUS门禁再次检查投影 |
| 取消同一张至零 | 原stock r11/r12、字节码、既有native回归；NUS门禁再次检查 |
| 同名不同实例 | stock对象实例语义＋native deckIdx调用链；NUS门禁选择两个不同instance_id并确认移除数量 |
| 达到需求与完成 | stock numCards/EmptyCage消费逻辑＋native最后pick提交；NUS门禁确认退出该选择 |
| 恢复部分选择 | 既有回归fixture及grid contract；NUS门禁比较两条取消后checkpoint |
| 编码和合法取消 | 源码字段/candidate引用核对；NUS门禁检查实际selected数值列与取消candidate |

结论限于已覆盖机制：**现有证据支持合法取消造成的策略选择循环，未确认必须修改生产规则的新错误。** 不将静态审核升级成全范围运行认证。若NUS任一核验失败，停止20M，保存最小分歧再修复；不能屏蔽取消动作绕过失败。

## 身份和迁移

- Native来源仍为`b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f`，与完成的λ实验相同。
- observation v2、policy input v5、action候选协议及Win reward/potential/episode limit不变。
- 训练实现来源重新计算并绑定于新计划，含可选预热、checkpoint v6、评估上下文和新实验execution-only门禁。默认旧入口保留v5，不用新来源对旧运行做自动exact resume。
- 冻结90M来自旧Act1环境；继续使用原r33显式weights-only simulator transition证据，原始环境来源保持，当前环境重评在NUS终点确认。
- 两个旧λ计划仅生命周期改为`COMPLETED_HISTORICAL`；原计划和完整完成manifest按原内容保存在`../act12-lambda-20261008/historical-bindings/`，标准与原source绑定未修改。
- 本地来源、stock摘要、延期验证和seed扫描记录见`local-validation.json`。无新环境迁移白名单。
