# NUS：中止 checkpoint 的独立开发确认

只在新的研究 worktree 构建和评估，不续训、不改写原 run。脚本会逐个验证四模型 SHA256；原 run 已变化或模型位置不匹配时停止，不用别的 checkpoint 替代。源码 native 必须与中止实验相同。封存最终保留集不使用。

## 准备与提交

在原来的服务器 checkout 中获取研发分支，然后建立新的 detached worktree。首次执行；新目录已存在时先检查，不要删除或覆盖。执行代码固定为已验收并推送的 `6f926a8e2e169735bfc113b99689352a143b1d53`；分支之后追加的报告提交不改变这些工具。

```bash
(
set -euo pipefail
cd /home/h/hengzhi/SLS-act12-critic20m-20261009-fix2
git fetch origin codex/critic20m-evidence-followup
git worktree add --detach /home/h/hengzhi/SLS-critic-review-20261010 6f926a8e2e169735bfc113b99689352a143b1d53
cd /home/h/hengzhi/SLS-critic-review-20261010
/home/h/hengzhi/venvs/sls/bin/python tools/submit_stopped_critic_qualification.py \
  --source-root /home/h/hengzhi/SLS-act12-critic20m-20261009-fix2 --dry-run
/home/h/hengzhi/venvs/sls/bin/python tools/submit_stopped_critic_qualification.py \
  --source-root /home/h/hengzhi/SLS-act12-critic20m-20261009-fix2
)
```

提交入口只用标准库，不在登录节点导入 Torch 或运行 native。资源为 GPU-long、xgpg、1×A100-40、16 CPU、64GB、最多 12h。输出 job id，请先把提交输出反馈。作业先只读收集原 preparation/compute-gate/preflight/allocation ledger 及 `sacct -j 924694`，随后在新工作树构建 native，执行四模型×4096 greedy 开局。每 128 个 seed 保存独立结果，成功结束才写 summary。`--dry-run` 不提交，正式入口写独占 receipt 防止重复提交。

程序完整的 CPU smoke 已在本地通过。GPU 和 Slurm 执行尚未本地模拟为通过；NUS 结果将记录实际 Torch/CUDA/硬件/source/模型/seed 身份。任何输入 hash、native 源、构建或评估执行错误都会停止，不启动训练。若队列或路径不同，请把错误输出反馈后再调整。

## 作业结束后打包反馈

在新的 worktree 执行；输出 archive 名称不得已经存在：

```bash
(
set -euo pipefail
cd /home/h/hengzhi/SLS-critic-review-20261010
test ! -e local/critic20m-qualification-20261010.tar.gz
tar -czf local/critic20m-qualification-20261010.tar.gz \
  local/reports/critic20m-nus-confirmation-20261010 \
  local/reports/critic20m-server-evidence-20261010.json \
  local/operator/stopped-critic-qualification-submission.json \
  local/runs/slurm-logs
sha256sum local/critic20m-qualification-20261010.tar.gz
)
```

下载该新包到 `D:/SLS/local/imports/critic20m/`，反馈 SHA256 和日志最后部分。若中途退出，先反馈错误及保存的 chunk；不要把缺失 seed 的部分结果记作失败或完整确认，不重用原输出目录。此轮不会执行新的 2M/20M 训练；自然 reset 训练 sampler 及其新配方需要在本轮确认之后独立验收、提交和推送。

## 本地复现归档审计

```powershell
conda activate DL
$env:CUDA_VISIBLE_DEVICES = '-1'
$env:PYTHONPATH = "$PWD/src;$PWD"
python tools/audit_stopped_critic.py `
  --archive D:/SLS/local/imports/critic20m/critic20m-stopped-20261010.tar.gz `
  --extract local/imports/critic20m-recheck-new-id `
  --parent D:/SLS/local/runs/ironclad-a20-act1-win-90m-continuation/final.pt `
  --output local/reports/critic20m-recheck-new-id.json
```

只读复核已有提取目录可加 `--reuse-extraction`；它从原 TAR 重读每个成员并比对提取文件 SHA256，不跳过验证、不写提取目录，仍需新的 output 文件名。
