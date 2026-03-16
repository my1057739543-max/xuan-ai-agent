package com.xuan.xuanopenagent.service;

import com.xuan.xuanopenagent.agent.XuanAgent;
import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.config.AgentProperties;
import com.xuan.xuanopenagent.model.ChatRequest;
import com.xuan.xuanopenagent.tools.TerminateTool;
import com.xuan.xuanopenagent.tools.TimeGetTool;
import com.xuan.xuanopenagent.tools.ToolRegistry;
import com.xuan.xuanopenagent.tools.WebSearchTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AgentServiceTest {

    @Test
    void shouldListToolsFromRegistry() {
        System.out.println("[TEST-START] shouldListToolsFromRegistry");
        AgentService agentService = new AgentService(
                scriptedAgent(),
            new ToolRegistry(new TimeGetTool(), new TerminateTool(), new WebSearchTool(), Optional.empty())
        );

        List<String> tools = agentService.listTools();
        System.out.println("[TOOLS] => " + tools);

        assertThat(tools).contains("time_get", "terminate_tool", "web_search");
        System.out.println("[TEST-PASS] shouldListToolsFromRegistry");
    }

    @Test
    void shouldAcceptChatRequestAndReturnEmitter() {
        System.out.println("[TEST-START] shouldAcceptChatRequestAndReturnEmitter");
        AgentService agentService = new AgentService(
                scriptedAgent(),
            new ToolRegistry(new TimeGetTool(), new TerminateTool(), new WebSearchTool(), Optional.empty())
        );

        ChatRequest request = new ChatRequest();
        request.setSessionId("sse-session-01");
        request.setUserId("u-01");
        request.setMessage("现在几点");

        ChatRequest.ChatOptions options = new ChatRequest.ChatOptions();
        options.setMaxSteps(2);
        request.setOptions(options);

        assertThat(agentService.streamChat(request)).isNotNull();
        System.out.println("[TEST-PASS] shouldAcceptChatRequestAndReturnEmitter");
    }

    private XuanAgent scriptedAgent() {
        AgentProperties properties = new AgentProperties();
        properties.setMaxSteps(4);
        properties.setMaxToolCalls(2);
        properties.setToolTimeoutSeconds(180);

        ToolRegistry toolRegistry = new ToolRegistry(new TimeGetTool(), new TerminateTool(), new WebSearchTool(), Optional.empty());
        return new XuanAgent((ChatClient) null, properties, toolRegistry) {

            @Override
            protected String think(AgentContext context) {
                return "scripted thought";
            }

            @Override
            protected Decision decide(AgentContext context, String thought) {
                return Decision.respond("scripted respond");
            }

            @Override
            protected String generateFinalResponse(AgentContext context, String thought) {
                return "scripted final";
            }

            @Override
            public String run(AgentContext context, java.util.function.Consumer<AgentEvent> emitter) {
                return super.run(context, emitter);
            }
        };
    }
}