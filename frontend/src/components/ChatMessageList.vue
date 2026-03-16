<script setup lang="ts">
import type { ChatMessage, StreamStatus } from '../types/chat'

defineProps<{
  messages: ChatMessage[]
  status: StreamStatus
}>()

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  return Number.isNaN(date.getTime()) ? '--:--' : date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <section class="message-list fade-rise">
    <header class="list-head">
      <h2>聊天记录</h2>
      <span class="status" :class="`status-${status}`">
        <i />
        {{ status }}
      </span>
    </header>

    <TransitionGroup name="msg" tag="ul">
      <li v-for="item in messages" :key="item.id" :class="['bubble', item.role]">
        <p class="content">{{ item.content }}</p>
        <small>{{ item.role === 'user' ? '你' : 'Agent' }} · {{ formatTime(item.createdAt) }}</small>
      </li>
    </TransitionGroup>
  </section>
</template>

<style scoped>
.message-list {
  border-radius: 22px;
  border: 1px solid var(--line);
  background: var(--panel);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow-soft);
  padding: 18px;
  display: grid;
  gap: 14px;
  min-height: 420px;
}

.list-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h2 {
  font-size: 26px;
  letter-spacing: -0.01em;
}

ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
  align-content: start;
}

.bubble {
  padding: 14px 15px;
  border-radius: 16px;
  border: 1px solid var(--line);
  display: grid;
  gap: 10px;
  max-width: 90%;
  box-shadow: 0 8px 20px rgba(24, 31, 45, 0.08);
}

.bubble.user {
  justify-self: end;
  background: linear-gradient(140deg, rgba(15, 106, 117, 0.2), rgba(15, 106, 117, 0.08));
  border-color: color-mix(in srgb, var(--brand) 34%, transparent);
}

.bubble.assistant {
  justify-self: start;
  background: linear-gradient(140deg, rgba(201, 117, 33, 0.15), rgba(255, 255, 255, 0.85));
  border-color: color-mix(in srgb, var(--accent) 32%, transparent);
}

.content {
  white-space: pre-wrap;
  line-height: 1.62;
  font-size: 14px;
}

small {
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 600;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 600;
}

.status i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.status-streaming {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 15%, white);
}

.status-streaming i {
  animation: pulse-dot 900ms infinite alternate ease-in-out;
}

.status-idle,
.status-done {
  color: var(--ok);
  background: color-mix(in srgb, var(--ok) 12%, white);
}

.status-error {
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 12%, white);
}

.msg-enter-active,
.msg-leave-active {
  transition: all 260ms ease;
}

.msg-enter-from,
.msg-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
