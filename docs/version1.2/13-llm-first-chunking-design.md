# 13. LLM 优先切片设计（含规则回退）

# 你必须使用spring ai这个框架开发如果涉及agent开发部分

## 1. 目标

将 1.1 的通用 Token 切片升级为 LLM 优先语义切片，让每个 chunk 更接近“完整技巧单元”。

---

## 2. 总体策略

主策略：

- LLM-first：优先调用大模型执行语义切片。

回退策略：

- 当 LLM 失败、超时、不可解析或超预算时，自动回退规则切片。

记录策略：

- 每个 chunk 记录 chunkMode=llm 或 chunkMode=rule。

---

## 3. 处理流程

1. 文档粗切分（按标题/段落）
2. 构建 LLM 切片 prompt（包含 gameKey、主题要求、输出 schema）
3. 调用模型返回结构化 chunks
4. 结构化校验（字段完整、类型正确、gameKey 一致）
5. Token 安全裁剪（超长块二次拆分）
6. 元数据补全并入库
7. 失败则自动回退 TokenTextSplitter

---

## 4. 建议输出 Schema（逻辑定义）

每个 chunk 至少包含：

- title
- gameKey
- topic
- steps（数组，可空）
- commonMistakes（数组，可空）
- drillPlan（可空）
- rawText

约束：

1. steps 与 commonMistakes 至少一个非空。
2. gameKey 必须与当前文档 gameKey 一致。
3. rawText 长度需在可 embedding 范围内。

---

## 5. Prompt 设计要求

1. 明确角色：游戏教练知识整理器。
2. 明确禁止：不得跨游戏混切，不得虚构来源。
3. 明确输出：严格 JSON，不允许自然语言前后缀。
4. 明确粒度：一个 chunk 对应一个技巧单元。

---

## 6. 回退触发条件

- LLM 调用超时
- JSON 解析失败
- schema 校验失败
- 单文档处理时延超过阈值
- 单文档 token 成本超过阈值

回退后动作：

1. 规则切片生成 chunk
2. 元数据补齐
3. 发 chunk_fallback 事件
4. 统计 fallback 原因

---

## 7. 配置项建议

- xuan.rag.chunk.strategy=llm_first
- xuan.rag.chunk.llm.enabled=true
- xuan.rag.chunk.llm.model=deepseek-chat
- xuan.rag.chunk.llm.timeout-seconds=20
- xuan.rag.chunk.llm.max-input-tokens=6000
- xuan.rag.chunk.llm.max-output-chunks=24
- xuan.rag.chunk.llm.fallback-to-rule=true
- xuan.rag.chunk.max-tokens=700
- xuan.rag.chunk.overlap-tokens=80

---

## 8. 事件与日志

建议新增事件：

- chunk_started
- chunk_llm_completed
- chunk_schema_validated
- chunk_fallback
- chunk_completed

日志字段建议：

- fileId
- gameKey
- chunkMode
- chunkCount
- latencyMs
- tokenUsage
- fallbackReason

---

## 9. 测试建议

1. 单元测试：schema 校验、回退逻辑。
2. 集成测试：不同游戏文档切片后 gameKey 保持一致。
3. 压测：大文档和多文档并发下 fallback 率。
4. 回归测试：规则切片路径仍可用。

---

## 10. 验收标准

- 结构化校验通过率 >= 95%
- fallback 率 <= 10%（压测口径）
- 入库 chunk 平均可执行片段占比显著提升
