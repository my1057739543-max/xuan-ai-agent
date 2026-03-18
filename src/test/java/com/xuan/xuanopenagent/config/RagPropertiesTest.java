package com.xuan.xuanopenagent.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolated property-binding test for {@link RagProperties}.
 * Uses a minimal Spring context: no datasource, no vector store, no AI model needed.
 */
@SpringBootTest(
        classes = RagPropertiesTest.TestConfig.class,
        properties = {
                "xuan.rag.upload-dir=/tmp/xuan-test-uploads",
                "xuan.rag.top-k=6",
                "xuan.rag.similarity-threshold=0.65",
                "xuan.rag.chunk-size=1000",
                "xuan.rag.min-chunk-size-chars=400",
                "xuan.rag.min-chunk-length-to-embed=15",
                "xuan.rag.advisor-allow-empty-context=false",
                "xuan.rag.max-file-size-mb=10",
                "xuan.rag.enabled-file-types=txt,pdf"
        }
)
class RagPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(RagProperties.class)
    static class TestConfig {
    }

    @Autowired
    RagProperties ragProperties;

    @Test
    void allPropertiesBindCorrectly() {
        assertThat(ragProperties.getUploadDir()).isEqualTo("/tmp/xuan-test-uploads");
        assertThat(ragProperties.getTopK()).isEqualTo(6);
        assertThat(ragProperties.getSimilarityThreshold()).isEqualTo(0.65);
        assertThat(ragProperties.getChunkSize()).isEqualTo(1000);
        assertThat(ragProperties.getMinChunkSizeChars()).isEqualTo(400);
        assertThat(ragProperties.getMinChunkLengthToEmbed()).isEqualTo(15);
        assertThat(ragProperties.isAdvisorAllowEmptyContext()).isFalse();
        assertThat(ragProperties.getMaxFileSizeMb()).isEqualTo(10);
        assertThat(ragProperties.getEnabledFileTypes()).containsExactly("txt", "pdf");
    }

    @Test
    void defaultsAreCorrect() {
        // Defaults can be verified via a separate bean with no overrides, but here
        // we at lease confirm the List type isn't null and the threshold is a valid ratio.
        assertThat(ragProperties.getSimilarityThreshold()).isBetween(0.0, 1.0);
        assertThat(ragProperties.getEnabledFileTypes()).isNotEmpty();
    }
}
