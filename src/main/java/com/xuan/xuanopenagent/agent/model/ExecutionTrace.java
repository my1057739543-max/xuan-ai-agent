package com.xuan.xuanopenagent.agent.model;

import java.time.Instant;

public class ExecutionTrace {

    private int step;
    private AgentState state;
    private String eventType;
    private Object input;
    private Object output;
    private long latencyMs;
    private Instant timestamp;

    public static ExecutionTrace of(int step, AgentState state, String eventType, Object input, Object output, long latencyMs) {
        ExecutionTrace trace = new ExecutionTrace();
        trace.setStep(step);
        trace.setState(state);
        trace.setEventType(eventType);
        trace.setInput(input);
        trace.setOutput(output);
        trace.setLatencyMs(latencyMs);
        trace.setTimestamp(Instant.now());
        return trace;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}