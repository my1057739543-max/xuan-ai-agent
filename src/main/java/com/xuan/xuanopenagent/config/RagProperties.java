package com.xuan.xuanopenagent.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * Business-level RAG configuration.
 * PgVector storage parameters live under spring.ai.vectorstore.pgvector.
 * Chunk / retrieval / upload parameters live here under xuan.rag.
 */
@Validated
@ConfigurationProperties(prefix = "xuan.rag")
public class RagProperties {

    /** Local directory where uploaded knowledge files are stored. */
    @NotBlank
    private String uploadDir = "./data/uploads";

    /** Number of chunks to retrieve per query (TopK). */
    @Min(1)
    @Max(50)
    private int topK = 4;

    /** Minimum cosine similarity for a chunk to be included in results. */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double similarityThreshold = 0.5;

    /** Target token chunk size for TokenTextSplitter. */
    @Min(100)
    private int chunkSize = 800;

    /** Minimum chunk character length fed to TokenTextSplitter. */
    @Min(10)
    private int minChunkSizeChars = 350;

    /** Chunks shorter than this are discarded before embedding. */
    @Min(1)
    private int minChunkLengthToEmbed = 10;

    /** Maximum number of texts per embedding request batch. */
    @Min(1)
    @Max(50)
    private int embeddingBatchSize = 10;

    /** When true, the advisor returns a model answer even when no chunks are retrieved. */
    private boolean advisorAllowEmptyContext = true;

    /** Maximum allowed upload file size in MB. */
    @Min(1)
    private int maxFileSizeMb = 20;

    /** File extensions accepted for knowledge base ingestion. */
    @NotEmpty
    private List<String> enabledFileTypes = List.of("txt", "md", "pdf");

        /** When true, retrieval is scoped by gameKey by default. */
        private boolean gameIsolationEnabled = true;

        /** Supported normalized game keys. */
        @NotEmpty
        private List<String> supportedGameKeys = List.of("valorant", "cs2", "apex", "lol");

        /** Alias map used by query resolver, e.g. "瓦" -> "valorant". */
        private Map<String, String> gameAliasMap = Map.of(
            "瓦", "valorant",
            "瓦罗兰特", "valorant",
            "valorant", "valorant",
            "cs", "cs2",
            "cs2", "cs2",
            "反恐精英2", "cs2",
            "apex", "apex",
            "apex legends", "apex",
            "lol", "lol",
            "英雄联盟", "lol"
        );

        /** Optional fallback game key when no explicit/detected game can be resolved. */
        private String defaultGameKey;

    // ---- LLM Chunking Configuration (M2) ----

    /** Chunking strategy: "llm_first" or "rule_only". */
    private String chunkStrategy = "llm_first";

    /** Whether to enable LLM-based chunking. */
    private boolean chunkLlmEnabled = true;

    /** Model name for LLM chunking, e.g. "deepseek-chat". */
    private String chunkLlmModel = "deepseek-chat";

    /** LLM chunking timeout in seconds. */
    @Min(1)
    private long chunkLlmTimeoutSeconds = 20;

    /** Maximum input tokens for LLM chunking. */
    @Min(1000)
    private int chunkLlmMaxInputTokens = 6000;

    /** Maximum number of chunks that can be generated per LLM call. */
    @Min(1)
    private int chunkLlmMaxOutputChunks = 24;

    /** Whether to fallback to rule-based chunking if LLM fails. */
    private boolean chunkFallbackToRule = true;

    /** Maximum tokens per chunk (for rule-based fallback and safety trimming). */
    @Min(200)
    @Max(2000)
    private int chunkMaxTokens = 700;

    /** Overlap tokens between chunks to maintain context continuity. */
    @Min(0)
    @Max(200)
    private int chunkOverlapTokens = 80;

    // ---- getters & setters ----

    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getMinChunkSizeChars() { return minChunkSizeChars; }
    public void setMinChunkSizeChars(int minChunkSizeChars) { this.minChunkSizeChars = minChunkSizeChars; }

