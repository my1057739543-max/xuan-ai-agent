# 模块 02：ReAct Agent 核心执行链路

## 模块目标
- 基于 Spring AI 实现一个最小可运行的 ReAct Agent 主循环。
- 支持设置最大的思考步数 `maxSteps`，并在达到上限后安全终止。
- 将“思考、决策、执行、观察、回复、终止”流程显式化，方便后续观测和扩展。

## 强制约束
- 你必须使用 Spring AI 这个框架开发如果涉及 Agent 开发部分。
- ！！！！！！！！！！！！重要！！！！！！！！本项目当前阶段首先需要快速搭建核心功能，不需要太多完善的设计和功能，所以设计上先实现一个简单的 ReAct Agent 框架，支持工具调用和过程可观测。后续再迭代多模态输入、更多工具、复杂规划算法等能力。

## 本模块范围
- 实现 Agent 抽象基类和主循环控制。
- 实现最简版 ReAct 状态流转。
- 通过 Spring AI `ChatClient` 驱动模型思考与输出。
- 支持达到最大步数后给出兜底回复或终止事件。

## 建议输出文件
- `com.xuan.xuanopenagent.agent.BaseAgent`
- `com.xuan.xuanopenagent.agent.ReActAgent`
- `com.xuan.xuanopenagent.agent.XuanAgent`

## 核心设计要求
- `BaseAgent` 定义统一入口：`run(AgentContext context, Consumer<AgentEvent> emitter)`。
- `BaseAgent` 负责通用异常处理、超时控制、步数控制。
- `ReActAgent` 负责步骤循环，至少覆盖以下状态：`INIT`、`THINKING`、`TOOL_DECISION`、`OBSERVING`、`RESPONDING`、`TERMINATED`、`FAILED`。
- `XuanAgent` 负责拼装 prompt 和调用 Spring AI `ChatClient`。
- 当前阶段不追求“真正复杂规划器”，而是做一个可稳定循环的简单 ReAct 框架。

## 推荐实现策略
1. 每轮先生成 thought 或 decision。
2. 判断是否需要工具调用。
3. 如果不需要工具，则直接进入最终回复。
4. 如果需要工具，则把工具结果写回上下文，再进入下一轮思考。
5. 超过 `maxSteps` 后强制结束，并输出终止原因。

## Prompt 约束建议
- 明确告知模型：可在回答、调用工具、结束任务三者中选一。
- 明确告知模型：禁止无限循环，必须在有限步内完成任务。
- 明确告知模型：优先使用已注册工具，不要虚构工具结果。

## 开发任务
1. 抽象 Agent 运行模板。
2. 建立基于状态的 ReAct 循环。
3. 让 `maxSteps` 真正参与终止判定。
4. 为后续工具调用预留扩展点。

## 非目标
- 不实现完整工具注册中心。
- 不实现前端展示。
- 不追求复杂计划拆分算法。

## 完成标准
- Agent 能接收上下文并进入思考循环。
- `maxSteps` 生效，循环不会失控。
- 最终可以输出最终回答或终止原因。
- 代码结构能直接承接模块 03 的工具接入。

## 与后续模块的衔接
- 本模块完成后，进入模块 03，把工具接入 Spring AI ToolCallback 体系并纳入 ReAct 流程。