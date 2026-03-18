# 模块 03：工具注册、调用与过程观测

## 模块目标
- 为 Agent 接入第一批基础工具，并统一工具注册方式。
- 基于 Spring AI 的工具能力完成最小可用工具调用链路。
- 让每次工具调用和工具结果都能被结构化记录，支撑过程可观测。

## 强制约束
- 你必须使用 Spring AI 这个框架开发如果涉及 Agent 开发部分。
- ！！！！！！！！！！！！重要！！！！！！！！本项目当前阶段首先需要快速搭建核心功能，不需要太多完善的设计和功能，所以设计上先实现一个简单的 ReAct Agent 框架，支持工具调用和过程可观测。后续再迭代多模态输入、更多工具、复杂规划算法等能力。

## 本模块范围
- 实现工具类与工具注册配置。
- 实现最小工具集：时间工具、终止工具。
- 预留 web 搜索工具扩展位，但可先用桩实现或占位接口。
- 把工具调用事件和工具结果事件纳入统一事件模型。

## 建议输出文件
- `com.xuan.xuanopenagent.tools.TimeGetTool`
- `com.xuan.xuanopenagent.tools.TerminateTool`
- `com.xuan.xuanopenagent.tools.WebSearchTool` 或占位实现
- `com.xuan.xuanopenagent.tools.ToolRegistry`
- 如有需要：`com.xuan.xuanopenagent.agent.ToolCallAgent`

## 核心设计要求
- 优先使用 Spring AI 的 `@Tool`、`ToolCallback`、`ToolCallbacks.from(...)` 完成工具接入。
- `ToolRegistry` 统一管理 `ToolCallback[]`，不要在多个业务类中重复创建。
- 时间工具返回结构尽量稳定，便于模型消费。
- `TerminateTool` 输出结构要明确包含 `reason` 和 `finalAnswer`。
- 工具调用前后都需要生成 `AgentEvent`，事件类型至少包括：`tool_call`、`tool_result`、`error`。

## 推荐最小工具集
1. `TimeGetTool`：返回当前时间和时区。
2. `TerminateTool`：显式结束当前任务。
3. `WebSearchTool`：先定义接口和占位实现，后续再接真实搜索能力。

## 开发任务
1. 用 Spring AI 注解完成工具类定义。
2. 把工具统一组装为 `ToolCallback[]`。
3. 在 Agent 中接入工具调用流程。
4. 将工具调用写入 `ExecutionTrace` 和 `AgentEvent`。

## 非目标
- 不做复杂权限管理。
- 不做多工具并发调度。
- 不做高级重试编排，仅保留最基本错误处理口子。

## 完成标准
- Agent 已能调用基础工具。
- 工具调用过程可被结构化观测。
- 新工具后续可以通过统一注册机制快速接入。
- 代码结构可以进入模块 04 的接口和流式输出集成。

## 与后续模块的衔接
- 本模块完成后，进入模块 04，对外提供 SSE 接口和服务层封装。