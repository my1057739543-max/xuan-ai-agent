<script setup lang="ts">
import type { RetrievalHit, StreamStatus } from '../types/chat'

const props = defineProps<{
  hits: RetrievalHit[]
  status: StreamStatus
}>()

function scoreText(score: number): string {
  return `${(score * 100).toFixed(1)}%`
}
</script>

<template>
  <section class="hit-panel">
    <div class="head">
      <p>Retrieval Hits</p>
      <span>{{ hits.length }} 条</span>
    </div>

    <div v-if="hits.length === 0" class="empty">
      {{ status === 'streaming' ? '等待检索结果...' : '本轮暂无检索命中' }}
    </div>

    <div v-else class="list">
      <article v-for="(item, index) in hits" :key="`${item.fileId}-${item.chunkIndex}-${index}`" class="hit-item">
        <header>
          <strong>{{ item.fileName || 'unknown-file' }}</strong>
          <span>chunk {{ item.chunkIndex }}</span>
        </header>
        <p class="snippet">{{ item.contentSnippet }}</p>
        <footer>
          <small>{{ item.sourceType || 'unknown' }}</small>
          <small>score {{ scoreText(item.score) }}</small>
        </footer>
      </article>
    </div>
  </section>
</template>

<style scoped>
.hit-panel {
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

.empty {
  font-size: 13px;
  color: var(--ink-soft);
  text-align: center;
  padding: 10px;
}

.list {
  display: grid;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

.hit-item {
  border: 1px solid color-mix(in srgb, var(--line) 70%, white);
  background: color-mix(in srgb, var(--panel-strong) 85%, white);
  border-radius: 10px;
  padding: 10px;
  display: grid;
  gap: 6px;
}

.hit-item header,
.hit-item footer {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.hit-item strong {
  font-size: 13px;
}

.hit-item span,
.hit-item small {
  font-size: 12px;
  color: var(--ink-soft);
}

.snippet {
  font-size: 13px;
  line-height: 1.45;
}
</style>
