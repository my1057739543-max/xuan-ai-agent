<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AgentTracePanel from '../components/AgentTracePanel.vue'
import ChatInputBox from '../components/ChatInputBox.vue'
import ChatMessageList from '../components/ChatMessageList.vue'
import KnowledgeBasePanel from '../components/KnowledgeBasePanel.vue'
import RetrievalHitPanel from '../components/RetrievalHitPanel.vue'
import { streamChat } from '../services/chatApi'
import { deleteKnowledgeFile, listKnowledgeFiles, uploadKnowledgeFile } from '../services/knowledgeApi'
import type { AgentEvent, ChatMessage, ChatRequest, KnowledgeFile, RetrievalHit, StreamStatus } from '../types/chat'

const pendingText = ref('')
const currentSessionId = ref(`s-${Date.now()}`)
const currentUserId = ref('u-local-dev')
const streamStatus = ref<StreamStatus>('idle')

const useKnowledgeBase = ref(false)
const fileIdFilter = ref('')
const retrievalHits = ref<RetrievalHit[]>([])
const knowledgeFiles = ref<KnowledgeFile[]>([])
const knowledgeLoading = ref(false)
const uploading = ref(false)
const deletingFileId = ref('')

const chatMessages = ref<ChatMessage[]>([
  {
    id: crypto.randomUUID(),
    role: 'assistant',
    content: '你好，我是 Xuan Agent。你可以直接提问，也可以开启知识库问答。',
    createdAt: new Date().toISOString(),
  },
])
const agentEvents = ref<AgentEvent[]>([])

const dedupeKeys = new Set<string>()
const activeController = ref<AbortController | null>(null)
let activeAssistantMessageId = ''

const statusText = computed(() => {
  if (streamStatus.value === 'streaming') {
    return useKnowledgeBase.value ? 'Agent 正在执行知识检索与回答' : 'Agent 正在执行 ReAct 流程'
  }
  if (streamStatus.value === 'error') {
    return '本轮失败，可修改问题后重试'
  }
  if (streamStatus.value === 'done') {
    return '本轮完成'
  }
  return '等待输入'
})

const statusTone = computed(() => {
  if (streamStatus.value === 'streaming') {
    return 'is-streaming'
  }
  if (streamStatus.value === 'error') {
    return 'is-error'
  }
  if (streamStatus.value === 'done') {
    return 'is-done'
  }
  return 'is-idle'
})

const summaryCards = computed(() => [
  { label: 'Messages', value: String(chatMessages.value.length).padStart(2, '0') },
  { label: 'Trace Events', value: String(agentEvents.value.length).padStart(2, '0') },
  { label: 'KB Files', value: String(knowledgeFiles.value.length).padStart(2, '0') },
  { label: 'Session', value: currentSessionId.value.slice(-6) },
])

function buildMessage(role: 'user' | 'assistant', content: string): ChatMessage {
  return {
    id: crypto.randomUUID(),
    role,
    content,
    createdAt: new Date().toISOString(),
  }
}

function buildDedupeKey(event: AgentEvent): string {
  return `${event.traceId}-${event.step}-${event.type}`
}

function addOrUpdateAssistantMessage(content: string, mode: 'append' | 'replace' = 'append'): void {
  if (!activeAssistantMessageId) {
    const message = buildMessage('assistant', '')
    activeAssistantMessageId = message.id
    chatMessages.value.push(message)
  }

  const target = chatMessages.value.find((item) => item.id === activeAssistantMessageId)
  if (!target) {
    return
  }

  if (mode === 'replace') {
    target.content = content
  } else {
    target.content = `${target.content}${content}`
  }
}

function toRetrievalHits(payload: Record<string, unknown>): RetrievalHit[] {
  const rawHits = payload.hits
  if (!Array.isArray(rawHits)) {
    return []
  }

  return rawHits.map((item) => {
    const record = (item ?? {}) as Record<string, unknown>
    return {
      fileId: String(record.fileId ?? ''),
      fileName: String(record.fileName ?? ''),
      chunkIndex: Number(record.chunkIndex ?? 0),
      sourceType: String(record.sourceType ?? ''),
      score: Number(record.score ?? 0),
      contentSnippet: String(record.contentSnippet ?? ''),
    }
  })
}

