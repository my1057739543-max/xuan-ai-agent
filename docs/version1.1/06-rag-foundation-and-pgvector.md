# 模块 06：RAG 基础配置与 PgVector 存储骨架

你必须使用spring ai这个框架开发如果涉及agent开发部分

## 1. 模块目标
- 在现有 Spring Boot 工程中完成 RAG 基础配置落地。
- 接入 Spring AI 官方 `PgVectorStore`，打通本地 PostgreSQL + pgvector。
- 明确 1.1 的配置边界：业务参数放 `xuan.rag`，向量库底层参数放 `spring.ai.vectorstore.pgvector`。

## 2. 本模块范围
- 新增 `RagProperties`，承载 chunk、检索、上传约束参数。
- 新增 `VectorStoreConfig`（如有必要）和 `RagAdvisorConfig`。
- 配置并验证 `spring.ai.vectorstore.pgvector.initialize-schema`。
- 初始化上传目录。
- 提供健康检查或自检日志，确认向量库可写可查。

## 3. 关键技术约束
- 必须使用 Spring AI 官方 starter：
  - `spring-ai-starter-vector-store-pgvector`
  - `spring-ai-rag`
- 不允许在本模块主路径中手写 embedding HTTP 请求。
- 不允许以自定义向量表替代 Spring AI 默认 `vector_store` 作为主实现。

## 4. 建议输出文件
- `src/main/java/com/xuan/xuanopenagent/config/RagProperties.java`
- `src/main/java/com/xuan/xuanopenagent/config/VectorStoreConfig.java`
- `src/main/java/com/xuan/xuanopenagent/config/RagAdvisorConfig.java`
- `src/main/resources/application.yml`（补充 pgvector 配置）
- `src/main/resources/application-local.yml`（仅本地凭据）

## 5. 配置基线
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

## 6. 验收标准
- 应用启动后可看到 PgVectorStore 初始化成功日志。
- `vector_store` 可完成最小写入与相似度检索。
- 上传目录自动创建且可写。
- 配置项绑定测试通过。

## 7. 风险与处理
- 风险：`dimensions` 与 embedding 模型维度不一致导致失败。
- 处理：维度作为显式检查项，启动阶段进行告警。
- 风险：忘记开启 `initialize-schema`。
- 处理：启动时打印关键配置摘要。
