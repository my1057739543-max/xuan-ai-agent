package com.xuan.xuanopenagent.agent;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.config.AgentProperties;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class BaseAgent {

	protected final AgentProperties agentProperties;

	protected BaseAgent(AgentProperties agentProperties) {
		this.agentProperties = Objects.requireNonNull(agentProperties, "agentProperties must not be null");
	}

	public String run(AgentContext context, Consumer<AgentEvent> emitter) {
		Objects.requireNonNull(context, "context must not be null");
		Consumer<AgentEvent> safeEmitter = emitter == null ? ignored -> { } : emitter;
		long deadlineMillis = System.currentTimeMillis() + (agentProperties.getToolTimeoutSeconds() * 1000L);

		try {
			return doRun(context, safeEmitter, deadlineMillis);
		} catch (Exception ex) {
			Map<String, Object> payload = new HashMap<>();
			payload.put("message", ex.getMessage());
			payload.put("state", "FAILED");
			payload.put("timestamp", Instant.now().toString());
			safeEmitter.accept(AgentEvent.of(
					context.getTraceId(),
					context.getSessionId(),
					context.getCurrentStep(),
					"error",
					payload
			));
			return "Agent failed: " + ex.getMessage();
			//all
		}
	}

	protected boolean isTimedOut(long deadlineMillis) {
		return System.currentTimeMillis() > deadlineMillis;
	}

	protected String maxStepsMessage() {
		return "已达到最大思考步数(" + agentProperties.getMaxSteps() + ")，请补充信息后重试。";
	}

	protected abstract String doRun(AgentContext context, Consumer<AgentEvent> emitter, long deadlineMillis);
}
