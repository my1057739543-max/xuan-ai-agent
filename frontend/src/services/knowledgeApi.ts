import type { KnowledgeFile, RagBatchIngestionResult, RagIngestionResult } from '../types/chat'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || ''

export async function uploadKnowledgeFile(file: File, gameKey: string, tags?: string): Promise<RagIngestionResult> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('gameKey', gameKey)
  if (tags && tags.trim()) {
    formData.append('tags', tags.trim())
  }

  const response = await fetch(`${apiBaseUrl}/api/knowledge/upload`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const bodyText = await response.text().catch(() => '')
    throw new Error(`upload failed: ${response.status} ${response.statusText} ${bodyText}`.trim())
  }

  return (await response.json()) as RagIngestionResult
}

export async function uploadKnowledgeFiles(files: File[], gameKey: string, tags?: string): Promise<RagBatchIngestionResult> {
  const formData = new FormData()
  for (const file of files) {
    formData.append('files', file)
  }
  formData.append('gameKey', gameKey)
  if (tags && tags.trim()) {
    formData.append('tags', tags.trim())
  }

  const response = await fetch(`${apiBaseUrl}/api/knowledge/upload/batch`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const bodyText = await response.text().catch(() => '')
    throw new Error(`batch upload failed: ${response.status} ${response.statusText} ${bodyText}`.trim())
  }

  return (await response.json()) as RagBatchIngestionResult
}

export async function listKnowledgeFiles(): Promise<KnowledgeFile[]> {
  const response = await fetch(`${apiBaseUrl}/api/knowledge/files`, {
    method: 'GET',
  })

  if (!response.ok) {
    const bodyText = await response.text().catch(() => '')
    throw new Error(`list files failed: ${response.status} ${response.statusText} ${bodyText}`.trim())
  }

  return (await response.json()) as KnowledgeFile[]
}

export async function deleteKnowledgeFile(fileId: string): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/knowledge/files/${encodeURIComponent(fileId)}`, {
    method: 'DELETE',
  })

  if (!response.ok) {
    const bodyText = await response.text().catch(() => '')
    throw new Error(`delete file failed: ${response.status} ${response.statusText} ${bodyText}`.trim())
  }
}
