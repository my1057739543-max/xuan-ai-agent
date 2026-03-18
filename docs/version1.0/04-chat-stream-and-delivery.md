# 模块 04：SSE 接口、服务封装与阶段验收

## 模块目标
- 将前 3 个模块的能力通过后端接口暴露出来。
- 支持前端通过 SSE 看到 Agent 的执行过程和最终结果。
- 完成第一阶段最小可演示版本，确保后续可以边跑边迭代。

## 强制约束
- 你必须使用 Spring AI 这个框架开发如果涉及 Agent 开发部分。
- ！！！！！！！！！！！！重要！！！！！！！！本项目当前阶段首先需要快速搭建核心功能，不需要太多完善的设计和功能，所以设计上先实现一个简单的 ReAct Agent 框架，支持工具调用和过程可观测。后续再迭代多模态输入、更多工具、复杂规划算法等能力。

## 本模块范围
- 增加 Controller 和 Service 层。
- 提供 `/api/chat/stream` SSE 接口。
- 将 `AgentEvent` 按统一协议输出给前端。
- 增加基础健康检查和最小验收用例。

## 建议输出文件
- `com.xuan.xuanopenagent.controller.ChatController`
- `com.xuan.xuanopenagent.controller.HealthController`
- `com.xuan.xuanopenagent.service.AgentService`
- `com.xuan.xuanopenagent.model.ChatRequest` 或对应 DTO
- 如有需要：SSE 响应相关辅助类

## 核心设计要求
- SSE 事件名统一建议为 `agent-event`。
- 输出数据结构直接复用 `AgentEvent`，不要再定义第二套不兼容协议。
- `AgentService` 负责解耦 Controller 与 Agent，避免 Controller 直接承载复杂业务。
- 接口至少支持：用户消息、会话 ID、用户 ID、可选运行参数。
- 流结束时必须发出 `done` 或 `error` 事件。

## 推荐接口
- `POST /api/chat/stream`
- `GET /health`
- `GET /api/tools` 可作为可选补充

## 开发任务
1. 定义请求 DTO 和基础校验。
2. 实现 Service 调用 Agent 并转发事件。
3. 实现 SSE 输出。
4. 增加最小测试，覆盖启动、健康检查、Agent 基础链路。

## 验收清单
- 请求进入后能触发 Agent 执行。
- 前端或 Postman 可收到连续 SSE 事件。
- 事件中至少能看到 thought、tool_call、tool_result、final、done 中的关键类型。
- 当达到 `maxSteps`、工具出错或正常完成时，系统行为可解释。

## 一期交付结果
- 一个基于 Spring AI 的最小自主规划 Agent。
- 支持最大思考步数控制。
- 支持基础工具调用。
- 支持过程可观测和 SSE 流式输出。
- 支持后续继续扩展工具、规划能力和前端展示。

## 下一阶段扩展方向
1. 接入真实 web 搜索工具。
2. 增加会话存储和执行轨迹查询。
3. 增加更明确的 planning 阶段和计划步骤展示。
4. 逐步演进多模态输入和更复杂的工具编排。