# Xuan Open Agent 项目总览（V2 架构版）
# 你必须使用spring ai这个框架开发如果涉及agent开发部分
！！！！！！！！！！！！重要！！！！！！！！我这个项目首先需要快速搭建核心功能不需要太多完善的设计和功能，所以在设计上我们会先做一个简单的ReAct Agent框架，支持工具调用和过程可观测。后续我们会在这个基础上迭代更多功能，比如多模态输入、更多工具、复杂规划算法等。
## 1. 项目目标与边界

### 1.1 项目目标
- 打造一个基于 Spring AI 的可自主规划 Agent 系统（类似 OpenManus 的规划-执行模式）。
- 支持前后端分离，前端实时展示 Agent 执行过程，后端提供稳定的对话与工具调用能力。
- 支持多轮会话、工具调用、过程可观测、可终止与可扩展。

### 1.2 一期边界
- 支持文本输入，不做语音/图片多模态。
- 工具先做少量通用工具（如 Web 搜索、时间、终止工具）。
- 模型供应商先接 1 家，保留可替换能力。

## 2. 总体架构（分层）

### 2.1 架构分层
- 前端层（Web UI）
  - 聊天输入输出、执行轨迹可视化、会话管理。
- API 网关层（Spring MVC Controller）
  - 接收请求、返回 SSE 流、鉴权与参数校验。
- Agent 应用层
  - 规划（Plan）/思考（Think）/执行（Act）/终止（Terminate）状态机。
- 工具层
  - 工具注册（我们使用spring ai 的toolcallback[]对象新建一个java文件来管理工具的注册）
- 模型适配层（Spring AI）
  - ChatClient、Prompt 模板、模型调用与响应标准化。
  - 
### 2.2 核心调用链
1. 前端提交用户输入（含会话 ID）到 `/api/chat/stream`。
2. 后端接受消息调用 Agent `run()` 方法，传入上下文和 SSE 事件发射器。
3. Agent 逐步输出事件（思考、工具调用、工具结果、最终回复）到 SSE。
4. 收到 `terminate` 或达到最大步数后结束流。

---

## 3. 后端模块设计

### 3.1 建议包结构
- `com.xuan.xuanopenagent.controller`
  - `ChatController`：SSE 对话接口
  - `HealthController`：健康检查
- `com.xuan.xuanopenagent.agent`
  - `BaseAgent`：Agent 抽象基类
  - `ReActAgent`：Think/Act 基础流程
  - `ToolCallAgent`：工具调用策略与约束
  - `XuanAgent`：最终业务 Agent实现 新建出Chatclient.builder来生成xuanAgent
- `com.xuan.xuanopenagent.tools`这个包用来放工具类
  - `webSearch`、`TimeGet`、`TerminateTool` 
- `com.xuan.xuanopenagent.service`
  - `AgentService`：Controller 与 Agent 解耦
- `com.xuan.xuanopenagent.config`
  - `AiConfig`、`SseConfig`、`AgentProperties`

### 3.2 核心类职责
- `BaseAgent`
  - 定义统一入口：`run(AgentContext context, Consumer<AgentEvent> emitter)`。
  - 定义步骤上限、超时与异常处理模板方法。
- `ReActAgent`
  - `think()`：根据上下文生成当前步骤意图。
  - `act()`：将意图转为工具调用或直接回答。
- `ToolCallAgent`
  - 维护 `maxToolCalls`、`maxSteps`、`retryPolicy`。
  - 调用 tools 包中的工具，并处理结果与异常。
- `XuanAgent`
  - 组装 Prompt、调用 Spring AI `ChatClient`。

### 3.3 Agent的状态 可以用一个enum枚举类来定义Agent的状态，便于管理和调试。建议状态如下：
- `INIT`：初始化上下文
- `ERROR`：agent出错
- `THINKING`：当前步骤思考
- `TOOL_DECISION`：决策是否调用工具
- `TOOL_RUNNING`：工具调用中
- `OBSERVING`：观察工具结果
- `RESPONDING`：生成回复中
- `TERMINATED`：正常结束
- `FAILED`：异常结束

状态流转示意：
`INIT -> PLANNING -> THINKING -> TOOL_DECISION -> (TOOL_RUNNING -> OBSERVING -> THINKING)* -> RESPONDING -> TERMINATED`

### 3.4 SSE 协议（建议统一事件）
事件名建议固定为 `agent-event`，`data` 为 JSON。

