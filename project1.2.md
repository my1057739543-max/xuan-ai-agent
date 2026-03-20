# Xuan Open Agent 项目规划 1.2：游戏助手场景增强版

# 你必须使用spring ai这个框架开发如果涉及agent开发部分
！！！！！！！！！！！！重要！！！！！！！！本版本目标不是推翻 1.1，而是在 1.1 基础上做业务化打磨：把通用 RAG 能力升级为“多游戏可控、检索更准、回答更稳”的游戏助手 Agent。优先解决“问A游戏却答到B游戏”的错误与“技巧文档切片不贴合实战问法”的问题。

## 1. 版本定位与目标

### 1.1 版本定位
- 1.0：ReAct Agent + 工具调用 + SSE 可观测。
- 1.1：基础 RAG（上传、切片、向量化、检索、前后端联动）。
- 1.2：游戏业务增强版，围绕“准确召回、正确归属、稳定回答”打磨。

### 1.2 本版本核心目标
- 建立“按游戏隔离”的知识组织方式，避免跨游戏污染回答。
- 设计更契合“游戏技巧文档”的切片策略，提高命中和可用性。
- 引入会话记忆摘要，让多轮提问上下文更稳定。
- 引入意图识别与问题重写，提升检索 query 的可检索性。
- 引入检索后处理，降低错误片段进入最终回答的概率。

### 1.3 本版本核心交付
- 游戏知识域隔离机制（gameKey + metadata filter）
- 游戏技巧专用 chunk 策略（LLM 优先切片 + 结构化输出）
- 会话记忆摘要模块（短期记忆压缩）
- 意图识别 -> 问题重写模块
- 检索后处理模块（过滤、去重、重排、冲突处理）
- 可观测与评估指标（命中率、跨游戏误答率、回答可执行性）

---

## 2. 版本边界

### 2.1 本版本必须做
- 支持多游戏知识隔离，至少覆盖以下主流游戏示例：
  - VALORANT（瓦罗兰特）
  - CS2
  - Apex Legends
  - League of Legends（LOL）
- 上传文档入库时强制绑定 `gameKey`。
- 查询阶段默认使用 `gameKey` 检索过滤。
- 若用户问题未显式给出游戏，先进行意图识别并尝试从会话记忆推断。
- 引入检索后处理管道，至少包含：低分过滤、重复片段去重、同义段合并。

### 2.2 本版本暂不做
- 不做多模态（截图识别、视频帧战术分析）。
- 不做复杂权限系统（团队、角色、租户 ACL）。
- 不做在线学习自动回写（用户反馈自动改库）。
- 不做大规模分布式检索集群。

---

## 3. 业务场景定义（游戏助手）

### 3.1 主要用户问题类型
- 技巧学习：如“瓦如何急停更稳？”
- 参数配置：如“CS2 灵敏度怎么换算？”
- 对局策略：如“残局 1v2 怎么处理？”
- 训练计划：如“每天 30 分钟怎么练枪？”

### 3.2 回答要求
- 回答必须与用户目标游戏一致。
- 回答优先给可执行步骤，不只讲概念。
- 回答中标注关键依据来源（命中片段摘要）。
- 若检索证据不足，明确说明不确定并给补充提问建议。

---

## 4. 知识域隔离设计（gameKey）

### 4.1 元数据规范
向量文档 metadata 至少包含：
- `gameKey`：游戏唯一键，例如 `valorant`、`cs2`、`apex`、`lol`
- `topic`：技巧主题，如 `counter_strafe`、`crosshair_placement`
- `skillLevel`：可选，`beginner|intermediate|advanced`
- `mapOrRole`：可选，如地图名、角色/位置
- `sourceType`：`guide|note|qa|patch_note`
- `fileId`、`chunkIndex`、`uploadTime`

### 4.2 gameKey 管理原则
- 上传接口新增必填 `gameKey` 字段。
- 允许维护别名映射，例如：
  - `瓦`、`瓦罗兰特`、`valorant` -> `valorant`
  - `cs`、`cs2`、`反恐精英2` -> `cs2`
- 检索默认带 `gameKey` 过滤；不允许无过滤全库检索作为默认行为。

