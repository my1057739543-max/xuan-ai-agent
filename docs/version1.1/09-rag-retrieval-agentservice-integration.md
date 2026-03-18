# 模块 09：检索增强问答与 AgentService 集成

你必须使用spring ai这个框架开发如果涉及agent开发部分

## 1. 模块目标
- 在 `AgentService` 前置接入 RAG 检索。
- 使用 `RetrievalAugmentationAdvisor` 生成增强回答。
- 将命中片段与回答链路联动到现有 SSE 输出。

## 2. 本模块范围
- 请求参数增加 `useKnowledgeBase` 开关。
- 根据开关动态挂载 `RetrievalAugmentationAdvisor`。
- 使用 `VectorStoreDocumentRetriever` 配置 `topK`、`similarityThreshold`。
- 支持按 metadata filter 进行范围检索。
- 返回命中片段摘要给前端展示。

## 3. 接入原则
- 复用已有 `/api/chat/stream`，不新增独立主聊天链路。
- 普通对话与知识库对话共用同一控制器和会话模型。
- 只在服务层前置 RAG，不引入复杂 Agent Tool 编排。

## 4. 建议输出文件
- `src/main/java/com/xuan/xuanopenagent/service/AgentService.java`
- `src/main/java/com/xuan/xuanopenagent/model/ChatRequest.java`
- `src/main/java/com/xuan/xuanopenagent/rag/RagRetrievalService.java`
- `src/main/java/com/xuan/xuanopenagent/rag/model/RetrievalHit.java`
- `frontend/src/components/RetrievalHitPanel.vue`
- `frontend/src/components/KnowledgeUploadPanel.vue`
- `frontend/src/services/knowledgeApi.ts`

## 5. 最小实现示例
```java
Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
  .documentRetriever(VectorStoreDocumentRetriever.builder()
    .vectorStore(vectorStore)
    .similarityThreshold(0.50)
    .topK(4)
    .build())
  .queryAugmenter(ContextualQueryAugmenter.builder()
    .allowEmptyContext(true)
    .build())
  .build();
```

## 6. 联调流程
1. 前端上传知识文件并等待 READY。
2. 前端发送聊天请求，携带 `useKnowledgeBase=true`。
3. 后端在 `AgentService` 挂载 RAG Advisor。
4. SSE 流中返回最终回答和命中片段摘要。
5. 前端展示回答与来源片段。

## 7. 验收标准
- `useKnowledgeBase=false` 时行为与旧链路一致。
- `useKnowledgeBase=true` 时回答可引用知识库内容。
- 命中片段可追溯到 `fileId` 与 `chunkIndex`。
- 检索为空时遵循 `advisorAllowEmptyContext` 行为。

## 8. 风险与处理
- 风险：阈值过高导致检索为空。
- 处理：先用 0.5 起步，日志观测后调参。
- 风险：阈值过低导致噪声上下文。
- 处理：结合 topK 和 metadata filter 控制召回范围。
