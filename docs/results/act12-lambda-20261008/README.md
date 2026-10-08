# Act1+Act2 匹配 λ 4M：结果、归档与下一步

2026-10-08。正常 Neow 开局、A20 Ironclad、两幕联合通关是唯一主要结果。
**两组训练完成；没有确认 λ=1 的优势，也没有任何一组达到既定的自动续训20M条件。**
这不是“再训练肯定无效”的证明，也不能把小样本的7局对6局解释为λ=.98更好。
本轮λ比较已结束；后续准备[针对迁移稳定性的完整critic预热新配方](../act12-critic20m-20261008/README.md)。这是新阶段方案，不改写本轮标准，也不宣称只是改变一个标量。

## 身份与归档

- job916291，固定干净提交 `6e54adbc0aa6fca81b994490837db382b245e42b`；
  同节点xgpg6、A100 PCIe40GB、Torch2.6.0+cu124、Python3.12.3。
- 每组90,013,696→94,027,776，新增4,014,080 decisions、245 updates；
  比计划目标多14,080，不足一个16,384决策rollout。不是94M的Act2训练。
- 同一90M父checkpoint SHA `274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0`；
  从Act1迁移到Act2目标时，保留全部模型权重，Adam、worker、RNG和循环状态重新初始化。
- 实验native `b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f`；
  training implementation `9dca86829696613903d07b7fcbc6d4928f1c957c98fd6eaad534d6e76519ba94`。
  64 workers/16 shards，rollout256，seq64，epochs2，LR3.125e-5；除λ及输出路径外配置匹配。
- 原包69,127,209 bytes，SHA `cf6e67ee78fac7d0de280dc105efbea9599a5ea9de4efc74592348379d1991ff`；
  完整读取gzip/tar，37个普通文件、无重复/越界/链接，展开140,112,783 bytes。
  两组bundle实际注册的20项文件均匹配原SHA。
- 原包和完整原始载荷保存在 `local/archives/act12-lambda-20261008/`；
  工作副本为 `local/runs/ironclad-a20-act12-lambda098-r1/` 与
  `local/runs/ironclad-a20-act12-lambda100-r1/`。导入拒绝覆盖已有目录。
  preparation、run.lock、服务器status和日志只作归档证据，不安装成当前可用准备记录。
- 原始 `status.json=ANALYSIS_FAILED` 保持不变。未覆盖56M champion、90M父模型或旧pilot。
  目录与逐文件SHA见 [archive-inventory.json](archive-inventory.json)。

这次不反序列化checkpoint，不加载Torch/CUDA，不采样、不更新模型、不测速。
文件SHA、配置、评估指向与manifest身份已核对；tensor有限性、checkpoint内部恢复状态、
下一次rollout/update逐位一致性**本次未验证**，不能据此批准状态保持续训。
中间checkpoint未下载，独立policy导出不存在，不能宣称服务器全部产物完整下载。

## 配对终点结果

确认开发区间 `[8000010000000,8000010002048)`，两组与冻结父模型同runtime、同native、同seed。
全部2048个正常开局参与配对，不按Act2幸存者筛样本。最终保留集没有使用。

| 模型 | 两幕联合成功 | Wilson95%区间 | 到达Act2 | 到达后联合成功 | Act2 Boss入场 |
|---|---:|---:|---:|---:|---:|
| 冻结90M父模型 | 4/2048，0.195% | 0.076–0.501% | 1433，69.97% | 4/1433，0.279% | 151 |
| λ=.98固定94M | 7/2048，0.342% | 0.166–0.704% | 782，38.18% | 7/782，0.895% | 104 |
| λ=1固定94M | 6/2048，0.293% | 0.134–0.638% | 802，39.16% | 6/802，0.748% | 79 |

| 比较 | 左胜右败 / 右胜左败 | 净百分点变化 | 精确双侧McNemar p |
|---|---:|---:|---:|
| λ=.98→λ=1 | 6 / 5 | −0.049pp | 1.000000 |
| 父模型→λ=1 | 4 / 6 | +0.098pp | 0.753906 |
| 父模型→λ=.98（次级比较） | 4 / 7 | +0.146pp | 0.548828 |

原预注册标准要求λ=1比control和父模型均至少+1pp，且通过Holm精确检验。
后续用户允许control与父模型作次级分析，但仍要求+1pp、p≤.05及健康完成。
**两条长训路径都不通过；即使暂时不考虑循环健康门禁，效果标准也不通过。**
不修改阈值给本轮补发资格。联合胜率尚未证明提高，也未证明下降；λ=1无优势证据。

## 曲线、checkpoint与Boss

周期256个固定开发seed，每约0.5M评估。λ=.98从baseline开始的成功数为
`0,2,2,0,1,1,0,2,0`；λ=1全部为0。成功数太少，256个seed对约0.3%真实胜率
仍约有46%的概率观察不到任何成功，不能据此断言模型没有成功能力。

