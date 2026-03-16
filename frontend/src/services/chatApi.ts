import { createParser, type EventSourceMessage } from 'eventsource-parser'
import type { AgentEvent, ChatRequest } from '../types/chat'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || ''

interface StreamHandlers {
  onEvent: (event: AgentEvent) => void
  onDone?: () => void
  onError?: (error: Error) => void
  signal?: AbortSignal
}

export async function streamChat(request: ChatRequest, handlers: StreamHandlers): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify(request),
    signal: handlers.signal,
  })

  if (!response.ok || !response.body) {
    const bodyText = await response.text().catch(() => '')
    throw new Error(`stream request failed: ${response.status} ${response.statusText} ${bodyText}`.trim())
  }

  const parser = createParser({
    onEvent(message: EventSourceMessage) {
      if (!message.data || message.data === '[DONE]') {
        return
      }

      const parsed = safeJsonParse(message.data)
      const event = normalizeEvent(parsed)
      if (!event) {
        return
      }

      handlers.onEvent(event)
      if (event.type === 'done') {
        handlers.onDone?.()
      }
    },
  })

  const reader = response.body.getReader()
  const decoder = new TextDecoder()

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        handlers.onDone?.()
        break
      }
      parser.feed(decoder.decode(value, { stream: true }))
    }
  } catch (error) {
    const normalizedError = error instanceof Error ? error : new Error(String(error))
    handlers.onError?.(normalizedError)
    throw normalizedError
  }
}

function safeJsonParse(raw: string): unknown {
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function normalizeEvent(value: unknown): AgentEvent | null {
  if (!value || typeof value !== 'object') {
    return null
  }

  const event = value as Record<string, unknown>
  const payload = isRecord(event.payload) ? event.payload : {}

  return {
    traceId: stringOr(event.traceId, 'trace-unknown'),
    sessionId: stringOr(event.sessionId, 'session-unknown'),
    step: numberOr(event.step, 0),
    type: stringOr(event.type, 'message_delta'),
    timestamp: stringOr(event.timestamp, new Date().toISOString()),
    payload,
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value)
}

function stringOr(value: unknown, fallback: string): string {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function numberOr(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}
