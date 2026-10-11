# Training contracts：job 821775 恢复审查

## job 821880：真实服务器验证摘要修正

821880 被源码验证复用门槛拒绝，尚未进入checkpoint恢复、baseline或update。
已通过用户在xlogin读取的实际manifest确认：

- 服务器旧 `source_tree_sha256` 为 `8be318932c332ad8a7fba2eb7408b7ed80623176ace76636aea1ac0ec530c1e2`。
- 旧报告来自干净的 `8a34674fe185cbb77c228d129df8993386675305` 提交，没有新式训练实现摘要。
- 服务器当前实现摘要 `9e524657…` 与上一版批准的目标完全相同。

上一修复错误地只登记了本地CPU报告的 `4e7938…` 旧摘要，未登记实际服务器报告。
这是证据映射遗漏，不能据此认定服务器PPO/环境发生变化。旧算法也包含安装生成文件，
本地没有egg-info；这解释了一种可能的跨环境摘要差异，但没有逐文件服务器证据时不宣称
已确认具体是哪一个文件导致不同。

现在额外登记真实服务器报告，绑定其旧摘要、干净Git提交和本次修订后的准确目标摘要。
本次代码只增加报告匹配条件和错误诊断，没有改变执行语义；不修改旧manifest或checkpoint。
已有preflight、benchmark、warm-start可以复用。未知旧摘要、错误revision或后续相关源码变化
仍会拒绝。回归测试直接使用以上NUS元数据，不再仅使用本地生成的合成摘要测试。

更新代码后，在xlogin先用下面的纯文件检查确认source gate，再提交。检查不导入Torch/native，
不加载模型、不分配GPU，不会改写任何训练产物；只有source gate成功才继续提交：

```bash
cd /home/h/hengzhi/SLS
git pull --ff-only
export SLS_PY=/home/h/hengzhi/venvs/sls/bin/python
export CUBLAS_WORKSPACE_CONFIG=:4096:8
$SLS_PY tools/check_training_sources.py && \
$SLS_PY tools/submit_slurm.py train --python "$SLS_PY" \
  --config configs/train/ironclad_a0_fullrun_15m.toml \
  --resume auto --partition gpu-long --time 2-00:00:00
```

`check_training_sources.py`只表示源码验证记录可复用；正式入口其余native、checkpoint、
runtime、identity和baseline门槛保持不变。

## job 821775 审查记录

基于当前实现及用户提供的失败信息：821775 运行8秒，FAILED / 1:0，
在 baseline 和 update 前因 `runtime.cuda_device` 不同被拒绝。
源设备为 `NVIDIA A100 80GB PCIe MIG 3g.40gb`，目标为 `NVIDIA A100-PCIE-40GB`。
没有另外下载服务器日志，也没有运行服务器作业或本地大规模 evaluation。

## 恢复结论

**直接复用现有迁移后的 latest.pt，不重新 warm-start，不重新 preflight/benchmark。**
本补丁没有改变模型、Observation、Simulator、PPO 更新、reward 或正式训练精度设置。
native source SHA 仍为 `24aa96acc54179d767b2666c615dfd836d8cbe7bab29ec1c2d13d5dbe6e12a23`。
原作业在恢复校验失败后只写 FAILED manifest，不保存未恢复的随机 trainer 到 latest。
重试会归档旧错误并继续；仍须完成尚未开始的新环境1000-seed baseline。

之前代码把全体 src/tools/configs 绑定成一个 hash，直接修改恢复工具也会使旧验证失效。
本次用 `configs/compatibility/training-validation-transitions.json` 明确绑定旧源树摘要
`4e7938b40612b42fc29dba0e3c194d09c87f579f15fe2e5cc7c610d46108bb34`
和审查后的新训练实现摘要。任何后续相关代码变化都会使该许可失效；未知旧报告也不会被放行。
不修改服务器旧 migration.json，不改 checkpoint metadata，不伪造重新通过验证。
新报告直接记录相关训练实现摘要；无关分析工具、原游戏适配器、其他配置和文档不使其失效。
活动配置继续通过 training identity 和保存的配置受保护。

## 分层规则与修复