function collectEvent(event: AgentEvent): void {
  const key = buildDedupeKey(event)
  if (dedupeKeys.has(key)) {
    return
  }

  dedupeKeys.add(key)
  agentEvents.value.push(event)

  if (event.type === 'retrieval') {
    retrievalHits.value = toRetrievalHits(event.payload)
  }

  if (event.type === 'message_delta') {
    const chunk = String(event.payload.content ?? '')
    if (chunk) {
      addOrUpdateAssistantMessage(chunk, 'append')
    }
  }

  if (event.type === 'final') {
    const content = String(event.payload.content ?? '')
    if (content) {
      addOrUpdateAssistantMessage(content, 'replace')
    }
  }

  if (event.type === 'error') {
    streamStatus.value = 'error'
    const message = String(event.payload.message ?? '请求失败，请稍后重试')
    addOrUpdateAssistantMessage(message, 'replace')
  }

  if (event.type === 'done' && streamStatus.value !== 'error') {
    streamStatus.value = 'done'
  }
}

async function loadKnowledgeFiles(): Promise<void> {
  knowledgeLoading.value = true
  try {
    knowledgeFiles.value = await listKnowledgeFiles()
    if (fileIdFilter.value && !knowledgeFiles.value.some((item) => item.fileId === fileIdFilter.value)) {
      fileIdFilter.value = ''
    }
  } catch (error) {
    addOrUpdateAssistantMessage(`知识库列表加载失败：${String(error)}`, 'replace')
  } finally {
    knowledgeLoading.value = false
  }
}

async function handleUpload(file: File): Promise<void> {
  uploading.value = true
  try {
    await uploadKnowledgeFile(file)
    await loadKnowledgeFiles()
  } catch (error) {
    addOrUpdateAssistantMessage(`上传失败：${String(error)}`, 'replace')
  } finally {
    uploading.value = false
  }
}

async function handleDelete(fileId: string): Promise<void> {
  deletingFileId.value = fileId
  try {
    await deleteKnowledgeFile(fileId)
    await loadKnowledgeFiles()
  } catch (error) {
    addOrUpdateAssistantMessage(`删除失败：${String(error)}`, 'replace')
  } finally {
    deletingFileId.value = ''
  }
}

async function sendMessage(): Promise<void> {
  const text = pendingText.value.trim()
  if (!text || streamStatus.value === 'streaming') {
    return
  }

  chatMessages.value.push(buildMessage('user', text))
  pendingText.value = ''
  streamStatus.value = 'streaming'
  retrievalHits.value = []
  agentEvents.value = []
  dedupeKeys.clear()
  activeAssistantMessageId = ''

  const controller = new AbortController()
  activeController.value = controller

  const request: ChatRequest = {
    sessionId: currentSessionId.value,
    userId: currentUserId.value,
    message: text,
    options: {
      useKnowledgeBase: useKnowledgeBase.value,
      fileIdFilter: fileIdFilter.value || undefined,
    },
  }

  try {
    await streamChat(request, {
      signal: controller.signal,
      onEvent: collectEvent,
      onDone: () => {
        if (streamStatus.value === 'streaming') {
          streamStatus.value = 'done'
        }
      },
      onError: () => {
        streamStatus.value = 'error'
      },
    })
  } catch (error) {
    if (controller.signal.aborted) {
      streamStatus.value = 'done'
      addOrUpdateAssistantMessage('已手动停止本轮执行。', 'replace')
    } else {
      streamStatus.value = 'error'
      addOrUpdateAssistantMessage(`请求异常：${String(error)}`, 'replace')
    }
  } finally {
    activeController.value = null
  }
}

function stopStream(): void {
  activeController.value?.abort()
}

onMounted(() => {
  loadKnowledgeFiles()
})

onBeforeUnmount(() => {
  activeController.value?.abort()
})
</script>

