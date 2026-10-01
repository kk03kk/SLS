# Win 70M → 90M：2026-10-02 决策与执行契约

本次服务器包日期为 2026-10-01，配置/计划文件的 `20261001` 用于标识该证据批次。
70M 固定 endpoint 相对冻结 56M 在开发确认集提升 +2.78pp，p=.01263，符合原预注册成功
规则；不是原版胜率或 reward-only 因果效应。[完整结案](results/win70m-20261001/README.md)。

## 训练方案

- hypothesis：已得到开发支持的 Win PPO 配方在新增约 20M decisions 后仍能提高正常 A20
  Act1 完整 clear probability。不是承诺 90M 必然更好。
- 唯一训练变量：预算从 70,008,832 到目标 90,000,000，最后最多超一批 16,383。
  约 1,221 新 update；不重置 Adam、workers、循环记忆、Python/torch/CUDA RNG 或 next_seed。
- source 为完整 `local/runs/ironclad-a20-act1-win-70m-continuation/latest.pt`，SHA256
  `ec3ca8723ba097649739418c57d944aad31a1efd65fc67222ff52f8b0b47017f`。
- control 为同 run `final.pt`，SHA256
  `cb53fee1ae3f47cc906665c903068dabaf38a328be1e07697e6d17c5b21d7d5c`。
  两者 model weights 完全一致；latest 保留训练状态，final 用于固定评估。
- 68M 周期最佳净胜 70M 仅 8/2048、p=.736，不足以牺牲最新训练状态回退。
- seed 继续 120000000 的原随机流（不添加第二个 training seed），64 workers /16 shards。
  Win reward、gamma=1、λ=.98、LR=3.125e-5、归一化、critic/model、entropy/PPO 强度全部不变。
  不改 observation/action/reward/native semantics；版本仍 input/model/checkpoint v5。
- training implementation `9da7e28c…` 与本轮服务器相同；native source `1e30bb6c…` 相同。
  仅预算/评估/路径身份重新绑定，fresh selection baseline。源码 Git commit 更新不表示
  可保证跨平台逐位重放。旧归档、旧 config、旧 champion 不覆盖。

## 评估与判据

- 新 periodic selection `[8000004000000,+512)`，每 4M，checkpoint 每 1M。
  旧 8e12 固定开发块已反复使用，轮换以降低持续选模对同块的依赖；不混入训练。
- 新 development confirmation `[8000003000000,+2048)`。训练结束在同一进程/实际 runtime
  自动评估 frozen 70M、固定 90M endpoint、periodic selected；三份 JSON 与 bundle 留档。
  不提交独立大评估作业，也不在确认集上继续挑 checkpoint。
- primary 为固定 90M 与固定 70M 的配对完整通关率。成功需净提升≥2pp、精确双侧 McNemar
  p<.05、描述性配对 95% 下界>0、health 全零；secondary 不能给出明确相反证据。
  2,048 不保证能检出任意小收益；一个 training seed 不证明配方多种子泛化。
- 若 endpoint 与 selected 上界都≤0，说明本轮预算没有改善；其余未达成功情形为无定论，
  不等于算法等效，也不自动继续 110M。结合诊断选择一项 λ 或 normalization 变化，并用
  相同起点/预算的 continuation control，不能把多个核心变化同时加入。
- 最终保留 `[9000000000000,+4096)` 本作业不用。之后先冻结一个 candidate/hash、baseline、
  实际 runtime、batch/decoding 与分析规则，再一次配对终评，不用其反复选模型。原版完整
  游戏泛化还需要独立 stock-game 测量，模拟器数字不能替代它。

## 服务器执行

一张 xgpg A100 40GB、16 CPU、64GB；预计 58–66 GPUh，申请 72h。估计来自本轮训练
102.77 decisions/s 加准备/评估余量，不是已运行测量。GPU node 内复用已验证 layout，执行
source/恢复 preflight；不在 login node 做训练或 native 编译。

最终阶段总结给出唯一复制命令：安全切换到 main、ff-only 拉取指定 commit、激活原 venv，
运行 `python tools/submit_win90m.py`。仅人类执行 submission。工具核对计划/config/source/hash，
拒绝脏源码、已有目标目录与重复 receipt。失败先查 receipt 和队列，不盲目重复提交。
若被 Slurm 限时中断，保留目录与日志，检查 manifest 再显式恢复；不把 exit=0 当 COMPLETE。

## Git 与验证证据

按人类要求，本次把最新提交快进到 main，再删除其余远程与本地分支；不重写 main 历史。
旧分支 tip 和可达关系保存在 `docs/results/win70m-20261001/git-consolidation.json`，本地
Git bundle 在忽略目录 `runs/archives/`。旧分支不存在 main 外的独有提交；AGENTS.md 的
既有修改未纳入本次，也未编辑。原下载训练/日志证据不随 Git 删除。

[本地验证记录](results/win70m-20261001/local-validation.json)包含 pytest/静态检查结果。
真实 70M→90M 初始化逐字段核对 model/optimizer/trainer/RNG/environments，源 hash 不变，
唯一 checkpoint contract 变化为 training_config_sha256。完整 A100 生产 layout 更新由
compute preflight 验证，本机未启动长期训练。
