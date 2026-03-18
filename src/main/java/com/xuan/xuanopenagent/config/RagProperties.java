package com.xuan.xuanopenagent.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

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

    /** When true, the advisor returns a model answer even when no chunks are retrieved. */
    private boolean advisorAllowEmptyContext = true;

    /** Maximum allowed upload file size in MB. */
    @Min(1)
    private int maxFileSizeMb = 20;

    /** File extensions accepted for knowledge base ingestion. */
    @NotEmpty
    private List<String> enabledFileTypes = List.of("txt", "md", "pdf");

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

    public boolean isAdvisorAllowEmptyContext() { return advisorAllowEmptyContext; }
    public void setAdvisorAllowEmptyContext(boolean advisorAllowEmptyContext) { this.advisorAllowEmptyContext = advisorAllowEmptyContext; }

    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }

    public List<String> getEnabledFileTypes() { return enabledFileTypes; }
    public void setEnabledFileTypes(List<String> enabledFileTypes) { this.enabledFileTypes = enabledFileTypes; }
}