control周期最高为2/256（0.781%），出现在90,505,216、91,013,120、93,503,488。
这些只是在反复查看的小开发集上的峰值，不是独立确认的最好模型，中间权重本包未包含。
λ=1周期所有checkpoint同为0。见 [summary.json](summary.json) 的完整紧凑曲线。

两组实际保存的 `best_progress.pt` 都是**90,013,696的初始化权重**。
当前 `HORIZON_CLEAR_COUNT` 的progress guard要求cycle_limits等健康字段全为0，
而每次周期评估都有6–24次cycle终止。control即使得到2个联合成功也被拒绝promotion；
这里不是Act1 reach被直接用于否决更高联合成功。
因此 `final-evaluation.json` 都评的是初始化所选模型（4/2048），
`endpoint-evaluation.json` 才是训练结束94M模型（7/6局）。
不把文件名“final”当成固定终点，不把自动所选模型称为研究意义上的最佳策略。

| Act2 Boss | 父模型成功/入场 | λ=.98成功/入场 | λ=1成功/入场 |
|---|---:|---:|---:|
| Automaton | 0/36 | 1/30 | 0/16 |
| Champ | 4/66 | 6/41 | 6/35 |
| Collector | 0/49 | 0/33 | 0/28 |

Boss人群和入幕牌组随策略而改变，不能用这些条件比例做严格配对因果比较。
但“几乎所有成功仍集中在Champ、绝大多数Act2入场连Boss都到不了”是明确的覆盖瓶颈。

实际保存的重要模型保持原位：control final SHA `4eab4cb154f17b9f48e81e160b799bd99ed1d58fcae9938f735b29aa26418098`；
experimental final SHA `0aebb484bbb8d75fdf78fdf6b0871f86938887bc00e0281bfbcc67133d56c1e3`。
7局是当前终点的最高观察值，不能认定为可信champion；本轮不导出/晋升演示模型。

## Act1 reach下降是否是灾难性遗忘

下降真实且巨大：control配对lost773/gained122，λ=1 lost734/gained103，
精确p分别约2.12e−116、4.03e−118；三个Act1 Boss方向均退化。
两组都到达Act2的共同seed上，control入幕平均HP比例由89.71%到86.82%，
λ=1由89.65%到84.78%；药水数量也下降。没有“牺牲Act1稳定性换来显著更强Act2资源”
的支持证据；牌组质量本身还不能仅用牌数判定。

但任务终点变了，路线/构筑可合理改变，条件人群和循环记忆也不同。
本轮没有同状态策略能力对照、旧任务重测或actor/critic干预实验，
**只能确认前段生存/入幕退化，不能完成catastrophic forgetting的因果诊断。**
联合通关仍是优化目标，不能恢复一个Act1奖励来“修好”reach报表。

另有具体流程风险：父模型182、control69、λ=1 86次确认集cycle终止全部在floor17。
已有原始失败尾迹见 [cycle-trace-evidence.json](cycle-trace-evidence.json)：
父模型和λ=1的样例在Boss遗物后反复对同一`select-card`执行REMOVE_CARD，未完成多选。
这是已观察的选择循环；不是证明REMOVE_CARD的native实现错误，也不能假定所有cycle
都由同一原因产生。`last_context`是最近敌人/事件上下文，可残留已结束的Boss；
不能将这些循环误报为Boss战斗死亡。
循环数量实际下降，所以循环本身不能解释整个Act2 reach崩降。

## 已确认问题、风险和优先级

1. **结果与门禁工具：已确认。** trainer只有final promotion通过才生成独立policy export，
   bundle只登记存在文件；study analyzer却无条件要求export，造成训练完成后的分析报错。
   本次增加无Torch历史分析入口，报告缺项及失败门禁，不造export、不绕过续训兼容。
   下一发布应将“证据可分析”和“策略可晋升”分开，避免把科学未通过变成执行失败。
   选模循环门禁也需单独设计：普通策略循环必须算失败局且披露；真正backend故障仍硬阻断。
   不更改本轮预注册门禁，未来是否允许非零策略循环须在新实验前明确。
2. **稀疏成功与跨任务critic：机制风险。** 两组训练19003/17765个episode只有51/43成功
   （约0.268%/0.242%），大部分样本几乎同为失败。
   原Act1 critic整体迁移到新目标，本来预测Act1回报；不能重新解释成两幕成功率。
   已有四条独立本地诊断死亡轨迹的实际shaped return约−1.036，λ=.98开局GAE return
   却为+.186到+.533；同一来源λ=1在完整片段可接近实际回报。
   这些旧探针说明风险，不证明它就是训练失败主因；本次λ=1效果不成立进一步限制该解释。
3. **critic学习与共享表示：待干预验证。** control全程value explained variance均值−.019，
   λ=1为−.282；最后10次为.307/.025。λ=1初期value loss均值.171，control为.039。
   不能把小value loss看成高成功预测能力：全失败目标容易拟合。
   共享梯度冲突旧探针cosine−.061也不能直接认定为主因。
