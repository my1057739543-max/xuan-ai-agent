package com.xuan.xuanopenagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RAG storage initializer.
 * <p>
 * On startup: ensures the local upload directory exists and logs the active
 * RAG configuration so the operator can verify the correct settings are loaded.
 * PgVectorStore itself is fully auto-configured via
 * spring.ai.vectorstore.pgvector.* properties.
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Bean
    public CommandLineRunner ragStartupInit(RagProperties rag) {
        return args -> {
            // Create upload directory if it does not already exist
            Path uploadPath = Paths.get(rag.getUploadDir());
            Files.createDirectories(uploadPath);
            log.info("[RAG] Upload directory ready: {}", uploadPath.toAbsolutePath());

            // Log active retrieval config for operator visibility
            log.info("[RAG] Config — topK={} similarityThreshold={} chunkSize={} allowEmptyContext={}",
                    rag.getTopK(),
                    rag.getSimilarityThreshold(),
                    rag.getChunkSize(),
                    rag.isAdvisorAllowEmptyContext());
            log.info("[RAG] File ingestion — maxFileSizeMb={} enabledTypes={}",
                    rag.getMaxFileSizeMb(),
                    rag.getEnabledFileTypes());
        };
    }
}
