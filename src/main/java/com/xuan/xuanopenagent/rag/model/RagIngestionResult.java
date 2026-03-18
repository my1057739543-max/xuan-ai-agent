package com.xuan.xuanopenagent.rag.model;

public class RagIngestionResult {

    private String fileId;
    private String status;
    private int documentCount;
    private int chunkCount;

    public RagIngestionResult() {
    }

    public RagIngestionResult(String fileId, String status, int documentCount, int chunkCount) {
        this.fileId = fileId;
        this.status = status;
        this.documentCount = documentCount;
        this.chunkCount = chunkCount;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }

    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
}
