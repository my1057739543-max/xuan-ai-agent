package com.xuan.xuanopenagent.controller;

import com.xuan.xuanopenagent.rag.RagIngestionService;
import com.xuan.xuanopenagent.rag.model.KnowledgeFile;
import com.xuan.xuanopenagent.rag.model.RagIngestionResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    private final RagIngestionService ragIngestionService;

    public KnowledgeBaseController(RagIngestionService ragIngestionService) {
        this.ragIngestionService = ragIngestionService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RagIngestionResult upload(@RequestPart("file") MultipartFile file) {
        return ragIngestionService.upload(file);
    }

    @GetMapping("/files")
    public List<KnowledgeFile> listFiles() {
        return ragIngestionService.listFiles();
    }

    @DeleteMapping("/files/{fileId}")
    public Map<String, Object> delete(@PathVariable String fileId) {
        ragIngestionService.deleteFile(fileId);
        return Map.of("fileId", fileId, "deleted", true);
    }
}
