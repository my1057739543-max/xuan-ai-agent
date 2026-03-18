package com.xuan.xuanopenagent.rag;

import com.xuan.xuanopenagent.rag.model.RagIngestionResult;
import com.xuan.xuanopenagent.rag.store.KnowledgeFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class RagIngestionIntegrationTest {

    @Autowired
    private RagIngestionService ragIngestionService;

    @Autowired
    private KnowledgeFileRepository knowledgeFileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_CONTENT = """
            # Test Document
            
            This is a test document for RAG integration testing.
            It contains multiple paragraphs to ensure chunking works correctly.
            
            The first paragraph discusses the basics.
            The second paragraph talks about more details.
            The third paragraph provides additional context.
            
            This is the final paragraph with some concluding remarks.
            """;

    @BeforeEach
    void cleanupBeforeTest() {
        // Drop and recreate vector_store table for dimension alignment
        try {
            jdbcTemplate.update("DROP TABLE IF EXISTS vector_store CASCADE");
            jdbcTemplate.update("""
                    CREATE TABLE vector_store (
                        id UUID PRIMARY KEY,
                        content TEXT,
                        metadata JSONB,
                        embedding vector(1024),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        } catch (Exception e) {
            System.out.println("[TEST] Vector store table reset: " + e.getMessage());
        }
        jdbcTemplate.update("DELETE FROM kb_file");
    }

    @Test
    void testUploadFileCreatesChunksAndVectors() throws IOException {
        // Arrange: Create test markdown file
        MultipartFile testFile = new MockMultipartFile(
                "file",
                "test-doc.md",
                "text/markdown",
                TEST_CONTENT.getBytes(StandardCharsets.UTF_8)
        );

        // Act: Upload file
        RagIngestionResult result = ragIngestionService.upload(testFile);

        // Assert: Check upload result
        assertNotNull(result.getFileId());
        assertEquals("READY", result.getStatus());
        assertTrue(result.getDocumentCount() > 0, "Should have read at least one document");
        assertTrue(result.getChunkCount() > 0, "Should have created at least one chunk");
        System.out.println("[TEST] Upload result: fileId=" + result.getFileId() +
                ", documents=" + result.getDocumentCount() +
                ", chunks=" + result.getChunkCount());

        // Assert: Check kb_file table
        Integer fileCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kb_file WHERE file_id = ?",
                Integer.class,
                result.getFileId()
        );
        assertEquals(1, fileCount, "Should have one file record");
        System.out.println("[TEST] kb_file table: 1 record found");

        // Assert: Check vector_store table
        Integer vectorCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'fileId' = ?",
                Integer.class,
                result.getFileId()
        );
        assertEquals(result.getChunkCount(), vectorCount,
                "Should have " + result.getChunkCount() + " vectors in vector_store");
        System.out.println("[TEST] vector_store table: " + vectorCount + " vectors found with fileId=" + result.getFileId());

        // Bonus: Inspect chunk structure
        String chunkSample = jdbcTemplate.queryForObject(
                "SELECT content FROM vector_store WHERE metadata->>'fileId' = ? LIMIT 1",
                String.class,
                result.getFileId()
        );
        assertNotNull(chunkSample, "Should have chunk content");
        System.out.println("[TEST] Sample chunk (first 200 chars): " +
                chunkSample.substring(0, Math.min(200, chunkSample.length())));
    }

    @Test
    void testDeleteFileCascadesVectorDeletion() throws IOException {
        // Arrange: Upload a file first
        MultipartFile testFile = new MockMultipartFile(
                "file",
                "delete-test.md",
                "text/markdown",
                TEST_CONTENT.getBytes(StandardCharsets.UTF_8)
        );
        RagIngestionResult uploadResult = ragIngestionService.upload(testFile);
        String fileId = uploadResult.getFileId();
        int chunkCountBeforeDelete = uploadResult.getChunkCount();

        // Verify initial state
        Integer vectorCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'fileId' = ?",
                Integer.class,
                fileId
        );
        assertEquals(chunkCountBeforeDelete, vectorCountBefore, "Should have vectors before delete");
        System.out.println("[TEST] Before delete: " + vectorCountBefore + " vectors in database");

        // Act: Delete the file
        ragIngestionService.deleteFile(fileId);

        // Assert: Check kb_file table is empty
        Integer fileCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kb_file WHERE file_id = ?",
                Integer.class,
                fileId
        );
        assertEquals(0, fileCountAfter, "kb_file record should be deleted");
        System.out.println("[TEST] After delete: kb_file record removed");

        // Assert: Check vector_store cascade deletion
        Integer vectorCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'fileId' = ?",
                Integer.class,
                fileId
        );
        assertEquals(0, vectorCountAfter, "All vectors with this fileId should be deleted");
        System.out.println("[TEST] After delete: " + vectorCountAfter + " vectors remain (cascade delete verified)");
    }

    @Test
    void testMultipleFilesIndependentVectors() throws IOException {
        // Arrange: Upload two different files
        MultipartFile file1 = new MockMultipartFile(
                "file", "file1.md", "text/markdown",
                "# File 1\nContent of file 1".getBytes(StandardCharsets.UTF_8)
        );
        MultipartFile file2 = new MockMultipartFile(
                "file", "file2.md", "text/markdown",
                "# File 2\nContent of file 2".getBytes(StandardCharsets.UTF_8)
        );

        // Act: Upload both files
        RagIngestionResult result1 = ragIngestionService.upload(file1);
        RagIngestionResult result2 = ragIngestionService.upload(file2);

        String fileId1 = result1.getFileId();
        String fileId2 = result2.getFileId();

        // Assert: Each file has independent vectors
        Integer count1 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'fileId' = ?",
                Integer.class,
                fileId1
        );
        Integer count2 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'fileId' = ?",
                Integer.class,
                fileId2
        );
        assertEquals(result1.getChunkCount(), count1, "File 1 should have " + result1.getChunkCount() + " vectors");
        assertEquals(result2.getChunkCount(), count2, "File 2 should have " + result2.getChunkCount() + " vectors");
        System.out.println("[TEST] File1: " + count1 + " vectors, File2: " + count2 + " vectors");

        // Act: Delete only file1
        ragIngestionService.deleteFile(fileId1);

        // Assert: Only file1's vectors are deleted
        Integer count1After = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'fileId' = ?",
                Integer.class,
                fileId1
        );
        Integer count2After = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'fileId' = ?",
                Integer.class,
                fileId2
        );
        assertEquals(0, count1After, "File 1 vectors should be completely deleted");
        assertEquals(count2, count2After, "File 2 vectors should remain unchanged");
        System.out.println("[TEST] After deleting file1: File1 vectors=" + count1After + ", File2 vectors=" + count2After);
    }
}