```json
{
  "traceId": "uuid",
  "sessionId": "s-001",
  "step": 3,
  "type": "tool_call",
  "timestamp": "2026-03-13T15:00:00Z",
  "payload": {
    "toolName": "web_search",
    "arguments": {"query": "Spring AI ReAct"}
  }
}
```

`type` 枚举建议：
- `plan`
- `thought`
- `tool_call`
- `tool_result`
- `message_delta`
- `final`
- `error`
- `done`

### 3.5 后端接口建议
- `POST /api/chat/stream`
  - 入参：`ChatRequest`
  - 出参：`text/event-stream`
- `GET /api/sessions/{sessionId}`
  - 返回会话摘要与最近执行轨迹
- `GET /api/tools`
  - 返回已注册工具列表

`ChatRequest` 示例：
```json
{
  "sessionId": "s-001",
  "message": "帮我规划一个两天杭州行程",
  "userId": "u-001",
  "options": {
    "maxSteps": 8,
    "temperature": 0.3
  }
}
```

---

## 4. 前端模块设计

### 4.1 页面与功能
- 聊天页（核心）
  - 输入框、消息流、发送/停止按钮
  - Agent 执行轨迹面板（思考、工具、结果）
- 会话页（可选一期）
  - 会话列表、重命名、删除、切换

### 4.2 前端状态管理
- `chatMessages`：用户与模型消息
- `agentEvents`：SSE 过程事件
- `currentSession`：当前会话信息
- `streamStatus`：`idle | streaming | error | done`

### 4.3 SSE 消费策略
- 使用 `EventSource` 或 `fetch + ReadableStream`。
- 事件按 `traceId + step + type` 去重。
- `done` 后自动关闭流。
- `error` 事件落地提示并支持重试。

### 4.4 执行链路可视化
- 时间轴视图：按 `step` 展示。
- 卡片类型：`thought`、`tool_call`、`tool_result`、`final`。
- 支持折叠中间过程，默认展示关键节点。

---

## 5. 数据与会话模型

### 5.1 关键对象
- `AgentContext`
  - `traceId`、`sessionId`、`userId`
  - `history`（对话历史）
  - `planSteps`（计划步骤）
  - `executionTraces`（执行日志）
  - `toolCallCount`、`tokenUsage`、`costEstimate`

- `ExecutionTrace`
  - `step`、`state`、`eventType`、`input`、`output`、`latencyMs`

### 5.2 持久化建议
- 先内存存储（Map + TTL）。


---

## 6. 工具系统设计

### 6.1 工具接口 
- 定义 `Tool` 接口，使用@Tools注解标记工具类，@ToolParam标记工具方法参数。
- 如下示例：
  class DateTimeTools {

  @Tool(description = "Get the current date and time in the user's timezone")
  String getCurrentDateTime() {
  return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
  }

}


### 6.2 工具注册与执行
- `ToolRegistry`：启动时注册。使用ToolCallback[]定义所有工具对象，传入ChatModel的默认选项中。
  - 下面是使用实例
  ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools());
    ChatModel chatModel = OllamaChatModel.builder()
    .ollamaApi(OllamaApi.builder().build())
    .defaultOptions(ToolCallingChatOptions.builder()
    .toolCallbacks(dateTimeTools)
    .build())
    .build();


### 6.3 `TerminateTool` 规范
- 用于通知 Agent 本轮已可结束。
- 触发后状态转 `TERMINATED`，并发出 `done` 事件。
- 输出结构示例：
```json
{
  "reason": "task_completed",
  "finalAnswer": "已完成你的任务，以下是总结..."
}
```

### 6.4 工具治理策略
- 单轮工具调用上限：`maxToolCalls`。
- 单工具超时：3min。
- 重试策略：仅对可重试错误进行 1-2 次重试。
- 幂等控制：对重复请求做去重键。

---



## 11. 当前版本结论

开发简单的 ReAct Agent 框架，支持工具调用与过程可观测，是实现自主规划 Agent 的关键第一步。通过明确分层架构、核心类职责与事件协议，我们为后续功能迭代（如多模态输入、更多工具、复杂规划算法）奠定了坚实基础。
所有的开发应优先使用spring ai框架提供的工具和接口，以确保与模型的无缝集成和未来的可扩展性。
本文件即作为下一阶段开发与拆任务的唯一架构基线。

