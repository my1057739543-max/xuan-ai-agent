package com.xuan.xuanopenagent.tools;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryTest {

    private final ToolRegistry toolRegistry = new ToolRegistry(
            new TimeGetTool(),
            new TerminateTool(),
            new WebSearchTool("https://api.tavily.com/search", "", 5, 15),
            Optional.empty()
    );

    @Test
    void shouldExposeToolCallbacksAndNames() {
        System.out.println("[TEST-START] shouldExposeToolCallbacksAndNames");
        assertThat(toolRegistry.getToolCallbacks()).isNotNull();
        assertThat(toolRegistry.getToolCallbacks().length).isGreaterThanOrEqualTo(3);
        assertThat(toolRegistry.getRegisteredToolNames())
                .contains("time_get", "terminate_tool", "web_search");
        System.out.println("[TEST-PASS] shouldExposeToolCallbacksAndNames | toolCount=" + toolRegistry.getToolCallbacks().length
                + " | names=" + toolRegistry.getRegisteredToolNames());
    }

    @Test
    void shouldInvokeTimeTool() {
        System.out.println("[TEST-START] shouldInvokeTimeTool");
        String result = toolRegistry.invoke("time_get", "");
        System.out.println("[TOOL-RESULT] time_get => " + result);
        assertThat(result).contains("iso");
        assertThat(result).contains("zone");
        System.out.println("[TEST-PASS] shouldInvokeTimeTool");
    }

    @Test
    void shouldInvokeTerminateTool() {
        System.out.println("[TEST-START] shouldInvokeTerminateTool");
        String result = toolRegistry.invoke("terminate_tool", "结束本次任务");
        System.out.println("[TOOL-RESULT] terminate_tool => " + result);
        assertThat(result).contains("reason");
        assertThat(result).contains("finalAnswer");
        System.out.println("[TEST-PASS] shouldInvokeTerminateTool");
    }

    @Test
    void shouldInvokeWebSearchPlaceholder() {
        System.out.println("[TEST-START] shouldInvokeWebSearchPlaceholder");
        String result = toolRegistry.invoke("web_search", "spring ai react");
        System.out.println("[TOOL-RESULT] web_search => " + result);
        assertThat(result).contains("status");
        assertThat(result).contains("api-key is not configured");
        System.out.println("[TEST-PASS] shouldInvokeWebSearchPlaceholder");
    }
}