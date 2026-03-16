package com.xuan.xuanopenagent.agent;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.config.AgentProperties;
import com.xuan.xuanopenagent.tools.TerminateTool;
import com.xuan.xuanopenagent.tools.TimeGetTool;
import com.xuan.xuanopenagent.tools.ToolRegistry;
import com.xuan.xuanopenagent.tools.WebSearchTool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallObservationTest {

    @Test
    void shouldEmitToolEventsAndStoreObservation() {
        AgentProperties properties = new AgentProperties();
        properties.setMaxSteps(4);
        properties.setMaxToolCalls(2);
        properties.setToolTimeoutSeconds(120);

        ToolRegistry toolRegistry = new ToolRegistry(new TimeGetTool(), new TerminateTool(), new WebSearchTool(), Optional.empty());
        ToolCallAgent agent = new ToolCallScriptedAgent(properties, toolRegistry);

        AgentContext context = AgentContext.initialize("session-tool-01", "user-01", "现在几点");
        List<AgentEvent> events = new ArrayList<>();
        String result = agent.run(context, events::add);

        assertThat(result).isEqualTo("final answer from scripted tool agent");
        assertThat(events).extracting(AgentEvent::getType)
                .contains("tool_call", "tool_result", "final", "done");
        assertThat(context.getHistory().stream().anyMatch(it -> it.contains("tool_result"))).isTrue();
    }

    private static class ToolCallScriptedAgent extends ToolCallAgent {

        private int index = 0;

        private ToolCallScriptedAgent(AgentProperties agentProperties, ToolRegistry toolRegistry) {
            super(agentProperties, toolRegistry);
        }

        @Override
        protected String think(AgentContext context) {
            return "需要一个时间工具";
        }

        @Override
        protected Decision decide(AgentContext context, String thought) {
            if (index == 0) {
                index++;
                return Decision.tool("time_get");
            }
            return Decision.respond("可以回复了");
        }

        @Override
        protected String generateFinalResponse(AgentContext context, String thought) {
            return "final answer from scripted tool agent";
        }
    }
}