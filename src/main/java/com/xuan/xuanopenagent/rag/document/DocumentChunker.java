package com.xuan.xuanopenagent.rag.document;

import com.xuan.xuanopenagent.config.RagProperties;
import com.xuan.xuanopenagent.rag.chunking.ChunkingConfig;
import com.xuan.xuanopenagent.rag.chunking.LLMDocumentTransformer;
import com.xuan.xuanopenagent.rag.chunking.event.ChunkingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DocumentChunker {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunker.class);

    private final RagProperties ragProperties;
    private final Optional<ChatClient> chunkingChatClient;
    private final ChunkingEventPublisher eventPublisher;

    public DocumentChunker(RagProperties ragProperties,
                           @Qualifier("chunkingChatClient") Optional<ChatClient> chunkingChatClient,
                           ChunkingEventPublisher eventPublisher) {
        this.ragProperties = ragProperties;
        this.chunkingChatClient = chunkingChatClient;
        this.eventPublisher = eventPublisher;
    }

    public List<Document> chunk(List<Document> sourceDocuments,
                                String fileId,
                                String fileName,
                                String sourceType,
                                String gameKey,
                                List<String> tags) {
        long totalStartTime = System.currentTimeMillis();
        
        // 发送切片开始事件
        eventPublisher.publishChunkStarted(fileId, fileName, gameKey);
        
        List<Document> chunks;
        String chunkMode = "rule";

        // 根据配置选择切片策略
        if ("llm_first".equalsIgnoreCase(ragProperties.getChunkStrategy()) && chunkingChatClient.isPresent()) {
            try {
                chunks = chunkWithLLM(sourceDocuments, fileId, fileName, gameKey);
                chunkMode = "llm";
                log.info("[RAG] fileId={} chunked with LLM. chunks={}", fileId, chunks.size());
            } catch (Exception e) {
                if (ragProperties.isChunkFallbackToRule()) {
                    log.warn("[RAG] fileId={} LLM chunking failed, falling back to rule-based", fileId, e);
                    eventPublisher.publishChunkFallback(fileId, fileName, gameKey, e.getMessage(), "rule_based");
                    chunks = chunkWithRules(sourceDocuments);
                    chunkMode = "rule";
                } else {
                    throw new RuntimeException("LLM chunking failed and fallback is disabled", e);
                }
            }
        } else {
            chunks = chunkWithRules(sourceDocuments);
            log.info("[RAG] fileId={} chunked with rule-based splitter. chunks={}", fileId, chunks.size());
        }

        long totalLatency = System.currentTimeMillis() - totalStartTime;
        
        // 发送切片完成事件
        eventPublisher.publishChunkCompleted(fileId, fileName, gameKey, chunks.size(), chunkMode, totalLatency);

        // 增强元数据
        return enrichChunks(chunks, fileId, fileName, sourceType, gameKey, tags, chunkMode);
    }

    /**
     * 使用 LLM 进行语义切片。
     */
    private List<Document> chunkWithLLM(List<Document> sourceDocuments, String fileId, String fileName, String gameKey) {
        long startTime = System.currentTimeMillis();
        
        ChunkingConfig config = new ChunkingConfig();
        config.setStrategy(ragProperties.getChunkStrategy());
        config.setLlmEnabled(ragProperties.isChunkLlmEnabled());
        config.setLlmModel(ragProperties.getChunkLlmModel());
        config.setLlmTimeoutSeconds(ragProperties.getChunkLlmTimeoutSeconds());
        config.setLlmMaxInputTokens(ragProperties.getChunkLlmMaxInputTokens());
        config.setLlmMaxOutputChunks(ragProperties.getChunkLlmMaxOutputChunks());
        config.setFallbackToRule(ragProperties.isChunkFallbackToRule());
        config.setMaxTokens(ragProperties.getChunkMaxTokens());
        config.setOverlapTokens(ragProperties.getChunkOverlapTokens());
        config.setGameKey(gameKey);
        config.setFileName(fileName);
        config.setFileId(fileId);

        // 创建 LLM 转换器并应用
        LLMDocumentTransformer transformer = new LLMDocumentTransformer(chunkingChatClient.get(), config,
                new LLMDocumentTransformer.ChunkingEventListener() {
                    @Override
                    public void onChunkStarted(String fid, String fname) {
                        // 已由 DocumentChunker 处理
                    }

                    @Override
                    public void onChunkLLMCompleted(String fid, int chunkCount, long latencyMs) {
                        eventPublisher.publishChunkLLMCompleted(fid, fileName, gameKey, chunkCount, latencyMs);
                    }

                    @Override
                    public void onChunkSchemaValidated(String fid, int validChunks, int invalidChunks) {
                        eventPublisher.publishChunkSchemaValidated(fid, fileName, gameKey, validChunks, invalidChunks);
                    }

                    @Override
                    public void onChunkFallback(String fid, String reason) {
                        eventPublisher.publishChunkFallback(fid, fileName, gameKey, reason, "rule_based");
                    }

                    @Override
                    public void onChunkCompleted(String fid, int totalChunks, String mode, long totalLatencyMs) {
                        // 由 DocumentChunker 处理最终完成事件
                    }
                });
        
        List<Document> chunks = transformer.apply(sourceDocuments);
        long latency = System.currentTimeMillis() - startTime;
        
        return chunks;
    }

    /**
     * 使用规则切片作为回退或主要策略。
     */
    private List<Document> chunkWithRules(List<Document> sourceDocuments) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(ragProperties.getChunkSize())
                .withMinChunkSizeChars(ragProperties.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(ragProperties.getMinChunkLengthToEmbed())
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        return splitter.apply(sourceDocuments);
    }

    /**
     * 增强 chunk 元数据，补充 fileId、gameKey 等字段。
     */
    private List<Document> enrichChunks(List<Document> chunks, String fileId, String fileName,
                                        String sourceType, String gameKey, List<String> tags, String chunkMode) {
        List<Document> enriched = new ArrayList<>(chunks.size());
        String uploadTime = Instant.now().toString();

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            
            // 补充基础元数据
            metadata.put("fileId", fileId);
            metadata.put("fileName", fileName);
            metadata.put("gameKey", gameKey);
            metadata.put("sourceType", sourceType);
            metadata.put("uploadTime", uploadTime);
            
            // 确保 chunkMode 被设置（除非已由 LLM 处理）
            if (!metadata.containsKey("chunkMode")) {
                metadata.put("chunkMode", chunkMode);
            }
            
            // chunkIndex 仅在没有时才设置（LLM 模式可能已处理）
            if (!metadata.containsKey("chunkIndex")) {
                metadata.put("chunkIndex", i);
            }
            
            if (tags != null && !tags.isEmpty()) {
                metadata.put("tags", tags);
            }

            enriched.add(chunk.mutate().metadata(metadata).build());
        }

        return enriched;
    }
}
