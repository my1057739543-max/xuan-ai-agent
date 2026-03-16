package com.xuan.xuanopenagent.agent;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "xuan.agent.max-steps=4",
        "xuan.agent.max-tool-calls=2",
        "xuan.agent.tool-timeout-seconds=180"
})

class XuanAgentLiveOutputTest {

    @Autowired
    private XuanAgent xuanAgent;

    @Test
    void shouldPrintModelEventsAndFinalAnswer() {
        AgentContext context = AgentContext.initialize("live-session-001", "live-user-001", "现在北京时间几点？");
        List<AgentEvent> events = new ArrayList<>();

        String answer = xuanAgent.run(context, event -> {
            events.add(event);
            System.out.println(formatEvent(event));
        });

        System.out.println("FINAL_ANSWER => " + answer);
        System.out.println("EVENT_COUNT => " + events.size());

        assertThat(answer).isNotBlank();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).getType()).isEqualTo("done");
    }

    private String formatEvent(AgentEvent event) {
        Map<String, Object> payload = event.getPayload();
        return "EVENT => step=" + event.getStep()
                + ", type=" + event.getType()
                + ", payload=" + payload;
    }
}