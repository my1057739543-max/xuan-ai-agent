package com.xuan.xuanopenagent.agent;

import com.xuan.xuanopenagent.agent.model.AgentContext;
import com.xuan.xuanopenagent.agent.model.AgentEvent;
import com.xuan.xuanopenagent.config.AgentProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReActAgentCoreTest {

    @Test
    void shouldRespondAndTerminateWithinMaxSteps() {
        AgentProperties properties = new AgentProperties();
        properties.setMaxSteps(4);
        properties.setMaxToolCalls(2);
        properties.setToolTimeoutSeconds(120);

        ReActAgent agent = new ScriptedAgent(properties, List.of(
                ReActAgent.Decision.respond("直接回答")
        ));

        AgentContext context = AgentContext.initialize("s-001", "u-001", "杭州现在几点");
        List<AgentEvent> events = new ArrayList<>();
        String result = agent.run(context, events::add);

        assertThat(result).isEqualTo("这是最终回答");
        assertThat(events).extracting(AgentEvent::getType).contains("plan", "thought", "final", "done");
        assertThat(context.getCurrentStep()).isEqualTo(1);
    }

    @Test
    void shouldRunToolThenRespond() {
        AgentProperties properties = new AgentProperties();
        properties.setMaxSteps(6);
        properties.setMaxToolCalls(3);
        properties.setToolTimeoutSeconds(120);

        ReActAgent agent = new ScriptedAgent(properties, List.of(
                ReActAgent.Decision.tool("time_get"),
                ReActAgent.Decision.respond("基于工具结果回复")
        ));

        AgentContext context = AgentContext.initialize("s-002", "u-001", "现在北京时间");
        List<AgentEvent> events = new ArrayList<>();
        String result = agent.run(context, events::add);

        assertThat(result).isEqualTo("这是最终回答");
        assertThat(events).extracting(AgentEvent::getType).contains("tool_call", "tool_result", "final", "done");
        assertThat(context.getToolCallCount()).isEqualTo(1);
    }

    @Test
    void shouldStopWhenMaxStepsReached() {
        AgentProperties properties = new AgentProperties();
        properties.setMaxSteps(2);
        properties.setMaxToolCalls(8);
        properties.setToolTimeoutSeconds(120);

        ReActAgent agent = new ScriptedAgent(properties, List.of(
                ReActAgent.Decision.tool("web_search"),
                ReActAgent.Decision.tool("web_search"),
                ReActAgent.Decision.tool("web_search")
        ));

        AgentContext context = AgentContext.initialize("s-003", "u-001", "搜一下 spring ai");
        List<AgentEvent> events = new ArrayList<>();
        String result = agent.run(context, events::add);

        assertThat(result).contains("已达到最大思考步数");
        assertThat(context.getCurrentStep()).isEqualTo(2);
        assertThat(events.get(events.size() - 1).getType()).isEqualTo("done");
    }

    private static class ScriptedAgent extends ReActAgent {

        private final List<Decision> script;
        private int index = 0;

        private ScriptedAgent(AgentProperties properties, List<Decision> script) {
            super(properties);
            this.script = script;
        }

        @Override
        protected String think(AgentContext context) {
            return "先判断是否需要工具";
        }

        @Override
        protected Decision decide(AgentContext context, String thought) {
            if (index >= script.size()) {
                return Decision.respond("默认回复");
            }
            return script.get(index++);
        }

        @Override
        protected String generateFinalResponse(AgentContext context, String thought) {
            return "这是最终回答";
        }

        @Override
        protected String observeToolResult(AgentContext context, Decision decision, String thought) {
            return "工具结果: " + decision.content();
        }
    }
}