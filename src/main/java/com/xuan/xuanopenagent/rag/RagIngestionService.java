package com.xuan.xuanopenagent.rag;

import com.xuan.xuanopenagent.config.RagProperties;
import com.xuan.xuanopenagent.rag.document.DocumentChunker;
import com.xuan.xuanopenagent.rag.document.DocumentReaderRouter;
import com.xuan.xuanopenagent.rag.model.KnowledgeFile;
import com.xuan.xuanopenagent.rag.model.RagIngestionResult;
import com.xuan.xuanopenagent.rag.model.RagBatchIngestionResult;
import com.xuan.xuanopenagent.rag.model.KnowledgeFileStatus;
import com.xuan.xuanopenagent.rag.store.KnowledgeFileRepository;
import com.xuan.xuanopenagent.service.CustomGameKeyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@Service
public class RagIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RagIngestionService.class);

    private final RagProperties ragProperties;
    private final KnowledgeFileRepository repository;
    private final DocumentReaderRouter documentReaderRouter;
    private final DocumentChunker documentChunker;
    private final VectorStore vectorStore;
    private final CustomGameKeyRegistry customGameKeyRegistry;

    public RagIngestionService(RagProperties ragProperties,
                               KnowledgeFileRepository repository,
                               DocumentReaderRouter documentReaderRouter,
                               DocumentChunker documentChunker,
                               VectorStore vectorStore,
                               CustomGameKeyRegistry customGameKeyRegistry) {
        this.ragProperties = ragProperties;
        this.repository = repository;
        this.documentReaderRouter = documentReaderRouter;
        this.documentChunker = documentChunker;
        this.vectorStore = vectorStore;
        this.customGameKeyRegistry = customGameKeyRegistry;
    }

    public RagIngestionResult upload(MultipartFile file, String gameKey, String tags, String customGameNames) {
        validate(file);
        String normalizedGameKey = normalizeAndValidateGameKey(gameKey);
        List<String> normalizedTags = normalizeTags(tags);

        String extension = extensionOf(file.getOriginalFilename());
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String storedName = fileId + "." + extension;
        Path uploadDir = Paths.get(ragProperties.getUploadDir());
        Path target = uploadDir.resolve(storedName);

        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", ex);
        }

        KnowledgeFile entity = new KnowledgeFile();
        entity.setFileId(fileId);
        entity.setGameKey(normalizedGameKey);
        entity.setTags(String.join(",", normalizedTags));
        entity.setOriginalName(file.getOriginalFilename());
        entity.setStoredName(storedName);
        entity.setExtension(extension);
        entity.setMimeType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setStatus(KnowledgeFileStatus.PROCESSING);
        repository.insertProcessing(entity);

        try {
            List<Document> documents = documentReaderRouter.read(target, extension);
            if (documents == null || documents.isEmpty()) {
                throw new IllegalStateException("Reader returned no documents");
            }

            List<Document> chunks = documentChunker.chunk(
                    documents,
                    fileId,
                    file.getOriginalFilename(),
                    extension,
                    normalizedGameKey,
                    normalizedTags
            );
            if (chunks.isEmpty()) {
                throw new IllegalStateException("Chunker returned no chunks");
            }

            addChunksInBatches(chunks, fileId);
            repository.markReady(fileId, chunks.size());
            
            // 注册用户输入的自定义游戏别名
            if (customGameNames != null && !customGameNames.isBlank()) {
                String[] aliases = customGameNames.split("[,\\s]+");
                if (aliases.length > 0) {
                    customGameKeyRegistry.registerCustomGameNames(normalizedGameKey, aliases);
                    log.info("[RAG] custom game names registered for gameKey={}: {}", normalizedGameKey, String.join(", ", aliases));
                }
            }
            
            log.info("[RAG] fileId={} ingested. documents={} chunks={}", fileId, documents.size(), chunks.size());

            return new RagIngestionResult(fileId, KnowledgeFileStatus.READY.name(), documents.size(), chunks.size());
        } catch (Exception ex) {
            String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            cleanupVectorsOnFailure(fileId);
            repository.markFailed(fileId, reason);
            log.error("[RAG] fileId={} read failed", fileId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File read failed: " + reason, ex);
        }
    }

    public RagBatchIngestionResult uploadBatch(MultipartFile[] files, String gameKey, String tags, String customGameNames) {
        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided");
        }

        RagBatchIngestionResult batchResult = new RagBatchIngestionResult();
        batchResult.setTotalFiles(files.length);

        List<RagBatchIngestionResult.Item> items = new ArrayList<>(files.length);
        int successCount = 0;

        for (MultipartFile file : files) {
            RagBatchIngestionResult.Item item = new RagBatchIngestionResult.Item();
            item.setFileName(file == null ? "" : file.getOriginalFilename());

            try {
                RagIngestionResult result = upload(file, gameKey, tags, customGameNames);
                item.setSuccess(true);
                item.setFileId(result.getFileId());
                item.setStatus(result.getStatus());
                item.setDocumentCount(result.getDocumentCount());
                item.setChunkCount(result.getChunkCount());
                successCount++;
            } catch (ResponseStatusException ex) {
                item.setSuccess(false);
                item.setStatus("FAILED");
                item.setErrorMessage(ex.getReason());
            } catch (Exception ex) {
                item.setSuccess(false);
                item.setStatus("FAILED");
                item.setErrorMessage(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            }

            items.add(item);
        }

        batchResult.setItems(items);
        batchResult.setSuccessCount(successCount);
        batchResult.setFailedCount(files.length - successCount);
        return batchResult;
    }

    public List<KnowledgeFile> listFiles() {
        return repository.findAll();
    }

    public void deleteFile(String fileId) {
        Optional<KnowledgeFile> existing = repository.findById(fileId);
        if (existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "fileId not found: " + fileId);
        }

        try {
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            vectorStore.delete(builder.eq("fileId", fileId).build());
            log.info("[RAG] fileId={} vectors deleted", fileId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete vectors by fileId: " + fileId, ex);
        }

        int affected = repository.deleteById(fileId);
        if (affected == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "fileId not found: " + fileId);
        }

        Path path = Paths.get(ragProperties.getUploadDir()).resolve(existing.get().getStoredName());
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("[RAG] fileId={} db deleted but file cleanup failed: {}", fileId, path, ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        long maxBytes = (long) ragProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File exceeds max size " + ragProperties.getMaxFileSizeMb() + "MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        List<String> allowed = ragProperties.getEnabledFileTypes().stream()
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .toList();
        if (!allowed.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type: " + extension + ", allowed=" + allowed);
        }

        String mime = file.getContentType();
        if (mime != null && !mime.isBlank()) {
            boolean mimeOk = switch (extension) {
                case "txt" -> mime.startsWith("text/");
                case "md" -> mime.equals("text/markdown") || mime.equals("text/plain");
                case "pdf" -> mime.equals("application/pdf");
                default -> false;
            };
            if (!mimeOk) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "MIME type does not match extension: " + mime + " for ." + extension);
            }
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have extension");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT).trim();
        if (ext.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File extension is empty");
        }
        return ext;
    }

    private String normalizeAndValidateGameKey(String gameKey) {
        if (gameKey == null || gameKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameKey is required");
        }

        String normalized = gameKey.trim().toLowerCase(Locale.ROOT);
        String mapped = mapAliasToGameKey(normalized);
        if (mapped.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gameKey is required");
        }
        return mapped;
    }

    private String mapAliasToGameKey(String key) {
        if (ragProperties.getGameAliasMap() == null || ragProperties.getGameAliasMap().isEmpty()) {
            return key;
        }
        for (Map.Entry<String, String> entry : ragProperties.getGameAliasMap().entrySet()) {
            String alias = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (!alias.isBlank() && alias.equals(key)) {
                String mapped = entry.getValue() == null ? "" : entry.getValue().trim().toLowerCase(Locale.ROOT);
                return mapped.isBlank() ? key : mapped;
            }
        }
        return key;
    }

    private List<String> normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(it -> !it.isBlank())
                .map(it -> it.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private void addChunksInBatches(List<Document> chunks, String fileId) {
        int batchLimit = Math.max(1, ragProperties.getEmbeddingBatchSize());
        int total = chunks.size();
        if (total <= batchLimit) {
            vectorStore.add(chunks);
            return;
        }

        int batches = (total + batchLimit - 1) / batchLimit;
        log.info("[RAG] fileId={} embedding in batches. totalChunks={} batchSize={} batches={}",
                fileId, total, batchLimit, batches);

        for (int start = 0; start < total; start += batchLimit) {
            int end = Math.min(start + batchLimit, total);
            vectorStore.add(chunks.subList(start, end));
        }
    }

    private void cleanupVectorsOnFailure(String fileId) {
        try {
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            vectorStore.delete(builder.eq("fileId", fileId).build());
        } catch (Exception cleanupEx) {
            log.warn("[RAG] fileId={} cleanup failed after ingestion error", fileId, cleanupEx);
        }
    }
}