### 4.3 跨游戏污染防护
- 若 query 识别出 `gameKey` 与会话最近 `gameKey` 冲突：
  - 以用户当前显式表达为准。
  - 并在日志事件中记录冲突判定。
- 若无法确定 `gameKey`：
  - 发出澄清问题（例如“你问的是瓦罗兰特还是 CS2？”）。

---

## 5. 游戏技巧文档切片策略（重点）

### 5.1 为什么 1.1 切片不够
- 通用 TokenTextSplitter 对“步骤型技巧”语义边界不敏感。
- 会把“前提、动作、错误示例、纠正建议”切散，导致召回片段可执行性下降。

### 5.2 1.2 切片目标
- 一个 chunk 尽量表达一个完整技巧单元。
- 同时保留可检索关键词（动作名、武器名、地图点位、错误动作）。
- 切片优先由大模型完成语义边界识别，避免步骤被截断。

### 5.3 切片主策略（LLM 优先）
1. 文档预切分：先按标题/段落做粗切，降低单次大模型输入长度。
2. LLM 语义切片：让大模型按“完整技巧单元”输出切片结果。
3. 结构化标准化：把 LLM 输出映射为统一 chunk 结构。
4. Token 安全裁剪：超过上限时做二次裁剪，保证可入库。
5. 元数据增强：写入 `gameKey`、`topic`、`difficulty`、`sectionType`。

### 5.4 LLM 切片输出约束（必须）
- 每个 chunk 必须围绕一个技巧主题。
- 每个 chunk 需要包含可执行信息，至少有 `steps` 或 `commonMistakes` 之一。
- 禁止跨游戏混切；若源文档有多游戏内容，必须拆成不同 `gameKey` 的 chunk。
- 输出必须是可解析结构（JSON 或等价结构），便于程序校验。

### 5.5 回退策略（稳定性要求）
- 当出现以下情况时，自动降级到规则切片（TokenTextSplitter）：
  - LLM 超时
  - LLM 输出不可解析
  - LLM 单次成本或时延超过阈值
- 降级后仍要保证元数据完整，并记录切片模式：
  - `chunkMode=llm` 或 `chunkMode=rule`

### 5.6 推荐 chunk 模板（示例）
- `title`: 急停（Counter-Strafe）
- `applicable`: valorant / cs2
- `whenToUse`: 中距离对枪前停稳
- `steps`: 3~5 步可执行动作
- `commonMistakes`: 常见错误与纠正
- `drillPlan`: 5~10 分钟训练法

说明：
- 最终入向量库内容可以是自然文本，但建议按上述结构拼成统一格式，提升 embedding 的判别性。
- LLM 切片必须产出该结构或可无损映射到该结构。

---

## 6. 新增能力一：会话记忆摘要模块

### 6.1 目标
- 在长对话中保留“用户游戏偏好、当前目标、最近技巧主题”，减少上下文漂移。

### 6.2 最小实现
- 每 N 轮（建议 6~8 轮）生成一次摘要。
- 摘要内容至少包括：
  - 当前 `gameKey`
  - 用户目标（上分、训练、纠错）
  - 最近已讲过的技巧点
  - 未解决问题
- 下一轮检索前，将摘要作为 query 重写与过滤的辅助输入。

### 6.3 存储建议
- 会话级存储：`sessionId -> memorySummary`
- 可先内存 + TTL，后续再落库。

---

## 7. 新增能力二：意图识别 -> 问题重写

### 7.1 意图识别分类（最小集）
- `skill_how_to`：技巧怎么做
- `mistake_fix`：错误纠正
- `training_plan`：训练计划
- `config_tuning`：参数设置
- `comparison`：方案对比

### 7.2 问题重写目标
- 把口语化、模糊问法改写为“可检索 query”。
- 补齐隐式信息：游戏名、技巧别名、术语标准化。

### 7.3 重写示例
- 原问：`我老是急停后第一枪飘怎么办`
- 重写：`valorant 急停（counter strafe）第一枪不准的常见原因与纠正步骤`

### 7.4 约束
- 重写不改变用户意图。
- 重写结果必须保留 `gameKey`。

---

## 8. 新增能力三：检索后处理

