# Xuan Open Agent 项目规划 1.1：基础 RAG 版本

# 你必须使用spring ai这个框架开发如果涉及agent开发部分
！！！！！！！！！！！！重要！！！！！！！！本版本目标是尽快落地一个最小可运行的 RAG 能力，不追求复杂检索策略，不引入多路召回、混合召回、重排序、知识图谱等增强能力。先把“上传文件 -> 切片 -> 向量化 -> 本地向量库存储 -> 检索增强问答”这一条主链路稳定打通。

## 1. 版本目标

### 1.1 本版本要解决的问题
- 允许前端上传文件作为知识库原始资料。
- 后端对上传文件进行基础解析与切片。
- 使用 Spring AI 的 Embedding 能力将文本切片转为向量。
- 使用本地向量数据库存储文档片段和向量。
- 在用户提问时，先进行相似度检索，再把召回内容拼接到 Prompt 中完成基础 RAG 问答。

### 1.2 本版本的核心交付
- 一个最小可用的知识库上传与构建能力。
- 一个最小可用的本地向量检索能力。
- 一个基于已有 Agent/Chat 链路的基础 RAG 回答能力。
- 一个前后端可联调、可演示的知识库问答流程。

### 1.3 本版本不做的内容
- 不做多路召回。
- 不做召回结果重排序。
- 不做复杂 chunk overlap 策略搜索优化。
- 不做复杂文档解析编排，如 OCR、表格结构还原、图片理解。
- 不做权限体系、租户隔离、文档审核流。
- 不做高可用分布式向量数据库部署。

---

## 2. 版本边界与技术路线

### 2.1 总体路线
- 继续基于 Spring AI 进行模型、Embedding、VectorStore 相关开发。
- 后端保留当前 Spring Boot + Spring AI 主链路。
- 前端在现有聊天界面基础上增加文件上传与知识库状态展示。
- 向量库采用 Spring AI 官方支持的 `PgVectorStore` 自动配置方案，优先走官方 starter 和标准抽象，减少自定义实现。

### 2.2 推荐最小技术选型
- 模型框架：Spring AI
- Embedding：Spring AI `EmbeddingModel`
- 文档读取与 ETL：Spring AI `DocumentReader` / `DocumentTransformer` / `DocumentWriter`
- 文档切片：Spring AI `TokenTextSplitter`
- 向量存储：本地 `PostgreSQL + pgvector`，代码层依赖 Spring AI `VectorStore` / `VectorStoreRetriever` 抽象
- RAG 编排：优先采用 Spring AI 官方 `RetrievalAugmentationAdvisor`
- 文件存储：本地磁盘目录
- 文件元数据存储：业务侧自建最小 `kb_file` 表
- chunk 元数据存储：优先放入 Spring AI `Document.metadata`，由 `PgVectorStore` 持久化到 `vector_store.metadata`

### 2.2.1 需要明确采用的 Spring AI 官方能力
- `spring-ai-starter-vector-store-pgvector`：接入 `PgVectorStore`
- `spring-ai-rag`：提供 `RetrievalAugmentationAdvisor`
- `spring-ai-markdown-document-reader`：读取 markdown 文档
- `spring-ai-pdf-document-reader`：读取 pdf 文档
- `EmbeddingModel`：由 Spring AI 模型 starter 提供
- `TokenTextSplitter.builder()`：按 token 进行可配置切片

说明：
- 1.1 版本不建议自己实现一套独立的 chunk 向量表写入逻辑作为主方案。
- 官方推荐路径是：把文件解析为 Spring AI `Document`，给 `Document` 挂上 metadata，然后调用 `VectorStore.add(documents)` 完成 embedding 与向量入库。

