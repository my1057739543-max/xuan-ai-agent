package com.xuan.xuanopenagent.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "xuan.agent")
public class AgentProperties {

    @Min(1)
    private int maxSteps = 20;

    @Min(1)
    private int maxToolCalls = 10;

    @Min(1)
    private int toolTimeoutSeconds = 180;

    @NotBlank
    private String modelName = "deepseek-chat";

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(int maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public int getToolTimeoutSeconds() {
        return toolTimeoutSeconds;
    }

    public void setToolTimeoutSeconds(int toolTimeoutSeconds) {
        this.toolTimeoutSeconds = toolTimeoutSeconds;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}