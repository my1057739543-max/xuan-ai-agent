# 模块 05：Vue.js 前端聊天界面与后端对接

## 模块目标
- 使用 Vue.js 快速搭建一个简单可用的聊天界面。
- 对接后端 `/api/chat/stream` SSE 接口，实时展示用户消息、模型回复和 Agent 执行过程。
- 当前阶段只追求最小可演示版本，不追求复杂 UI、复杂状态管理和完整会话系统。

## 强制约束
- 你必须使用 Spring AI 这个框架开发如果涉及 Agent 开发部分。
- ！！！！！！！！！！！！重要！！！！！！！！本项目当前阶段首先需要快速搭建核心功能，不需要太多完善的设计和功能，所以设计上先实现一个简单的 ReAct Agent 框架，支持工具调用和过程可观测。后续再迭代多模态输入、更多工具、复杂规划算法等能力。

## 本模块范围
- 基于 Vue.js 实现单页聊天界面。
- 支持输入消息、发送请求、展示回复。
- 支持展示 Agent 的 thought、tool_call、tool_result、final、done 等事件。
- 支持流式状态展示：发送中、执行中、完成、错误。

## 技术选型建议
- 前端框架：Vue 3
- 构建工具：Vite
- HTTP 方式：`fetch + ReadableStream` 或基于 SSE 协议的流式消费
- 状态管理：当前阶段优先使用组件内响应式状态，不强制引入 Pinia
- UI：先使用最简单的原生样式或少量 CSS，不引入重型组件库

## 页面目标
- 一个聊天页面即可，不需要多页面路由。
- 页面区域建议包括：
  - 顶部标题栏
  - 聊天消息区
  - Agent 执行过程区
  - 输入框和发送按钮
  - 可选停止按钮

## 建议目录结构
- `src/views/ChatView.vue`
- `src/components/ChatMessageList.vue`
- `src/components/AgentTracePanel.vue`
- `src/components/ChatInputBox.vue`
- `src/services/chatApi.ts`
- `src/types/chat.ts`

## 前端核心状态
- `chatMessages`：展示用户消息与助手回复
- `agentEvents`：展示后端流式返回的 Agent 过程事件
- `streamStatus`：`idle | streaming | done | error`
- `currentSessionId`：当前会话 ID
- `pendingText`：输入框内容

## 与后端对接要求
- 请求接口：`POST /api/chat/stream`
- 请求体建议包含：

```json
{
  "sessionId": "s-001",
  "userId": "u-001",
  "message": "现在杭州几点了？",
  "options": {
    "maxSteps": 6
  }
}
```

- 前端需要能解析后端返回的 `agent-event` 事件。
- 每条事件都按 `traceId + step + type` 做基础去重。
- 收到 `done` 后关闭流并更新状态。
- 收到 `error` 后提示错误并允许用户重新发送。

## 事件展示建议
- `thought`：展示为“思考中”卡片
- `tool_call`：展示调用了哪个工具和参数
- `tool_result`：展示工具返回结果
- `message_delta`：可逐步拼接回答内容
- `final`：展示最终答案
- `done`：标记本轮结束

## 推荐最小交互
1. 用户输入消息并点击发送。
2. 前端立即把用户消息加入消息列表。
3. 前端开始监听流式响应。
4. 收到事件后更新右侧或下方的执行轨迹面板。
5. 收到最终结果后，把助手回复加入消息区。
6. 流结束后恢复输入状态。

## 开发任务
1. 创建 Vue 3 前端项目基础结构。
2. 完成聊天页布局和基础样式。
3. 完成后端 SSE 对接逻辑。
4. 建立消息和事件的数据类型定义。
5. 增加基础错误处理和加载状态。

## 非目标
- 不实现复杂会话管理。
- 不实现用户系统。
- 不实现复杂 UI 动效。
- 不实现多标签页会话同步。

## 完成标准
- 前端可以向后端发送聊天请求。
- 可以实时展示 Agent 执行过程。
- 可以看到最终回复和结束状态。
- 页面足够简单但可以完成联调和演示。

## 联调验收清单
- 输入消息后，后端成功收到请求。
- 前端可持续接收 thought、tool_call、tool_result、final、done 等事件。
- 出错时有明确提示，不会让页面卡死。
- 后端达到 `maxSteps` 时，前端能正确展示结束结果。

## 后续扩展方向
1. 增加会话列表和历史记录。
2. 增加中止当前请求能力。
3. 优化执行轨迹展示为时间轴。
4. 增加 Markdown 渲染和代码高亮。
5. 接入更完整的前端状态管理。