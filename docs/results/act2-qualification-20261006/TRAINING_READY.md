# 修复版 Act1+Act2 λ pilot：本地准备完成

2026-10-07。训练目标是正常Neow开局的A20 Ironclad Act1+Act2联合通关。尚未运行新的NUS训练；没有新增胜率或λ有效性结论。

## 保真度结案与边界

当前Native来源`b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f`。24义务/72受控重放通过，5条正常production轨迹完整匹配。另一次独立production捕获实测Discovery检索更新15次；native默认14次。只输入实测15次后187边界直到终止完全一致，见`discovery-r31-clock-conditioned.json`。无条件差异保留于`discovery-r31-strict.json`。

这是有独立证据的时序随机依赖，不修改生成卡牌、不忽略字段、不搜索计数。训练仍使用确定性14次约定。条件匹配不会升级为无条件production认证。更多时序组合、另外两种Boss自然轨迹及更广内容仍未验证；原完整认证器未放宽。

## 唯一训练速度改动

CPU编码的primitive list转换采用NumPy类型化数组再建立Torch视图，保留dtype、ID、mask、顺序与特征数值，不缓存动态状态。model依赖显式增加NumPy，服务器缺失时仅安装该依赖，保留已有CUDA/Torch环境；runtime记录NumPy版本，preflight检查原始转换等价。

96个固定stock公共决策全部编码字段一致；真实90M模型logits/logprob/value/循环状态一致；固定rollout及PPO更新后的参数逐元素一致。ABBA顺序的小型Windows工作负载，collect+optimize中位耗时2.10157→2.03073秒，减少约3.4%。CPU编码+padding原型微基准250.64→172.98毫秒，不能把31%当成训练整体加速。证据：`encoder-optimization-r32.json`。

不改混合精度、worker数量、rollout长度、PPO更新强度、reward、网络或训练分布。NUS在实际64workers/16shards、256rollout下自行benchmark并执行墙钟门禁，本地测量不替代服务器结果。

## 预注册实验

机器设计：`lambda-study-r1.json`。控制λ=.98，实验λ=1，其余实际配置一致。固定90,013,696步父模型全部权重，包括critic；两臂各新增4M，共同首个训练seed130000000；各自重置Adam/RNG/workers/recurrent state。新目录不覆盖旧pilot或champion。

λ=1消除rollout内指数衰减，rollout末端仍用critic bootstrap；不宣称纯整局Monte Carlo。首个训练seed下明确配对验证，有发展收益后再做训练seed复制。

周期诊断：[8000009000000,8000009000256)，每500k一次。独立development确认：[8000010000000,8000010002048)。固定终点为主，周期最优只诊断。原最终保留区间[9000000000000,9000000004096)保持封存。

成功：实验固定终点联合通关率相对同环境control及冻结90M均至少+1pp，两个配对exact McNemar比较经Holm family alpha=.05通过；健康完成、两个互不重叠晚期窗口均有真实联合成功。小或不确定的正收益为INCONCLUSIVE，无联合收益不自动延长；未完成或运行不兼容是执行问题。

## 一次排队、同节点运行

唯一入口：`tools/submit_act12_lambda_study.py --study docs/results/act2-qualification-20261006/lambda-study-r1.json`。提交一个48h、A100-40、16CPU、64G的gpu-long作业，按既有xgpg约束。

作业内：control准备/训练→核对COMPLETE manifest及来源/预算→experimental准备/训练→严格配对分析。两臂共用节点和runtime，避免评估hostname/推理设置不一致。每臂preparation上限24h，第二臂也受分配剩余时间减30分钟分析预留的约束。每臂独立测量吞吐；预算不满足即停，不静默减少训练量。

信号转发到自有训练子进程，保留安全checkpoint；不把安全退出的零返回码当成完成。中断、失败、manifest未完成或预算不足均不开始下一臂。输出已存在或提交receipt已存在则拒绝盲目重复。

登录节点只进行Git、文件SHA与计划解析，实测import路径不加载Torch/CUDA，避免之前登录节点libtorch_cuda映射问题。真实权重深验证、依赖、Native构建、CUDA、短更新和精确恢复检查都在GPU节点进行。

## 本地验收

- 两种λ的实际父权重preflight、短PPO更新及精确checkpoint恢复PASS，仍保留Act1训练来源。
- 最终完整Python测试：1201通过、1跳过、4条受测试控制的runtime/provenance警告。
- 33/33配置、Ruff、词表/内容生成检查、Git diff whitespace检查通过。
- wheel构建通过；Native编译来源与r30受控证据一致。
- 63个受保护文件与preferences/betaPreferences/saves三个目录成员集合恢复一致；AGENTS.md未修改。
- 两臂计划与父权重/配置/实现/依赖/启动工具SHA绑定，deep validation及Slurm dry-run通过。

训练完成后自动分析位于`local/runs/act12-lambda-study-r1/analysis.json`。下载两臂完整目录、该study目录及两个preparation目录，保留原始metrics/checkpoints/manifest/benchmark。若job中断，应先检查status、receipt和日志，不能重跑首次提交命令覆盖旧证据。
