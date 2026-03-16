package com.xuan.xuanopenagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WebSearchTool {

    @Tool(description = "Web search placeholder tool for future integration")
    public Map<String, String> webSearch(String query) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("query", query == null ? "" : query);
        result.put("status", "placeholder");
        result.put("summary", "WebSearchTool is a placeholder in module 03. Replace with real search provider later.");
        return result;
    }
}