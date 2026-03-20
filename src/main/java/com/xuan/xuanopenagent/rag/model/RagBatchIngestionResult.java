package com.xuan.xuanopenagent.rag.model;

import java.util.ArrayList;
import java.util.List;

public class RagBatchIngestionResult {

    private int totalFiles;
    private int successCount;
    private int failedCount;
    private List<Item> items = new ArrayList<>();

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {
        private String fileName;
        private boolean success;
        private String fileId;
        private String status;
        private int documentCount;
        private int chunkCount;
        private String errorMessage;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getDocumentCount() {
            return documentCount;
        }

        public void setDocumentCount(int documentCount) {
            this.documentCount = documentCount;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
