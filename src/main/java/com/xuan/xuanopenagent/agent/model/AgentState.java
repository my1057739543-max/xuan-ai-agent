package com.xuan.xuanopenagent.agent.model;

public enum AgentState {
    INIT,
    PLANNING,
    THINKING,
    TOOL_DECISION,
    TOOL_RUNNING,
    OBSERVING,
    RESPONDING,
    TERMINATED,
    ERROR,
    FAILED
}