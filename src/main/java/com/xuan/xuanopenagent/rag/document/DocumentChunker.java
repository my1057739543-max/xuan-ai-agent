package com.xuan.xuanopenagent.rag.document;

import com.xuan.xuanopenagent.config.RagProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DocumentChunker {

    private final RagProperties ragProperties;

    public DocumentChunker(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public List<Document> chunk(List<Document> sourceDocuments, String fileId, String fileName, String sourceType) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(ragProperties.getChunkSize())
                .withMinChunkSizeChars(ragProperties.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(ragProperties.getMinChunkLengthToEmbed())
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.apply(sourceDocuments);
        List<Document> enriched = new ArrayList<>(chunks.size());
        String uploadTime = Instant.now().toString();

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("fileId", fileId);
            metadata.put("fileName", fileName);
            metadata.put("chunkIndex", i);
            metadata.put("sourceType", sourceType);
            metadata.put("uploadTime", uploadTime);

            enriched.add(chunk.mutate().metadata(metadata).build());
        }

        return enriched;
    }
}