4. **normalization/探索：风险。** control combat advantage std前10次.268、后10次.041，
   normalization缩放均值约4→30；其他domain同向。重标度可能放大弱信号，也可能是合法
   尺度适应，暂无因果证据。λ=1训练Neow第2位置约96.8%，control约65.8%；
   编码中确有Neow数值和selected-card字段，位置集中不能直接证明缺失表征。
5. **更新强度：没有单步爆炸证据。** 平均KL约.0044/.0042，clip fraction约3.8%/3.7%，
   无nonfinite、backend truncation或step limit。梯度裁剪约80%/79%仍值得关注；
   小每步KL不排除245次更新的累积漂移。
6. **速度：明确CPU成本，未证明优化收益。** 两组更新吞吐约114.7 decisions/s；
   每组纯collect+optimize约9.72h，manifest总时长12.94/12.96h。
   transition约27.2%、encoding约21.0%、优化约28.2%的更新时间；主要瓶颈不是纯GPU算力。
   已有字段缓存/不可变禁止集合只有逻辑与小测试资格，不能报服务器加速。
7. **模拟器：已有有界Act2校验可结案。** 已知Discovery计时依赖继续披露；
   不因低胜率重启整个原版游戏认证。多选循环仅做定点复核；晚幕校验继续延期。
   当前本地native0fd7…、serialization训练实现d313…不同于本轮来源，不能静默续训。

## 下一步建议：有限工程收尾后，准备一个新的20M配方

不直接从94M原样延长，不继续进行λ扫描，不同时改网络、normalization、课程和reward。
优先收尾两项可解释工程工作：结果/门禁语义修正；对现有Boss后多选循环尾迹核对
action、已选状态、确认/取消及原版规则。纯源码与已有证据可先做；如发现实质差异，
仅补最小受控场景，更新契约。无需重启全游戏审计，也不启动本地GPU。

**下一训练方法的首选研究假设：跨任务时先适应critic，能减少actor被错误初始目标
带偏的代价。该假设尚未证实，方案状态是PROPOSED，不是READY。**

建议重新从冻结90M、正常Neow开局出发，λ保持.98（不宣称更优），新增约20M：
前32个rollout即524,288 decisions只拟合value_head（LayerNorm+Linear），冻结actor、
共享encoder与GRU；使用完整终止轨迹的真实回报，跨rollout未终止的轨迹暂存，
不把旧Act1 bootstrap当成新目标真值。然后剩余预算恢复原PPO。
初期状态、Adam、worker游标、RNG和warmup进度必须可安全恢复，不能意外重跑warmup。
只改变“初始critic适应阶段”；reward、正常开局分布、model结构、λ、LR和worker布局保持。
不是从94M完整学习状态续接，而是显式的新训练配方，需新训练身份。

训练seed建议先沿用130000000做可追溯的阶段干预；本轮control前4M作为历史、单seed
早期参照，不能称为20M预算匹配control。固定20M终点的主要任务是验证联合能力是否
提高；若要证明warmup相对原PPO的因果优势，需要另一个同预算control，本次不默认追加。
稀疏成功仍可能是主导瓶颈：如果warmup后联合成功持续匮乏，它不会凭空生成有效探索。
这时再选困难状态/成功经验训练方向，不能本轮一起加入。

周期每2M/512、每1M存盘；新开发空间沿用之前预留的
`[8000012000000,+512)` / `[8000013000000,+4096)`，发布前再查实际记录冲突。
固定终点与冻结90M同runtime评估全4096开局，主要成功判据仍为联合胜率至少+1pp、
配对精确p≤.05、无未解释backend/证据故障；周期峰值和Act1 reach不作为科研提前停止。
未满足即“新配方无确认收益”；不修改阈值，不直接再加20M。
9e12最终保留集继续封存，候选配方确定后才安排一次正式终评。

本轮实测20M仅更新约48.4h；加评估、准备及中断余量，规划约55–65h是粗估，
并非完成时间保证。用多个48h allocation续接同一逻辑训练，现有保守调度可预留3段，
预算完成后后续段退出。warmup支持、来源绑定、轻量回归和服务器准备门禁完成并发布
以后才能给启动命令。本次未生成可启动配置、未commit/push、未提交作业。

## 本次复算入口与验证

```powershell
$env:PYTHONDONTWRITEBYTECODE='1'
$env:PYTHONPATH='D:/SLS/src;D:/SLS'
& D:/Anaconda/envs/DL/python.exe tools/analyze_downloaded_act12_study.py `
  --study docs/results/act2-qualification-20261006/lambda-study-r1.json `
  --output local/reports/act12-lambda-new-analysis.json
```

工具拒绝已有输出；只读原结果，不要求当前native等于历史native，也不授权恢复。
核对历史来源与预注册SHA，复算完整seed、分Boss、配对、资源、曲线、健康和时序。
8项纯Python证据校验测试通过（0.17s），包括隔离进程确认工具不导入Torch。
Ruff与CRLF-aware diff检查通过；报告再次复算后JSON完全一致，37项原始载荷SHA再次匹配。
没有运行完整训练相关测试或GPU检查，AGENTS.md未修改。
完整原始报告为 `local/archives/act12-lambda-20261008/analysis-v3.json`。
