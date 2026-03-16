package com.xuan.xuanopenagent.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class ToolRegistry {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> KNOWN_CITIES = Arrays.asList(
        "北京", "上海", "广州", "深圳", "杭州", "南京", "苏州", "成都", "重庆", "武汉",
        "西安", "天津", "长沙", "郑州", "青岛", "厦门", "福州", "宁波", "合肥", "昆明"
    );
    private static final List<String> POI_KEYWORDS = Arrays.asList(
        "咖啡店", "咖啡馆", "咖啡厅", "餐厅", "饭店", "酒店", "地铁站", "景点", "商场", "医院"
    );

    private final TimeGetTool timeGetTool;
    private final TerminateTool terminateTool;
    private final WebSearchTool webSearchTool;
    private final ToolCallback[] localToolCallbacks;
    private final ToolCallback[] mcpToolCallbacks;
    private final ToolCallback[] toolCallbacks;

    public ToolRegistry(TimeGetTool timeGetTool,
                        TerminateTool terminateTool,
                        WebSearchTool webSearchTool,
                        Optional<SyncMcpToolCallbackProvider> mcpToolCallbackProvider) {
        this.timeGetTool = timeGetTool;
        this.terminateTool = terminateTool;
        this.webSearchTool = webSearchTool;
        this.localToolCallbacks = ToolCallbacks.from(timeGetTool, terminateTool, webSearchTool);
        this.mcpToolCallbacks = mcpToolCallbackProvider
                .map(SyncMcpToolCallbackProvider::getToolCallbacks)
                .orElseGet(() -> new ToolCallback[0]);
        this.toolCallbacks = mergeToolCallbacks(localToolCallbacks, mcpToolCallbacks);
    }

    public ToolCallback[] getToolCallbacks() {
        return toolCallbacks;
    }

    public List<String> getRegisteredToolNames() {
        List<String> names = new ArrayList<>();
        names.add("time_get");
        names.add("terminate_tool");
        names.add("web_search");

        for (ToolCallback callback : mcpToolCallbacks) {
            names.add(callback.getToolDefinition().name());
        }
        return names;
    }

    public String invoke(String toolName, String queryOrInput) {
        String normalized = normalize(toolName);

        if (normalized.contains("time")) {
            return stringify(timeGetTool.getCurrentDateTime());
        }

        if (normalized.contains("terminate")) {
            return stringify(terminateTool.terminateTask("task_completed", queryOrInput));
        }

        if ("web_search".equals(normalized)) {
            return stringify(webSearchTool.webSearch(queryOrInput));
        }

        Optional<ToolCallback> mcpTool = findMcpTool(toolName);
        if (mcpTool.isPresent()) {
            return invokeMcpTool(mcpTool.get(), queryOrInput);
        }

        if (normalized.contains("web") || normalized.contains("search")) {
            return stringify(webSearchTool.webSearch(queryOrInput));
        }

        throw new IllegalArgumentException("Unknown tool: " + toolName);
    }

    private Optional<ToolCallback> findMcpTool(String toolName) {
        String normalized = normalize(toolName);
        for (ToolCallback callback : mcpToolCallbacks) {
            String callbackName = normalize(callback.getToolDefinition().name());
            if (callbackName.equals(normalized) || callbackName.contains(normalized) || normalized.contains(callbackName)) {
                return Optional.of(callback);
            }
        }
        return Optional.empty();
    }

    private String invokeMcpTool(ToolCallback callback, String queryOrInput) {
        String callbackName = callback.getToolDefinition().name();
        Map<String, Object> payload = buildMcpPayload(callbackName, queryOrInput);
        String json = toJson(payload);
        return callback.call(json);
    }

    private Map<String, Object> buildMcpPayload(String callbackName, String queryOrInput) {
        String normalizedName = normalize(callbackName);
        String query = queryOrInput == null ? "" : queryOrInput;
        String city = extractCity(query);
        String keyword = extractKeyword(query);
        String area = extractArea(query, city);

        Map<String, Object> payload = new LinkedHashMap<>();
        if (normalizedName.contains("text_search")) {
            payload.put("keywords", keyword);
            if (!city.isBlank()) {
                payload.put("city", city);
            }
            return payload;
        }
        if (normalizedName.contains("around_search")) {
            String location = resolveLocation(area, city);
            payload.put("keywords", keyword);
            payload.put("location", location);
            payload.put("radius", "3000");
            return payload;
        }
        if (normalizedName.contains("weather")) {
            payload.put("city", city.isBlank() ? query : city);
            return payload;
        }
        if (normalizedName.contains("geo") && !normalizedName.contains("regeo")) {
            payload.put("address", area.isBlank() ? query : area);
            if (!city.isBlank()) {
                payload.put("city", city);
            }
            return payload;
        }
        if (normalizedName.contains("regeocode")) {
            String location = resolveLocation(area, city);
            payload.put("location", location);
            return payload;
        }

        payload.put("keywords", keyword);
        payload.put("query", query);
        return payload;
    }

    private String extractCity(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        for (String city : KNOWN_CITIES) {
            if (query.contains(city)) {
                return city;
            }
        }
        return "";
    }

    private String extractKeyword(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        for (String keyword : POI_KEYWORDS) {
            if (query.contains(keyword)) {
                return keyword;
            }
        }
        String cleaned = query
                .replace("帮我", "")
                .replace("查询", "")
                .replace("查一下", "")
                .replace("搜一下", "")
                .replace("附近的", "")
                .replace("附近", "")
                .replace("并给我一个简短建议", "")
                .replace("并给我建议", "")
                .replace("现在", "")
                .trim();
        return cleaned.isBlank() ? query : cleaned;
    }

    private String extractArea(String query, String city) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String area = query
                .replace("帮我", "")
                .replace("查询", "")
                .replace("查一下", "")
                .replace("搜一下", "")
                .replace("并给我一个简短建议", "")
                .replace("并给我建议", "")
                .trim();

        String keyword = extractKeyword(query);
        if (!keyword.isBlank()) {
            area = area.replace(keyword, "");
        }
        area = area.replace("附近的", "")
                .replace("附近", "")
                .replace("周边", "")
                .trim();

        if (!city.isBlank() && !area.contains(city)) {
            area = city + area;
        }
        return area.isBlank() ? query : area;
    }

    private String resolveLocation(String area, String city) {
        Optional<ToolCallback> geoTool = findMcpTool("maps_geo");
        if (geoTool.isEmpty()) {
            return "120.155070,30.274085";
        }

        Map<String, Object> geoPayload = new LinkedHashMap<>();
        geoPayload.put("address", area);
        if (city != null && !city.isBlank()) {
            geoPayload.put("city", city);
        }

        try {
            String raw = geoTool.get().call(toJson(geoPayload));
            Object parsed = extractWrappedJson(raw);
            if (parsed instanceof Map<?, ?> map) {
                Object geocodesObj = map.get("geocodes");
                if (geocodesObj instanceof List<?> geocodes && !geocodes.isEmpty()) {
                    Object first = geocodes.getFirst();
                    if (first instanceof Map<?, ?> firstMap) {
                        Object location = firstMap.get("location");
                        if (location instanceof String locationString && !locationString.isBlank()) {
                            return locationString;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "120.155070,30.274085";
    }

    private Object extractWrappedJson(String raw) {
        try {
            Object parsed = OBJECT_MAPPER.readValue(raw, Object.class);
            if (parsed instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> wrapper) {
                    Object text = wrapper.get("text");
                    if (text instanceof String textValue && !textValue.isBlank()) {
                        return OBJECT_MAPPER.readValue(textValue, Object.class);
                    }
                }
            }
            return parsed;
        } catch (Exception ex) {
            return raw;
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize MCP tool payload", ex);
        }
    }

    private String normalize(String toolName) {
        if (toolName == null) {
            return "";
        }
        return toolName.trim().toLowerCase(Locale.ROOT);
    }

    private String stringify(Map<String, String> payload) {
        return payload.toString();
    }

    private ToolCallback[] mergeToolCallbacks(ToolCallback[] local, ToolCallback[] mcp) {
        ToolCallback[] merged = new ToolCallback[local.length + mcp.length];
        System.arraycopy(local, 0, merged, 0, local.length);
        System.arraycopy(mcp, 0, merged, local.length, mcp.length);
        return merged;
    }
}