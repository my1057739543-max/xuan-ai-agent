export type StreamStatus = 'idle' | 'streaming' | 'done' | 'error'

export interface ChatOptions {
  maxSteps?: number
  temperature?: number
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
