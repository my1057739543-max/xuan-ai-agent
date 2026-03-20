<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  uploading: boolean
}>()

const emit = defineEmits<{
  (event: 'upload', files: File[], gameKey: string, tags?: string): void
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFileNames = ref<string[]>([])
const selectedFiles = ref<File[]>([])
const selectedGameKey = ref('valorant')
const selectedTags = ref('')

function onSelectFile(event: Event): void {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files ?? [])
  selectedFiles.value = files
  selectedFileNames.value = files.map((file) => file.name)
}

function onUpload(): void {
  if (selectedFiles.value.length === 0 || props.uploading) {
    return
  }

  emit('upload', selectedFiles.value, selectedGameKey.value, selectedTags.value)
  selectedFiles.value = []
  selectedFileNames.value = []
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
      <input ref="fileInput" class="file-input" type="file" multiple accept=".txt,.md,.pdf" @change="onSelectFile" />
      <button class="upload-btn" :disabled="selectedFiles.length === 0 || uploading" @click="onUpload">
        {{ uploading ? '上传中...' : '上传' }}
      </button>
    </div>

    <label class="game-row">
      <span>gameKey</span>
      <select v-model="selectedGameKey" class="game-select">
        <option value="valorant">valorant</option>
        <option value="cs2">cs2</option>
        <option value="apex">apex</option>
        <option value="lol">lol</option>
      </select>
    </label>

    <label class="game-row">
      <span>tags</span>
      <input
        v-model="selectedTags"
        class="tag-input"
        type="text"
        placeholder="如: aim,movement,counter-strafe"
      />
    </label>

    <p class="selected-name">{{ selectedFiles.length > 0 ? `已选择 ${selectedFiles.length} 个文件` : '未选择文件' }}</p>
    <p v-if="selectedFileNames.length > 0" class="selected-name">{{ selectedFileNames.join(' | ') }}</p>
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

.game-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ink-soft);
}

.game-select {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-strong);
  color: var(--ink);
  padding: 5px 8px;
}

.tag-input {
  min-width: 230px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-strong);
  color: var(--ink);
  padding: 5px 8px;
}
</style>
