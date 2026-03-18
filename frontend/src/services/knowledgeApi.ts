import type { KnowledgeFile, RagIngestionResult } from '../types/chat'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || ''

export async function uploadKnowledgeFile(file: File): Promise<RagIngestionResult> {
  const formData = new FormData()
  formData.append('file', file)

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
