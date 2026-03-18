export type StreamStatus = 'idle' | 'streaming' | 'done' | 'error'

export interface ChatOptions {
  maxSteps?: number
  temperature?: number
  useKnowledgeBase?: boolean
  fileIdFilter?: string
}

export interface ChatRequest {
  sessionId: string
  userId: string
  message: string
  options?: ChatOptions
}

export type AgentEventType =
  | 'plan'
  | 'thought'
  | 'tool_call'
  | 'tool_result'
  | 'message_delta'
  | 'final'
  | 'error'
  | 'done'
  | string

export interface AgentEvent {
  traceId: string
  sessionId: string
  step: number
  type: AgentEventType
  timestamp: string
  payload: Record<string, unknown>
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface RetrievalHit {
  fileId: string
  fileName: string
  chunkIndex: number
  sourceType: string
  score: number
  contentSnippet: string
}

export type KnowledgeFileStatus = 'PROCESSING' | 'READY' | 'FAILED'

export interface KnowledgeFile {
  fileId: string
  originalName: string
  storedName: string
  extension: string
  mimeType: string
  sizeBytes: number
  status: KnowledgeFileStatus
  errorMessage?: string
  documentCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface RagIngestionResult {
  fileId: string
  status: string
  documentCount: number
  chunkCount: number
}
