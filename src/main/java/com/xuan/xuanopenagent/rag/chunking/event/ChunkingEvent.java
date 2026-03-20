package com.xuan.xuanopenagent.rag.chunking.event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * LLM 切片过程中的事件。
 * 包含事件类型、元数据、时间戳等信息。
 */
public class ChunkingEvent {

    /**
     * 事件类型枚举。
     */
    public enum EventType {
        CHUNK_STARTED("chunk_started", "切片开始"),
        CHUNK_LLM_COMPLETED("chunk_llm_completed", "LLM 切片完成"),
        CHUNK_SCHEMA_VALIDATED("chunk_schema_validated", "Schema 校验完成"),
        CHUNK_FALLBACK("chunk_fallback", "降级到规则切片"),
        CHUNK_COMPLETED("chunk_completed", "切片完全完成");

        private final String code;
        private final String description;

        EventType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    private EventType eventType;
    private String fileId;
    private String fileName;
    private String gameKey;
    private LocalDateTime timestamp;
    private Map<String, Object> data = new HashMap<>();

    public ChunkingEvent(EventType eventType, String fileId, String fileName, String gameKey) {
        this.eventType = eventType;
        this.fileId = fileId;
        this.fileName = fileName;
        this.gameKey = gameKey;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public EventType getEventType() { return eventType; }
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getGameKey() { return gameKey; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }

    public ChunkingEvent withData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public ChunkingEvent withData(Map<String, Object> data) {
        this.data.putAll(data);
        return this;
    }

    @Override
    public String toString() {
        return "ChunkingEvent{" +
                "type=" + eventType.code +
                ", fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", gameKey='" + gameKey + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
}
