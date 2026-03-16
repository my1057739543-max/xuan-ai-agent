<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import AgentTracePanel from '../components/AgentTracePanel.vue'
import ChatInputBox from '../components/ChatInputBox.vue'
import ChatMessageList from '../components/ChatMessageList.vue'
import { streamChat } from '../services/chatApi'
import type { AgentEvent, ChatMessage, ChatRequest, StreamStatus } from '../types/chat'

const pendingText = ref('')
const currentSessionId = ref(`s-${Date.now()}`)
const currentUserId = ref('u-local-dev')
const streamStatus = ref<StreamStatus>('idle')
const chatMessages = ref<ChatMessage[]>([
  {
    id: crypto.randomUUID(),
    role: 'assistant',
    content: '你好，我是 Xuan Agent。你可以直接提问，我会把执行轨迹实时展示出来。',
    createdAt: new Date().toISOString(),
  },
])
const agentEvents = ref<AgentEvent[]>([])

const dedupeKeys = new Set<string>()
const activeController = ref<AbortController | null>(null)
let activeAssistantMessageId = ''

const statusText = computed(() => {
  if (streamStatus.value === 'streaming') {
    return 'Agent 正在执行 ReAct 流程'
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

function collectEvent(event: AgentEvent): void {
  const key = buildDedupeKey(event)
  if (dedupeKeys.has(key)) {
    return
  }

  dedupeKeys.add(key)
  agentEvents.value.push(event)

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

async function sendMessage(): Promise<void> {
  const text = pendingText.value.trim()
  if (!text || streamStatus.value === 'streaming') {
    return
  }

  chatMessages.value.push(buildMessage('user', text))
  pendingText.value = ''
  streamStatus.value = 'streaming'
  agentEvents.value = []
  dedupeKeys.clear()
  activeAssistantMessageId = ''

  const controller = new AbortController()
  activeController.value = controller

  const request: ChatRequest = {
    sessionId: currentSessionId.value,
    userId: currentUserId.value,
    message: text,
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

onBeforeUnmount(() => {
  activeController.value?.abort()
})
</script>

<template>
  <main class="chat-view">
    <header class="hero fade-rise">
      <div class="hero-title-group">
        <p class="tag">Xuan Open Agent</p>
        <h1>Conversation Control Deck</h1>
        <p class="hero-subtitle">一个面向调试与演示的实时 ReAct 观测界面</p>
      </div>
      <p class="desc" :class="statusTone">{{ statusText }}</p>
    </header>

    <section class="summary-strip fade-rise">
      <article v-for="card in summaryCards" :key="card.label" class="summary-card">
        <p>{{ card.label }}</p>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <section class="grid">
      <ChatMessageList :messages="chatMessages" :status="streamStatus" />
      <AgentTracePanel :events="agentEvents" :status="streamStatus" />
    </section>

    <ChatInputBox v-model="pendingText" :status="streamStatus" @send="sendMessage" @stop="stopStream" />
  </main>
</template>

<style scoped>
.chat-view {
  max-width: 1280px;
  margin: 0 auto;
  min-height: 100svh;
  padding: 32px 18px 24px;
  display: grid;
  gap: 16px;
}

.hero {
  border: 1px solid var(--line-strong);
  border-radius: 24px;
  background:
    linear-gradient(115deg, rgba(15, 106, 117, 0.08), rgba(201, 117, 33, 0.11)),
    var(--panel);
  box-shadow: var(--shadow);
  padding: 24px;
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
  font-size: 42px;
  letter-spacing: -0.02em;
  line-height: 1.03;
}

.hero-subtitle {
  color: var(--ink-soft);
  font-size: 15px;
}

.desc {
  max-width: 250px;
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  font-size: 28px;
  line-height: 1;
  font-family: 'Fraunces', Georgia, serif;
  color: var(--ink);
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 16px;
}

@media (max-width: 980px) {
  .chat-view {
    padding: 14px 12px;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 18px;
  }

  h1 {
    font-size: 30px;
  }

  .desc {
    width: 100%;
    max-width: none;
  }

  .grid {
    grid-template-columns: 1fr;
  }

  .summary-strip {
    grid-template-columns: 1fr;
  }
}
</style>
