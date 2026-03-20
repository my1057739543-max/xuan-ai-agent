package com.xuan.xuanopenagent.rag.chunking.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * LLM 切片事件发布器。
 * 负责发送 chunk_started、chunk_fallback 等事件到应用上下文，
 * 以便其他组件（如 SSE 发送器、监控系统）订阅处理。
 */
@Component
public class ChunkingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChunkingEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    public ChunkingEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 发送 chunk_started 事件。
     */
    public void publishChunkStarted(String fileId, String fileName, String gameKey) {
        ChunkingEvent event = new ChunkingEvent(
                ChunkingEvent.EventType.CHUNK_STARTED, fileId, fileName, gameKey
        );
        log.debug("[ChunkingEvent] Publishing: {}", event);
        eventPublisher.publishEvent(event);
    }

    /**
     * 发送 chunk_llm_completed 事件。
     */
    public void publishChunkLLMCompleted(String fileId, String fileName, String gameKey, 
                                          int chunkCount, long latencyMs) {
        ChunkingEvent event = new ChunkingEvent(
                ChunkingEvent.EventType.CHUNK_LLM_COMPLETED, fileId, fileName, gameKey
        )
        .withData("chunkCount", chunkCount)
        .withData("latencyMs", latencyMs)
        .withData("latencySeconds", latencyMs / 1000.0);
        
        log.info("[ChunkingEvent] LLM chunking completed: fileId={} chunks={} latency={}ms", 
                fileId, chunkCount, latencyMs);
        eventPublisher.publishEvent(event);
    }

    /**
     * 发送 chunk_schema_validated 事件。
     */
    public void publishChunkSchemaValidated(String fileId, String fileName, String gameKey,
                                             int validChunks, int invalidChunks) {
        ChunkingEvent event = new ChunkingEvent(
                ChunkingEvent.EventType.CHUNK_SCHEMA_VALIDATED, fileId, fileName, gameKey
        )
        .withData("validChunks", validChunks)
        .withData("invalidChunks", invalidChunks)
        .withData("totalChunks", validChunks + invalidChunks)
        .withData("validRate", validChunks == 0 ? 0 : 
                Math.round((double) validChunks / (validChunks + invalidChunks) * 10000) / 100.0);
        
        log.info("[ChunkingEvent] Schema validation: fileId={} valid={} invalid={}", 
                fileId, validChunks, invalidChunks);
        eventPublisher.publishEvent(event);
    }

    /**
     * 发送 chunk_fallback 事件。
     */
    public void publishChunkFallback(String fileId, String fileName, String gameKey,
                                      String reason, String fallbackMode) {
        ChunkingEvent event = new ChunkingEvent(
                ChunkingEvent.EventType.CHUNK_FALLBACK, fileId, fileName, gameKey
        )
        .withData("reason", reason)
        .withData("fallbackMode", fallbackMode);
        
        log.warn("[ChunkingEvent] Fallback to {}: fileId={} reason={}", 
                fallbackMode, fileId, reason);
        eventPublisher.publishEvent(event);
    }

    /**
     * 发送 chunk_completed 事件。
     */
    public void publishChunkCompleted(String fileId, String fileName, String gameKey,
                                       int totalChunks, String chunkMode, long totalLatencyMs) {
        ChunkingEvent event = new ChunkingEvent(
                ChunkingEvent.EventType.CHUNK_COMPLETED, fileId, fileName, gameKey
        )
        .withData("totalChunks", totalChunks)
        .withData("chunkMode", chunkMode)
        .withData("totalLatencyMs", totalLatencyMs)
        .withData("totalLatencySeconds", totalLatencyMs / 1000.0)
        .withData("throughput", totalLatencyMs > 0 
                ? Math.round((double) totalChunks / totalLatencyMs * 1000) 
                : 0);
        
        log.info("[ChunkingEvent] Chunking completed: fileId={} chunks={} mode={} latency={}ms",
                fileId, totalChunks, chunkMode, totalLatencyMs);
        eventPublisher.publishEvent(event);
    }
}
