package com.xuan.xuanopenagent.agent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentContext {

    private String traceId;
    private String sessionId;
    private String userId;
    private String message;
    private List<String> history;
    private Integer requestedMaxSteps;
    private int toolCallCount;
    private int currentStep;
    private List<ExecutionTrace> executionTraces;

    public AgentContext() {
        this.traceId = UUID.randomUUID().toString();
        this.history = new ArrayList<>();
        this.executionTraces = new ArrayList<>();
    }

    public static AgentContext initialize(String sessionId, String userId, String message) {
        AgentContext context = new AgentContext();
        context.setSessionId(sessionId);
        context.setUserId(userId);
        context.setMessage(message);
        context.addHistory(message);
        return context;
    }

    public int advanceStep() {
        currentStep += 1;
        return currentStep;
    }

    public int incrementToolCallCount() {
        toolCallCount += 1;
        return toolCallCount;
    }

    public void addHistory(String entry) {
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(entry);
    }

    public void addExecutionTrace(ExecutionTrace trace) {
        if (executionTraces == null) {
            executionTraces = new ArrayList<>();
        }
        executionTraces.add(trace);
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

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

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }

    public int getToolCallCount() {
        return toolCallCount;
    }

    public Integer getRequestedMaxSteps() {
        return requestedMaxSteps;
    }

    public void setRequestedMaxSteps(Integer requestedMaxSteps) {
        this.requestedMaxSteps = requestedMaxSteps;
    }

    public void setToolCallCount(int toolCallCount) {
        this.toolCallCount = toolCallCount;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public List<ExecutionTrace> getExecutionTraces() {
        return executionTraces;
    }

    public void setExecutionTraces(List<ExecutionTrace> executionTraces) {
        this.executionTraces = executionTraces;
    }
}