package com.xuan.xuanopenagent.rag;

import com.xuan.xuanopenagent.config.RagProperties;
import com.xuan.xuanopenagent.rag.document.DocumentChunker;
import com.xuan.xuanopenagent.rag.document.DocumentReaderRouter;
import com.xuan.xuanopenagent.rag.model.KnowledgeFile;
import com.xuan.xuanopenagent.rag.model.RagIngestionResult;
import com.xuan.xuanopenagent.rag.model.KnowledgeFileStatus;
import com.xuan.xuanopenagent.rag.store.KnowledgeFileRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class RagIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RagIngestionService.class);

    private final RagProperties ragProperties;
    private final KnowledgeFileRepository repository;
    private final DocumentReaderRouter documentReaderRouter;
    private final DocumentChunker documentChunker;
    private final VectorStore vectorStore;

    public RagIngestionService(RagProperties ragProperties,
                               KnowledgeFileRepository repository,
                               DocumentReaderRouter documentReaderRouter,
                               DocumentChunker documentChunker,
                               VectorStore vectorStore) {
        this.ragProperties = ragProperties;
        this.repository = repository;
        this.documentReaderRouter = documentReaderRouter;
        this.documentChunker = documentChunker;
        this.vectorStore = vectorStore;
    }

    public RagIngestionResult upload(MultipartFile file) {
        validate(file);

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

            List<Document> chunks = documentChunker.chunk(documents, fileId, file.getOriginalFilename(), extension);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("Chunker returned no chunks");
            }

            vectorStore.add(chunks);
            repository.markReady(fileId, chunks.size());
            log.info("[RAG] fileId={} ingested. documents={} chunks={}", fileId, documents.size(), chunks.size());

            return new RagIngestionResult(fileId, KnowledgeFileStatus.READY.name(), documents.size(), chunks.size());
        } catch (Exception ex) {
            String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            repository.markFailed(fileId, reason);
            log.error("[RAG] fileId={} read failed", fileId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File read failed: " + reason, ex);
        }
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
}
