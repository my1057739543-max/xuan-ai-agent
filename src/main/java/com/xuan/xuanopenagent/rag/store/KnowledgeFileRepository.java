package com.xuan.xuanopenagent.rag.store;

import com.xuan.xuanopenagent.rag.model.KnowledgeFile;
import com.xuan.xuanopenagent.rag.model.KnowledgeFileStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeFileRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureTable();
    }

    public void ensureTable() {
        jdbcTemplate.execute("""
                create table if not exists kb_file (
                    file_id varchar(64) primary key,
                    game_key varchar(50) not null,
                    tags text,
                    original_name varchar(255) not null,
                    stored_name varchar(255) not null,
                    extension varchar(20) not null,
                    mime_type varchar(255),
                    size_bytes bigint not null,
                    status varchar(20) not null,
                    error_message text,
                    document_count integer,
                    created_at timestamp not null,
                    updated_at timestamp not null
                )
                """);

            jdbcTemplate.execute("alter table kb_file add column if not exists game_key varchar(50)");
            jdbcTemplate.execute("alter table kb_file add column if not exists tags text");
    }

    public void insertProcessing(KnowledgeFile file) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                        insert into kb_file (
                            file_id, game_key, tags, original_name, stored_name, extension, mime_type, size_bytes,
                            status, error_message, document_count, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                file.getFileId(),
                file.getGameKey(),
                file.getTags(),
                file.getOriginalName(),
                file.getStoredName(),
                file.getExtension(),
                file.getMimeType(),
                file.getSizeBytes(),
                KnowledgeFileStatus.PROCESSING.name(),
                null,
                null,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    public void markReady(String fileId, int documentCount) {
        jdbcTemplate.update("""
                        update kb_file
                        set status = ?, error_message = null, document_count = ?, updated_at = ?
                        where file_id = ?
                        """,
                KnowledgeFileStatus.READY.name(),
                documentCount,
                Timestamp.valueOf(LocalDateTime.now()),
                fileId
        );
    }

    public void markFailed(String fileId, String errorMessage) {
        jdbcTemplate.update("""
                        update kb_file
                        set status = ?, error_message = ?, updated_at = ?
                        where file_id = ?
                        """,
                KnowledgeFileStatus.FAILED.name(),
                errorMessage,
                Timestamp.valueOf(LocalDateTime.now()),
                fileId
        );
    }

    public List<KnowledgeFile> findAll() {
        return jdbcTemplate.query("""
                select file_id, game_key, tags, original_name, stored_name, extension, mime_type, size_bytes,
                     status, error_message, document_count, created_at, updated_at
                from kb_file
                order by created_at desc
                """, this::mapRow);
    }

    public Optional<KnowledgeFile> findById(String fileId) {
        List<KnowledgeFile> result = jdbcTemplate.query("""
                select file_id, game_key, tags, original_name, stored_name, extension, mime_type, size_bytes,
                     status, error_message, document_count, created_at, updated_at
                from kb_file
                where file_id = ?
                """, this::mapRow, fileId);
        return result.stream().findFirst();
    }

    public int deleteById(String fileId) {
        return jdbcTemplate.update("delete from kb_file where file_id = ?", fileId);
    }

    private KnowledgeFile mapRow(ResultSet rs, int rowNum) throws SQLException {
        KnowledgeFile file = new KnowledgeFile();
        file.setFileId(rs.getString("file_id"));
        file.setGameKey(rs.getString("game_key"));
        file.setTags(rs.getString("tags"));
        file.setOriginalName(rs.getString("original_name"));
        file.setStoredName(rs.getString("stored_name"));
        file.setExtension(rs.getString("extension"));
        file.setMimeType(rs.getString("mime_type"));
        file.setSizeBytes(rs.getLong("size_bytes"));
        file.setStatus(KnowledgeFileStatus.valueOf(rs.getString("status")));
        file.setErrorMessage(rs.getString("error_message"));

        int count = rs.getInt("document_count");
        file.setDocumentCount(rs.wasNull() ? null : count);

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        file.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        file.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return file;
    }
}
