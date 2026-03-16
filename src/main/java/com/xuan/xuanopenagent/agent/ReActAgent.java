package com.xuan.xuanopenagent.agent;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.agent.model.AgentState;
import com.xuan.xuanopenagent.agent.model.ExecutionTrace;
import com.xuan.xuanopenagent.config.AgentProperties;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class ReActAgent extends BaseAgent {

    protected ReActAgent(AgentProperties agentProperties) {
	super(agentProperties);
    }

    @Override
    protected String doRun(AgentContext context, Consumer<AgentEvent> emitter, long deadlineMillis) {
		int maxSteps = agentProperties.getMaxSteps();

	emit(emitter, context, context.getCurrentStep(), "plan", Map.of(
		"state", AgentState.INIT.name(),
		"message", "开始执行 ReAct 流程",
		"maxSteps", maxSteps
	));

	while (context.getCurrentStep() < maxSteps) {
	    if (isTimedOut(deadlineMillis)) {
		String timeoutMsg = "执行超时，任务终止。";
		emit(emitter, context, context.getCurrentStep(), "error", Map.of(
			"state", AgentState.FAILED.name(),
			"message", timeoutMsg
		));
		emit(emitter, context, context.getCurrentStep(), "done", Map.of("reason", "timeout"));
		return timeoutMsg;
	    }

	    int step = context.advanceStep();
	    long stepStart = System.currentTimeMillis();

	    String thought = think(context);
	    context.addExecutionTrace(ExecutionTrace.of(
		    step,
		    AgentState.THINKING,
		    "thought",
		    context.getMessage(),
		    thought,
		    System.currentTimeMillis() - stepStart
	    ));
	    emit(emitter, context, step, "thought", Map.of(
		    "state", AgentState.THINKING.name(),
		    "content", thought
	    ));

	    Decision decision = decide(context, thought);
	    context.addExecutionTrace(ExecutionTrace.of(
		    step,
		    AgentState.TOOL_DECISION,
		    "decision",
		    thought,
		    decision.action().name(),
		    System.currentTimeMillis() - stepStart
	    ));

	    if (decision.action() == Action.RESPOND) {
		String finalAnswer = generateFinalResponse(context, thought);
		context.addHistory(finalAnswer);
		emit(emitter, context, step, "final", Map.of(
			"state", AgentState.RESPONDING.name(),
			"content", finalAnswer
		));
		emit(emitter, context, step, "done", Map.of(
			"state", AgentState.TERMINATED.name(),
			"reason", "responded"
		));
		return finalAnswer;
	    }

	    if (decision.action() == Action.TERMINATE) {
		String finalAnswer = decision.content() == null || decision.content().isBlank()
			? "任务已结束。"
			: decision.content();
		emit(emitter, context, step, "final", Map.of(
			"state", AgentState.TERMINATED.name(),
			"content", finalAnswer
		));
		emit(emitter, context, step, "done", Map.of(
			"state", AgentState.TERMINATED.name(),
			"reason", "model_terminated"
		));
		return finalAnswer;
	    }

	    if (context.getToolCallCount() >= agentProperties.getMaxToolCalls()) {
		String toolLimitMsg = "已达到工具调用上限(" + agentProperties.getMaxToolCalls() + ")，任务终止。";
		emit(emitter, context, step, "error", Map.of(
			"state", AgentState.FAILED.name(),
			"message", toolLimitMsg
		));
		emit(emitter, context, step, "done", Map.of(
			"state", AgentState.FAILED.name(),
			"reason", "tool_limit"
		));
		return toolLimitMsg;
	    }

	    context.incrementToolCallCount();
	    String toolName = decision.content() == null || decision.content().isBlank() ? "unknown_tool" : decision.content();
	    emit(emitter, context, step, "tool_call", Map.of(
		    "state", AgentState.TOOL_RUNNING.name(),
		    "toolName", toolName
	    ));

	    String observation = observeToolResult(context, decision, thought);
	    context.addHistory("tool_result: " + observation);
	    context.addExecutionTrace(ExecutionTrace.of(
		    step,
		    AgentState.OBSERVING,
		    "tool_result",
		    toolName,
		    observation,
		    System.currentTimeMillis() - stepStart
	    ));
	    emit(emitter, context, step, "tool_result", Map.of(
		    "state", AgentState.OBSERVING.name(),
		    "toolName", toolName,
		    "content", observation
	    ));
	}

	String maxStepsResult = maxStepsMessage();
	emit(emitter, context, context.getCurrentStep(), "final", Map.of(
		"state", AgentState.TERMINATED.name(),
		"content", maxStepsResult
	));
	emit(emitter, context, context.getCurrentStep(), "done", Map.of(
		"state", AgentState.TERMINATED.name(),
		"reason", "max_steps"
	));
	return maxStepsResult;
    }

    protected abstract String think(AgentContext context);

    protected abstract Decision decide(AgentContext context, String thought);

    protected abstract String generateFinalResponse(AgentContext context, String thought);

    protected String observeToolResult(AgentContext context, Decision decision, String thought) {
	return "工具调用已记录，等待模块 03 接入真实工具。";
    }

    protected void emit(Consumer<AgentEvent> emitter, AgentContext context, int step, String type, Map<String, Object> payload) {
	Map<String, Object> mutablePayload = new LinkedHashMap<>(payload);
	mutablePayload.put("timestamp", Instant.now().toString());
	emitter.accept(AgentEvent.of(context.getTraceId(), context.getSessionId(), step, type, mutablePayload));
    }

    protected enum Action {
	RESPOND,
	TOOL,
	TERMINATE
    }

    protected record Decision(Action action, String content) {
	public static Decision respond(String content) {
	    return new Decision(Action.RESPOND, content);
	}

	public static Decision tool(String toolName) {
	    return new Decision(Action.TOOL, toolName);
	}

	public static Decision terminate(String reasonOrAnswer) {
	    return new Decision(Action.TERMINATE, reasonOrAnswer);
	}
    }
}
