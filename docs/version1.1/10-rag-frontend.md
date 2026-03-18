# 模块 10：RAG 前端收尾与可视化集成

你必须使用 spring ai 这个框架开发如果涉及 agent 开发部分。

## 1. 模块目标
- 在现有聊天页面完成知识库功能收尾。
- 增加 `useKnowledgeBase` 开关与可选 `fileIdFilter` 选择。
- 提供知识库文件可视化管理：上传、列表、删除、状态展示。
- 展示检索命中片段（retrieval hits）与最终回答联动。

## 2. 本模块范围
- 前端页面布局调整（聊天区 + 知识库侧栏/面板）。
- 聊天请求参数扩展：`options.useKnowledgeBase`、`options.fileIdFilter`。
- 知识库接口前端封装：上传、列表、删除。
- SSE 事件解析增强：支持 `retrieval`、`agent-event`、`final`、`done`。
- 文件状态可视化（PROCESSING / READY / FAILED）。

## 3. 接入原则
- 复用现有聊天主链路与 `POST /api/chat/stream`。
- 不创建第二套聊天页；在当前页面增强交互即可。
- 知识库面板与聊天输入联动，但保持解耦的组件边界。
- 当 `useKnowledgeBase=false` 时，行为必须与旧版本一致。

## 4. 建议输出文件
- `frontend/src/views/ChatView.vue`
- `frontend/src/components/KnowledgeBasePanel.vue`
- `frontend/src/components/KnowledgeUploadPanel.vue`
- `frontend/src/components/RetrievalHitPanel.vue`
- `frontend/src/components/KnowledgeFileTable.vue`
- `frontend/src/services/knowledgeApi.ts`
- `frontend/src/services/chatApi.ts`（补充 options 字段与 retrieval SSE 处理）
- `frontend/src/types/chat.ts`（扩展 ChatOptions / RetrievalHit / KnowledgeFile）

## 5. 页面与交互设计
### 5.1 主界面布局
- 左侧/上方：聊天消息区（保持现有体验）。
- 右侧/下方：知识库面板。
- 移动端：知识库面板折叠为抽屉或 Tab，不影响输入体验。

### 5.2 聊天输入区增强
- 新增开关：`使用知识库`（默认关闭）。
- 新增下拉：`知识文件范围`（可选）
  - 全部文件（默认）
  - 指定 fileId（显示文件名）
- 发送时构造请求：
```json
{
  "sessionId": "...",
  "userId": "...",
  "message": "...",
  "options": {
    "useKnowledgeBase": true,
    "fileIdFilter": "可选"
  }
}
```

### 5.3 知识库面板
- 上传区：选择文件并上传，展示进度和结果。
- 文件表格：展示 `fileName`、`status`、`chunkCount`、`createTime`。
- 删除按钮：删除文件并刷新列表。
- 状态色标：
  - READY: 绿色
  - PROCESSING: 橙色
  - FAILED: 红色

### 5.4 检索命中展示
- 监听 SSE `retrieval` 事件。
- 展示命中数量 `hitCount`。
- 展示命中列表（source/fileName/chunkIndex/score/snippet）。
- 点击命中可高亮或定位到对应来源信息（可选）。

## 6. 接口约定
### 6.1 知识库接口
- 上传文件：`POST /api/knowledge-base/upload`（multipart/form-data）
- 文件列表：`GET /api/knowledge-base/files`
- 删除文件：`DELETE /api/knowledge-base/{fileId}`

### 6.2 聊天流式接口
- `POST /api/chat/stream`
- 当启用知识库时，预期事件序列：
  1. `retrieval`
  2. `agent-event`（思考/工具/中间态）
  3. `final`
  4. `done`

## 7. 前端类型建议
```ts
export interface ChatOptions {
  maxSteps?: number
  temperature?: number
  useKnowledgeBase?: boolean
  fileIdFilter?: string
}

export interface RetrievalHit {
  fileId: string
  fileName: string
  chunkIndex: number
  sourceType: string
  score: number
  contentSnippet: string
}

export interface KnowledgeFile {
  fileId: string
  originalName: string
  status: 'PROCESSING' | 'READY' | 'FAILED'
  chunkCount?: number
  createdAt?: string
}
```

## 8. 验收标准
- `useKnowledgeBase` 开关可用，默认关闭。
- 上传后文件出现在列表中，状态可视化正确。
- 删除文件后列表即时刷新且无脏数据残留。
- 启用知识库聊天时可看到 `retrieval` 命中面板。
- 禁用知识库聊天时不显示检索命中，回答链路与旧版一致。
- 桌面和移动端均可正常操作主要功能。

## 9. 风险与处理
- 风险：上传后状态短时间停留 PROCESSING。
  - 处理：前端轮询列表或刷新按钮，必要时提示“处理中”。
- 风险：SSE 事件顺序偶发波动。
  - 处理：按事件类型归类渲染，避免强依赖严格顺序。
- 风险：删除后 UI 未同步。
  - 处理：删除成功后主动刷新文件列表并清理选中的 `fileIdFilter`。

## 10. 联调流程
1. 打开页面，上传 `txt/md/pdf` 文件。
2. 在文件列表确认状态变为 READY。
3. 打开 `使用知识库` 开关。
4. 发送问题并观察 `retrieval` 命中与最终回答。
5. 删除某文件后再次提问，确认命中来源已变化或消失。