### 2.3 本版本对“本地向量数据库”的定义
- 1.1 版本目标是先做“本地可运行、可持久化”的向量数据库能力。
- 默认方案不采用纯内存存储，也不采用进程退出即丢失数据的轻量实现。
- 默认方案采用本机部署的 `PostgreSQL + pgvector`，既满足本地开发，又能保留上传后的知识库数据。
- 业务代码层仍统一依赖 Spring AI `VectorStore` / `VectorStoreRetriever` 抽象，避免后续切换底层实现时影响业务逻辑。
- `PgVectorStore` 的 schema 初始化不是默认开启，1.1 版本需要显式配置 `spring.ai.vectorstore.pgvector.initialize-schema=true` 或自行建表。

说明：
- 如果你要的是“本地运行、本地存储、服务重启后数据仍在”的向量数据库体验，那么 `PostgreSQL + pgvector` 更符合 1.1 的实际需求。
- 如果你要的是“完全从零自己实现一个向量数据库产品”，那会显著偏离 1.1 版本快速交付目标，不建议现在做。

---

## 3. 功能范围

### 3.1 前端功能
- 新增文件上传入口。
- 支持用户选择本地文件并上传到后端。
- 支持查看当前上传任务状态：待上传、处理中、成功、失败。
- 支持在聊天时指定是否启用知识库问答。
- 支持展示当前回答是否命中了知识库，以及命中的片段摘要。

### 3.2 后端功能
- 接收上传文件并保存原始文件。
- 使用 Spring AI `DocumentReader` 体系完成基础解析。
- 使用 `TokenTextSplitter` 对文档进行切片。
- 将切片封装为带 metadata 的 `Document`。
- 调用 `VectorStore.add(...)` 写入本地向量库，由底层 `EmbeddingModel` 完成向量化。
- 在问答阶段进行相似度检索。
- 将召回结果通过 Spring AI Advisor 注入问答过程，生成最终回答。

### 3.3 文档范围
1. txt
2. md
3. pdf

说明：
- 1.1 版本建议先优先支持 `txt` 和 `md`。
- `pdf` 可以作为扩展支持，但要接受解析质量不稳定这一现实。
- doc/docx 如果当前没有明确刚需，建议延后到 1.2。

---

## 4. 目标架构

### 4.1 新增分层
- 文件接入层
  - 负责上传、校验、保存原始文件。
- 文档处理层
  - 负责基于 Spring AI Reader/Transformer 解析文本、切片、清洗。
- 向量构建层
  - 负责把业务文件转换成 Spring AI `Document` 集合并交给 `VectorStore` 入库。
- 向量检索层
  - 负责基于 `VectorStoreRetriever` / `VectorStoreDocumentRetriever` 做相似度检索。
- RAG 应用层
  - 负责通过 `RetrievalAugmentationAdvisor` 将检索结果注入问答链路。

### 4.2 核心调用链
1. 前端上传文件到后端。
2. 后端保存文件并创建文档处理任务。
3. 后端使用 Spring AI `DocumentReader` 把文件解析为 `Document`。
4. 后端使用 `TokenTextSplitter` 把文档切成多个 chunk document。
5. 后端为每个 chunk document 补充 `fileId`、`fileName`、`chunkIndex` 等 metadata。
6. 后端调用 `VectorStore.add(documents)`，由 Spring AI 底层自动完成 embedding 和向量持久化。
7. 用户发起问题时，`AgentService` 根据开关决定是否启用 `RetrievalAugmentationAdvisor`。
8. `VectorStoreDocumentRetriever` 以 `topK`、`similarityThreshold` 和 metadata filter 检索相关片段。
9. `ContextualQueryAugmenter` 把召回内容注入问答上下文。
10. 模型生成基于知识库的回答。

---

## 5. 建议包结构

- `com.xuan.xuanopenagent.rag`
  - `RagService`
  - `RagIngestionService`
  - `RagRetrievalService`
- `com.xuan.xuanopenagent.rag.document`
  - `DocumentChunker`
- `com.xuan.xuanopenagent.rag.store`
  - `VectorStoreManager`
  - `KnowledgeFileRepository`
