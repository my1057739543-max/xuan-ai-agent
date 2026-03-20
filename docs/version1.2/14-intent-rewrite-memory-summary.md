# 14. 意图识别、问题重写与会话记忆摘要

# 你必须使用spring ai这个框架开发如果涉及agent开发部分

## 1. 目标

提升检索输入质量和多轮稳定性，减少“问法模糊导致召回偏移”。

---

## 2. 意图识别

最小意图集合：

- skill_how_to
- mistake_fix
- training_plan
- config_tuning
- comparison

输出字段建议：

- intentType
- confidence
- entities（weapon/map/role）
- detectedGameKey

---

## 3. 问题重写

目的：

- 将口语问题转为检索友好 query。

要求：

1. 不改变原始意图。
2. 保留或补齐 gameKey。
3. 合并同义术语（如 急停 / counter strafe）。

示例：

- 原问：我急停后第一枪总飘怎么办
- 重写：valorant 急停 counter strafe 第一枪不准的原因、纠正步骤、训练方法

---

## 4. 会话记忆摘要

## 4.1 摘要触发

- 每 6~8 轮触发一次。
- 或在 gameKey 切换时立即触发一次。

### 4.2 摘要内容

- currentGameKey
- userGoal
- coveredSkills
- unresolvedQuestions
- preferredStyle（步骤型/原理型）

### 4.3 存储方式

- sessionId -> summary（内存 + TTL）
- 后续可扩展为数据库持久化

---

## 5. 检索前组装流程

1. 读取最新会话摘要。
2. 执行意图识别。
3. 执行 query 重写。
4. 输出 retrievalQuery 与 finalGameKey。

---

## 6. 事件可观测

建议新增事件：

- intent_detected
- query_rewritten
- memory_updated

payload 建议：

- intentType
- rewrittenQuery
- currentGameKey
- summaryVersion

---

## 7. 配置项建议

- xuan.agent.intent-rewrite.enabled=true
- xuan.agent.intent-rewrite.model=deepseek-chat
- xuan.agent.intent-rewrite.timeout-seconds=8
- xuan.agent.memory-summary.enabled=true
- xuan.agent.memory-summary.every-n-turns=6
- xuan.agent.memory-summary.ttl-minutes=180

---

## 8. 验收标准

1. 模糊问法可输出稳定重写 query。
2. 重写结果 gameKey 正确率达到目标。
3. 长会话中上下文漂移明显下降。
4. 可观测事件可完整回放决策过程。
