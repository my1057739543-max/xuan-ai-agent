# 模块 08：文档切片与向量入库

你必须使用spring ai这个框架开发如果涉及agent开发部分

## 1. 模块目标
- 用 Spring AI `TokenTextSplitter` 将文档切片。
- 为切片补充可检索 metadata。
- 通过 `VectorStore.add(List<Document>)` 完成 embedding 与向量持久化。
- 建立“文件库主数据（`kb_file`）与向量数据（`vector_store`）”的一致性关系，支持按 `fileId` 级联删除向量。

## 2. 本模块范围
- `DocumentChunker` 实现与参数化。
- chunk metadata 规范。
- 向量入库过程与失败回滚策略。
- 文件维度统计（chunk 数量、入库结果）回写 `kb_file`。
- 文件删除时按 `fileId` 清理对应向量（级联删除策略）。

## 3. 切片参数建议
- `chunk-size`: 800
- `min-chunk-size-chars`: 350
- `min-chunk-length-to-embed`: 10
- 中文语料追加中文标点到 `punctuationMarks`。

## 4. metadata 规范
每个 chunk 至少包含：
- `fileId`
- `fileName`
- `chunkIndex`
- `sourceType`
- `uploadTime`

删除联动要求：
- `fileId` 必须作为删除向量的唯一关联键，入库时不可缺失。
- 所有切片必须保留同一个来源文件的 `fileId`，禁止混写或丢失。

## 5. 建议输出文件
- `src/main/java/com/xuan/xuanopenagent/rag/document/DocumentChunker.java`
- `src/main/java/com/xuan/xuanopenagent/rag/RagIngestionService.java`（补充入库流程）
- `src/main/java/com/xuan/xuanopenagent/rag/model/RagIngestionResult.java`
- `src/main/java/com/xuan/xuanopenagent/rag/RagDeletionService.java`（或在 `RagIngestionService` 内补充删除向量逻辑）

## 6. 处理流程
1. 接收模块 07 产出的 `Document` 列表。
2. 调用 `TokenTextSplitter.builder()` 切片。
3. 对每个 chunk 追加 metadata。
4. 调用 `VectorStore.add(...)` 入库。
5. 统计 chunk 数量并更新 `kb_file` 状态为 READY。
6. 文件删除请求到达时，按 `fileId` 执行向量清理，再删除 `kb_file` 与原始文件（或采用事务一致性补偿策略）。

## 7. 验收标准
- 同一文件可稳定生成可重复的 chunk 序列。
- `vector_store` 中可检索到带 metadata 的文档片段。
- 入库失败会将 `kb_file` 标记为 FAILED，并记录失败原因。
- 对空文档、超短文档有清晰处理策略。
- 删除文件后，`vector_store` 中该 `fileId` 的切片向量不可再被检索命中。
- 删除失败时有可追踪日志，并可通过重试或补偿任务完成最终一致性清理。

## 8. 风险与处理
- 风险：文档过长导致 embedding 批次异常。
- 处理：使用 Spring AI batching 策略，必要时调小单批输入。
- 风险：切片过碎导致召回噪声。
- 处理：先用默认参数，基于检索日志再迭代调优。
