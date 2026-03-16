# 模块 01：Spring AI Agent 基础骨架

## 模块目标
- 建立最小可运行的 Agent 项目骨架，为后续 ReAct 流程、工具调用和 SSE 观测打底。
- 完成 Spring AI 相关基础配置，确保后续所有 Agent 开发都基于 Spring AI 体系推进。
- 先把核心链路跑通，不在这一阶段做复杂抽象、持久化和高级规划能力。

## 强制约束
- 你必须使用 Spring AI 这个框架开发如果涉及 Agent 开发部分。
- ！！！！！！！！！！！！重要！！！！！！！！本项目当前阶段首先需要快速搭建核心功能，不需要太多完善的设计和功能，所以设计上先实现一个简单的 ReAct Agent 框架，支持工具调用和过程可观测。后续再迭代多模态输入、更多工具、复杂规划算法等能力。

## 本模块范围
- 补齐基础包结构。
- 定义 Agent 运行涉及的核心模型对象。
- 建立 Spring AI `ChatClient` 或 `ChatModel` 装配入口。
- 增加 Agent 运行参数配置，如 `maxSteps`、`maxToolCalls`、`timeout`。

## 建议输出文件
- `com.xuan.xuanopenagent.config.AiConfig`
- `com.xuan.xuanopenagent.config.AgentProperties`
- `com.xuan.xuanopenagent.agent.model.AgentContext`
- `com.xuan.xuanopenagent.agent.model.AgentEvent`
- `com.xuan.xuanopenagent.agent.model.AgentState`
- `com.xuan.xuanopenagent.agent.model.ExecutionTrace`

## 核心设计要求
- `AgentProperties` 至少包含：`maxSteps`、`maxToolCalls`、`toolTimeoutSeconds`、`modelName`。
- `AgentContext` 至少包含：`traceId`、`sessionId`、`userId`、`message`、`history`、`toolCallCount`、`currentStep`、`executionTraces`。
- `AgentEvent` 要能直接支撑 SSE 输出，字段建议包括：`traceId`、`sessionId`、`step`、`type`、`timestamp`、`payload`。
- `AgentState` 先按主文档中的状态实现，不额外扩展复杂状态。
- Spring AI 配置优先采用当前项目已接入的模型 starter，不引入第二套 Agent 框架。

## 开发任务
1. 建立 `config` 和 `agent.model` 下的基础类。
2. 把模型调用配置集中到 `AiConfig`，避免后续散落在业务类中。
3. 在 `application.yml` 中增加 Agent 基础配置项。
4. 明确一个统一的事件输出结构，供后续 SSE 直接复用。

## 非目标
- 不实现具体工具逻辑。
- 不实现完整对话接口。
- 不做数据库持久化。

## 完成标准
- 项目中已具备最小 Agent 配置骨架。
- Spring AI 模型调用入口已统一。
- Agent 基础上下文、状态、事件对象已齐备。
- 后续模块可以直接在此基础上实现 ReAct 循环与工具调用。

## 与后续模块的衔接
- 本模块完成后，进入模块 02，实现 `BaseAgent`、`ReActAgent` 和 `XuanAgent` 的主执行链路。