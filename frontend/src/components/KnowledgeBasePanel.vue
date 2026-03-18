<script setup lang="ts">
import KnowledgeFileTable from './KnowledgeFileTable.vue'
import KnowledgeUploadPanel from './KnowledgeUploadPanel.vue'
import type { KnowledgeFile } from '../types/chat'

const props = defineProps<{
  files: KnowledgeFile[]
  loading: boolean
  uploading: boolean
  deletingFileId: string
  useKnowledgeBase: boolean
  fileIdFilter: string
}>()

const emit = defineEmits<{
  (event: 'refresh'): void
  (event: 'upload', file: File): void
  (event: 'delete', fileId: string): void
  (event: 'update:useKnowledgeBase', value: boolean): void
  (event: 'update:fileIdFilter', value: string): void
}>()
</script>

<template>
  <aside class="kb-panel">
    <div class="panel-head">
      <h3>Knowledge Base</h3>
      <button class="refresh-btn" :disabled="loading" @click="emit('refresh')">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </div>

    <label class="toggle-row">
      <input
        type="checkbox"
        :checked="useKnowledgeBase"
        @change="emit('update:useKnowledgeBase', ($event.target as HTMLInputElement).checked)"
      />
      <span>使用知识库回答</span>
    </label>

    <KnowledgeUploadPanel :uploading="uploading" @upload="(file) => emit('upload', file)" />

    <KnowledgeFileTable
      :files="files"
      :deleting-file-id="deletingFileId"
      :file-id-filter="fileIdFilter"
      @delete="(fileId) => emit('delete', fileId)"
      @select-filter="(fileId) => emit('update:fileIdFilter', fileId)"
      @clear-filter="emit('update:fileIdFilter', '')"
    />
  </aside>
</template>

<style scoped>
.kb-panel {
  border: 1px solid var(--line-strong);
  border-radius: 18px;
  background: var(--panel);
  box-shadow: var(--shadow-soft);
  padding: 14px;
  display: grid;
  gap: 10px;
  align-content: start;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-head h3 {
  font-size: 20px;
}

.refresh-btn {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 6px 12px;
  background: var(--panel-strong);
  cursor: pointer;
}

.refresh-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.toggle-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--ink);
}

.toggle-row input {
  accent-color: var(--brand);
}
</style>
