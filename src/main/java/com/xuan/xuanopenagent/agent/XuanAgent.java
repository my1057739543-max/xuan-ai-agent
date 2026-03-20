package com.xuan.xuanopenagent.agent;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.config.AgentProperties;
import com.xuan.xuanopenagent.tools.ToolRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class XuanAgent extends ToolCallAgent {

	private final ChatClient chatClient;

	public XuanAgent(ChatClient xuanAgentChatClient, AgentProperties agentProperties, ToolRegistry toolRegistry) {
		super(agentProperties, toolRegistry);
		this.chatClient = xuanAgentChatClient;
	}

	@Override
	protected String think(AgentContext context) {
		String systemPrompt = buildSystemPrompt("thought");
		String prompt = """
				你是一个 ReAct Agent。
				当前目标：%s
				当前步骤：%d
				历史：%s

				请输出简短 thought，说明下一步要做什么。
				""".formatted(context.getMessage(), context.getCurrentStep(), historyToText(context.getHistory()));

		return callModel(systemPrompt, prompt);
	}

	@Override
	protected Decision decide(AgentContext context, String thought) {
		String systemPrompt = buildSystemPrompt("decision");
		String availableTools = String.join(", ", toolRegistry.getRegisteredToolNames());
		String prompt = """
				你是一个 ReAct Agent 的决策器。
				用户目标：%s
				当前 thought：%s
				可用工具：%s

				你只能输出以下三种动作之一，严格按两行格式输出：
				ACTION: RESPOND 或 TOOL 或 TERMINATE
				CONTENT: 具体内容

				说明：
				- 如果可以直接回答，ACTION=RESPOND，CONTENT 写回答摘要。
				- 如果需要工具，ACTION=TOOL，CONTENT 写工具名（必须来自可用工具）。
				- 如果涉及联网搜索，优先选择名称包含 text_search 或 web_search 的工具。
				- 如果问题包含“附近、周边、离某地近”等位置检索语义，优先先用 maps_geo 获取地点坐标，再用 maps_around_search 搜索周边 POI。
				- 如果是天气问题，优先使用 maps_weather。
				- 如果任务应结束，ACTION=TERMINATE，CONTENT 写结束理由。
				""".formatted(context.getMessage(), thought, availableTools);

		String raw = callModel(systemPrompt, prompt);
		return parseDecision(raw);
	}

	@Override
	protected String generateFinalResponse(AgentContext context, String thought) {
		String systemPrompt = buildSystemPrompt("final");
		String prompt = """
				请根据用户问题和当前上下文生成最终回复。
				用户问题：%s
				latest_thought：%s
				历史上下文：%s

				要求：
				- 用中文回复
				- 直接给结论
				- 结构简洁
				- 最终回答必须是纯文本，不要使用 Markdown 格式
				- 不要输出 **、#、```、>、- 这类 Markdown 标记
				- 如需分点，可用 1. 2. 3. 的纯文本编号
				- 若问题与游戏技巧无关，明确说明你专注游戏领域，并引导用户给出游戏相关问题
				""".formatted(context.getMessage(), thought, historyToText(context.getHistory()));
		return callModel(systemPrompt, prompt);
	}

	private String callModel(String systemPrompt, String userPrompt) {
		String content = chatClient.prompt()
				.system(systemPrompt)
				.user(userPrompt)
				.call()
				.content();
		return content == null ? "" : content.trim();
	}

	private String buildSystemPrompt(String stage) {
		return agentProperties.getSystemPrompt() + "\n"
				+ "你必须遵循：\n"
				+ "1) 仅处理游戏相关内容（玩法、技巧、训练、配置、战术、角色理解）。\n"
				+ "2) 对非游戏问题简洁拒答并引导到游戏问题。\n"
				+ "3) 回答优先给可执行步骤，避免空泛表述。\n"
				+ "当前阶段=" + stage;
	}

	private String historyToText(List<String> history) {
		if (history == null || history.isEmpty()) {
			return "[]";
		}
		return String.join(" | ", history);
	}

	private Decision parseDecision(String modelOutput) {
		if (modelOutput == null || modelOutput.isBlank()) {
			return Decision.respond("模型未输出决策，转为直接回复");
		}

		String upper = modelOutput.toUpperCase();
		String content = extractContent(modelOutput);

		if (upper.contains("ACTION: TOOL")) {
			return Decision.tool(content.isBlank() ? "unknown_tool" : content);
		}
		if (upper.contains("ACTION: TERMINATE")) {
			return Decision.terminate(content.isBlank() ? "任务结束" : content);
		}
		return Decision.respond(content.isBlank() ? "开始直接回答" : content);
	}

	private String extractContent(String modelOutput) {
		String[] lines = modelOutput.split("\\R");
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.toUpperCase().startsWith("CONTENT:")) {
				return trimmed.substring("CONTENT:".length()).trim();
			}
		}
		return "";
	}
}
