package com.xuan.xuanopenagent.config;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Configuration;

/**
 * Factory for creating {@link RetrievalAugmentationAdvisor} instances.
 * <p>
 * Each call to {@code buildRetrievalAdvisor()} creates a fresh advisor wired with
 * the active {@link RagProperties} and the auto-configured {@link VectorStore}.
 * Callers (e.g. AgentService in module 09) should build a new advisor per request.
 * Per-document filter expression support will be added in module 09.
 */
@Configuration
public class RagAdvisorConfig {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public RagAdvisorConfig(VectorStore vectorStore, RagProperties ragProperties) {
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    /**
     * Build a retrieval advisor that searches across all documents in the vector store.
     */
    public RetrievalAugmentationAdvisor buildRetrievalAdvisor() {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(ragProperties.getSimilarityThreshold())
                        .topK(ragProperties.getTopK())
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(ragProperties.isAdvisorAllowEmptyContext())
                        .build())
                .build();
    }

    /**
     * Build a retrieval advisor that optionally scopes retrieval by fileId metadata.
     */
    public RetrievalAugmentationAdvisor buildRetrievalAdvisor(String fileIdFilter) {
        VectorStoreDocumentRetriever.Builder retrieverBuilder = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .topK(ragProperties.getTopK());

        if (fileIdFilter != null && !fileIdFilter.isBlank()) {
            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            retrieverBuilder.filterExpression(filterBuilder.eq("fileId", fileIdFilter).build());
        }

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retrieverBuilder.build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(ragProperties.isAdvisorAllowEmptyContext())
                        .build())
                .build();
    }
}
