<script setup lang="ts">
import type { AgentEvent, StreamStatus } from '../types/chat'

const props = defineProps<{
  events: AgentEvent[]
  status: StreamStatus
}>()

function compactPayload(payload: Record<string, unknown>): string {
  const source = { ...payload }
  delete source.timestamp
  const json = JSON.stringify(source)
  return json.length > 180 ? `${json.slice(0, 180)}...` : json
}

function eventLabel(event: AgentEvent): string {
  return `${String(event.type).toUpperCase()} · step ${event.step}`
}
</script>

<template>
  <section class="trace-panel fade-rise">
    <header class="trace-head">
      <h2>执行轨迹</h2>
      <span>{{ props.events.length }} events</span>
    </header>

    <ul class="timeline">
      <TransitionGroup name="trace" tag="div" class="timeline-inner">
        <li v-for="event in props.events" :key="`${event.traceId}-${event.step}-${event.type}-${event.timestamp}`" class="trace-item">
          <div class="badge">{{ eventLabel(event) }}</div>
          <p>{{ compactPayload(event.payload) }}</p>
        </li>
      </TransitionGroup>
    </ul>

    <p v-if="status === 'idle' && props.events.length === 0" class="empty">发送消息后，这里会显示 thought、tool_call、tool_result、final、done。</p>
  </section>
</template>

<style scoped>
.trace-panel {
  border-radius: 22px;
  border: 1px solid var(--line);
  background: var(--panel);
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow-soft);
  padding: 18px;
  display: grid;
  gap: 14px;
  height: clamp(420px, 62vh, 760px);
  overflow: hidden;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.trace-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h2 {
  font-size: 26px;
  letter-spacing: -0.01em;
}

header span {
  color: var(--ink-soft);
  font-size: 13px;
  font-weight: 700;
}

ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
  align-content: start;
}

.timeline {
  position: relative;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
}

.timeline::-webkit-scrollbar {
  width: 8px;
}

.timeline::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: color-mix(in srgb, var(--line-strong) 55%, transparent);
}

.timeline::before {
  content: '';
  position: absolute;
  left: 12px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: linear-gradient(var(--brand), var(--accent));
  opacity: 0.35;
}

.timeline-inner {
  display: grid;
  gap: 10px;
}

.trace-item {
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 11px 12px 11px 16px;
  background: var(--panel-strong);
  display: grid;
  gap: 8px;
  margin-left: 26px;
  position: relative;
  box-shadow: 0 8px 18px rgba(21, 29, 42, 0.08);
}

.trace-item::before {
  content: '';
  position: absolute;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  left: -20px;
  top: 14px;
  background: var(--accent);
  box-shadow: 0 0 0 4px rgba(201, 117, 33, 0.18);
}

.badge {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--brand);
  font-weight: 700;
}

p {
  font-size: 13px;
  line-height: 1.4;
  color: var(--ink-soft);
  margin: 0;
  word-break: break-word;
}

.empty {
  font-size: 13px;
  color: var(--ink-soft);
}

.trace-enter-active,
.trace-leave-active {
  transition: all 220ms ease;
}

.trace-enter-from,
.trace-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}
</style>
