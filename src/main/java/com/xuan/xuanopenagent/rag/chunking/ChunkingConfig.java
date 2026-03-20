package com.xuan.xuanopenagent.rag.chunking;

/**
 * LLM 切片的运行时配置对象。
 * 包含整个切片过程所需的参数。
 */
public class ChunkingConfig {

    /**
     * 切片策略：llm_first（优先使用LLM）或 rule_only（仅使用规则切片）。
     */
    private String strategy;

    /**
     * 是否启用 LLM 切片。
     */
    private boolean llmEnabled;

    /**
     * LLM 模型名称，例如 "deepseek-chat"。
     */
    private String llmModel;

    /**
     * LLM 调用超时时间（秒）。
     */
    private long llmTimeoutSeconds;

    /**
     * LLM 最大输入 token 数，超过则进行文本提前切分。
     */
    private int llmMaxInputTokens;

    /**
     * LLM 最大输出 chunk 数，用于控制一次调用的输出规模。
     */
    private int llmMaxOutputChunks;

    /**
     * LLM 失败时是否回退到规则切片。
     */
    private boolean fallbackToRule;

    /**
     * 单个 chunk 的最大 token 数（用于规则切片和安全裁剪）。
     */
    private int maxTokens;

    /**
     * chunk 之间的重叠 token 数，用于保证上下文连贯性。
     */
    private int overlapTokens;

    /**
     * 当前处理文件的 gameKey。
     */
    private String gameKey;

    /**
     * 当前处理文件的原始名称（用于日志和事件）。
     */
    private String fileName;

    /**
     * 当前处理文件的 ID（用于日志和事件）。
     */
    private String fileId;

    // Constructors
    public ChunkingConfig() {}

    public ChunkingConfig(String strategy, boolean llmEnabled, String llmModel, 
                          long llmTimeoutSeconds, int llmMaxInputTokens, int llmMaxOutputChunks,
                          boolean fallbackToRule, int maxTokens, int overlapTokens) {
        this.strategy = strategy;
        this.llmEnabled = llmEnabled;
        this.llmModel = llmModel;
        this.llmTimeoutSeconds = llmTimeoutSeconds;
        this.llmMaxInputTokens = llmMaxInputTokens;
        this.llmMaxOutputChunks = llmMaxOutputChunks;
        this.fallbackToRule = fallbackToRule;
        this.maxTokens = maxTokens;
        this.overlapTokens = overlapTokens;
    }

    // Getters and Setters
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public boolean isLlmEnabled() { return llmEnabled; }
    public void setLlmEnabled(boolean llmEnabled) { this.llmEnabled = llmEnabled; }

    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }

    public long getLlmTimeoutSeconds() { return llmTimeoutSeconds; }
    public void setLlmTimeoutSeconds(long llmTimeoutSeconds) { this.llmTimeoutSeconds = llmTimeoutSeconds; }

    public int getLlmMaxInputTokens() { return llmMaxInputTokens; }
    public void setLlmMaxInputTokens(int llmMaxInputTokens) { this.llmMaxInputTokens = llmMaxInputTokens; }

    public int getLlmMaxOutputChunks() { return llmMaxOutputChunks; }
    public void setLlmMaxOutputChunks(int llmMaxOutputChunks) { this.llmMaxOutputChunks = llmMaxOutputChunks; }

    public boolean isFallbackToRule() { return fallbackToRule; }
    public void setFallbackToRule(boolean fallbackToRule) { this.fallbackToRule = fallbackToRule; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getOverlapTokens() { return overlapTokens; }
    public void setOverlapTokens(int overlapTokens) { this.overlapTokens = overlapTokens; }

    public String getGameKey() { return gameKey; }
    public void setGameKey(String gameKey) { this.gameKey = gameKey; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    @Override
    public String toString() {
        return "ChunkingConfig{" +
                "strategy='" + strategy + '\'' +
                ", llmEnabled=" + llmEnabled +
                ", llmModel='" + llmModel + '\'' +
                ", llmTimeoutSeconds=" + llmTimeoutSeconds +
                ", maxTokens=" + maxTokens +
                ", gameKey='" + gameKey + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileId='" + fileId + '\'' +
                '}';
    }
}
