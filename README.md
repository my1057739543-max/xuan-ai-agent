# Xuan Open Agent

一个面向游戏技巧问答场景的 AI Agent 项目，基于 Spring Boot 3 + Spring AI + ReAct + RAG + Tool Calling + MCP，支持流式对话、知识库检索、多工具协同与可观测事件回放。

---

## 1. 项目能力

- ReAct 智能体：Thought -> Decision -> Tool Call -> Observation -> Final
- SSE 流式对话：后端连续推送执行事件与增量输出
- RAG 知识库：上传 txt/md/pdf，切片、向量化、检索增强回答
- LLM-first 切片：结构化切片 + 校验 + 规则回退
- gameKey 隔离检索：按游戏维度过滤知识，降低跨游戏污染
- 意图识别与查询重写：提升检索召回稳定性
- 会话摘要记忆：多轮对话上下文压缩与状态追踪
- 工具调用：Web Search、时间、终止工具、MCP 工具
- 前端一体化面板：聊天区、执行轨迹、知识库管理、检索命中展示

---

## 2. 技术栈

### 后端

- Java 21
- Spring Boot 3.5.x
- Spring AI 1.1.2
- DeepSeek Chat Model（主模型）
- DashScope（Qwen / Embedding）
- PostgreSQL + pgvector
- MCP Client（可选）

### 前端

- Vue 3 + TypeScript
- Vite 8
- SSE 事件流解析

---

## 3. 核心架构

1. 用户请求进入聊天接口
2. 可选执行 RAG 检索增强（意图识别 -> 查询重写 -> 检索）
3. 增强后的上下文进入 ReAct Agent
4. Agent 根据决策调用工具或直接回答
5. 全流程通过 SSE 事件回放给前端

主要模块：

- controller：聊天、知识库、健康检查、工具列表
- service：Agent 编排与对话流程
- agent：ReAct 执行循环与工具观察
- rag：文档读取、切分、入库、检索
- tools：可调用工具实现
- frontend：控制台式可视化交互页面

---

## 4. 快速开始

### 4.1 环境要求

- JDK 21
- Maven Wrapper（项目已内置 mvnw / mvnw.cmd）
- Node.js 18+
- PostgreSQL 14+（推荐 15/16）
- pgvector 扩展

### 4.2 数据库准备

创建数据库（示例）：

```sql
CREATE DATABASE xuan_open_agent;
\c xuan_open_agent;
CREATE EXTENSION IF NOT EXISTS vector;
```

说明：

- vector_store 表由 Spring AI PgVector 自动初始化（配置开启）
- kb_file 表由应用启动时自动创建

### 4.3 本地配置

在 src/main/resources/application-local.yml 中填写：

- spring.ai.deepseek.api-key
- spring.ai.dashscope.api-key
- xuan.tools.web-search.api-key（可选）
- MCP 相关 key（可选）

默认端口：8123

### 4.4 启动后端

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Mac/Linux:

```bash
./mvnw spring-boot:run
```

### 4.5 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端开发端口一般为 5173（以 Vite 输出为准）。

---

## 5. 配置说明（重点）

### 聊天与 Agent

- xuan.agent.max-steps：最大推理步数
- xuan.agent.max-tool-calls：最大工具调用次数
- xuan.agent.tool-timeout-seconds：本轮执行超时
- xuan.agent.system-prompt：系统提示词

### RAG

- xuan.rag.top-k：召回条数（当前默认 4）
- xuan.rag.similarity-threshold：相似度阈值
- xuan.rag.embedding-batch-size：向量入库批次大小
- xuan.rag.chunk-strategy：llm_first 或 rule_only
- xuan.rag.game-isolation-enabled：是否按 gameKey 隔离

### 模型分工

- DeepSeek：主对话模型
- DashScope Qwen：切片模型、部分降级场景
- DashScope Embedding：向量化

---

## 6. 主要接口

### 聊天

- POST /api/chat/stream
- Content-Type: application/json
- 返回：text/event-stream

请求示例：

```json
{
	"sessionId": "s-001",
	"userId": "u-001",
	"message": "jett 怎么练急停后一枪",
	"options": {
		"useKnowledgeBase": true,
		"fileIdFilter": "",
		"gameKey": "valorant"
	}
}
```

### 知识库上传

- POST /api/knowledge/upload
- multipart/form-data
- 参数：file, gameKey, tags(可选), customGameNames(可选)

### 批量上传

- POST /api/knowledge/upload/batch
- multipart/form-data
- 参数：files[], gameKey, tags(可选), customGameNames(可选)

### 文件管理

- GET /api/knowledge/files
- DELETE /api/knowledge/files/{fileId}

### 其他

- GET /api/tools
- GET /health

---

## 7. 前端交互说明

前端页面包含：

- 聊天记录
- 执行轨迹（thought/tool_call/tool_result/final/done）
- 知识库上传与文件管理
- 检索命中列表

上传知识时支持：

- 自定义 gameKey 输入
- 自定义别名 customGameNames（例如：绣湖, rusty lake）

别名用于识别映射，实际检索过滤依然基于标准 gameKey。

---

## 8. 事件可观测

流式事件中可见：

- plan
- thought
- tool_call
- tool_result
- retrieval
- intent_detected
- query_rewritten
- memory_updated
- final
- done
- error

用于排障和推理过程可视化。

---

## 9. 常见问题

### Q1: 前端有回复，但后端出现 DeepSeek 解析异常日志

典型日志：

Error while extracting response for type DeepSeekApi$ChatCompletion

这通常是上游链路抖动（如 HTTP/2 RST_STREAM）导致主模型响应解析失败，系统可能自动走备用模型继续回答。

建议：

- 观察频率（偶发可接受，频发需治理）
- 检查 API key 余额/限流
- 必要时增加重试、降级、熔断策略

### Q2: Agent 工具调用循环

项目已加入重复工具结果的 loop guard 提示机制，避免过早终止同时降低死循环概率。建议结合 maxSteps 与 maxToolCalls 控制上限。

### Q3: 上传后检索不到

排查顺序：

1. 是否开启 useKnowledgeBase
2. gameKey 是否一致
3. fileIdFilter 是否误选
4. 向量库是否入库成功（查看 kb_file 状态）

---

## 10. 开发与构建

后端编译：

```powershell
.\mvnw.cmd -q -DskipTests test-compile
```

前端构建：

```bash
cd frontend
npm run build
```

---

## 11. 路线建议

- 增加模型降级事件（model_fallback）用于前端可观测
- 引入更细粒度的工具调度策略（按意图限制工具）
- 增加离线评测集（检索命中率、答案相关度）
- 将别名映射与会话摘要持久化到数据库

---

## 12. License

See LICENSE.
