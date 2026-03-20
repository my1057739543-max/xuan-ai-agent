package com.xuan.xuanopenagent.agent;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.config.AgentProperties;
import com.xuan.xuanopenagent.tools.ToolRegistry;

import java.util.Objects;

public abstract class ToolCallAgent extends ReActAgent {

	protected final ToolRegistry toolRegistry;

	protected ToolCallAgent(AgentProperties agentProperties, ToolRegistry toolRegistry) {
		super(agentProperties);
		this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry must not be null");
	}

	@Override
	protected String observeToolResult(AgentContext context, Decision decision, String thought) {
		String toolInput = context.getMessage();
		if (context.getHistory() != null && !context.getHistory().isEmpty()) {
			String firstUserMessage = context.getHistory().get(0);
			if (firstUserMessage != null && !firstUserMessage.isBlank()) {
				toolInput = firstUserMessage;
			}
		}
		return toolRegistry.invoke(decision.content(), toolInput);
	}
}
