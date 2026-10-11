# 新训练前独立复核与有限门禁

2026-10-07。本次只复核当前源码、证据和启动契约，未运行新stock局、优化训练或提交服务器任务；优先级2继续暂停。

## 决定

开始准备新的修复版Act1+Act2有限pilot，暂不提交长训练。不继续扩展全部Act2内容审核。优先级1的有限收尾成立，但不自动满足训练发布验收；bounded-closeout-r30.json也明确ready_for_new_nus_submission=false。

复核Native源码摘要、已编译摘要与r30收尾身份一致，12份受控/production报告文件哈希与收尾绑定一致。已有72次受控通过、5条完整production匹配，不证明全部随机分支正确，也不估计胜率。1183项完整测试对应优先级1收尾；后来NumPy编码改动没有完整验收，因此不能把旧完整测试当成当前整个工作树都通过。

## 必须完成：有限任务及结束条件

### 1. 解释已知Discovery分歧

旧production中Power Potion的Discovery之后，Infernal Blade生成卡牌不同。源码确认它们共享cardRandomRng，DiscoveryAction.update按动画更新消耗该流，但实际旧运行更新次数没有证据。

只对该代表seed进行带独立被动时钟见证的新production采集，同时做无输入严格重放与实测计时条件重放。不搜索让结果匹配的次数、不替换卡牌、不忽略RNG后果。条件重放若从首次分歧直到终止全部一致，可将该次差异归为计时依赖；无条件严格结果仍保留，明确native采用确定性时序约定及未验证范围。

若实测时钟不能解释差异，缩成最小根因修复；仅重跑受影响与相邻场景。不重新扩大8自然轨迹或全敌人认证。原版时序随机性可以作为有证据的pilot限制；未解释的伤害、费用、动作或固定随机流错误阻断提交。

本次源码额外发现诊断接口范围不一致：clock解析/production重放接受1..180，native验证setter和checkpoint稀疏输入只接受1..120。接下来须统一支持范围与测试，或在诊断入口明确拒绝不支持的计数，不能截断实测数值。它是核验工具契约问题，不是已证实的新训练规则错误。

### 2. 定案编码优化，冻结最终工作树

当前NumPy转换仅有CPU微基准和初步等价测试。发布前只有两种合格状态：完成所有输入dtype/ID/mask、模型logprob/value/recurrent memory、固定rollout及PPO更新等价和适当完整测试；或者明确撤回该未验收优化，采用已验证编码路径。速度优化不作为环境正确的必要条件。

推荐恢复优先级2后有界验证这一项，再做实际父模型、固定layout/rollout的端到端墙钟。若没有稳定收益或验证成本失控，撤回，不为速度无限延期。Windows微基准不能替代NUS，服务器在同一次提交里先做短preflight/benchmark，失败即停。

### 3. 新环境训练设计与迁移

现有act12-win-pilot.json是COMPLETED_HISTORICAL；旧λ设计是SUPERSEDED_BY_ACT2_RULES_AUDIT_NOT_SUBMITTABLE，旧transition指向旧Native。不能改历史配置后当成新训练，也不能通过关闭身份检查复用。

新配置/输出目录/运行身份应固定90,013,696步父checkpoint，只迁移模型全部权重（包括critic），重新初始化Adam、RNG、workers、循环状态与checkpoint selection。绑定当前Native规则修复证据、训练实现、encoding、配置和依赖。旧94M结果保留历史身份。

首选研究变量仍为λ，但不是已证实瓶颈。rollout.py末端仍使用bootstrap_values，ppo.py收集后仍运行critic；λ=1只消除rollout内部的λ指数衰减，在非终止边界仍依赖critic。因此不能描述为完整局纯Monte Carlo回报或声称已经解决长程信用问题。保留rollout256、sequence64和其他算法变量；用实际父模型检查两种λ的优势/回报尺度和有限更新健康度。

推荐修复版λ=.98 control与λ=1实验，共同父模型、共同规则、相同第一训练seed和各4M预算。它提供一个训练seed下的配方对照，不能证明跨训练seed普适性；有联合收益再复现。最终预算/墙钟要用端到端测量定案；两臂不会因一次提交就变成一臂的计算成本。

若只运行一个新配方，必须预注册为新配方验证，相对同环境冻结90M比较；不能将收益归因于λ。旧环境λ=.98训练永远不能充当本次严格control。

### 4. 同环境开发评估与发布验收

冻结90M与新候选在同一修复版runtime、同一正常Neow开局、同一显式Act2评估契约上比较。主指标joint clear，Act1 reach/伤害/Boss/训练reward仅诊断。current best_checkpoint.py的HORIZON_CLEAR_COUNT已有按成功数及运行健康度排序逻辑，不需要为本轮新造Act1选择指标。

周期开发块用于诊断，独立开发确认块用于固定终点主比较；预先锁定checkpoint规则、effect threshold和配对统计。低胜率下几局差异需报告成功计数、discordant pairs和区间，256局不足以据此宣称小幅提高。最终保留区间[9000000000000,9000000004096)继续封存。

实际父权重本地短collect/optimize、真实joint成功/死亡/跨幕终止、checkpoint恢复、合法动作和数值健康度通过。长训练前运行适当完整Python测试、native/config/static检查；冻结source身份后重新生成plan，审查Git上传只含项目自有代码及紧凑证据，再commit/push main并提供固定SHA服务器命令。服务器不执行旧已完成plan。

## 可延期内容

Automaton/Collector的额外自然轨迹、更广泛敌人/遗物组合、Act3/Heart、扩大网络、critic重构、优势归一化改变、困难状态训练和多训练seed大规模复制，均不成为本次训练前置任务。新pilot如果出现相关首次实质分歧，再定点处理。

## 重新打开环境修复的触发条件

发现未解释且可复现的伤害/费用/合法动作/奖励/终止或固定随机流差异；修复后回归失败；观察/action/规则来源改变却没有迁移；实际父模型短训练出现非法动作、NaN、错误跨幕终止或不可靠checkpoint恢复。仅因尚未测试某组合，不自动无限增加认证任务。
