package com.mutuo.superreforge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 验证发布元数据的用户可见契约。
 *
 * <p>如果模组 ID、作者、目标版本或许可证被模板默认值覆盖，这些测试会立即失败，
 * 避免生成一个能编译但无法被正确识别的 JAR。
 */
final class BootstrapMetadataTest {
    private static final Path MODS_TOML =
            Path.of("src", "main", "templates", "META-INF", "neoforge.mods.toml");

    @Test
    void metadataDeclaresThePublishedIdentityAndRequiredPlatforms() throws IOException {
        String metadata = Files.readString(MODS_TOML, StandardCharsets.UTF_8);

        assertTrue(metadata.contains("modId=\"${mod_id}\""));
        assertTrue(metadata.contains("displayName=\"${mod_name}\""));
        assertTrue(metadata.contains("authors=\"${mod_authors}\""));
        assertTrue(metadata.contains("license=\"${mod_license}\""));
        assertTrue(metadata.contains("modId=\"neoforge\""));
        assertTrue(metadata.contains("versionRange=\"[${neo_version},)\""));
        assertTrue(metadata.contains("modId=\"minecraft\""));
        assertTrue(metadata.contains("versionRange=\"${minecraft_version_range}\""));
    }

    @Test
    void testsRunOnJavaTwentyOneOrNewer() {
        assertTrue(Runtime.version().feature() >= 21, "Super Reforge 必须使用 Java 21 或更高版本构建");
    }
}