### 8.1 目标
- 让进入最终回答的证据更干净、更相关、更一致。

### 8.2 最小后处理管道
1. 低分过滤：低于阈值的 hit 丢弃。
2. 去重：基于内容相似度或同源 chunk 去重。
3. 主题聚合：按 `topic` 聚类，避免重复表达。
4. 轻量重排：优先“步骤完整 + 当前意图匹配”的片段。
5. 冲突检查：若出现跨游戏证据，剔除非当前 `gameKey` 证据。

### 8.3 输出给生成阶段的数据结构
- `topEvidence`: 前 3~5 条证据
- `evidenceSummary`: 压缩后的要点
- `riskFlags`: 证据不足/冲突/过旧等标记

---

## 9. 系统流程（1.2）

1. 用户提问进入 `/api/chat/stream`。
2. 意图识别与 `gameKey` 判定。
3. 若信息不足，触发澄清；否则执行问题重写。
4. 按 `gameKey + rewrittenQuery` 执行检索。
5. 对检索结果执行后处理（过滤/去重/重排/冲突处理）。
6. 将后处理结果与会话摘要一起注入 Agent。
7. Agent 生成回答；必要时继续调用工具。
8. 返回 SSE 事件并更新会话记忆摘要。

---

## 10. 配置项建议（新增）

建议在 `xuan.rag` 或 `xuan.agent` 下新增：
- `xuan.rag.game-isolation-enabled=true`
- `xuan.rag.default-game-key=valorant`
- `xuan.rag.chunk.strategy=llm_first`
- `xuan.rag.chunk.llm.enabled=true`
- `xuan.rag.chunk.llm.model=deepseek-chat`
- `xuan.rag.chunk.llm.max-input-tokens=6000`
- `xuan.rag.chunk.llm.max-output-chunks=24`
- `xuan.rag.chunk.llm.timeout-seconds=20`
- `xuan.rag.chunk.llm.fallback-to-rule=true`
- `xuan.rag.chunk.max-tokens=700`
- `xuan.rag.chunk.overlap-tokens=80`
- `xuan.rag.postprocess.enabled=true`
- `xuan.rag.postprocess.min-score=0.55`
- `xuan.agent.memory-summary.enabled=true`
- `xuan.agent.memory-summary.every-n-turns=6`
- `xuan.agent.intent-rewrite.enabled=true`

---

## 11. 事件可观测性（SSE 扩展）

在现有事件基础上新增或细化：
- `intent_detected`
- `query_rewritten`
- `retrieval_postprocessed`
- `memory_updated`
- `clarification_needed`

每个事件 payload 至少带：
- `traceId`
- `sessionId`
- `gameKey`
- `timestamp`

---

## 12. 验收标准（必须量化）

### 12.1 功能验收
- 上传知识时可指定并保存 `gameKey`。
- 同一问题在不同 `gameKey` 下检索结果明显不同。
- 对话中可看到意图识别、重写、后处理事件。
- 会话超过阈值轮数后可看到记忆摘要更新。

### 12.2 效果验收（建议基线）
- 跨游戏误答率 <= 5%。
- 检索命中可用片段比例 >= 75%。
- 回答含可执行步骤比例 >= 80%。
- LLM 切片结构化校验通过率 >= 95%。
- LLM 切片降级率（到规则切片）<= 10%（压测口径）。
- 关键问题端到端响应时延可控（例如 P95 <= 8s，按本地环境可调整）。

---

## 13. 开发拆分建议（里程碑）

- M1：gameKey 隔离 + 上传入库改造
- M2：LLM 优先切片 + 规则回退 + 结构化校验
- M3：意图识别与问题重写
- M4：检索后处理
- M5：会话记忆摘要
- M6：联调评测与参数调优

---

## 14. 本版本结论

1.2 版本不是“增加更多功能按钮”，而是把 1.1 的基础 RAG 打磨成“能稳定服务游戏问答”的业务化系统。核心抓手是三件事：
- 按游戏隔离（防串题）
- 按技巧结构切片（提命中）
- 检索链路增强（意图重写 + 后处理 + 记忆摘要）

本文件作为 1.2 开发的主约束文档，后续实现细节（代码、接口、测试）必须与此边界保持一致。
