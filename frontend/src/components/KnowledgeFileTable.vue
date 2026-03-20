<script setup lang="ts">
import type { KnowledgeFile } from '../types/chat'

const props = defineProps<{
  files: KnowledgeFile[]
  deletingFileId: string
  fileIdFilter: string
}>()

const emit = defineEmits<{
  (event: 'delete', fileId: string): void
  (event: 'select-filter', fileId: string): void
  (event: 'clear-filter'): void
}>()

function statusClass(status: string): string {
  if (status === 'READY') return 'ready'
  if (status === 'PROCESSING') return 'processing'
  if (status === 'FAILED') return 'failed'
  return ''
}

function shortFileId(fileId: string): string {
  if (!fileId) return '-'
  return `${fileId.slice(0, 8)}...${fileId.slice(-4)}`
}
</script>

<template>
  <section class="table-shell">
    <div class="table-head">
      <p>Knowledge Files</p>
      <button class="clear-filter" :disabled="!fileIdFilter" @click="emit('clear-filter')">清空范围</button>
    </div>

    <div v-if="files.length === 0" class="empty">暂无知识文件</div>

    <div v-else class="rows">
      <article v-for="item in files" :key="item.fileId" class="row">
        <div class="meta">
          <strong :title="item.originalName">{{ item.originalName || item.storedName }}</strong>
          <small>fileId: {{ shortFileId(item.fileId) }}</small>
        </div>

        <div class="chips">
          <span class="chip" :class="statusClass(item.status)">{{ item.status }}</span>
          <span class="chip soft">chunks: {{ item.documentCount ?? 0 }}</span>
        </div>

        <div class="ops">
          <button
            class="scope-btn"
            :class="{ active: fileIdFilter === item.fileId }"
            @click="emit('select-filter', item.fileId)"
          >
            {{ fileIdFilter === item.fileId ? '已选范围' : '设为范围' }}
          </button>
          <button class="delete-btn" :disabled="deletingFileId === item.fileId" @click="emit('delete', item.fileId)">
            {{ deletingFileId === item.fileId ? '删除中...' : '删除' }}
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.table-shell {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--panel);
  padding: 12px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.table-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-head p {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ink-soft);
  font-weight: 800;
}

.clear-filter {
  border: 1px solid var(--line);
  background: var(--panel-strong);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.clear-filter:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty {
  font-size: 13px;
  color: var(--ink-soft);
  text-align: center;
  padding: 10px;
}

.rows {
  display: grid;
  gap: 8px;
  max-height: none;
  overflow: auto;
}

.row {
  border: 1px solid color-mix(in srgb, var(--line) 75%, white);
  border-radius: 10px;
  padding: 10px;
  display: grid;
  gap: 8px;
  background: color-mix(in srgb, var(--panel-strong) 85%, white);
}

.meta strong {
  display: block;
  font-size: 13px;
  line-height: 1.35;
}

.meta small {
  color: var(--ink-soft);
  font-size: 12px;
}

.chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.chip {
  border-radius: 999px;
  border: 1px solid transparent;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
}

.soft {
  color: var(--ink-soft);
  border-color: color-mix(in srgb, var(--line) 70%, transparent);
  background: color-mix(in srgb, var(--line) 12%, white);
}

.ready {
  color: var(--ok);
  border-color: color-mix(in srgb, var(--ok) 40%, transparent);
  background: color-mix(in srgb, var(--ok) 12%, white);
}

.processing {
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 40%, transparent);
  background: color-mix(in srgb, var(--accent) 14%, white);
}

.failed {
  color: var(--danger);
  border-color: color-mix(in srgb, var(--danger) 40%, transparent);
  background: color-mix(in srgb, var(--danger) 12%, white);
}

.ops {
  display: flex;
  gap: 8px;
}

.scope-btn,
.delete-btn {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  border: 1px solid var(--line);
}

.scope-btn {
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.scope-btn.active {
  border-color: color-mix(in srgb, var(--brand) 50%, transparent);
  color: var(--brand);
}

.delete-btn {
  background: color-mix(in srgb, var(--danger) 8%, white);
  color: var(--danger);
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