<template>
  <main class="chat-view">
    <header class="hero fade-rise">
      <div class="hero-title-group">
        <p class="tag">Xuan Open Agent</p>
        <h1>Conversation + Knowledge Deck</h1>
        <p class="hero-subtitle">聊天、知识库管理与检索命中可视化的一体化面板</p>
      </div>
      <p class="desc" :class="statusTone">{{ statusText }}</p>
    </header>

    <section class="summary-strip fade-rise">
      <article v-for="card in summaryCards" :key="card.label" class="summary-card">
        <p>{{ card.label }}</p>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <section class="layout-grid">
      <div class="chat-main">
        <section class="message-grid">
          <ChatMessageList :messages="chatMessages" :status="streamStatus" />
          <AgentTracePanel :events="agentEvents" :status="streamStatus" />
        </section>
        <ChatInputBox v-model="pendingText" :status="streamStatus" @send="sendMessage" @stop="stopStream" />
      </div>

      <div class="kb-side">
        <KnowledgeBasePanel
          :files="knowledgeFiles"
          :loading="knowledgeLoading"
          :uploading="uploading"
          :deleting-file-id="deletingFileId"
          :use-knowledge-base="useKnowledgeBase"
          :file-id-filter="fileIdFilter"
          @refresh="loadKnowledgeFiles"
          @upload="handleUpload"
          @delete="handleDelete"
          @update:use-knowledge-base="(v) => (useKnowledgeBase = v)"
          @update:file-id-filter="(v) => (fileIdFilter = v)"
        />

        <RetrievalHitPanel :hits="retrievalHits" :status="streamStatus" />
      </div>
    </section>
  </main>
</template>

<style scoped>
.chat-view {
  max-width: 1320px;
  margin: 0 auto;
  min-height: 100svh;
  padding: 26px 18px 24px;
  display: grid;
  gap: 14px;
}

.hero {
  border: 1px solid var(--line-strong);
  border-radius: 24px;
  background:
    linear-gradient(115deg, rgba(15, 106, 117, 0.08), rgba(201, 117, 33, 0.11)),
    var(--panel);
  box-shadow: var(--shadow);
  padding: 22px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.hero-title-group {
  display: grid;
  gap: 8px;
}

.tag {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--ink-soft);
  font-weight: 700;
}

h1 {
  font-size: 40px;
  letter-spacing: -0.02em;
  line-height: 1.04;
}

.hero-subtitle {
  color: var(--ink-soft);
  font-size: 15px;
}

.desc {
  max-width: 270px;
  border-radius: 999px;
  border: 1px solid transparent;
  padding: 10px 14px;
  line-height: 1.4;
  font-weight: 700;
  text-align: center;
  font-size: 13px;
}

.is-idle {
  color: var(--ok);
  border-color: color-mix(in srgb, var(--ok) 40%, transparent);
  background: color-mix(in srgb, var(--ok) 12%, white);
}

.is-streaming {
  color: var(--brand);
  border-color: color-mix(in srgb, var(--brand) 45%, transparent);
  background: linear-gradient(90deg, rgba(15, 106, 117, 0.14), rgba(15, 106, 117, 0.05));
}

.is-error {
  color: var(--danger);
  border-color: color-mix(in srgb, var(--danger) 45%, transparent);
  background: color-mix(in srgb, var(--danger) 10%, white);
}

.is-done {
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 45%, transparent);
  background: color-mix(in srgb, var(--accent) 11%, white);
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  border-radius: 16px;
  border: 1px solid var(--line);
  background: var(--panel);
  box-shadow: var(--shadow-soft);
  padding: 12px 14px;
  display: grid;
  gap: 4px;
}

.summary-card p {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.09em;
  color: var(--ink-soft);
  font-weight: 700;
}

.summary-card strong {
  font-size: 26px;
  line-height: 1;
  font-family: 'Fraunces', Georgia, serif;
  color: var(--ink);
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 12px;
  align-items: start;
}

.chat-main {
  display: grid;
  gap: 12px;
}

.message-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 380px);
  gap: 12px;
  align-items: stretch;
}

.kb-side {
  display: grid;
  gap: 12px;
  position: sticky;
  top: 14px;
}

@media (max-width: 1180px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }

  .kb-side {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .message-grid {
    grid-template-columns: 1fr;
  }

  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  h1 {
    font-size: 30px;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .desc {
    max-width: none;
  }

  .kb-side {
    grid-template-columns: 1fr;
  }
}
</style>
