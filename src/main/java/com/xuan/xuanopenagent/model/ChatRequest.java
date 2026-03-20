package com.xuan.xuanopenagent.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String userId;

    @NotBlank
    private String message;

    @Valid
    private ChatOptions options = new ChatOptions();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ChatOptions getOptions() {
        return options;
    }

    public void setOptions(ChatOptions options) {
        this.options = options;
    }

    public static class ChatOptions {

        private Integer maxSteps;
        private Double temperature;
        private Boolean useKnowledgeBase = false;
        private String fileIdFilter;
        private String gameKey;

        public Integer getMaxSteps() {
            return maxSteps;
        }

        public void setMaxSteps(Integer maxSteps) {
            this.maxSteps = maxSteps;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Boolean getUseKnowledgeBase() {
            return useKnowledgeBase;
        }

        public void setUseKnowledgeBase(Boolean useKnowledgeBase) {
            this.useKnowledgeBase = useKnowledgeBase;
        }

        public String getFileIdFilter() {
            return fileIdFilter;
        }

        public void setFileIdFilter(String fileIdFilter) {
            this.fileIdFilter = fileIdFilter;
        }

        public String getGameKey() {
            return gameKey;
        }

        public void setGameKey(String gameKey) {
            this.gameKey = gameKey;
        }
    }
}