- `com.xuan.xuanopenagent.rag.model`
  - `KnowledgeFile`
  - `RetrievalHit`
  - `RagQuery`
  - `RagAnswer`
- `com.xuan.xuanopenagent.controller`
  - `KnowledgeBaseController`
- `com.xuan.xuanopenagent.service`
  - 在已有 `AgentService` 基础上增加 RAG 问答路由逻辑
- `com.xuan.xuanopenagent.config`
  - `RagProperties`
  - `VectorStoreConfig`
  - `RagAdvisorConfig`

说明：
- 1.1 版本不建议再抽象自定义 `DocumentParser` 体系作为主实现。
- `txt`、`md`、`pdf` 优先直接适配 Spring AI 已有 Reader，业务层只做少量封装与路由。

---

## 6. 核心模块设计

### 6.1 文件上传模块

职责：
- 接收前端上传文件。
- 校验文件大小、扩展名、MIME 类型。
- 生成文件 ID 和知识库条目。
- 将文件保存到本地目录。

建议接口：
- `POST /api/knowledge/upload`
- `GET /api/knowledge/files`
- `DELETE /api/knowledge/files/{fileId}`

建议上传约束：
- 单文件大小先限制在 10MB 或 20MB。
- 一次只处理单文件上传，批量上传放后续版本。
- 文件名与物理文件名解耦，避免覆盖。

### 6.2 文档解析模块

职责：
- 根据文件类型选择解析器。
- 把不同格式统一转成纯文本。
- 做基础清洗，如去掉多余空行、控制字符。

建议策略：
- `txt`：优先使用 Spring AI `TextReader`。
- `md`：优先使用 Spring AI `MarkdownDocumentReader`，保留标题和基础结构信息。
- `pdf`：优先使用 Spring AI `PagePdfDocumentReader`，1.1 只接受“可提取文本”的 PDF。

实现原则：
- 业务层只负责根据扩展名选择 Reader，不重复造 Reader 轮子。
- Reader 输出统一转为 Spring AI `Document` 列表，便于后续切片与入库。
- 如果 pdf 目录结构明确，后续可再评估 `ParagraphPdfDocumentReader`，但 1.1 不作为默认方案。

### 6.3 切片模块

职责：
- 将长文本切分为适合 embedding 和召回的块。

推荐最小策略：
- 使用 Spring AI `TokenTextSplitter.builder()`。
- chunk size：先按 token 维度控制，默认从 800 起步。
- `minChunkSizeChars` 可从 350 起步。
- `minChunkLengthToEmbed` 保持较小值，避免碎片过多。
- 中文内容建议显式补充中文标点符号到 `punctuationMarks`。

设计原则：
- 先使用简单稳定策略，不做语义边界优化。
- 每个 chunk 保留来源文件 ID、chunk index、原始标题等元数据。
- 使用 builder 配置，而不是手写字符截断逻辑。
- 小文本如果本身未超过 chunk size，直接保持单 chunk，不做过度切分。

### 6.4 向量化模块

职责：
- 让 Spring AI 的 `VectorStore` 在写入时自动调用 `EmbeddingModel` 完成向量化。

要求：
- 不要在业务层手动拼 embedding HTTP 请求，统一走 Spring AI 抽象。
- 主入库路径优先使用 `VectorStore.add(List<Document>)`，不要自己先算 `float[]` 再手工写库。
- embedding 失败时要记录失败文件和失败原因。
- 允许后续重建索引。

补充说明：
- `EmbeddingModel` 仍然是核心依赖，但 1.1 主链路不建议在业务代码里显式循环调用 `embed(String)`。
- `PgVectorStore` 会基于注入的 `EmbeddingModel` 处理 `Document` 的内容并持久化。
- 如果后续要做诊断、维度校验、离线预检，再单独直接注入 `EmbeddingModel`。

### 6.5 向量存储模块

职责：
- 管理本地向量库实例。
- 写入 Spring AI `Document` 内容与 metadata。
- 提供基础相似度搜索能力。