| 变化 | 当前处理及理由 |
| --- | --- |
| GPU 名称、MIG/full GPU 名称差异 | runtime rebind，保留模型、Adam、worker、GRU、RNG；warning及manifest记录差异，不承诺跨硬件逐位一致 |
| Git commit | 可记录并 rebind；warm-start的实现摘要另外保护代码，不能靠换commit绕过源码审查 |
| hostname、Slurm job/partition、路径 | 运行记录，不作为模型兼容条件；路径移动须保留原配置、manifest及checkpoints |
| CUDA device count / CPU↔CUDA | 拒绝自动重绑；现格式保存全部可见设备RNG，拓扑变化不能仅按字符串处理 |
| Python ABI、Torch/CUDA/cuDNN版本、确定性模式 | 拒绝自动恢复，需明确的软件栈迁移及验证，不能认为任意版本都可重放 |
| matmul precision、cuDNN benchmark、CUBLAS配置 | 新checkpoint显式记录；已有记录发生变化时拒绝。旧checkpoint未记录的值标为MISSING并warning，不伪造旧值 |
| Observation/schema/词表、模型形状、PPO/reward、profile、workers/shards、seed边界 | 继续严格拒绝，需对应的显式迁移或新实验 |
| native 源码 / content 语义 | 自动runtime rebind现在严格拒绝；修复了旧代码反而允许native摘要变化的漏洞 |
| native同源重编译 | benchmark布局可复用，旧吞吐仅参考并warning；已验证warm-start仍绑定其native二进制，实际重编译后须验证执行环境 |
| benchmark GPU/节点不同 | 不锁硬件名称；吞吐数据不能当作新节点的性能保证，布局仍须合法且与checkpoint一致 |
| A100营销名称 | preflight/warm-start只要求CUDA及真实执行验证；Slurm仍默认申请A100 40GB资源 |
| 第一完整更新KL>0.05或clip>0.5 | 诊断warning，单步经验阈值不是兼容性证明。指标/参数有限、确实更新、同环境恢复重放仍为硬门槛 |
| checkpoint保存频率 | 可以变化，恢复identity沿用已保存配置的原频率表示，实际频率记入manifest；不重置任何训练状态 |
| evaluation seeds/频率/选模/阶段目标 | 继续保护同一训练比较协议，修改应明确作为实验配置变化 |
| evaluation_max_steps / deterministic | 补上旧identity遗漏：与保存的配置比较，避免静默改变评估口径或确定性 |
| baseline质量下限、final导出门槛 | 保留。前者是预设预算保护，后者决定部署资格，不属于GPU兼容检查 |

预检的 `--skip-build` 不再无理由要求编译器。warm-start不再硬编码48:16；实际布局来自
benchmark，完成完整更新验证后被写入identity，正式恢复仍禁止自行改变worker数。
本轮配置仍使用原48:16布局，没有调参。

发现旧warm-start/preflight没有像正式训练一样显式设置matmul precision=high；
新验证入口已统一设置。旧验证的该值不可从checkpoint证明，因此仅记录缺失，
不宣称它验证过跨精度逐位一致。正式训练的既有high设置没有改变，旧验证已证明
参数/worker/optimizer恢复链路，因此无需为新增metadata字段重做整个迁移。

## 本地验证

442 passed、1 skipped；Ruff通过。测试覆盖实际GPU字符串差异、Git差异、legacy metadata、
native/模型/PPO/软件栈/设备拓扑拒绝、相关源码变更拒绝、无关工具修改、benchmark重用、
checkpoint频率与评估频率的区别，以及旧identity缺失设置的补充保护。
CPU实际worker回归先更新以填充Adam，再恢复GPU名称变化的checkpoint；下一步指标与模型
张量一致，间接验证Adam/RNG/worker/记忆完整恢复。CPU测试模拟metadata差异，
不冒充MIG与整卡跨硬件逐位重放试验。

## 下一条服务器命令

先提交并推送本地补丁（必须包含兼容转换JSON、src/tools/tests和本说明），再执行：

```bash
cd /home/h/hengzhi/SLS
git pull --ff-only
export SLS_PY=/home/h/hengzhi/venvs/sls/bin/python
export CUBLAS_WORKSPACE_CONFIG=:4096:8
$SLS_PY tools/submit_slurm.py train --python "$SLS_PY" \
  --config configs/train/ironclad_a0_fullrun_15m.toml \
  --resume auto --partition gpu-long --time 2-00:00:00
```

现有editable安装会读取新Python代码，无需重装依赖或重编native。不要删除FAILED目录，
不要改旧checkpoint的GPU字符串，也不要再次调用warm-start覆盖新链路。
训练入口自动检查原initial文件hash、production验证标志、审查转换、native二进制、
训练identity和checkpoint契约；通过后记录rebind并开始baseline，baseline过门后才更新。
如果服务器报告的旧源树不在已审查转换中，或实际出现其他契约差异，应读取新错误逐项判断；
不要手动改hash来强行放行。
