package com.mutuo.superreforge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * 验证发布元数据的用户可见契约。
 *
 * <p>如果模组 ID、作者、目标版本或许可证被模板默认值覆盖，这些测试会立即失败，
 * 避免生成一个能编译但无法被正确识别的 JAR。
 */
final class BootstrapMetadataTest {
    @Test
    void metadataDeclaresThePublishedIdentityAndRequiredPlatforms() throws IOException {
        String metadata;
        try (var stream = BootstrapMetadataTest.class
                .getClassLoader()
                .getResourceAsStream("META-INF/neoforge.mods.toml")) {
            assertTrue(stream != null, "构建产物必须包含 META-INF/neoforge.mods.toml");
            metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(metadata.contains("modId=\"superreforge\""));
        assertTrue(metadata.contains("displayName=\"Super Reforge\""));
        assertTrue(metadata.contains("authors=\"MUTUO\""));
        assertTrue(metadata.contains("license=\"LGPL-3.0-or-later\""));
        assertTrue(metadata.contains("modId=\"neoforge\""));
        assertTrue(metadata.contains("versionRange=\"[21.1.244,)\""));
        assertTrue(metadata.contains("modId=\"minecraft\""));
        assertTrue(metadata.contains("versionRange=\"[1.21.1]\""));
    }

    @Test
    void testsRunOnJavaTwentyOneOrNewer() {
        assertTrue(Runtime.version().feature() >= 21, "Super Reforge 必须使用 Java 21 或更高版本构建");
    }
}