推荐实现：
- 使用 Spring AI 官方 `PgVectorStore` 作为默认本地向量数据库实现。
- 代码层统一依赖 Spring AI `VectorStore` / `VectorStoreRetriever` 接口，不把业务直接耦合到某一个具体实现类。
- 元数据至少包含：
  - `fileId`
  - `fileName`
  - `chunkIndex`
  - `sourceType`
  - `uploadTime`
  - `knowledgeBaseEnabled` 或未来的分组标记字段

最小落地要求：
- 数据能持久化保存。
- 能按文档片段写入 `Document` 并由底层自动生成向量。
- 能执行基础相似度检索。
- 不追求复杂索引优化、分区、冷热分层等能力。
- 需要显式配置 schema 初始化或手工初始化，不能假设 Spring AI 默认帮你建表。

### 6.5.1 最小表设计

1.1 版本不追求复杂库表，优先遵循 Spring AI `PgVectorStore` 默认表结构，再加一张业务文件表即可跑通。

表一：`kb_file`

作用：
- 保存上传文件的基础信息。

建议字段：
- `id` bigint primary key generated always as identity
- `file_id` varchar(64) not null unique
- `file_name` varchar(255) not null
- `content_type` varchar(100)
- `status` varchar(32) not null
- `chunk_count` int default 0
- `created_at` timestamp not null default current_timestamp

表二：`vector_store`

作用：
- 由 Spring AI `PgVectorStore` 管理，保存切片内容、metadata 和 embedding。

建议字段：
- `id` uuid primary key
- `content` text
- `metadata` json
- `embedding` vector(1536)
- 以及 Spring AI 自动管理所需的基础结构

建议索引：
- `create index on vector_store using hnsw (embedding vector_cosine_ops);`

最小 SQL 示例：

```sql
create extension if not exists vector;
create extension if not exists hstore;
create extension if not exists "uuid-ossp";

create table if not exists kb_file (
  id bigint generated always as identity primary key,
  file_id varchar(64) not null unique,
  file_name varchar(255) not null,
  content_type varchar(100),
  status varchar(32) not null,
  chunk_count int default 0,
  created_at timestamp not null default current_timestamp
);

create table if not exists vector_store (
  id uuid default uuid_generate_v4() primary key,
  content text,
  metadata json,
  embedding vector(1536)
);

create index if not exists vector_store_embedding_hnsw_idx
  on vector_store using hnsw (embedding vector_cosine_ops);
```

说明：
- `vector(1536)` 只是示例维度，最终要以你选用的 Embedding 模型输出维度为准。
- 如果配置了 `spring.ai.vectorstore.pgvector.dimensions`，需要与实际 embedding 维度一致；如果不显式配置，可优先依赖模型维度推断。
- 如果启用 `spring.ai.vectorstore.pgvector.initialize-schema=true`，Spring AI 会尝试自动初始化 `vector_store` 及扩展依赖，1.1 版本应尽量复用这个能力。
- 如果采用 Spring AI 自动初始化，就不要再手工维护一份与官方默认结构不一致的 `vector_store` DDL。
- 业务维度的 `fileId`、`chunkIndex` 等信息优先存进 `metadata`，查询和删除时通过 metadata filter 完成。

### 6.6 检索问答模块

职责：
- 将用户问题交给 RAG Advisor 链路。
- 检索最相关的若干 chunk。
- 使用 Spring AI 官方 QueryAugmenter 将上下文注入问答过程。
- 生成基于知识库的最终回答。

推荐最小检索策略：
- TopK = 3 或 5
- similarityThreshold = 0.5 左右起步，后续再调优
- 召回内容按得分顺序直接进入增强上下文，不做重排序
- 文件删除、单知识库范围查询优先通过 metadata filter 控制，而不是自己拼 SQL。

推荐最小实现方式：

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

String answer = chatClient.prompt()
  .advisors(retrievalAugmentationAdvisor)
  .user(question)
  .call()
  .content();
