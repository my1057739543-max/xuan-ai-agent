package com.xuan.xuanopenagent.rag.chunking;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * LLM 优先切片输出的结构化 Schema。
 * 每个 chunk 对应一个技巧单元，包含完整的学习内容。
 */
public class LLMChunkingSchema {

    /**
     * 技巧标题，简洁明了。
     */
    @JsonProperty("title")
    private String title;

    /**
     * 游戏标识，必须与文档的 gameKey 一致。
     */
    @JsonProperty("gameKey")
    private String gameKey;

    /**
     * 所属话题或主题，用于内容分类。
     * 例如：策略、操作、装备、心态等。
     */
    @JsonProperty("topic")
    private String topic;

    /**
     * 执行步骤列表，描述技巧的具体操作步骤。
     * 可为空，但与 commonMistakes 至少一个非空。
     */
    @JsonProperty("steps")
    private List<String> steps;

    /**
     * 常见错误列表，描述初学者可能犯的错误及应对方法。
     * 可为空，但与 steps 至少一个非空。
     */
    @JsonProperty("commonMistakes")
    private List<String> commonMistakes;

    /**
     * 训练计划建议，针对该技巧的练习方案。
     * 可为空。
     */
    @JsonProperty("drillPlan")
    private String drillPlan;

    /**
     * 原始文本，由 chunk 实际内容组成，需在可 embedding 范围内。
     * 此字段用于向量存储的内容。
     */
    @JsonProperty("rawText")
    private String rawText;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGameKey() { return gameKey; }
    public void setGameKey(String gameKey) { this.gameKey = gameKey; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public List<String> getCommonMistakes() { return commonMistakes; }
    public void setCommonMistakes(List<String> commonMistakes) { this.commonMistakes = commonMistakes; }

    public String getDrillPlan() { return drillPlan; }
    public void setDrillPlan(String drillPlan) { this.drillPlan = drillPlan; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    /**
     * 校验 Schema 的有效性。
     * 
     * @return 如果有效返回 true，否则返回 false
     */
    public boolean isValid() {
        // 标题必填
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        
        // gameKey 必填且不为空
        if (gameKey == null || gameKey.trim().isEmpty()) {
            return false;
        }
        
        // 话题必填
        if (topic == null || topic.trim().isEmpty()) {
            return false;
        }
        
        // steps 和 commonMistakes 至少一个非空
        boolean hasSteps = steps != null && !steps.isEmpty();
        boolean hasCommonMistakes = commonMistakes != null && !commonMistakes.isEmpty();
        if (!hasSteps && !hasCommonMistakes) {
            return false;
        }
        
        // rawText 必填且不为空
        if (rawText == null || rawText.trim().isEmpty()) {
            return false;
        }
        
        return true;
    }

    /**
     * 获取校验错误描述。
     * 
     * @return 错误描述字符串，如果有效则返回空字符串
     */
    public String getValidationError() {
        if (title == null || title.trim().isEmpty()) {
            return "title is required";
        }
        
        if (gameKey == null || gameKey.trim().isEmpty()) {
            return "gameKey is required and must not be empty";
        }
        
        if (topic == null || topic.trim().isEmpty()) {
            return "topic is required";
        }
        
        boolean hasSteps = steps != null && !steps.isEmpty();
        boolean hasCommonMistakes = commonMistakes != null && !commonMistakes.isEmpty();
        if (!hasSteps && !hasCommonMistakes) {
            return "either steps or commonMistakes must be non-empty";
        }
        
        if (rawText == null || rawText.trim().isEmpty()) {
            return "rawText is required and must not be empty";
        }
        
        return "";
    }

    @Override
    public String toString() {
        return "LLMChunkingSchema{" +
                "title='" + title + '\'' +
                ", gameKey='" + gameKey + '\'' +
                ", topic='" + topic + '\'' +
                ", stepsCount=" + (steps != null ? steps.size() : 0) +
                ", mistakesCount=" + (commonMistakes != null ? commonMistakes.size() : 0) +
                ", rawTextLength=" + (rawText != null ? rawText.length() : 0) +
                '}';
    }
}
