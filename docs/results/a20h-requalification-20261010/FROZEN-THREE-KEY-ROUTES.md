# 冻结模型辅助三钥匙路线：原版首次 RNG 分歧

2026-10-10，DL / CPU，隔离分支 `codex/a20h-parity-qualification`。
生产模拟器、网络、奖励、PPO、训练分布和恢复契约均未改动；服务器任务未操作。
本轮是规则与可复现性诊断，不是正式胜率、课程资格或新训练。

## 版本与推理身份

读取冻结 90M 的只读 policy artifact：文件 SHA256
`2e8aa20ba50cab3fef7ea726db988137bd0018f9379c18637084faa65f5ce29f`，
模型 SHA256 `ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c`。
训练身份是 `IRONCLAD_A20_ACT1`，旧 native 来源
`1e30bb6cfa32f600cd63c59983c14ab8928fac4452c1586d630295d2c6ff9453`。
本轮运行 `IRONCLAD_A20_HEART`，当前 native 来源
`e4f8452ff3cea688e49a362488814d4c2e29d9b721bcc6cd71c8d9f1d9fa733a`，
二进制 `6ee14bf97312bab04b66fcb0a6767f52c4a688eeb0c1fcf5953b5ab8764f570b`。
这是明确的诊断环境迁移，不恢复历史优化器，不增加 checkpoint 兼容白名单。

正常 Neow 开局；模型负责实际战斗、构筑等决策，公开地图规划器干预选路，
遇到真实合法钥匙获取选择时优先获取钥匙。没有人工牌组、HP、金币或钥匙注入。
每局实际改变 5 个模型 argmax；完整保留干预和未干预选择、原模型概率/value 与实际动作。
不是纯 greedy 模型能力评估，也不能将不同 seed 的旧启发式失败 cohort 当作因果对照。

每个边界用模型自己的 recurrent memory 推理，下一步 previous action 输入实际执行的
动作类型加 1，previous reward 输入实际 backend base reward。历史 commit
`5435d4a2f423a1e3b445977a48fae41ac90b5837` 的 PPO 与当前 PPO/evaluate 源码均采用
这一 base reward 输入；shaped reward 是训练目标，不能据其名称替换 recurrent 输入。
公开推理历史和 private native checkpoint 分文件存储；private 状态不进入模型。

## 4 条 bounded native 轨迹

运行前 seed namespace 冲突检查通过，预算每局最多 256 个实际决策。

| Seed | 实际动作 | 结果 | 终点 |
|---|---:|---|---|
| 131200530 | 124 | 三钥匙后主动诊断停止 | Act1 floor9 |
| 131200531 | 120 | 实际 DEATH，仅 Ruby/Sapphire | Act1 floor12 |
| 131200532 | 74 | 三钥匙后主动诊断停止 | Act1 floor7 |
| 131200533 | 194 | 三钥匙后主动诊断停止 | Act1 floor12 |

总计 512 个实际动作、516 个公开/native 边界。再次从正常 reset 推理，所有公开和 private
JSONL 的 SHA256 逐字节一致，包含模型输入、候选顺序、memory 哈希和实际动作。
从全部 516 个完整 checkpoint 分别恢复，执行全部已记录后缀，每一步完整 checkpoint 一致。
没有删除 replay_required、未完整状态或状态字段；这不验证尚未执行的游戏后缀。
三钥匙停止既不是死亡，也不是 Act1 或 A20H 通关。3/4 不报告为胜率。

## 最短路线的实际原版复验未通过

原版 JAR `cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673`，
Oracle 1.3.52 `0710d21de30dc28dc7c137d63c587eec0e0d16a4b0e96d6708bde89699044f44`。
从正常开局执行 seed 131200532 的真实动作记录。原版未执行模型推理，所以这份证据只比较
实际参考动作的规则路径，不声称 original/backend 编码与 recurrent memory 等价。

原版记录 60 个边界、59 个动作。在 boundary 59 首次 RNG 不同：

| 项目 | Native 默认 | 原版实际 |
|---|---:|---:|
| Discovery retrieval updates | 14 | 15 |
| card_random counter | 48 | 51 |
| card_random seed0 | 5147829919490081228 | 15933971079182409890 |
| card_random seed1 | 8782328499002539523 | 18175474268977837455 |

该边界的完整公开 Observation 和合法动作仍一致；0–58 的公开状态、合法动作、有效 RNG
均一致，59 个已执行 transition 的公开状态、base reward 和 terminal/horizon 信息一致。
boundary 58 为实际 `SELECT_CARD / CHOICE:1`，Oracle 被动记录实际检索更新 15 次、FPS 限制 60。
原版此时只有 Sapphire，尚未取得 Emerald/Ruby；因此没有同一局原版三钥匙通过证据。
首次 RNG 分歧就停止，不将之后未执行的 native 动作转称为真实原版后缀。

对同一份实际原版前缀做两次独立 native reset：默认路径准确重现只有 boundary 59 RNG
不匹配；仅在 boundary 58 提供实际被动时钟见证 15 后，全部 60 个边界的公开/动作/RNG
和 transition 匹配。两种路径分别生成自己的 60 个 checkpoint，120 个完整后缀恢复均通过。
后者是条件化因果定位，不是默认生产路径资格，也不是新增两局原版样本。

绑定 stock JAR 的 `DiscoveryAction.update()` 字节码显示：每次 update 先生成候选，再检查
duration/retrieveCard 并 tickDuration。当前 native `chooseDiscoveryCard()` 会按检索更新次数
重复生成并丢弃候选，默认次数为 14。该样本支持“额外一次实际 update 消耗额外 RNG”解释，
不能断言所有 seed 固定为 15，也不能从本例推出所有未来可见行为必然不同。
此前自然 Emerald 路线的两个真实时钟均为 14；本轮说明“60 FPS 限制意味着固定 14”
不能作为全局一致性保证。未来随机卡牌/药水结果仍可能因此分叉。

## 结论与下一步

模拟器校验现在应优先确认 **实际部署运行时的时间语义**，再扩展三钥匙与后段资格。
不能把默认 14 全改成 15，也不能在生产推理输入里泄露 RNG/Oracle 时钟来追平原版。
下一轮先用固定公开动作、重复真实运行、不同负载/帧时长采样，量化 Discovery RNG 分歧；
明确固定时间步受控部署与真实帧驱动部署各自的训练/评估身份，形成单独可审查方案。
同时选择未涉及此生成卡牌分支的自然路线扩大规则覆盖；需如实注明选择偏向。

其余高优先缺口是自然同局三钥匙后完整 Act2–4、Heart 攻击与药水的长交互、
尸体能力/召唤及正常历史恢复。不要用增训或性能优化掩盖这些资格缺口。
后段状态课程可准备来源/公开前缀/模型记忆/恢复验证，但 `training_gate=NOT_QUALIFIED`，
本轮状态不接入生产训练。critic 判断仍等待服务器正式归档，不从本轮 value 推导成功概率。

真实失败前缀已冻结成独立回归，默认与条件化结果分别断言，并保留完整恢复后缀。
CPU 全量 **1746 passed、2 skipped、4 warnings**，257.56 秒；Ruff、词表、diff 检查通过。
配置仍为 31/33，两个既有 lambda 实验的 bound implementation 身份不匹配未改写。
native 来源和训练实现摘要保持不变，未重编译或覆盖原工作区 native 构建产物。
原工作区 197 个待提交文件/status 未改变，所有游戏目标恢复，owned journal 为 RECOVERED。
测试、版本绑定、全部实际数据与复现辅助脚本见 `frozen-three-key-routes-evidence.json` 封存索引。