    public int getMinChunkLengthToEmbed() { return minChunkLengthToEmbed; }
    public void setMinChunkLengthToEmbed(int minChunkLengthToEmbed) { this.minChunkLengthToEmbed = minChunkLengthToEmbed; }

    public int getEmbeddingBatchSize() { return embeddingBatchSize; }
    public void setEmbeddingBatchSize(int embeddingBatchSize) { this.embeddingBatchSize = embeddingBatchSize; }

    public boolean isAdvisorAllowEmptyContext() { return advisorAllowEmptyContext; }
    public void setAdvisorAllowEmptyContext(boolean advisorAllowEmptyContext) { this.advisorAllowEmptyContext = advisorAllowEmptyContext; }

    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }

    public List<String> getEnabledFileTypes() { return enabledFileTypes; }
    public void setEnabledFileTypes(List<String> enabledFileTypes) { this.enabledFileTypes = enabledFileTypes; }

    public boolean isGameIsolationEnabled() { return gameIsolationEnabled; }
    public void setGameIsolationEnabled(boolean gameIsolationEnabled) { this.gameIsolationEnabled = gameIsolationEnabled; }

    public List<String> getSupportedGameKeys() { return supportedGameKeys; }
    public void setSupportedGameKeys(List<String> supportedGameKeys) { this.supportedGameKeys = supportedGameKeys; }

    public Map<String, String> getGameAliasMap() { return gameAliasMap; }
    public void setGameAliasMap(Map<String, String> gameAliasMap) { this.gameAliasMap = gameAliasMap; }

    public String getDefaultGameKey() { return defaultGameKey; }
    public void setDefaultGameKey(String defaultGameKey) { this.defaultGameKey = defaultGameKey; }

    public String getChunkStrategy() { return chunkStrategy; }
    public void setChunkStrategy(String chunkStrategy) { this.chunkStrategy = chunkStrategy; }

    public boolean isChunkLlmEnabled() { return chunkLlmEnabled; }
    public void setChunkLlmEnabled(boolean chunkLlmEnabled) { this.chunkLlmEnabled = chunkLlmEnabled; }

    public String getChunkLlmModel() { return chunkLlmModel; }
    public void setChunkLlmModel(String chunkLlmModel) { this.chunkLlmModel = chunkLlmModel; }

    public long getChunkLlmTimeoutSeconds() { return chunkLlmTimeoutSeconds; }
    public void setChunkLlmTimeoutSeconds(long chunkLlmTimeoutSeconds) { this.chunkLlmTimeoutSeconds = chunkLlmTimeoutSeconds; }

    public int getChunkLlmMaxInputTokens() { return chunkLlmMaxInputTokens; }
    public void setChunkLlmMaxInputTokens(int chunkLlmMaxInputTokens) { this.chunkLlmMaxInputTokens = chunkLlmMaxInputTokens; }

    public int getChunkLlmMaxOutputChunks() { return chunkLlmMaxOutputChunks; }
    public void setChunkLlmMaxOutputChunks(int chunkLlmMaxOutputChunks) { this.chunkLlmMaxOutputChunks = chunkLlmMaxOutputChunks; }

    public boolean isChunkFallbackToRule() { return chunkFallbackToRule; }
    public void setChunkFallbackToRule(boolean chunkFallbackToRule) { this.chunkFallbackToRule = chunkFallbackToRule; }

    public int getChunkMaxTokens() { return chunkMaxTokens; }
    public void setChunkMaxTokens(int chunkMaxTokens) { this.chunkMaxTokens = chunkMaxTokens; }

    public int getChunkOverlapTokens() { return chunkOverlapTokens; }
    public void setChunkOverlapTokens(int chunkOverlapTokens) { this.chunkOverlapTokens = chunkOverlapTokens; }
}
