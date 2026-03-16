<script setup lang="ts">
import { computed } from 'vue'
import type { StreamStatus } from '../types/chat'

const props = defineProps<{
  modelValue: string
  status: StreamStatus
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'send'): void
  (event: 'stop'): void
}>()

const canSend = computed(() => props.status !== 'streaming' && props.modelValue.trim().length > 0)

function onEnter(event: KeyboardEvent): void {
  if (event.key !== 'Enter' || event.shiftKey) {
    return
  }
  event.preventDefault()
  if (canSend.value) {
    emit('send')
  }
}
</script>

<template>
  <section class="input-shell fade-rise">
    <div class="input-head">
      <p>Command Input</p>
      <span>Enter 发送 · Shift+Enter 换行</span>
    </div>

    <textarea
      :value="modelValue"
      rows="3"
      placeholder="输入你的问题，例如：帮我查下西湖附近咖啡店"
      @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
      @keydown="onEnter"
    />

    <div class="actions">
      <button class="send-btn" :disabled="!canSend" @click="emit('send')">发送</button>
      <button class="stop-btn" :disabled="status !== 'streaming'" @click="emit('stop')">停止</button>
    </div>
  </section>
</template>

<style scoped>
.input-shell {
  display: grid;
  gap: 12px;
  border-radius: 22px;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-soft);
  padding: 16px;
  background: var(--panel);
  backdrop-filter: blur(10px);
}

.input-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
}

.input-head p {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ink-soft);
  font-weight: 800;
}

.input-head span {
  font-size: 12px;
  color: var(--ink-soft);
}

textarea {
  width: 100%;
  resize: none;
  border-radius: 16px;
  border: 1px solid color-mix(in srgb, var(--brand) 28%, transparent);
  background: var(--panel-strong);
  color: var(--ink);
  padding: 14px;
  line-height: 1.58;
  font-size: 15px;
}

textarea:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--brand) 55%, white);
  outline-offset: 2px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

button {
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 9px 20px;
  font-weight: 700;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: transform 160ms ease, opacity 160ms ease, box-shadow 160ms ease;
}

button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(20, 28, 39, 0.15);
}

.send-btn {
  color: #fff;
  border-color: color-mix(in srgb, var(--brand) 45%, transparent);
  background: linear-gradient(120deg, var(--brand), color-mix(in srgb, var(--brand) 70%, black));
}

.stop-btn {
  color: #fff;
  border-color: color-mix(in srgb, var(--danger) 45%, transparent);
  background: linear-gradient(120deg, var(--danger), color-mix(in srgb, var(--danger) 70%, black));
}

@media (max-width: 680px) {
  .input-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions {
    justify-content: stretch;
  }

  .actions button {
    flex: 1;
  }
}
</style>
