<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  uploading: boolean
}>()

const emit = defineEmits<{
  (event: 'upload', file: File): void
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFileName = ref('')
const selectedFile = ref<File | null>(null)

function onSelectFile(event: Event): void {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null
  selectedFile.value = file
  selectedFileName.value = file?.name ?? ''
}

function onUpload(): void {
  if (!selectedFile.value || props.uploading) {
    return
  }

  emit('upload', selectedFile.value)
  selectedFile.value = null
  selectedFileName.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}
</script>

<template>
  <section class="upload-panel">
    <div class="head">
      <p>Knowledge Upload</p>
      <span>支持 txt / md / pdf</span>
    </div>

    <div class="picker-row">
      <input ref="fileInput" class="file-input" type="file" accept=".txt,.md,.pdf" @change="onSelectFile" />
      <button class="upload-btn" :disabled="!selectedFile || uploading" @click="onUpload">
        {{ uploading ? '上传中...' : '上传' }}
      </button>
    </div>

    <p class="selected-name">{{ selectedFileName || '未选择文件' }}</p>
  </section>
</template>

<style scoped>
.upload-panel {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--panel);
  padding: 12px;
  display: grid;
  gap: 10px;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: baseline;
}

.head p {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--ink-soft);
  font-weight: 800;
}

.head span {
  font-size: 12px;
  color: var(--ink-soft);
}

.picker-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.file-input {
  width: 100%;
  border: 1px dashed color-mix(in srgb, var(--brand) 38%, transparent);
  border-radius: 10px;
  background: var(--panel-strong);
  color: var(--ink-soft);
  padding: 8px;
}

.upload-btn {
  border: 1px solid color-mix(in srgb, var(--brand) 45%, transparent);
  border-radius: 999px;
  background: linear-gradient(120deg, var(--brand), color-mix(in srgb, var(--brand) 70%, black));
  color: #fff;
  font-weight: 700;
  padding: 8px 14px;
  cursor: pointer;
}

.upload-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.selected-name {
  font-size: 12px;
  color: var(--ink-soft);
  word-break: break-all;
}
</style>
