package com.xuan.xuanopenagent.agent;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.tools.ToolRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "xuan.agent.max-steps=4",
        "xuan.agent.max-tool-calls=3",
        "xuan.agent.tool-timeout-seconds=180"
})
@Disabled("Manual backend live test. Remove Disabled after confirming DeepSeek key, MCP config, Node.js and network are available.")
class XuanAgentBackendLiveTest {

    @Autowired
    private XuanAgent xuanAgent;

    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void shouldRunFullBackendFlowAndPrintLogs() {
        System.out.println("[TEST-START] shouldRunFullBackendFlowAndPrintLogs");
        System.out.println("[REGISTERED-TOOLS] => " + toolRegistry.getRegisteredToolNames());

        AgentContext context = AgentContext.initialize(
                "backend-live-session-001",
                "backend-live-user-001",
                "帮我查询杭州西湖附近的咖啡店，并给我一个简短建议"
        );
        context.setRequestedMaxSteps(4);

        List<AgentEvent> events = new ArrayList<>();
        String finalAnswer = xuanAgent.run(context, event -> {
            events.add(event);
            System.out.println(formatEvent(event));
        });

        System.out.println("[FINAL-ANSWER] => " + finalAnswer);
        System.out.println("[TRACE-SIZE] => " + context.getExecutionTraces().size());
        System.out.println("[EVENT-SIZE] => " + events.size());

        assertThat(toolRegistry.getRegisteredToolNames()).isNotEmpty();
        assertThat(finalAnswer).isNotBlank();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).getType()).isEqualTo("done");
        System.out.println("[TEST-PASS] shouldRunFullBackendFlowAndPrintLogs");
    }

    private String formatEvent(AgentEvent event) {
        return "[AGENT-EVENT] step=" + event.getStep()
                + ", type=" + event.getType()
                + ", payload=" + event.getPayload();
    }
}