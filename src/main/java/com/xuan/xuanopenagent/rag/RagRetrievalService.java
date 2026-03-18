package com.xuan.xuanopenagent.rag;

import com.xuan.xuanopenagent.config.RagProperties;
import com.xuan.xuanopenagent.rag.model.RetrievalHit;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RagRetrievalService {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public RagRetrievalService(VectorStore vectorStore, RagProperties ragProperties) {
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    public List<RetrievalHit> retrieve(String query, String fileIdFilter) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(ragProperties.getTopK())
                .similarityThreshold(ragProperties.getSimilarityThreshold());

        if (fileIdFilter != null && !fileIdFilter.isBlank()) {
            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            builder.filterExpression(filterBuilder.eq("fileId", fileIdFilter).build());
        }

        List<Document> documents = vectorStore.similaritySearch(builder.build());
        List<RetrievalHit> hits = new ArrayList<>(documents.size());

        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            RetrievalHit hit = new RetrievalHit();
            hit.setFileId(asString(metadata.get("fileId")));
            hit.setFileName(asString(metadata.get("fileName")));
            hit.setChunkIndex(asInt(metadata.get("chunkIndex")));
            hit.setSourceType(asString(metadata.get("sourceType")));
            hit.setScore(document.getScore() == null ? 0.0 : document.getScore());
            hit.setContentSnippet(toSnippet(document.getText()));
            hits.add(hit);
        }

        return hits;
    }

    public String buildPromptContext(List<RetrievalHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下是知识库检索到的参考片段，请优先基于这些内容回答：\n");
        for (int i = 0; i < hits.size(); i++) {
            RetrievalHit hit = hits.get(i);
            sb.append(i + 1)
                    .append(". fileId=").append(defaultIfBlank(hit.getFileId(), "unknown"))
                    .append(", fileName=").append(defaultIfBlank(hit.getFileName(), "unknown"))
                    .append(", chunkIndex=").append(hit.getChunkIndex())
                    .append("\n")
                    .append(hit.getContentSnippet())
                    .append("\n");
        }
        return sb.toString();
    }

    private String toSnippet(String text) {
        if (text == null) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 220) {
            return compact;
        }
        return compact.substring(0, 220) + "...";
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