```

说明：
- 这条链路比手动“检索后拼 Prompt”更贴近 Spring AI 官方 RAG 实践。
- 1.1 版本建议优先采用这种官方 Advisor 方案。
- 如果后续需要按 `fileId` 或知识库范围过滤，可以通过 `VectorStoreDocumentRetriever.FILTER_EXPRESSION` 在请求级动态传入 filter。

---

## 7. 与现有 Agent 体系的关系

### 7.1 1.1 版本建议采用的接入方式
- 不要一开始把 RAG 做成特别复杂的 Agent Tool 编排。
- 直接在 `AgentService` 前置 RAG 检索。
- 在问答主链路中加一个“是否启用知识库检索”的判断。
- 如果用户开启知识库模式，则在 `AgentService` 中先构建 `RetrievalAugmentationAdvisor`，再进入后续模型调用流程。
- 检索服务优先依赖 `VectorStoreRetriever` 或 `VectorStoreDocumentRetriever`，只暴露读能力，避免把写操作暴露给纯查询路径。

### 7.2 推荐两种接入方案

方案 A：在 `AgentService` 前置 RAG 检索
- Controller 接到聊天请求。
- 先判断是否启用 RAG。
- 如果启用，则在 `AgentService` 中构建 `RetrievalAugmentationAdvisor`。
- 然后继续走统一的聊天调用链路，完成基础 RAG 回答。

方案 B：新增独立 RAG Chat Service
- 普通 Agent 对话走原链路。
- 知识库问答走 `RagChatService`。
- `RagChatService` 内部使用 `ChatClient + RetrievalAugmentationAdvisor`。
- 后续再考虑把 RAG 合并进 Agent Tool 能力。

1.1 版本当前确定采用方案 A，也就是在 `AgentService` 前置 RAG 检索。这样可以复用当前聊天主链路，减少额外服务拆分，先优先把功能跑通。

---

## 8. 后端接口建议

### 8.1 文件上传接口

`POST /api/knowledge/upload`

返回示例：

```json
{
  "fileId": "kb-file-001",
  "fileName": "spring-ai-rag.md",
  "status": "PROCESSING"
}
```

### 8.2 文件列表接口

`GET /api/knowledge/files`

返回示例：

```json
[
  {
    "fileId": "kb-file-001",
    "fileName": "spring-ai-rag.md",
    "status": "READY",
    "chunkCount": 26
  }
]
```

### 8.3 RAG 问答接口

可以有两种设计：

方案 1：复用已有 `/api/chat/stream`

```json
{
  "sessionId": "s-001",
  "userId": "u-001",
  "message": "总结一下知识库中关于 Spring AI RAG 的内容",
  "options": {
    "useKnowledgeBase": true
  }
}
```

方案 2：新增独立接口 `/api/rag/chat/stream`

1.1 版本当前建议优先方案 1，也就是继续复用已有 `/api/chat/stream`，只在请求参数中增加 `useKnowledgeBase` 之类的开关，与第 7 章确定的 `AgentService` 前置接入方式保持一致。

### 8.4 检索结果展示接口

`GET /api/rag/retrieval/{traceId}`

说明：
- 可选实现。
- 用于前端展示“本次回答命中了哪些片段”。
- 如果时间有限，可先把命中片段直接放到 SSE `payload` 中。

---

## 9. 前端设计建议

### 9.1 页面能力
- 在现有聊天界面增加“知识库上传区”。
- 增加知识库文件列表。
- 增加“启用知识库回答”开关。
- 增加回答命中来源展示区。

### 9.2 前端最小页面结构
- 知识库上传卡片
  - 上传按钮
  - 上传状态
  - 文件列表
- 聊天卡片
  - 输入框
  - 是否启用知识库开关
  - 流式回答区
- 检索命中卡片
  - 命中文件名
  - 命中 chunk 摘要

### 9.3 前端最小交互
1. 用户上传文件。
2. 前端展示文件处理中状态。
3. 处理完成后展示“已入库”。
4. 用户打开知识库问答开关。
5. 用户发送问题。
6. 前端展示回答和命中片段。

---

## 10. 配置设计

建议新增 `RagProperties`：

- `uploadDir`：文件上传目录
- `topK`：检索数量
- `similarityThreshold`：相似度阈值
- `chunkSize`：切片长度
- `minChunkSizeChars`：最小切片字符数
- `minChunkLengthToEmbed`：最短可入库 chunk 长度
- `advisorAllowEmptyContext`：无命中时是否允许继续回答
- `maxFileSizeMb`：最大上传文件大小
- `enabledFileTypes`：允许的扩展名
- `initializeSchema`：是否启用 PgVector schema 自动初始化
- `vectorTableName`：向量表名，默认建议沿用 `vector_store`
- `schemaName`：schema 名称

建议 `application.yml` 示例：

```yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/xuan_open_agent
    username: postgres
    password: postgres
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true
        schema-name: public
        table-name: vector_store
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
        max-document-batch-size: 10000

