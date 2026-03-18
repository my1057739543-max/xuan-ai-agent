package com.xuan.xuanopenagent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class WebSearchTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;
    private final int maxResults;

    public WebSearchTool(@Value("${xuan.tools.web-search.endpoint:https://api.tavily.com/search}") String endpoint,
                         @Value("${xuan.tools.web-search.api-key:}") String apiKey,
                         @Value("${xuan.tools.web-search.max-results:5}") int maxResults,
                         @Value("${xuan.tools.web-search.timeout-seconds:15}") int timeoutSeconds) {
        this.endpoint = endpoint;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.maxResults = Math.max(1, Math.min(maxResults, 10));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 3)))
                .build();
    }

    @Tool(description = "Real-time web search tool based on Tavily API")
    public Map<String, String> webSearch(String query) {
        Map<String, String> result = new LinkedHashMap<>();
        String normalizedQuery = query == null ? "" : query.trim();
        result.put("query", normalizedQuery);

        if (normalizedQuery.isBlank()) {
            result.put("status", "error");
            result.put("summary", "query is empty");
            return result;
        }

        if (apiKey.isBlank()) {
            result.put("status", "error");
            result.put("summary", "Web search api-key is not configured. Please set xuan.tools.web-search.api-key in application-local.yml");
            return result;
        }

        try {
            String payload = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "api_key", apiKey,
                    "query", normalizedQuery,
                    "max_results", maxResults,
                    "include_answer", true,
                    "search_depth", "advanced"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                result.put("status", "error");
                result.put("summary", "web search failed, http=" + response.statusCode());
                result.put("raw", response.body());
                return result;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            String answer = root.path("answer").asText("");
            String summary = answer.isBlank() ? root.path("query").asText("") : answer;

            JsonNode resultsNode = root.path("results");
            String sources = "";
            if (resultsNode.isArray()) {
                sources = StreamSupport.stream(resultsNode.spliterator(), false)
                        .limit(maxResults)
                        .map(node -> {
                            String title = node.path("title").asText("");
                            String url = node.path("url").asText("");
                            String contentSnippet = node.path("content").asText("");
                            if (contentSnippet.length() > 120) {
                                contentSnippet = contentSnippet.substring(0, 120) + "...";
                            }
                            return "- " + title + " | " + url + " | " + contentSnippet;
                        })
                        .collect(Collectors.joining("\n"));
            }

            result.put("status", "ok");
            result.put("summary", summary.isBlank() ? "search success" : summary);
            result.put("sources", sources);
            return result;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            result.put("status", "error");
            result.put("summary", "web search exception: " + ex.getMessage());
            return result;
        }
    }
}