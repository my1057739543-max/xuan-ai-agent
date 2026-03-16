package com.xuan.xuanopenagent.agent.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AgentEvent {

    private String traceId;
    private String sessionId;
    private int step;
    private String type;
    private Instant timestamp;
    private Map<String, Object> payload = new LinkedHashMap<>();

    public static AgentEvent of(String traceId, String sessionId, int step, String type, Map<String, Object> payload) {
        AgentEvent event = new AgentEvent();
        event.setTraceId(traceId);
        event.setSessionId(sessionId);
        event.setStep(step);
        event.setType(type);
        event.setTimestamp(Instant.now());
        if (payload != null) {
            event.setPayload(new LinkedHashMap<>(payload));
        }
        return event;
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

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}