xuan:
  rag:
    upload-dir: ./data/uploads
    chunk-size: 800
    min-chunk-size-chars: 350
    min-chunk-length-to-embed: 10
    top-k: 4
    similarity-threshold: 0.5
    advisor-allow-empty-context: true
    max-file-size-mb: 20
    enabled-file-types:
      - txt
      - md
      - pdf
```

说明：
- `initialize-schema` 在 Spring AI 新版本里需要显式开启，不能依赖旧版本默认行为。
- `dimensions` 如果显式配置，必须与当前 Embedding 模型输出维度一致，否则表结构会错。
- 检索相关参数优先放在 `xuan.rag`，PgVector 的底层存储参数放在 `spring.ai.vectorstore.pgvector`。

---

## 11. 建议输出文件

### 11.1 配置层
- `com.xuan.xuanopenagent.config.RagProperties`
- `com.xuan.xuanopenagent.config.VectorStoreConfig`
- `com.xuan.xuanopenagent.config.RagAdvisorConfig`
- 如确有必要再补充 `com.xuan.xuanopenagent.config.PgVectorSchemaInitializer`，但默认优先使用 Spring AI `initialize-schema`

### 11.2 模型层
- `com.xuan.xuanopenagent.rag.model.KnowledgeFile`
- `com.xuan.xuanopenagent.rag.model.RetrievalHit`
- `com.xuan.xuanopenagent.rag.model.RagAnswer`

### 11.3 服务层
- `com.xuan.xuanopenagent.rag.RagIngestionService`
- `com.xuan.xuanopenagent.rag.RagRetrievalService`
- `com.xuan.xuanopenagent.rag.RagService`
- 在现有 `com.xuan.xuanopenagent.service.AgentService` 中增加 RAG 前置编排逻辑

### 11.4 文档处理层
- `com.xuan.xuanopenagent.rag.document.DocumentChunker`

### 11.4.1 需要补充的 Maven 依赖
- `org.springframework.ai:spring-ai-rag`
- `org.springframework.ai:spring-ai-starter-vector-store-pgvector`
- `org.springframework.ai:spring-ai-markdown-document-reader`
- `org.springframework.ai:spring-ai-pdf-document-reader`
- 以及与你当前模型供应商匹配的 Spring AI model starter

### 11.5 控制器层
- `com.xuan.xuanopenagent.controller.KnowledgeBaseController`
- 继续复用现有聊天接口控制器，必要时只扩展请求参数

### 11.6 前端文件
- `frontend/src/components/KnowledgeUploadPanel.vue`
- `frontend/src/components/KnowledgeFileList.vue`
- `frontend/src/components/RetrievalHitPanel.vue`
- `frontend/src/services/knowledgeApi.ts`
- `frontend/src/types/knowledge.ts`

---

## 12. 开发任务拆分建议

### 模块 A：RAG 配置与本地存储骨架
- 增加 `RagProperties`
- 增加 `VectorStoreConfig`
- 连接本地 PostgreSQL
- 接入 `spring-ai-starter-vector-store-pgvector`
- 显式配置 `initialize-schema`
- 建立上传目录

### 模块 B：文件上传与解析
- 完成上传接口
- 完成文件保存
- 基于 Spring AI Reader 完成 `txt/md/pdf` 基础解析
- 增加文件状态管理

### 模块 C：切片与向量化入库
- 实现 `DocumentChunker`
- 基于 `TokenTextSplitter.builder()` 完成切片
- 实现 chunk 元数据封装
- 写入 VectorStore

### 模块 D：检索增强问答
- 在 `AgentService` 前置执行 RAG 检索
- 构建 `RetrievalAugmentationAdvisor`
- 增加 metadata filter 能力
- 输出最终回答
- 返回命中片段信息

### 模块 E：前端知识库页面与联调
- 完成上传区
- 完成文件列表
- 完成命中片段展示
- 完成聊天页知识库模式联调

---

## 13. 验收标准

### 13.1 功能验收
- 用户可以上传文本类文件。
- 文件可以被成功解析和切片。
- 切片可以被成功向量化并写入本地向量库。
- 用户提问时系统可以从知识库召回相关片段。
- 模型回答能够明显利用召回内容，而不是完全脱离知识库。

### 13.2 工程验收
- 所有 RAG 相关能力仍基于 Spring AI 进行集成。
- 文档 ingestion 主链路基于 Spring AI Reader/Splitter/VectorStore，而不是手写 embedding 与向量 SQL。
- 配置清晰，支持本地快速启动。
- 前后端链路可联调。
- 日志中能看到上传、切片、入库、检索等关键过程。

### 13.3 演示验收
- 上传一个 markdown 文档。
- 文档处理完成。
- 提一个文档中可回答的问题。
- 前端能看到回答内容和知识命中来源。

---

## 14. 风险与注意点

### 14.1 PDF 解析质量风险
- 某些 PDF 是扫描件或版式复杂文档，1.1 版本不保证解析质量。

### 14.2 本地向量库存储能力有限
- 适合小规模演示与开发验证。
- 如果文档量明显上升，后续应继续在 Spring AI `VectorStore` 抽象下切换更正式的向量数据库实现。

### 14.3 Embedding 成本与耗时
- 上传大文件时向量化会变慢。
- `VectorStore.add(...)` 底层仍会触发 embedding 调用，1.1 版本建议先同步处理，必要时后续再改成异步任务。

### 14.4 Prompt 注入与幻觉风险
- 即使有检索，模型仍可能补充知识库之外的内容。
- 1.1 版本应在 Prompt 中明确要求“优先基于召回内容回答”。

### 14.5 Spring AI 官方实现边界
- `PgVectorStore` 的 schema 初始化需要显式开启，不能遗漏。
- embedding 维度必须和所选模型匹配，否则 `vector_store` 表会与模型不兼容。
- metadata filter 是后续按文件、按知识库隔离检索和删除的关键能力，1.1 就要在数据模型里预留。
- 如果查询改写、查询扩展后续要启用，Spring AI 官方建议相关 `ChatClient.Builder` 使用低温度，例如 `0.0`，但 1.1 版本暂不默认开启这些高级 pre-retrieval 模块。

---

## 15. 当前版本结论

`project1.1` 的目标不是做一个完整成熟的企业级 RAG 平台，而是基于现有 Xuan Open Agent 工程，快速落地一个可运行、可验证、可演示的基础 RAG 版本。只要我们把文件上传、文本切片、向量化、本地向量存储和检索增强回答这条链路跑通，后续就可以在此基础上继续迭代更复杂的召回、重排序、混合检索和知识治理能力。

本文件作为 1.1 版本 RAG 开发阶段的总体规划基线。