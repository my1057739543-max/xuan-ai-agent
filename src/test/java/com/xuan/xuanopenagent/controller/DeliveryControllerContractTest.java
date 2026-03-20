package com.xuan.xuanopenagent.controller;

import com.xuan.xuanopenagent.agent.XuanAgent;
import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.config.AgentProperties;
import com.xuan.xuanopenagent.config.RagProperties;
import com.xuan.xuanopenagent.rag.RagRetrievalService;
import com.xuan.xuanopenagent.service.AgentService;
import com.xuan.xuanopenagent.tools.TerminateTool;
import com.xuan.xuanopenagent.tools.TimeGetTool;
import com.xuan.xuanopenagent.tools.ToolRegistry;
import com.xuan.xuanopenagent.tools.WebSearchTool;
import com.xuan.xuanopenagent.controller.ChatController;
import com.xuan.xuanopenagent.controller.HealthController;
import com.xuan.xuanopenagent.controller.ToolController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.mock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ChatController.class, HealthController.class, ToolController.class})
@Import(DeliveryControllerContractTest.StubConfig.class)
class DeliveryControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeHealthEndpoint() throws Exception {
        System.out.println("[TEST-START] shouldExposeHealthEndpoint");
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
        System.out.println("[TEST-PASS] shouldExposeHealthEndpoint");
    }

    @Test
    void shouldExposeToolsEndpoint() throws Exception {
        System.out.println("[TEST-START] shouldExposeToolsEndpoint");
        mockMvc.perform(get("/api/tools"))
                .andExpect(status().isOk());
        System.out.println("[TEST-PASS] shouldExposeToolsEndpoint");
    }

    @Test
    void shouldAcceptChatStreamRequest() throws Exception {
        System.out.println("[TEST-START] shouldAcceptChatStreamRequest");
        String body = """
                {
                  \"sessionId\": \"s-1001\",
                  \"userId\": \"u-1001\",
                  \"message\": \"现在北京时间几点\",
                  \"options\": {\"maxSteps\": 2}
                }
                """;

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(body))
                .andExpect(status().isOk());

        System.out.println("[TEST-PASS] shouldAcceptChatStreamRequest");
    }

    @TestConfiguration
    static class StubConfig {

        @Bean
        ToolRegistry toolRegistry() {
            return new ToolRegistry(
                    new TimeGetTool(),
                    new TerminateTool(),
                    new WebSearchTool("https://api.tavily.com/search", "", 5, 15),
                    Optional.empty()
            );
        }

        @Bean
        AgentProperties agentProperties() {
            AgentProperties properties = new AgentProperties();
            properties.setMaxSteps(3);
            properties.setMaxToolCalls(2);
            properties.setToolTimeoutSeconds(60);
            return properties;
        }

        @Bean
        XuanAgent xuanAgent(AgentProperties agentProperties, ToolRegistry toolRegistry) {
            return new XuanAgent((ChatClient) null, agentProperties, toolRegistry) {

                @Override
                protected String think(AgentContext context) {
                    return "stub thought";
                }

                @Override
                protected Decision decide(AgentContext context, String thought) {
                    return Decision.respond("stub response");
                }

                @Override
                protected String generateFinalResponse(AgentContext context, String thought) {
                    return "stub final answer";
                }
            };
        }

        @Bean
        RagProperties ragProperties() {
            return new RagProperties();
        }

        @Bean
        RagRetrievalService ragRetrievalService(RagProperties ragProperties) {
            VectorStore vectorStore = mock(VectorStore.class);
            return new RagRetrievalService(vectorStore, ragProperties);
        }

        @Bean
        AgentService agentService(XuanAgent xuanAgent,
                                  ToolRegistry toolRegistry,
                                  RagRetrievalService ragRetrievalService,
                                  RagProperties ragProperties) {
            return new AgentService(xuanAgent, toolRegistry, ragRetrievalService, ragProperties);
        }
    }
}