package com.xuan.xuanopenagent.rag.document;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class DocumentReaderRouter {

    public List<Document> read(Path filePath, String extension) {
        String ext = extension.toLowerCase();
        return switch (ext) {
            case "txt" -> new TextReader(new FileSystemResource(filePath)).read();
            case "md" -> {
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder().build();
                yield new MarkdownDocumentReader(new FileSystemResource(filePath), config).read();
            }
            case "pdf" -> {
                PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder().build();
                yield new PagePdfDocumentReader(filePath.toUri().toString(), config).read();
            }
            default -> throw new IllegalArgumentException("Unsupported extension: " + extension);
        };
    }
}
